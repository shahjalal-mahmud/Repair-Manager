package com.appriyo.repairmanager.presentation.state

import com.appriyo.repairmanager.data.model.TaliKhataEntry
import com.appriyo.repairmanager.data.model.TaliKhataHistoryEntry
import com.appriyo.repairmanager.presentation.viewmodel.TaliKhataFilter
import com.appriyo.repairmanager.presentation.viewmodel.TaliKhataSortOption
import com.appriyo.repairmanager.presentation.viewmodel.TaliKhataSummary

/**
 * Single immutable state object for the whole TaliKhata feature (list screen
 * + detail screen + add/edit dialog), exposed via StateFlow from
 * TaliKhataViewModel. No calculation happens in Compose - everything here is
 * already derived.
 */
data class TaliKhataUiState(
    val isLoading: Boolean = true,
    val entries: List<TaliKhataEntry> = emptyList(),
    val filteredEntries: List<TaliKhataEntry> = emptyList(),
    val summary: TaliKhataSummary = TaliKhataSummary(),

    val searchQuery: String = "",
    val filter: TaliKhataFilter = TaliKhataFilter.ALL,
    val sortOption: TaliKhataSortOption = TaliKhataSortOption.NEWEST,

    val showAddEditDialog: Boolean = false,
    val entryBeingEdited: TaliKhataEntry? = null,
    val isSaving: Boolean = false,

    val selectedEntry: TaliKhataEntry? = null,
    val history: List<TaliKhataHistoryEntry> = emptyList(),

    val errorMessage: String? = null
)