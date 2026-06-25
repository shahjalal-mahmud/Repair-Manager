// app/src/main/java/com/appriyo/repairmanager/presentation/screens/NotesScreen.kt
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
import androidx.compose.ui.unit.dp
import com.appriyo.repairmanager.data.model.Note
import com.appriyo.repairmanager.presentation.viewmodel.NotesViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: NotesViewModel = koinViewModel()
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
                Icon(Icons.Filled.Add, contentDescription = "Add note")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(text = "Notes", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                label = { Text("Search notes") },
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
                                "No notes yet. Tap + to add one."
                            else
                                "No matching notes.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredNotes, key = { it.id }) { note ->
                            NoteCard(
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
        NoteEditDialog(
            initialTitle = uiState.editingNote?.title ?: "",
            initialDescription = uiState.editingNote?.description ?: "",
            isSaving = uiState.isSaving,
            titleError = uiState.titleError,
            isEditing = uiState.editingNote != null,
            onDismiss = { viewModel.dismissDialog() },
            onSave = { title, description -> viewModel.saveNote(title, description) }
        )
    }
}

@Composable
private fun NoteCard(
    note: Note,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit note")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete note")
                }
            }
        }
    }
}

@Composable
private fun NoteEditDialog(
    initialTitle: String,
    initialDescription: String,
    isSaving: Boolean,
    titleError: String?,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    var description by remember(initialDescription) { mutableStateOf(initialDescription) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Note" else "Add Note") },
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
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving,
                onClick = { onSave(title, description) }
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