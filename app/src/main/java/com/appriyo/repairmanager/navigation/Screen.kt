package com.appriyo.repairmanager.navigation

sealed class Screen(val route: String) {
    // Auth screens
    object Login : Screen("login")

    // Bottom Navigation Screens
    object Dashboard : Screen("dashboard")
    object AddRepair : Screen("add_repair")
    object CustomerList : Screen("customer_list")
    object Notes : Screen("notes")
    object Employee : Screen("employee")
    object TaliKhata : Screen("talikhata")

    // Detail screens (not part of bottom nav)
    object CustomerDetails : Screen("customer_details/{repairId}") {
        fun passId(repairId: String) = "customer_details/$repairId"
    }
    object EditRepair : Screen("edit_repair/{repairId}") {
        fun passId(repairId: String) = "edit_repair/$repairId"
    }

    // SMS settings (new)
    object SmsSettings : Screen("sms_settings")
}