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
    val isSearchActive: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,

    val isDialogOpen: Boolean = false,
    val editingNote: EmployeeNote? = null,
    val titleError: String? = null,

    val noteToDelete: EmployeeNote? = null,
    val isDeleting: Boolean = false,

    val toast: ToastMessage? = null,

    /**
     * View mode:
     *  - SINGLE_DAY: show entries from [selectedDate] (defaults to today)
     *  - MONTH: show entries aggregated across the whole month [selectedMonthStart]
     */
    val viewMode: LedgerViewMode = LedgerViewMode.SINGLE_DAY,
    val selectedDate: Date = Date(),
    val selectedMonthStart: Date = Date(),

    val showDatePicker: Boolean = false,
    val showMonthPicker: Boolean = false
)

enum class LedgerViewMode { SINGLE_DAY, MONTH }