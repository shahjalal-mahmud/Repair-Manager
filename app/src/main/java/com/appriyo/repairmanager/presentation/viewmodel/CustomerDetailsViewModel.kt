// app/src/main/java/com/appriyo/repairmanager/presentation/viewmodel/CustomerDetailsViewModel.kt
package com.appriyo.repairmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.repairmanager.data.repository.RepairRepository
import com.appriyo.repairmanager.presentation.state.CustomerDetailsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * ViewModel for the CustomerDetailsScreen.
 * Subscribes to a realtime listener on a single repair document, so status
 * changes made here or on another device are reflected immediately.
 */
class CustomerDetailsViewModel(
    private val repairRepository: RepairRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerDetailsUiState())
    val uiState = _uiState.asStateFlow()

    fun loadRepair(repairId: String) {
        repairRepository.observeRepair(repairId)
            .onEach { repair ->
                _uiState.value = _uiState.value.copy(isLoading = false, repair = repair)
            }
            .catch { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = throwable.localizedMessage ?: "Failed to load repair record."
                )
            }
            .launchIn(viewModelScope)
    }

    fun updateStatus(repairId: String, newStatus: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdatingStatus = true)
            val result = repairRepository.updateStatus(repairId, newStatus)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isUpdatingStatus = false)
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isUpdatingStatus = false,
                        errorMessage = exception.localizedMessage ?: "Failed to update status."
                    )
                }
            )
        }
    }

    fun deleteRepair(repairId: String) {
        viewModelScope.launch {
            val result = repairRepository.deleteRepair(repairId)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isDeleted = true)
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = exception.localizedMessage ?: "Failed to delete repair record."
                    )
                }
            )
        }
    }

    fun consumeError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}