// app/src/main/java/com/appriyo/repairmanager/data/sms/SmsSender.kt
package com.appriyo.repairmanager.data.sms

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat

data class SimSlotOption(val index: Int, val label: String)

/**
 * Thin wrapper around SmsManager that supports dual-SIM slot selection.
 * simSlotIndex == -1 means "let the system pick the default SIM".
 *
 * On Android 13+ (SDK 33+), [SmsManager.sendTextMessage] from a non-default
 * SMS app is silently dropped on some OEMs (Pixel, One UI 5+). To detect
 * that case, we attach non-null [sentIntent] PendingIntents pointing at
 * [SmsSentReceiver] so the framework reports back via the broadcast
 * `Activity.RESULT_OK` / `SmsManager.RESULT_ERROR_*` codes — instead of
 * pretending the send succeeded.
 */
class SmsSender(private val context: Context) {

    fun hasSendPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED

    /**
     * Sends [message] to [phoneNumber] using the configured SIM slot.
     *
     * Returns true **only if the framework accepted the handoff** (i.e. did
     * not throw before the call returned). The authoritative success/failure
     * arrives asynchronously via [SmsSentReceiver], which writes the result
     * to Firestore. This synchronous return is used by the caller purely to
     * decide whether to update the UI; the Firestore log is the source of
     * truth for "did the message actually go out".
     *
     * Delivery receipts remain out of scope (we do not pass a deliveryIntent).
     */
    fun sendSms(
        phoneNumber: String,
        message: String,
        simSlotIndex: Int,
        repairId: String,
        status: String
    ): Boolean {
        if (phoneNumber.isBlank() || !hasSendPermission()) return false

        return try {
            val smsManager = resolveSmsManager(simSlotIndex)
            val sentIntent = buildSentIntent(repairId, status)
            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                val sentIntents = ArrayList<PendingIntent>(parts.size).apply { repeat(parts.size) { add(sentIntent) } }
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, null)
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, sentIntent, null)
            }
            true
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /** Lists SIM slots available for the settings screen's slot picker. */
    fun getAvailableSimSlots(): List<SimSlotOption> {
        val options = mutableListOf(SimSlotOption(-1, "Automatic (system default)"))
        // Android 13+ (SDK 33+): SubscriptionManager.getActiveSubscriptionInfoList
        // returns null unless READ_PHONE_NUMBERS is granted in addition to
        // READ_PHONE_STATE. We require both so the user can actually see/select
        // their SIM slots.
        val hasPhonePerms = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) ==
                PackageManager.PERMISSION_GRANTED
        if (!hasPhonePerms) {
            return options
        }
        return try {
            val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
            val activeSubs = subscriptionManager?.activeSubscriptionInfoList.orEmpty()
            activeSubs.forEachIndexed { index, info ->
                val label = info.displayName?.toString()?.takeIf { it.isNotBlank() } ?: "SIM ${index + 1}"
                options.add(SimSlotOption(index, label))
            }
            options
        } catch (e: SecurityException) {
            options
        }
    }

    /**
     * Builds a PendingIntent that fires [SmsSentReceiver] with this send's
     * identity (repairId + status) so the receiver can look up the right
     * log doc to update.
     *
     * - FLAG_IMMUTABLE is required on targetSdk 31+ and is harmless on older versions.
     * - FLAG_UPDATE_CURRENT lets us reuse the same PendingIntent across retries of
     *   the same (repairId, status) pair — the extras are overwritten on each send.
     * - The PendingIntent must be unique-per-(repairId, status) so two repairs
     *   updating the SMS sender at the same instant don't collide. We use a
     *   request code derived from the pair's hashCode.
     */
    private fun buildSentIntent(repairId: String, status: String): PendingIntent {
        val intent = Intent(context, SmsSentReceiver::class.java).apply {
            action = SmsSentReceiver.ACTION_SMS_SENT
            putExtra(SmsSentReceiver.EXTRA_REPAIR_ID, repairId)
            putExtra(SmsSentReceiver.EXTRA_STATUS, status)
        }
        val requestCode = (repairId + "|" + status).hashCode()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    private fun resolveSmsManager(simSlotIndex: Int): SmsManager {
        if (simSlotIndex < 0) return defaultSmsManager()
        // Same READ_PHONE_NUMBERS gate as getAvailableSimSlots — without it,
        // activeSubscriptionInfoList is null on Android 13+ and we silently
        // fall back to the default SIM (which may not be the configured one).
        val canResolveSim =
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
                PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) ==
                PackageManager.PERMISSION_GRANTED
        if (!canResolveSim) return defaultSmsManager()
        return try {
            val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
            val subInfo = subscriptionManager?.activeSubscriptionInfoList?.getOrNull(simSlotIndex)
            if (subInfo != null) {
                SmsManager.getSmsManagerForSubscriptionId(subInfo.subscriptionId)
            } else {
                defaultSmsManager()
            }
        } catch (e: SecurityException) {
            defaultSmsManager()
        }
    }

    private fun defaultSmsManager(): SmsManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
}
