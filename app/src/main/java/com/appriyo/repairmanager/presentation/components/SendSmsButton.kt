// app/src/main/java/com/appriyo/repairmanager/presentation/components/SendSmsButton.kt
package com.appriyo.repairmanager.presentation.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.appriyo.repairmanager.data.sms.SmsTemplateProvider

/**
 * Manual "Send SMS" button for the Customer Details screen.
 * Uses ACTION_SENDTO so it opens the user's own SMS app prefilled with the templated
 * message - no SEND_SMS permission required, and the user can edit before sending.
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
            // Same canonical sms: URI + ?body= query parameter pattern used by
            // openSmsComposer() - see that function for the rationale.
            val sanitizedNumber = phoneNumber.filter { it.isDigit() || it == '+' }
            val uri = ("sms:$sanitizedNumber").toUri().buildUpon()
                .appendQueryParameter("body", message)
                .build()
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "No SMS app found on this device.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ActivityNotFoundException) {
                Log.w("SendSmsButton", "SMS app not found", e)
                Toast.makeText(context, "No SMS app found on this device.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("SendSmsButton", "Failed to open SMS composer", e)
                Toast.makeText(context, "Couldn't open SMS app: ${e.message ?: "unknown error"}", Toast.LENGTH_SHORT).show()
            }
        }
    ) {
        Text("Send SMS")
    }
}