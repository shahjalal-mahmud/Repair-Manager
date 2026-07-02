package com.appriyo.repairmanager.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    // Default landing tab - the dashboard.
    object Home : BottomNavItem(
        route = Screen.Dashboard.route,
        icon = Icons.Default.Home,
        label = "Home"
    )

    object AddCustomer : BottomNavItem(
        route = Screen.AddRepair.route,
        icon = Icons.Default.Add,
        label = "Add"
    )

    object Customers : BottomNavItem(
        route = Screen.CustomerList.route,
        icon = Icons.AutoMirrored.Filled.List,
        label = "Customers"
    )

    object Notes : BottomNavItem(
        route = Screen.Notes.route,
        icon = Icons.AutoMirrored.Filled.Note,
        label = "Notes"
    )

    object Employee : BottomNavItem(
        route = Screen.Employee.route,
        icon = Icons.Default.Person,
        label = "Employee"
    )

    // No longer shown in the bottom nav - accessible from the dashboard instead.
    object TaliKhata : BottomNavItem(
        route = Screen.TaliKhata.route,
        icon = Icons.Default.AccountBalanceWallet,
        label = "TaliKhata"
    )
}