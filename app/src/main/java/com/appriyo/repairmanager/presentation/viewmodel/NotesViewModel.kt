// app/src/main/java/com/appriyo/repairmanager/presentation/viewmodel/NotesViewModel.kt
package com.appriyo.repairmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class NotesViewModel(
    private val notesRepository: NotesRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState = _uiState.asStateFlow()

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
        _uiState.value = _uiState.value.copy(
            isDialogOpen = true,
            editingNote = null,
            titleError = null
        )
    }

    fun openEditDialog(note: Note) {
        _uiState.value = _uiState.value.copy(
            isDialogOpen = true,
            editingNote = note,
            titleError = null
        )
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(
            isDialogOpen = false,
            editingNote = null,
            titleError = null
        )
    }

    fun saveNote(title: String, description: String) {
        if (title.isBlank()) {
            _uiState.value = _uiState.value.copy(titleError = "Title is required.")
            return
        }

        val editingNoteId = _uiState.value.editingNote?.id
        val isEditing = editingNoteId != null

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null, titleError = null)

            val result = if (editingNoteId == null) {
                val createdBy = authRepository.getCurrentUser()?.uid.orEmpty()
                notesRepository.createNote(title.trim(), description.trim(), createdBy)
            } else {
                notesRepository.updateNote(editingNoteId, title.trim(), description.trim())
            }

            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        isDialogOpen = false,
                        editingNote = null,
                        errorMessage = null,
                        toast = ToastMessage(if (isEditing) "Note updated" else "Note added")
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = exception.localizedMessage ?: "Failed to save note. Please try again."
                    )
                }
            )
        }
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
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        noteToDelete = null,
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