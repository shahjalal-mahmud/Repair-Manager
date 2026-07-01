// app/src/main/java/com/appriyo/repairmanager/presentation/state/EmployeeNotesUiState.kt
package com.appriyo.repairmanager.presentation.state

import com.appriyo.repairmanager.data.model.EmployeeNote
import com.appriyo.repairmanager.presentation.components.ToastMessage
import java.util.Date

data class EmployeeNotesUiState(
    // Raw realtime Firestore stream, newest first, completely unfiltered.
    val allNotes: List<EmployeeNote> = emptyList(),
    // Notes for the selected period + search query - what the UI actually renders.
    val filteredNotes: List<EmployeeNote> = emptyList(),
    // Totals for the selected period, computed once in the ViewModel.
    val summary: LedgerSummary = LedgerSummary(),

    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,

    val isDialogOpen: Boolean = false,
    val editingNote: EmployeeNote? = null,
    val titleError: String? = null,

    val noteToDelete: EmployeeNote? = null,
    val isDeleting: Boolean = false,

    val toast: ToastMessage? = null,

    // Date filtering - lives entirely here so Compose never filters anything itself.
    val selectedFilter: LedgerDateFilter = LedgerDateFilter.TODAY,
    val customDate: Date = Date(),
    val customRangeStart: Date? = null,
    val customRangeEnd: Date? = null,
    val showDatePicker: Boolean = false,
    val showRangePicker: Boolean = false
)