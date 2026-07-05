package com.appriyo.repairmanager.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import com.appriyo.repairmanager.domain.cashbox.CashBoxType
import com.appriyo.repairmanager.notifications.NotificationNavigator
import com.appriyo.repairmanager.presentation.screens.AddRepairScreen
import com.appriyo.repairmanager.presentation.screens.CashBoxScreen
import com.appriyo.repairmanager.presentation.screens.CustomerDetailsScreen
import com.appriyo.repairmanager.presentation.screens.CustomerListScreen
import com.appriyo.repairmanager.presentation.screens.DashboardScreen
import com.appriyo.repairmanager.presentation.screens.DeliveryListScreen
import com.appriyo.repairmanager.presentation.screens.EditRepairScreen
import com.appriyo.repairmanager.presentation.screens.EmployeeScreen
import com.appriyo.repairmanager.presentation.screens.LoginScreen
import com.appriyo.repairmanager.presentation.screens.NotesScreen
import com.appriyo.repairmanager.presentation.screens.ProductSellListScreen
import com.appriyo.repairmanager.presentation.screens.SmsSettingsScreen
import com.appriyo.repairmanager.presentation.screens.TaliKhataScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    val pendingRoute by NotificationNavigator.pendingRoute.collectAsState()
    LaunchedEffect(pendingRoute) {
        pendingRoute?.let { route ->
            navController.navigate(route)
            NotificationNavigator.consume()
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Auth screen (no bottom nav)
        composable(Screen.Login.route) {
            LoginScreen(navController)
        }

        // Bottom Navigation Screens
        composable(Screen.Dashboard.route) {
            DashboardScreen(navController)
        }

        composable(Screen.AddRepair.route) {
            AddRepairScreen(navController)
        }

        composable(Screen.CustomerList.route) {
            CustomerListScreen(navController)
        }

        composable(Screen.Notes.route) {
            NotesScreen()
        }

        composable(Screen.Employee.route) {
            EmployeeScreen()
        }

        composable(Screen.TaliKhata.route) {
            TaliKhataScreen()
        }

        // Detail screens (no bottom nav)
        composable(
            route = Screen.CustomerDetails.route,
            arguments = listOf(navArgument("repairId") { type = NavType.StringType })
        ) { backStackEntry ->
            val repairId = backStackEntry.arguments?.getString("repairId") ?: ""
            CustomerDetailsScreen(navController, repairId)
        }

        composable(
            route = Screen.EditRepair.route,
            arguments = listOf(navArgument("repairId") { type = NavType.StringType })
        ) { backStackEntry ->
            val repairId = backStackEntry.arguments?.getString("repairId") ?: ""
            EditRepairScreen(navController, repairId)
        }

        // SMS settings (new)
        composable(Screen.SmsSettings.route) {
            SmsSettingsScreen()
        }

        composable(
            route = Screen.DeliveryList.route,
            arguments = listOf(navArgument("filterType") { type = NavType.StringType })
        ) { backStackEntry ->
            val filterType = backStackEntry.arguments?.getString("filterType") ?: "all"
            DeliveryListScreen(navController, filterType)
        }

        // Cash Box Management (new) - single screen, driven by accountType argument
        composable(
            route = Screen.CashBox.route,
            arguments = listOf(navArgument("accountType") { type = NavType.StringType })
        ) { backStackEntry ->
            val typeKey = backStackEntry.arguments?.getString("accountType") ?: CashBoxType.PRODUCT.firestoreId
            CashBoxScreen(navController, CashBoxType.fromFirestoreId(typeKey))
        }

        // Product Sell / POS Invoice (new)
        composable(Screen.ProductSellList.route) {
            ProductSellListScreen(navController)
        }
    }
}