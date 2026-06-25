// app/src/main/java/com/appriyo/repairmanager/presentation/screens/EmployeeScreen.kt
package com.appriyo.repairmanager.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.appriyo.repairmanager.data.model.EmployeeNote
import com.appriyo.repairmanager.presentation.viewmodel.EmployeeNotesViewModel
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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

    val filteredNotes = remember(uiState.notes, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) {
            uiState.notes
        } else {
            uiState.notes.filter {
                it.title.contains(uiState.searchQuery, ignoreCase = true) ||
                        it.description.contains(uiState.searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.openAddDialog() }) {
                Icon(Icons.Filled.Add, contentDescription = "Add employee note")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(text = "Employee Notes", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                label = { Text("Search employee notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 12.dp)
            )

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                filteredNotes.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (uiState.searchQuery.isBlank())
                                "No entries yet. Tap + to add one."
                            else
                                "No matching entries.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredNotes, key = { it.id }) { note ->
                            EmployeeNoteCard(
                                note = note,
                                onEdit = { viewModel.openEditDialog(note) },
                                onDelete = { viewModel.deleteNote(note.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.isDialogOpen) {
        EmployeeNoteEditDialog(
            initialTitle = uiState.editingNote?.title ?: "",
            initialDescription = uiState.editingNote?.description ?: "",
            initialTotalPayment = uiState.editingNote?.totalPayment?.let { formatAmount(it) } ?: "",
            initialProfit = uiState.editingNote?.profit?.let { formatAmount(it) } ?: "",
            isSaving = uiState.isSaving,
            titleError = uiState.titleError,
            isEditing = uiState.editingNote != null,
            onDismiss = { viewModel.dismissDialog() },
            onSave = { title, description, totalPayment, profit ->
                viewModel.saveNote(title, description, totalPayment, profit)
            }
        )
    }
}

@Composable
private fun EmployeeNoteCard(
    note: EmployeeNote,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = note.title, style = MaterialTheme.typography.titleMedium)
            if (note.description.isNotBlank()) {
                Text(
                    text = note.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Text(
                text = "Total Payment: ${formatAmount(note.totalPayment)}  •  Profit: ${formatAmount(note.profit)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit entry")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete entry")
                }
            }
        }
    }
}

@Composable
private fun EmployeeNoteEditDialog(
    initialTitle: String,
    initialDescription: String,
    initialTotalPayment: String,
    initialProfit: String,
    isSaving: Boolean,
    titleError: String?,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    var description by remember(initialDescription) { mutableStateOf(initialDescription) }
    var totalPayment by remember(initialTotalPayment) { mutableStateOf(initialTotalPayment) }
    var profit by remember(initialProfit) { mutableStateOf(initialProfit) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Entry" else "Add Entry") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    isError = titleError != null,
                    supportingText = { titleError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = totalPayment,
                    onValueChange = { totalPayment = it },
                    label = { Text("Total Payment") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = profit,
                    onValueChange = { profit = it },
                    label = { Text("Profit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving,
                onClick = { onSave(title, description, totalPayment, profit) }
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

private fun formatAmount(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", value)
    }
}