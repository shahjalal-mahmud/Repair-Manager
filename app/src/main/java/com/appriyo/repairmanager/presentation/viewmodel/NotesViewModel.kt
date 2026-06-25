// app/src/main/java/com/appriyo/repairmanager/presentation/viewmodel/NotesViewModel.kt
package com.appriyo.repairmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.repairmanager.data.model.Note
import com.appriyo.repairmanager.data.repository.AuthRepository
import com.appriyo.repairmanager.data.repository.NotesRepository
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
                        errorMessage = null
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

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            val result = notesRepository.deleteNote(noteId)
            result.fold(
                onSuccess = { /* realtime listener refreshes the list automatically */ },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = exception.localizedMessage ?: "Failed to delete note. Please try again."
                    )
                }
            )
        }
    }

    fun consumeError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}