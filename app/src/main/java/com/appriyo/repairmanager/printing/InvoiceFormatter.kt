package com.appriyo.repairmanager.printing

import com.appriyo.repairmanager.data.model.Repair
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Formats a [Repair] into a plain-text ESC/POS invoice string.
 * Store info is now hardcoded.
 */
object InvoiceFormatter {

    private const val LINE = "--------------------------------"
    private val DATE_FMT = SimpleDateFormat("dd/MM/yyyy", Locale.US)

    // HARDCODED STORE DETAILS - Change these to match your store
    private const val STORE_NAME = "Your Store Name"
    private const val STORE_ADDRESS = "123 Main Street, City"
    private const val STORE_PHONE = "+1234567890"

    fun buildInvoiceText(repair: Repair): String {
        val createdDate = repair.createdAt?.let { DATE_FMT.format(it) } ?: "-"

        val accessories = listOf(
            "Batt" to repair.batteryIncluded,
            "SIM" to repair.simIncluded,
            "Mem" to repair.memoryCardIncluded,
            "Tray" to repair.simTrayIncluded,
            "Cover" to repair.backCoverIncluded
        ).filter { it.second }.map { it.first }

        val securityLine: String = when (repair.securityType) {
            "None" -> "None"
            "Password" -> "Password: ${repair.password.take(16)}"
            "Pattern" -> "Pattern: ${repair.pattern.take(16)}"
            "Password & Pattern" -> "PWD+Pattern"
            else -> repair.securityType
        }

        return buildString {
            // Header with hardcoded store info
            appendLine("  ${STORE_NAME.uppercase().take(28)}")
            appendLine("  ${STORE_ADDRESS.take(30)}")
            appendLine("  Call: ${STORE_PHONE.take(20)}")
            appendLine(LINE)

            // Invoice meta
            appendLine("INVOICE: ${repair.serialNumber}")
            appendLine("DATE   : $createdDate")
            appendLine("STATUS : ${repair.status.take(12).uppercase()}")
            appendLine(LINE)

            // Customer
            appendLine("CUSTOMER INFO")
            appendLine("Name   : ${repair.customerName.take(20)}")
            appendLine("Phone  : ${repair.phoneNumber.take(15)}")
            appendLine(LINE)

            // Device
            appendLine("DEVICE INFO")
            appendLine("Model  : ${repair.deviceModel.ifBlank { "-" }.take(20)}")
            appendLine("Problem: ${repair.problemDescription.ifBlank { "-" }.take(20)}")
            appendLine("Deliver: ${repair.expectedDeliveryDate.ifBlank { "-" }.take(16)}")
            appendLine(LINE)

            // Payment
            if (repair.paymentInfo.isNotBlank()) {
                appendLine("PAYMENT INFO")
                appendLine("Info   : ${repair.paymentInfo.take(22)}")
                appendLine(LINE)
            }

            // Security
            appendLine("SECURITY")
            appendLine("Lock   : $securityLine")
            appendLine("Dead OK: ${if (repair.deadPhonePermission) "YES - APPROVED" else "No"}")
            appendLine(LINE)

            // Accessories
            if (accessories.isNotEmpty()) {
                appendLine("ACCESSORIES")
                appendLine(accessories.joinToString(", "))
                appendLine(LINE)
            }

            // Box / Additional
            if (repair.boxNumber.isNotBlank()) {
                appendLine("Box#   : ${repair.boxNumber.take(20)}")
            }
            if (repair.additionalDetails.isNotBlank()) {
                appendLine("Notes  : ${repair.additionalDetails.take(22)}")
            }
            if (repair.boxNumber.isNotBlank() || repair.additionalDetails.isNotBlank()) {
                appendLine(LINE)
            }

            // Footer
            appendLine("   THANK YOU FOR CHOOSING US!")
            appendLine(" Bring this invoice for delivery")
            appendLine()
            appendLine()
            appendLine()
        }
    }
}