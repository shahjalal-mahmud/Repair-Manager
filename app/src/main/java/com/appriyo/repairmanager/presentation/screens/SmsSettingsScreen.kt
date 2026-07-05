// app/src/main/java/com/appriyo/repairmanager/presentation/screens/SmsSettingsScreen.kt
package com.appriyo.repairmanager.presentation.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import com.appriyo.repairmanager.domain.security.PinValidator
import com.appriyo.repairmanager.presentation.state.SmsSettingsUiState
import com.appriyo.repairmanager.presentation.viewmodel.SmsSettingsViewModel

@Composable
fun SmsSettingsScreen(
    viewModel: SmsSettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.SEND_SMS] == true
        viewModel.refreshSimSlots()
        if (granted) {
            viewModel.enableThisDeviceAsSmsSender()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "SMS Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.fillMaxWidth(0.8f)) {
                        Text(
                            text = "Use this device for SMS sending",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Only one device should have this enabled - the one with a SIM card.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isThisDeviceSmsSender,
                        onCheckedChange = { checked ->
                            if (checked) {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.SEND_SMS,
                                        Manifest.permission.READ_PHONE_STATE,
                                        // Android 13+ requires READ_PHONE_NUMBERS for
                                        // SubscriptionManager.getActiveSubscriptionInfoList
                                        // to return non-null on some OEMs. Without it
                                        // the configured SIM slot is silently ignored.
                                        Manifest.permission.READ_PHONE_NUMBERS
                                    )
                                )
                            } else {
                                viewModel.disableThisDeviceAsSmsSender()
                            }
                        }
                    )
                }

                if (!uiState.isThisDeviceSmsSender && uiState.currentSenderDeviceName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Current SMS sender device: ${uiState.currentSenderDeviceName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (uiState.isThisDeviceSmsSender) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "SIM slot for outgoing SMS", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    var expanded by remember { mutableStateOf(false) }
                    val selectedLabel = uiState.availableSimSlots
                        .firstOrNull { it.index == uiState.selectedSimSlotIndex }
                        ?.label ?: "Automatic (system default)"

                    Box {
                        OutlinedButton(onClick = { expanded = true }) {
                            Text(selectedLabel)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            uiState.availableSimSlots.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        viewModel.selectSimSlot(option.index)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.refreshSimSlots() }) {
                        Text("Refresh SIM list")
                    }
                }
            }
        }

        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))
        ChangePinCard(
            status = uiState.pinChangeStatus,
            onChangePin = { current, new, confirm ->
                viewModel.changePin(current, new, confirm)
            },
            onUserEditedFields = { viewModel.resetPinChangeStatus() }
        )
    }
}

@Composable
private fun ChangePinCard(
    status: SmsSettingsUiState.PinChangeStatus,
    onChangePin: (current: String, new: String, confirm: String) -> Unit,
    onUserEditedFields: () -> Unit
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    // Clear success / error status once the user starts typing a new PIN so
    // stale "PIN changed!" doesn't linger forever.
    LaunchedEffect(currentPin, newPin, confirmPin) {
        if (currentPin.isNotEmpty() || newPin.isNotEmpty() || confirmPin.isNotEmpty()) {
            onUserEditedFields()
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Change Owner PIN",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Enter your current PIN, then a new 6-digit PIN.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = currentPin,
                onValueChange = { input ->
                    if (input.length <= PinValidator.PIN_LENGTH && input.all { it.isDigit() }) {
                        currentPin = input
                    }
                },
                label = { Text("Current PIN") },
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = newPin,
                onValueChange = { input ->
                    if (input.length <= PinValidator.PIN_LENGTH && input.all { it.isDigit() }) {
                        newPin = input
                    }
                },
                label = { Text("New PIN") },
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = confirmPin,
                onValueChange = { input ->
                    if (input.length <= PinValidator.PIN_LENGTH && input.all { it.isDigit() }) {
                        confirmPin = input
                    }
                },
                label = { Text("Confirm New PIN") },
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )

            val canSave = currentPin.length == PinValidator.PIN_LENGTH &&
                newPin.length == PinValidator.PIN_LENGTH &&
                confirmPin.length == PinValidator.PIN_LENGTH &&
                status !is SmsSettingsUiState.PinChangeStatus.Saving

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onChangePin(currentPin, newPin, confirmPin) },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (status is SmsSettingsUiState.PinChangeStatus.Saving) "Saving..." else "Save New PIN")
            }

            Spacer(modifier = Modifier.height(8.dp))
            when (status) {
                is SmsSettingsUiState.PinChangeStatus.Success -> {
                    Text(
                        text = "PIN changed successfully.",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    // Clear the form after a brief moment so the user can change it again later.
                    LaunchedEffect(Unit) {
                        currentPin = ""
                        newPin = ""
                        confirmPin = ""
                    }
                }
                is SmsSettingsUiState.PinChangeStatus.WrongPin -> {
                    Text(
                        text = "Current PIN is incorrect.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                is SmsSettingsUiState.PinChangeStatus.Error -> {
                    Text(
                        text = status.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                else -> Unit
            }
        }
    }
}