// app/src/main/java/com/appriyo/repairmanager/presentation/screens/PrinterSettingsScreen.kt
package com.appriyo.repairmanager.presentation.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appriyo.repairmanager.data.model.PrinterDevice
import com.appriyo.repairmanager.presentation.viewmodel.PrintViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * Screen for discovering paired Bluetooth printers and saving the selection to Firestore.
 * Runtime permission handling is done here via [rememberLauncherForActivityResult].
 */
@Composable
fun PrinterSettingsScreen(
    viewModel: PrintViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) {
            viewModel.onPermissionsGranted()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Bluetooth permission denied. Cannot scan for printers."
                )
            }
        }
    }

    // Trigger permission request whenever the ViewModel flags missing permissions
    LaunchedEffect(uiState.missingPermissions) {
        if (uiState.missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(uiState.missingPermissions.toTypedArray())
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.consumeSuccess()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.consumeError()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Printer Settings", style = MaterialTheme.typography.headlineSmall)

            Spacer(Modifier.height(8.dp))

            // Current selection
            if (uiState.selectedPrinter.selectedPrinterName.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "Selected: ${uiState.selectedPrinter.selectedPrinterName}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            uiState.selectedPrinter.selectedPrinterAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Button(
                onClick = { viewModel.refreshPairedDevices() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Print, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Scan Paired Devices")
            }

            Spacer(Modifier.height(16.dp))

            when {
                uiState.isLoadingSettings -> {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                }

                uiState.pairedDevices.isEmpty() -> {
                    Text(
                        "No paired devices found. Pair your POS printer in Android Bluetooth settings first, then scan again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    Text("Select a printer:", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn {
                        items(uiState.pairedDevices, key = { it.address }) { device ->
                            PrinterDeviceCard(
                                device = device,
                                isSelected = device.address == uiState.selectedPrinter.selectedPrinterAddress,
                                isSaving = uiState.isSavingPrinter,
                                onSelect = { viewModel.selectAndSavePrinter(device) }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrinterDeviceCard(
    device: PrinterDevice,
    isSelected: Boolean,
    isSaving: Boolean,
    onSelect: () -> Unit
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isSaving) { onSelect() },
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}