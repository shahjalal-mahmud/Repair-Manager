// app/src/main/java/com/appriyo/repairmanager/domain/cashbox/CashTransaction.kt
package com.appriyo.repairmanager.domain.cashbox

import java.util.Date

/**
 * A single manual entry inside a cash box, stored at:
 * users/{uid}/cashBoxes/{boxId}/transactions/{id}
 */
data class CashTransaction(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val type: TransactionType = TransactionType.INCOME,
    val createdAt: Date? = null
) {
    /** Positive for income, negative for expense - used for balance math. */
    val signedAmount: Double
        get() = if (type == TransactionType.INCOME) amount else -amount
}