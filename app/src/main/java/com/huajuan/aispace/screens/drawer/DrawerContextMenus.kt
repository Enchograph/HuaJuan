package com.huajuan.aispace.screens.drawer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RemoveFromQueue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.huajuan.aispace.R
import com.huajuan.aispace.data.Conversation
import com.huajuan.aispace.ui.theme.AppDimens
@Composable
fun ContextMenu(
    conversation: Conversation,
    offset: IntOffset,
    isPinned: Boolean,
    onDismiss: () -> Unit,
    onPin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) {
                onDismiss()
            }
    ) {
        Surface(
            modifier = Modifier
                .width(250.dp)
                .align(Alignment.TopStart)
                .offset {
                    val x = offset.x.coerceAtMost(
                        (screenWidth - 250.dp).roundToPx().coerceAtLeast(0)
                    )
                    val y = offset.y.coerceAtMost(
                        (screenHeight - 150.dp).roundToPx().coerceAtLeast(0)
                    )
                    IntOffset(x, y)
                },
            tonalElevation = 8.dp,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .width(250.dp)
                    .padding(AppDimens.spacingS)
            ) {
                ContextMenuItem(
                    icon = if (isPinned) Icons.Default.RemoveFromQueue else Icons.Default.PushPin,
                    text = stringResource(if (isPinned) R.string.drawer_unpin_conversation else R.string.drawer_pin_conversation),
                    onClick = onPin
                )

                ContextMenuItem(
                    icon = Icons.Default.Edit,
                    text = stringResource(R.string.drawer_edit_conversation_name),
                    onClick = onEdit
                )

                ContextMenuItem(
                    icon = Icons.Default.Delete,
                    text = stringResource(R.string.drawer_delete_conversation),
                    textColor = MaterialTheme.colorScheme.error,
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    ListItem(
        headlineContent = {
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier
                    .size(24.dp)
                    .offset(x = (-8).dp)
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) {
                onClick()
            }
            .padding(horizontal = AppDimens.spacingS)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTitleDialog(
    conversation: Conversation,
    currentTitle: String,
    onTitleChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.drawer_edit_conversation_name),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = currentTitle,
                    onValueChange = onTitleChange,
                    label = { Text(stringResource(R.string.drawer_conversation_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (currentTitle.isNotBlank()) {
                        onSave(currentTitle)
                    }
                },
                enabled = currentTitle.isNotBlank()
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
