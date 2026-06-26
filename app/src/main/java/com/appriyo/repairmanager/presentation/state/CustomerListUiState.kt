// app/src/main/java/com/appriyo/repairmanager/presentation/state/CustomerListUiState.kt
package com.appriyo.repairmanager.presentation.state

import android.graphics.Bitmap
import com.appriyo.repairmanager.data.model.Repair

data class CustomerListUiState(
    val isLoading: Boolean = true,
    val repairs: List<Repair> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null,
    // keyed by repair.id → first photo thumbnail (null = no photo or not yet loaded)
    val thumbnails: Map<String, Bitmap?> = emptyMap()
) {
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