package com.appriyo.repairmanager.navigation

import com.appriyo.repairmanager.domain.cashbox.CashBoxType

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

    object DeliveryList : Screen("delivery_list/{filterType}") {
        fun passFilter(filterKey: String) = "delivery_list/$filterKey"
    }

    // Cash Box Management (new) - one screen, reused for both Product/Market boxes
    object CashBox : Screen("cash_box/{accountType}") {
        fun passType(type: CashBoxType) = "cash_box/${type.firestoreId}"
    }
}