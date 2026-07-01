// app/src/main/java/com/appriyo/repairmanager/data/model/WorkerType.kt
package com.appriyo.repairmanager.data.model

/**
 * The shop currently has exactly two workers who can be credited with a job:
 * A - Shop Owner, B - Employee. This is intentionally a closed set, not a
 * generic "employee" system.
 */
enum class WorkerType(val storageValue: String, val displayLabel: String) {
    A("A", "A"),
    B("B", "B");

    companion object {
        /** Old Firestore documents written before this field existed default to A. */
        fun fromStorageValue(value: String?): WorkerType =
            WorkerType.entries.firstOrNull { it.storageValue == value } ?: A
    }
}