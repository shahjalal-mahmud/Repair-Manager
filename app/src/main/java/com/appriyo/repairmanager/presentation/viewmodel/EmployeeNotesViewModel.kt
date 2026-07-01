// app/src/main/java/com/appriyo/repairmanager/presentation/viewmodel/EmployeeNotesViewModel.kt
package com.appriyo.repairmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.repairmanager.data.model.EmployeeNote
import com.appriyo.repairmanager.data.model.WorkerType
import com.appriyo.repairmanager.data.repository.AuthRepository
import com.appriyo.repairmanager.data.repository.EmployeeNotesRepository
import com.appriyo.repairmanager.presentation.components.ToastMessage
import com.appriyo.repairmanager.presentation.components.ToastType
import com.appriyo.repairmanager.presentation.state.EmployeeNotesUiState
import com.appriyo.repairmanager.presentation.state.LedgerDateFilter
import com.appriyo.repairmanager.presentation.state.LedgerSummary
import com.appriyo.repairmanager.presentation.state.WorkerStats
import com.appriyo.repairmanager.presentation.utils.LedgerDateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

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
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Failed to load entries.")
                    }
                }
                .collect { notes ->
                    _uiState.update { it.copy(allNotes = notes, isLoading = false) }
                    recomputeDerivedState()
                }
        }
    }

    // ------------------------------------------------------------------
    // Date filtering - all filtering/aggregation happens here, never in Compose.
    // ------------------------------------------------------------------

    fun onFilterSelected(filter: LedgerDateFilter) {
        when (filter) {
            LedgerDateFilter.CUSTOM_DATE -> _uiState.update { it.copy(showDatePicker = true) }
            LedgerDateFilter.CUSTOM_RANGE -> _uiState.update { it.copy(showRangePicker = true) }
            else -> {
                _uiState.update { it.copy(selectedFilter = filter) }
                recomputeDerivedState()
            }
        }
    }

    fun onCustomDateSelected(date: Date) {
        _uiState.update {
            it.copy(
                selectedFilter = LedgerDateFilter.CUSTOM_DATE,
                customDate = date,
                showDatePicker = false
            )
        }
        recomputeDerivedState()
    }

    fun onCustomRangeSelected(start: Date, end: Date) {
        val (rangeStart, rangeEnd) = if (start.after(end)) end to start else start to end
        _uiState.update {
            it.copy(
                selectedFilter = LedgerDateFilter.CUSTOM_RANGE,
                customRangeStart = rangeStart,
                customRangeEnd = rangeEnd,
                showRangePicker = false
            )
        }
        recomputeDerivedState()
    }

    fun dismissDatePicker() {
        _uiState.update { it.copy(showDatePicker = false) }
    }

    fun dismissRangePicker() {
        _uiState.update { it.copy(showRangePicker = false) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        recomputeDerivedState()
    }

    private fun recomputeDerivedState() {
        val state = _uiState.value

        val periodNotes = when (state.selectedFilter) {
            LedgerDateFilter.TODAY -> employeeNotesRepository.filterForToday(state.allNotes)
            LedgerDateFilter.YESTERDAY -> employeeNotesRepository.filterForDate(
                state.allNotes,
                LedgerDateUtils.addDays(Date(), -1)
            )
            LedgerDateFilter.THIS_WEEK -> employeeNotesRepository.filterForWeek(state.allNotes)
            LedgerDateFilter.THIS_MONTH -> employeeNotesRepository.filterForMonth(state.allNotes)
            LedgerDateFilter.CUSTOM_DATE -> employeeNotesRepository.filterForDate(state.allNotes, state.customDate)
            LedgerDateFilter.CUSTOM_RANGE -> employeeNotesRepository.filterForRange(
                state.allNotes,
                state.customRangeStart ?: state.customDate,
                state.customRangeEnd ?: state.customDate
            )
        }

        // Search narrows what's displayed, but never affects the period's summary totals.
        val visibleNotes = if (state.searchQuery.isBlank()) {
            periodNotes
        } else {
            periodNotes.filter {
                it.title.contains(state.searchQuery, ignoreCase = true) ||
                        it.description.contains(state.searchQuery, ignoreCase = true)
            }
        }

        _uiState.update {
            it.copy(filteredNotes = visibleNotes, summary = buildSummary(periodNotes))
        }
    }

    private fun buildSummary(notes: List<EmployeeNote>): LedgerSummary {
        val workerANotes = notes.filter { WorkerType.fromStorageValue(it.workerType) == WorkerType.A }
        val workerBNotes = notes.filter { WorkerType.fromStorageValue(it.workerType) == WorkerType.B }

        val workerAStats = WorkerStats(
            workerType = WorkerType.A,
            entryCount = workerANotes.size,
            totalPayment = workerANotes.sumOf { it.totalPayment },
            totalProfit = workerANotes.sumOf { it.profit }
        )
        val workerBStats = WorkerStats(
            workerType = WorkerType.B,
            entryCount = workerBNotes.size,
            totalPayment = workerBNotes.sumOf { it.totalPayment },
            totalProfit = workerBNotes.sumOf { it.profit }
        )

        return LedgerSummary(
            totalEntries = notes.size,
            totalPayment = workerAStats.totalPayment + workerBStats.totalPayment,
            totalProfit = workerAStats.totalProfit + workerBStats.totalProfit,
            workerAStats = workerAStats,
            workerBStats = workerBStats
        )
    }

    // ------------------------------------------------------------------
    // Dialog / CRUD
    // ------------------------------------------------------------------

    fun openAddDialog() {
        _uiState.update { it.copy(isDialogOpen = true, editingNote = null, titleError = null) }
    }

    fun openEditDialog(note: EmployeeNote) {
        _uiState.update { it.copy(isDialogOpen = true, editingNote = note, titleError = null) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(isDialogOpen = false, editingNote = null, titleError = null) }
    }

    /**
     * totalPaymentText / profitText come straight from the text fields; blank
     * or unparsable values default to 0.0 since both are optional. workerType
     * is mandatory and always has a concrete value - the segmented selector
     * defaults to A and can never be left empty.
     */
    fun saveNote(
        title: String,
        description: String,
        totalPaymentText: String,
        profitText: String,
        workerType: WorkerType
    ) {
        if (title.isBlank()) {
            _uiState.update { it.copy(titleError = "Title is required.") }
            return
        }

        val totalPayment = totalPaymentText.trim().toDoubleOrNull() ?: 0.0
        val profit = profitText.trim().toDoubleOrNull() ?: 0.0
        val editingNoteId = _uiState.value.editingNote?.id
        val isEditing = editingNoteId != null

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, titleError = null) }

            val result = if (editingNoteId == null) {
                val createdBy = authRepository.getCurrentUser()?.uid.orEmpty()
                employeeNotesRepository.createEmployeeNote(
                    title.trim(), description.trim(), totalPayment, profit, workerType.storageValue, createdBy
                )
            } else {
                employeeNotesRepository.updateEmployeeNote(
                    editingNoteId, title.trim(), description.trim(), totalPayment, profit, workerType.storageValue
                )
            }

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isDialogOpen = false,
                            editingNote = null,
                            errorMessage = null,
                            toast = ToastMessage(if (isEditing) "Entry updated" else "Entry added")
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = exception.localizedMessage ?: "Failed to save entry. Please try again."
                        )
                    }
                }
            )
        }
    }

    /** Step 1 of delete: open the confirmation dialog for this entry. */
    fun requestDeleteNote(note: EmployeeNote) {
        _uiState.update { it.copy(noteToDelete = note) }
    }

    /** User tapped Cancel on the confirmation dialog. */
    fun dismissDeleteConfirmation() {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(noteToDelete = null) }
    }

    /** Step 2 of delete: user tapped OK/Delete on the confirmation dialog. */
    fun confirmDeleteNote() {
        val note = _uiState.value.noteToDelete ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }

            val result = employeeNotesRepository.deleteEmployeeNote(note.id)

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            noteToDelete = null,
                            toast = ToastMessage("Entry deleted", ToastType.SUCCESS)
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            noteToDelete = null,
                            toast = ToastMessage(
                                exception.localizedMessage ?: "Failed to delete entry.",
                                ToastType.ERROR
                            )
                        )
                    }
                }
            )
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun consumeToast() {
        _uiState.update { it.copy(toast = null) }
    }
}