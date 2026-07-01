package com.appriyo.repairmanager.presentation.components.talikhata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
    val amount = amountText.toDoubleOrNull() ?: 0.0
    val newBalance = if (isEditing) {
        if (isIncrease) currentBalance + amount else currentBalance - amount
    } else {
        amount
    }
    val hasNegativeError = isEditing && !isIncrease && newBalance < 0.0

    val canSave = personName.isNotBlank() &&
            (!isEditing || amountText.isBlank() || !hasNegativeError) &&
            !isSaving

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Entry" else "Add Entry") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Type", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaliKhataType.entries.forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { type = option },
                            label = { Text(option.label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text("Person Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Details") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (isEditing) {
                    HorizontalDivider()
                    Text(
                        text = "Current Balance: ${formatCurrency(currentBalance)}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text("Adjustment Amount", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = isIncrease,
                            onClick = { isIncrease = true },
                            label = { Text("+") }
                        )
                        FilterChip(
                            selected = !isIncrease,
                            onClick = { isIncrease = false },
                            label = { Text("-") }
                        )
                    }

                    Text(
                        text = "New Balance: ${formatCurrency(newBalance)}",
                        style = MaterialTheme.typography.bodyMedium
                    )

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
                        label = { Text("Amount") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canSave,
                onClick = {
                    onSave(personName, phoneNumber, details, type, amount, isIncrease)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}