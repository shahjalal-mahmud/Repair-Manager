package com.appriyo.repairmanager.presentation.components.talikhata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appriyo.repairmanager.data.model.TaliKhataHistoryEntry
import java.text.SimpleDateFormat
import java.util.Locale

/** Simple, non-timeline list of history rows for one entry. */
@Composable
fun TaliKhataHistoryList(
    history: List<TaliKhataHistoryEntry>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "History",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (history.isEmpty()) {
            Text(
                text = "No history yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            history.forEach { record ->
                TaliKhataHistoryRow(record)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun TaliKhataHistoryRow(record: TaliKhataHistoryEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = record.operationEnum.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            record.timestamp?.let {
                Text(
                    text = formatTimestamp(it.time),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
            Text(
                text = formatCurrency(record.amount),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Balance: ${formatCurrency(record.balanceAfter)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTimestamp(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(millis)