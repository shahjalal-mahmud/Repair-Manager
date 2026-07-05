package com.appriyo.repairmanager.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.appriyo.repairmanager.data.model.NoteCategory

/**
 * Reusable UI atoms for the Notes feature's category system.
 *
 * - [NoteCategoryBadge]         — small pill used on note cards / list rows.
 * - [NoteCategoryTabsWithCounts] — Material 3 tab row across the top of the screen.
 * - [CategorySelector]          — segmented control inside the Add/Edit dialog.
 *
 * All three bind colors to [MaterialTheme.colorScheme] tokens so they
 * automatically adapt to dark mode / dynamic-color without any hard-coded
 * hex values.
 */

/** Per-category accent colors + icon, resolved against the current Material 3 theme. */
internal data class CategoryPalette(
    val container: Color,
    val onContainer: Color,
    val icon: ImageVector
)

@Composable
internal fun noteCategoryPalette(category: NoteCategory): CategoryPalette {
    val scheme = MaterialTheme.colorScheme
    return when (category) {
        NoteCategory.GENERAL -> CategoryPalette(
            container = scheme.primaryContainer,
            onContainer = scheme.onPrimaryContainer,
            icon = Icons.AutoMirrored.Filled.Note
        )
        NoteCategory.REMINDER -> CategoryPalette(
            container = scheme.tertiaryContainer,
            onContainer = scheme.onTertiaryContainer,
            icon = Icons.Filled.NotificationsActive
        )
        NoteCategory.IMPORTANT -> CategoryPalette(
            container = scheme.errorContainer,
            onContainer = scheme.onErrorContainer,
            icon = Icons.Filled.PriorityHigh
        )
    }
}

/** Small pill chip showing a category's icon + label. Pass [compact]=true to drop the icon. */
@Composable
fun NoteCategoryBadge(
    category: NoteCategory,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = noteCategoryPalette(category)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(palette.container)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (!compact) {
            Icon(
                imageVector = palette.icon,
                contentDescription = null,
                tint = palette.onContainer,
                modifier = Modifier.size(13.dp)
            )
        }
        Text(
            text = category.displayLabel,
            style = MaterialTheme.typography.labelSmall,
            color = palette.onContainer,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Horizontal Material 3 tab row with one tab per [NoteCategory]. Each tab shows the
 * category's icon, label, and an optional count badge (hidden when the count is 0).
 *
 * Counts should be supplied from the unfiltered note list so they remain accurate while
 * the user is searching.
 */
@Composable
fun NoteCategoryTabsWithCounts(
    selected: NoteCategory,
    counts: Map<NoteCategory, Int>,
    onSelect: (NoteCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val entries = NoteCategory.entries
    val selectedIndex = entries.indexOf(selected).coerceAtLeast(0)

    PrimaryTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        entries.forEachIndexed { index, category ->
            val count = counts[category] ?: 0
            val isSelected = selectedIndex == index
            val palette = noteCategoryPalette(category)
            Tab(
                selected = isSelected,
                onClick = { onSelect(category) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = palette.icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = category.displayLabel,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (count > 0) {
                        Badge(
                            containerColor = palette.container,
                            contentColor = palette.onContainer
                        ) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pill-style category picker used inside the Add/Edit dialog so the user can choose which
 * bucket to save the note into.
 *
 * Built from a plain [Row] of three equal-weight tiles rather than M3's
 * [SingleChoiceSegmentedButtonRow] because the latter doesn't gracefully handle three
 * icon+label segments at narrow dialog widths — segments end up with mismatched sizes or
 * wrapped labels. Each tile here is a self-contained tap target with uniform horizontal
 * padding and a single-line label, so the row stays tidy at any dialog size.
 *
 * Active tile uses each category's [CategoryPalette] colors so the picker agrees
 * visually with the matching tab.
 */
@Composable
fun CategorySelector(
    selected: NoteCategory,
    onChange: (NoteCategory) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val entries = NoteCategory.entries
    val scheme = MaterialTheme.colorScheme
    val containerShape = RoundedCornerShape(16.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(containerShape)
            .background(scheme.surfaceContainerHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        entries.forEach { category ->
            CategoryPickerTile(
                category = category,
                isSelected = selected == category,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                onClick = { if (enabled) onChange(category) }
            )
        }
    }
}

/** One tile inside [CategorySelector]. Equal-weight so all three segments stay the same size. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerTile(
    category: NoteCategory,
    isSelected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = noteCategoryPalette(category)
    val scheme = MaterialTheme.colorScheme
    val containerColor = if (isSelected) palette.container else scheme.surface
    val contentColor = if (isSelected) palette.onContainer else scheme.onSurfaceVariant
    val borderColor = if (isSelected) palette.onContainer.copy(alpha = 0.5f)
        else scheme.outlineVariant.copy(alpha = 0.6f)

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        contentColor = contentColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        tonalElevation = if (isSelected) 1.dp else 0.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = palette.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = category.displayLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )
        }
    }
}
