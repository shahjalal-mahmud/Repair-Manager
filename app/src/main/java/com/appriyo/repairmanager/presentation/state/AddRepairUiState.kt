// app/src/main/java/com/appriyo/repairmanager/presentation/state/AddRepairUiState.kt
package com.appriyo.repairmanager.presentation.state

/**
 * UI state for the Add Repair screen.
 *
 * The state is split into three independent lifecycles so that a Snackbar
 * showing up never wipes out the inline field errors and vice versa.
 *
 *  - [fieldErrors] / [isLoading]   - long-lived form state.
 *  - [snackbarMessage]             - one-shot UI message. Cleared by
 *                                    AddRepairViewModel.consumeSnackbar.
 *  - [saveSuccess]                 - one-shot success event. Cleared by
 *                                    AddRepairViewModel.consumeSaveSuccess.
 *  - [printSuccess] / [printErrorMessage] / [missingPermissions] - mirror
 *                                    of the print subsystem's state.
 */
data class AddRepairUiState(
    val isLoading: Boolean = false,

    /**
     * Map of field key -> human-readable error message. Persists until the
     * user edits the offending field or the next save attempt succeeds.
     * Never cleared by Snackbar lifecycle.
     */
    val fieldErrors: Map<String, String> = emptyMap(),

    val snackbarMessage: SnackbarMessage? = null,
    val saveSuccess: SaveSuccessEvent? = null,

    val printSuccess: Boolean? = null,
    val printErrorMessage: String? = null,
    val missingPermissions: List<String> = emptyList()
)

/**
 * Tagged message types so the screen (and any future tests) can tell
 * validation messages apart from generic errors.
 */
sealed class SnackbarMessage {
    abstract val text: String

    /** Neutral info (e.g. "Contact saved to phonebook."). */
    data class Info(override val text: String) : SnackbarMessage()

    /** Aggregated validation messages from a failed Save attempt. */
    data class Validation(override val text: String) : SnackbarMessage()

    /** Backend / unexpected failures. */
    data class Error(override val text: String) : SnackbarMessage()
}

/** Emitted once when a repair has been persisted. */
data class SaveSuccessEvent(
    val serialNumber: String
)