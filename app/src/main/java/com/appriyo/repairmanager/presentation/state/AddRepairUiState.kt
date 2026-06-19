// app/src/main/java/com/appriyo/repairmanager/presentation/state/AddRepairUiState.kt
package com.appriyo.repairmanager.presentation.state

data class AddRepairUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val generatedSerialNumber: String? = null,
    val fieldErrors: Map<String, String> = emptyMap()
)