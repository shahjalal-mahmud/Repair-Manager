package com.appriyo.repairmanager.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object AddRepair : Screen("add_repair")
    object CustomerList : Screen("customer_list")
    object CustomerDetails : Screen("customer_details/{repairId}") {
        fun passId(repairId: String) = "customer_details/$repairId"
    }
}