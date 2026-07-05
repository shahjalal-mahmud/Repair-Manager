// app/src/main/java/com/appriyo/repairmanager/presentation/components/PinEntryDialog.kt
package com.appriyo.repairmanager.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appriyo.repairmanager.domain.security.PinValidator

/**
 * Six-box OTP-style PIN entry dialog.
 *
 * Visual rules:
 *  - All six boxes are guaranteed to be the **same width**, regardless of
 *    the dialog's content width, because each one uses `Modifier.weight(1f)`
 *    inside a Row that fills the dialog's available width.
 *  - The currently-focused empty box shows a thin colored border plus a
 *    blinking caret cursor, just like the Material/Google OTP field.
 *  - Filled boxes get the primary color tint so the user can see progress.
 *  - Digits are rendered at a fixed sp size so the box never "shrinks"
 *    when a character is added.
 *
 * The dialog itself is purely a visual input layer; verification is owned
 * by the caller (typically [PinProtectedAction] + SecurityViewModel).
 */
@Composable
fun PinEntryDialog(
    title: String = "Owner PIN",
    message: String = "Enter the 6-digit owner PIN to continue.",
    isVerifying: Boolean = false,
    errorMessage: String? = null,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(TextFieldValue("", TextRange.Zero)) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        // Auto-focus the (invisible) BasicTextField when the dialog opens
        // so the soft keyboard appears immediately.
        focusRequester.requestFocus()
    }

    val canSubmit = value.text.length == PinValidator.PIN_LENGTH && !isVerifying

    AlertDialog(
        onDismissRequest = { if (!isVerifying) onDismiss() },
        shape = RoundedCornerShape(20.dp),
        icon = {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
        },
        title = {
            Text(text = title, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))

                // Hidden BasicTextField captures keyboard input. It owns the
                // real TextFieldValue so the boxes stay a pure projection.
                BasicTextField(
                    value = value,
                    onValueChange = { newValue ->
                        val digitsOnly = newValue.text.filter { it.isDigit() }
                            .take(PinValidator.PIN_LENGTH)
                        value = TextFieldValue(
                            text = digitsOnly,
                            selection = TextRange(digitsOnly.length)
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .size(width = 1.dp, height = 1.dp)
                        .focusRequester(focusRequester)
                )

                // The visible row of 6 boxes. Fills the full dialog width so
                // every box can take an equal share via Modifier.weight(1f).
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(PinValidator.PIN_LENGTH) { index ->
                        val char = value.text.getOrNull(index)?.toString().orEmpty()
                        val isFocused = char.isEmpty() && index == value.text.length
                        PinDigitBox(
                            char = char,
                            isFocused = isFocused
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(value.text) },
                enabled = canSubmit,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isVerifying) "Checking..." else "Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isVerifying) {
                Text("Cancel")
            }
        }
    )
}

/**
 * A single OTP cell. Three states:
 *  - **Empty / not next** → light border, neutral background.
 *  - **Empty / next**     → colored border + blinking caret cursor.
 *  - **Filled**           → colored border + tinted background + bold digit.
 *
 * The caret uses an infinite alpha animation (1s cycle, Reverse) which feels
 * natural and matches standard OTP field UX on Material/Android.
 */
@Composable
private fun RowScope.PinDigitBox(
    char: String,
    isFocused: Boolean
) {
    // Blinking caret, runs as long as the box is focused and empty.
    val transition = rememberInfiniteTransition(label = "pin-caret")
    val caretAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pin-caret-alpha"
    )

    val filled = char.isNotEmpty()
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    val borderColor = when {
        filled -> primaryColor
        isFocused -> primaryColor
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val borderWidth = if (isFocused && !filled) 2.dp else if (filled) 2.dp else 1.dp

    val backgroundColor = when {
        filled -> primaryColor.copy(alpha = 0.10f)
        isFocused -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    Box(
        modifier = Modifier
            .weight(1f)                 // equal width per box, the key fix
            .height(58.dp)              // fixed height so all boxes are identical
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (filled) {
            // Filled state: large, bold digit in the primary color.
            Text(
                text = char,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = onPrimary.takeIf { false } ?: primaryColor
            )
        } else if (isFocused) {
            // Focused but empty: show the blinking caret in the primary color.
            Box(
                modifier = Modifier
                    .size(width = 2.dp, height = 22.dp)
                    .alpha(caretAlpha)
                    .background(primaryColor)
            )
        }
        // Else: leave empty (neither digit nor caret).
    }
}