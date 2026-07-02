// app/src/main/java/com/appriyo/repairmanager/presentation/viewmodel/NotesViewModel.kt
package com.appriyo.repairmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.repairmanager.data.media.MediaAttachment
import com.appriyo.repairmanager.data.media.MediaStorageManager
import com.appriyo.repairmanager.data.media.NoteMediaStore
import com.appriyo.repairmanager.data.model.Note
import com.appriyo.repairmanager.data.repository.AuthRepository
import com.appriyo.repairmanager.data.repository.NotesRepository
import com.appriyo.repairmanager.presentation.components.ToastMessage
import com.appriyo.repairmanager.presentation.components.ToastType
import com.appriyo.repairmanager.presentation.state.NotesUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.UUID

class NotesViewModel(
    private val notesRepository: NotesRepository,
    private val authRepository: AuthRepository,
    private val noteMediaStore: NoteMediaStore,
    private val mediaStorageManager: MediaStorageManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState = _uiState.asStateFlow()

    /** Attachments present when the dialog was opened. Used on cancel to delete any
     *  photo files the user captured/picked during this session but never saved. */
    private var attachmentsSnapshotOnOpen: List<MediaAttachment> = emptyList()

    init {
        observeNotes()
    }

    private fun observeNotes() {
        viewModelScope.launch {
            notesRepository.observeNotes()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to load notes."
                    )
                }
                .collect { notes ->
                    _uiState.value = _uiState.value.copy(notes = notes, isLoading = false)
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun openAddDialog() {
        // Note doesn't exist yet, so new photos are namespaced under a throwaway draft id
        // until the note is actually saved (see saveNote).
        val draftId = UUID.randomUUID().toString()
        attachmentsSnapshotOnOpen = emptyList()
        _uiState.value = _uiState.value.copy(
            isDialogOpen = true,
            editingNote = null,
            titleError = null,
            editingAttachments = emptyList(),
            currentDraftId = draftId
        )
    }

    fun openEditDialog(note: Note) {
        val attachments = noteMediaStore.getAttachments(note.id)
        attachmentsSnapshotOnOpen = attachments
        _uiState.value = _uiState.value.copy(
            isDialogOpen = true,
            editingNote = note,
            titleError = null,
            editingAttachments = attachments,
            currentDraftId = note.id
        )
    }

    /** Called by the UI right after a photo was captured or picked into shared storage. */
    fun addMediaAttachment(attachment: MediaAttachment) {
        _uiState.value = _uiState.value.copy(
            editingAttachments = _uiState.value.editingAttachments + attachment
        )
    }

    /** Called when the user removes a photo from the dialog. Deletes the underlying file immediately. */
    fun removeMediaAttachment(attachment: MediaAttachment) {
        mediaStorageManager.delete(attachment.uri)
        _uiState.value = _uiState.value.copy(
            editingAttachments = _uiState.value.editingAttachments.filterNot { it.uri == attachment.uri }
        )
    }

    fun dismissDialog() {
        if (_uiState.value.isSaving) return

        // Any photo that wasn't part of the note when the dialog opened, and never got
        // saved, would otherwise be an orphaned file sitting in shared storage forever.
        val current = _uiState.value.editingAttachments
        val orphaned = current.filterNot { c -> attachmentsSnapshotOnOpen.any { it.uri == c.uri } }
        orphaned.forEach { mediaStorageManager.delete(it.uri) }

        _uiState.value = _uiState.value.copy(
            isDialogOpen = false,
            editingNote = null,
            titleError = null,
            editingAttachments = emptyList(),
            currentDraftId = ""
        )
        attachmentsSnapshotOnOpen = emptyList()
    }

    fun saveNote(title: String, description: String) {
        if (title.isBlank()) {
            _uiState.value = _uiState.value.copy(titleError = "Title is required.")
            return
        }

        val editingNoteId = _uiState.value.editingNote?.id
        val isEditing = editingNoteId != null
        val attachmentsToPersist = _uiState.value.editingAttachments

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null, titleError = null)

            if (editingNoteId == null) {
                val createdBy = authRepository.getCurrentUser()?.uid.orEmpty()
                notesRepository.createNote(title.trim(), description.trim(), createdBy).fold(
                    onSuccess = { note ->
                        noteMediaStore.setAttachments(note.id, attachmentsToPersist)
                        onSaveSuccess(isEditing = false)
                    },
                    onFailure = { onSaveFailure(it) }
                )
            } else {
                notesRepository.updateNote(editingNoteId, title.trim(), description.trim()).fold(
                    onSuccess = {
                        noteMediaStore.setAttachments(editingNoteId, attachmentsToPersist)
                        onSaveSuccess(isEditing = true)
                    },
                    onFailure = { onSaveFailure(it) }
                )
            }
        }
    }

    private fun onSaveSuccess(isEditing: Boolean) {
        attachmentsSnapshotOnOpen = emptyList()
        _uiState.value = _uiState.value.copy(
            isSaving = false,
            isDialogOpen = false,
            editingNote = null,
            editingAttachments = emptyList(),
            currentDraftId = "",
            errorMessage = null,
            mediaRefreshTick = _uiState.value.mediaRefreshTick + 1,
            toast = ToastMessage(if (isEditing) "Note updated" else "Note added")
        )
    }

    private fun onSaveFailure(exception: Throwable) {
        _uiState.value = _uiState.value.copy(
            isSaving = false,
            errorMessage = exception.localizedMessage ?: "Failed to save note. Please try again."
        )
    }

    /** Step 1 of delete: open the confirmation dialog for this note. */
    fun requestDeleteNote(note: Note) {
        _uiState.value = _uiState.value.copy(noteToDelete = note)
    }

    /** User tapped Cancel on the confirmation dialog. */
    fun dismissDeleteConfirmation() {
        if (_uiState.value.isDeleting) return
        _uiState.value = _uiState.value.copy(noteToDelete = null)
    }

    /** Step 2 of delete: user tapped OK/Delete on the confirmation dialog. */
    fun confirmDeleteNote() {
        val note = _uiState.value.noteToDelete ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)

            val result = notesRepository.deleteNote(note.id)

            result.fold(
                onSuccess = {
                    // Note is gone - clean up its local photo files too, they're orphaned otherwise.
                    noteMediaStore.getAttachments(note.id).forEach { mediaStorageManager.delete(it.uri) }
                    noteMediaStore.removeNote(note.id)
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        noteToDelete = null,
                        mediaRefreshTick = _uiState.value.mediaRefreshTick + 1,
                        toast = ToastMessage("Note deleted", ToastType.SUCCESS)
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        noteToDelete = null,
                        toast = ToastMessage(
                            exception.localizedMessage ?: "Failed to delete note.",
                            ToastType.ERROR
                        )
                    )
                }
            )
        }
    }

    fun consumeError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun consumeToast() {
        _uiState.value = _uiState.value.copy(toast = null)
    }
}