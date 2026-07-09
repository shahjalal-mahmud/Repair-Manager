// app/src/main/java/com/appriyo/repairmanager/presentation/state/ProductSellUiState.kt
package com.appriyo.repairmanager.presentation.state

import com.appriyo.repairmanager.data.model.ProductSell
import java.util.Date

/**
 * Which date range the product sell list is currently showing.
 */
enum class ProductSellDateFilter {
    TODAY,
    ALL,
    SPECIFIC_DATE
}

/**
 * UI state for the Product Sell / Invoice screen.
 *
 * Mirrors the [AddRepairUiState] pattern so the screen can expose
 * "Save only" vs "Save & Print" flows and react to bluetooth permission
 * requests the same way the existing Add Repair flow does.
 */
data class ProductSellUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val generatedSerialNumber: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val printSuccess: Boolean? = null,
    val printErrorMessage: String? = null,
    val missingPermissions: List<String> = emptyList(),

    /** Realtime list of ALL product sell records, newest first (unfiltered). */
    val productSells: List<ProductSell> = emptyList(),

    /** Free-text search applied on top of the date filter. */
    val searchQuery: String = "",

    /** Which date range is currently applied to the list. Defaults to TODAY. */
    val dateFilter: ProductSellDateFilter = ProductSellDateFilter.TODAY,

    /** Only meaningful when [dateFilter] == SPECIFIC_DATE. */
    val selectedDate: Date = Date()
)