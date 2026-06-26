package com.appriyo.repairmanager.printing

import com.appriyo.repairmanager.data.model.Repair
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Formats a [Repair] record into a plain-text ESC/POS printable invoice.
 *
 * This formatter generates a clean, well-structured invoice layout suitable
 * for thermal receipt printers. Store information is hardcoded for simplicity.
 *
 * @author RepairManager Team
 * @since 1.0.0
 */
object InvoiceFormatter {

    // ======================== CONSTANTS ========================

    private const val LINE_SEPARATOR = "--------------------------------"
    private val DATE_FORMATTER = SimpleDateFormat("dd/MM/yyyy", Locale.US)

    // ======================== STORE CONFIGURATION ========================
    private const val STORE_NAME = "...Tama Electronic..."
    private const val STORE_ADDRESS = "Sachibuniya Bazar, Lobonchora, Khulna"
    private const val STORE_PHONE = "01910544744"

    // ======================== PUBLIC API ========================

    /**
     * Builds a formatted invoice text string from a [Repair] record.
     *
     * @param repair The repair record to format
     * @return A plain-text invoice string formatted for ESC/POS printing
     */
    fun buildInvoiceText(repair: Repair): String {
        val createdDate = extractCreatedDate(repair)
        val accessories = extractAccessories(repair)
        val securityLine = formatSecurityInfo(repair)

        return buildString {
            // --- Store Header ---
            appendLine("  ${STORE_NAME.uppercase().take(28)}")
            appendLine("  ${STORE_ADDRESS.take(30)}")
            appendLine("  Call: ${STORE_PHONE.take(20)}")
            appendLine(LINE_SEPARATOR)

            // --- Invoice Metadata ---
            appendLine("INVOICE: ${repair.serialNumber}")
            appendLine("DATE    : $createdDate")
            appendLine("STATUS  : ${repair.status.take(12).uppercase()}")
            appendLine(LINE_SEPARATOR)

            // --- Customer Information ---
            appendLine("CUSTOMER INFO")
            appendLine("Name    : ${repair.customerName.take(20)}")
            appendLine("Phone   : ${repair.phoneNumber.take(15)}")
            appendLine(LINE_SEPARATOR)

            // --- Device Information ---
            appendLine("DEVICE INFO")
            appendLine("Model   : ${repair.deviceModel.ifBlank { "-" }.take(20)}")
            appendLine("Problem : ${repair.problemDescription.ifBlank { "-" }.take(20)}")
            appendLine("Deliver : ${repair.expectedDeliveryDate.ifBlank { "-" }.take(16)}")
            appendLine(LINE_SEPARATOR)

            // --- Payment Information (Optional) ---
            if (repair.paymentInfo.isNotBlank()) {
                appendLine("PAYMENT INFO")
                appendLine("Info    : ${repair.paymentInfo.take(22)}")
                appendLine(LINE_SEPARATOR)
            }

            // --- Security Information ---
            appendLine("SECURITY")
            appendLine("Lock    : $securityLine")
            appendLine("Dead OK : ${if (repair.deadPhonePermission) "YES - APPROVED" else "No"}")
            appendLine(LINE_SEPARATOR)

            // --- Accessories (Optional) ---
            if (accessories.isNotEmpty()) {
                appendLine("ACCESSORIES")
                appendLine(accessories.joinToString(", "))
                appendLine(LINE_SEPARATOR)
            }

            // --- Additional Details (Optional) ---
            var hasAdditionalInfo = false
            if (repair.boxNumber.isNotBlank()) {
                appendLine("Box #   : ${repair.boxNumber.take(20)}")
                hasAdditionalInfo = true
            }
            if (repair.additionalDetails.isNotBlank()) {
                appendLine("Notes   : ${repair.additionalDetails.take(22)}")
                hasAdditionalInfo = true
            }
            if (hasAdditionalInfo) {
                appendLine(LINE_SEPARATOR)
            }

            // --- Footer ---
            appendLine("   THANK YOU FOR CHOOSING US!")
            appendLine(" Bring this invoice for delivery")
            appendLine()
            appendLine()
            appendLine()
        }
    }

    // ======================== PRIVATE HELPERS ========================

    /**
     * Extracts and formats the creation date from the repair record.
     * Uses the actual creation timestamp, not the current date.
     */
    private fun extractCreatedDate(repair: Repair): String {
        return repair.createdAt?.let { DATE_FORMATTER.format(it) } ?: "-"
    }

    /**
     * Builds a list of included accessories from the repair record.
     */
    private fun extractAccessories(repair: Repair): List<String> {
        return listOf(
            "Batt" to repair.batteryIncluded,
            "SIM" to repair.simIncluded,
            "Mem" to repair.memoryCardIncluded,
            "Tray" to repair.simTrayIncluded,
            "Cover" to repair.backCoverIncluded
        ).filter { it.second }
            .map { it.first }
    }

    /**
     * Formats security information based on the security type.
     */
    private fun formatSecurityInfo(repair: Repair): String {
        return when (repair.securityType) {
            "None" -> "None"
            "Password" -> "Password: ${repair.password.take(16)}"
            "Pattern" -> "Pattern: ${repair.pattern.take(16)}"
            "Password & Pattern" -> "PWD+Pattern"
            else -> repair.securityType
        }
    }
}