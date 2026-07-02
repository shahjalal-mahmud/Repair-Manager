// app/src/main/java/com/appriyo/repairmanager/presentation/utils/CommunicationUtils.kt
package com.appriyo.repairmanager.presentation.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import com.appriyo.repairmanager.data.model.Repair
import com.appriyo.repairmanager.data.model.TaliKhataEntry
import com.appriyo.repairmanager.data.model.TaliKhataType
import com.appriyo.repairmanager.presentation.components.talikhata.formatCurrency

/**
 * Opens the device's default SMS app with the conversation for [phoneNumber] prefilled
 * with [message]. Uses ACTION_SENDTO ("smsto:") so the user reviews and taps Send
 * themselves - this does NOT require the SEND_SMS permission.
 */
fun openSmsComposer(context: Context, phoneNumber: String, message: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "smsto:$phoneNumber".toUri()
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

/**
 * Builds the SMS body for a TaliKhata ledger entry — includes the person's
 * name, formatted balance and (if present) the entry's details. The user reviews
 * and taps Send manually in their messaging app.
 */
fun buildTaliKhataSms(entry: TaliKhataEntry): String {
    val amount = formatCurrency(entry.balance)
    val direction = if (entry.typeEnum == TaliKhataType.YOU_OWE) "I owe you" else "you owe"
    val core = "Hi ${entry.personName}, just a reminder that $direction $amount."
    return if (entry.details.isNotBlank()) {
        "$core Ref: ${entry.details}. Thank you!"
    } else {
        "$core Thank you!"
    }
}