// app/src/main/java/com/appriyo/repairmanager/presentation/viewmodel/SmsSettingsViewModel.kt
package com.appriyo.repairmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.repairmanager.data.repository.AppSettingsRepository
import com.appriyo.repairmanager.data.repository.SecurityRepository
import com.appriyo.repairmanager.data.sms.DeviceIdProvider
import com.appriyo.repairmanager.data.sms.SmsSender
import com.appriyo.repairmanager.domain.security.PinValidator
import com.appriyo.repairmanager.presentation.state.SmsSettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SmsSettingsViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    private val deviceIdProvider: DeviceIdProvider,
    private val smsSender: SmsSender,
    private val securityRepository: SecurityRepository
) : ViewModel() {

    private val thisDeviceId = deviceIdProvider.getDeviceId()

    private val _uiState = MutableStateFlow(
        SmsSettingsUiState(availableSimSlots = smsSender.getAvailableSimSlots())
    )
    val uiState = _uiState.asStateFlow()

    init {
        observeSettings()
    }

    private fun observeSettings() {
        appSettingsRepository.observeSettings()
            .onEach { settings ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isThisDeviceSmsSender = settings?.smsSenderDeviceId == thisDeviceId,
                    currentSenderDeviceName = settings?.smsSenderDeviceName.orEmpty(),
                    selectedSimSlotIndex = settings?.simSlotIndex ?: -1,
                    availableSimSlots = smsSender.getAvailableSimSlots(),
                    errorMessage = null
                )
            }
            .catch { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.localizedMessage)
            }
            .launchIn(viewModelScope)
    }

    fun enableThisDeviceAsSmsSender() {
        viewModelScope.launch {
            appSettingsRepository.setSmsSenderDevice(thisDeviceId, deviceIdProvider.getDeviceDisplayName())
                .onFailure { e -> _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage) }
        }
    }

    fun disableThisDeviceAsSmsSender() {
        viewModelScope.launch {
            appSettingsRepository.setSmsSenderDevice("", "")
                .onFailure { e -> _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage) }
        }
    }

    fun selectSimSlot(index: Int) {
        viewModelScope.launch {
            appSettingsRepository.setSimSlotIndex(index)
                .onFailure { e -> _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage) }
        }
    }

    fun refreshSimSlots() {
        _uiState.value = _uiState.value.copy(availableSimSlots = smsSender.getAvailableSimSlots())
    }

    /**
     * Changes the owner PIN. Validates locally first, then delegates the
     * verify-and-update to [SecurityRepository.setPin].
     *
     * UI feedback is signalled through [SmsSettingsUiState.pinChangeStatus]:
     *  - Idle    → no change attempted
     *  - Saving  → request in flight
     *  - Success → PIN was changed
     *  - WrongPin → current PIN did not match
     *  - Error  → Firestore failure
     */
    fun changePin(currentPin: String, newPin: String, confirmPin: String) {
        val cleanCurrent = currentPin.trim()
        val cleanNew = newPin.trim()
        val cleanConfirm = confirmPin.trim()

        when {
            !PinValidator.isWellFormed(cleanCurrent) -> {
                _uiState.value = _uiState.value.copy(
                    pinChangeStatus = SmsSettingsUiState.PinChangeStatus.Error(
                        "Current PIN must be exactly 6 digits."
                    )
                )
            }
            !PinValidator.isWellFormed(cleanNew) -> {
                _uiState.value = _uiState.value.copy(
                    pinChangeStatus = SmsSettingsUiState.PinChangeStatus.Error(
                        "New PIN must be exactly 6 digits."
                    )
                )
            }
            cleanNew != cleanConfirm -> {
                _uiState.value = _uiState.value.copy(
                    pinChangeStatus = SmsSettingsUiState.PinChangeStatus.Error(
                        "New PIN and confirm PIN do not match."
                    )
                )
            }
            cleanCurrent == cleanNew -> {
                _uiState.value = _uiState.value.copy(
                    pinChangeStatus = SmsSettingsUiState.PinChangeStatus.Error(
                        "New PIN must be different from the current PIN."
                    )
                )
            }
            else -> {
                _uiState.value = _uiState.value.copy(
                    pinChangeStatus = SmsSettingsUiState.PinChangeStatus.Saving
                )
                viewModelScope.launch {
                    val result = securityRepository.setPin(cleanCurrent, cleanNew)
                    val newStatus = result.fold(
                        onSuccess = { ok ->
                            if (ok) SmsSettingsUiState.PinChangeStatus.Success
                            else SmsSettingsUiState.PinChangeStatus.WrongPin
                        },
                        onFailure = { e ->
                            SmsSettingsUiState.PinChangeStatus.Error(
                                e.localizedMessage ?: "Failed to change PIN."
                            )
                        }
                    )
                    _uiState.value = _uiState.value.copy(pinChangeStatus = newStatus)
                }
            }
        }
    }

    /** Resets [SmsSettingsUiState.pinChangeStatus] back to Idle (e.g. when the user starts typing again). */
    fun resetPinChangeStatus() {
        if (_uiState.value.pinChangeStatus !is SmsSettingsUiState.PinChangeStatus.Idle) {
            _uiState.value = _uiState.value.copy(
                pinChangeStatus = SmsSettingsUiState.PinChangeStatus.Idle
            )
        }
    }

    fun consumeError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}