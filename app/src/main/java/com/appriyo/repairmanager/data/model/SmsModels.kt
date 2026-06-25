// app/src/main/java/com/appriyo/repairmanager/data/model/SmsModels.kt
package com.appriyo.repairmanager.data.model

import com.google.firebase.firestore.PropertyName
import java.util.Date

/**
 * Firestore document: appSettings/global
 * Single shared doc that designates which physical device is responsible
 * for sending SMS (Device B, the one with a SIM card).
 */
data class AppSettings(
    @get:PropertyName("smsSenderDeviceId") @set:PropertyName("smsSenderDeviceId")
    var smsSenderDeviceId: String = "",

    @get:PropertyName("smsSenderDeviceName") @set:PropertyName("smsSenderDeviceName")
    var smsSenderDeviceName: String = "",

    @get:PropertyName("simSlotIndex") @set:PropertyName("simSlotIndex")
    var simSlotIndex: Int = -1, // -1 = system default / automatic

    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Date? = null
)

/**
 * Firestore document: smsLogs/{repairId}_{status}
 * Existence of this document is the de-duplication mechanism - if it exists,
 * that status has already been texted for that repair and must never be sent again.
 */
data class SmsLog(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("repairId") @set:PropertyName("repairId")
    var repairId: String = "",

    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = "",

    @get:PropertyName("phoneNumber") @set:PropertyName("phoneNumber")
    var phoneNumber: String = "",

    @get:PropertyName("message") @set:PropertyName("message")
    var message: String = "",

    @get:PropertyName("sentAt") @set:PropertyName("sentAt")
    var sentAt: Date? = null,

    @get:PropertyName("sentByDeviceId") @set:PropertyName("sentByDeviceId")
    var sentByDeviceId: String = "",

    @get:PropertyName("success") @set:PropertyName("success")
    var success: Boolean = false
)

/**
 * Kept separate from FirestorePaths in Repair.kt so the SMS feature never requires
 * touching that existing, working file.
 */
object SmsFirestorePaths {
    const val APP_SETTINGS_COLLECTION = "appSettings"
    const val GLOBAL_SETTINGS_DOC = "global"
    const val SMS_LOGS_COLLECTION = "smsLogs"
}