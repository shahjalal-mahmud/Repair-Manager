// app/src/main/java/com/appriyo/repairmanager/data/model/Repair.kt
package com.appriyo.repairmanager.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Firestore-compatible data class representing a single repair record.
 *
 * Collection: "repairs"
 * Document ID: Firestore auto-generated ID (mirrored into [id] for convenience)
 *
 * IMPORTANT: This class requires a no-argument constructor for Firestore's
 * automatic deserialization (toObject<Repair>()). All properties have default
 * values, which satisfies that requirement.
 */
data class Repair(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("serialNumber") @set:PropertyName("serialNumber")
    var serialNumber: String = "",

    @get:PropertyName("customerName") @set:PropertyName("customerName")
    var customerName: String = "",

    @get:PropertyName("phoneNumber") @set:PropertyName("phoneNumber")
    var phoneNumber: String = "",

    @get:PropertyName("deviceName") @set:PropertyName("deviceName")
    var deviceName: String = "",

    @get:PropertyName("problem") @set:PropertyName("problem")
    var problem: String = "",

    @get:PropertyName("expectedDeliveryDate") @set:PropertyName("expectedDeliveryDate")
    var expectedDeliveryDate: String = "",

    @get:PropertyName("paymentInfo") @set:PropertyName("paymentInfo")
    var paymentInfo: String = "",

    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = RepairStatus.PENDING,

    @ServerTimestamp
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Date? = null,

    @ServerTimestamp
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Date? = null,

    @get:PropertyName("createdBy") @set:PropertyName("createdBy")
    var createdBy: String = ""
)

/**
 * Simple string-based status constants (kept intentionally simple,
 * no enums stored in Firestore to avoid serialization edge cases).
 */
object RepairStatus {
    const val PENDING = "Pending"
    const val IN_PROGRESS = "In Progress"
    const val COMPLETED = "Completed"
    const val DELIVERED = "Delivered"
    const val CANCELLED = "Cancelled"
}

/**
 * Firestore collection / document path constants used across the data layer.
 */
object FirestorePaths {
    const val REPAIRS_COLLECTION = "repairs"
    const val COUNTERS_COLLECTION = "counters"
    const val REPAIR_COUNTER_DOC = "repairCounter"
    const val LAST_SERIAL_FIELD = "lastSerial"
}