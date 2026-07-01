// app/src/main/java/com/appriyo/repairmanager/presentation/state/LedgerSummary.kt
package com.appriyo.repairmanager.presentation.state

import com.appriyo.repairmanager.data.model.WorkerType

/** Aggregated totals for a single worker within the selected period. */
data class WorkerStats(
    val workerType: WorkerType,
    val entryCount: Int = 0,
    val totalPayment: Double = 0.0,
    val totalProfit: Double = 0.0
) {
    /** Cost is never entered manually - always derived as Payment - Profit. */
    val totalCost: Double
        get() = totalPayment - totalProfit
}

/** Aggregated shop-wide totals for the selected period. Computed once in the ViewModel. */
data class LedgerSummary(
    val totalEntries: Int = 0,
    val totalPayment: Double = 0.0,
    val totalProfit: Double = 0.0,
    val workerAStats: WorkerStats = WorkerStats(WorkerType.A),
    val workerBStats: WorkerStats = WorkerStats(WorkerType.B)
) {
    val totalCost: Double
        get() = totalPayment - totalProfit
}