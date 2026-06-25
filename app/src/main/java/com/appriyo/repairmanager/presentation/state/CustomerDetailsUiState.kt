// app/src/main/java/com/appriyo/repairmanager/presentation/state/CustomerDetailsUiState.kt
package com.appriyo.repairmanager.presentation.state

import com.appriyo.repairmanager.data.model.Repair

data class CustomerDetailsUiState(
    val isLoading: Boolean = true,
    val repair: Repair? = null,
    val isDeleted: Boolean = false,
    val isUpdatingStatus: Boolean = false,
    val errorMessage: String? = null
)