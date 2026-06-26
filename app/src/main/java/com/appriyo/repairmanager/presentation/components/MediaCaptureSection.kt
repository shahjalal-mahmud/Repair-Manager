package com.appriyo.repairmanager.presentation.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.appriyo.repairmanager.data.media.MediaAttachment
import com.appriyo.repairmanager.data.media.MediaStorageManager
import com.appriyo.repairmanager.data.media.MediaType
import com.appriyo.repairmanager.data.media.loadMediaThumbnail
import kotlinx.coroutines.launch

/**
 * Lets the user attach photos and videos to a repair via camera or gallery.
 *
 * Files are written to shared device storage (see [MediaStorageManager]), so
 * they survive an app uninstall or update. Nothing here is uploaded anywhere.
 */
@Composable
fun MediaCaptureSection(
    attachments: List<MediaAttachment>,
    onAdd: (MediaAttachment) -> Unit,
    onRemove: (MediaAttachment) -> Unit,
    draftId: String,
    enabled: Boolean = true,
    maxPhotos: Int = 10,
    maxVideos: Int = 3
) {
    val context = LocalContext.current
    val manager = remember { MediaStorageManager(context) }
    val coroutineScope = rememberCoroutineScope()

    val photoCount = attachments.count { it.type == MediaType.PHOTO }
    val videoCount = attachments.count { it.type == MediaType.VIDEO }
    val photosFull = photoCount >= maxPhotos
    val videosFull = videoCount >= maxVideos

    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPermissionAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showPhotoMenu by remember { mutableStateOf(false) }
    var showVideoMenu by remember { mutableStateOf(false) }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCaptureUri
        if (success && uri != null) {
            manager.finalize(uri, isVideo = false)
            onAdd(MediaAttachment(uri, MediaType.PHOTO))
        } else if (uri != null) {
            manager.delete(uri)
        }
        pendingCaptureUri = null
    }

    val captureVideoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        val uri = pendingCaptureUri
        if (success && uri != null) {
            manager.finalize(uri, isVideo = true)
            onAdd(MediaAttachment(uri, MediaType.VIDEO))
        } else if (uri != null) {
            manager.delete(uri)
        }
        pendingCaptureUri = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            pendingPermissionAction?.invoke()
        } else {
            toast("Camera permission is required to take photos or videos.")
        }
        pendingPermissionAction = null
    }

    val pickImagesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val remaining = maxPhotos - photoCount
        if (remaining <= 0) {
            toast("You can attach up to $maxPhotos photos.")
            return@rememberLauncherForActivityResult
        }
        val toImport = uris.take(remaining)
        if (uris.size > remaining) toast("Only $remaining more photo(s) could be added (max $maxPhotos).")
        coroutineScope.launch {
            toImport.forEach { source ->
                manager.importPickedMedia(source, isVideo = false, draftId = draftId)
                    ?.let { onAdd(MediaAttachment(it, MediaType.PHOTO)) }
            }
        }
    }

    val pickVideosLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val remaining = maxVideos - videoCount
        if (remaining <= 0) {
            toast("You can attach up to $maxVideos videos.")
            return@rememberLauncherForActivityResult
        }
        val toImport = uris.take(remaining)
        if (uris.size > remaining) toast("Only $remaining more video(s) could be added (max $maxVideos).")
        coroutineScope.launch {
            toImport.forEach { source ->
                manager.importPickedMedia(source, isVideo = true, draftId = draftId)
                    ?.let { onAdd(MediaAttachment(it, MediaType.VIDEO)) }
            }
        }
    }

    fun launchCameraForPhoto() {
        val uri = manager.createImageCaptureUri(draftId)
        if (uri == null) { toast("Could not prepare storage for the photo."); return }
        pendingCaptureUri = uri
        takePictureLauncher.launch(uri)
    }

    fun launchCameraForVideo() {
        val uri = manager.createVideoCaptureUri(draftId)
        if (uri == null) { toast("Could not prepare storage for the video."); return }
        pendingCaptureUri = uri
        captureVideoLauncher.launch(uri)
    }

    fun requestCameraThen(action: () -> Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        if (granted) action() else {
            pendingPermissionAction = action
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Photos $photoCount/$maxPhotos  •  Videos $videoCount/$maxVideos",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (attachments.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(attachments, key = { it.uri.toString() }) { item ->
                    MediaThumbnailTile(attachment = item, enabled = enabled, onRemove = { onRemove(item) })
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { showPhotoMenu = true },
                    enabled = enabled && !photosFull,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (photosFull) "Photo limit reached" else "Add Photo")
                }
                DropdownMenu(expanded = showPhotoMenu, onDismissRequest = { showPhotoMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Take Photo") },
                        leadingIcon = { Icon(Icons.Filled.AddAPhoto, contentDescription = null) },
                        onClick = { showPhotoMenu = false; requestCameraThen { launchCameraForPhoto() } }
                    )
                    DropdownMenuItem(
                        text = { Text("Choose from Gallery") },
                        leadingIcon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                        onClick = {
                            showPhotoMenu = false
                            pickImagesLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { showVideoMenu = true },
                    enabled = enabled && !videosFull,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (videosFull) "Video limit reached" else "Add Video")
                }
                DropdownMenu(expanded = showVideoMenu, onDismissRequest = { showVideoMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Record Video") },
                        leadingIcon = { Icon(Icons.Filled.Videocam, contentDescription = null) },
                        onClick = { showVideoMenu = false; requestCameraThen { launchCameraForVideo() } }
                    )
                    DropdownMenuItem(
                        text = { Text("Choose from Gallery") },
                        leadingIcon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                        onClick = {
                            showVideoMenu = false
                            pickVideosLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Stored privately on this device. Not uploaded — safe even if you uninstall or update the app.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MediaThumbnailTile(
    attachment: MediaAttachment,
    enabled: Boolean,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    var thumbnail by remember(attachment.uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(attachment.uri) {
        thumbnail = loadMediaThumbnail(context, attachment.uri)
    }

    Box(modifier = Modifier.size(84.dp)) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val bmp = thumbnail
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(84.dp).clip(RoundedCornerShape(12.dp))
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }

            if (attachment.type == MediaType.VIDEO) {
                Icon(
                    Icons.Filled.PlayCircle,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        IconButton(
            onClick = onRemove,
            enabled = enabled,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(22.dp)
                .background(MaterialTheme.colorScheme.error, CircleShape)
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.onError,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}