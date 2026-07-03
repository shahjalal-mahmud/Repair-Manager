// app/src/main/java/com/appriyo/repairmanager/domain/cashbox/CashBoxFormat.kt
package com.appriyo.repairmanager.domain.cashbox

import java.util.Locale

/** Formats an amount as "৳ 10,000" (or "-৳ 3,000" for negative balances). */
fun formatTaka(amount: Double): String {
    val rounded = kotlin.math.abs(amount)
    val sign = if (amount < 0) "-" else ""
    return "$sign৳ ${String.format(Locale.US, "%,.0f", rounded)}"
}