package com.appriyo.repairmanager.presentation.screens.talikhata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appriyo.repairmanager.data.model.TaliKhataEntry
import com.appriyo.repairmanager.presentation.components.talikhata.TaliKhataAddEditDialog
import com.appriyo.repairmanager.presentation.components.talikhata.TaliKhataDetailBottomSheet
import com.appriyo.repairmanager.presentation.components.talikhata.TaliKhataEmptyState
import com.appriyo.repairmanager.presentation.components.talikhata.TaliKhataEntryCard
import com.appriyo.repairmanager.presentation.components.talikhata.TaliKhataFilterChips
import com.appriyo.repairmanager.presentation.components.talikhata.TaliKhataSearchField
import com.appriyo.repairmanager.presentation.components.talikhata.TaliKhataSortMenu
import com.appriyo.repairmanager.presentation.components.talikhata.TaliKhataSummaryCards
import com.appriyo.repairmanager.presentation.viewmodel.TaliKhataViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Main TaliKhata screen.
 *
 * This composable is UI-only: it reads [TaliKhataViewModel.uiState] and
 * forwards every user action to the ViewModel. No Firestore, media, or SMS
 * logic lives here.
 *
 * NOTE ON VIEWMODEL API: the exact method names below (onSearchQueryChange,
 * onFilterChange, onSortOptionChange, onAddEntryClick, onEditEntryClick,
 * onDeleteEntryClick, onEntryClick, onDismissAddEditDialog, onSaveEntry,
 * onDismissDetail, onAddPhotoClick, onPhotosClick, onSmsClick) are the
 * expected surface this screen needs from TaliKhataViewModel. If your actual
 * ViewModel uses different names, rename the calls below to match - the
 * screen makes no other assumptions about ViewModel internals.
 */
@Composable
fun TaliKhataScreen(
    viewModel: TaliKhataViewModel = koinViewModel(),
    onPhotosClick: (TaliKhataEntry) -> Unit = { viewModel.onPhotosClick(it) },
    onSmsClick: (TaliKhataEntry) -> Unit = { viewModel.onSmsClick(it) }
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.onAddEntryClick() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Entry") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "TaliKhata",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Track money you owe and money owed to you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            TaliKhataSummaryCards(
                summary = uiState.summary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            TaliKhataSearchField(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TaliKhataFilterChips(
                    selected = uiState.filter,
                    onFilterSelected = { viewModel.onFilterChange(it) },
                    modifier = Modifier.weight(1f)
                )
                TaliKhataSortMenu(
                    selected = uiState.sortOption,
                    onSortSelected = { viewModel.onSortOptionChange(it) }
                )
            }

            when {
                uiState.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.filteredEntries.isEmpty() -> {
                    TaliKhataEmptyState(modifier = Modifier.fillMaxSize())
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 8.dp,
                            end = 16.dp,
                            bottom = 96.dp
                        )
                    ) {
                        items(uiState.filteredEntries, key = { it.id }) { entry ->
                            TaliKhataEntryCard(
                                entry = entry,
                                onClick = { viewModel.onEntryClick(entry) },
                                onPhotosClick = { onPhotosClick(entry) },
                                onSmsClick = { onSmsClick(entry) },
                                onEditClick = { viewModel.onEditEntryClick(entry) },
                                onDeleteClick = { viewModel.onDeleteEntryClick(entry) }
                            )
                        }
                    }
                }
            }
        }

        if (uiState.showAddEditDialog) {
            TaliKhataAddEditDialog(
                entry = uiState.entryBeingEdited,
                isSaving = uiState.isSaving,
                onDismiss = { viewModel.onDismissAddEditDialog() },
                onSave = { name, phone, details, type, amount, isIncrease ->
                    viewModel.onSaveEntry(name, phone, details, type, amount, isIncrease)
                }
            )
        }

        uiState.selectedEntry?.let { entry ->
            TaliKhataDetailBottomSheet(
                entry = entry,
                history = uiState.history,
                photoUris = emptyList(), // supplied by ViewModel/MediaRepository in real wiring
                onDismiss = { viewModel.onDismissDetail() },
                onAddPhotoClick = { viewModel.onAddPhotoClick(entry) }
            )
        }
    }
}