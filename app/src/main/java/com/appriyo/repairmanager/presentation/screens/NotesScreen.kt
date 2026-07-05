package com.appriyo.repairmanager.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.appriyo.repairmanager.data.media.MediaAttachment
import com.appriyo.repairmanager.data.media.MediaStorageManager
import com.appriyo.repairmanager.data.media.MediaType
import com.appriyo.repairmanager.data.media.NoteMediaStore
import com.appriyo.repairmanager.data.media.loadMediaThumbnail
import com.appriyo.repairmanager.data.model.Note
import com.appriyo.repairmanager.data.model.NoteCategory
import com.appriyo.repairmanager.presentation.components.CategorySelector
import com.appriyo.repairmanager.presentation.components.DeleteConfirmationDialog
import com.appriyo.repairmanager.presentation.components.NoteCategoryBadge
import com.appriyo.repairmanager.presentation.components.NoteCategoryTabsWithCounts
import com.appriyo.repairmanager.presentation.components.TopToastHost
import com.appriyo.repairmanager.presentation.viewmodel.NotesViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.io.File

private const val MAX_NOTE_PHOTOS = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: NotesViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeError()
        }
    }

    // Search is always global across all categories; the tab only narrows the
    // displayed list when the search box is empty.
    val searchedNotes = remember(uiState.notes, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) {
            uiState.notes
        } else {
            uiState.notes.filter {
                it.title.contains(uiState.searchQuery, ignoreCase = true) ||
                        it.description.contains(uiState.searchQuery, ignoreCase = true)
            }
        }
    }

    val displayedNotes = remember(
        searchedNotes,
        uiState.selectedTab,
        uiState.searchQuery
    ) {
        if (uiState.searchQuery.isNotBlank()) searchedNotes
        else searchedNotes.filter { it.categoryEnum == uiState.selectedTab }
    }

    // Counts come from the raw list so the tab badges stay accurate while the user is searching.
    val tabCounts = remember(uiState.notes) {
        val byCategory = uiState.notes.groupingBy { it.categoryEnum }.eachCount()
        NoteCategory.entries.associateWith { byCategory[it] ?: 0 }
    }

    Scaffold(
        topBar = {
            NotesTopBar(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                selectedTab = uiState.selectedTab,
                tabCounts = tabCounts,
                onTabSelected = { viewModel.onTabSelected(it) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openAddDialog() },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Note") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> LoadingState()
                displayedNotes.isEmpty() -> EmptyNotesState(
                    isSearching = uiState.searchQuery.isNotBlank(),
                    activeTab = uiState.selectedTab
                )
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
                    ) {
                        items(displayedNotes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                mediaVersion = uiState.mediaRefreshTick,
                                onEdit = { viewModel.openEditDialog(note) },
                                onDelete = { viewModel.requestDeleteNote(note) }
                            )
                        }
                    }
                }
            }
        }
    }

    TopToastHost(
        toast = uiState.toast,
        onConsumed = { viewModel.consumeToast() },
        modifier = Modifier
    )

    if (uiState.isDialogOpen) {
        NoteEditDialog(
            initialTitle = uiState.editingNote?.title ?: "",
            initialDescription = uiState.editingNote?.description ?: "",
            category = uiState.draftCategory,
            attachments = uiState.editingAttachments,
            draftId = uiState.currentDraftId,
            isSaving = uiState.isSaving,
            titleError = uiState.titleError,
            isEditing = uiState.editingNote != null,
            onCategoryChange = { viewModel.onDraftCategoryChange(it) },
            onAddAttachment = { viewModel.addMediaAttachment(it) },
            onRemoveAttachment = { viewModel.removeMediaAttachment(it) },
            onDismiss = { viewModel.dismissDialog() },
            onSave = { title, description, category ->
                viewModel.saveNote(title, description, category)
            }
        )
    }

    uiState.noteToDelete?.let { note ->
        DeleteConfirmationDialog(
            title = "Delete note?",
            message = "\"${note.title}\" will be permanently removed. This can't be undone.",
            isDeleting = uiState.isDeleting,
            onConfirm = { viewModel.confirmDeleteNote() },
            onDismiss = { viewModel.dismissDeleteConfirmation() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedTab: NoteCategory,
    tabCounts: Map<NoteCategory, Int>,
    onTabSelected: (NoteCategory) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Notes",
                    fontWeight = FontWeight.SemiBold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        NotesSearchField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        NoteCategoryTabsWithCounts(
            selected = selectedTab,
            counts = tabCounts,
            onSelect = onTabSelected
        )
    }
}

@Composable
private fun NotesSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Search all notes") },
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(strokeWidth = 3.dp)
    }
}

/** Pair of (title, subtitle) strings for the empty state — single source of truth per branch. */
private data class EmptyCopy(val title: String, val subtitle: String)

@Composable
private fun EmptyNotesState(
    isSearching: Boolean,
    activeTab: NoteCategory
) {
    val copy = when {
        isSearching -> EmptyCopy(
            title = "No matching notes",
            subtitle = "Try a different search term — results can come from any category"
        )
        activeTab == NoteCategory.GENERAL -> EmptyCopy(
            title = "No general notes yet",
            subtitle = "Tap + to add your first note"
        )
        activeTab == NoteCategory.REMINDER -> EmptyCopy(
            title = "No reminders yet",
            subtitle = "Tap + to set a reminder for later"
        )
        else -> EmptyCopy(
            title = "No important notes saved",
            subtitle = "Tap + to save something worth keeping"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSearching) Icons.Filled.SearchOff else Icons.Filled.EditNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = copy.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = copy.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NoteCard(
    note: Note,
    mediaVersion: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val mediaStore = remember { NoteMediaStore(context) }
    var attachments by remember(note.id, mediaVersion) { mutableStateOf<List<MediaAttachment>>(emptyList()) }

    LaunchedEffect(note.id, mediaVersion) {
        attachments = mediaStore.getAttachments(note.id)
    }

    Card(
        onClick = onEdit,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (attachments.isNotEmpty()) {
                NoteThumbnailBadge(attachment = attachments.first(), extraCount = attachments.size - 1)
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = note.title.trim().take(1).ifBlank { "N" }.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                NoteCategoryBadge(category = note.categoryEnum)

                if (note.description.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = note.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(4.dp))

            Column {
                IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edit note",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.height(2.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete note",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

/** Small thumbnail shown on a note card when the note has one or more attached photos. */
@Composable
private fun NoteThumbnailBadge(attachment: MediaAttachment, extraCount: Int) {
    val context = LocalContext.current
    var thumbnail by remember(attachment.uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(attachment.uri) {
        thumbnail = loadMediaThumbnail(context, attachment.uri)
    }

    Box(modifier = Modifier.size(44.dp)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val bmp = thumbnail
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            }
        }
        if (extraCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clip(RoundedCornerShape(topStart = 6.dp, bottomEnd = 10.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "+$extraCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

/** Small section label used inside the Add/Edit dialog to give each form group a clear heading. */
@Composable
private fun FormSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditDialog(
    initialTitle: String,
    initialDescription: String,
    category: NoteCategory,
    attachments: List<MediaAttachment>,
    draftId: String,
    isSaving: Boolean,
    titleError: String?,
    isEditing: Boolean,
    onCategoryChange: (NoteCategory) -> Unit,
    onAddAttachment: (MediaAttachment) -> Unit,
    onRemoveAttachment: (MediaAttachment) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, String, NoteCategory) -> Unit
) {
    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    var description by remember(initialDescription) { mutableStateOf(initialDescription) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = if (isEditing) "Edit Note" else "New Note",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 540.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FormSectionLabel(text = "Category")
                CategorySelector(
                    selected = category,
                    onChange = onCategoryChange,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )

                FormSectionLabel(text = "Details")
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    isError = titleError != null,
                    supportingText = { titleError?.let { Text(it) } },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                FormSectionLabel(text = "Attachments")
                NotePhotosSection(
                    attachments = attachments,
                    draftId = draftId,
                    enabled = !isSaving,
                    onAdd = onAddAttachment,
                    onRemove = onRemoveAttachment
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                shape = RoundedCornerShape(12.dp),
                onClick = { onSave(title, description, category) }
            ) {
                Text(if (isSaving) "Saving..." else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Photo attach section for the note dialog: take a photo or pick from gallery, up to
 * [maxPhotos]. Photos only — no video, per the Notes feature's requirements. Files are
 * written to shared device storage via [MediaStorageManager] (same mechanism used for
 * repair attachments), never uploaded anywhere.
 */
@Composable
private fun NotePhotosSection(
    attachments: List<MediaAttachment>,
    draftId: String,
    enabled: Boolean,
    maxPhotos: Int = MAX_NOTE_PHOTOS,
    onAdd: (MediaAttachment) -> Unit,
    onRemove: (MediaAttachment) -> Unit
) {
    val context = LocalContext.current
    val manager = remember { MediaStorageManager(context) }
    val coroutineScope = rememberCoroutineScope()

    val photoCount = attachments.size
    val photosFull = photoCount >= maxPhotos

    var pendingCapturePath by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPermissionAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val path = pendingCapturePath
        val file = path?.let { File(it) }
        if (success && file != null) {
            val savedUri = manager.commitCapturedMedia(file, isVideo = false, draftId = draftId)
            if (savedUri != null) {
                onAdd(MediaAttachment(savedUri, MediaType.PHOTO))
            } else {
                toast("Could not save the photo. Please try again.")
            }
        } else if (file != null) {
            manager.discardTempFile(file)
        }
        pendingCapturePath = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            pendingPermissionAction?.invoke()
        } else {
            toast("Camera permission is required to take photos.")
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

    fun launchCameraForPhoto() {
        val file = manager.createTempCaptureFile(isVideo = false, draftId = draftId)
        val uri = runCatching { manager.uriForCaptureFile(file) }.getOrNull()
        if (uri == null) { toast("Could not prepare the camera."); return }
        pendingCapturePath = file.absolutePath
        takePictureLauncher.launch(uri)
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
            text = "Photos $photoCount/$maxPhotos",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        if (attachments.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(attachments, key = { it.uri.toString() }) { item ->
                    NotePhotoThumbnail(attachment = item, enabled = enabled, onRemove = { onRemove(item) })
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Box {
            OutlinedButton(
                onClick = { showMenu = true },
                enabled = enabled && !photosFull,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (photosFull) "Photo limit reached" else "Add Photo")
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Take Photo") },
                    leadingIcon = { Icon(Icons.Filled.AddAPhoto, contentDescription = null) },
                    onClick = { showMenu = false; requestCameraThen { launchCameraForPhoto() } }
                )
                DropdownMenuItem(
                    text = { Text("Choose from Gallery") },
                    leadingIcon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        pickImagesLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = "Stored privately on this device. Not uploaded.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NotePhotoThumbnail(
    attachment: MediaAttachment,
    enabled: Boolean,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    var thumbnail by remember(attachment.uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(attachment.uri) {
        thumbnail = loadMediaThumbnail(context, attachment.uri)
    }

    Box(modifier = Modifier.size(64.dp)) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val bmp = thumbnail
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp))
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            }
        }

        IconButton(
            onClick = onRemove,
            enabled = enabled,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(20.dp)
                .background(MaterialTheme.colorScheme.error, CircleShape)
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.onError,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}