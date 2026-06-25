// app/src/main/java/com/appriyo/repairmanager/data/sms/SmsTemplateProvider.kt
package com.appriyo.repairmanager.data.sms

import com.appriyo.repairmanager.data.model.RepairStatus

/**
 * Centralized, easy-to-edit SMS copy. To change wording later, edit only this file.
 */
object SmsTemplateProvider {

    private val templates: Map<String, String> = mapOf(
        RepairStatus.PENDING to
                "Dear {customerName}, your device has been received successfully. Repair ID: {serialNumber}.",
        RepairStatus.IN_PROGRESS to
                "Dear {customerName}, your repair is currently in progress. Repair ID: {serialNumber}.",
        RepairStatus.COMPLETED to
                "Dear {customerName}, your device repair has been completed and is ready for delivery. Repair ID: {serialNumber}.",
        RepairStatus.DELIVERED to
                "Dear {customerName}, thank you for choosing our service. Repair ID: {serialNumber}.",
        RepairStatus.CANCELLED to
                "Dear {customerName}, your repair request has been cancelled. Repair ID: {serialNumber}."
    )

    fun getMessage(status: String, customerName: String, serialNumber: String): String {
        val template = templates[status]
            ?: "Dear {customerName}, your repair status has been updated to $status. Repair ID: {serialNumber}."
        return template
            .replace("{customerName}", customerName.ifBlank { "Customer" })
            .replace("{serialNumber}", serialNumber)
    }
}