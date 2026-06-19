package com.appriyo.repairmanager.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.appriyo.repairmanager.presentation.screens.AddRepairScreen
import com.appriyo.repairmanager.presentation.screens.CustomerDetailsScreen
import com.appriyo.repairmanager.presentation.screens.CustomerListScreen
import com.appriyo.repairmanager.presentation.screens.DashboardScreen
import com.appriyo.repairmanager.presentation.screens.LoginScreen
import com.appriyo.repairmanager.presentation.screens.SplashScreen

@Composable
fun NavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController)
        }

        composable(Screen.Login.route) {
            LoginScreen(navController)
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(navController)
        }

        composable(Screen.AddRepair.route) {
            AddRepairScreen(navController)
        }

        composable(Screen.CustomerList.route) {
            CustomerListScreen(navController)
        }

        composable(
            route = Screen.CustomerDetails.route,
            arguments = listOf(navArgument("repairId") { type = NavType.StringType })
        ) { backStackEntry ->
            val repairId = backStackEntry.arguments?.getString("repairId") ?: ""
            CustomerDetailsScreen(navController, repairId)
        }
    }
}