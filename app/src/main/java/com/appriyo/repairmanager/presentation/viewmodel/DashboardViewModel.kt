package com.appriyo.repairmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.repairmanager.data.repository.RepairRepository
import com.appriyo.repairmanager.domain.delivery.DeliveryFilterUtils
import com.appriyo.repairmanager.domain.delivery.DeliverySummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * If DashboardScreen already has its own ViewModel, merge this stream into
 * it instead of adding a second one.
 */
class DashboardViewModel(
    private val repairRepository: RepairRepository
) : ViewModel() {

    private val _summary = MutableStateFlow(DeliverySummary())
    val summary = _summary.asStateFlow()

    init {
        repairRepository.observeRepairs()
            .onEach { repairs -> _summary.value = DeliveryFilterUtils.summarize(repairs) }
            .catch { /* keep last known summary on transient errors */ }
            .launchIn(viewModelScope)
    }
}