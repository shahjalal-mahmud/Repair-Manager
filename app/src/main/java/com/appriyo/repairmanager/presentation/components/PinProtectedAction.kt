// app/src/main/java/com/appriyo/repairmanager/presentation/components/PinProtectedAction.kt
package com.appriyo.repairmanager.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.appriyo.repairmanager.presentation.viewmodel.SecurityViewModel

/**
 * Lightweight controller returned by [PinProtectedAction]. Screens obtain it
 * once (in their outer composable) and call [prompt] before any sensitive
 * action. The dialog is rendered by [PinProtectedAction] itself, so screens
 * do not need to wire any extra UI for the PIN gate.
 */
@Stable
class PinGateController internal constructor(
    private val showRef: () -> Unit
) {
    /**
     * Triggers the PIN prompt. If the user enters the correct PIN, [block]
     * is invoked. If they cancel or enter the wrong PIN, [block] is skipped.
     */
    fun prompt(block: () -> Unit) {
        pendingAction = block
        showRef()
    }

    internal var pendingAction: (() -> Unit)? = null
}

/**
 * The reusable PIN gate. Place this once near the top of any screen that
 * has PIN-protected actions, then call [PinGateController.prompt] before
 * each gated action (e.g. add, edit, delete).
 *
 * The same gate is intended to be reused for the Employee screen and any
 * other future feature that needs an owner-only check.
 *
 * Example:
 * ```
 * val pinGate = PinProtectedAction(securityViewModel) { /* verified */ }
 *
 * FloatingActionButton(onClick = {
 *     pinGate.prompt { showAddDialog = true }
 * })
 * ```
 */
@Composable
fun PinProtectedAction(
    securityViewModel: SecurityViewModel,
    onVerified: () -> Unit = {}
): PinGateController {
    var showDialog by remember { mutableStateOf(false) }

    val controller = remember {
        PinGateController(showRef = { showDialog = true })
    }

    if (showDialog) {
        val state by securityViewModel.uiState.collectAsState()
        PinEntryDialog(
            isVerifying = state.isVerifying,
            errorMessage = state.errorMessage,
            onSubmit = { pin ->
                securityViewModel.verify(pin) { ok ->
                    if (ok) {
                        val action = controller.pendingAction
                        controller.pendingAction = null
                        showDialog = false
                        action?.invoke()
                        onVerified()
                    }
                    // On failure: keep dialog open so the user can retry.
                    // The error message is rendered inside the dialog.
                }
            },
            onDismiss = {
                controller.pendingAction = null
                securityViewModel.clearError()
                showDialog = false
            }
        )
    }

    return controller
}