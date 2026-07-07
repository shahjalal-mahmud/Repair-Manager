package com.appriyo.repairmanager.presentation.components.talikhata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.appriyo.repairmanager.data.model.TaliKhataEntry
import com.appriyo.repairmanager.data.model.TaliKhataType

/**
 * Add/Edit dialog for a TaliKhata entry.
 *
 * - When [entry] is null: creates a new entry (name/phone/details/type/initial amount).
 * - When [entry] is non-null: lets the user edit identity fields AND apply a balance
 *   adjustment (+/-) against the current balance, with a live "New Balance" preview
 *   and validation that blocks a subtraction that would go negative.
 *
 * [onSave] receives everything the caller's ViewModel needs to invoke
 * updateDetails(...) and, for edits with a non-zero amount, adjustBalance(...).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaliKhataAddEditDialog(
    entry: TaliKhataEntry?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        personName: String,
        phoneNumber: String,
        details: String,
        type: TaliKhataType,
        amount: Double,
        isIncrease: Boolean
    ) -> Unit
) {
    val isEditing = entry != null

    var personName by remember { mutableStateOf(entry?.personName.orEmpty()) }
    var phoneNumber by remember { mutableStateOf(entry?.phoneNumber.orEmpty()) }
    var details by remember { mutableStateOf(entry?.details.orEmpty()) }
    var type by remember { mutableStateOf(entry?.typeEnum ?: TaliKhataType.THEY_OWE_YOU) }
    var amountText by remember { mutableStateOf("") }
    var isIncrease by remember { mutableStateOf(true) }

    val currentBalance = entry?.balance ?: 0.0
    // Treat a blank/empty field as 0 for the "no adjustment" case, but block
    // typing actual garbage like "abc" or "1.2.3" from being silently coerced
    // to 0.0 - that previously let users "save" adjustments they never made.
    val parsedAmount = amountText.toDoubleOrNull()
    val amount = parsedAmount ?: 0.0
    val hasInvalidAmountText = amountText.isNotBlank() && parsedAmount == null
    val newBalance = if (isEditing) {
        if (isIncrease) currentBalance + amount else currentBalance - amount
    } else {
        amount
    }
    val hasNegativeError = isEditing && !isIncrease && newBalance < 0.0

    // For new entries the initial amount must be a non-negative number; for
    // edits the adjustment may be left blank (meaning "no balance change").
    val newEntryRequiresValidAmount = !isEditing && (parsedAmount == null || amount < 0.0)

    val canSave = personName.isNotBlank() &&
            !isSaving &&
            !hasInvalidAmountText &&
            !hasNegativeError &&
            !newEntryRequiresValidAmount

    // Shared design tokens for every input in the dialog so they read as one
    // consistent surface. `heightIn(min = fieldHeight)` is the actual fix for
    // the reported "I can't see what I typed" bug: M3's OutlinedTextField
    // shrinks to fit its container when there's no min height, and inside an
    // AlertDialog `text` slot the measured height was too small for the body
    // text + floating label to coexist.
    val fieldShape = RoundedCornerShape(12.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = if (isEditing) "Edit Entry" else "Add Entry",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Type picker - presented as a labelled segmented-style row so
                // it's clear these two options belong together and belong to
                // this dialog.
                Text(
                    text = "Type",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaliKhataType.entries.forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { type = option },
                            label = {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor =
                                    MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor =
                                    MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text("Person Name") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    shape = fieldShape,
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = fieldShape,
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                )

                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Details") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = false,
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    shape = fieldShape,
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 88.dp)
                )

                if (isEditing) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Current Balance",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(currentBalance),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Text(
                        text = "Adjustment Amount",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = hasInvalidAmountText,
                        supportingText = if (hasInvalidAmountText) {
                            { Text("Enter a valid number") }
                        } else null,
                        shape = fieldShape,
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = isIncrease,
                            onClick = { isIncrease = true },
                            label = {
                                Text(
                                    "+ Add",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor =
                                    MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor =
                                    MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        FilterChip(
                            selected = !isIncrease,
                            onClick = { isIncrease = false },
                            label = {
                                Text(
                                    "- Subtract",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor =
                                    MaterialTheme.colorScheme.tertiaryContainer,
                                selectedLabelColor =
                                    MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "New Balance",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(newBalance),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    if (hasNegativeError) {
                        Text(
                            text = "Adjustment exceeds current balance.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Initial Amount") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = hasInvalidAmountText || newEntryRequiresValidAmount,
                        supportingText = {
                            val msg = when {
                                hasInvalidAmountText -> "Enter a valid number"
                                newEntryRequiresValidAmount -> "Initial amount is required"
                                else -> "Amount owed / owing to start this entry with"
                            }
                            Text(msg)
                        },
                        shape = fieldShape,
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canSave,
                onClick = {
                    onSave(personName, phoneNumber, details, type, amount, isIncrease)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}