// app/src/main/java/com/appriyo/repairmanager/presentation/screens/CustomerListScreen.kt
package com.appriyo.repairmanager.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.appriyo.repairmanager.data.model.Repair
import com.appriyo.repairmanager.data.model.RepairStatus
import com.appriyo.repairmanager.navigation.Screen
import com.appriyo.repairmanager.presentation.viewmodel.CustomerListViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun CustomerListScreen(
    navController: NavHostController,
    viewModel: CustomerListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Customer / Repair List",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            label = { Text("Search by name, phone, device, or serial") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                }
            }

            uiState.filteredRepairs.isEmpty() -> {
                Text(
                    text = if (uiState.searchQuery.isBlank()) {
                        "No repair records yet. Tap \"Add New Repair\" from the dashboard to create one."
                    } else {
                        "No repairs match your search."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> {
                LazyColumn {
                    items(uiState.filteredRepairs, key = { it.id }) { repair ->
                        RepairItemCard(
                            repair = repair,
                            onClick = {
                                navController.navigate(Screen.CustomerDetails.passId(repair.id))
                            },
                            onStatusSelected = { newStatus ->
                                viewModel.updateStatus(repair.id, newStatus)
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun RepairItemCard(
    repair: Repair,
    onClick: () -> Unit,
    onStatusSelected: (String) -> Unit
) {
    var statusMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = repair.serialNumber,
                    style = MaterialTheme.typography.titleMedium
                )

                Box {
                    Text(
                        text = repair.status,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor(repair.status),
                        modifier = Modifier.clickable { statusMenuExpanded = true }
                    )

                    DropdownMenu(
                        expanded = statusMenuExpanded,
                        onDismissRequest = { statusMenuExpanded = false }
                    ) {
                        RepairStatus.ALL.forEach { statusOption ->
                            DropdownMenuItem(
                                text = { Text(statusOption) },
                                onClick = {
                                    onStatusSelected(statusOption)
                                    statusMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Text(
                text = repair.customerName,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = repair.phoneNumber,
                style = MaterialTheme.typography.bodyMedium
            )
            if (repair.deviceModel.isNotBlank()) {
                Text(
                    text = repair.deviceModel,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (repair.expectedDeliveryDate.isNotBlank()) {
                Text(
                    text = "Delivery: ${repair.expectedDeliveryDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun statusColor(status: String) = when (status) {
    RepairStatus.PENDING -> MaterialTheme.colorScheme.error
    RepairStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
    RepairStatus.COMPLETED -> MaterialTheme.colorScheme.secondary
    RepairStatus.DELIVERED -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.outline
}