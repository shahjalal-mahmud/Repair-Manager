// app/src/main/java/com/appriyo/repairmanager/presentation/screens/EmployeeScreen.kt
package com.appriyo.repairmanager.presentation.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.appriyo.repairmanager.data.model.EmployeeNote
import com.appriyo.repairmanager.presentation.components.DeleteConfirmationDialog
import com.appriyo.repairmanager.presentation.components.TopToastHost
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Employee Notes",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (uiState.notes.isEmpty()) {
                        "Track payments and profit per entry"
                    } else {
                        "${uiState.notes.size} ${if (uiState.notes.size == 1) "entry" else "entries"}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))

                EmployeeSearchField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) }
                )

                Spacer(Modifier.height(12.dp))

                when {
                    uiState.isLoading -> LoadingState()
                    filteredNotes.isEmpty() -> EmptyEmployeeState(isSearching = uiState.searchQuery.isNotBlank())
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 96.dp)
                        ) {
                            items(filteredNotes, key = { it.id }) { note ->
                                EmployeeNoteCard(
                                    note = note,
                                    onEdit = { viewModel.openEditDialog(note) },
                                    onDelete = { viewModel.requestDeleteNote(note) }
                                )
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

    uiState.noteToDelete?.let { note ->
        DeleteConfirmationDialog(
            title = "Delete entry?",
            message = "\"${note.title}\" will be permanently removed. This can't be undone.",
            isDeleting = uiState.isDeleting,
            onConfirm = { viewModel.confirmDeleteNote() },
            onDismiss = { viewModel.dismissDeleteConfirmation() }
        )
    }
}

@Composable
private fun EmployeeSearchField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Search employee notes") },
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

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(strokeWidth = 3.dp)
    }
}

@Composable
private fun EmptyEmployeeState(isSearching: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSearching) Icons.Filled.PersonOff else Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (isSearching) "No matching entries" else "No entries yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (isSearching) "Try a different search term" else "Tap + to add your first entry",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmployeeNoteCard(
    note: EmployeeNote,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onEdit,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (note.description.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = note.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(4.dp))

                Column {
                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit entry",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete entry",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AmountChip(
                    label = "Payment",
                    amount = note.totalPayment,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    icon = Icons.Filled.Payments
                )
                AmountChip(
                    label = "Profit",
                    amount = note.profit,
                    containerColor = Color(0xFFDFF5E1),
                    contentColor = Color(0xFF1B5E20),
                    icon = Icons.AutoMirrored.Filled.TrendingUp
                )
            }
        }
    }
}

@Composable
private fun AmountChip(
    label: String,
    amount: Double,
    containerColor: Color,
    contentColor: Color,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "$label: ${formatAmount(amount)}",
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.Medium
        )
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
        onDismissRequest = { if (!isSaving) onDismiss() },
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = if (isEditing) "Edit Entry" else "New Entry",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
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
                        label = { Text("Payment") },
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