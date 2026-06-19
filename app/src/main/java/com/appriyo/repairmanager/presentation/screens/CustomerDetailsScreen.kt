package com.appriyo.repairmanager.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.appriyo.repairmanager.navigation.Screen

@Composable
fun CustomerDetailsScreen(navController: NavHostController, repairId: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "📄 Customer Details Screen",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This is Customer Details Screen",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Repair ID: $repairId",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Demo repair details card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                DetailRow("Serial Number", "RM-000001")
                DetailRow("Customer Name", "John Doe")
                DetailRow("Phone Number", "+8801XXXXXXXXX")
                DetailRow("Device Name", "Samsung A34")
                DetailRow("Problem", "Display Issue")
                DetailRow("Delivery Date", "2026-06-20")
                DetailRow("Payment Info", "Advance 500")
                DetailRow("Status", "Pending")
                DetailRow("Created Date", "2026-06-19 10:30 AM")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Button(
            onClick = { /* Navigate to edit screen (future) */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("✏️ Edit Repair")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { /* Update status (future) */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🔄 Update Status")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { /* Print invoice (future) */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🖨️ Reprint Invoice")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("⬅️ Back")
        }
    }
}

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