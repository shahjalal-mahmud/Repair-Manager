package com.appriyo.repairmanager.presentation.components.talikhata

import android.net.Uri
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.appriyo.repairmanager.data.model.TaliKhataEntry
import com.appriyo.repairmanager.data.model.TaliKhataHistoryEntry

/**
 * Bottom sheet shown when a ledger entry card is tapped.
 * Shows entry details, a photo grid (with an Add Photo button), and history.
 * Photo loading and Add Photo behavior are entirely delegated via [photoUris]
 * and [onAddPhotoClick] - no MediaStore access happens here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaliKhataDetailBottomSheet(
    entry: TaliKhataEntry,
    history: List<TaliKhataHistoryEntry>,
    photoUris: List<Uri>,
    onDismiss: () -> Unit,
    onAddPhotoClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            TypeBadge(type = entry.typeEnum)

            Text(
                text = entry.personName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (entry.phoneNumber.isNotBlank()) {
                Text(
                    text = entry.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (entry.details.isNotBlank()) {
                Text(
                    text = entry.details,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Text(
                text = "Current Balance: ${formatCurrency(entry.balance)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = "Photos",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            PhotoGrid(
                photoUris = photoUris,
                onAddPhotoClick = onAddPhotoClick
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            TaliKhataHistoryList(history = history)
        }
    }
}

@Composable
private fun PhotoGrid(
    photoUris: List<Uri>,
    onAddPhotoClick: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(((photoUris.size / 3 + 1) * 110).dp.coerceAtMost(330.dp))
    ) {
        item {
            AddPhotoTile(onClick = onAddPhotoClick)
        }
        items(photoUris) { uri ->
            PhotoTile(uri = uri)
        }
    }
}

@Composable
private fun AddPhotoTile(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Add, contentDescription = "Add Photo")
    }
}

@Composable
private fun PhotoTile(uri: Uri) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageURI(uri)
            }
        },
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp)),
        update = { imageView ->
            imageView.setImageURI(uri)
        }
    )
}