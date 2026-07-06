// app/src/main/java/com/appriyo/repairmanager/presentation/components/SendSmsButton.kt
package com.appriyo.repairmanager.presentation.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.appriyo.repairmanager.data.sms.SmsTemplateProvider

private const val TAG = "SendSmsButton"

/**
 * Manual "Send SMS" button for the Customer Details screen.
 * Uses ACTION_SENDTO so it opens the user's own SMS app prefilled with the templated
 * message - no SEND_SMS permission required, and the user can edit before sending.
 *
 * Recipient auto-fill is done the same way as openSmsComposer() - via the
 * sms: URI PLUS Intent.EXTRA_PHONE_NUMBER, because Google Messages, Samsung
 * Messages, and MIUI Messages each prefer a different one. Just setting
 * the URI is not enough on most OEM devices.
 *
 * Wire this into CustomerDetailsScreen like:
 *   SendSmsButton(
 *       customerName = repair.customerName,
 *       serialNumber = repair.serialNumber,
 *       status = repair.status,
 *       phoneNumber = repair.phoneNumber
 *   )
 */
@Composable
fun SendSmsButton(
    customerName: String,
    serialNumber: String,
    status: String,
    phoneNumber: String,
    paymentInfo: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Button(
        modifier = modifier,
        onClick = {
            if (phoneNumber.isBlank()) {
                Toast.makeText(context, "No phone number on file for this customer.", Toast.LENGTH_SHORT).show()
                return@Button
            }
            val message = SmsTemplateProvider.getMessage(status, customerName, serialNumber, paymentInfo)

            // Strip spaces/dashes/parens - the recipient segment of an sms:
            // URI must be a phone number, not free text.
            val sanitizedNumber = phoneNumber.filter { it.isDigit() || it == '+' }
            val uri: Uri = Uri.parse("sms:$sanitizedNumber")

            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                // Three places for the recipient - each major SMS app reads
                // a different one.
                putExtra(Intent.EXTRA_PHONE_NUMBER, sanitizedNumber)
                @Suppress("DEPRECATION")
                putExtra("address", sanitizedNumber)

                // Three forms of the body, for the same reason.
                putExtra(Intent.EXTRA_TEXT, message)
                putExtra("sms_body", message)

                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "No SMS app found on this device.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ActivityNotFoundException) {
                Log.w(TAG, "SMS app not found", e)
                Toast.makeText(context, "No SMS app found on this device.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open SMS composer", e)
                Toast.makeText(context, "Couldn't open SMS app: ${e.message ?: "unknown error"}", Toast.LENGTH_SHORT).show()
            }
        }
    ) {
        Text("Send SMS")
    }
}