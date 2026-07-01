// app/src/main/java/com/appriyo/repairmanager/data/model/EmployeeNote.kt
package com.appriyo.repairmanager.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Firestore-compatible data class representing a manual repair-job/profit note
 * that powers the Daily Work Ledger.
 *
 * NOTE: This is NOT employee management. There is no employee account,
 * authentication, attendance, salary, or role system involved - it is simply
 * a ledger entry recording a job, who did it (A = Shop Owner, B = Employee),
 * its total payment, and its profit.
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

    /**
     * "A" (Shop Owner) or "B" (Employee). Never null/empty - mandatory on every
     * entry. Existing documents written before this field existed will simply
     * be missing it in Firestore, which Firestore's POJO mapping leaves at
     * this default ("A") rather than overwriting it - so old data keeps working
     * without any migration.
     */
    @get:PropertyName("workerType") @set:PropertyName("workerType")
    var workerType: String = WorkerType.A.storageValue,

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