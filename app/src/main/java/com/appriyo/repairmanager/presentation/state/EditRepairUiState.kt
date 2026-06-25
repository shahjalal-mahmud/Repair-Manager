// app/src/main/java/com/appriyo/repairmanager/presentation/state/EditRepairUiState.kt
package com.appriyo.repairmanager.presentation.state

import com.appriyo.repairmanager.data.model.Repair

data class EditRepairUiState(
    val isLoadingInitialData: Boolean = true,
    val repair: Repair? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val fieldErrors: Map<String, String> = emptyMap()
)