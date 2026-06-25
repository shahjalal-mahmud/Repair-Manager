// app/src/main/java/com/appriyo/repairmanager/presentation/state/SmsSettingsUiState.kt
package com.appriyo.repairmanager.presentation.state

data class SmsSettingsUiState(
    val isLoading: Boolean = true,
    val isThisDeviceSmsSender: Boolean = false,
    val currentSenderDeviceName: String = "",
    val selectedSimSlotIndex: Int = -1,
    val availableSimSlots: List<com.appriyo.repairmanager.data.sms.SimSlotOption> = emptyList(),
    val errorMessage: String? = null
)