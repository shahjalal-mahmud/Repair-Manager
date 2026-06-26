// app/src/main/java/com/appriyo/repairmanager/presentation/state/NotesUiState.kt
package com.appriyo.repairmanager.presentation.state

import com.appriyo.repairmanager.data.model.Note
import com.appriyo.repairmanager.presentation.components.ToastMessage

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isDialogOpen: Boolean = false,
    val editingNote: Note? = null,
    val titleError: String? = null,
    val noteToDelete: Note? = null,
    val isDeleting: Boolean = false,
    val toast: ToastMessage? = null
)