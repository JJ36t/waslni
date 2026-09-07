package com.waslni.driver.core.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.waslni.driver.R

/**
 * Generic confirmation dialog with title, message, and one or two actions.
 *
 * Used across the app for:
 *   - Confirm-delete-customer
 *   - Permission rationale
 *   - Discard-edit confirmation
 *
 * @param title       Dialog title.
 * @param message     Dialog body.
 * @param confirmText Label for the confirm button.
 * @param onConfirm   Called when the confirm button is tapped.
 * @param onDismiss   Called when the dialog is dismissed (tap outside, back).
 * @param dismissText Optional label for the dismiss button. If null, only
 *                    the confirm button is shown.
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String? = stringResource(R.string.cancel)
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText) }
        },
        dismissButton = if (dismissText != null) {
            { TextButton(onClick = onDismiss) { Text(dismissText) } }
        } else null
    )
}
