package com.appriyo.repairmanager.data.model

/**
 * Direction of a TaliKhata ledger entry.
 *
 * YOU_OWE        - the shop owner owes [personName] money.
 * THEY_OWE_YOU   - [personName] owes the shop owner money.
 *
 * Stored in Firestore as [firestoreValue] - never as a raw string elsewhere
 * in the app.
 */
enum class TaliKhataType(val firestoreValue: String, val label: String) {
    YOU_OWE("YOU_OWE", "You Owe"),
    THEY_OWE_YOU("THEY_OWE_YOU", "Owes You");

    companion object {
        fun fromFirestoreValue(value: String?): TaliKhataType =
            entries.firstOrNull { it.firestoreValue == value } ?: THEY_OWE_YOU
    }
}