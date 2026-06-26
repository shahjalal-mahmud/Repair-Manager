// app/src/main/java/com/appriyo/repairmanager/data/model/Repair.kt
package com.appriyo.repairmanager.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

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

    @get:PropertyName("draftId") @set:PropertyName("draftId")
    var draftId: String = "",

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

object RepairStatus {
    const val PENDING = "Pending"
    const val IN_PROGRESS = "In Progress"
    const val COMPLETED = "Completed"
    const val DELIVERED = "Delivered"
    const val CANCELLED = "Cancelled"

    val ALL = listOf(PENDING, IN_PROGRESS, COMPLETED, DELIVERED, CANCELLED)
}

object SecurityType {
    const val NONE = "None"
    const val PASSWORD = "Password"
    const val PATTERN = "Pattern"
    const val BOTH = "Password & Pattern"

    val ALL = listOf(NONE, PASSWORD, PATTERN, BOTH)
}

object FirestorePaths {
    const val REPAIRS_COLLECTION = "repairs"
    const val COUNTERS_COLLECTION = "counters"
    const val REPAIR_COUNTER_DOC = "repairCounter"
    const val LAST_SERIAL_FIELD = "lastSerial"
}