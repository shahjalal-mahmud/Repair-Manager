package com.appriyo.repairmanager.data.model

/**
 * The category a [Note] belongs to. Notes are split into three buckets so a
 * user can keep different kinds of information organized:
 *
 * - [GENERAL]    — the default / catch-all bucket (regular notes).
 * - [REMINDER]   — things to come back to later ("call X tomorrow", etc.).
 * - [IMPORTANT]  — high-signal reference info worth keeping (prices, contacts, snippets).
 *
 * The enum is intentionally a closed set; if a fourth kind is ever needed,
 * add it here AND in the UI tab row (see `NoteCategoryTabs`).
 */
enum class NoteCategory(
    val storageValue: String,
    val displayLabel: String
) {
    GENERAL("GENERAL", "General"),
    REMINDER("REMINDER", "Reminder"),
    IMPORTANT("IMPORTANT", "Important");

    companion object {
        /**
         * Resolve a value persisted in Firestore to its enum counterpart.
         * Old documents written before this field existed — or ones with
         * an unknown / corrupt value — fall back to [GENERAL] so the UI
         * always has a valid category to display.
         */
        fun fromStorageValue(value: String?): NoteCategory =
            entries.firstOrNull { it.storageValue == value } ?: GENERAL
    }
}
