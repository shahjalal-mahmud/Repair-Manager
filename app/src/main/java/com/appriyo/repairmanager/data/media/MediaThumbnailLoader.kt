package com.appriyo.repairmanager.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val THUMBNAIL_SIZE = 220

/** Loads a small preview bitmap for a photo or video Uri created by [MediaStorageManager]. */
suspend fun loadMediaThumbnail(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.loadThumbnail(uri, Size(THUMBNAIL_SIZE, THUMBNAIL_SIZE), null)
        } else {
            legacyThumbnail(context, uri)
        }
    }.getOrNull()
}

private fun legacyThumbnail(context: Context, uri: Uri): Bitmap? {
    val path = legacyFilePath(context, uri) ?: return null
    val mimeType = context.contentResolver.getType(uri).orEmpty()
    return if (mimeType.startsWith("video")) {
        @Suppress("DEPRECATION")
        ThumbnailUtils.createVideoThumbnail(path, MediaStore.Images.Thumbnails.MICRO_KIND)
    } else {
        val options = BitmapFactory.Options().apply { inSampleSize = 4 }
        BitmapFactory.decodeFile(path, options)
    }
}

private fun legacyFilePath(context: Context, uri: Uri): String? {
    val projection = arrayOf(MediaStore.MediaColumns.DATA)
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        val column = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
        if (cursor.moveToFirst()) return cursor.getString(column)
    }
    return null
}