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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the CustomerDetailsScreen.
 * Subscribes to a realtime listener on a single repair document, so status
 * changes made here or on another device are reflected immediately.
 */
class CustomerDetailsViewModel(
    private val repairRepository: RepairRepository,
    private val context: Context
) : ViewModel() {

    private val mediaRepository = MediaRepository(context)

    private val _uiState = MutableStateFlow(CustomerDetailsUiState())
    val uiState = _uiState.asStateFlow()

    private var currentRepairId: String? = null

    fun loadRepair(repairId: String) {
        // Store the current repair ID for media loading
        currentRepairId = repairId

        // Reset media state when loading a new repair
        _uiState.update {
            it.copy(
                isLoading = true,
                attachments = emptyList(),
                isLoadingMedia = false
            )
        }

        repairRepository.observeRepair(repairId)
            .onEach { repair ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        repair = repair,
                        errorMessage = null
                    )
                }
                // Load all media for this repair in the background
                if (repair?.draftId?.isNotBlank() ?: true) {
                    repair?.draftId?.let { loadMedia(it) }
                } else {
                    // No draftId means no media to load
                    _uiState.update { it.copy(isLoadingMedia = false) }
                }
            }
            .catch { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.localizedMessage ?: "Failed to load repair record."
                    )
                }
            }
            .launchIn(viewModelScope)
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

    fun updateStatus(repairId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isUpdatingStatus = true, errorMessage = null) }
                val result = repairRepository.updateStatus(repairId, newStatus)
                result.fold(
                    onSuccess = {
                        // The repair will be updated via the observer
                        _uiState.update { it.copy(isUpdatingStatus = false) }
                    },
                    onFailure = { exception ->
                        _uiState.update {
                            it.copy(
                                isUpdatingStatus = false,
                                errorMessage = exception.localizedMessage ?: "Failed to update status."
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

    fun deleteRepair(repairId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(errorMessage = null) }
                val result = repairRepository.deleteRepair(repairId)
                result.fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                isDeleted = true,
                                repair = null,
                                attachments = emptyList()
                            )
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update {
                            it.copy(
                                errorMessage = exception.localizedMessage ?: "Failed to delete repair record."
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = e.localizedMessage ?: "An unexpected error occurred."
                    )
                }
            }
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}