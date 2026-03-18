package com.huajuan.aispace.screens.drawer

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.huajuan.aispace.R
import com.huajuan.aispace.data.Conversation
import com.huajuan.aispace.data.Repository
import com.huajuan.aispace.ui.theme.AppDimens
import com.huajuan.aispace.ui.theme.HuaJuanTheme
import com.huajuan.aispace.utils.formatConversationTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 侧边栏导航
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SideDrawer(
    onChatSelected: () -> Unit,
    onConversationSelected: (String) -> Unit,
    onSettingPageSelected: () -> Unit,
    onAssistantListPageSelected: () -> Unit,
    onKnowledgeBaseSelected: () -> Unit,
    onImageGenerationSelected: () -> Unit,
    onTranslationSelected: () -> Unit,
    onFilesSelected: () -> Unit,
    onRemoteTerminalSelected: () -> Unit,
    onOpenSearch: () -> Unit,
    onDrawerDragState: DraggableState,
    onDrawerDragStopped: (Float) -> Unit,
    conversations: List<Conversation>,
    drawerWidth: Dp,
    darkTheme: Boolean,
    repository: Repository,
    currentConversationId: String?,
    currentAssistantName: String,
    currentAssistantId: String,
    isChatSelected: Boolean,
    isAssistantListSelected: Boolean,
    isKnowledgeBaseSelected: Boolean,
    isImageGenerationSelected: Boolean,
    isTranslationSelected: Boolean,
    isFilesSelected: Boolean,
    isRemoteTerminalSelected: Boolean,
    isSearchSelected: Boolean,
    isSettingsSelected: Boolean,
) {
    HuaJuanTheme(darkTheme = darkTheme) {
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

        Box(modifier = Modifier.width(drawerWidth)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(statusBarPadding)
                    .background(MaterialTheme.colorScheme.background)
            )

            ModalDrawerSheet(
                modifier = Modifier
                    .width(drawerWidth)
                    .drawBehind {
                        val shadowWidth = 8.dp.toPx()
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.12f),
                                    Color.Transparent
                                ),
                                startX = size.width - shadowWidth,
                                endX = size.width
                            ),
                            topLeft = Offset(x = size.width - shadowWidth, y = 0f),
                            size = androidx.compose.ui.geometry.Size(width = shadowWidth, height = size.height)
                        )
                    }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = onDrawerDragState,
                        onDragStopped = { velocity -> onDrawerDragStopped(velocity) }
                    ),
                drawerContainerColor = MaterialTheme.colorScheme.background,
                drawerTonalElevation = 1.dp,
                drawerShape = RectangleShape
            ) {
                var showContextMenu by remember { mutableStateOf(false) }
                var selectedConversation by remember { mutableStateOf<Conversation?>(null) }
                var showEditDialog by remember { mutableStateOf(false) }
                var newTitle by remember { mutableStateOf("") }
                val scope = rememberCoroutineScope()
                var contextMenuOffset by remember { mutableStateOf(IntOffset.Zero) }
                val conversationPositions =
                    remember { mutableStateMapOf<String, Pair<Offset, androidx.compose.ui.geometry.Size>>() }
                var pinnedConversations by remember { mutableStateOf(setOf<String>()) }
                var isPrimaryCollapsed by rememberSaveable { mutableStateOf(true) }
                val assistantCatalogVersion by repository.getAssistantCatalogVersionFlow().collectAsState()
                val currentAssistantEmoji = remember(currentAssistantId, assistantCatalogVersion) {
                    repository.getAssistantEmoji(currentAssistantId)
                }
                val showRemoteTerminal = remember { repository.getDebugMode() }

                val listState = rememberLazyListState()
                val orderedConversations = remember(conversations) {
                    conversations.mapIndexed { index, conversation -> index to conversation }
                }
                val pinnedList = remember(orderedConversations, pinnedConversations) {
                    orderedConversations.filter { (_, conversation) -> conversation.id in pinnedConversations }
                }
                val unpinnedList = remember(orderedConversations, pinnedConversations) {
                    orderedConversations.filter { (_, conversation) -> conversation.id !in pinnedConversations }
                }
                val selectedIndex = remember(conversations, currentConversationId) {
                    val id = currentConversationId ?: return@remember -1
                    conversations.indexOfFirst { it.id == id }
                }

                LaunchedEffect(currentConversationId, pinnedConversations, conversations) {
                    val targetId = currentConversationId ?: return@LaunchedEffect
                    val pinnedIndex = pinnedList.indexOfFirst { it.second.id == targetId }
                    val unpinnedIndex = unpinnedList.indexOfFirst { it.second.id == targetId }
                    val targetIndex = when {
                        pinnedIndex >= 0 && pinnedList.isNotEmpty() -> 1 + pinnedIndex
                        pinnedIndex >= 0 -> pinnedIndex
                        unpinnedIndex >= 0 && pinnedList.isNotEmpty() -> 1 + pinnedList.size + 1 + unpinnedIndex
                        unpinnedIndex >= 0 -> unpinnedIndex
                        else -> -1
                    }
                    if (targetIndex >= 0) {
                        try {
                            listState.animateScrollToItem(index = targetIndex)
                        } catch (_: Exception) {
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        DrawerPrimarySection(
                            isCollapsed = isPrimaryCollapsed,
                            onToggleCollapse = { isPrimaryCollapsed = !isPrimaryCollapsed },
                            onChatClick = onChatSelected,
                            onSearchClick = onOpenSearch,
                            onAssistantListClick = onAssistantListPageSelected,
                            onKnowledgeBaseClick = onKnowledgeBaseSelected,
                            onImageGenerationClick = onImageGenerationSelected,
                            onTranslationClick = onTranslationSelected,
                            onFilesClick = onFilesSelected,
                            onRemoteTerminalClick = onRemoteTerminalSelected,
                            isChatSelected = isChatSelected,
                            isSearchSelected = isSearchSelected,
                            isAssistantListSelected = isAssistantListSelected,
                            isKnowledgeBaseSelected = isKnowledgeBaseSelected,
                            isImageGenerationSelected = isImageGenerationSelected,
                            isTranslationSelected = isTranslationSelected,
                            isFilesSelected = isFilesSelected,
                            isRemoteTerminalSelected = isRemoteTerminalSelected,
                            showRemoteTerminal = showRemoteTerminal,
                            assistantName = currentAssistantName,
                            assistantEmoji = currentAssistantEmoji
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (pinnedConversations.isNotEmpty()) {
                                    item {
                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = pinnedConversations.isNotEmpty(),
                                            enter = fadeIn(animationSpec = tween(180)) + expandVertically(),
                                            exit = fadeOut(animationSpec = tween(180)) + shrinkVertically()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp)
                                                    .heightIn(min = 56.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(MaterialTheme.shapes.medium)
                                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                                        .border(
                                                            width = 1.dp,
                                                            color = MaterialTheme.colorScheme.outline,
                                                            shape = MaterialTheme.shapes.medium
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.PushPin,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = stringResource(R.string.drawer_pin_conversation_section),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    items(pinnedList, key = { it.first }) { (index, conversation) ->
                                        ConversationRow(
                                            conversation = conversation,
                                            isSelected = selectedIndex >= 0 && selectedIndex == index,
                                            icon = Icons.Outlined.PushPin,
                                            iconSize = 20.dp,
                                            onClick = { onConversationSelected(conversation.id) },
                                            onLongClick = {
                                                conversationPositions[conversation.id]?.let { (position, size) ->
                                                    val x = position.x.toInt()
                                                    val y = (position.y + size.height - 155).toInt()
                                                    contextMenuOffset = IntOffset(x, y)
                                                }
                                                selectedConversation = conversation
                                                showContextMenu = true
                                            },
                                            onPositioned = { position, size ->
                                                conversationPositions[conversation.id] = position to size
                                            }
                                        )
                                    }

                                    item {
                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = pinnedConversations.isNotEmpty(),
                                            enter = fadeIn(animationSpec = tween(180)),
                                            exit = fadeOut(animationSpec = tween(180))
                                        ) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant
                                            )
                                        }
                                    }
                                }

                                items(unpinnedList, key = { it.first }) { (index, conversation) ->
                                    ConversationRow(
                                        conversation = conversation,
                                        isSelected = selectedIndex >= 0 && selectedIndex == index,
                                        icon = Icons.Outlined.ChatBubbleOutline,
                                        iconSize = 20.dp,
                                        onClick = { onConversationSelected(conversation.id) },
                                        onLongClick = {
                                            conversationPositions[conversation.id]?.let { (position, size) ->
                                                val x = position.x.toInt()
                                                val y = (position.y + size.height - 155).toInt()
                                                contextMenuOffset = IntOffset(x, y)
                                            }
                                            selectedConversation = conversation
                                            showContextMenu = true
                                        },
                                        onPositioned = { position, size ->
                                            conversationPositions[conversation.id] = position to size
                                        }
                                    )
                                }
                            }

                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = AppDimens.screenPadding, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            DrawerUserFooter(
                                modifier = Modifier.padding(top = 0.dp, bottom = AppDimens.spacingS),
                                onSettingClick = onSettingPageSelected,
                                selected = isSettingsSelected
                            )
                        }
                    }

                    val context = LocalContext.current
                    if (showContextMenu && selectedConversation != null) {
                        val conversation = selectedConversation!!
                        ContextMenu(
                            conversation = conversation,
                            offset = contextMenuOffset,
                            isPinned = conversation.id in pinnedConversations,
                            onDismiss = {
                                showContextMenu = false
                                selectedConversation = null
                            },
                            onPin = {
                                if (conversation.id in pinnedConversations) {
                                    pinnedConversations = pinnedConversations - conversation.id
                                } else {
                                    pinnedConversations = pinnedConversations + conversation.id
                                }
                                showContextMenu = false
                                selectedConversation = null
                            },
                            onEdit = {
                                newTitle = conversation.title
                                showEditDialog = true
                                showContextMenu = false
                            },
                            onDelete = {
                                scope.launch {
                                    try {
                                        repository.deleteConversation(conversation.id)
                                        if (conversation.id in pinnedConversations) {
                                            pinnedConversations = pinnedConversations - conversation.id
                                        }
                                        onConversationSelected("deleted:${conversation.id}")
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, context.getString(R.string.drawer_conversation_deleted), Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, context.getString(R.string.drawer_delete_failed, e.message), Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                                showContextMenu = false
                                selectedConversation = null
                            }
                        )
                    }

                    if (showEditDialog && selectedConversation != null) {
                        val conversation = selectedConversation!!
                        EditTitleDialog(
                            conversation = conversation,
                            currentTitle = newTitle,
                            onTitleChange = { newTitle = it },
                            onDismiss = {
                                showEditDialog = false
                                selectedConversation = null
                            },
                            onSave = { title ->
                                scope.launch {
                                    try {
                                        repository.updateConversationTitle(conversation.id, title)
                                        onConversationSelected("refresh_needed")
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, context.getString(R.string.drawer_conversation_renamed), Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, context.getString(R.string.drawer_update_failed, e.message), Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                                showEditDialog = false
                                selectedConversation = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: Conversation,
    isSelected: Boolean,
    icon: ImageVector,
    iconSize: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPositioned: (Offset, androidx.compose.ui.geometry.Size) -> Unit
) {
    val context = LocalContext.current
    val containerColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(200),
        label = "conversationContainerColor"
    )
    val iconBg by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(200),
        label = "conversationIconBg"
    )
    val iconTint by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "conversationIconTint"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.005f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "conversationScale"
    )
    val indicatorColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    val subtitleText = conversation.lastMessage.ifBlank { context.getString(R.string.drawer_no_messages) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.screenPadding)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            )
            .background(color = containerColor, shape = MaterialTheme.shapes.large)
            .drawBehind {
                if (indicatorColor.alpha > 0f) {
                    val lineWidth = 3.dp.toPx()
                    drawRoundRect(
                        color = indicatorColor,
                        topLeft = Offset(x = 0f, y = size.height * 0.2f),
                        size = androidx.compose.ui.geometry.Size(width = lineWidth, height = size.height * 0.6f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )
                }
            }
            .onGloballyPositioned { coordinates ->
                val positionInRoot = coordinates.boundsInRoot().topLeft
                val size = coordinates.size.toSize()
                onPositioned(positionInRoot, size)
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .padding(horizontal = AppDimens.spacingM, vertical = AppDimens.spacingS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(MaterialTheme.shapes.small)
                .background(iconBg)
                .then(
                    if (isSelected) {
                        Modifier
                    } else {
                        Modifier.border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = MaterialTheme.shapes.small
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(iconSize)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = conversation.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitleText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatConversationTime(context, conversation.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
