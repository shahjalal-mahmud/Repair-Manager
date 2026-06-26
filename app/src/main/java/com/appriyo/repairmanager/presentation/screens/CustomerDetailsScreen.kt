// app/src/main/java/com/appriyo/repairmanager/presentation/screens/CustomerDetailsScreen.kt
package com.appriyo.repairmanager.presentation.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.appriyo.repairmanager.navigation.Screen
import com.appriyo.repairmanager.presentation.components.SectionCard
import com.appriyo.repairmanager.presentation.components.StatusChip
import com.appriyo.repairmanager.presentation.utils.buildStatusUpdateSms
import com.appriyo.repairmanager.presentation.utils.openSmsComposer
import com.appriyo.repairmanager.presentation.viewmodel.CustomerDetailsViewModel
import com.appriyo.repairmanager.presentation.viewmodel.PrintViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailsScreen(
    navController: NavHostController,
    repairId: String,
    viewModel: CustomerDetailsViewModel = koinViewModel(),
    printViewModel: PrintViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val printUiState by printViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* user can retry Print */ }

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

    LaunchedEffect(printUiState.missingPermissions) {
        if (printUiState.missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(printUiState.missingPermissions.toTypedArray())
            printViewModel.onPermissionsGranted()
        }
    }

    LaunchedEffect(printUiState.successMessage) {
        printUiState.successMessage?.let { message ->
            coroutineScope.launch { snackbarHostState.showSnackbar(message) }
            printViewModel.consumeSuccess()
        }
    }

    LaunchedEffect(printUiState.errorMessage) {
        printUiState.errorMessage?.let { message ->
            coroutineScope.launch { snackbarHostState.showSnackbar(message) }
            printViewModel.consumeError()
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
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Repair details", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                uiState.repair == null -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("This repair record could not be found. It may have been deleted.")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { navController.popBackStack() }) { Text("Back") }
                    }
                }

                else -> {
                    val repair = uiState.repair!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {

                        // Header card: serial, status, customer, quick actions
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            elevation = CardDefaults.cardElevation(0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = repair.serialNumber,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    StatusChip(
                                        status = repair.status,
                                        enabled = !uiState.isUpdatingStatus,
                                        onStatusSelected = { newStatus -> viewModel.updateStatus(repairId, newStatus) }
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = repair.customerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = repair.phoneNumber,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .weight(1f)
                                    ) {
                                        QuickAction(
                                            icon = Icons.Filled.Sms,
                                            label = "SMS",
                                            onClick = {
                                                openSmsComposer(context, repair.phoneNumber, buildStatusUpdateSms(repair))
                                            }
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        QuickAction(
                                            icon = Icons.Filled.Print,
                                            label = if (printUiState.isPrinting) "Printing…" else "Print",
                                            enabled = !printUiState.isPrinting,
                                            onClick = { printViewModel.printRepair(repair) }
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        QuickAction(
                                            icon = Icons.Filled.Edit,
                                            label = "Edit",
                                            onClick = { navController.navigate(Screen.EditRepair.passId(repair.id)) }
                                        )
                                    }
                                }
                            }
                        }

                        SectionCard(title = "Device & issue", icon = Icons.Filled.Smartphone) {
                            DetailRow("Device model", repair.deviceModel.ifBlank { "-" })
                            DetailRow("Problem", repair.problemDescription.ifBlank { "-" })
                            DetailRow("Expected delivery", repair.expectedDeliveryDate.ifBlank { "-" })
                            DetailRow("Box number", repair.boxNumber.ifBlank { "-" })
                            DetailRow("Additional details", repair.additionalDetails.ifBlank { "-" })
                            DetailRow("Created by", repair.createdBy.ifBlank { "-" })
                        }

                        if (repair.paymentInfo.isNotBlank()) {
                            SectionCard(title = "Payment", icon = Icons.Filled.Payments) {
                                DetailRow("Info", repair.paymentInfo)
                            }
                        }

                        SectionCard(title = "Security", icon = Icons.Filled.Lock) {
                            DetailRow("Security type", repair.securityType)
                            if (repair.password.isNotBlank()) DetailRow("Password", repair.password)
                            if (repair.pattern.isNotBlank()) DetailRow("Pattern", repair.pattern)
                            DetailRow(
                                "Dead phone permission",
                                if (repair.deadPhonePermission) "Granted" else "Not granted"
                            )
                        }

                        SectionCard(title = "Accessories received", icon = Icons.Filled.Inventory2) {
                            DetailRow("Battery", yesNo(repair.batteryIncluded))
                            DetailRow("SIM", yesNo(repair.simIncluded))
                            DetailRow("Memory card", yesNo(repair.memoryCardIncluded))
                            DetailRow("SIM tray", yesNo(repair.simTrayIncluded))
                            DetailRow("Back cover", yesNo(repair.backCoverIncluded))
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedButton(
                            onClick = { showDeleteConfirmation = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete repair")
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(vertical = 10.dp)
            .then(
                if (enabled) Modifier.then(
                    Modifier
                ) else Modifier
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(icon, contentDescription = label)
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun yesNo(value: Boolean) = if (value) "Yes" else "No"

@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}