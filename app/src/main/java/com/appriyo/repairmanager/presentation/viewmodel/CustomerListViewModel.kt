// app/src/main/java/com/appriyo/repairmanager/presentation/viewmodel/CustomerListViewModel.kt
package com.appriyo.repairmanager.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.repairmanager.data.media.MediaRepository
import com.appriyo.repairmanager.data.media.loadMediaThumbnail
import com.appriyo.repairmanager.data.model.Repair
import com.appriyo.repairmanager.data.repository.RepairRepository
import com.appriyo.repairmanager.presentation.state.CustomerListUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the CustomerListScreen.
 * Subscribes to a single realtime Firestore listener for the whole list;
 * search is done client-side against that already-loaded list.
 */
class CustomerListViewModel(
    private val repairRepository: RepairRepository,
    private val context: Context
) : ViewModel() {

    private val mediaRepository = MediaRepository(context)

    private val _uiState = MutableStateFlow(CustomerListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeRepairs()
    }

    private fun observeRepairs() {
        repairRepository.observeRepairs()
            .onEach { repairs ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        repairs = repairs,
                        errorMessage = null
                    )
                }
                // Load thumbnails after updating the repairs list
                loadThumbnails(repairs)
            }
            .catch { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.localizedMessage ?: "Failed to load repair records."
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * For every repair that has a draftId and at least one photo, load the
     * first-photo thumbnail in the background and push it into state.
     * Already-loaded thumbnails are skipped to avoid redundant IO.
     */
    private fun loadThumbnails(repairs: List<Repair>) {
        repairs
            .filter { it.draftId.isNotBlank() && it.photoCount > 0 }
            .forEach { repair ->
                // Skip if thumbnail is already loaded
                if (_uiState.value.thumbnails.containsKey(repair.id)) return@forEach

                viewModelScope.launch {
                    try {
                        val attachment = mediaRepository.loadFirstPhoto(repair.draftId)
                        val bitmap = attachment?.let { loadMediaThumbnail(context, it.uri) }
                        if (bitmap != null) {
                            _uiState.update { state ->
                                state.copy(thumbnails = state.thumbnails + (repair.id to bitmap))
                            }
                        }
                    } catch (e: Exception) {
                        // Log error but don't crash - thumbnail loading is non-critical
                        // Consider adding logging here: Log.e("CustomerListVM", "Failed to load thumbnail", e)
                    }
                }
            }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateStatus(repairId: String, newStatus: String) {
        viewModelScope.launch {
            val result = repairRepository.updateStatus(repairId, newStatus)
            result.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        errorMessage = exception.localizedMessage ?: "Failed to update status."
                    )
                }
            }
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}