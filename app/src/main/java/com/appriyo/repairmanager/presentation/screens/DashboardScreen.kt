// app/src/main/java/com/appriyo/repairmanager/presentation/screens/DashboardScreen.kt
package com.appriyo.repairmanager.presentation.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.appriyo.repairmanager.domain.cashbox.CashBoxType
import com.appriyo.repairmanager.domain.delivery.DeliveryFilter
import com.appriyo.repairmanager.navigation.Screen
import com.appriyo.repairmanager.presentation.components.CashBoxDashboardCard
import com.appriyo.repairmanager.presentation.components.DeliveryCard
import com.appriyo.repairmanager.presentation.viewmodel.DashboardViewModel
import org.koin.androidx.compose.koinViewModel

private data class DashboardFeature(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String,
    val tint: Color
)

@Composable
fun DashboardScreen(navController: NavHostController) {
    val dashboardViewModel: DashboardViewModel = koinViewModel()
    val summary by dashboardViewModel.summary.collectAsState()
    val features = listOf(
        DashboardFeature(
            title = "Add Repair",
            subtitle = "Create a new job",
            icon = Icons.Default.Add,
            route = Screen.AddRepair.route,
            tint = Color(0xFF3B82F6)
        ),
        DashboardFeature(
            title = "Customers",
            subtitle = "View all repairs",
            icon = Icons.AutoMirrored.Filled.List,
            route = Screen.CustomerList.route,
            tint = Color(0xFF8B5CF6)
        ),
        DashboardFeature(
            title = "Notes",
            subtitle = "Quick reminders",
            icon = Icons.AutoMirrored.Filled.Note,
            route = Screen.Notes.route,
            tint = Color(0xFFF59E0B)
        ),
        DashboardFeature(
            title = "Employee",
            subtitle = "Manage your team",
            icon = Icons.Default.Person,
            route = Screen.Employee.route,
            tint = Color(0xFF10B981)
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Welcome back",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = {
                        navController.navigate(Screen.DeliveryList.passFilter(DeliveryFilter.TODAY.key))
                    }
                ) {
                    BadgedBox(
                        badge = {
                            if (summary.todayCount > 0) {
                                Badge { Text(summary.todayCount.toString()) }
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Today's deliveries")
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        item {
            TaliKhataCard(onClick = {
                navController.navigate(Screen.TaliKhata.route)
            })
            Spacer(Modifier.height(20.dp))
        }

        item {
            Text(
                text = "Quick Access",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
        }

        items(features.chunked(2)) { rowFeatures ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowFeatures.forEach { feature ->
                    FeatureCard(
                        feature = feature,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(feature.route) }
                    )
                }
                // Keep single trailing item the same width as its sibling in an odd-count row.
                if (rowFeatures.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Cash Box Management (new) - Product Box & Market Box balance cards.
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Cash Boxes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
        }
        item {
            CashBoxDashboardCard(
                accountType = CashBoxType.PRODUCT,
                onClick = { navController.navigate(Screen.CashBox.passType(CashBoxType.PRODUCT)) }
            )
        }
        item {
            Spacer(Modifier.height(12.dp))
            CashBoxDashboardCard(
                accountType = CashBoxType.MARKET,
                onClick = { navController.navigate(Screen.CashBox.passType(CashBoxType.MARKET)) }
            )
        }

        item {
            Spacer(Modifier.height(80.dp))
        }
        // 5-card grid/row — wire each onClick to the matching filter:
        item {
            Text(
                text = "Deliveries",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
        }

        item {
            DeliveryCard(
                title = "Today", count = summary.todayCount,
                icon = Icons.Filled.Today, accentColor = MaterialTheme.colorScheme.primary,
                onClick = { navController.navigate(Screen.DeliveryList.passFilter(DeliveryFilter.TODAY.key)) }
            )
        }
        item {
            Spacer(Modifier.height(12.dp))
            DeliveryCard(
                title = "Tomorrow", count = summary.tomorrowCount,
                icon = Icons.Filled.Event, accentColor = MaterialTheme.colorScheme.tertiary,
                onClick = { navController.navigate(Screen.DeliveryList.passFilter(DeliveryFilter.TOMORROW.key)) }
            )
        }
        item {
            Spacer(Modifier.height(12.dp))
            DeliveryCard(
                title = "Overdue", count = summary.overdueCount,
                icon = Icons.Filled.Warning, accentColor = MaterialTheme.colorScheme.error,
                onClick = { navController.navigate(Screen.DeliveryList.passFilter(DeliveryFilter.OVERDUE.key)) }
            )
        }
        item {
            Spacer(Modifier.height(12.dp))
            DeliveryCard(
                title = "Delivered", count = summary.deliveredCount,
                icon = Icons.Filled.CheckCircle, accentColor = Color(0xFF2E7D32),
                onClick = { navController.navigate(Screen.DeliveryList.passFilter(DeliveryFilter.DELIVERED.key)) }
            )
        }
        item {
            Spacer(Modifier.height(12.dp))
            DeliveryCard(
                title = "All Deliveries", count = summary.allCount,
                icon = Icons.Filled.LocalShipping, accentColor = MaterialTheme.colorScheme.secondary,
                onClick = { navController.navigate(Screen.DeliveryList.passFilter(DeliveryFilter.ALL.key)) }
            )
        }

        item {
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun TaliKhataCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TaliKhata",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = "Track your credit ledger",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingFlat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun FeatureCard(
    feature: DashboardFeature,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.height(128.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(feature.tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = feature.tint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = feature.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}