package com.appriyo.repairmanager.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * A single, read-only audit record stored at
 * users/{uid}/talikhata/{entryId}/history/{id}.
 *
 * Never editable or deletable by the user - written only by
 * TaliKhataRepository whenever an entry is created or its balance adjusted.
 */
data class TaliKhataHistoryEntry(
    @DocumentId
    var id: String = "",
    val operation: String = TaliKhataHistoryOperation.CREATED.firestoreValue,
    val amount: Double = 0.0,
    val balanceAfter: Double = 0.0,
    @ServerTimestamp
    val timestamp: Date? = null
) {
    val operationEnum: TaliKhataHistoryOperation
        get() = TaliKhataHistoryOperation.fromFirestoreValue(operation)
}