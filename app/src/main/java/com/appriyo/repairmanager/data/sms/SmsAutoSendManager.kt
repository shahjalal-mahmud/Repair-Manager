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
 * and for each one: claims smsLogs/{repairId}_{status} transactionally, and only if
 * that succeeds (meaning no SMS was sent yet for this exact repair+status) does it
 * actually send the SMS and record the result.
 *
 * Call start() once, from application scope, and it runs for the lifetime of the process.
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

                    if (isThisDeviceSender) {
                        repairRepository.observeRepairChanges().map { changed -> settings to changed }
                    } else {
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

        if (!claimed) return // already sent for this repair+status

        val success = smsSender.sendSms(repair.phoneNumber, message, simSlotIndex)
        smsLogRepository.updateLogResult(repair.id, repair.status, success)
    }
}