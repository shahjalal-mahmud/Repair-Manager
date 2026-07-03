// app/src/main/java/com/appriyo/repairmanager/domain/cashbox/CashBoxSummary.kt
package com.appriyo.repairmanager.domain.cashbox

import java.util.Date

/**
 * Aggregated snapshot stored at users/{uid}/cashBoxes/{boxId}.
 * Kept up to date by CashBoxRepository on every add/update/delete.
 */
data class CashBoxSummary(
    val currentBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val transactionCount: Int = 0,
    val updatedAt: Date? = null
)