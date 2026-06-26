// app/src/main/java/com/appriyo/repairmanager/presentation/components/TopToast.kt
package com.appriyo.repairmanager.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Type of toast to show. Drives color + icon.
 */
enum class ToastType { SUCCESS, ERROR }

/**
 * Simple immutable toast payload held in UI state.
 */
data class ToastMessage(
    val text: String,
    val type: ToastType = ToastType.SUCCESS
)

/**
 * A custom in-app "toast" pinned to the top-right of the screen, since Android's
 * native Toast cannot be reliably repositioned on modern API levels.
 *
 * Place this as the LAST child inside a top-level Box(Modifier.fillMaxSize()) so it
 * draws above the rest of the screen content.
 */
@Composable
fun TopToastHost(
    toast: ToastMessage?,
    onConsumed: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2200)
            onConsumed()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 8.dp, end = 16.dp, start = 16.dp)
    ) {
        AnimatedVisibility(
            visible = toast != null,
            enter = slideInVertically(animationSpec = tween(250)) { -it } + fadeIn(tween(250)),
            exit = slideOutVertically(animationSpec = tween(200)) { -it } + fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            if (toast != null) {
                val containerColor = if (toast.type == ToastType.SUCCESS) {
                    Color(0xFF2E7D32)
                } else {
                    MaterialTheme.colorScheme.error
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = containerColor,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .widthIn(max = 280.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (toast.type == ToastType.SUCCESS) {
                                Icons.Filled.CheckCircle
                            } else {
                                Icons.Filled.Error
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = toast.text,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}