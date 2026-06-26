// app/src/main/java/com/appriyo/repairmanager/presentation/screens/CustomerListScreen.kt
package com.appriyo.repairmanager.presentation.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.appriyo.repairmanager.data.model.Repair
import com.appriyo.repairmanager.navigation.Screen
import com.appriyo.repairmanager.presentation.components.StatusChip
import com.appriyo.repairmanager.presentation.utils.buildStatusUpdateSms
import com.appriyo.repairmanager.presentation.utils.openSmsComposer
import com.appriyo.repairmanager.presentation.viewmodel.CustomerListViewModel
import com.appriyo.repairmanager.presentation.viewmodel.PrintViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    navController: NavHostController,
    viewModel: CustomerListViewModel = koinViewModel(),
    printViewModel: PrintViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val printUiState by printViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* user can just tap Print again */ }

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

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Repairs", fontWeight = FontWeight.SemiBold) },
                    actions = {
                        IconButton(onClick = { navController.navigate(Screen.SmsSettings.route) }) {
                            Icon(Icons.Filled.Settings, contentDescription = "SMS Settings")
                        }
                    }
                )
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = { Text("Search name, phone, device, serial") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
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

                uiState.filteredRepairs.isEmpty() -> {
                    EmptyState(isSearching = uiState.searchQuery.isNotBlank())
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.filteredRepairs, key = { it.id }) { repair ->
                            RepairCard(
                                repair = repair,
                                onClick = {
                                    navController.navigate(Screen.CustomerDetails.passId(repair.id))
                                },
                                onStatusSelected = { newStatus ->
                                    viewModel.updateStatus(repair.id, newStatus)
                                },
                                onSendSms = {
                                    openSmsComposer(context, repair.phoneNumber, buildStatusUpdateSms(repair))
                                },
                                onPrint = { printViewModel.printRepair(repair) },
                                onEdit = {
                                    navController.navigate(Screen.EditRepair.passId(repair.id))
                                },
                                isPrinting = printUiState.isPrinting
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(isSearching: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Filled.Inbox,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isSearching) "No repairs match your search." else "No repair records yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!isSearching) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap \"Add New Repair\" from the dashboard to create one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RepairCard(
    repair: Repair,
    onClick: () -> Unit,
    onStatusSelected: (String) -> Unit,
    onSendSms: () -> Unit,
    onPrint: () -> Unit,
    onEdit: () -> Unit,
    isPrinting: Boolean
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header: invoice serial + status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = repair.serialNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                StatusChip(status = repair.status, onStatusSelected = onStatusSelected)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = repair.customerName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            InfoRow(icon = Icons.Filled.Phone, text = repair.phoneNumber)

            if (repair.deviceModel.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                InfoRow(icon = Icons.Filled.Smartphone, text = repair.deviceModel)
            }

            if (repair.paymentInfo.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                InfoRow(icon = Icons.Filled.Payments, text = repair.paymentInfo, maxLines = 1)
            }

            if (repair.expectedDeliveryDate.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Delivery: ${repair.expectedDeliveryDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FilledTonalIconButton(onClick = onSendSms) {
                    Icon(Icons.Filled.Sms, contentDescription = "Send SMS")
                }
                FilledTonalIconButton(onClick = onPrint, enabled = !isPrinting) {
                    if (isPrinting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Print, contentDescription = "Print invoice")
                    }
                }
                FilledTonalIconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit repair")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String, maxLines: Int = 1) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}