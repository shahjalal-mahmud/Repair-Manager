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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Inventory2
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
import androidx.compose.ui.graphics.vector.ImageVector
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
    val icon: ImageVector,
    val route: String
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
            route = Screen.AddRepair.route
        ),
        DashboardFeature(
            title = "Customers",
            subtitle = "View all repairs",
            icon = Icons.AutoMirrored.Filled.List,
            route = Screen.CustomerList.route
        ),
        DashboardFeature(
            title = "Notes",
            subtitle = "Quick reminders",
            icon = Icons.AutoMirrored.Filled.Note,
            route = Screen.Notes.route
        ),
        DashboardFeature(
            title = "Employee",
            subtitle = "Manage your team",
            icon = Icons.Default.Person,
            route = Screen.Employee.route
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // -------- Header --------
        item {
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
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
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
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = "Today's deliveries"
                        )
                    }
                }
            }
        }

        // -------- Hero stats strip --------
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatTile(
                    modifier = Modifier.weight(1f),
                    label = "Today",
                    value = summary.todayCount,
                    icon = Icons.Filled.Today,
                    accent = MaterialTheme.colorScheme.primary
                )
                StatTile(
                    modifier = Modifier.weight(1f),
                    label = "Overdue",
                    value = summary.overdueCount,
                    icon = Icons.Filled.Warning,
                    accent = MaterialTheme.colorScheme.error
                )
                StatTile(
                    modifier = Modifier.weight(1f),
                    label = "All Jobs",
                    value = summary.allCount,
                    icon = Icons.Filled.Inventory2,
                    accent = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // -------- TaliKhata hero --------
        item {
            TaliKhataCard(onClick = { navController.navigate(Screen.TaliKhata.route) })
        }

        // -------- Quick Access --------
        item { SectionHeader("Quick Actions") }
        items(features.chunked(2)) { rowFeatures ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowFeatures.forEach { feature ->
                    FeatureTile(
                        feature = feature,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(feature.route) }
                    )
                }
                if (rowFeatures.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // -------- Cash Boxes --------
        item { SectionHeader("Cash Boxes") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CashBoxDashboardCard(
                    accountType = CashBoxType.CURRENT,
                    onClick = { navController.navigate(Screen.CashBox.passType(CashBoxType.CURRENT)) }
                )
                CashBoxDashboardCard(
                    accountType = CashBoxType.PRODUCT,
                    onClick = { navController.navigate(Screen.CashBox.passType(CashBoxType.PRODUCT)) }
                )
                CashBoxDashboardCard(
                    accountType = CashBoxType.MARKET,
                    onClick = { navController.navigate(Screen.CashBox.passType(CashBoxType.MARKET)) }
                )
            }
        }

        // -------- Deliveries --------
        item { SectionHeader("Deliveries") }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DeliveryCard(
                    title = "Today",
                    count = summary.todayCount,
                    icon = Icons.Filled.Today,
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        navController.navigate(Screen.DeliveryList.passFilter(DeliveryFilter.TODAY.key))
                    },
                    modifier = Modifier.weight(1f)
                )
                DeliveryCard(
                    title = "Tomorrow",
                    count = summary.tomorrowCount,
                    icon = Icons.Filled.Event,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    onClick = {
                        navController.navigate(Screen.DeliveryList.passFilter(DeliveryFilter.TOMORROW.key))
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DeliveryCard(
                    title = "Overdue",
                    count = summary.overdueCount,
                    icon = Icons.Filled.Warning,
                    accentColor = MaterialTheme.colorScheme.error,
                    onClick = {
                        navController.navigate(Screen.DeliveryList.passFilter(DeliveryFilter.OVERDUE.key))
                    },
                    modifier = Modifier.weight(1f)
                )
                DeliveryCard(
                    title = "Delivered",
                    count = summary.deliveredCount,
                    icon = Icons.Filled.CheckCircle,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        navController.navigate(Screen.DeliveryList.passFilter(DeliveryFilter.DELIVERED.key))
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            DeliveryCard(
                title = "All Deliveries",
                count = summary.allCount,
                icon = Icons.Filled.LocalShipping,
                accentColor = MaterialTheme.colorScheme.primary,
                onClick = {
                    navController.navigate(Screen.DeliveryList.passFilter(DeliveryFilter.ALL.key))
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

// -----------------------------------------------------------------------------
// Private composables
// -----------------------------------------------------------------------------

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun StatTile(
    label: String,
    value: Int,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TaliKhataCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TaliKhata",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = "Track your credit ledger",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun FeatureTile(
    feature: DashboardFeature,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = feature.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}