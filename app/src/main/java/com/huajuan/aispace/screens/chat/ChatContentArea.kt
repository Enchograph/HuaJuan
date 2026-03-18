package com.huajuan.aispace.screens.chat

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.huajuan.aispace.components.image.SmartAsyncImage
import com.huajuan.aispace.R
import com.huajuan.aispace.data.Message
import com.huajuan.aispace.data.Repository
import com.huajuan.aispace.data.ToolExecutionRecord
import com.huajuan.aispace.data.WebCitation
import com.huajuan.aispace.navigation.DrawerUiResetEffect
import com.huajuan.aispace.screens.chat.components.SystemPromptBanner
import com.huajuan.aispace.utils.AttachmentUtils
import com.huajuan.aispace.utils.ThinkTagProcessor
import com.huajuan.aispace.utils.formatTime
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput

@Composable
@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
fun ChatContentArea(
    messages: List<Message>,
    repository: Repository,
    roleName: String,
    modelDisplayName: String,
    systemPrompt: String,
    prefetchNextLine: Boolean = false,
    streamingMessageId: String? = null,
    tokenAnimationUseFixedDuration: Boolean = false,
    tokenAnimationFixedDurationMs: Int = 120,
    onPrefetchLineReadyChange: (Boolean) -> Unit = {},
    listState: LazyListState,
    conversationId: String?,
    modifier: Modifier = Modifier,
    onUpdateMessageThinkVisibility: (String, Boolean) -> Unit = { _, _ -> },
    onUpdateMessageDebugPromptVisibility: (String, Boolean) -> Unit = { _, _ -> },
    onImageClick: ((List<String>, Int) -> Unit)? = null,
    onBackgroundTap: () -> Unit = {},
    onOpenAssistantSettings: () -> Unit = {},
    drawerUiResetRequest: Int = 0
) {
    val scope = rememberCoroutineScope()
    var selectedAttachment by remember { mutableStateOf<com.huajuan.aispace.data.FileAttachment?>(null) }
    var contextMenuMessage by remember { mutableStateOf<Message?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val authority = "${context.packageName}.fileprovider"
    val imageIndexMap = remember(messages) {
        var running = 0
        val map = mutableMapOf<String, Int>()
        messages.forEach { message ->
            if (message.imageUris.isNotEmpty()) {
                map[message.id] = running
                running += message.imageUris.size
            }
        }
        map
    }
    val allImages = remember(messages) { messages.flatMap { it.imageUris } }
    val bottomAnchorHeightDp = remember { Animatable(1f) }
    val nowMs by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }

    DrawerUiResetEffect(drawerUiResetRequest) {
        selectedAttachment = null
        contextMenuMessage = null
    }

    LaunchedEffect(prefetchNextLine) {
        onPrefetchLineReadyChange(false)
        val targetHeight = if (prefetchNextLine) 24f else 1f
        bottomAnchorHeightDp.animateTo(
            targetValue = targetHeight,
            animationSpec = tween(durationMillis = 140)
        )
        onPrefetchLineReadyChange(prefetchNextLine)
    }

    if (selectedAttachment != null) {
        val attachment = selectedAttachment!!
        AlertDialog(
            onDismissRequest = { selectedAttachment = null },
            title = { Text(text = attachment.displayName) },
            text = {
                Column {
                    Text(text = stringResource(R.string.chat_content_type, attachment.mimeType ?: stringResource(R.string.unknown)))
                    Text(text = stringResource(R.string.chat_content_size, AttachmentUtils.formatFileSize(context, attachment.sizeBytes)))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val uri = AttachmentUtils.saveAttachmentToDownloads(context, attachment)
                        if (uri != null) {
                            Toast.makeText(context, context.getString(R.string.file_saved_to_downloads), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, context.getString(R.string.file_save_failed), Toast.LENGTH_SHORT).show()
                        }
                        selectedAttachment = null
                    }
                }) {
                    Text(stringResource(R.string.action_download))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    try {
                        val uri = if (!attachment.localPath.isNullOrBlank()) {
                            val file = java.io.File(attachment.localPath!!)
                            FileProvider.getUriForFile(context, authority, file)
                        } else {
                            Uri.parse(attachment.uri)
                        }
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, attachment.mimeType ?: "*/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, context.getString(R.string.file_open_failed), Toast.LENGTH_SHORT).show()
                    } finally {
                        selectedAttachment = null
                    }
                }) {
                    Text(stringResource(R.string.action_open))
                }
            }
        )
    }

    if (contextMenuMessage != null) {
        val message = contextMenuMessage!!
        val hasThink = ThinkTagProcessor.containsThinkTag(message.text)
        ModalBottomSheet(
            onDismissRequest = { contextMenuMessage = null },
            sheetState = sheetState
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.action_copy)) },
                leadingContent = {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                },
                modifier = Modifier.clickable {
                    clipboardManager.setText(AnnotatedString(message.text))
                    Toast.makeText(context, context.getString(R.string.chat_copied), Toast.LENGTH_SHORT).show()
                    contextMenuMessage = null
                }
            )

            if (!message.isUser) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.action_like)) },
                    leadingContent = { Icon(Icons.Outlined.ThumbUp, contentDescription = null) },
                    modifier = Modifier.clickable {
                        Toast.makeText(context, context.getString(R.string.chat_liked), Toast.LENGTH_SHORT).show()
                        contextMenuMessage = null
                    }
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.action_dislike)) },
                    leadingContent = { Icon(Icons.Outlined.ThumbDown, contentDescription = null) },
                    modifier = Modifier.clickable {
                        Toast.makeText(context, context.getString(R.string.chat_disliked), Toast.LENGTH_SHORT).show()
                        contextMenuMessage = null
                    }
                )

                if (hasThink) {
                    ListItem(
                        headlineContent = { Text(if (message.showThink) stringResource(R.string.chat_hide_thinking) else stringResource(R.string.chat_show_thinking)) },
                        leadingContent = {
                            Icon(
                                if (message.showThink) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.clickable {
                            onUpdateMessageThinkVisibility(message.id, !message.showThink)
                            contextMenuMessage = null
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    LazyColumn(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { onBackgroundTap() })
        },
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp),
        userScrollEnabled = true,
        state = listState
    ) {
        val showDebugInfo = repository.getDebugMode()

        item {
            SystemPromptBanner(
                roleName = roleName,
                modelDisplayName = modelDisplayName,
                systemPrompt = systemPrompt.ifBlank { stringResource(R.string.default_assistant_prompt) },
                onClick = onOpenAssistantSettings
            )
        }

        if (showDebugInfo) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.chat_debug_info),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.chat_debug_conversation_id, conversationId ?: "default"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.chat_debug_message_count, messages.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(messages, key = { message -> message.id }) { message ->
            val isUser = message.isUser
            val isStreamingMessage = !isUser && message.id == streamingMessageId
            val tokenAlpha = remember(message.id) { Animatable(1f) }
            LaunchedEffect(message.text.length, isStreamingMessage) {
                if (isStreamingMessage && message.text.isNotBlank()) {
                    tokenAlpha.snapTo(0f)
                    tokenAlpha.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = if (tokenAnimationUseFixedDuration) {
                                tokenAnimationFixedDurationMs.coerceIn(40, 800)
                            } else {
                                120
                            },
                            easing = LinearEasing
                        )
                    )
                } else {
                    tokenAlpha.snapTo(1f)
                }
            }
            val sidePadding = if (isUser) Modifier.padding(start = 60.dp) else Modifier
            val timePadding = if (isUser) Modifier.padding(top = 4.dp, start = 60.dp) else Modifier.padding(top = 8.dp)
            val imageShape = if (isUser) {
                RoundedCornerShape(10.dp, 10.dp, 0.dp, 10.dp)
            } else {
                RoundedCornerShape(10.dp, 10.dp, 10.dp, 0.dp)
            }
            val fileShape = if (isUser) {
                RoundedCornerShape(12.dp, 12.dp, 0.dp, 12.dp)
            } else {
                RoundedCornerShape(12.dp, 12.dp, 12.dp, 0.dp)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(sidePadding)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                    ) {
                        if (showDebugInfo && isUser && message.debugPromptText.isNotBlank()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateContentSize()
                                    .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
                                    .combinedClickable(
                                        onClick = {
                                            onUpdateMessageDebugPromptVisibility(
                                                message.id,
                                                !message.showDebugPrompt
                                            )
                                        },
                                        onLongClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            contextMenuMessage = message
                                        }
                                    ),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                                shadowElevation = 1.dp
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = stringResource(R.string.chat_debug_request),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    AnimatedVisibility(
                                        visible = message.showDebugPrompt,
                                        enter = fadeIn(
                                            animationSpec = tween(
                                                durationMillis = 160,
                                                easing = FastOutSlowInEasing
                                            )
                                        ) + expandVertically(
                                            animationSpec = tween(
                                                durationMillis = 160,
                                                easing = FastOutSlowInEasing
                                            )
                                        ) + slideInVertically(
                                            initialOffsetY = { it / 4 },
                                            animationSpec = tween(
                                                durationMillis = 160,
                                                easing = FastOutSlowInEasing
                                            )
                                        ),
                                        exit = fadeOut(
                                            animationSpec = tween(
                                                durationMillis = 140,
                                                easing = FastOutSlowInEasing
                                            )
                                        ) + shrinkVertically(
                                            animationSpec = tween(
                                                durationMillis = 140,
                                                easing = FastOutSlowInEasing
                                            )
                                        ) + slideOutVertically(
                                            targetOffsetY = { -it / 4 },
                                            animationSpec = tween(
                                                durationMillis = 140,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(top = 8.dp)) {
                                            Text(
                                                text = message.debugPromptText,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                    if (!message.showDebugPrompt) {
                                        val preview = remember(message.debugPromptText) {
                                            message.debugPromptText
                                                .lineSequence()
                                                .toList()
                                                .take(2)
                                                .joinToString("\n")
                                        }
                                        Text(
                                            text = preview,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (message.imageUris.isNotEmpty()) {
                            if (message.imageUris.size == 1) {
                                val imageUri = message.imageUris.first()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                ) {
                                    SmartAsyncImage(
                                        imageIdOrUri = imageUri,
                                        repository = repository,
                                        contentDescription = if (isUser) stringResource(R.string.chat_selected_image) else stringResource(R.string.image_generation_title),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                        fixedSizeDp = 219.dp,
                                        modifier = Modifier
                                        .clip(imageShape)
                                        .clickable {
                                            onBackgroundTap()
                                            val start = imageIndexMap[message.id] ?: 0
                                            onImageClick?.invoke(allImages, start)
                                        }
                                    )
                                }
                            } else {
                                val scrollState = rememberScrollState()
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(if (isUser) Alignment.End else Alignment.Start),
                                    contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                                ) {
                                    Row(
                                        modifier = Modifier.horizontalScroll(scrollState),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (isUser) {
                                            val thumbSize = 88.dp
                                            message.imageUris.forEachIndexed { index, imageUri ->
                                                SmartAsyncImage(
                                                    imageIdOrUri = imageUri,
                                                    repository = repository,
                                                    contentDescription = stringResource(R.string.chat_selected_image),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                    fixedSizeDp = thumbSize,
                                                    modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        onBackgroundTap()
                                                        val start = imageIndexMap[message.id] ?: 0
                                                        onImageClick?.invoke(allImages, start + index)
                                                    }
                                                )
                                            }
                                        } else {
                                            val previewSize = 160.dp
                                            message.imageUris.forEachIndexed { index, imageUri ->
                                                SmartAsyncImage(
                                                    imageIdOrUri = imageUri,
                                                    repository = repository,
                                                    contentDescription = stringResource(R.string.image_generation_title),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                                    fixedSizeDp = previewSize,
                                                    modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        onBackgroundTap()
                                                        val start = imageIndexMap[message.id] ?: 0
                                                        onImageClick?.invoke(allImages, start + index)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (message.attachments.isNotEmpty()) {
                            message.attachments.forEach { attachment ->
                                Row(
                                    modifier = Modifier
                                        .align(if (isUser) Alignment.End else Alignment.Start)
                                        .clip(fileShape)
                                        .background(
                                            if (isUser) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable {
                                            onBackgroundTap()
                                            selectedAttachment = attachment
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                                        contentDescription = stringResource(R.string.chat_attachment),
                                        tint = if (isUser) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    Column {
                                        Text(
                                            text = attachment.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isUser) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        Text(
                                            text = AttachmentUtils.formatFileSize(context, attachment.sizeBytes),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isUser) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        if (!isUser && isStreamingMessage && message.text.isBlank()) {
                            ThinkingMessageBubble(
                                text = stringResource(R.string.chat_thinking_in_progress, 0),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
                            )
                        }
                        if (message.text.isNotBlank()) {
                            if (isUser) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateContentSize()
                                        .clip(RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                                        .combinedClickable(
                                            onClick = { onBackgroundTap() },
                                            onLongClick = {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                contextMenuMessage = message
                                            }
                                        ),
                                    color = MaterialTheme.colorScheme.primary,
                                    shadowElevation = 1.dp
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Markdown(
                                            content = message.text,
                                            colors = markdownColor(
                                                text = MaterialTheme.colorScheme.onPrimary,
                                                codeBackground = MaterialTheme.colorScheme.primaryContainer,
                                                codeText = MaterialTheme.colorScheme.onPrimaryContainer
                                            ),
                                            typography = markdownTypography(
                                                MaterialTheme.typography.bodyLarge,
                                                MaterialTheme.typography.bodyMedium,
                                                MaterialTheme.typography.headlineSmall,
                                                MaterialTheme.typography.headlineSmall,
                                                MaterialTheme.typography.titleLarge,
                                                MaterialTheme.typography.titleMedium,
                                                MaterialTheme.typography.titleSmall,
                                                MaterialTheme.typography.bodyLarge
                                            )
                                        )
                                    }
                                }
                            } else {
                                val segments = remember(message.text) {
                                    ThinkTagProcessor.splitToSegments(message.text)
                                }
                                val hasThink = remember(message.text) {
                                    segments.any { it is ThinkTagProcessor.RenderSegment.ThinkSegment }
                                }
                                val thinkMetaByIndex = remember(message.thinkMeta) {
                                    message.thinkMeta.associateBy { it.index }
                                }

                                if (isStreamingMessage && !hasThink) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateContentSize()
                                            .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
                                            .combinedClickable(
                                                onClick = { onBackgroundTap() },
                                                onLongClick = {
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    contextMenuMessage = message
                                                }
                                            ),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shadowElevation = 1.dp
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            val baseColor = MaterialTheme.colorScheme.onSurface
                                            val stableText = message.text.dropLast(1)
                                            val animatedToken = message.text.takeLast(1)
                                            Text(
                                                text = buildAnnotatedString {
                                                    append(stableText)
                                                    withStyle(SpanStyle(color = baseColor.copy(alpha = tokenAlpha.value))) {
                                                        append(animatedToken)
                                                    }
                                                },
                                                color = baseColor,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                    }
                                } else {
                                    segments.forEach { segment ->
                                        when (segment) {
                                            is ThinkTagProcessor.RenderSegment.TextSegment -> {
                                                if (segment.text.isNotBlank()) {
                                                    Surface(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .animateContentSize()
                                                            .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
                                                            .combinedClickable(
                                                                onClick = { onBackgroundTap() },
                                                                onLongClick = {
                                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                    contextMenuMessage = message
                                                                }
                                                            ),
                                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                                        shadowElevation = 1.dp
                                                    ) {
                                                        Column(modifier = Modifier.padding(16.dp)) {
                                                            Markdown(
                                                                content = segment.text,
                                                                colors = markdownColor(
                                                                    text = MaterialTheme.colorScheme.onSurface,
                                                                    codeBackground = MaterialTheme.colorScheme.secondaryContainer,
                                                                    codeText = MaterialTheme.colorScheme.onSecondaryContainer
                                                                ),
                                                                typography = markdownTypography(
                                                                    MaterialTheme.typography.bodyLarge,
                                                                    MaterialTheme.typography.bodyMedium,
                                                                    MaterialTheme.typography.headlineSmall,
                                                                    MaterialTheme.typography.headlineSmall,
                                                                    MaterialTheme.typography.titleLarge,
                                                                    MaterialTheme.typography.titleMedium,
                                                                    MaterialTheme.typography.titleSmall,
                                                                    MaterialTheme.typography.bodyLarge
                                                                )
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            is ThinkTagProcessor.RenderSegment.ThinkSegment -> {
                                                val meta = thinkMetaByIndex[segment.index]
                                                val elapsedSec = if (segment.isClosed) {
                                                    meta?.durationSec
                                                        ?: if (meta?.startAtMs != null && meta.endAtMs != null) {
                                                            (((meta.endAtMs - meta.startAtMs) / 1000L).toInt()).coerceAtLeast(0)
                                                        } else {
                                                            0
                                                        }
                                                } else {
                                                    if (meta?.startAtMs != null) {
                                                        (((nowMs - meta.startAtMs) / 1000L).toInt()).coerceAtLeast(0)
                                                    } else {
                                                        0
                                                    }
                                                }
                                                ThinkingMessageBubble(
                                                    text = if (segment.isClosed) {
                                                        stringResource(R.string.chat_thinking_done, elapsedSec)
                                                    } else {
                                                        stringResource(R.string.chat_thinking_in_progress, elapsedSec)
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .animateContentSize()
                                                        .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
                                                        .combinedClickable(
                                                            onClick = {
                                                                onUpdateMessageThinkVisibility(message.id, !message.showThink)
                                                            },
                                                            onLongClick = {
                                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                contextMenuMessage = message
                                                            }
                                                        )
                                                ) {
                                                    AnimatedVisibility(
                                                        visible = message.showThink,
                                                        enter = fadeIn(
                                                            animationSpec = tween(
                                                                durationMillis = 160,
                                                                easing = FastOutSlowInEasing
                                                            )
                                                        ) + expandVertically(
                                                            animationSpec = tween(
                                                                durationMillis = 160,
                                                                easing = FastOutSlowInEasing
                                                            )
                                                        ) + slideInVertically(
                                                            initialOffsetY = { it / 4 },
                                                            animationSpec = tween(
                                                                durationMillis = 160,
                                                                easing = FastOutSlowInEasing
                                                            )
                                                        ),
                                                        exit = fadeOut(
                                                            animationSpec = tween(
                                                                durationMillis = 140,
                                                                easing = FastOutSlowInEasing
                                                            )
                                                        ) + shrinkVertically(
                                                            animationSpec = tween(
                                                                durationMillis = 140,
                                                                easing = FastOutSlowInEasing
                                                            )
                                                        ) + slideOutVertically(
                                                            targetOffsetY = { -it / 4 },
                                                            animationSpec = tween(
                                                                durationMillis = 140,
                                                                easing = FastOutSlowInEasing
                                                            )
                                                        )
                                                    ) {
                                                        Column {
                                                            Text(
                                                                text = segment.content.ifBlank { "..." },
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                style = MaterialTheme.typography.bodyMedium
                                                            )
                                                        }
                                                    }
                                                    if (!message.showThink && segment.content.isNotBlank()) {
                                                        val previewLines = segment.content
                                                            .lineSequence()
                                                            .toList()
                                                        val preview = if (segment.isClosed) {
                                                            previewLines.take(2).joinToString("\n")
                                                        } else {
                                                            previewLines.takeLast(2).joinToString("\n")
                                                        }
                                                        Text(
                                                            text = preview,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (!isUser && message.citations.isNotEmpty()) {
                            CitationList(citations = message.citations)
                        }
                        if (!isUser && message.toolEvents.isNotEmpty()) {
                            ToolEventList(events = message.toolEvents)
                        }
                    }
                }

                Text(
                    text = formatTime(message.timestamp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = timePadding
                )
            }
        }

        // Dedicated bottom anchor for robust stick-to-bottom scrolling.
        item(key = "bottom-anchor") {
            Spacer(modifier = Modifier.height(bottomAnchorHeightDp.value.dp))
        }
    }
}

@Composable
private fun ThinkingMessageBubble(
    text: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        content()
    }
}

@Composable
private fun ToolEventList(events: List<ToolExecutionRecord>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(R.string.chat_tool_calls),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        events.sortedBy { it.timestamp.time }.forEach { event ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Text(
                        text = "${event.name} · ${event.status}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (event.inputPreview.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.chat_tool_input, event.inputPreview),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (event.outputPreview.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.chat_tool_output, event.outputPreview),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CitationList(citations: List<WebCitation>) {
    val uriHandler = LocalUriHandler.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    var selectedCitation by remember { mutableStateOf<WebCitation?>(null) }

    if (selectedCitation != null) {
        val citation = selectedCitation!!
        AlertDialog(
            onDismissRequest = { selectedCitation = null },
            title = {
                Text(text = citation.title.ifBlank { citation.url })
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (citation.snippet.isNotBlank()) {
                        Text(
                            text = citation.snippet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (citation.url.isNotBlank()) {
                        Text(
                            text = citation.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                if (citation.url.isNotBlank()) {
                    TextButton(onClick = {
                        runCatching { uriHandler.openUri(citation.url) }
                        selectedCitation = null
                    }) {
                        Text(stringResource(R.string.action_open))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCitation = null }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.ExpandMore else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.chat_sources),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = citations.size.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        citations.forEach { citation ->
                            val label = "[${citation.index}] ${citation.title.ifBlank { citation.url }}"
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable {
                                    selectedCitation = citation
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
