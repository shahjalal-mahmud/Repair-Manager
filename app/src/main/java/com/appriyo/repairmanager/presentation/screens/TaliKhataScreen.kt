package com.appriyo.repairmanager.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.appriyo.repairmanager.presentation.components.talikhata.TaliKhataAddEditDialog
import com.appriyo.repairmanager.presentation.components.talikhata.TaliKhataDetailBottomSheet
import com.appriyo.repairmanager.presentation.components.talikhata.TaliKhataEmptyState
import com.appriyo.repairmanager.presentation.components.talikhata.TaliKhataEntryCard
import com.appriyo.repairmanager.presentation.components.talikhata.TaliKhataFilterChips
import com.appriyo.repairmanager.presentation.components.talikhata.TaliKhataSearchField
import com.appriyo.repairmanager.presentation.components.talikhata.TaliKhataSortMenu
import com.appriyo.repairmanager.presentation.components.talikhata.TaliKhataSummaryCards
import com.appriyo.repairmanager.presentation.utils.openSmsComposer
import com.appriyo.repairmanager.presentation.viewmodel.TaliKhataEvent
import com.appriyo.repairmanager.presentation.viewmodel.TaliKhataViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Main TaliKhata screen.
 *
 * This composable is UI-only: it reads [TaliKhataViewModel.uiState] and
 * forwards every user action to the ViewModel. No Firestore or SMS logic
 * lives here.
 *
 * The screen collects one-off events from [TaliKhataViewModel.events]:
 *   - `OpenSms` → forwards to `openSmsComposer(...)` which opens the device's
 *                 default SMS app with the conversation and pre-written body.
 *                 The user reviews and taps Send manually.
 */
@Composable
fun TaliKhataScreen(
    viewModel: TaliKhataViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is TaliKhataEvent.OpenSms) {
                openSmsComposer(
                    context = context,
                    phoneNumber = event.phoneNumber,
                    message = event.message
                )
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
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
        ) {
            // Header - kept inside the screen-wide 16.dp padding.
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
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
                    // No horizontal contentPadding here - the parent already
                    // gives the list 16.dp on each side. Previously the list
                    // added 16.dp AGAIN, so cards were indented 32.dp from
                    // the screen edges.
                    LazyColumn(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = 96.dp
                        )
                    ) {
                        items(uiState.filteredEntries, key = { it.id }) { entry ->
                            TaliKhataEntryCard(
                                entry = entry,
                                onClick = { viewModel.onEntryClick(entry) },
                                onSmsClick = { viewModel.onSmsClick(entry) },
                                onEditClick = { viewModel.onEditEntryClick(entry) },
                                onDeleteClick = { viewModel.onDeleteEntryClick(entry) }
                            )
                        }
                    }
                }
            }
        }
    }

    // ModalBottomSheet and AlertDialog must live OUTSIDE the Scaffold content
    // lambda. They are window-level popups and crash on Android 15+ (SDK 36)
    // when composed as children of the Scaffold's content layout, because the
    // stricter anchor/lookahead checks in Compose BOM 2026.04.x fail under
    // the enforced edge-to-edge insets.
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

    // Use liveSelectedEntry so the sheet picks up balance updates from the
    // realtime Firestore listener. If the entry was deleted out from under us,
    // the sheet just dismisses itself.
    val liveEntry = uiState.liveSelectedEntry
    val selectedId = uiState.selectedEntryId
    LaunchedEffect(selectedId, liveEntry?.id) {
        if (selectedId != null && liveEntry == null) {
            viewModel.onDismissDetail()
        }
    }
    if (selectedId != null && liveEntry != null) {
        TaliKhataDetailBottomSheet(
            entry = liveEntry,
            history = uiState.history,
            onDismiss = { viewModel.onDismissDetail() }
        )
    }

    uiState.pendingDeleteEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { viewModel.onCancelDelete() },
            title = { Text("Delete entry?") },
            text = {
                Text(
                    "This will permanently remove ${entry.personName} and all of " +
                            "their history. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onConfirmDelete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onCancelDelete() }) {
                    Text("Cancel")
                }
            }
        )
    }
}