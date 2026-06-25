// app/src/main/java/com/appriyo/repairmanager/presentation/viewmodel/SmsSettingsViewModel.kt
package com.appriyo.repairmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.repairmanager.data.repository.AppSettingsRepository
import com.appriyo.repairmanager.data.sms.DeviceIdProvider
import com.appriyo.repairmanager.data.sms.SmsSender
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
    private val smsSender: SmsSender
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

    fun consumeError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}