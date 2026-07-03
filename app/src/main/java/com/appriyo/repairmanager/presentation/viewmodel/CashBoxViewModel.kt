// app/src/main/java/com/appriyo/repairmanager/presentation/viewmodel/CashBoxViewModel.kt
package com.appriyo.repairmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.repairmanager.data.repository.CashBoxRepository
import com.appriyo.repairmanager.domain.cashbox.CashBoxSummary
import com.appriyo.repairmanager.domain.cashbox.CashBoxType
import com.appriyo.repairmanager.domain.cashbox.CashTransaction
import com.appriyo.repairmanager.domain.cashbox.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** Immutable UI state for CashBoxScreen. */
data class CashBoxUiState(
    val accountType: CashBoxType = CashBoxType.PRODUCT,
    val summary: CashBoxSummary = CashBoxSummary(),
    val transactions: List<CashTransaction> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

/**
 * One ViewModel implementation shared by both Product Box and Market Box,
 * parameterized by [accountType]. Injected via Koin using parametersOf(type),
 * mirroring the existing DeliveryListViewModel(filterKey) pattern.
 */
class CashBoxViewModel(
    private val repository: CashBoxRepository,
    private val accountType: CashBoxType
) : ViewModel() {

    private val _uiState = MutableStateFlow(CashBoxUiState(accountType = accountType))
    val uiState = _uiState.asStateFlow()

    init {
        combine(
            repository.observeSummary(accountType),
            repository.observeTransactions(accountType)
        ) { summary, transactions -> summary to transactions }
            .onEach { (summary, transactions) ->
                _uiState.value = _uiState.value.copy(
                    summary = summary,
                    transactions = transactions,
                    isLoading = false,
                    errorMessage = null
                )
            }
            .catch { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = throwable.message ?: "Failed to load ${accountType.displayName} data"
                )
            }
            .launchIn(viewModelScope)
    }

    fun addTransaction(title: String, description: String, amount: Double, type: TransactionType) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            val result = repository.addTransaction(accountType, title.trim(), description.trim(), amount, type)
            result.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(errorMessage = throwable.message ?: "Could not save transaction")
            }
            _uiState.value = _uiState.value.copy(isSaving = false)
        }
    }

    fun updateTransaction(
        original: CashTransaction,
        title: String,
        description: String,
        amount: Double,
        type: TransactionType
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            val result = repository.updateTransaction(
                accountType, original, title.trim(), description.trim(), amount, type
            )
            result.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(errorMessage = throwable.message ?: "Could not update transaction")
            }
            _uiState.value = _uiState.value.copy(isSaving = false)
        }
    }

    fun deleteTransaction(transaction: CashTransaction) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(errorMessage = null)
            val result = repository.deleteTransaction(accountType, transaction)
            result.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(errorMessage = throwable.message ?: "Could not delete transaction")
            }
        }
    }

    fun consumeError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}