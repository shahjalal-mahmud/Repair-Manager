package com.appriyo.repairmanager.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Firestore-compatible data class representing a general business note.
 *
 * Collection: "notes"
 * Document ID: Firestore auto-generated ID (mirrored into [id] for convenience)
 */
data class Note(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",

    @get:PropertyName("description") @set:PropertyName("description")
    var description: String = "",

    @ServerTimestamp
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Date? = null,

    @ServerTimestamp
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Date? = null,

    @get:PropertyName("createdBy") @set:PropertyName("createdBy")
    var createdBy: String = "",

    /**
     * Stored category value (kept as a primitive so Firestore's POJO
     * deserialization works without surprises). Use [categoryEnum] when
     * reading and convert back via [storageValue] when writing.
     * Old documents without this field default to [NoteCategory.GENERAL].
     */
    @get:PropertyName("category") @set:PropertyName("category")
    var category: String = NoteCategory.GENERAL.storageValue
) {
    /** Typed view of [category] so call sites don't have to call [NoteCategory.fromStorageValue] themselves. */
    val categoryEnum: NoteCategory get() = NoteCategory.fromStorageValue(category)
}

/**
 * Firestore collection path constant for the Notes module.
 * Kept separate from FirestorePaths (in Repair.kt) so the existing Repair
 * model file does not need to be touched at all.
 */
object NotesFirestorePaths {
    const val NOTES_COLLECTION = "notes"
}