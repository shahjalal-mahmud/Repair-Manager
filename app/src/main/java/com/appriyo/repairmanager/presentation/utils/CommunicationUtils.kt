// app/src/main/java/com/appriyo/repairmanager/presentation/utils/CommunicationUtils.kt
package com.appriyo.repairmanager.presentation.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.appriyo.repairmanager.data.model.Repair

/**
 * Opens the device's default SMS app with the conversation for [phoneNumber] prefilled
 * with [message]. Uses ACTION_SENDTO ("smsto:") so the user reviews and taps Send
 * themselves - this does NOT require the SEND_SMS permission.
 */
fun openSmsComposer(context: Context, phoneNumber: String, message: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
            putExtra("sms_body", message)
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No messaging app found on this device.", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Builds a short, friendly status-update SMS for a repair.
 */
fun buildStatusUpdateSms(repair: Repair): String {
    val device = repair.deviceModel.ifBlank { "your device" }
    return "Hi ${repair.customerName}, update on your repair (Invoice: ${repair.serialNumber}, " +
            "$device): status is now \"${repair.status}\". Thank you for your patience!"
}