package com.appriyo.repairmanager.presentation.components.talikhata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appriyo.repairmanager.presentation.viewmodel.TaliKhataSummary
import java.util.Locale

/**
 * Row of three summary cards shown at the top of TaliKhataScreen.
 * Purely presentational - all figures come from [summary].
 */
@Composable
fun TaliKhataSummaryCards(
    summary: TaliKhataSummary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryCard(
            title = "You Owe",
            amount = summary.totalYouOwe,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            title = "Owes You",
            amount = summary.totalTheyOweYou,
            modifier = Modifier.weight(1f)
        )
        // Net balance is color-coded: green when you're owed, red when you
        // owe, neutral when exactly zero. Avoids the user having to do the
        // mental arithmetic from a single negative number.
        val net = summary.netBalance
        val netColor = when {
            net > 0.0 -> MaterialTheme.colorScheme.tertiary   // in your favour
            net < 0.0 -> MaterialTheme.colorScheme.error      // you owe net
            else -> Color.Unspecified
        }
        SummaryCard(
            title = "Net Balance",
            amount = net,
            amountColor = netColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: Double,
    modifier: Modifier = Modifier,
    amountColor: Color = Color.Unspecified
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatCurrency(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (amountColor == Color.Unspecified)
                    MaterialTheme.colorScheme.onSurface
                else amountColor
            )
        }
    }
}

/** Simple currency formatter shared by the TaliKhata UI. */
fun formatCurrency(amount: Double): String =
    String.format(Locale.US, "৳%,.2f", amount)