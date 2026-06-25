// app/src/main/java/com/appriyo/repairmanager/presentation/components/RepairFormComponents.kt
package com.appriyo.repairmanager.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * A checkbox with a trailing label, used for the accessory toggles
 * (battery, SIM, memory card, SIM tray, back cover) and the dead-phone consent flag.
 */
@Composable
fun LabeledCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        Text(text = label)
    }
}

/**
 * A simple read-only text field that opens a [DropdownMenu] of [options] when tapped.
 * Built on plain DropdownMenu (rather than ExposedDropdownMenuBox) to avoid relying on
 * menu-anchor APIs that have changed across recent Compose Material3 versions.
 */
@Composable
fun OptionDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = {
                Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Transparent overlay so taps open the menu even though the text field is read-only.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = enabled) { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}