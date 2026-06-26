// app/src/main/java/com/appriyo/repairmanager/presentation/viewmodel/EmployeeNotesViewModel.kt
package com.appriyo.repairmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.repairmanager.data.model.EmployeeNote
import com.appriyo.repairmanager.data.repository.AuthRepository
import com.appriyo.repairmanager.data.repository.EmployeeNotesRepository
import com.appriyo.repairmanager.presentation.components.ToastMessage
import com.appriyo.repairmanager.presentation.components.ToastType
import com.appriyo.repairmanager.presentation.state.EmployeeNotesUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class EmployeeNotesViewModel(
    private val employeeNotesRepository: EmployeeNotesRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmployeeNotesUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeNotes()
    }

    private fun observeNotes() {
        viewModelScope.launch {
            employeeNotesRepository.observeEmployeeNotes()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to load entries."
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

    fun openEditDialog(note: EmployeeNote) {
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

    /**
     * totalPaymentText / profitText come straight from the text fields;
     * blank or unparsable values default to 0.0 since both are optional.
     */
    fun saveNote(
        title: String,
        description: String,
        totalPaymentText: String,
        profitText: String
    ) {
        if (title.isBlank()) {
            _uiState.value = _uiState.value.copy(titleError = "Title is required.")
            return
        }

        val totalPayment = totalPaymentText.trim().toDoubleOrNull() ?: 0.0
        val profit = profitText.trim().toDoubleOrNull() ?: 0.0
        val editingNoteId = _uiState.value.editingNote?.id
        val isEditing = editingNoteId != null

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null, titleError = null)

            val result = if (editingNoteId == null) {
                val createdBy = authRepository.getCurrentUser()?.uid.orEmpty()
                employeeNotesRepository.createEmployeeNote(
                    title.trim(), description.trim(), totalPayment, profit, createdBy
                )
            } else {
                employeeNotesRepository.updateEmployeeNote(
                    editingNoteId, title.trim(), description.trim(), totalPayment, profit
                )
            }

            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        isDialogOpen = false,
                        editingNote = null,
                        errorMessage = null,
                        toast = ToastMessage(if (isEditing) "Entry updated" else "Entry added")
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = exception.localizedMessage ?: "Failed to save entry. Please try again."
                    )
                }
            )
        }
    }

    /** Step 1 of delete: open the confirmation dialog for this entry. */
    fun requestDeleteNote(note: EmployeeNote) {
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

            val result = employeeNotesRepository.deleteEmployeeNote(note.id)

            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        noteToDelete = null,
                        toast = ToastMessage("Entry deleted", ToastType.SUCCESS)
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        noteToDelete = null,
                        toast = ToastMessage(
                            exception.localizedMessage ?: "Failed to delete entry.",
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