// app/src/main/java/com/appriyo/repairmanager/presentation/viewmodel/AddRepairViewModel.kt
package com.appriyo.repairmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.repairmanager.data.model.RepairStatus
import com.appriyo.repairmanager.data.repository.AuthRepository
import com.appriyo.repairmanager.data.repository.RepairRepository
import com.appriyo.repairmanager.presentation.state.AddRepairUiState
import com.appriyo.repairmanager.presentation.state.SaveSuccessEvent
import com.appriyo.repairmanager.presentation.state.SnackbarMessage
import com.appriyo.repairmanager.util.PhoneNumberNormalizer
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
                    val newPrintSuccess = when {
                        printState.successMessage != null -> true
                        printState.errorMessage != null -> false
                        else -> state.printSuccess
                    }
                    if (state.printSuccess == newPrintSuccess &&
                        state.printErrorMessage == printState.errorMessage &&
                        state.missingPermissions == printState.missingPermissions
                    ) {
                        state
                    } else {
                        state.copy(
                            printSuccess = newPrintSuccess,
                            printErrorMessage = printState.errorMessage,
                            missingPermissions = printState.missingPermissions
                        )
                    }
                }
                if (printState.successMessage != null) printViewModel.consumeSuccess()
                if (printState.errorMessage != null) printViewModel.consumeError()
            }
        }
    }

    // ── public save entry points ──────────────────────────────────────────

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
        draftId: String = "",
        photoCount: Int = 0,
        videoCount: Int = 0
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
        draftId: String = "",
        photoCount: Int = 0,
        videoCount: Int = 0
    ) = saveRepair(
        customerName, phoneNumber, deviceModel, problemDescription,
        expectedDeliveryDate, paymentInfo, additionalDetails, boxNumber,
        securityType, password, pattern, batteryIncluded, simIncluded,
        memoryCardIncluded, simTrayIncluded, backCoverIncluded, deadPhonePermission,
        draftId, photoCount, videoCount, shouldPrint = true
    )

    // ── validation lifecycle helpers (UI-driven) ───────────────────────────

    /**
     * Remove the error for a single field. Called by the screen from each
     * TextField's onValueChange so that the error vanishes as soon as the
     * user starts correcting it.
     */
    fun clearFieldError(field: String) {
        _uiState.update { state ->
            if (state.fieldErrors.containsKey(field)) {
                val updated = state.fieldErrors.toMutableMap().apply { remove(field) }
                state.copy(fieldErrors = updated)
            } else state
        }
    }

    fun consumeSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun consumeSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = null) }
    }

    fun consumePrintSuccess() {
        _uiState.update { it.copy(printSuccess = null) }
    }

    fun consumePrintError() {
        _uiState.update { it.copy(printErrorMessage = null) }
    }

    fun consumeMissingPermissions() {
        _uiState.update { it.copy(missingPermissions = emptyList()) }
    }

    // ── core save flow ────────────────────────────────────────────────────

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
        val errors = validate(customerName, phoneNumber)
        if (errors.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    fieldErrors = errors,
                    snackbarMessage = SnackbarMessage.Validation(joinMessages(errors))
                )
            }
            return
        }

        val currentUserId = authRepository.getCurrentUser()?.uid.orEmpty()
        if (currentUserId.isEmpty()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    snackbarMessage = SnackbarMessage.Error(
                        "You must be signed in to save a repair record."
                    )
                )
            }
            return
        }

        val normalizedPhone = PhoneNumberNormalizer.normalizeOrEmpty(phoneNumber)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = repairRepository.createRepair(
                customerName = customerName.trim(),
                phoneNumber = normalizedPhone,
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
                            fieldErrors = emptyMap(),
                            saveSuccess = SaveSuccessEvent(
                                serialNumber = repair.serialNumber
                            )
                        )
                    }
                    if (shouldPrint) printViewModel.printRepair(repair)
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            snackbarMessage = SnackbarMessage.Error(
                                exception.localizedMessage
                                    ?: "Failed to save repair record. Please try again."
                            )
                            // fieldErrors intentionally left alone - the
                            // user may still be correcting them.
                        )
                    }
                }
            )
        }
    }

    // ── validation ────────────────────────────────────────────────────────

    /**
     * Returns an empty map when valid, otherwise a per-field error map.
     *
     * The phone is OPTIONAL: a blank value is accepted, and a non-blank
     * value is run through [PhoneNumberNormalizer] first. Only after
     * normalization do we decide whether to flag it as invalid. This way
     * users can save with no phone at all, and pasted/typed numbers in
     * any common BD format round-trip correctly.
     */
    private fun validate(
        customerName: String,
        phoneRaw: String
    ): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (customerName.isBlank()) {
            errors[FIELD_CUSTOMER_NAME] = "Customer name is required."
        }
        val trimmedPhone = phoneRaw.trim()
        if (trimmedPhone.isNotBlank() &&
            PhoneNumberNormalizer.normalize(trimmedPhone) == null
        ) {
            errors[FIELD_PHONE_NUMBER] =
                "Phone number must be a valid 11-digit Bangladeshi number (e.g. 01712345678)."
        }
        return errors
    }

    private fun joinMessages(errors: Map<String, String>): String =
        errors.entries.joinToString(" ") { it.value }

    companion object {
        /** Field keys referenced by [AddRepairUiState.fieldErrors]. */
        const val FIELD_CUSTOMER_NAME = "customerName"
        const val FIELD_PHONE_NUMBER = "phoneNumber"
    }
}
