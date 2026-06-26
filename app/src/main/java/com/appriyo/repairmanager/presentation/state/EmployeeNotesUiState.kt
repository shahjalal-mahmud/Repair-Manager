// app/src/main/java/com/appriyo/repairmanager/presentation/state/EmployeeNotesUiState.kt
package com.appriyo.repairmanager.presentation.state

import com.appriyo.repairmanager.data.model.EmployeeNote
import com.appriyo.repairmanager.presentation.components.ToastMessage

data class EmployeeNotesUiState(
    val notes: List<EmployeeNote> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isDialogOpen: Boolean = false,
    val editingNote: EmployeeNote? = null,
    val titleError: String? = null,
    val noteToDelete: EmployeeNote? = null,
    val isDeleting: Boolean = false,
    val toast: ToastMessage? = null
)