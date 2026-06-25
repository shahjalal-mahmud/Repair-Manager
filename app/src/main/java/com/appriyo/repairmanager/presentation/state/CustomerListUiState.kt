// app/src/main/java/com/appriyo/repairmanager/presentation/state/CustomerListUiState.kt
package com.appriyo.repairmanager.presentation.state

import com.appriyo.repairmanager.data.model.Repair

data class CustomerListUiState(
    val isLoading: Boolean = true,
    val repairs: List<Repair> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null
) {
    /**
     * Client-side filter by customer name, phone number, serial number, or device model.
     * Kept client-side per Phase 1 scope - no need for a search index on the free tier.
     */
    val filteredRepairs: List<Repair>
        get() {
            if (searchQuery.isBlank()) return repairs
            val query = searchQuery.trim().lowercase()
            return repairs.filter { repair ->
                repair.customerName.lowercase().contains(query) ||
                        repair.phoneNumber.lowercase().contains(query) ||
                        repair.serialNumber.lowercase().contains(query) ||
                        repair.deviceModel.lowercase().contains(query)
            }
        }
}