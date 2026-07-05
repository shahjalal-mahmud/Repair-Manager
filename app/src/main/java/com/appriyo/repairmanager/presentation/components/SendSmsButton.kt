// app/src/main/java/com/appriyo/repairmanager/presentation/components/SendSmsButton.kt
package com.appriyo.repairmanager.presentation.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
                putExtra("sms_body", message)
            }
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "No SMS app found on this device.", Toast.LENGTH_SHORT).show()
            }
        }
    ) {
        Text("Send SMS")
    }
}