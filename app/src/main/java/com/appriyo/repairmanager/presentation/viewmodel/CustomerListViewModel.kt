// app/src/main/java/com/appriyo/repairmanager/presentation/viewmodel/CustomerListViewModel.kt
package com.appriyo.repairmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.repairmanager.data.repository.RepairRepository
import com.appriyo.repairmanager.presentation.state.CustomerListUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * ViewModel for the CustomerListScreen.
 * Subscribes to a single realtime Firestore listener for the whole list;
 * search is done client-side against that already-loaded list.
 */
class CustomerListViewModel(
    private val repairRepository: RepairRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeRepairs()
    }

    private fun observeRepairs() {
        repairRepository.observeRepairs()
            .onEach { repairs ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    repairs = repairs,
                    errorMessage = null
                )
            }
            .catch { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = throwable.localizedMessage ?: "Failed to load repair records."
                )
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun updateStatus(repairId: String, newStatus: String) {
        viewModelScope.launch {
            val result = repairRepository.updateStatus(repairId, newStatus)
            result.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = exception.localizedMessage ?: "Failed to update status."
                )
            }
        }
    }

    fun consumeError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}