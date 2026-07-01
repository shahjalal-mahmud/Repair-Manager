// app/src/main/java/com/appriyo/repairmanager/presentation/screens/EmployeeScreen.kt
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.appriyo.repairmanager.presentation.screens

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.appriyo.repairmanager.data.model.EmployeeNote
import com.appriyo.repairmanager.data.model.WorkerType
import com.appriyo.repairmanager.presentation.components.DeleteConfirmationDialog
import com.appriyo.repairmanager.presentation.components.TopToastHost
import com.appriyo.repairmanager.presentation.state.LedgerDateFilter
import com.appriyo.repairmanager.presentation.state.LedgerSummary
import com.appriyo.repairmanager.presentation.state.WorkerStats
import com.appriyo.repairmanager.presentation.utils.LedgerDateUtils
import com.appriyo.repairmanager.presentation.viewmodel.EmployeeNotesViewModel
import org.koin.androidx.compose.koinViewModel
import java.util.Date
import java.util.Locale

@Composable
fun EmployeeScreen(
    viewModel: EmployeeNotesViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openAddDialog() },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add Entry") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Daily Work Ledger",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = LedgerDateUtils.formatFilterHeader(
                            uiState.selectedFilter,
                            uiState.customDate,
                            uiState.customRangeStart,
                            uiState.customRangeEnd
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                }

                stickyHeader {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(bottom = 10.dp)
                    ) {
                        LedgerFilterRow(
                            selectedFilter = uiState.selectedFilter,
                            onFilterSelected = { viewModel.onFilterSelected(it) }
                        )
                    }
                }

                if (uiState.isLoading) {
                    item { LoadingState() }
                } else {
                    item {
                        Spacer(Modifier.height(10.dp))
                        SummaryDashboard(summary = uiState.summary)
                        Spacer(Modifier.height(14.dp))
                        WorkerBreakdownSection(summary = uiState.summary)
                        Spacer(Modifier.height(16.dp))
                        EmployeeSearchField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) }
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    if (uiState.filteredNotes.isEmpty()) {
                        item { EmptyEmployeeState(isSearching = uiState.searchQuery.isNotBlank()) }
                    } else {
                        items(uiState.filteredNotes, key = { it.id }) { note ->
                            WorkLedgerCard(
                                note = note,
                                onEdit = { viewModel.openEditDialog(note) },
                                onDelete = { viewModel.requestDeleteNote(note) }
                            )
                            Spacer(Modifier.height(10.dp))
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
            initialDate = uiState.customDate,
            onDismiss = { viewModel.dismissDatePicker() },
            onConfirm = { viewModel.onCustomDateSelected(it) }
        )
    }

    if (uiState.showRangePicker) {
        LedgerDateRangePickerDialog(
            onDismiss = { viewModel.dismissRangePicker() },
            onConfirm = { start, end -> viewModel.onCustomRangeSelected(start, end) }
        )
    }
}

// ---------------------------------------------------------------------
// Filter row
// ---------------------------------------------------------------------

@Composable
private fun LedgerFilterRow(
    selectedFilter: LedgerDateFilter,
    onFilterSelected: (LedgerDateFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(LedgerDateFilter.entries) { filter ->
            val needsCalendarIcon = filter == LedgerDateFilter.CUSTOM_DATE || filter == LedgerDateFilter.CUSTOM_RANGE
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.label) },
                leadingIcon = if (needsCalendarIcon) {
                    {
                        Icon(
                            Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------
// Dashboard
// ---------------------------------------------------------------------

@Composable
private fun SummaryDashboard(summary: LedgerSummary, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(
                label = "Total Entries",
                value = summary.totalEntries.toString(),
                icon = Icons.Filled.Receipt,
                accentColor = Color(0xFF7C3AED),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Total Payment",
                value = formatCurrency(summary.totalPayment),
                icon = Icons.Filled.Payments,
                accentColor = Color(0xFF0EA5E9),
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(
                label = "Total Profit",
                value = formatCurrency(summary.totalProfit),
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                accentColor = Color(0xFF16A34A),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Total Cost",
                value = formatCurrency(summary.totalCost),
                icon = Icons.Filled.RequestQuote,
                accentColor = Color(0xFFDC2626),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------
// Worker breakdown
// ---------------------------------------------------------------------

@Composable
private fun WorkerBreakdownSection(summary: LedgerSummary, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "Worker Breakdown",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            WorkerStatsCard(stats = summary.workerAStats, modifier = Modifier.weight(1f))
            WorkerStatsCard(stats = summary.workerBStats, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun WorkerStatsCard(stats: WorkerStats, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            WorkerBadge(stats.workerType)
            Spacer(Modifier.height(10.dp))
            WorkerStatRow("Entries", stats.entryCount.toString())
            WorkerStatRow("Payment", formatCurrency(stats.totalPayment))
            WorkerStatRow("Profit", formatCurrency(stats.totalProfit))
            WorkerStatRow("Cost", formatCurrency(stats.totalCost))
        }
    }
}

@Composable
private fun WorkerStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

// ---------------------------------------------------------------------
// Search
// ---------------------------------------------------------------------

@Composable
private fun EmployeeSearchField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Search this period's entries") },
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

// ---------------------------------------------------------------------
// Loading / empty states
// ---------------------------------------------------------------------

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(strokeWidth = 3.dp)
    }
}

@Composable
private fun EmptyEmployeeState(isSearching: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSearching) Icons.Filled.SearchOff else Icons.Filled.EventBusy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (isSearching) "No matching entries" else "No work recorded for this date.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (isSearching) "Try a different search term" else "Tap + to log today's first repair job",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------------
// Entry card
// ---------------------------------------------------------------------

@Composable
private fun WorkLedgerCard(
    note: EmployeeNote,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val worker = WorkerType.fromStorageValue(note.workerType)
    val cost = note.totalPayment - note.profit

    Card(
        onClick = onEdit,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WorkerBadge(worker)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edit entry",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete entry",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = LedgerDateUtils.formatEntryTimestamp(note.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (note.description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = note.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniAmountBlock("Payment", note.totalPayment, Color(0xFF0EA5E9), Modifier.weight(1f))
                MiniAmountBlock("Profit", note.profit, Color(0xFF16A34A), Modifier.weight(1f))
                MiniAmountBlock("Cost", cost, Color(0xFFDC2626), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MiniAmountBlock(label: String, amount: Double, accentColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = accentColor)
        Spacer(Modifier.height(2.dp))
        Text(
            text = formatCurrency(amount),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ---------------------------------------------------------------------
// Worker selector + badge
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

@Composable
private fun WorkerBadge(worker: WorkerType) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(workerColor(worker))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = worker.storageValue,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
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
// Date / date-range pickers
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

@Composable
private fun LedgerDateRangePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Date, Date) -> Unit
) {
    val state = rememberDateRangePickerState()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                DateRangePicker(
                    state = state,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        val start = state.selectedStartDateMillis
                        val end = state.selectedEndDateMillis
                        if (start != null && end != null) {
                            onConfirm(
                                LedgerDateUtils.utcMillisToLocalDate(start),
                                LedgerDateUtils.utcMillisToLocalDate(end)
                            )
                        } else {
                            onDismiss()
                        }
                    }) { Text("OK") }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
// Formatting helpers
// ---------------------------------------------------------------------

private fun formatCurrency(value: Double): String = "৳${formatAmount(value)}"

private fun formatAmount(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", value)
    }
}