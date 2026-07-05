// app/src/main/java/com/appriyo/repairmanager/presentation/screens/EmployeeScreen.kt
@file:OptIn(ExperimentalMaterial3Api::class)

package com.appriyo.repairmanager.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.appriyo.repairmanager.data.model.EmployeeNote
import com.appriyo.repairmanager.data.model.WorkerType
import com.appriyo.repairmanager.presentation.components.DeleteConfirmationDialog
import com.appriyo.repairmanager.presentation.components.PinProtectedAction
import com.appriyo.repairmanager.presentation.components.TopToastHost
import com.appriyo.repairmanager.presentation.state.LedgerSummary
import com.appriyo.repairmanager.presentation.state.LedgerViewMode
import com.appriyo.repairmanager.presentation.state.WorkerStats
import com.appriyo.repairmanager.presentation.utils.LedgerDateUtils
import com.appriyo.repairmanager.presentation.viewmodel.EmployeeNotesViewModel
import com.appriyo.repairmanager.presentation.viewmodel.SecurityViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun EmployeeScreen(
    viewModel: EmployeeNotesViewModel = koinViewModel(),
    securityViewModel: SecurityViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Money-value visibility. Locked by default so employees cannot see
    // Payment / Profit / Cost. The owner unlocks with the same 6-digit PIN
    // used elsewhere. State is local to this composable, so leaving and
    // returning to the screen automatically re-locks the values.
    var valuesUnlocked by remember { mutableStateOf(false) }

    // Single PIN gate for edit/delete actions. Adding (FAB) is NOT gated:
    // anyone can create new entries, but only the owner (PIN) can edit or
    // delete existing ones.
    val pinGate = PinProtectedAction(securityViewModel)

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                // Anyone can add new entries. No PIN gate on create.
                FloatingActionButton(
                    onClick = { viewModel.openAddDialog() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Entry")
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // ---- Top bar: title + month/day selector + visibility toggle + search toggle ----
                TopBar(
                    viewMode = uiState.viewMode,
                    selectedDate = uiState.selectedDate,
                    selectedMonthStart = uiState.selectedMonthStart,
                    isSearchActive = uiState.isSearchActive,
                    valuesUnlocked = valuesUnlocked,
                    onPickDate = { viewModel.openDatePicker() },
                    onPickMonth = { viewModel.openMonthPicker() },
                    onSwitchToDay = { viewModel.switchToDayView() },
                    onSwitchToMonth = { viewModel.switchToMonthView() },
                    onToggleSearch = { viewModel.toggleSearch() },
                    onToggleValuesVisibility = {
                        if (valuesUnlocked) {
                            // Already unlocked - tap to re-lock immediately.
                            valuesUnlocked = false
                        } else {
                            // Require PIN to unlock.
                            pinGate.prompt { valuesUnlocked = true }
                        }
                    }
                )

                // ---- Search field (only when toggled on) ----
                if (uiState.isSearchActive) {
                    SearchField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        onClose = { viewModel.closeSearch() }
                    )
                }

                if (uiState.isLoading) {
                    LoadingState()
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(top = 6.dp, bottom = 96.dp)
                    ) {
                        // ---- Compact dashboard (single row of 4 stats) ----
                        item {
                            CompactSummaryRow(
                                summary = uiState.summary,
                                valuesUnlocked = valuesUnlocked
                            )
                            Spacer(Modifier.height(6.dp))
                        }

                        // ---- Worker breakdown (single compact row) ----
                        item {
                            CompactWorkerBreakdown(
                                summary = uiState.summary,
                                valuesUnlocked = valuesUnlocked
                            )
                            Spacer(Modifier.height(8.dp))
                            EntriesHeader(count = uiState.filteredNotes.size)
                            Spacer(Modifier.height(6.dp))
                        }

                        // ---- Entries ----
                        if (uiState.filteredNotes.isEmpty()) {
                            item {
                                EmptyEmployeeState(
                                    isSearching = uiState.searchQuery.isNotBlank(),
                                    isMonthView = uiState.viewMode == LedgerViewMode.MONTH
                                )
                            }
                        } else {
                            items(uiState.filteredNotes, key = { it.id }) { note ->
                                CompactEntryRow(
                                    note = note,
                                    valuesUnlocked = valuesUnlocked,
                                    onEdit = {
                                        pinGate.prompt { viewModel.openEditDialog(note) }
                                    },
                                    onDelete = {
                                        pinGate.prompt { viewModel.requestDeleteNote(note) }
                                    }
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }

        TopToastHost(
            toast = uiState.toast,
            onConsumed = { viewModel.consumeToast() },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    if (uiState.isDialogOpen) {
        WorkLedgerEntryDialog(
            initialWorker = WorkerType.fromStorageValue(
                uiState.editingNote?.workerType ?: WorkerType.A.storageValue
            ),
            initialTitle = uiState.editingNote?.title ?: "",
            initialDescription = uiState.editingNote?.description ?: "",
            initialTotalPayment = uiState.editingNote?.totalPayment?.let { formatAmount(it) } ?: "",
            initialProfit = uiState.editingNote?.profit?.let { formatAmount(it) } ?: "",
            isSaving = uiState.isSaving,
            titleError = uiState.titleError,
            isEditing = uiState.editingNote != null,
            onDismiss = { viewModel.dismissDialog() },
            onSave = { worker, title, description, totalPayment, profit ->
                viewModel.saveNote(title, description, totalPayment, profit, worker)
            }
        )
    }

    uiState.noteToDelete?.let { note ->
        DeleteConfirmationDialog(
            title = "Delete entry?",
            message = "\"${note.title}\" will be permanently removed. This can't be undone.",
            isDeleting = uiState.isDeleting,
            onConfirm = { viewModel.confirmDeleteNote() },
            onDismiss = { viewModel.dismissDeleteConfirmation() }
        )
    }

    if (uiState.showDatePicker) {
        LedgerDatePickerDialog(
            initialDate = uiState.selectedDate,
            onDismiss = { viewModel.dismissDatePicker() },
            onConfirm = { viewModel.onDateSelected(it) }
        )
    }

    if (uiState.showMonthPicker) {
        LedgerMonthPickerDialog(
            initialMonthStart = uiState.selectedMonthStart,
            onDismiss = { viewModel.dismissMonthPicker() },
            onConfirm = { viewModel.onMonthSelected(it) }
        )
    }
}

// ---------------------------------------------------------------------
// Top bar (single line: title · month selector · search icon)
// ---------------------------------------------------------------------

@Composable
private fun TopBar(
    viewMode: LedgerViewMode,
    selectedDate: Date,
    selectedMonthStart: Date,
    isSearchActive: Boolean,
    valuesUnlocked: Boolean,
    onPickDate: () -> Unit,
    onPickMonth: () -> Unit,
    onSwitchToDay: () -> Unit,
    onSwitchToMonth: () -> Unit,
    onToggleSearch: () -> Unit,
    onToggleValuesVisibility: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Ledger",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            // Inline date/month pill that opens the respective picker
            DateOrMonthPill(
                viewMode = viewMode,
                selectedDate = selectedDate,
                selectedMonthStart = selectedMonthStart,
                onPickDate = onPickDate,
                onPickMonth = onPickMonth,
                onSwitchToDay = onSwitchToDay,
                onSwitchToMonth = onSwitchToMonth
            )
        }
        // Toggle for hiding / showing money values. Eye-open when values are
        // masked (locked), eye-off when values are revealed.
        IconButton(onClick = onToggleValuesVisibility) {
            Icon(
                imageVector = if (valuesUnlocked) Icons.Filled.VisibilityOff else Icons.Filled.RemoveRedEye,
                contentDescription = if (valuesUnlocked) "Hide money values" else "Show money values",
                tint = if (valuesUnlocked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = onToggleSearch) {
            Icon(
                imageVector = if (isSearchActive) Icons.Filled.Close else Icons.Filled.Search,
                contentDescription = if (isSearchActive) "Close search" else "Open search",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DateOrMonthPill(
    viewMode: LedgerViewMode,
    selectedDate: Date,
    selectedMonthStart: Date,
    onPickDate: () -> Unit,
    onPickMonth: () -> Unit,
    onSwitchToDay: () -> Unit,
    onSwitchToMonth: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val label = when (viewMode) {
        LedgerViewMode.SINGLE_DAY -> formatShortDate(selectedDate)
        LedgerViewMode.MONTH -> formatMonthYear(selectedMonthStart)
    }
    val icon = if (viewMode == LedgerViewMode.MONTH)
        Icons.Filled.CalendarMonth else Icons.Filled.CalendarToday

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { menuOpen = true }
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false }
        ) {
            DropdownMenuItem(
                text = { Text("Today") },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Today,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = {
                    menuOpen = false
                    onSwitchToDay()
                    onPickDate()
                }
            )
            DropdownMenuItem(
                text = { Text("Pick a day") },
                leadingIcon = {
                    Icon(
                        Icons.Filled.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = {
                    menuOpen = false
                    onSwitchToDay()
                    onPickDate()
                }
            )
            DropdownMenuItem(
                text = { Text("Whole month") },
                leadingIcon = {
                    Icon(
                        Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = {
                    menuOpen = false
                    onSwitchToMonth()
                    onPickMonth()
                }
            )
        }
    }
}

// ---------------------------------------------------------------------
// Compact dashboard - one row of 4 stat chips
// ---------------------------------------------------------------------

@Composable
private fun CompactSummaryRow(summary: LedgerSummary, valuesUnlocked: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MiniStat(
            label = "Entries",
            value = summary.totalEntries.toString(),
            accent = Color(0xFF7C3AED),
            modifier = Modifier.weight(1f)
        )
        MiniStat(
            label = "Payment",
            value = if (valuesUnlocked) formatCurrency(summary.totalPayment) else HIDDEN_MONEY_PLACEHOLDER,
            accent = Color(0xFF0EA5E9),
            modifier = Modifier.weight(1.4f)
        )
        MiniStat(
            label = "Profit",
            value = if (valuesUnlocked) formatCurrency(summary.totalProfit) else HIDDEN_MONEY_PLACEHOLDER,
            accent = Color(0xFF16A34A),
            modifier = Modifier.weight(1.4f)
        )
        MiniStat(
            label = "Cost",
            value = if (valuesUnlocked) formatCurrency(summary.totalCost) else HIDDEN_MONEY_PLACEHOLDER,
            accent = Color(0xFFDC2626),
            modifier = Modifier.weight(1.2f)
        )
    }
}

@Composable
private fun MiniStat(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.10f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------------------------------------------------------------------
// Compact worker breakdown - single row, both workers side by side
// ---------------------------------------------------------------------

@Composable
private fun CompactWorkerBreakdown(summary: LedgerSummary, valuesUnlocked: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CompactWorkerCard(
            stats = summary.workerAStats,
            valuesUnlocked = valuesUnlocked,
            modifier = Modifier.weight(1f)
        )
        CompactWorkerCard(
            stats = summary.workerBStats,
            valuesUnlocked = valuesUnlocked,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CompactWorkerCard(
    stats: WorkerStats,
    valuesUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    val color = workerColor(stats.workerType)
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stats.workerType.storageValue,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${stats.entryCount} ${if (stats.entryCount == 1) "job" else "jobs"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (valuesUnlocked) {
                        "P ${formatAmount(stats.totalPayment)} · Pr ${formatAmount(stats.totalProfit)}"
                    } else {
                        "P $HIDDEN_MONEY_PLACEHOLDER · Pr $HIDDEN_MONEY_PLACEHOLDER"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EntriesHeader(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Entries",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 6.dp, vertical = 1.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------
// Search
// ---------------------------------------------------------------------

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Search entries") },
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(
                        onClick = { onValueChange("") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(6.dp))
        TextButton(onClick = onClose) {
            Text("Cancel")
        }
    }
}

// ---------------------------------------------------------------------
// Loading / empty states
// ---------------------------------------------------------------------

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(strokeWidth = 3.dp)
    }
}

@Composable
private fun EmptyEmployeeState(isSearching: Boolean, isMonthView: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSearching) Icons.Filled.SearchOff else Icons.Filled.EventBusy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = when {
                isSearching -> "No matching entries"
                isMonthView -> "No entries recorded for this month."
                else -> "No entries recorded for today."
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (isSearching) "Try a different search term"
            else "Tap + to log the first repair job",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------------
// Compact entry row - all details, but tiny and never overflows
// ---------------------------------------------------------------------

@Composable
private fun CompactEntryRow(
    note: EmployeeNote,
    valuesUnlocked: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val worker = WorkerType.fromStorageValue(note.workerType)
    val cost = note.totalPayment - note.profit

    Card(
        onClick = onEdit,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            // Top row: worker badge · title (fills) · edit · delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(workerColor(worker))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = worker.storageValue,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edit entry",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete entry",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Description (single line)
            if (note.description.isNotBlank()) {
                Text(
                    text = note.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(Modifier.height(6.dp))

            // Bottom row: 3 micro-stats inline
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                InlineStat(
                    label = "Pay",
                    value = if (valuesUnlocked) formatAmount(note.totalPayment) else HIDDEN_MONEY_PLACEHOLDER,
                    accent = Color(0xFF0EA5E9),
                    modifier = Modifier.weight(1f)
                )
                InlineStat(
                    label = "Profit",
                    value = if (valuesUnlocked) formatAmount(note.profit) else HIDDEN_MONEY_PLACEHOLDER,
                    accent = Color(0xFF16A34A),
                    modifier = Modifier.weight(1f)
                )
                InlineStat(
                    label = "Cost",
                    value = if (valuesUnlocked) formatAmount(cost) else HIDDEN_MONEY_PLACEHOLDER,
                    accent = Color(0xFFDC2626),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = LedgerDateUtils.formatEntryTimestamp(note.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun InlineStat(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accent
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------------------------------------------------------------------
// Worker selector + badge helpers
// ---------------------------------------------------------------------

@Composable
private fun WorkerSelector(
    selected: WorkerType,
    onSelect: (WorkerType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        WorkerType.entries.forEach { worker ->
            val isSelected = worker == selected
            val backgroundColor = if (isSelected) workerColor(worker) else Color.Transparent
            val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(backgroundColor)
                    .clickable { onSelect(worker) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = workerFullLabel(worker),
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

private fun workerColor(worker: WorkerType): Color = when (worker) {
    WorkerType.A -> Color(0xFF2F6FED)
    WorkerType.B -> Color(0xFFF08A24)
}

private fun workerFullLabel(worker: WorkerType): String = when (worker) {
    WorkerType.A -> "A · Owner"
    WorkerType.B -> "B · Employee"
}

// ---------------------------------------------------------------------
// Add/Edit dialog
// ---------------------------------------------------------------------

@Composable
private fun WorkLedgerEntryDialog(
    initialWorker: WorkerType,
    initialTitle: String,
    initialDescription: String,
    initialTotalPayment: String,
    initialProfit: String,
    isSaving: Boolean,
    titleError: String?,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSave: (WorkerType, String, String, String, String) -> Unit
) {
    var worker by remember(initialWorker) { mutableStateOf(initialWorker) }
    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    var description by remember(initialDescription) { mutableStateOf(initialDescription) }
    var totalPayment by remember(initialTotalPayment) { mutableStateOf(initialTotalPayment) }
    var profit by remember(initialProfit) { mutableStateOf(initialProfit) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(text = if (isEditing) "Edit Entry" else "New Entry", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "Worker",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                WorkerSelector(selected = worker, onSelect = { worker = it })

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    isError = titleError != null,
                    supportingText = { titleError?.let { Text(it) } },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = totalPayment,
                        onValueChange = { totalPayment = it },
                        label = { Text("Total Payment") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = profit,
                        onValueChange = { profit = it },
                        label = { Text("Profit") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                shape = RoundedCornerShape(12.dp),
                onClick = { onSave(worker, title, description, totalPayment, profit) }
            ) {
                Text(if (isSaving) "Saving..." else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        }
    )
}

// ---------------------------------------------------------------------
// Date / month pickers
// ---------------------------------------------------------------------

@Composable
private fun LedgerDatePickerDialog(
    initialDate: Date,
    onDismiss: () -> Unit,
    onConfirm: (Date) -> Unit
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialDate.time)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis != null) {
                    onConfirm(LedgerDateUtils.utcMillisToLocalDate(millis))
                } else {
                    onDismiss()
                }
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DatePicker(state = state)
    }
}

/**
 * Lightweight month picker. Shows the last 24 months (e.g. Jul 2024 .. Jul 2026)
 * as a scrollable list. Picking a row selects the entire month at midnight.
 */
@Composable
private fun LedgerMonthPickerDialog(
    initialMonthStart: Date,
    onDismiss: () -> Unit,
    onConfirm: (Date) -> Unit
) {
    val now = Date()
    val months = remember {
        val list = mutableListOf<Date>()
        val cal = Calendar.getInstance()
        cal.time = LedgerDateUtils.startOfMonth(now)
        repeat(24) {
            list.add(cal.time)
            cal.add(Calendar.MONTH, -1)
        }
        list
    }

    var selectedIndex by remember {
        val idx = months.indexOfFirst {
            LedgerDateUtils.startOfMonth(it) == LedgerDateUtils.startOfMonth(initialMonthStart)
        }
        mutableStateOf(if (idx >= 0) idx else 0)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(text = "Select month", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    items(months.size) { index ->
                        val monthDate = months[index]
                        val isSelected = index == selectedIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                                .clickable { selectedIndex = index }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioDot(selected = isSelected)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = formatMonthYear(monthDate),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(months[selectedIndex]) },
                shape = RoundedCornerShape(12.dp)
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun RadioDot(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else Color.Transparent
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!selected) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

// ---------------------------------------------------------------------
// Formatting helpers
// ---------------------------------------------------------------------

private fun formatCurrency(value: Double): String = "৳${formatAmount(value)}"

/**
 * Placeholder shown in place of any money value while the screen is locked.
 * Looks like a real value at a glance but reveals nothing if it isn't.
 */
private const val HIDDEN_MONEY_PLACEHOLDER = "••••••"

private fun formatAmount(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", value)
    }
}

private fun formatShortDate(date: Date): String {
    val today = Date()
    return if (LedgerDateUtils.isSameDay(date, today)) {
        "Today"
    } else {
        SimpleDateFormat("d MMM", Locale.getDefault()).format(date)
    }
}

private fun formatMonthYear(date: Date): String =
    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date)