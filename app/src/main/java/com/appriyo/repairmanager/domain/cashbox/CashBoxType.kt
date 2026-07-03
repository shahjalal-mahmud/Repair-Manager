// app/src/main/java/com/appriyo/repairmanager/domain/cashbox/CashBoxType.kt
package com.appriyo.repairmanager.domain.cashbox

/**
 * The two manual cash accounts supported by the Cash Box Management feature.
 * [firestoreId] is the literal document id under `users/{uid}/cashBoxes/{firestoreId}`.
 */
enum class CashBoxType(
    val firestoreId: String,
    val displayName: String,
    val subtitle: String
) {
    PRODUCT(
        firestoreId = "product",
        displayName = "Product Box",
        subtitle = "Cash used for buying products"
    ),
    MARKET(
        firestoreId = "market",
        displayName = "Market Box",
        subtitle = "Cash used for market & daily expenses"
    );

    companion object {
        fun fromFirestoreId(id: String): CashBoxType =
            entries.firstOrNull { it.firestoreId == id } ?: PRODUCT
    }
}