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
 *
 * [selectedEntryId] is the id of the entry whose detail sheet is open. The
 * live [TaliKhataEntry] is looked up from [entries] in [liveSelectedEntry] so
 * the sheet always reflects the latest Firestore data (balance, etc.).
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

    val selectedEntryId: String? = null,
    val history: List<TaliKhataHistoryEntry> = emptyList(),

    val pendingDeleteEntry: TaliKhataEntry? = null,

    val errorMessage: String? = null
) {
    /**
     * The freshest known [TaliKhataEntry] for [selectedEntryId]. Falls back to
     * `null` if the entry no longer exists (e.g. it was deleted while the
     * sheet was open).
     */
    val liveSelectedEntry: TaliKhataEntry?
        get() = selectedEntryId?.let { id -> entries.firstOrNull { it.id == id } }
}