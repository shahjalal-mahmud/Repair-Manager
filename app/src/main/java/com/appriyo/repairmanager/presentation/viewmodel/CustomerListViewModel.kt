// app/src/main/java/com/appriyo/repairmanager/presentation/viewmodel/CustomerDetailsViewModel.kt
package com.appriyo.repairmanager.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.repairmanager.data.media.MediaRepository
import com.appriyo.repairmanager.data.repository.RepairRepository
import com.appriyo.repairmanager.presentation.state.CustomerDetailsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the CustomerDetailsScreen.
 * Loads a single repair record and its associated media attachments.
 */
class CustomerDetailsViewModel(
    private val repairRepository: RepairRepository,
    context: Context
) : ViewModel() {

    private val mediaRepository = MediaRepository(context)

    private val _uiState = MutableStateFlow(CustomerDetailsUiState())
    val uiState = _uiState.asStateFlow()

    /**
     * Load a repair by its ID and then load all associated media attachments.
     */
    fun loadRepair(repairId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repairRepository.getRepair(repairId)
            result.fold(
                onSuccess = { repair ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            repair = repair,
                            errorMessage = null
                        )
                    }
                    // Load all media for this repair in the background
                    if (repair?.draftId?.isNotBlank() == true) {
                        loadMedia(repair.draftId)
                    } else {
                        // No draftId means no media to load
                        _uiState.update { it.copy(isLoadingMedia = false) }
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.localizedMessage ?: "Could not load repair."
                        )
                    }
                }
            )
        }
    }

    /**
     * Load all media attachments for a given draftId.
     * This loads full-resolution media (not thumbnails) for the detail view.
     */
    private fun loadMedia(draftId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingMedia = true, errorMessage = null) }
                val attachments = mediaRepository.loadAttachments(draftId)
                _uiState.update {
                    it.copy(
                        attachments = attachments,
                        isLoadingMedia = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingMedia = false,
                        errorMessage = e.localizedMessage ?: "Failed to load media attachments."
                    )
                }
            }
        }
    }

    /**
     * Update the status of a repair and refresh the local state.
     */
    fun updateStatus(repairId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isUpdatingStatus = true, errorMessage = null) }
                val result = repairRepository.updateStatus(repairId, newStatus)
                result.fold(
                    onSuccess = {
                        _uiState.update { state ->
                            state.copy(
                                isUpdatingStatus = false,
                                repair = state.repair?.copy(status = newStatus)
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                isUpdatingStatus = false,
                                errorMessage = e.localizedMessage ?: "Failed to update status."
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUpdatingStatus = false,
                        errorMessage = e.localizedMessage ?: "An unexpected error occurred."
                    )
                }
            }
        }
    }

    /**
     * Delete a repair and mark the state as deleted.
     */
    fun deleteRepair(repairId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val result = repairRepository.deleteRepair(repairId)
                result.fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isDeleted = true,
                                repair = null
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = e.localizedMessage ?: "Delete failed."
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "An unexpected error occurred."
                    )
                }
            }
        }
    }

    /**
     * Consume and clear any error message in the state.
     */
    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}