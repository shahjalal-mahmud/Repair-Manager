package com.appriyo.repairmanager.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.appriyo.repairmanager.navigation.Screen

data class RepairItem(
    val id: String,
    val serialNumber: String,
    val customerName: String,
    val deviceName: String,
    val status: String
)

@Composable
fun CustomerListScreen(navController: NavHostController) {
    var searchQuery by remember { mutableStateOf("") }

    // Demo data
    val repairs = listOf(
        RepairItem("1", "RM-000001", "John Doe", "Samsung A34", "Pending"),
        RepairItem("2", "RM-000002", "Jane Smith", "iPhone 13", "Repaired"),
        RepairItem("3", "RM-000003", "Bob Johnson", "Google Pixel 6", "Delivered"),
        RepairItem("4", "RM-000004", "Alice Williams", "Xiaomi Mi 11", "Cancelled"),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "📋 Customer List Screen",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This is Customer List Screen",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search by name, phone, device, or serial") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(repairs) { repair ->
                RepairItemCard(
                    repair = repair,
                    onClick = {
                        navController.navigate(
                            Screen.CustomerDetails.passId(repair.id)
                        )
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun RepairItemCard(repair: RepairItem, onClick: () -> Unit) {
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = repair.serialNumber,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = repair.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (repair.status) {
                        "Pending" -> MaterialTheme.colorScheme.error
                        "Repaired" -> MaterialTheme.colorScheme.primary
                        "Delivered" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.tertiary
                    }
                )
            }
            Text(
                text = repair.customerName,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = repair.deviceName,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}