// app/src/main/java/com/appriyo/repairmanager/presentation/state/SmsSettingsUiState.kt
package com.appriyo.repairmanager.presentation.state

data class SmsSettingsUiState(
    val isLoading: Boolean = true,
    val isThisDeviceSmsSender: Boolean = false,
    val currentSenderDeviceName: String = "",
    val selectedSimSlotIndex: Int = -1,
    val availableSimSlots: List<com.appriyo.repairmanager.data.sms.SimSlotOption> = emptyList(),
    val errorMessage: String? = null,
    val pinChangeStatus: PinChangeStatus = PinChangeStatus.Idle
) {
    /**
     * Discrete states for the "Change Owner PIN" section. Keeping these as a
     * nested sealed hierarchy makes the UI switch exhaustive and prevents
     * stray "success" toasts when the operation actually failed.
     */
    sealed interface PinChangeStatus {
        data object Idle : PinChangeStatus
        data object Saving : PinChangeStatus
        data object Success : PinChangeStatus
        data object WrongPin : PinChangeStatus
        data class Error(val message: String) : PinChangeStatus
    }
}