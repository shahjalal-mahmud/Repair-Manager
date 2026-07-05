// app/src/main/java/com/appriyo/repairmanager/data/sms/SmsAutoSendManager.kt
package com.appriyo.repairmanager.data.sms

import android.content.Context
import com.appriyo.repairmanager.data.repository.AppSettingsRepository
import com.appriyo.repairmanager.data.repository.RepairRepository
import com.appriyo.repairmanager.data.repository.SmsLogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The "automatic SMS listener" from the spec.
 *
 * Only does anything on the device whose ID matches appSettings.smsSenderDeviceId.
 * On that device, it watches the repairs collection for added/modified documents,
 * and for each one: claims smsLogs/{repairId}_{status} (transactionally) and, only
 * if that succeeds, calls [SmsSender.sendSms].
 *
 * The authoritative success/failure signal comes back via [SmsSentReceiver],
 * which calls [SmsLogRepository.recordAttemptResult] to mark the doc successful
 * or schedule the next retry via the backoff window.
 *
 * Process-lifetime model:
 *  - When this device becomes the designated sender, [ensureServiceRunning] is
 *    called so [SmsAutoSendService] raises a foreground notification. This is
 *    what keeps the listener alive on Android 13+ (where the OS aggressively
 *    kills background processes that don't hold a foreground role).
 *  - When this device loses the sender designation, [stopService] is called so
 *    we don't keep a notification the user can see.
 *  - On Android 10 (and any other version where you don't grant the runtime
 *    notification permission), the service still starts in the background -
 *    the listener will work as long as the process is alive.
 */
class SmsAutoSendManager(
    private val context: Context,
    private val repairRepository: RepairRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val smsLogRepository: SmsLogRepository,
    private val smsSender: SmsSender,
    private val deviceIdProvider: DeviceIdProvider
) {

    private var started = false

    /**
     * Latest known value of `isThisDeviceSender`, computed inside the
     * listener flow and updated whenever a new appSettings snapshot arrives.
     * Used by [onAppForegrounded] to decide synchronously whether to start
     * the foreground service (we can't wait for a new snapshot).
     */
    @Volatile
    private var cachedIsSender: Boolean = false

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(scope: CoroutineScope) {
        if (started) return
        started = true

        scope.launch {
            appSettingsRepository.observeSettings()
                .flatMapLatest { settings ->
                    val isThisDeviceSender = settings != null &&
                            settings.smsSenderDeviceId.isNotBlank() &&
                            settings.smsSenderDeviceId == deviceIdProvider.getDeviceId()
                    cachedIsSender = isThisDeviceSender

                    if (isThisDeviceSender) {
                        // Only start the foreground service if we're in a
                        // context where that's legal (Android 12+ restricts
                        // background FGS starts). The flag is set by
                        // MainActivity onResume / onPause; outside that
                        // window we still run the listener in-process.
                        if (canStartForegroundService) {
                            ensureServiceRunning()
                        }
                        repairRepository.observeRepairChanges().map { changed -> settings to changed }
                    } else {
                        stopService()
                        flowOf(null)
                    }
                }
                .catch { /* keep the listener alive across transient Firestore errors */ }
                .collect { pair ->
                    if (pair == null) return@collect
                    val (settings, changedRepairs) = pair
                    changedRepairs.forEach { repair ->
                        processRepair(repair, settings!!.simSlotIndex)
                    }
                }
        }
    }

    /**
     * Set by [com.appriyo.repairmanager.MainActivity] onResume/onPause to
     * gate when [SmsAutoSendService.start] may be called. Android 12+
     * rejects `startForegroundService` from a background context, so we
     * defer it until the user has the app visible.
     */
    @Volatile
    var canStartForegroundService: Boolean = false

    /**
     * Called by MainActivity.onResume. If this device is the SMS sender
     * (per the most recently cached appSettings snapshot), starts the
     * foreground service. Safe to call repeatedly.
     *
     * On a cold start, the first appSettings snapshot arrives AFTER
     * onResume, so cachedIsSender may still be false here. In that case
     * the snapshot handler inside start() will pick up the flag and call
     * ensureServiceRunning() on the next emission.
     */
    fun onAppForegrounded() {
        canStartForegroundService = true
        if (cachedIsSender) {
            ensureServiceRunning()
        }
    }

    /**
     * Called by MainActivity.onPause.
     *
     * Important: we DO NOT stop the foreground service here. The whole
     * point of the FGS is to keep the listener alive while the user
     * backgrounds the app on Android 13+. Stopping it would let the OS
     * kill the process and break SMS auto-send.
     *
     * We only flip the flag so that if the FGS is stopped for some other
     * reason (e.g. error), we won't try to re-start it from a now-backgrounded
     * activity context.
     */
    fun onAppBackgrounded() {
        canStartForegroundService = false
    }

    private suspend fun processRepair(repair: com.appriyo.repairmanager.data.model.Repair, simSlotIndex: Int) {
        if (repair.id.isBlank() || repair.status.isBlank() || repair.phoneNumber.isBlank()) return
        if (!smsSender.hasSendPermission()) return // will be retried on the next snapshot once permission is granted

        val message = SmsTemplateProvider.getMessage(
            status = repair.status,
            customerName = repair.customerName,
            serialNumber = repair.serialNumber,
            paymentInfo = repair.paymentInfo
        )

        val claimed = smsLogRepository.tryClaimLog(
            repairId = repair.id,
            status = repair.status,
            phoneNumber = repair.phoneNumber,
            message = message,
            deviceId = deviceIdProvider.getDeviceId()
        ).getOrNull() ?: return

        if (!claimed) return
        // tryClaimLog guarantees at most one in-flight send per (repairId, status).
        // The actual success/failure is written asynchronously by SmsSentReceiver
        // via SmsLogRepository.recordAttemptResult - we do NOT optimistically
        // write success here, because on Android 13+ a successful framework call
        // can still result in a silent radio drop that only the receiver sees.

        smsSender.sendSms(
            phoneNumber = repair.phoneNumber,
            message = message,
            simSlotIndex = simSlotIndex,
            repairId = repair.id,
            status = repair.status
        )
    }

    private fun ensureServiceRunning() {
        try {
            SmsAutoSendService.start(context)
        } catch (_: Exception) {
            // If the foreground service can't start (e.g. POST_NOTIFICATIONS denied
            // on Android 13+), the listener still runs in-process for as long as
            // the process is alive. The original behaviour is preserved.
        }
    }

    private fun stopService() {
        try {
            SmsAutoSendService.stop(context)
        } catch (_: Exception) {
            // Best-effort. The service will exit on its own on the next onDestroy.
        }
    }
}
