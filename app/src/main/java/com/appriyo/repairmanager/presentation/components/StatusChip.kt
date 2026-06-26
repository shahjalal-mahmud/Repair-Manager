// app/src/main/java/com/appriyo/repairmanager/presentation/components/StatusChip.kt
package com.appriyo.repairmanager.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.appriyo.repairmanager.data.model.RepairStatus

@Composable
fun statusContainerColor(status: String): Color = when (status) {
    RepairStatus.PENDING -> MaterialTheme.colorScheme.errorContainer
    RepairStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primaryContainer
    RepairStatus.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer
    RepairStatus.DELIVERED -> MaterialTheme.colorScheme.tertiaryContainer
    else -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
fun statusContentColor(status: String): Color = when (status) {
    RepairStatus.PENDING -> MaterialTheme.colorScheme.onErrorContainer
    RepairStatus.IN_PROGRESS -> MaterialTheme.colorScheme.onPrimaryContainer
    RepairStatus.COMPLETED -> MaterialTheme.colorScheme.onSecondaryContainer
    RepairStatus.DELIVERED -> MaterialTheme.colorScheme.onTertiaryContainer
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

/**
 * Tappable status pill. Tapping opens a dropdown to pick a new status -
 * this IS the "selecting box" the shop owner uses to move a repair through its lifecycle.
 */
@Composable
fun StatusChip(
    status: String,
    enabled: Boolean = true,
    onStatusSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = statusContainerColor(status),
        modifier = Modifier.clickable(enabled = enabled) { expanded = true }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = status,
                style = MaterialTheme.typography.labelMedium,
                color = statusContentColor(status)
            )
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = "Change status",
                tint = statusContentColor(status),
                modifier = Modifier.size(16.dp).padding(start = 2.dp)
            )
        }
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        RepairStatus.ALL.forEach { option ->
            DropdownMenuItem(
                text = { Text(option) },
                onClick = {
                    onStatusSelected(option)
                    expanded = false
                }
            )
        }
    }
}