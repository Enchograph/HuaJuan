package com.huajuan.aispace.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.huajuan.aispace.data.FileAttachment
import com.huajuan.aispace.R
import com.huajuan.aispace.ui.theme.Blue20
import com.huajuan.aispace.ui.theme.Blue90
import com.huajuan.aispace.ui.theme.Blue95
import com.huajuan.aispace.utils.AttachmentUtils
import java.util.Locale

data class BottomInputSecondaryStatusItem(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val onClear: () -> Unit
)

@Composable
fun BottomInputArea(
    isExpanded: Boolean,
    isPendingExpand: Boolean,
    onExpandChange: (Boolean) -> Unit,
    inputText: String = "",
    onInputTextChanged: (String) -> Unit = {},
    onSendMessage: (String) -> Unit = {},
    isStreaming: Boolean = false,
    onStopMessage: () -> Unit = {},
    onOpenImageSelector: () -> Unit = {},
    onOpenCamera: () -> Unit = {},
    onOpenFilePicker: () -> Unit = {},
    onQuickPickImage: (String) -> Unit = {},
    onOpenKnowledgeBasePicker: () -> Unit = {},
    onOpenSearchSettings: () -> Unit = {},
    onOpenMcpSettings: () -> Unit = {},
    onOpenQuickPhrasePicker: () -> Unit = {},
    currentKnowledgeBaseLabel: String = "",
    currentSearchProviderLabel: String = "",
    selectedImageUris: List<String> = emptyList(),
    onSelectedImageUrisChange: (List<String>) -> Unit = {},
    selectedFileAttachments: List<FileAttachment> = emptyList(),
    onSelectedFileAttachmentsChange: (List<FileAttachment>) -> Unit = {},
    secondaryStatusItems: List<BottomInputSecondaryStatusItem> = emptyList()
) {
    var text by remember { mutableStateOf(inputText) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var pendingFocus by remember { mutableStateOf(false) }
    val sendEnabled = text.isNotBlank() || selectedImageUris.isNotEmpty() || selectedFileAttachments.isNotEmpty()
    val expandedOrPending = isExpanded || isPendingExpand
    val sendScale by animateFloatAsState(
        targetValue = if (sendEnabled) 1f else 0.96f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "send_scale"
    )
    val expandRotation by animateFloatAsState(
        targetValue = if (expandedOrPending) 45f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "expand_rotation"
    )

    LaunchedEffect(inputText) {
        if (inputText != text) {
            text = inputText
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .imePadding()
    ) {
        SecondaryStatusRow(items = secondaryStatusItems)

        PrimaryStatusRow(
            selectedImageUris = selectedImageUris,
            onSelectedImageUrisChange = onSelectedImageUrisChange,
            selectedFileAttachments = selectedFileAttachments,
            onSelectedFileAttachmentsChange = onSelectedFileAttachmentsChange,
            onOpenImageSelector = onOpenImageSelector,
            onOpenFilePicker = onOpenFilePicker
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .padding(bottom = 6.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = {
                            text = it
                            onInputTextChanged(it)
                        },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.chat_input_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { state ->
                                if (state.isFocused && isExpanded) {
                                    onExpandChange(false)
                                }
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (isStreaming) {
                                    onStopMessage()
                                } else if (text.isNotBlank() || selectedImageUris.isNotEmpty() || selectedFileAttachments.isNotEmpty()) {
                                    onSendMessage(text)
                                    text = ""
                                    onSelectedImageUrisChange(emptyList())
                                    onSelectedFileAttachmentsChange(emptyList())
                                    focusManager.clearFocus()
                                }
                            }
                        ),
                        singleLine = true
                    )

                    if (isExpanded) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                ) {
                                    pendingFocus = true
                                    onExpandChange(false)
                                }
                        )
                    }
                }

                IconButton(
                    onClick = {
                        if (isStreaming) {
                            onStopMessage()
                        } else if (sendEnabled) {
                            onSendMessage(text)
                            text = ""
                            onSelectedImageUrisChange(emptyList())
                            onSelectedFileAttachmentsChange(emptyList())
                            focusManager.clearFocus()
                        }
                    },
                    modifier = Modifier.graphicsLayer {
                        scaleX = sendScale
                        scaleY = sendScale
                    }
                ) {
                    Icon(
                        imageVector = when {
                            isStreaming -> Icons.Filled.Stop
                            sendEnabled -> Icons.AutoMirrored.Filled.Send
                            else -> Icons.AutoMirrored.Outlined.Send
                        },
                        contentDescription = if (isStreaming) {
                            stringResource(R.string.chat_stop_message)
                        } else {
                            stringResource(R.string.chat_send_message)
                        },
                        tint = when {
                            isStreaming -> MaterialTheme.colorScheme.error
                            sendEnabled -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                IconButton(
                    onClick = {
                        if (!expandedOrPending) {
                            focusManager.clearFocus()
                        }
                        onExpandChange(!expandedOrPending)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = if (expandedOrPending) {
                            stringResource(R.string.action_close)
                        } else {
                            stringResource(R.string.chat_add)
                        },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.graphicsLayer { rotationZ = expandRotation }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
        ) {
            ExpandedInputArea(
                onOpenImageSelector = onOpenImageSelector,
                onOpenCamera = onOpenCamera,
                onOpenFilePicker = onOpenFilePicker,
                onQuickPickImage = onQuickPickImage,
                onOpenKnowledgeBasePicker = onOpenKnowledgeBasePicker,
                currentKnowledgeBaseLabel = currentKnowledgeBaseLabel,
                onOpenSearchSettings = onOpenSearchSettings,
                onOpenMcpSettings = onOpenMcpSettings,
                onOpenQuickPhrasePicker = onOpenQuickPhrasePicker,
                currentSearchProviderLabel = currentSearchProviderLabel
            )
        }
    }

    LaunchedEffect(isExpanded, pendingFocus) {
        if (!isExpanded && pendingFocus) {
            pendingFocus = false
            focusRequester.requestFocus()
        }
    }
}

@Composable
private fun PrimaryStatusRow(
    selectedImageUris: List<String>,
    onSelectedImageUrisChange: (List<String>) -> Unit,
    selectedFileAttachments: List<FileAttachment>,
    onSelectedFileAttachmentsChange: (List<FileAttachment>) -> Unit,
    onOpenImageSelector: () -> Unit,
    onOpenFilePicker: () -> Unit
) {
    val hasItems = selectedImageUris.isNotEmpty() || selectedFileAttachments.isNotEmpty()
    AnimatedVisibility(
        visible = hasItems,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(selectedImageUris, key = { uri -> "image:$uri" }) { uri ->
                ImageStatusChip(
                    uri = uri,
                    onClick = onOpenImageSelector,
                    onClear = { onSelectedImageUrisChange(selectedImageUris - uri) }
                )
            }
            items(selectedFileAttachments, key = { attachment -> "file:${attachment.id}" }) { attachment ->
                FileStatusChip(
                    attachment = attachment,
                    onClick = onOpenFilePicker,
                    onClear = {
                        onSelectedFileAttachmentsChange(
                            selectedFileAttachments.filterNot { it.id == attachment.id }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun SecondaryStatusRow(items: List<BottomInputSecondaryStatusItem>) {
    AnimatedVisibility(
        visible = items.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items, key = { item -> item.key }) { item ->
                SecondaryStatusChip(item = item)
            }
        }
    }
}

@Composable
private fun ImageStatusChip(
    uri: String,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = uri,
            contentDescription = stringResource(R.string.chat_selected_image),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        StatusChipCloseButton(
            modifier = Modifier.align(Alignment.TopEnd),
            contentDescription = stringResource(R.string.chat_remove_image),
            onClick = onClear
        )
    }
}

@Composable
private fun FileStatusChip(
    attachment: FileAttachment,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier
            .height(64.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 120.dp, max = 188.dp)
                .fillMaxHeight()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = fileIconForAttachment(attachment),
                    contentDescription = stringResource(R.string.chat_attachment),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = attachment.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = AttachmentUtils.formatFileSize(LocalContext.current, attachment.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            StatusChipCloseButton(
                modifier = Modifier.align(Alignment.TopEnd),
                contentDescription = stringResource(R.string.chat_remove_file),
                onClick = onClear
            )
        }
    }
}

@Composable
private fun SecondaryStatusChip(item: BottomInputSecondaryStatusItem) {
    val isDark = isSystemInDarkTheme()
    val chipContainerColor = if (isDark) Blue20 else Blue95
    val chipContentColor = if (isDark) Blue90 else Blue20
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(18.dp)),
        color = chipContainerColor,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = item.onClick)
                .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                modifier = Modifier.size(16.dp),
                tint = chipContentColor
            )
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodySmall,
                color = chipContentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            StatusChipCloseButton(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                borderShape = CircleShape,
                contentDescription = stringResource(R.string.action_clear),
                onClick = item.onClear
            )
        }
    }
}

@Composable
private fun StatusChipCloseButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    borderShape: Shape = CircleShape
) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .size(20.dp)
            .background(containerColor, CircleShape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                shape = borderShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = contentDescription,
            modifier = Modifier.size(12.dp),
            tint = contentColor
        )
    }
}

private fun fileIconForAttachment(attachment: FileAttachment): ImageVector {
    val fileName = attachment.displayName.lowercase(Locale.ROOT)
    val ext = fileName.substringAfterLast('.', "")
    return when {
        attachment.mimeType?.startsWith("image/") == true || ext in setOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "svg") -> Icons.Outlined.Image
        attachment.mimeType?.startsWith("video/") == true || ext in setOf("mp4", "mkv", "mov", "avi", "webm", "m4v") -> Icons.Outlined.Movie
        ext == "pdf" -> Icons.Outlined.Description
        attachment.mimeType?.startsWith("text/") == true || ext in setOf("txt", "md", "rtf") -> Icons.Outlined.TextSnippet
        ext in setOf("xls", "xlsx", "csv", "tsv", "ods") -> Icons.Outlined.TableChart
        ext in setOf("json", "xml", "yaml", "yml", "toml", "ini", "properties", "kt", "java", "js", "ts", "py", "cpp", "c", "h") -> Icons.Outlined.Code
        else -> Icons.AutoMirrored.Outlined.InsertDriveFile
    }
}

@Composable
fun ExpandedInputArea(
    onOpenImageSelector: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenFilePicker: () -> Unit,
    onQuickPickImage: (String) -> Unit,
    onOpenKnowledgeBasePicker: () -> Unit,
    onOpenSearchSettings: () -> Unit,
    onOpenMcpSettings: () -> Unit,
    onOpenQuickPhrasePicker: () -> Unit,
    currentKnowledgeBaseLabel: String,
    currentSearchProviderLabel: String
) {
    com.huajuan.aispace.screens.chat.components.ExpandedInputArea(
        onOpenImageSelector = onOpenImageSelector,
        onOpenCamera = onOpenCamera,
        onOpenFilePicker = onOpenFilePicker,
        onQuickPickImage = onQuickPickImage,
        onOpenKnowledgeBasePicker = onOpenKnowledgeBasePicker,
        currentKnowledgeBaseLabel = currentKnowledgeBaseLabel,
        onOpenSearchSettings = onOpenSearchSettings,
        onOpenMcpSettings = onOpenMcpSettings,
        onOpenQuickPhrasePicker = onOpenQuickPhrasePicker,
        currentSearchProviderLabel = currentSearchProviderLabel
    )
}
