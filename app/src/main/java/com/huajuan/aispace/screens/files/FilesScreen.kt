package com.huajuan.aispace.screens.files

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.FileProvider
import com.huajuan.aispace.R
import com.huajuan.aispace.components.SearchField
import com.huajuan.aispace.components.image.SmartAsyncImage
import com.huajuan.aispace.data.FileAttachment
import com.huajuan.aispace.data.Repository
import com.huajuan.aispace.navigation.DrawerUiResetEffect
import com.huajuan.aispace.ui.theme.AppDimens
import com.huajuan.aispace.utils.AttachmentUtils
import com.huajuan.aispace.utils.formatTimeAgo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

private enum class FileCategory {
    All,
    Document,
    Image,
    Text,
    Other
}

private enum class FileSort {
    Time,
    Name,
    Size
}

private enum class SortDirection {
    Ascending,
    Descending
}

private data class HistoryFileItem(
    val id: String,
    val displayName: String,
    val category: FileCategory,
    val sizeBytes: Long?,
    val timestamp: Long,
    val conversationTitle: String,
    val conversationId: String,
    val messageId: String,
    val mimeType: String?,
    val imageUri: String? = null,
    val attachment: FileAttachment? = null
)

private data class FileActionMenuState(
    val item: HistoryFileItem,
    val anchorBounds: Rect
)

private fun SortDirection.toggle(): SortDirection =
    if (this == SortDirection.Ascending) SortDirection.Descending else SortDirection.Ascending

private fun sortItems(
    items: List<HistoryFileItem>,
    sort: FileSort,
    direction: SortDirection
): List<HistoryFileItem> {
    val locale = Locale.getDefault()
    return when (sort) {
        FileSort.Time -> {
            if (direction == SortDirection.Ascending) {
                items.sortedWith(
                    compareBy<HistoryFileItem> { it.timestamp }
                        .thenBy { it.displayName.lowercase(locale) }
                )
            } else {
                items.sortedWith(
                    compareByDescending<HistoryFileItem> { it.timestamp }
                        .thenBy { it.displayName.lowercase(locale) }
                )
            }
        }

        FileSort.Name -> {
            if (direction == SortDirection.Ascending) {
                items.sortedWith(
                    compareBy<HistoryFileItem> { it.displayName.lowercase(locale) }
                        .thenByDescending { it.timestamp }
                )
            } else {
                items.sortedWith(
                    compareByDescending<HistoryFileItem> { it.displayName.lowercase(locale) }
                        .thenByDescending { it.timestamp }
                )
            }
        }

        FileSort.Size -> {
            if (direction == SortDirection.Ascending) {
                items.sortedWith(
                    compareBy<HistoryFileItem> { it.sizeBytes ?: Long.MIN_VALUE }
                        .thenBy { it.displayName.lowercase(locale) }
                )
            } else {
                items.sortedWith(
                    compareByDescending<HistoryFileItem> { it.sizeBytes ?: Long.MIN_VALUE }
                        .thenBy { it.displayName.lowercase(locale) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    onMenuClick: () -> Unit,
    drawerUiResetRequest: Int = 0,
    repository: Repository
) {
    val context = LocalContext.current
    val assistantId = remember { repository.getCurrentAssistantId() }
    var allItems by remember { mutableStateOf<List<HistoryFileItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(FileCategory.All) }
    var selectedSort by remember { mutableStateOf(FileSort.Time) }
    var sortDirection by remember { mutableStateOf(SortDirection.Descending) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var deleteTargets by remember { mutableStateOf<List<HistoryFileItem>>(emptyList()) }
    var showSortMenu by remember { mutableStateOf(false) }
    var actionMenuState by remember { mutableStateOf<FileActionMenuState?>(null) }
    val scope = rememberCoroutineScope()

    DrawerUiResetEffect(drawerUiResetRequest) {
        searchQuery = ""
        deleteTargets = emptyList()
        showSortMenu = false
        actionMenuState = null
    }

    LaunchedEffect(Unit) {
        isLoading = true
        val loaded = withContext(Dispatchers.IO) {
            val conversations = repository.getConversationsAsync(assistantId)
            val titleMap = conversations.associate { it.id to it.title }
            val result = mutableListOf<HistoryFileItem>()
            conversations.forEach { conversation ->
                val messages = repository.getMessagesAsync(conversation.id, assistantId)
                messages.forEach { message ->
                    message.attachments.forEach { attachment ->
                        val category = when {
                            attachment.isImage -> FileCategory.Image
                            attachment.mimeType?.startsWith("text/") == true -> FileCategory.Text
                            attachment.mimeType?.contains("pdf") == true ||
                                attachment.mimeType?.contains("word") == true ||
                                attachment.mimeType?.contains("powerpoint") == true ||
                                attachment.mimeType?.contains("excel") == true -> FileCategory.Document

                            else -> FileCategory.Other
                        }
                        result.add(
                            HistoryFileItem(
                                id = attachment.id,
                                displayName = attachment.displayName,
                                category = category,
                                sizeBytes = attachment.sizeBytes,
                                timestamp = message.timestamp.time,
                                conversationTitle = titleMap[conversation.id]
                                    ?: context.getString(R.string.unknown_conversation),
                                conversationId = conversation.id,
                                messageId = message.id,
                                mimeType = attachment.mimeType,
                                attachment = attachment
                            )
                        )
                    }
                    message.imageUris.forEachIndexed { index, uri ->
                        result.add(
                            HistoryFileItem(
                                id = "${message.id}_img_$index",
                                displayName = context.getString(
                                    R.string.file_image_name,
                                    message.timestamp.time
                                ),
                                category = FileCategory.Image,
                                sizeBytes = null,
                                timestamp = message.timestamp.time,
                                conversationTitle = titleMap[conversation.id]
                                    ?: context.getString(R.string.unknown_conversation),
                                conversationId = conversation.id,
                                messageId = message.id,
                                mimeType = "image/*",
                                imageUri = uri
                            )
                        )
                    }
                }
            }
            result
        }
        allItems = loaded
        isLoading = false
    }

    val filtered = remember(allItems, searchQuery, selectedCategory, selectedSort, sortDirection) {
        val normalizedQuery = searchQuery.trim().lowercase(Locale.getDefault())
        val base = allItems.filter { item ->
            val matchesCategory =
                selectedCategory == FileCategory.All || item.category == selectedCategory
            val matchesQuery = normalizedQuery.isBlank() ||
                item.displayName.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                item.conversationTitle.lowercase(Locale.getDefault()).contains(normalizedQuery)
            matchesCategory && matchesQuery
        }
        sortItems(base, selectedSort, sortDirection)
    }
    val selectionMode = selectedIds.isNotEmpty()
    val allFilteredIds = remember(filtered) { filtered.map { it.id }.toSet() }

    LaunchedEffect(allFilteredIds, selectionMode) {
        if (selectionMode && !allFilteredIds.containsAll(selectedIds)) {
            selectedIds = selectedIds.intersect(allFilteredIds)
        }
    }

    if (deleteTargets.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { deleteTargets = emptyList() },
            title = { Text(stringResource(R.string.file_delete_title)) },
            text = { Text(stringResource(R.string.file_delete_message, deleteTargets.size)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val targets = deleteTargets
                        scope.launch {
                            deleteFileItems(repository, targets, assistantId)
                            val deletedIds = targets.mapTo(mutableSetOf()) { it.id }
                            allItems = allItems.filterNot { it.id in deletedIds }
                            selectedIds = selectedIds - deletedIds
                            deleteTargets = emptyList()
                            actionMenuState = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargets = emptyList() }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.file_screen_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            Icons.Outlined.Menu,
                            contentDescription = stringResource(R.string.cd_open_drawer)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppDimens.screenPadding)
            ) {
                SearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = stringResource(R.string.file_search_hint),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppDimens.spacingM)
                )

                Spacer(modifier = Modifier.height(AppDimens.spacingM))

                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.cardSpacing)) {
                    FileCategoryTabs(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it }
                    )
                    FileSortBar(
                        selectedSort = selectedSort,
                        sortDirection = sortDirection,
                        showSortMenu = showSortMenu,
                        onShowSortMenuChange = { showSortMenu = it },
                        onSortSelected = { selectedSort = it },
                        onDirectionToggle = { sortDirection = sortDirection.toggle() }
                    )
                }

                Spacer(modifier = Modifier.height(AppDimens.spacingM))

                if (selectionMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = AppDimens.spacingS),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.file_selected_count, selectedIds.size),
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    selectedIds =
                                        if (selectedIds.size == allFilteredIds.size) emptySet() else allFilteredIds
                                }
                            ) {
                                Text(
                                    if (selectedIds.size == allFilteredIds.size) {
                                        stringResource(R.string.action_clear_selection)
                                    } else {
                                        stringResource(R.string.action_select_all)
                                    }
                                )
                            }
                            IconButton(
                                onClick = {
                                    selectedIds.forEach { id ->
                                        filtered.firstOrNull { it.id == id }?.let { item ->
                                            downloadItem(context, item)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = stringResource(R.string.file_batch_download)
                                )
                            }
                            IconButton(
                                onClick = {
                                    val items = filtered.filter { it.id in selectedIds }
                                    shareItems(context, items)
                                }
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = stringResource(R.string.file_batch_share)
                                )
                            }
                            IconButton(
                                onClick = {
                                    deleteTargets = allItems.filter { it.id in selectedIds }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.file_batch_delete)
                                )
                            }
                        }
                    }
                }

                if (isLoading) {
                    Text(
                        text = stringResource(R.string.file_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(AppDimens.spacingXxl),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.file_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(AppDimens.cardSpacing),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filtered, key = { it.id }) { item ->
                            val isSelected = item.id in selectedIds
                            var itemBounds by remember(item.id) { mutableStateOf(Rect.Zero) }

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { coordinates ->
                                        itemBounds = coordinates.boundsInWindow()
                                    }
                                    .combinedClickable(
                                        onClick = {
                                            if (selectionMode) {
                                                selectedIds =
                                                    if (isSelected) selectedIds - item.id else selectedIds + item.id
                                            } else {
                                                openItem(context, item)
                                            }
                                        },
                                        onLongClick = {
                                            if (selectionMode) {
                                                selectedIds =
                                                    if (isSelected) selectedIds - item.id else selectedIds + item.id
                                            } else {
                                                actionMenuState = FileActionMenuState(
                                                    item = item,
                                                    anchorBounds = itemBounds
                                                )
                                            }
                                        }
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(AppDimens.cardSpacing),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (selectionMode) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = {
                                                selectedIds =
                                                    if (isSelected) selectedIds - item.id else selectedIds + item.id
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(AppDimens.spacingS))
                                    }
                                    if (item.category == FileCategory.Image && item.imageUri != null) {
                                        SmartAsyncImage(
                                            imageIdOrUri = item.imageUri,
                                            repository = repository,
                                            contentDescription = item.displayName,
                                            fixedSizeDp = 52.dp,
                                            modifier = Modifier
                                                .size(52.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.secondaryContainer,
                                                    MaterialTheme.shapes.small
                                                )
                                        )
                                    } else {
                                        val icon = if (item.category == FileCategory.Image) {
                                            Icons.Default.Photo
                                        } else {
                                            Icons.AutoMirrored.Filled.InsertDriveFile
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.secondaryContainer,
                                                    MaterialTheme.shapes.small
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(AppDimens.spacingM))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.displayName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${item.conversationTitle} · ${formatTimeAgo(context, item.timestamp)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = buildMeta(context, item),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            actionMenuState?.takeUnless { selectionMode }?.let { menuState ->
                FileItemActionMenu(
                    item = menuState.item,
                    anchorBounds = menuState.anchorBounds,
                    onDismiss = { actionMenuState = null },
                    onDownload = {
                        actionMenuState = null
                        downloadItem(context, menuState.item)
                    },
                    onShare = {
                        actionMenuState = null
                        shareItems(context, listOf(menuState.item))
                    },
                    onDelete = {
                        actionMenuState = null
                        deleteTargets = listOf(menuState.item)
                    },
                    onEnterSelection = {
                        actionMenuState = null
                        selectedIds = setOf(menuState.item.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun FileItemActionMenu(
    item: HistoryFileItem,
    anchorBounds: Rect,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onEnterSelection: () -> Unit
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val menuWidth = 228.dp
    val estimatedMenuHeight = 272.dp
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.roundToPx() }
    val menuWidthPx = with(density) { menuWidth.roundToPx() }
    val estimatedMenuHeightPx = with(density) { estimatedMenuHeight.roundToPx() }
    val horizontalMarginPx = with(density) { 12.dp.roundToPx() }
    val verticalMarginPx = with(density) { 12.dp.roundToPx() }

    val preferredX = anchorBounds.right.toInt() - menuWidthPx
    val preferredY = anchorBounds.bottom.toInt() + verticalMarginPx / 2
    val fallbackY = anchorBounds.top.toInt() - estimatedMenuHeightPx - verticalMarginPx / 2
    val menuX = preferredX.coerceIn(
        horizontalMarginPx,
        (screenWidthPx - menuWidthPx - horizontalMarginPx).coerceAtLeast(horizontalMarginPx)
    )
    val menuY = if (preferredY + estimatedMenuHeightPx <= screenHeightPx - verticalMarginPx) {
        preferredY
    } else {
        fallbackY.coerceAtLeast(verticalMarginPx)
    }

    Popup(
        alignment = Alignment.TopStart,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            Surface(
                modifier = Modifier
                    .offset { IntOffset(menuX, menuY) }
                    .widthIn(min = 196.dp, max = menuWidth)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {},
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 10.dp,
                shadowElevation = 18.dp
            ) {
                Column(modifier = Modifier.padding(vertical = AppDimens.spacingS)) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = AppDimens.spacingL,
                            vertical = AppDimens.spacingS
                        )
                    ) {
                        Text(
                            text = item.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.conversationTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    FileActionMenuItem(
                        icon = Icons.Default.Download,
                        text = stringResource(R.string.action_download),
                        onClick = onDownload
                    )
                    FileActionMenuItem(
                        icon = Icons.Default.Share,
                        text = stringResource(R.string.action_share),
                        onClick = onShare
                    )
                    FileActionMenuItem(
                        icon = Icons.Default.Delete,
                        text = stringResource(R.string.action_delete),
                        textColor = MaterialTheme.colorScheme.error,
                        onClick = onDelete
                    )
                    FileActionMenuItem(
                        icon = Icons.Outlined.SelectAll,
                        text = stringResource(R.string.knowledge_enter_selection_mode),
                        onClick = onEnterSelection
                    )
                }
            }
        }
    }
}

@Composable
private fun FileActionMenuItem(
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
                tint = textColor
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
private fun FileCategoryTabs(
    selectedCategory: FileCategory,
    onCategorySelected: (FileCategory) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        FileCategory.values().forEach { category ->
            SegmentedButton(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                shape = MaterialTheme.shapes.small,
                label = { Text(categoryLabel(category)) }
            )
        }
    }
}

@Composable
private fun FileSortBar(
    selectedSort: FileSort,
    sortDirection: SortDirection,
    showSortMenu: Boolean,
    onShowSortMenuChange: (Boolean) -> Unit,
    onSortSelected: (FileSort) -> Unit,
    onDirectionToggle: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = { onShowSortMenuChange(true) },
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                tonalElevation = 1.dp
            ) {
                Text(
                    text = sortLabel(selectedSort),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            IconButton(onClick = onDirectionToggle) {
                Icon(
                    imageVector = if (sortDirection == SortDirection.Ascending) {
                        Icons.Default.ArrowUpward
                    } else {
                        Icons.Default.ArrowDownward
                    },
                    contentDescription = if (sortDirection == SortDirection.Ascending) {
                        stringResource(R.string.file_sort_to_desc)
                    } else {
                        stringResource(R.string.file_sort_to_asc)
                    }
                )
            }
        }

        DropdownMenu(
            expanded = showSortMenu,
            onDismissRequest = { onShowSortMenuChange(false) }
        ) {
            FileSort.values().forEach { sort ->
                DropdownMenuItem(
                    text = { Text(sortLabel(sort)) },
                    onClick = {
                        onSortSelected(sort)
                        onShowSortMenuChange(false)
                    }
                )
            }
        }
    }
}

private fun buildMeta(context: android.content.Context, item: HistoryFileItem): String {
    val size = AttachmentUtils.formatFileSize(context, item.sizeBytes)
    val type = context.getString(
        when (item.category) {
            FileCategory.All -> R.string.file_category_all
            FileCategory.Document -> R.string.file_category_document
            FileCategory.Image -> R.string.file_category_image
            FileCategory.Text -> R.string.file_category_text
            FileCategory.Other -> R.string.file_category_other
        }
    )
    return context.getString(R.string.file_meta, type, size)
}

private fun downloadItem(context: android.content.Context, item: HistoryFileItem) {
    val attachment = item.attachment ?: run {
        val imageUri = item.imageUri ?: run {
            Toast.makeText(
                context,
                context.getString(R.string.file_not_supported_download),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (imageUri.startsWith("http://") || imageUri.startsWith("https://")) {
            Toast.makeText(
                context,
                context.getString(R.string.file_remote_image_save_hint),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val localPath = when {
            imageUri.startsWith("file://") -> imageUri.removePrefix("file://")
            imageUri.startsWith("/") -> imageUri
            else -> null
        }
        FileAttachment(
            uri = imageUri,
            localPath = localPath,
            displayName = "${item.displayName}.png",
            mimeType = "image/png",
            sizeBytes = null,
            isImage = true
        )
    }
    CoroutineScope(Dispatchers.Main).launch {
        val uri = AttachmentUtils.saveAttachmentToDownloads(context, attachment)
        if (uri != null) {
            Toast.makeText(
                context,
                context.getString(R.string.file_saved_to_downloads),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.file_save_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

private fun shareItems(context: android.content.Context, items: List<HistoryFileItem>) {
    val uris = items.mapNotNull { resolveShareUri(context, it) }
    if (uris.isEmpty()) {
        Toast.makeText(
            context,
            context.getString(R.string.file_nothing_to_share),
            Toast.LENGTH_SHORT
        ).show()
        return
    }
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uris.first())
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
    }
    intent.type = "*/*"
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.file_share_title))
    )
}

private suspend fun deleteFileItems(
    repository: Repository,
    items: List<HistoryFileItem>,
    assistantId: String
) {
    for (item in items) {
        repository.removeAttachmentFromMessage(
            assistantId = assistantId,
            conversationId = item.conversationId,
            messageId = item.messageId,
            attachmentId = item.attachment?.id,
            imageUri = item.imageUri
        )
        item.attachment?.localPath?.let { path ->
            runCatching { File(path).delete() }
        }
        val imageId = item.imageUri
        if (imageId != null && shouldDeleteImageId(imageId)) {
            repository.deleteImageById(imageId)
        }
    }
}

private fun shouldDeleteImageId(imageUri: String): Boolean {
    return !(imageUri.startsWith("http://") ||
        imageUri.startsWith("https://") ||
        imageUri.startsWith("content://") ||
        imageUri.startsWith("file://") ||
        imageUri.startsWith("/") ||
        imageUri.startsWith("data:"))
}

private fun resolveShareUri(context: android.content.Context, item: HistoryFileItem): Uri? {
    return when {
        !item.attachment?.localPath.isNullOrBlank() -> {
            val file = File(item.attachment!!.localPath!!)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }

        item.attachment != null -> Uri.parse(item.attachment.uri)
        item.imageUri != null -> {
            val imageUri = item.imageUri
            when {
                imageUri.startsWith("file://") -> {
                    val file = File(imageUri.removePrefix("file://"))
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                }

                imageUri.startsWith("/") -> {
                    val file = File(imageUri)
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                }

                imageUri.startsWith("content://") -> Uri.parse(imageUri)
                else -> null
            }
        }

        else -> null
    }
}

private fun openItem(context: android.content.Context, item: HistoryFileItem) {
    try {
        val uri = when {
            !item.attachment?.localPath.isNullOrBlank() -> {
                val file = File(item.attachment!!.localPath!!)
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }

            item.attachment != null -> Uri.parse(item.attachment.uri)
            item.imageUri != null -> Uri.parse(item.imageUri)
            else -> null
        } ?: return
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, item.mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(
            context,
            context.getString(R.string.file_open_failed),
            Toast.LENGTH_SHORT
        ).show()
    }
}

@Composable
private fun categoryLabel(category: FileCategory): String = when (category) {
    FileCategory.All -> stringResource(R.string.file_category_all)
    FileCategory.Document -> stringResource(R.string.file_category_document)
    FileCategory.Image -> stringResource(R.string.file_category_image)
    FileCategory.Text -> stringResource(R.string.file_category_text)
    FileCategory.Other -> stringResource(R.string.file_category_other)
}

@Composable
private fun sortLabel(sort: FileSort): String = when (sort) {
    FileSort.Time -> stringResource(R.string.file_sort_time)
    FileSort.Name -> stringResource(R.string.file_sort_name)
    FileSort.Size -> stringResource(R.string.file_sort_size)
}
