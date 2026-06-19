package com.appriyo.repairmanager.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
fun DashboardScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "📊 Dashboard Screen",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This is Dashboard Screen",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Apply the weight here, where RowScope is available!
            StatCard("Total Repairs", "0", modifier = Modifier.weight(1f))
            StatCard("Pending", "0", modifier = Modifier.weight(1f))
            StatCard("Delivered", "0", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action Buttons
        Button(
            onClick = { navController.navigate(Screen.AddRepair.route) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("➕ Add New Repair")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { navController.navigate(Screen.CustomerList.route) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🔍 Search / View All Repairs")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Dashboard.route) { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🚪 Logout (Demo)")
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        // Chain the incoming modifier with the internal padding
        modifier = modifier.padding(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}