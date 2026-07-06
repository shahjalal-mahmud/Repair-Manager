// app/src/main/java/com/appriyo/repairmanager/presentation/utils/CommunicationUtils.kt
package com.appriyo.repairmanager.presentation.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.appriyo.repairmanager.data.model.Repair
import com.appriyo.repairmanager.data.model.TaliKhataEntry
import com.appriyo.repairmanager.data.model.TaliKhataType
import com.appriyo.repairmanager.presentation.components.talikhata.formatCurrency

private const val TAG = "CommunicationUtils"

/**
 * Opens the device's default SMS app with [phoneNumber] already filled in as
 * the recipient and [message] placed in the compose field. The user reviews
 * and taps Send manually - this does NOT require the SEND_SMS permission.
 *
 * The trick to making the recipient actually pre-fill across all the major
 * Android SMS apps (Google Messages, Samsung Messages, Xiaomi MIUI Messages,
 * stock AOSP Messaging, etc.) is to send the number via THREE places at once:
 *
 *  1. The `sms:` URI's path (e.g. `sms:+8801712345678`) - Google Messages
 *     and AOSP Messaging read this.
 *  2. `Intent.EXTRA_PHONE_NUMBER` - Samsung Messages and MIUI read this.
 *  3. As the data URI's authority when the number is empty (so the user can
 *     still see the body if no number is on file).
 *
 * The message body is also passed in three forms:
 *  - `Intent.EXTRA_TEXT`       (standard ACTION_SEND contract)
 *  - `"sms_body"` extra        (legacy AOSP Messaging contract)
 *
 * Earlier versions used `Uri.buildUpon().appendQueryParameter("body", ...)`,
 * but that gets re-encoded by some SMS apps into the recipient field,
 * causing the symptom "the SMS app opens but the recipient is blank / asks
 * me to pick one". We deliberately keep the body OUT of the URI.
 *
 * Requires a matching `<queries>` entry in AndroidManifest.xml so that
 * `resolveActivity()` can see SMS apps on Android 11+ (package visibility).
 */
fun openSmsComposer(context: Context, phoneNumber: String, message: String) {
    try {
        // Strip everything except digits and the leading '+'. The recipient
        // segment of an sms: URI must be a phone number, not free text - any
        // dashes/spaces/parens that the user typed in the entry would be
        // rejected by the SMS app's recipient validator.
        val sanitizedNumber = phoneNumber.filter { it.isDigit() || it == '+' }

        // Build the URI by hand rather than via Uri.Builder, because Uri.Builder
        // percent-encodes the '+' in "+880..." to "%2B...". Most SMS apps
        // tolerate that, but some (older Samsung) don't decode it back, so
        // they treat the number as "%2B8801712345678" and refuse to match it
        // to a contact. String interpolation keeps the raw '+' intact.
        val recipientSegment = if (sanitizedNumber.isBlank()) "" else sanitizedNumber
        val uri: Uri = Uri.parse("sms:$recipientSegment")

        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            // 1) Redundant on most apps, but some OEM apps (Samsung, MIUI)
            //    read the recipient from EXTRA_PHONE_NUMBER and IGNORE the
            //    sms: URI. Setting both ensures the field is filled.
            putExtra(Intent.EXTRA_PHONE_NUMBER, sanitizedNumber)

            // 2) Some apps read the recipient from "address" (legacy intent
            //    extra), although it's been deprecated since Android 1.0.
            //    Cheap to set, harmless if ignored.
            @Suppress("DEPRECATION")
            putExtra("address", sanitizedNumber)

            // Body - send three forms so every SMS app picks one up:
            putExtra(Intent.EXTRA_TEXT, message)   // Google Messages
            putExtra("sms_body", message)          // AOSP / legacy Messaging
            putExtra(Intent.EXTRA_SUBJECT, "")     // belt + braces

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
        Log.w(TAG, "SMS app not found", e)
        Toast.makeText(
            context,
            "No messaging app found on this device.",
            Toast.LENGTH_SHORT
        ).show()
    } catch (e: Exception) {
        Log.e(TAG, "Failed to open SMS composer", e)
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