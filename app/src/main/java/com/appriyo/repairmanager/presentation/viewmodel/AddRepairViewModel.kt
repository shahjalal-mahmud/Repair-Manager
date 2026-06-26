package com.appriyo.repairmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.repairmanager.data.model.RepairStatus
import com.appriyo.repairmanager.data.repository.AuthRepository
import com.appriyo.repairmanager.data.repository.RepairRepository
import com.appriyo.repairmanager.presentation.state.AddRepairUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
                _uiState.value = _uiState.value.copy(
                    printSuccess = if (printState.successMessage != null) true
                    else if (printState.errorMessage != null) false
                    else _uiState.value.printSuccess,
                    printErrorMessage = printState.errorMessage,
                    missingPermissions = printState.missingPermissions
                )
                if (printState.successMessage != null) printViewModel.consumeSuccess()
                if (printState.errorMessage != null) printViewModel.consumeError()
            }
        }
    }

    /**
     * Save repair without printing
     */
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
        deadPhonePermission: Boolean
    ) {
        saveRepair(
            customerName = customerName,
            phoneNumber = phoneNumber,
            deviceModel = deviceModel,
            problemDescription = problemDescription,
            expectedDeliveryDate = expectedDeliveryDate,
            paymentInfo = paymentInfo,
            additionalDetails = additionalDetails,
            boxNumber = boxNumber,
            securityType = securityType,
            password = password,
            pattern = pattern,
            batteryIncluded = batteryIncluded,
            simIncluded = simIncluded,
            memoryCardIncluded = memoryCardIncluded,
            simTrayIncluded = simTrayIncluded,
            backCoverIncluded = backCoverIncluded,
            deadPhonePermission = deadPhonePermission,
            shouldPrint = false
        )
    }

    /**
     * Save repair and print invoice
     */
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
        deadPhonePermission: Boolean
    ) {
        saveRepair(
            customerName = customerName,
            phoneNumber = phoneNumber,
            deviceModel = deviceModel,
            problemDescription = problemDescription,
            expectedDeliveryDate = expectedDeliveryDate,
            paymentInfo = paymentInfo,
            additionalDetails = additionalDetails,
            boxNumber = boxNumber,
            securityType = securityType,
            password = password,
            pattern = pattern,
            batteryIncluded = batteryIncluded,
            simIncluded = simIncluded,
            memoryCardIncluded = memoryCardIncluded,
            simTrayIncluded = simTrayIncluded,
            backCoverIncluded = backCoverIncluded,
            deadPhonePermission = deadPhonePermission,
            shouldPrint = true
        )
    }

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
        shouldPrint: Boolean
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

        val currentUserId = authRepository.getCurrentUser()?.uid.orEmpty()

        if (currentUserId.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isSuccess = false,
                errorMessage = "You must be signed in to save a repair record."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                fieldErrors = emptyMap()
            )

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
                createdBy = currentUserId
            )

            result.fold(
                onSuccess = { repair ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        errorMessage = null,
                        generatedSerialNumber = repair.serialNumber
                    )

                    // Print if requested
                    if (shouldPrint) {
                        printViewModel.printRepair(repair)
                    }
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = false,
                        errorMessage = exception.localizedMessage
                            ?: "Failed to save repair record. Please try again."
                    )
                }
            )
        }
    }

    fun consumeOneTimeEvents() {
        _uiState.value = _uiState.value.copy(
            isSuccess = false,
            errorMessage = null,
            printSuccess = null,
            printErrorMessage = null,
            fieldErrors = emptyMap()
        )
    }

    fun consumePrintError() {
        _uiState.value = _uiState.value.copy(
            printErrorMessage = null
        )
    }

    fun consumeMissingPermissions() {
        _uiState.value = _uiState.value.copy(missingPermissions = emptyList())
    }

    private fun validateFields(
        customerName: String,
        phoneNumber: String
    ): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        if (customerName.isBlank()) {
            errors["customerName"] = "Customer name is required."
        }

        val trimmedPhone = phoneNumber.trim()
        if (trimmedPhone.isBlank()) {
            errors["phoneNumber"] = "Phone number is required."
        } else if (!trimmedPhone.matches(Regex("^\\d{11}$"))) {
            errors["phoneNumber"] = "Phone number must be exactly 11 digits."
        }

        return errors
    }
}