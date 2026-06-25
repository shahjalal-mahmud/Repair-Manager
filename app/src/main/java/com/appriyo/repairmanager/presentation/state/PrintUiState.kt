// app/src/main/java/com/appriyo/repairmanager/presentation/state/PrintUiState.kt
package com.appriyo.repairmanager.presentation.state

import com.appriyo.repairmanager.data.model.PrinterDevice
import com.appriyo.repairmanager.data.model.PrinterSettings

data class PrintUiState(
    // Paired device list (shown in printer picker)
    val pairedDevices: List<PrinterDevice> = emptyList(),

    // Currently saved / selected printer
    val selectedPrinter: PrinterSettings = PrinterSettings(),

    // Transient status shown to the user
    val isPrinting: Boolean = false,
    val isSavingPrinter: Boolean = false,
    val isLoadingSettings: Boolean = true,

    // Permission gate
    val missingPermissions: List<String> = emptyList(),

    // One-shot messages
    val successMessage: String? = null,
    val errorMessage: String? = null
)