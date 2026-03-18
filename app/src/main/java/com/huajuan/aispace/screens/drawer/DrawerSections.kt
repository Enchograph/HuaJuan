package com.huajuan.aispace.screens.drawer

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.huajuan.aispace.ui.theme.AppDimens
import com.huajuan.aispace.R
import com.huajuan.aispace.components.AssistantEmojiIcon

@Composable
fun DrawerUserFooter(
    onSettingClick: () -> Unit,
    selected: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.screenPadding)
            .heightIn(min = 52.dp)
            .clickable { onSettingClick() }
            .background(
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                } else {
                    androidx.compose.ui.graphics.Color.Transparent
                },
                shape = MaterialTheme.shapes.medium
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(AppDimens.spacingM))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(MaterialTheme.shapes.small)
                .background(
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = MaterialTheme.shapes.small
                )
                .then(
                    if (selected) {
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
            Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // The launcher foreground asset has built-in transparent padding.
                        // Scale it up so the visible logo fills the rounded rect.
                        scaleX = 1.5f
                        scaleY = 1.5f
                    }
            )
        }
        Spacer(modifier = Modifier.width(AppDimens.spacingM))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onSettingClick) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.settings_title),
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
fun DrawerPrimarySection(
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onChatClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAssistantListClick: () -> Unit,
    onKnowledgeBaseClick: () -> Unit,
    onImageGenerationClick: () -> Unit,
    onTranslationClick: () -> Unit,
    onFilesClick: () -> Unit,
    onRemoteTerminalClick: () -> Unit,
    isChatSelected: Boolean,
    isSearchSelected: Boolean,
    isAssistantListSelected: Boolean,
    isKnowledgeBaseSelected: Boolean,
    isImageGenerationSelected: Boolean,
    isTranslationSelected: Boolean,
    isFilesSelected: Boolean,
    isRemoteTerminalSelected: Boolean,
    showRemoteTerminal: Boolean,
    assistantName: String,
    assistantEmoji: String
) {
    val entryHeight = 52.dp
    val entrySpacing = 2.dp
    val extraCount = if (showRemoteTerminal) 5 else 4
    val extraHeight = entryHeight * extraCount.toFloat() + entrySpacing * (extraCount - 1).toFloat()
    val fullHeight = extraHeight + entrySpacing + entryHeight
    val containerHeight by animateDpAsState(
        targetValue = if (isCollapsed) {
            entryHeight
        } else {
            fullHeight
        },
        animationSpec = tween(180),
        label = "drawerPrimaryHeight"
    )
    val maskHeight = if (fullHeight > containerHeight) {
        fullHeight - containerHeight
    } else {
        0.dp
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.screenPadding, vertical = AppDimens.spacingXxs),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(AppDimens.spacingXxs)
    ) {
        DrawerEntryItem(
            title = stringResource(R.string.drawer_search),
            icon = Icons.Outlined.Search,
            iconBg = MaterialTheme.colorScheme.surfaceVariant,
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onSearchClick,
            selected = isSearchSelected,
            baseContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            showIconOutline = true
        )
        DrawerEntryItem(
            title = stringResource(R.string.drawer_chat),
            icon = Icons.Default.AutoAwesome,
            iconBg = MaterialTheme.colorScheme.primary,
            iconTint = MaterialTheme.colorScheme.onPrimary,
            onClick = onChatClick,
            selected = false,
            showIconOutline = false
        )
        DrawerEmojiEntryItem(
            title = assistantName,
            emoji = assistantEmoji,
            onClick = onAssistantListClick,
            selected = isAssistantListSelected
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(containerHeight)
                .clipToBounds()
        ) {
            Column(
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(AppDimens.spacingXxs)
            ) {
                DrawerEntryItem(
                    title = stringResource(R.string.drawer_knowledge_base),
                    icon = Icons.Default.Book,
                    iconBg = MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onKnowledgeBaseClick,
                    selected = isKnowledgeBaseSelected,
                    showIconOutline = true
                )
                DrawerEntryItem(
                    title = stringResource(R.string.drawer_image_generation),
                    icon = Icons.Default.Collections,
                    iconBg = MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onImageGenerationClick,
                    selected = isImageGenerationSelected,
                    showIconOutline = true
                )
                DrawerEntryItem(
                    title = stringResource(R.string.drawer_translation),
                    icon = Icons.Default.Translate,
                    iconBg = MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onTranslationClick,
                    selected = isTranslationSelected,
                    showIconOutline = true
                )
                DrawerEntryItem(
                    title = stringResource(R.string.drawer_files),
                    icon = Icons.Default.Folder,
                    iconBg = MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onFilesClick,
                    selected = isFilesSelected,
                    showIconOutline = true
                )
                if (showRemoteTerminal) {
                    DrawerEntryItem(
                        title = stringResource(R.string.drawer_remote_terminal),
                        icon = Icons.Default.Code,
                        iconBg = MaterialTheme.colorScheme.surfaceVariant,
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onRemoteTerminalClick,
                        selected = isRemoteTerminalSelected,
                        showIconOutline = true
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(maskHeight)
                    .align(Alignment.BottomStart)
                    .background(MaterialTheme.colorScheme.background)
            )

            DrawerEntryItem(
                title = if (isCollapsed) {
                    stringResource(R.string.drawer_expand)
                } else {
                    stringResource(R.string.drawer_collapse)
                },
                icon = if (isCollapsed) Icons.Default.UnfoldMore else Icons.Default.UnfoldLess,
                iconBg = MaterialTheme.colorScheme.surfaceVariant,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onToggleCollapse,
                selected = false,
                showIconOutline = true,
                isUtility = true,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = AppDimens.screenPadding, vertical = AppDimens.spacingS),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun DrawerEntryItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: androidx.compose.ui.graphics.Color,
    iconTint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    selected: Boolean,
    showIconOutline: Boolean,
    baseContainerColor: androidx.compose.ui.graphics.Color? = null,
    isUtility: Boolean = false,
    modifier: Modifier = Modifier
) {
    val indicatorColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        androidx.compose.ui.graphics.Color.Transparent
    }
    val textColor = if (isUtility) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        baseContainerColor ?: androidx.compose.ui.graphics.Color.Transparent
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(onClick = onClick)
            .background(
                color = containerColor,
                shape = MaterialTheme.shapes.medium
            )
            .drawBehind {
                if (indicatorColor.alpha > 0f) {
                    val lineWidth = 3.dp.toPx()
                    drawRoundRect(
                        color = indicatorColor,
                        topLeft = androidx.compose.ui.geometry.Offset(x = 0f, y = size.height * 0.2f),
                        size = androidx.compose.ui.geometry.Size(width = lineWidth, height = size.height * 0.6f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )
                }
            }
            .padding(horizontal = AppDimens.spacingM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconBg, MaterialTheme.shapes.small)
                .then(
                    if (showIconOutline) {
                        Modifier.border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = MaterialTheme.shapes.small
                        )
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(AppDimens.spacingM))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DrawerEmojiEntryItem(
    title: String,
    emoji: String,
    onClick: () -> Unit,
    selected: Boolean
) {
    val indicatorColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        androidx.compose.ui.graphics.Color.Transparent
    }
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        androidx.compose.ui.graphics.Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(onClick = onClick)
            .background(
                color = containerColor,
                shape = MaterialTheme.shapes.medium
            )
            .drawBehind {
                if (indicatorColor.alpha > 0f) {
                    val lineWidth = 3.dp.toPx()
                    drawRoundRect(
                        color = indicatorColor,
                        topLeft = androidx.compose.ui.geometry.Offset(x = 0f, y = size.height * 0.2f),
                        size = androidx.compose.ui.geometry.Size(width = lineWidth, height = size.height * 0.6f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )
                }
            }
            .padding(horizontal = AppDimens.spacingM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssistantEmojiIcon(
            emoji = emoji,
            selected = selected
        )
        Spacer(modifier = Modifier.width(AppDimens.spacingM))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
