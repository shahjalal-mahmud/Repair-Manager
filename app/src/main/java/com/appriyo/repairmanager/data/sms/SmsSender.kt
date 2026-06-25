// app/src/main/java/com/appriyo/repairmanager/data/sms/SmsSender.kt
package com.appriyo.repairmanager.data.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat

data class SimSlotOption(val index: Int, val label: String)

/**
 * Thin wrapper around SmsManager that supports dual-SIM slot selection.
 * simSlotIndex == -1 means "let the system pick the default SIM".
 */
class SmsSender(private val context: Context) {

    fun hasSendPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED

    /**
     * Sends [message] to [phoneNumber] using the configured SIM slot.
     * Returns true only if the message was handed off to the radio without throwing.
     * (This reports submission success, not delivery - delivery receipts are out of scope.)
     */
    fun sendSms(phoneNumber: String, message: String, simSlotIndex: Int): Boolean {
        if (phoneNumber.isBlank() || !hasSendPermission()) return false

        return try {
            val smsManager = resolveSmsManager(simSlotIndex)
            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
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
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
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

    private fun resolveSmsManager(simSlotIndex: Int): SmsManager {
        if (simSlotIndex < 0) return defaultSmsManager()
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