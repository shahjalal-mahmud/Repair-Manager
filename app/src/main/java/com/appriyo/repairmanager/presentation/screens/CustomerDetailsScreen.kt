// app/src/main/java/com/appriyo/repairmanager/presentation/screens/CustomerDetailsScreen.kt
package com.appriyo.repairmanager.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.appriyo.repairmanager.data.model.RepairStatus
import com.appriyo.repairmanager.navigation.Screen
import com.appriyo.repairmanager.presentation.components.OptionDropdown
import com.appriyo.repairmanager.presentation.viewmodel.CustomerDetailsViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailsScreen(
    navController: NavHostController,
    repairId: String,
    viewModel: CustomerDetailsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(repairId) {
        viewModel.loadRepair(repairId)
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            navController.navigate(Screen.CustomerList.route) {
                popUpTo(Screen.CustomerList.route) { inclusive = true }
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message)
                viewModel.consumeError()
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete repair record?") },
            text = { Text("This will permanently delete this customer's repair record. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    viewModel.deleteRepair(repairId)
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                    }
                }

                uiState.repair == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("This repair record could not be found. It may have been deleted.")
                        Spacer(height = 16.dp)
                        Button(onClick = { navController.popBackStack() }) {
                            Text("Back")
                        }
                    }
                }

                else -> {
                    val repair = uiState.repair!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = repair.serialNumber,
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Spacer(height = 16.dp)

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                DetailRow("Customer Name", repair.customerName)
                                DetailRow("Phone Number", repair.phoneNumber)
                                DetailRow("Device Model", repair.deviceModel.ifBlank { "-" })
                                DetailRow("Problem", repair.problemDescription.ifBlank { "-" })
                                DetailRow("Expected Delivery", repair.expectedDeliveryDate.ifBlank { "-" })
                                DetailRow("Payment Info", repair.paymentInfo.ifBlank { "-" })
                                DetailRow("Box Number", repair.boxNumber.ifBlank { "-" })
                                DetailRow("Additional Details", repair.additionalDetails.ifBlank { "-" })
                                DetailRow("Created By", repair.createdBy.ifBlank { "-" })
                            }
                        }

                        Spacer(height = 16.dp)

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Text(
                                    text = "Security Information",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(height = 8.dp)
                                DetailRow("Security Type", repair.securityType)
                                if (repair.password.isNotBlank()) DetailRow("Password", repair.password)
                                if (repair.pattern.isNotBlank()) DetailRow("Pattern", repair.pattern)
                                DetailRow(
                                    "Dead Phone Permission",
                                    if (repair.deadPhonePermission) "Granted" else "Not granted"
                                )
                            }
                        }

                        Spacer(height = 16.dp)

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Text(
                                    text = "Accessories Received",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(height = 8.dp)
                                DetailRow("Battery", yesNo(repair.batteryIncluded))
                                DetailRow("SIM", yesNo(repair.simIncluded))
                                DetailRow("Memory Card", yesNo(repair.memoryCardIncluded))
                                DetailRow("SIM Tray", yesNo(repair.simTrayIncluded))
                                DetailRow("Back Cover", yesNo(repair.backCoverIncluded))
                            }
                        }

                        Spacer(height = 16.dp)

                        OptionDropdown(
                            label = "Status",
                            options = RepairStatus.ALL,
                            selectedOption = repair.status,
                            onOptionSelected = { newStatus -> viewModel.updateStatus(repairId, newStatus) },
                            enabled = !uiState.isUpdatingStatus
                        )

                        Spacer(height = 24.dp)

                        Button(
                            onClick = { navController.navigate(Screen.EditRepair.passId(repair.id)) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Edit Repair")
                        }

                        Spacer(height = 8.dp)

                        OutlinedButton(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Delete Repair")
                        }

                        Spacer(height = 8.dp)

                        OutlinedButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Back")
                        }

                        Spacer(height = 24.dp)
                    }
                }
            }
        }
    }
}

private fun yesNo(value: Boolean) = if (value) "Yes" else "No"

@Composable
fun DetailRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun Spacer(height: androidx.compose.ui.unit.Dp) {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(height))
}