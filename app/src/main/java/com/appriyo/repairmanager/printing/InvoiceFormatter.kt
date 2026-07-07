package com.appriyo.repairmanager.printing

import com.appriyo.repairmanager.data.model.ProductSell
import com.appriyo.repairmanager.data.model.Repair
import java.text.SimpleDateFormat
import java.util.Calendar
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

    // ======================== PRODUCT SELL INVOICE ========================

    /**
     * Builds a formatted sale-invoice text string from a [ProductSell]
     * record. This is a separate, professionally laid-out invoice intended
     * to be handed to the customer when they purchase a product, and it
     * prominently surfaces the warranty details with computed expiry date.
     *
     * The store header (name, address, contact) is intentionally reused
     * from the same hardcoded values as [buildInvoiceText] so both
     * invoices share a consistent brand.
     *
     * @param sell The product sell record to format
     * @return A plain-text invoice string formatted for ESC/POS printing
     */
    fun buildInvoiceText(sell: ProductSell): String {
        val sellDate = sell.createdAt?.let { DATE_FORMATTER.format(it) } ?: "-"
        val sellTime = sell.createdAt?.let {
            SimpleDateFormat("hh:mm a", Locale.US).format(it)
        } ?: "-"
        val warrantyStart = sell.warrantyStartDate.ifBlank { sellDate }

        // productPrice / paymentAmount / warrantyMonths are free text (the
        // shopkeeper may type digits, Bangla digits, or words like "Free").
        // We always print them exactly as typed, and only attempt the
        // numeric extras below (Due amount, computed warranty expiry) when
        // they happen to parse cleanly as plain numbers - never block or
        // crash on non-numeric input.
        val parsedPrice = sell.productPrice.trim().toDoubleOrNull()
        val parsedPaid = sell.paymentAmount.trim().toDoubleOrNull()
        val balanceDue = if (parsedPrice != null && parsedPaid != null) {
            (parsedPrice - parsedPaid).coerceAtLeast(0.0)
        } else {
            null
        }

        val warrantyMonthsNumber = sell.warrantyMonths.trim().toIntOrNull()
        val warrantyExpiry = if (warrantyMonthsNumber != null && warrantyMonthsNumber > 0) {
            computeWarrantyExpiry(warrantyStart, warrantyMonthsNumber)
        } else {
            null
        }

        return buildString {
            // --- Store Header ---
            appendLine("  ${STORE_NAME.uppercase().take(28)}")
            appendLine("  ${STORE_ADDRESS.take(30)}")
            appendLine("  Call: ${STORE_PHONE.take(20)}")
            appendLine(LINE_SEPARATOR)

            // --- Invoice Metadata ---
            appendLine("SALE INVOICE: ${sell.serialNumber}")
            appendLine("DATE    : $sellDate   TIME: $sellTime")
            appendLine(LINE_SEPARATOR)

            // --- Product Information ---
            appendLine("PRODUCT INFO")
            appendLine("Item    : ${sell.productName.take(24)}")
            if (sell.productSerial.isNotBlank()) {
                appendLine("S/N     : ${sell.productSerial.take(24)}")
            }
            appendLine(LINE_SEPARATOR)

            // --- Pricing ---
            appendLine("PRICING")
            appendLine("Price   : Taka ${sell.productPrice.trim().ifBlank { "-" }.take(20)}")
            appendLine("Paid    : Taka ${sell.paymentAmount.trim().ifBlank { "-" }.take(20)}")
            if (balanceDue != null && balanceDue > 0.0) {
                appendLine("Due     : Taka ${formatMoney(balanceDue)}")
            }
            appendLine(LINE_SEPARATOR)

            // --- Warranty Information ---
            appendLine("WARRANTY")
            if (sell.warrantyMonths.isNotBlank()) {
                appendLine("Period  : ${sell.warrantyMonths.trim().take(16)} Month(s)")
                appendLine("From    : ${warrantyStart.take(16)}")
                if (warrantyExpiry != null) {
                    appendLine("Until   : ${warrantyExpiry.take(16)}")
                }
            } else {
                appendLine("Period  : No Warranty")
            }
            if (sell.warrantyDetails.isNotBlank()) {
                appendLine("Terms   : ${sell.warrantyDetails.take(22)}")
            }
            appendLine(LINE_SEPARATOR)

            // --- Additional Notes (Optional) ---
            if (sell.notes.isNotBlank()) {
                appendLine("NOTES")
                appendLine("Notes   : ${sell.notes.take(22)}")
                appendLine(LINE_SEPARATOR)
            }

            // --- Footer ---
            appendLine("   THANK YOU FOR YOUR PURCHASE!")
            appendLine(" Keep this invoice for warranty.")
            appendLine()
            appendLine()
            appendLine()
        }
    }

    /**
     * Formats a monetary amount with two decimals, no thousand separator so
     * it stays compact on a 32-char thermal receipt.
     */
    private fun formatMoney(value: Double): String {
        return String.format(Locale.US, "%.2f", value)
    }

    /**
     * Computes the warranty expiry date as a dd/MM/yyyy string by adding
     * [months] to [startDate]. Returns "-" when:
     *  - [startDate] is blank
     *  - [months] is 0 or negative
     *  - the start date can't be parsed
     *
     * The parser is permissive: it accepts dd/MM/yyyy and dd-MM-yyyy,
     * matching the formats used elsewhere in the app.
     */
    private fun computeWarrantyExpiry(startDate: String, months: Int): String {
        if (startDate.isBlank() || months <= 0) return "-"
        val parts = startDate.split('/', '-').takeIf { it.size == 3 } ?: return "-"
        return try {
            val day = parts[0].trim().toInt()
            val month = parts[1].trim().toInt()
            val year = parts[2].trim().toInt()
            val cal = Calendar.getInstance().apply {
                isLenient = false
                clear()
                set(year, month - 1, day)
                add(Calendar.MONTH, months)
            }
            DATE_FORMATTER.format(cal.time)
        } catch (_: Exception) {
            "-"
        }
    }
}