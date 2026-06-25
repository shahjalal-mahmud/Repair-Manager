package com.appriyo.repairmanager.presentation.state

data class PrintUiState(
    val isPrinting: Boolean = false,
    val missingPermissions: List<String> = emptyList(),
    val successMessage: String? = null,
    val errorMessage: String? = null
)