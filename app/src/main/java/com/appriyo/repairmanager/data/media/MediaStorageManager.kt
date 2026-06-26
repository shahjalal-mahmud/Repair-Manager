package com.appriyo.repairmanager.data.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Saves photos and videos into the device's *shared* storage
 * (Pictures/RepairManager and Movies/RepairManager), instead of app-private
 * storage.
 *
 * **Why shared storage:** files written here live in the actual Pictures/
 * Movies directories the Files app and Gallery can see. Because they are not
 * app-private, they are NOT deleted when this app is uninstalled or updated —
 * exactly the persistence behavior you asked for.
 */
class MediaStorageManager(private val context: Context) {

    private val resolver get() = context.contentResolver

    companion object {
        private const val ALBUM_NAME = "RepairManager"
    }

    /** Creates a new (pending) image entry and returns its Uri for camera capture. */
    fun createImageCaptureUri(draftId: String): Uri? {
        val name = fileName(draftId, "jpg")
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$ALBUM_NAME")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            } else {
                val albumDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    ALBUM_NAME
                ).apply { mkdirs() }
                put(MediaStore.Images.Media.DATA, File(albumDir, name).absolutePath)
            }
        }
        return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    /** Creates a new (pending) video entry and returns its Uri for camera capture. */
    fun createVideoCaptureUri(draftId: String): Uri? {
        val name = fileName(draftId, "mp4")
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/$ALBUM_NAME")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            } else {
                val albumDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    ALBUM_NAME
                ).apply { mkdirs() }
                put(MediaStore.Video.Media.DATA, File(albumDir, name).absolutePath)
            }
        }
        return resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
    }

    /** Marks a pending MediaStore entry (API 29+) as finished and visible. No-op below API 29. */
    fun finalize(uri: Uri, isVideo: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            runCatching { resolver.update(uri, values, null, null) }
        }
    }

    /**
     * Copies a Photo-Picker-selected item into our own MediaStore entry, so the
     * app holds a durable, independent copy rather than just a temporary
     * read grant on someone else's file.
     */
    fun importPickedMedia(sourceUri: Uri, isVideo: Boolean, draftId: String): Uri? {
        val destUri = (if (isVideo) createVideoCaptureUri(draftId) else createImageCaptureUri(draftId))
            ?: return null
        return try {
            resolver.openInputStream(sourceUri)?.use { input ->
                resolver.openOutputStream(destUri)?.use { output -> input.copyTo(output) }
            }
            finalize(destUri, isVideo)
            destUri
        } catch (e: Exception) {
            runCatching { resolver.delete(destUri, null, null) }
            null
        }
    }

    /** Permanently deletes a media file (used when the user removes an attachment, or on cancel). */
    fun delete(uri: Uri): Boolean =
        runCatching { resolver.delete(uri, null, null) > 0 }.getOrDefault(false)

    /**
     * Directory for short-lived camera-capture files, cleared by the OS under
     * storage pressure. Files here are deleted as soon as [commitCapturedMedia]
     * copies them into shared storage.
     */
    private val captureDir: File
        get() = File(context.cacheDir, "captures").apply { mkdirs() }

    /**
     * Creates an empty temp file for the camera to write into, exposed as a
     * FileProvider Uri.
     *
     * **Why not write directly into MediaStore for camera capture:** a
     * MediaStore entry created (and owned) by our app is not reliably
     * writable by a *different* app (the camera app) — many camera apps
     * fail or crash trying to write into it. A FileProvider Uri grants the
     * camera app a proper, temporary write permission, which is the
     * standard, reliable pattern for ACTION_IMAGE_CAPTURE / VIDEO_CAPTURE.
     */
    fun createTempCaptureFile(isVideo: Boolean, draftId: String): File {
        val ext = if (isVideo) "mp4" else "jpg"
        return File(captureDir, fileName(draftId, ext))
    }

    /** Wraps a temp capture file as a FileProvider Uri suitable for camera intents. */
    fun uriForCaptureFile(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /**
     * Called after a successful camera capture. Copies the temp file's bytes
     * into shared Pictures/Movies storage (so it survives uninstall/update,
     * same as gallery-picked media) and deletes the temp file.
     */
    fun commitCapturedMedia(file: File, isVideo: Boolean, draftId: String): Uri? {
        if (!file.exists()) return null
        val destUri = (if (isVideo) createVideoCaptureUri(draftId) else createImageCaptureUri(draftId))
            ?: return null
        return try {
            file.inputStream().use { input ->
                resolver.openOutputStream(destUri)?.use { output -> input.copyTo(output) }
            }
            finalize(destUri, isVideo)
            file.delete()
            destUri
        } catch (e: Exception) {
            runCatching { resolver.delete(destUri, null, null) }
            runCatching { file.delete() }
            null
        }
    }

    /** Discards a temp capture file when the user cancels out of the camera. */
    fun discardTempFile(file: File) {
        runCatching { file.delete() }
    }

    private fun fileName(draftId: String, ext: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US).format(Date())
        return "RM_${draftId}_$timestamp.$ext"
    }
}