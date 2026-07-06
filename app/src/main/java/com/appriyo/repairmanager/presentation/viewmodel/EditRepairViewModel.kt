// app/src/main/java/com/appriyo/repairmanager/presentation/viewmodel/EditRepairViewModel.kt
package com.appriyo.repairmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.repairmanager.data.repository.RepairRepository
import com.appriyo.repairmanager.presentation.state.EditRepairUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the EditRepairScreen.
 *
 * Loads the repair once (a live-updating form would be confusing to edit),
 * validates, and writes updates back via [RepairRepository.updateRepair].
 * id, serialNumber, createdAt and createdBy are never touched here.
 */
class EditRepairViewModel(
    private val repairRepository: RepairRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditRepairUiState())
    val uiState = _uiState.asStateFlow()

    fun loadRepair(repairId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingInitialData = true, errorMessage = null)

            val result = repairRepository.getRepair(repairId)

            result.fold(
                onSuccess = { repair ->
                    if (repair == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoadingInitialData = false,
                            errorMessage = "This repair record could not be found."
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoadingInitialData = false,
                            repair = repair
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingInitialData = false,
                        errorMessage = exception.localizedMessage ?: "Failed to load repair record."
                    )
                }
            )
        }
    }

    fun updateRepair(
        repairId: String,
        customerName: String,
        phoneNumber: String,
        deviceModel: String,
        problemDescription: String,
        expectedDeliveryDate: String,
        paymentInfo: String,
        additionalDetails: String,
        boxNumber: String,
        securityType: String,
        password: String,
        pattern: String,
        batteryIncluded: Boolean,
        simIncluded: Boolean,
        memoryCardIncluded: Boolean,
        simTrayIncluded: Boolean,
        backCoverIncluded: Boolean,
        deadPhonePermission: Boolean,
        status: String
    ) {
        val errors = validateFields(customerName, phoneNumber)

        if (errors.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                fieldErrors = errors,
                errorMessage = "Please fix the highlighted fields before saving.",
                isSuccess = false
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, fieldErrors = emptyMap())

            val result = repairRepository.updateRepair(
                repairId = repairId,
                customerName = customerName.trim(),
                phoneNumber = phoneNumber.trim(),
                deviceModel = deviceModel.trim(),
                problemDescription = problemDescription.trim(),
                expectedDeliveryDate = expectedDeliveryDate.trim(),
                paymentInfo = paymentInfo.trim(),
                additionalDetails = additionalDetails.trim(),
                boxNumber = boxNumber.trim(),
                securityType = securityType,
                password = password.trim(),
                pattern = pattern.trim(),
                batteryIncluded = batteryIncluded,
                simIncluded = simIncluded,
                memoryCardIncluded = memoryCardIncluded,
                simTrayIncluded = simTrayIncluded,
                backCoverIncluded = backCoverIncluded,
                deadPhonePermission = deadPhonePermission,
                status = status
            )

            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = false,
                        errorMessage = exception.localizedMessage ?: "Failed to update repair record."
                    )
                }
            )
        }
    }

    fun consumeOneTimeEvents() {
        _uiState.value = _uiState.value.copy(isSuccess = false, errorMessage = null)
    }

    private fun validateFields(customerName: String, phoneNumber: String): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        if (customerName.isBlank()) {
            errors["customerName"] = "Customer name is required."
        }

        val trimmedPhone = phoneNumber.trim()
        if (trimmedPhone.isNotBlank() && !trimmedPhone.matches(Regex("^\\d{11}$"))) {
            errors["phoneNumber"] = "Phone number must be exactly 11 digits."
        }

        return errors
    }
}