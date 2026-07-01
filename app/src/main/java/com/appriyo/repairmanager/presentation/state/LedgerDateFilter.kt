// app/src/main/java/com/appriyo/repairmanager/presentation/state/LedgerDateFilter.kt
package com.appriyo.repairmanager.presentation.state

/** Period filters for the Daily Work Ledger screen. */
enum class LedgerDateFilter(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    CUSTOM_DATE("Custom Date"),
    CUSTOM_RANGE("Custom Range")
}