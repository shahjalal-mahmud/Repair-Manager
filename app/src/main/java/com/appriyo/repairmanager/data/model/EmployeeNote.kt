// app/src/main/java/com/appriyo/repairmanager/data/model/EmployeeNote.kt
package com.appriyo.repairmanager.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Firestore-compatible data class representing a manual repair-job/profit note.
 *
 * NOTE: This is NOT employee management. There is no employee account,
 * authentication, attendance, salary, or role system involved - it is simply
 * a notebook entry recording a job, its total payment, and its profit.
 *
 * Collection: "employeeNotes"
 * Document ID: Firestore auto-generated ID (mirrored into [id] for convenience)
 */
data class EmployeeNote(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",

    @get:PropertyName("description") @set:PropertyName("description")
    var description: String = "",

    @get:PropertyName("totalPayment") @set:PropertyName("totalPayment")
    var totalPayment: Double = 0.0,

    @get:PropertyName("profit") @set:PropertyName("profit")
    var profit: Double = 0.0,

    @ServerTimestamp
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Date? = null,

    @ServerTimestamp
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Date? = null,

    @get:PropertyName("createdBy") @set:PropertyName("createdBy")
    var createdBy: String = ""
)

object EmployeeNotesFirestorePaths {
    const val EMPLOYEE_NOTES_COLLECTION = "employeeNotes"
}