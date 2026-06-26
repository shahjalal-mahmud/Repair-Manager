// app/src/main/java/com/appriyo/repairmanager/presentation/viewmodel/AddRepairViewModel.kt
package com.appriyo.repairmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.repairmanager.data.model.RepairStatus
import com.appriyo.repairmanager.data.repository.AuthRepository
import com.appriyo.repairmanager.data.repository.RepairRepository
import com.appriyo.repairmanager.presentation.state.AddRepairUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddRepairViewModel(
    private val repairRepository: RepairRepository,
    private val authRepository: AuthRepository,
    private val printViewModel: PrintViewModel
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddRepairUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            printViewModel.uiState.collect { printState ->
                _uiState.update { state ->
                    state.copy(
                        printSuccess = if (printState.successMessage != null) true
                        else if (printState.errorMessage != null) false
                        else state.printSuccess,
                        printErrorMessage = printState.errorMessage,
                        missingPermissions = printState.missingPermissions
                    )
                }
                if (printState.successMessage != null) printViewModel.consumeSuccess()
                if (printState.errorMessage != null) printViewModel.consumeError()
            }
        }
    }

    fun saveRepairOnly(
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
        draftId: String = "",           // ← new with default
        photoCount: Int = 0,            // ← new with default
        videoCount: Int = 0             // ← new with default
    ) = saveRepair(
        customerName, phoneNumber, deviceModel, problemDescription,
        expectedDeliveryDate, paymentInfo, additionalDetails, boxNumber,
        securityType, password, pattern, batteryIncluded, simIncluded,
        memoryCardIncluded, simTrayIncluded, backCoverIncluded, deadPhonePermission,
        draftId, photoCount, videoCount, shouldPrint = false
    )

    fun saveAndPrintRepair(
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
        draftId: String = "",           // ← new with default
        photoCount: Int = 0,            // ← new with default
        videoCount: Int = 0             // ← new with default
    ) = saveRepair(
        customerName, phoneNumber, deviceModel, problemDescription,
        expectedDeliveryDate, paymentInfo, additionalDetails, boxNumber,
        securityType, password, pattern, batteryIncluded, simIncluded,
        memoryCardIncluded, simTrayIncluded, backCoverIncluded, deadPhonePermission,
        draftId, photoCount, videoCount, shouldPrint = true
    )

    private fun saveRepair(
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
        draftId: String,
        photoCount: Int,
        videoCount: Int,
        shouldPrint: Boolean
    ) {
        val errors = validateFields(customerName, phoneNumber)
        if (errors.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    fieldErrors = errors,
                    errorMessage = "Please fix the highlighted fields before saving.",
                    isSuccess = false
                )
            }
            return
        }

        val currentUserId = authRepository.getCurrentUser()?.uid.orEmpty()
        if (currentUserId.isEmpty()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isSuccess = false,
                    errorMessage = "You must be signed in to save a repair record."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    fieldErrors = emptyMap()
                )
            }

            val result = repairRepository.createRepair(
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
                status = RepairStatus.PENDING,
                createdBy = currentUserId,
                draftId = draftId,
                photoCount = photoCount,
                videoCount = videoCount
            )

            result.fold(
                onSuccess = { repair ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            errorMessage = null,
                            generatedSerialNumber = repair.serialNumber
                        )
                    }
                    if (shouldPrint) printViewModel.printRepair(repair)
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = false,
                            errorMessage = exception.localizedMessage
                                ?: "Failed to save repair record. Please try again."
                        )
                    }
                }
            )
        }
    }

    fun consumeOneTimeEvents() {
        _uiState.update {
            it.copy(
                isSuccess = false,
                errorMessage = null,
                printSuccess = null,
                printErrorMessage = null,
                fieldErrors = emptyMap()
            )
        }
    }

    fun consumePrintError() {
        _uiState.update { it.copy(printErrorMessage = null) }
    }

    fun consumeMissingPermissions() {
        _uiState.update { it.copy(missingPermissions = emptyList()) }
    }

    private fun validateFields(customerName: String, phoneNumber: String): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (customerName.isBlank()) errors["customerName"] = "Customer name is required."
        val trimmedPhone = phoneNumber.trim()
        if (trimmedPhone.isBlank()) {
            errors["phoneNumber"] = "Phone number is required."
        } else if (!trimmedPhone.matches(Regex("^\\d{11}$"))) {
            errors["phoneNumber"] = "Phone number must be exactly 11 digits."
        }
        return errors
    }
}