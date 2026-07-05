// app/src/main/java/com/appriyo/repairmanager/presentation/state/SecurityUiState.kt
package com.appriyo.repairmanager.presentation.state

/**
 * Immutable UI state for PIN verification interactions.
 *
 * The PIN system is intentionally per-action: each sensitive operation
 * (add, edit, delete) prompts for the PIN independently. There is no
 * long-lived "unlocked" flag - only a transient verifying state and a
 * transient error message that the PinEntryDialog displays inline.
 */
data class SecurityUiState(
    val isVerifying: Boolean = false,
    val errorMessage: String? = null
)