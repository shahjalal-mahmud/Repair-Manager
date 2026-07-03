package com.appriyo.repairmanager.presentation.state

import com.appriyo.repairmanager.data.model.Repair
import com.appriyo.repairmanager.domain.delivery.DeliveryFilter

data class DeliveryListUiState(
    val filter: DeliveryFilter = DeliveryFilter.ALL,
    val isLoading: Boolean = true,
    val repairs: List<Repair> = emptyList(),
    val errorMessage: String? = null
)