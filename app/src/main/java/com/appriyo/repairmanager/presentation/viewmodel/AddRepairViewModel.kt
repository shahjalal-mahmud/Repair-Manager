// app/src/main/java/com/appriyo/repairmanager/presentation/viewmodel/AddRepairViewModel.kt
package com.appriyo.repairmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.repairmanager.data.repository.AuthRepository
import com.appriyo.repairmanager.data.repository.RepairRepository
import com.appriyo.repairmanager.presentation.state.AddRepairUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the AddRepairScreen.
 *
 * Handles:
 *  - Field validation
 *  - Calling the repository to create the repair record
 *  - Exposing UI state (loading / success / error) via StateFlow
 */
class AddRepairViewModel(
    private val repairRepository: RepairRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddRepairUiState())
    val uiState = _uiState.asStateFlow()

    /**
     * Validates the supplied fields and, if valid, creates a new repair record
     * in Firestore via [RepairRepository.createRepair].
     */
    fun saveRepair(
        customerName: String,
        phoneNumber: String,
        deviceName: String,
        problem: String,
        expectedDeliveryDate: String,
        paymentInfo: String
    ) {
        val errors = validateFields(
            customerName = customerName,
            phoneNumber = phoneNumber,
            deviceName = deviceName,
            problem = problem,
            expectedDeliveryDate = expectedDeliveryDate
        )

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
                deviceName = deviceName.trim(),
                problem = problem.trim(),
                expectedDeliveryDate = expectedDeliveryDate.trim(),
                paymentInfo = paymentInfo.trim(),
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

    /**
     * Resets the success/error flags. Call after the UI has reacted to a
     * success or error event (e.g. after navigating away or dismissing a snackbar)
     * to avoid re-triggering the same one-time event on recomposition.
     */
    fun consumeOneTimeEvents() {
        _uiState.value = _uiState.value.copy(
            isSuccess = false,
            errorMessage = null
        )
    }

    private fun validateFields(
        customerName: String,
        phoneNumber: String,
        deviceName: String,
        problem: String,
        expectedDeliveryDate: String
    ): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        if (customerName.isBlank()) {
            errors["customerName"] = "Customer name is required."
        }

        if (phoneNumber.isBlank()) {
            errors["phoneNumber"] = "Phone number is required."
        } else if (phoneNumber.trim().length < 6) {
            errors["phoneNumber"] = "Enter a valid phone number."
        }

        if (deviceName.isBlank()) {
            errors["deviceName"] = "Device name is required."
        }

        if (problem.isBlank()) {
            errors["problem"] = "Problem description is required."
        }

        if (expectedDeliveryDate.isBlank()) {
            errors["expectedDeliveryDate"] = "Expected delivery date is required."
        }

        return errors
    }
}