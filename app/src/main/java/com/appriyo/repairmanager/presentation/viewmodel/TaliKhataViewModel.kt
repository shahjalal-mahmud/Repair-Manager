package com.appriyo.repairmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.repairmanager.data.model.TaliKhataEntry
import com.appriyo.repairmanager.data.model.TaliKhataType
import com.appriyo.repairmanager.data.repository.TaliKhataRepository
import com.appriyo.repairmanager.presentation.state.TaliKhataUiState
import com.appriyo.repairmanager.presentation.utils.buildTaliKhataSms
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * One-off UI events that need something the ViewModel can't do itself
 * (e.g. opening the device's SMS app needs a Context, which the ViewModel
 * intentionally does not hold).
 */
sealed class TaliKhataEvent {
    data class OpenSms(val phoneNumber: String, val message: String) : TaliKhataEvent()
}

/**
 * Thin orchestration layer over [TaliKhataRepository]. Owns UI state
 * (search/filter/sort/dialogs/selection) and forwards create/update/delete
 * calls to the repository - no Firestore query logic lives here.
 */
class TaliKhataViewModel(
    private val repository: TaliKhataRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaliKhataUiState())
    val uiState: StateFlow<TaliKhataUiState> = _uiState.asStateFlow()

    // SharedFlow with extraBufferCapacity=1 so a tap that happens before the
    // LaunchedEffect collector is attached is buffered instead of silently
    // dropped by tryEmit(). A regular SharedFlow(replay=0, extraBufferCapacity=0)
    // returns false from tryEmit() when there's no active collector.
    private val _events = MutableSharedFlow<TaliKhataEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val events: SharedFlow<TaliKhataEvent> = _events

    private var historyJob: Job? = null

    init {
        observeEntries()
    }

    private fun observeEntries() {
        viewModelScope.launch {
            repository.observeEntries().collect { result ->
                result.onSuccess { entries ->
                    _uiState.update { it.copy(isLoading = false, entries = entries) }
                    recompute()
                }.onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
            }
        }
    }

    // ---- Search / filter / sort ----

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        recompute()
    }

    fun onFilterChange(filter: TaliKhataFilter) {
        _uiState.update { it.copy(filter = filter) }
        recompute()
    }

    fun onSortOptionChange(sortOption: TaliKhataSortOption) {
        _uiState.update { it.copy(sortOption = sortOption) }
        recompute()
    }

    private fun recompute() {
        val state = _uiState.value
        val query = state.searchQuery.trim()

        var list = state.entries
        if (query.isNotEmpty()) {
            list = list.filter {
                it.personName.contains(query, ignoreCase = true) ||
                        it.phoneNumber.contains(query, ignoreCase = true)
            }
        }
        list = when (state.filter) {
            TaliKhataFilter.ALL -> list
            TaliKhataFilter.YOU_OWE -> list.filter { it.typeEnum == TaliKhataType.YOU_OWE }
            TaliKhataFilter.THEY_OWE_YOU -> list.filter { it.typeEnum == TaliKhataType.THEY_OWE_YOU }
        }
        // Sort by createdAt with nullsLast so entries that haven't received
        // their server timestamp yet still get a stable order and don't
        // shuffle the list every snapshot.
        list = when (state.sortOption) {
            TaliKhataSortOption.NEWEST -> list.sortedWith(
                compareByDescending<TaliKhataEntry> { it.createdAt != null }
                    .thenByDescending { it.createdAt }
            )
            TaliKhataSortOption.OLDEST -> list.sortedWith(
                compareBy<TaliKhataEntry> { it.createdAt != null }
                    .thenBy { it.createdAt }
            )
            TaliKhataSortOption.HIGHEST_BALANCE -> list.sortedByDescending { it.balance }
            TaliKhataSortOption.LOWEST_BALANCE -> list.sortedBy { it.balance }
            TaliKhataSortOption.ALPHABETICAL -> list.sortedBy { it.personName.lowercase() }
        }

        val youOwe = state.entries.filter { it.typeEnum == TaliKhataType.YOU_OWE }.sumOf { it.balance }
        val owedToYou = state.entries.filter { it.typeEnum == TaliKhataType.THEY_OWE_YOU }.sumOf { it.balance }

        _uiState.update {
            it.copy(
                filteredEntries = list,
                summary = TaliKhataSummary(
                    totalEntries = state.entries.size,
                    totalYouOwe = youOwe,
                    totalTheyOweYou = owedToYou
                )
            )
        }
    }

    // ---- Add / edit dialog ----

    fun onAddEntryClick() {
        _uiState.update { it.copy(showAddEditDialog = true, entryBeingEdited = null) }
    }

    fun onEditEntryClick(entry: TaliKhataEntry) {
        // Re-resolve the latest snapshot from the live entries list, in case
        // it was updated by a remote change between the card render and the
        // tap on Edit.
        val latest = _uiState.value.entries.firstOrNull { it.id == entry.id } ?: entry
        _uiState.update { it.copy(showAddEditDialog = true, entryBeingEdited = latest) }
    }

    fun onDismissAddEditDialog() {
        _uiState.update { it.copy(showAddEditDialog = false, entryBeingEdited = null) }
    }

    fun onSaveEntry(
        personName: String,
        phoneNumber: String,
        details: String,
        type: TaliKhataType,
        amount: Double,
        isIncrease: Boolean
    ) {
        val editing = _uiState.value.entryBeingEdited
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val result = if (editing == null) {
                val id = repository.generateEntryId()
                repository.createEntry(
                    entryId = id,
                    personName = personName,
                    phoneNumber = phoneNumber,
                    details = details,
                    initialBalance = amount,
                    type = type,
                    createdBy = auth.currentUser?.uid.orEmpty()
                )
            } else {
                val detailsResult = repository.updateDetails(editing.id, personName, phoneNumber, details, type)
                if (detailsResult.isFailure) {
                    detailsResult
                } else if (amount != 0.0) {
                    repository.adjustBalance(editing.id, amount, isIncrease).map { }
                } else {
                    Result.success(Unit)
                }
            }

            result.onSuccess {
                _uiState.update { it.copy(showAddEditDialog = false, entryBeingEdited = null, isSaving = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
            }
        }
    }

    // ---- Delete (two-step confirm) ----

    fun onDeleteEntryClick(entry: TaliKhataEntry) {
        // Stage the entry for deletion. The UI is responsible for showing a
        // confirmation dialog before calling onConfirmDelete().
        val latest = _uiState.value.entries.firstOrNull { it.id == entry.id } ?: entry
        _uiState.update { it.copy(pendingDeleteEntry = latest) }
    }

    fun onCancelDelete() {
        _uiState.update { it.copy(pendingDeleteEntry = null) }
    }

    fun onConfirmDelete() {
        val target = _uiState.value.pendingDeleteEntry ?: return
        _uiState.update { it.copy(pendingDeleteEntry = null) }
        viewModelScope.launch {
            repository.deleteEntry(target.id).onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    // ---- Detail sheet ----

    fun onEntryClick(entry: TaliKhataEntry) {
        _uiState.update { it.copy(selectedEntryId = entry.id, history = emptyList()) }
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            repository.observeHistory(entry.id).collect { result ->
                result.onSuccess { history ->
                    _uiState.update { it.copy(history = history) }
                }
            }
        }
    }

    fun onDismissDetail() {
        historyJob?.cancel()
        _uiState.update { it.copy(selectedEntryId = null, history = emptyList()) }
    }

    // ---- SMS (delegated to the UI layer via events; no Context here) ----

    fun onSmsClick(entry: TaliKhataEntry) {
        if (entry.phoneNumber.isBlank()) return
        val message = buildTaliKhataSms(entry)
        // tryEmit() with extraBufferCapacity=1 will hold one event even if no
        // collector is attached yet (e.g. between recomposition and the next
        // LaunchedEffect restart). Falls back to a suspended emit if the
        // buffer is somehow full.
        val event = TaliKhataEvent.OpenSms(entry.phoneNumber, message)
        if (!_events.tryEmit(event)) {
            viewModelScope.launch { _events.emit(event) }
        }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * Chip filter shown above the TaliKhata list.
 */
enum class TaliKhataFilter(val label: String) {
    ALL("All"),
    YOU_OWE("You Owe"),
    THEY_OWE_YOU("Owes You")
}

/**
 * Sort options exposed in the TaliKhata list toolbar.
 */
enum class TaliKhataSortOption(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    HIGHEST_BALANCE("Highest Balance"),
    LOWEST_BALANCE("Lowest Balance"),
    ALPHABETICAL("Alphabetical")
}

/**
 * Aggregate figures shown in the summary dashboard at the top of the
 * TaliKhata screen. Computed over ALL entries (ignores search/filter/sort).
 */
data class TaliKhataSummary(
    val totalEntries: Int = 0,
    val totalYouOwe: Double = 0.0,
    val totalTheyOweYou: Double = 0.0
) {
    /** Positive = net you are owed. Negative = net you owe. */
    val netBalance: Double
        get() = totalTheyOweYou - totalYouOwe
}