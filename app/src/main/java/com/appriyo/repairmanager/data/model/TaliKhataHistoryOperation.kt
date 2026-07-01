package com.appriyo.repairmanager.data.model

/**
 * The kind of auditable event recorded in
 * users/{uid}/talikhata/{entryId}/history.
 */
enum class TaliKhataHistoryOperation(val firestoreValue: String, val label: String) {
    CREATED("CREATED", "Created"),
    INCREASE("INCREASE", "Added"),
    DECREASE("DECREASE", "Paid");

    companion object {
        fun fromFirestoreValue(value: String?): TaliKhataHistoryOperation =
            entries.firstOrNull { it.firestoreValue == value } ?: CREATED
    }
}