// ============================================================================
// REPAIR DATA MODEL
// ============================================================================

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
 * **Important:** This class requires a no-argument constructor for Firestore's
 * automatic deserialization via `toObject<Repair>()`. All properties have default
 * values to satisfy this requirement.
 *
 * **Migration Note:** Fields `deviceName` and `problem` have been renamed to
 * `deviceModel` and `problemDescription` respectively to align with the Phase 1 spec.
 *
 * @property id Unique document identifier (mirrored from Firestore)
 * @property serialNumber Human-readable sequential identifier (format: RM-XXXXXX)
 * @property customerName Full name of the customer
 * @property phoneNumber Contact phone number
 * @property deviceModel Model of the device being repaired
 * @property problemDescription Description of the reported issue
 * @property expectedDeliveryDate Estimated completion date
 * @property paymentInfo Payment method or transaction details
 * @property additionalDetails Supplementary notes or instructions
 * @property boxNumber Storage box identifier
 * @property status Current repair status (see [RepairStatus])
 * @property securityType Lock screen security type (see [SecurityType])
 * @property password Device password (if applicable)
 * @property pattern Device pattern lock (if applicable)
 * @property batteryIncluded Whether battery was included with the device
 * @property simIncluded Whether SIM card was included
 * @property memoryCardIncluded Whether memory card was included
 * @property simTrayIncluded Whether SIM tray was included
 * @property backCoverIncluded Whether back cover was included
 * @property deadPhonePermission Permission to work on a dead/non-responsive device
 * @property photoCount Number of photos captured (future implementation)
 * @property videoCount Number of videos captured (future implementation)
 * @property createdAt Timestamp when the record was created (server timestamp)
 * @property updatedAt Timestamp when the record was last updated (server timestamp)
 * @property createdBy User identifier who created this record
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

    @get:PropertyName("deviceModel") @set:PropertyName("deviceModel")
    var deviceModel: String = "",

    @get:PropertyName("problemDescription") @set:PropertyName("problemDescription")
    var problemDescription: String = "",

    @get:PropertyName("expectedDeliveryDate") @set:PropertyName("expectedDeliveryDate")
    var expectedDeliveryDate: String = "",

    @get:PropertyName("paymentInfo") @set:PropertyName("paymentInfo")
    var paymentInfo: String = "",

    @get:PropertyName("additionalDetails") @set:PropertyName("additionalDetails")
    var additionalDetails: String = "",

    @get:PropertyName("boxNumber") @set:PropertyName("boxNumber")
    var boxNumber: String = "",

    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = RepairStatus.PENDING,

    @get:PropertyName("securityType") @set:PropertyName("securityType")
    var securityType: String = SecurityType.NONE,

    @get:PropertyName("password") @set:PropertyName("password")
    var password: String = "",

    @get:PropertyName("pattern") @set:PropertyName("pattern")
    var pattern: String = "",

    @get:PropertyName("batteryIncluded") @set:PropertyName("batteryIncluded")
    var batteryIncluded: Boolean = false,

    @get:PropertyName("simIncluded") @set:PropertyName("simIncluded")
    var simIncluded: Boolean = false,

    @get:PropertyName("memoryCardIncluded") @set:PropertyName("memoryCardIncluded")
    var memoryCardIncluded: Boolean = false,

    @get:PropertyName("simTrayIncluded") @set:PropertyName("simTrayIncluded")
    var simTrayIncluded: Boolean = false,

    @get:PropertyName("backCoverIncluded") @set:PropertyName("backCoverIncluded")
    var backCoverIncluded: Boolean = false,

    @get:PropertyName("deadPhonePermission") @set:PropertyName("deadPhonePermission")
    var deadPhonePermission: Boolean = false,

    @get:PropertyName("photoCount") @set:PropertyName("photoCount")
    var photoCount: Int = 0,

    @get:PropertyName("videoCount") @set:PropertyName("videoCount")
    var videoCount: Int = 0,

    @ServerTimestamp
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Date? = null,

    @ServerTimestamp
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Date? = null,

    @get:PropertyName("createdBy") @set:PropertyName("createdBy")
    var createdBy: String = ""
)

// ============================================================================
// CONSTANTS & ENUMS
// ============================================================================

/**
 * Defines the possible statuses for a repair record.
 *
 * **Status Lifecycle:**
 * PENDING → IN_PROGRESS → COMPLETED → DELIVERED
 *                      ↘ CANCELLED
 */
object RepairStatus {
    const val PENDING = "Pending"
    const val IN_PROGRESS = "In Progress"
    const val COMPLETED = "Completed"
    const val DELIVERED = "Delivered"
    const val CANCELLED = "Cancelled"

    val ALL = listOf(PENDING, IN_PROGRESS, COMPLETED, DELIVERED, CANCELLED)
}

/**
 * Defines the possible security types for a locked device.
 */
object SecurityType {
    const val NONE = "None"
    const val PASSWORD = "Password"
    const val PATTERN = "Pattern"
    const val BOTH = "Password & Pattern"

    val ALL = listOf(NONE, PASSWORD, PATTERN, BOTH)
}

/**
 * Firestore collection and document path constants.
 */
object FirestorePaths {
    const val REPAIRS_COLLECTION = "repairs"
    const val COUNTERS_COLLECTION = "counters"
    const val REPAIR_COUNTER_DOC = "repairCounter"
    const val LAST_SERIAL_FIELD = "lastSerial"
}