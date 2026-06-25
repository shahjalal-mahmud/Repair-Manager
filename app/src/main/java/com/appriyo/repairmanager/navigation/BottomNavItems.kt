// app/src/main/java/com/appriyo/repairmanager/navigation/BottomNavItems.kt
package com.appriyo.repairmanager.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object AddCustomer : BottomNavItem(
        route = Screen.AddRepair.route,
        icon = Icons.Default.Add,
        label = "Add Customer"
    )

    object Customers : BottomNavItem(
        route = Screen.CustomerList.route,
        icon = Icons.Default.List,
        label = "Customers"
    )

    object Notes : BottomNavItem(
        route = Screen.Notes.route,
        icon = Icons.Default.Note,
        label = "Notes"
    )

    object Employee : BottomNavItem(
        route = Screen.Employee.route,
        icon = Icons.Default.Person,
        label = "Employee"
    )
}