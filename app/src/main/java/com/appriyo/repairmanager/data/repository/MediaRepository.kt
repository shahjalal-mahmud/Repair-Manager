// app/src/main/java/com/appriyo/repairmanager/data/media/MediaRepository.kt
package com.appriyo.repairmanager.data.media

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Queries the device's MediaStore for all photos and videos that belong to a
 * given repair, identified by its [draftId].
 *
 * Files are named  RM_{draftId}_{timestamp}.jpg / .mp4  and live in
 * Pictures/RepairManager  and  Movies/RepairManager  respectively.
 */
class MediaRepository(private val context: Context) {

    /**
     * Returns all [MediaAttachment]s for the given [draftId], photos first
     * then videos, each sorted oldest-first within their group.
     */
    suspend fun loadAttachments(draftId: String): List<MediaAttachment> =
        withContext(Dispatchers.IO) {
            val photos = queryMedia(
                collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                else
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                namePrefix = "RM_${draftId}_",
                type = MediaType.PHOTO
            )
            val videos = queryMedia(
                collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                else
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                namePrefix = "RM_${draftId}_",
                type = MediaType.VIDEO
            )
            photos + videos
        }

    /**
     * Returns only the very first photo for [draftId], or null if none exist.
     * Used by the list card to show a single preview thumbnail.
     */
    suspend fun loadFirstPhoto(draftId: String): MediaAttachment? =
        withContext(Dispatchers.IO) {
            queryMedia(
                collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                else
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                namePrefix = "RM_${draftId}_",
                type = MediaType.PHOTO,
                limit = 1
            ).firstOrNull()
        }

    private fun queryMedia(
        collection: Uri,
        namePrefix: String,
        type: MediaType,
        limit: Int = Int.MAX_VALUE
    ): List<MediaAttachment> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_ADDED
        )
        // DISPLAY_NAME LIKE 'RM_{draftId}_%'
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("${namePrefix}%")
        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} ASC"

        val results = mutableListOf<MediaAttachment>()
        context.contentResolver.query(
            collection, projection, selection, selectionArgs, sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            while (cursor.moveToNext() && results.size < limit) {
                val id = cursor.getLong(idCol)
                val uri = Uri.withAppendedPath(collection, id.toString())
                results += MediaAttachment(uri, type)
            }
        }
        return results
    }
}