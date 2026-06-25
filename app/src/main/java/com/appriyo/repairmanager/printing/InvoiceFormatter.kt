// app/src/main/java/com/appriyo/repairmanager/printing/InvoiceFormatter.kt
package com.appriyo.repairmanager.printing

import com.appriyo.repairmanager.data.model.Repair
import com.appriyo.repairmanager.data.model.StoreInfo
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Formats a [Repair] + [StoreInfo] into a plain-text ESC/POS invoice string.
 *
 * Width is optimised for 48mm POS printers (32 characters per line).
 * The formatting style is preserved from the original InvoiceFormatter;
 * fields are mapped from the new Repair model.
 *
 * Output is passed directly to [POSPrinterHelper.printText].
 */
object InvoiceFormatter {

    private const val LINE = "--------------------------------"
    private val DATE_FMT = SimpleDateFormat("dd/MM/yyyy", Locale.US)

    fun buildInvoiceText(repair: Repair, storeInfo: StoreInfo): String {
        val createdDate = repair.createdAt?.let { DATE_FMT.format(it) } ?: "-"

        val accessories = listOf(
            "Batt"  to repair.batteryIncluded,
            "SIM"   to repair.simIncluded,
            "Mem"   to repair.memoryCardIncluded,
            "Tray"  to repair.simTrayIncluded,
            "Cover" to repair.backCoverIncluded
        ).filter { it.second }.map { it.first }

        val securityLine: String = when (repair.securityType) {
            "None"             -> "None"
            "Password"         -> "Password: ${repair.password.take(16)}"
            "Pattern"          -> "Pattern: ${repair.pattern.take(16)}"
            "Password & Pattern" -> "PWD+Pattern"
            else               -> repair.securityType
        }

        return buildString {
            // ── Header ──────────────────────────────────────────────────────
            appendLine("  ${storeInfo.storeName.uppercase().take(28)}")
            appendLine("  ${storeInfo.address.take(30)}")
            appendLine("  Call: ${storeInfo.phone.take(20)}")
            appendLine(LINE)

            // ── Invoice meta ─────────────────────────────────────────────────
            appendLine("INVOICE: ${repair.serialNumber}")
            appendLine("DATE   : $createdDate")
            appendLine("STATUS : ${repair.status.take(12).uppercase()}")
            appendLine(LINE)

            // ── Customer ─────────────────────────────────────────────────────
            appendLine("CUSTOMER INFO")
            appendLine("Name   : ${repair.customerName.take(20)}")
            appendLine("Phone  : ${repair.phoneNumber.take(15)}")
            appendLine(LINE)

            // ── Device ───────────────────────────────────────────────────────
            appendLine("DEVICE INFO")
            appendLine("Model  : ${repair.deviceModel.ifBlank { "-" }.take(20)}")
            appendLine("Problem: ${repair.problemDescription.ifBlank { "-" }.take(20)}")
            appendLine("Deliver: ${repair.expectedDeliveryDate.ifBlank { "-" }.take(16)}")
            appendLine(LINE)

            // ── Payment ──────────────────────────────────────────────────────
            if (repair.paymentInfo.isNotBlank()) {
                appendLine("PAYMENT INFO")
                appendLine("Info   : ${repair.paymentInfo.take(22)}")
                appendLine(LINE)
            }

            // ── Security ─────────────────────────────────────────────────────
            appendLine("SECURITY")
            appendLine("Lock   : $securityLine")
            appendLine(
                "Dead OK: ${if (repair.deadPhonePermission) "YES - APPROVED" else "No"}"
            )
            appendLine(LINE)

            // ── Accessories ──────────────────────────────────────────────────
            if (accessories.isNotEmpty()) {
                appendLine("ACCESSORIES")
                appendLine(accessories.joinToString(", "))
                appendLine(LINE)
            }

            // ── Box / Additional ─────────────────────────────────────────────
            if (repair.boxNumber.isNotBlank()) {
                appendLine("Box#   : ${repair.boxNumber.take(20)}")
            }
            if (repair.additionalDetails.isNotBlank()) {
                appendLine("Notes  : ${repair.additionalDetails.take(22)}")
            }
            if (repair.boxNumber.isNotBlank() || repair.additionalDetails.isNotBlank()) {
                appendLine(LINE)
            }

            // ── Footer ───────────────────────────────────────────────────────
            appendLine("   THANK YOU FOR CHOOSING US!")
            appendLine(" Bring this invoice for delivery")
            appendLine()
            appendLine()
            appendLine()
        }
    }
}