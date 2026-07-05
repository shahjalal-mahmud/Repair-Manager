// app/src/main/java/com/appriyo/repairmanager/presentation/viewmodel/SecurityViewModel.kt
package com.appriyo.repairmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.repairmanager.data.repository.SecurityRepository
import com.appriyo.repairmanager.presentation.state.SecurityUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Verifies PIN input against the owner's PIN stored in Firestore.
 *
 * Each call to [verify] hits Firestore so a PIN change made from the
 * Settings screen is picked up immediately on the very next attempt.
 */
class SecurityViewModel(
    private val securityRepository: SecurityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState = _uiState.asStateFlow()

    /**
     * Validates [input] against the stored owner PIN.
     *
     * @param onResult `true` if the PIN matched (caller proceeds with the
     *  protected action), `false` if it did not (caller keeps the prompt
     *  open so the user can retry).
     */
    fun verify(input: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isVerifying = true, errorMessage = null) }
            val result = securityRepository.verifyPin(input)
            result.fold(
                onSuccess = { ok ->
                    _uiState.update {
                        it.copy(
                            isVerifying = false,
                            errorMessage = if (ok) null else "Incorrect PIN. Try again."
                        )
                    }
                    onResult(ok)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isVerifying = false,
                            errorMessage = error.message ?: "Could not verify PIN."
                        )
                    }
                    onResult(false)
                }
            )
        }
    }

    /** Clears any inline error after the user starts typing again. */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}