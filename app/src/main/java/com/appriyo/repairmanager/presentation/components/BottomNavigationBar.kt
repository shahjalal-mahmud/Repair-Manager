// app/src/main/java/com/appriyo/repairmanager/presentation/components/BottomNavigationBar.kt
package com.appriyo.repairmanager.presentation.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.appriyo.repairmanager.navigation.BottomNavItem
import com.appriyo.repairmanager.navigation.Screen

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.AddCustomer,
        BottomNavItem.Customers,
        BottomNavItem.Notes,
        BottomNavItem.Employee
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide bottom nav for detail screens and auth screen
    val shouldShowBottomNav = when (currentRoute) {
        Screen.CustomerDetails.route -> false
        Screen.EditRepair.route -> false
        Screen.Login.route -> false
        Screen.SmsSettings.route -> false
        else -> true
    }

    if (shouldShowBottomNav) {
        NavigationBar(
            modifier = modifier
        ) {
            items.forEach { item ->
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label
                        )
                    },
                    label = { Text(item.label) },
                    selected = currentRoute == item.route,
                    onClick = {
                        navController.navigate(item.route) {
                            // Avoid multiple copies of the same destination
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}