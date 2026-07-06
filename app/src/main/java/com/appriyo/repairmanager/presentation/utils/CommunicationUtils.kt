// app/src/main/java/com/appriyo/repairmanager/presentation/utils/CommunicationUtils.kt
package com.appriyo.repairmanager.presentation.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import com.appriyo.repairmanager.data.model.Repair
import com.appriyo.repairmanager.data.model.TaliKhataEntry
import com.appriyo.repairmanager.data.model.TaliKhataType
import com.appriyo.repairmanager.presentation.components.talikhata.formatCurrency

/**
 * Opens the device's default SMS app with the conversation for [phoneNumber]
 * prefilled and [message] placed in the compose field. The user reviews and
 * taps Send manually - this does NOT require the SEND_SMS permission.
 *
 * Implementation note: earlier versions used the legacy `smsto:` scheme +
 * `sms_body` extra. That combination is unreliable on modern devices:
 *   - Google Messages and several OEM SMS apps ignore the recipient
 *     when launched with `smsto:` and just open a blank composer.
 *   - `sms_body` is a non-standard extra; some apps ignore it.
 * The current implementation uses the canonical `sms:` URI (RFC 5724)
 * with the body passed as the `body` query parameter, which is what the
 * Android docs and modern messaging apps expect.
 *
 * Requires a matching `<queries>` entry in AndroidManifest.xml so that
 * `resolveActivity()` can see SMS apps on Android 11+ (package visibility).
 */
fun openSmsComposer(context: Context, phoneNumber: String, message: String) {
    try {
        // Strip anything that isn't a digit or '+' - the recipient segment of
        // an sms: URI must be a phone number, not arbitrary text.
        val sanitizedNumber = phoneNumber.filter { it.isDigit() || it == '+' }
        val uri = if (sanitizedNumber.isBlank()) {
            "sms:".toUri()
        } else {
            "sms:$sanitizedNumber".toUri()
        }.buildUpon()
            .appendQueryParameter("body", message)
            .build()

        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            // FLAG_ACTIVITY_NEW_TASK lets us safely launch from any Context
            // (Application, Service, etc). When the Context is already an
            // Activity (the normal case here) this flag is harmless.
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // resolveActivity() avoids a silent failure on devices without an
        // SMS app (or on Android 11+ where package visibility rules hide
        // SMS apps unless you declared `<queries>` in the manifest).
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(
                context,
                "No messaging app found on this device.",
                Toast.LENGTH_SHORT
            ).show()
        }
    } catch (e: ActivityNotFoundException) {
        Log.w("CommunicationUtils", "SMS app not found", e)
        Toast.makeText(
            context,
            "No messaging app found on this device.",
            Toast.LENGTH_SHORT
        ).show()
    } catch (e: Exception) {
        Log.e("CommunicationUtils", "Failed to open SMS composer", e)
        Toast.makeText(
            context,
            "Couldn't open SMS app: ${e.message ?: "unknown error"}",
            Toast.LENGTH_SHORT
        ).show()
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