package com.appriyo.repairmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.repairmanager.data.repository.RepairRepository
import com.appriyo.repairmanager.domain.delivery.DeliveryFilter
import com.appriyo.repairmanager.domain.delivery.DeliveryFilterUtils
import com.appriyo.repairmanager.presentation.state.DeliveryListUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DeliveryListViewModel(
    private val repairRepository: RepairRepository,
    filterKey: String
) : ViewModel() {

    private val filter = DeliveryFilter.fromKey(filterKey)

    private val _uiState = MutableStateFlow(DeliveryListUiState(filter = filter))
    val uiState = _uiState.asStateFlow()

    init {
        repairRepository.observeRepairs()
            .onEach { allRepairs ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        repairs = DeliveryFilterUtils.filter(allRepairs, filter),
                        errorMessage = null
                    )
                }
            }
            .catch { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.localizedMessage ?: "Failed to load deliveries."
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun updateStatus(repairId: String, newStatus: String) {
        viewModelScope.launch {
            repairRepository.updateStatus(repairId, newStatus).onFailure { exception ->
                _uiState.update {
                    it.copy(errorMessage = exception.localizedMessage ?: "Failed to update status.")
                }
            }
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}