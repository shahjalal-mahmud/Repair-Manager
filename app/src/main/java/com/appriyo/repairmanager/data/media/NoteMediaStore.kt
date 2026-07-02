package com.appriyo.repairmanager.data.media

import android.content.Context
import androidx.core.net.toUri
import org.json.JSONArray
import androidx.core.content.edit

/**
 * Persists which locally-stored photo attachments belong to which note.
 *
 * Notes themselves sync across devices via Firestore (see [com.appriyo.repairmanager.data.repository.NotesRepository]),
 * but the photo files backing a [MediaAttachment] live only in *this*
 * device's shared storage (see [MediaStorageManager]) — a Uri captured on
 * one phone is meaningless on another. So the noteId -> attachment Uris
 * mapping is kept in SharedPreferences on-device only, and is never written
 * to Firestore or otherwise synced.
 */
class NoteMediaStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** All photo attachments currently associated with [noteId], in the order they were added. */
    fun getAttachments(noteId: String): List<MediaAttachment> {
        if (noteId.isBlank()) return emptyList()
        val raw = prefs.getString(key(noteId), null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                runCatching { array.getString(i).toUri() }.getOrNull()
            }.map { MediaAttachment(it, MediaType.PHOTO) }
        }.getOrDefault(emptyList())
    }

    /** Overwrites the full set of attachments stored for [noteId]. */
    fun setAttachments(noteId: String, attachments: List<MediaAttachment>) {
        if (noteId.isBlank()) return
        if (attachments.isEmpty()) {
            removeNote(noteId)
            return
        }
        val array = JSONArray()
        attachments.forEach { array.put(it.uri.toString()) }
        prefs.edit { putString(key(noteId), array.toString()) }
    }

    /** Clears the stored attachment list for [noteId] (does not delete the underlying files). */
    fun removeNote(noteId: String) {
        if (noteId.isBlank()) return
        prefs.edit { remove(key(noteId)) }
    }

    private fun key(noteId: String) = "attachments_$noteId"

    companion object {
        private const val PREFS_NAME = "note_media_store"
    }
}