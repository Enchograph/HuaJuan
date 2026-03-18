package com.huajuan.aispace.screens.knowledge

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SnippetFolder
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.huajuan.aispace.R
import com.huajuan.aispace.ui.theme.AppDimens
import com.huajuan.aispace.data.FileAttachment
import com.huajuan.aispace.data.KnowledgeBaseDto
import com.huajuan.aispace.data.KnowledgeCategory
import com.huajuan.aispace.data.KnowledgeJobStage
import com.huajuan.aispace.data.KnowledgeItemStatus
import com.huajuan.aispace.data.KnowledgeItemUpsert
import com.huajuan.aispace.data.Repository
import com.huajuan.aispace.data.ModelDataProvider
import com.huajuan.aispace.data.ModelCapabilityResolver
import com.huajuan.aispace.data.ModelUsage
import com.huajuan.aispace.components.settings.SettingsListItem
import com.huajuan.aispace.components.settings.SettingsSectionCard
import com.huajuan.aispace.components.SearchField
import com.huajuan.aispace.components.model.ModelRequirementDialog
import com.huajuan.aispace.components.model.ModelRequirementIssue
import com.huajuan.aispace.components.model.resolveKnowledgeModelRequirementIssue
import com.huajuan.aispace.navigation.DrawerUiResetEffect
import com.huajuan.aispace.screens.settings.sections.ModelPickerScreen
import com.huajuan.aispace.utils.AttachmentUtils
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Locale

private data class KnowledgeItem(
    val id: String,
    val title: String,
    val meta: String,
    val metaJson: String,
    val category: KnowledgeCategory,
    val status: KnowledgeItemStatus,
    val sourceUri: String,
    val mimeType: String?,
    val icon: ImageVector,
    val canRetry: Boolean
)

private data class EmbeddingOption(
    val label: String,
    val ref: String
)

private data class PendingFileImport(
    val attachment: FileAttachment,
    val sourceUri: String,
    val contentHash: String?
)

private data class FileNameConflict(
    val incoming: PendingFileImport,
    val existingItemId: String,
    val existingTitle: String,
    val remaining: List<PendingFileImport>
)

private data class KnowledgeDeleteRequest(
    val items: List<KnowledgeItem>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeBaseScreen(
    onMenuClick: () -> Unit,
    drawerUiResetRequest: Int = 0,
    repository: Repository,
    onOpenKnowledgeBaseDetail: (KnowledgeBaseDto) -> Unit,
    onOpenKnowledgeBaseCreate: () -> Unit,
    refreshToken: Int
) {
    var knowledgeBases by remember { mutableStateOf<List<KnowledgeBaseDto>>(emptyList()) }
    var kbStats by remember { mutableStateOf<Map<String, Map<String, Int>>>(emptyMap()) }
    var deleteTarget by remember { mutableStateOf<KnowledgeBaseDto?>(null) }
    val scope = rememberCoroutineScope()
    val modelOptions = remember { buildEmbeddingOptions(repository, ModelUsage.Embedding) }
    DrawerUiResetEffect(drawerUiResetRequest) {
        deleteTarget = null
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(refreshToken) {
        val loaded = repository.listKnowledgeBases()
        knowledgeBases = loaded
        val stats = mutableMapOf<String, Map<String, Int>>()
        loaded.forEach { kb ->
            stats[kb.id] = repository.getKnowledgeBaseStats(kb.id)
        }
        kbStats = stats
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.knowledge_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Outlined.Menu, contentDescription = stringResource(R.string.cd_open_drawer))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = AppDimens.screenPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppDimens.spacingM, bottom = AppDimens.spacingS),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.knowledge_my_bases),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Button(onClick = onOpenKnowledgeBaseCreate) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(AppDimens.spacingS))
                    Text(stringResource(R.string.knowledge_create))
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = AppDimens.screenPaddingBottom),
                verticalArrangement = Arrangement.spacedBy(AppDimens.listSpacing)
            ) {
                items(knowledgeBases, key = { it.id }) { kb ->
                    val stats = kbStats[kb.id].orEmpty().mapKeys { entry ->
                        labelForCategory(repository.getContext(), KnowledgeCategory.fromValue(entry.key))
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenKnowledgeBaseDetail(kb) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier.padding(AppDimens.cardPadding),
                            verticalArrangement = Arrangement.spacedBy(AppDimens.cardSpacing)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = MaterialTheme.shapes.medium
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.SnippetFolder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(AppDimens.spacingM))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = kb.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = kb.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(onClick = { deleteTarget = kb }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = stringResource(R.string.knowledge_delete_title),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(AppDimens.spacingS)
                            ) {
                                stats.forEach { (label, value) ->
                                    AssistChip(
                                        onClick = { onOpenKnowledgeBaseDetail(kb) },
                                        label = { Text(stringResource(R.string.knowledge_stat, label, value)) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.knowledge_embedding_model, resolveEmbeddingLabel(repository.getContext(), modelOptions, kb.embeddingModelRef)),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.knowledge_updated_at, formatTime(repository.getContext(), kb.updatedAt)),
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

    if (deleteTarget != null) {
        val target = deleteTarget!!
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.knowledge_delete_title)) },
            text = { Text(stringResource(R.string.knowledge_delete_message, target.name)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.deleteKnowledgeBase(target.id)
                        knowledgeBases = knowledgeBases.filterNot { it.id == target.id }
                        kbStats = kbStats - target.id
                        deleteTarget = null
                    }
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    // model picker moved into CreateKnowledgeBaseScreen
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeBaseDetailScreen(
    knowledgeBase: KnowledgeBaseDto,
    repository: Repository,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenProviderModelSettings: (String) -> Unit,
    refreshToken: Int
) {
    var kb by remember { mutableStateOf(knowledgeBase) }
    var selectedCategory by remember { mutableStateOf(KnowledgeCategory.Files) }
    var items by remember { mutableStateOf<List<KnowledgeItem>>(emptyList()) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var showWebsiteDialog by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var isIndexing by remember { mutableStateOf(false) }
    var indexingLabel by remember { mutableStateOf(repository.getContext().getString(R.string.knowledge_indexing)) }
    var folderName by remember { mutableStateOf("") }
    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }
    var websiteInput by remember { mutableStateOf("") }
    var modelRequirementIssue by remember { mutableStateOf<ModelRequirementIssue?>(null) }
    var fileNameConflict by remember { mutableStateOf<FileNameConflict?>(null) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var actionTargetItem by remember { mutableStateOf<KnowledgeItem?>(null) }
    var deleteRequest by remember { mutableStateOf<KnowledgeDeleteRequest?>(null) }
    var batchStatusText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val modelOptions = remember { buildEmbeddingOptions(repository, ModelUsage.Embedding) }
    val context = LocalContext.current
    val gson = remember { Gson() }
    val jobs by repository.getKnowledgeIndexJobsFlow().collectAsState()
    val categoryTabs = remember {
        listOf(
            KnowledgeCategory.Files,
            KnowledgeCategory.Notes,
            KnowledgeCategory.Urls,
            KnowledgeCategory.Websites,
            KnowledgeCategory.Folders
        )
    }
    val selectionMode = selectedIds.isNotEmpty()

    fun ensureKnowledgeModelReady(): Boolean {
        val issue = resolveKnowledgeModelRequirementIssue(repository, kb.embeddingModelRef)
        if (issue != null) {
            modelRequirementIssue = issue
            return false
        }
        return true
    }

    ModelRequirementDialog(
        issue = modelRequirementIssue,
        onDismiss = { modelRequirementIssue = null },
        onOpenModelPicker = onOpenSettings,
        onOpenProviderSettings = {
            parseProviderFromModelRef(kb.embeddingModelRef)?.let(onOpenProviderModelSettings)
        }
    )

    fun itemHasActiveJob(itemId: String): Boolean {
        return jobs.any { it.itemId == itemId && it.success == null }
    }

    fun toggleSelection(itemId: String) {
        selectedIds = if (itemId in selectedIds) selectedIds - itemId else selectedIds + itemId
    }

    suspend fun refreshCurrentItems() {
        items = loadItems(repository, kb.id, selectedCategory)
    }

    suspend fun deleteKnowledgeItems(targets: List<KnowledgeItem>) {
        targets.forEach { repository.removeKnowledgeItem(it.id) }
        selectedIds = emptySet()
        refreshCurrentItems()
    }

    suspend fun reindexKnowledgeItems(targets: List<KnowledgeItem>) {
        if (targets.isEmpty()) return
        if (!ensureKnowledgeModelReady()) return
        val candidates = targets.filter { it.category != KnowledgeCategory.Folders }
        if (candidates.isEmpty()) return
        val readyTargets = candidates.filterNot { itemHasActiveJob(it.id) }
        val skippedCount = candidates.size - readyTargets.size
        if (readyTargets.isEmpty()) {
            Toast.makeText(
                context,
                context.getString(R.string.knowledge_reindex_skipped_busy),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        isIndexing = true
        batchStatusText = context.getString(R.string.knowledge_batch_reindexing, readyTargets.size)
        try {
            readyTargets.forEach { target ->
                indexingLabel = context.getString(R.string.knowledge_indexing_item, target.title)
                val result = repository.rebuildKnowledgeItemIndex(target.id)
                if (!result.success) {
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.knowledge_index_failed,
                            result.message.ifBlank {
                                context.getString(R.string.knowledge_index_failed_short)
                            }
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            if (skippedCount > 0) {
                Toast.makeText(
                    context,
                    context.getString(R.string.knowledge_reindex_skipped_busy_count, skippedCount),
                    Toast.LENGTH_SHORT
                ).show()
            }
            selectedIds = emptySet()
            refreshCurrentItems()
        } finally {
            isIndexing = false
            indexingLabel = context.getString(R.string.knowledge_indexing)
            batchStatusText = null
        }
    }

    suspend fun downloadKnowledgeItems(targets: List<KnowledgeItem>) {
        val downloadable = targets.mapNotNull { buildAttachmentForKnowledgeItem(it) }
        if (downloadable.isEmpty()) {
            Toast.makeText(context, context.getString(R.string.knowledge_download_unavailable), Toast.LENGTH_SHORT).show()
            return
        }
        var successCount = 0
        downloadable.forEach { attachment ->
            val uri = AttachmentUtils.saveAttachmentToDownloads(context, attachment)
            if (uri != null) successCount++
        }
        Toast.makeText(
            context,
            if (successCount == downloadable.size) {
                context.getString(R.string.file_saved_to_downloads)
            } else {
                context.getString(R.string.knowledge_download_partial, successCount, downloadable.size)
            },
            Toast.LENGTH_SHORT
        ).show()
        selectedIds = emptySet()
    }

    suspend fun importKnowledgeFile(
        pending: PendingFileImport,
        overwriteItemId: String? = null
    ) {
        indexingLabel = context.getString(R.string.knowledge_indexing_item, pending.attachment.displayName)
        val item = repository.upsertKnowledgeItem(
            KnowledgeItemUpsert(
                id = overwriteItemId,
                kbId = kb.id,
                category = KnowledgeCategory.Files,
                title = pending.attachment.displayName,
                sourceUri = pending.sourceUri,
                mimeType = pending.attachment.mimeType,
                status = KnowledgeItemStatus.Pending,
                metaJson = buildKnowledgeFileMetaJson(gson, pending.attachment, pending.contentHash)
            )
        )
        val result = repository.rebuildKnowledgeItemIndex(item.id)
        if (!result.success) {
            Toast.makeText(
                context,
                context.getString(
                    R.string.knowledge_index_failed,
                    result.message ?: context.getString(R.string.knowledge_index_failed_short)
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    suspend fun processPendingFiles(queue: List<PendingFileImport>) {
        if (queue.isEmpty()) {
            refreshCurrentItems()
            return
        }
        isIndexing = true
        batchStatusText = context.getString(R.string.knowledge_batch_importing, queue.size)
        try {
            val existingItems = repository.listKnowledgeItems(kb.id, KnowledgeCategory.Files, null).toMutableList()
            for ((index, pending) in queue.withIndex()) {
                val duplicate = pending.contentHash?.lowercase(Locale.ROOT)?.let { hash ->
                    existingItems.firstOrNull { existing ->
                        contentHashForItem(existing)?.lowercase(Locale.ROOT) == hash
                    }
                }
                if (duplicate != null) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.knowledge_duplicate_skipped, pending.attachment.displayName),
                        Toast.LENGTH_SHORT
                    ).show()
                    continue
                }

                val incomingName = pending.attachment.displayName.trim().lowercase(Locale.ROOT)
                val sameNameItem = existingItems.firstOrNull { existing ->
                    displayTitleForItem(existing).trim().lowercase(Locale.ROOT) == incomingName
                }
                if (sameNameItem != null && !contentHashForItem(sameNameItem).equals(pending.contentHash, ignoreCase = true)) {
                    fileNameConflict = FileNameConflict(
                        incoming = pending,
                        existingItemId = sameNameItem.id,
                        existingTitle = displayTitleForItem(sameNameItem),
                        remaining = queue.drop(index + 1)
                    )
                    return
                }

                importKnowledgeFile(pending)
                existingItems += repository.listKnowledgeItems(kb.id, KnowledgeCategory.Files, null)
                    .first { existing ->
                        existing.sourceUri == pending.sourceUri &&
                            contentHashForItem(existing).equals(pending.contentHash, ignoreCase = true)
                    }
            }
            refreshCurrentItems()
        } finally {
            isIndexing = false
            indexingLabel = context.getString(R.string.knowledge_indexing)
            batchStatusText = null
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val pendingFiles = uris.mapNotNull { uri ->
                runCatching {
                    repository.getContext().contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                val attachment = com.huajuan.aispace.utils.AttachmentUtils.buildAttachmentFromUri(
                    repository.getContext(),
                    uri
                ) ?: return@mapNotNull null
                PendingFileImport(
                    attachment = attachment,
                    sourceUri = uri.toString(),
                    contentHash = computeFileMd5(attachment.localPath)
                )
            }
            processPendingFiles(pendingFiles)
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(knowledgeBase.id, refreshToken) {
        repository.getKnowledgeBase(knowledgeBase.id)?.let { kb = it }
        items = loadItems(repository, knowledgeBase.id, selectedCategory)
    }

    LaunchedEffect(selectedCategory) {
        selectedIds = emptySet()
        items = loadItems(repository, kb.id, selectedCategory)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = kb.name,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = resolveEmbeddingLabel(context, modelOptions, kb.embeddingModelRef),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.knowledge_back_to_list))
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.knowledge_search))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.knowledge_settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = AppDimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.listSpacing)
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppDimens.spacingM)
            ) {
                categoryTabs.forEach { category ->
                    SegmentedButton(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        shape = MaterialTheme.shapes.small,
                        label = { Text(labelForCategory(context, category)) }
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = AppDimens.screenPaddingBottom),
                verticalArrangement = Arrangement.spacedBy(AppDimens.cardSpacing)
            ) {
                val activeJobs = jobs.filter { it.kbId == kb.id && (it.success == null || it.success == false) }
                if (activeJobs.isNotEmpty() || !batchStatusText.isNullOrBlank()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(AppDimens.cardPadding),
                                verticalArrangement = Arrangement.spacedBy(AppDimens.spacingS)
                            ) {
                                Text(
                                    text = stringResource(R.string.knowledge_task_center_inline),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                batchStatusText?.let { summary ->
                                    Text(
                                        text = summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                activeJobs.forEach { job ->
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(job.title, fontWeight = FontWeight.Medium)
                                        Text(
                                            text = job.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (job.stage == KnowledgeJobStage.Embedding || job.totalUnits > 0 || job.totalBatches > 0) {
                                            LinearProgressIndicator(
                                                progress = { if (job.success == true) 1f else job.progress },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (selectionMode) {
                    item {
                        val visibleIds = items.map { it.id }.toSet()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = AppDimens.spacingS),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.knowledge_selected_count, selectedIds.size),
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = {
                                    selectedIds = if (selectedIds.size == visibleIds.size) emptySet() else visibleIds
                                }) {
                                    Text(
                                        if (selectedIds.size == visibleIds.size) {
                                            stringResource(R.string.action_clear_selection)
                                        } else {
                                            stringResource(R.string.action_select_all)
                                        }
                                    )
                                }
                                if (selectedCategory == KnowledgeCategory.Files) {
                                    IconButton(onClick = {
                                        val targets = items.filter { it.id in selectedIds }
                                        scope.launch { downloadKnowledgeItems(targets) }
                                    }) {
                                        Icon(
                                            Icons.Default.Download,
                                            contentDescription = stringResource(R.string.knowledge_batch_download)
                                        )
                                    }
                                }
                                if (selectedCategory != KnowledgeCategory.Folders) {
                                    IconButton(onClick = {
                                        val targets = items.filter { it.id in selectedIds }
                                        scope.launch { reindexKnowledgeItems(targets) }
                                    }) {
                                        Icon(
                                            Icons.Outlined.Refresh,
                                            contentDescription = stringResource(R.string.knowledge_batch_reindex)
                                        )
                                    }
                                }
                                IconButton(onClick = {
                                    deleteRequest = KnowledgeDeleteRequest(items.filter { it.id in selectedIds })
                                }) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = stringResource(R.string.action_delete)
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    val addLabel = when (selectedCategory) {
                        KnowledgeCategory.Files -> stringResource(R.string.knowledge_add_file)
                        KnowledgeCategory.Notes -> stringResource(R.string.knowledge_add_note)
                        KnowledgeCategory.Urls -> stringResource(R.string.knowledge_add_url)
                        KnowledgeCategory.Websites -> stringResource(R.string.knowledge_crawl_site)
                        KnowledgeCategory.Folders -> stringResource(R.string.knowledge_new_folder)
                    }
                    Button(
                        onClick = {
                            if (!ensureKnowledgeModelReady()) return@Button
                            when (selectedCategory) {
                                KnowledgeCategory.Files -> filePickerLauncher.launch(arrayOf("*/*"))
                                KnowledgeCategory.Notes -> showNoteDialog = true
                                KnowledgeCategory.Urls -> showUrlDialog = true
                                KnowledgeCategory.Websites -> showWebsiteDialog = true
                                KnowledgeCategory.Folders -> showFolderDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !selectionMode
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(AppDimens.spacingS))
                        Text(addLabel)
                    }
                }

                items(items, key = { it.id }) { item ->
                    val hasActiveJob = jobs.any { it.itemId == item.id && it.success == null }
                    val isSelected = item.id in selectedIds
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (selectionMode) {
                                        toggleSelection(item.id)
                                    }
                                },
                                onLongClick = {
                                    if (selectionMode) {
                                        toggleSelection(item.id)
                                    } else {
                                        actionTargetItem = item
                                    }
                                }
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(AppDimens.cardPadding),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectionMode) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { toggleSelection(item.id) }
                                )
                                Spacer(modifier = Modifier.width(AppDimens.spacingS))
                            }
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = MaterialTheme.shapes.small
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(AppDimens.spacingM))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = item.meta,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!selectionMode && item.canRetry) {
                                IconButton(
                                    onClick = {
                                        if (!ensureKnowledgeModelReady()) return@IconButton
                                        scope.launch {
                                            reindexKnowledgeItems(listOf(item))
                                        }
                                    },
                                    enabled = !hasActiveJob
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = stringResource(R.string.knowledge_retry_index),
                                        tint = if (hasActiveJob) {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    )
                                }
                            }
                            if (!selectionMode) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text(stringResource(R.string.knowledge_new_note)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spacingS)) {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text(stringResource(R.string.knowledge_note_title)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        label = { Text(stringResource(R.string.knowledge_note_content)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val title = noteTitle.trim().ifBlank { context.getString(R.string.knowledge_untitled_note) }
                    val content = noteContent.trim()
                    val metaJson = "{\"content\":${gson.toJson(content)}}"
                    if (!ensureKnowledgeModelReady()) return@TextButton
                    scope.launch {
                        isIndexing = true
                        indexingLabel = context.getString(R.string.knowledge_indexing_item, title)
                        val item = repository.upsertKnowledgeItem(
                            KnowledgeItemUpsert(
                                kbId = kb.id,
                                category = KnowledgeCategory.Notes,
                                title = title,
                                sourceUri = "note:$title",
                                mimeType = "text/plain",
                                status = KnowledgeItemStatus.Pending,
                                metaJson = metaJson
                            )
                        )
                        try {
                            val result = repository.rebuildKnowledgeItemIndex(item.id)
                            if (!result.success) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.knowledge_index_failed, result.message),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            items = loadItems(repository, kb.id, selectedCategory)
                        } finally {
                            isIndexing = false
                            indexingLabel = context.getString(R.string.knowledge_indexing)
                        }
                    }
                    noteTitle = ""
                    noteContent = ""
                    showNoteDialog = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text(stringResource(R.string.knowledge_add_url)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spacingS)) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text(stringResource(R.string.knowledge_url)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val url = urlInput.trim()
                    if (url.isNotBlank()) {
                        if (!ensureKnowledgeModelReady()) return@TextButton
                        val metaJson = "{\"content\":${gson.toJson(url)}}"
                        scope.launch {
                            isIndexing = true
                            indexingLabel = context.getString(R.string.knowledge_indexing_item, url)
                        val item = repository.upsertKnowledgeItem(
                            KnowledgeItemUpsert(
                                kbId = kb.id,
                                category = KnowledgeCategory.Urls,
                                title = url,
                                sourceUri = url,
                                mimeType = "text/plain",
                                status = KnowledgeItemStatus.Pending,
                                    metaJson = metaJson
                                )
                            )
                            try {
                                val result = repository.rebuildKnowledgeItemIndex(item.id)
                                if (!result.success) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.knowledge_index_failed, result.message),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                items = loadItems(repository, kb.id, selectedCategory)
                            } finally {
                                isIndexing = false
                                indexingLabel = context.getString(R.string.knowledge_indexing)
                            }
                        }
                    }
                    urlInput = ""
                    showUrlDialog = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showWebsiteDialog) {
        AlertDialog(
            onDismissRequest = { showWebsiteDialog = false },
            title = { Text(stringResource(R.string.knowledge_crawl_site)) },
            text = {
                OutlinedTextField(
                    value = websiteInput,
                    onValueChange = { websiteInput = it },
                    label = { Text(stringResource(R.string.knowledge_url)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val url = websiteInput.trim()
                    if (url.isNotBlank()) {
                        if (!ensureKnowledgeModelReady()) return@TextButton
                        scope.launch {
                            isIndexing = true
                            indexingLabel = context.getString(R.string.knowledge_crawling_and_indexing, url)
                            try {
                                val result = repository.fetchWebsiteAndIndex(kb.id, url)
                                if (!result.success) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.knowledge_crawl_failed, result.message),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                items = loadItems(repository, kb.id, selectedCategory)
                            } finally {
                                isIndexing = false
                                indexingLabel = context.getString(R.string.knowledge_indexing)
                            }
                        }
                    }
                    websiteInput = ""
                    showWebsiteDialog = false
                }) { Text(stringResource(R.string.knowledge_crawl)) }
            },
            dismissButton = {
                TextButton(onClick = { showWebsiteDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text(stringResource(R.string.knowledge_new_folder)) },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text(stringResource(R.string.knowledge_folder_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = folderName.trim().ifBlank { context.getString(R.string.knowledge_untitled_folder) }
                    scope.launch {
                        repository.upsertKnowledgeItem(
                            KnowledgeItemUpsert(
                                kbId = kb.id,
                                category = KnowledgeCategory.Folders,
                                title = name,
                                sourceUri = "folder:$name",
                                mimeType = null,
                                status = KnowledgeItemStatus.Ready,
                                metaJson = "{}"
                            )
                        )
                        items = loadItems(repository, kb.id, selectedCategory)
                    }
                    folderName = ""
                    showFolderDialog = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showFolderDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (fileNameConflict != null) {
        val conflict = fileNameConflict!!
        AlertDialog(
            onDismissRequest = { fileNameConflict = null },
            title = { Text(stringResource(R.string.knowledge_overwrite_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.knowledge_overwrite_message,
                        conflict.incoming.attachment.displayName,
                        conflict.existingTitle
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val pending = conflict.incoming
                    val overwriteId = conflict.existingItemId
                    val remaining = conflict.remaining
                    fileNameConflict = null
                    scope.launch {
                        isIndexing = true
                        try {
                            importKnowledgeFile(pending, overwriteItemId = overwriteId)
                            items = loadItems(repository, kb.id, selectedCategory)
                        } finally {
                            isIndexing = false
                            indexingLabel = context.getString(R.string.knowledge_indexing)
                        }
                        processPendingFiles(remaining)
                    }
                }) { Text(stringResource(R.string.knowledge_overwrite_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    val pending = conflict.incoming
                    val remaining = conflict.remaining
                    fileNameConflict = null
                    scope.launch {
                        isIndexing = true
                        try {
                            importKnowledgeFile(pending)
                            items = loadItems(repository, kb.id, selectedCategory)
                        } finally {
                            isIndexing = false
                            indexingLabel = context.getString(R.string.knowledge_indexing)
                        }
                        processPendingFiles(remaining)
                    }
                }) { Text(stringResource(R.string.knowledge_keep_both)) }
            }
        )
    }

    if (deleteRequest != null) {
        val request = deleteRequest!!
        AlertDialog(
            onDismissRequest = { deleteRequest = null },
            title = { Text(stringResource(R.string.knowledge_item_delete_title)) },
            text = {
                Text(
                    if (request.items.size == 1) {
                        stringResource(R.string.knowledge_item_delete_message, request.items.first().title)
                    } else {
                        stringResource(R.string.knowledge_batch_delete_message, request.items.size)
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val targets = request.items
                    deleteRequest = null
                    scope.launch { deleteKnowledgeItems(targets) }
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteRequest = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (actionTargetItem != null) {
        val item = actionTargetItem!!
        val canDownload = item.category == KnowledgeCategory.Files
        val canReindex = item.category != KnowledgeCategory.Folders
        val hasActiveJob = itemHasActiveJob(item.id)
        ModalBottomSheet(onDismissRequest = { actionTargetItem = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = AppDimens.screenPaddingBottom)
            ) {
                Text(
                    text = stringResource(R.string.knowledge_item_actions_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = AppDimens.screenPadding, vertical = AppDimens.spacingS)
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = AppDimens.screenPadding)
                )
                if (canDownload) {
                    SettingsListItem(
                        title = stringResource(R.string.action_download),
                        leading = {
                            Icon(Icons.Default.Download, contentDescription = null)
                        },
                        onClick = {
                            actionTargetItem = null
                            scope.launch { downloadKnowledgeItems(listOf(item)) }
                        },
                        itemPadding = PaddingValues(horizontal = AppDimens.screenPadding, vertical = 0.dp)
                    )
                }
                if (canReindex) {
                    SettingsListItem(
                        title = stringResource(R.string.knowledge_retry_index),
                        subtitle = if (hasActiveJob) stringResource(R.string.knowledge_reindex_busy) else null,
                        enabled = !hasActiveJob,
                        leading = {
                            Icon(Icons.Outlined.Refresh, contentDescription = null)
                        },
                        onClick = {
                            actionTargetItem = null
                            scope.launch { reindexKnowledgeItems(listOf(item)) }
                        },
                        itemPadding = PaddingValues(horizontal = AppDimens.screenPadding, vertical = 0.dp)
                    )
                }
                SettingsListItem(
                    title = stringResource(R.string.knowledge_enter_selection_mode),
                    leading = {
                        Icon(Icons.Outlined.SelectAll, contentDescription = null)
                    },
                    onClick = {
                        actionTargetItem = null
                        selectedIds = setOf(item.id)
                    },
                    itemPadding = PaddingValues(horizontal = AppDimens.screenPadding, vertical = 0.dp)
                )
                SettingsListItem(
                    title = stringResource(R.string.action_delete),
                    leading = {
                        Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    },
                    trailing = {},
                    onClick = {
                        actionTargetItem = null
                        deleteRequest = KnowledgeDeleteRequest(listOf(item))
                    },
                    itemPadding = PaddingValues(horizontal = AppDimens.screenPadding, vertical = 0.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateKnowledgeBaseScreen(
    repository: Repository,
    onBack: () -> Unit,
    onOpenProviderModelSettings: (String) -> Unit,
    onCreated: (KnowledgeBaseDto) -> Unit
) {
    val context = LocalContext.current
    val modelOptions = remember { buildEmbeddingOptions(repository) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var modelRef by remember { mutableStateOf("") }
    var showModelPicker by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var modelRequirementIssue by remember { mutableStateOf<ModelRequirementIssue?>(null) }
    val scope = rememberCoroutineScope()

    if (showModelPicker) {
        ModalBottomSheet(onDismissRequest = { showModelPicker = false }) {
            ModelPickerScreen(
                repository = repository,
                modelUsage = ModelUsage.Embedding,
                onSelectModel = { ref ->
                    modelRef = ref
                    showModelPicker = false
                }
            )
        }
    }

    ModelRequirementDialog(
        issue = modelRequirementIssue,
        onDismiss = { modelRequirementIssue = null },
        onOpenModelPicker = { showModelPicker = true },
        onOpenProviderSettings = {
            parseProviderFromModelRef(modelRef)?.let(onOpenProviderModelSettings)
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.knowledge_create), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.cd_back))
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(AppDimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spacingL)
        ) {
            SettingsSectionCard(title = stringResource(R.string.knowledge_basic_info)) {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.cardSpacing)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.knowledge_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.knowledge_description)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }

            SettingsSectionCard(title = stringResource(R.string.knowledge_embedding_title)) {
                SettingsListItem(
                    title = stringResource(R.string.knowledge_vector_model),
                    subtitle = resolveEmbeddingLabel(context, modelOptions, modelRef),
                    onClick = { showModelPicker = true }
                )
                Text(
                    text = stringResource(R.string.knowledge_embedding_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            errorText?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.action_cancel)) }
                TextButton(onClick = {
                    val trimmedName = name.trim()
                    if (trimmedName.isBlank()) {
                        errorText = context.getString(R.string.knowledge_error_missing_name)
                        return@TextButton
                    }
                    if (modelRef.isBlank()) {
                        modelRequirementIssue = ModelRequirementIssue.MissingModel
                        errorText = context.getString(R.string.knowledge_error_missing_model)
                        return@TextButton
                    }
                    val requirementIssue = resolveKnowledgeModelRequirementIssue(repository, modelRef)
                    if (requirementIssue != null) {
                        modelRequirementIssue = requirementIssue
                        return@TextButton
                    }
                    errorText = null
                    scope.launch {
                        val kb = repository.createKnowledgeBase(
                            name = trimmedName,
                            description = description.trim().ifBlank { context.getString(R.string.knowledge_new_description_default) },
                            embeddingModelRef = modelRef
                        )
                        onCreated(kb)
                    }
                }) { Text(stringResource(R.string.action_create)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeBaseSettingsScreen(
    repository: Repository,
    kbId: String,
    onBack: () -> Unit,
    onOpenProviderModelSettings: (String) -> Unit,
    onUpdated: () -> Unit,
    onDeleted: () -> Unit
) {
    val context = LocalContext.current
    val modelOptions = remember { buildEmbeddingOptions(repository, ModelUsage.Embedding) }
    var kb by remember { mutableStateOf<KnowledgeBaseDto?>(null) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var modelRef by remember { mutableStateOf("") }
    var showModelPicker by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var modelRequirementIssue by remember { mutableStateOf<ModelRequirementIssue?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(kbId) {
        val loaded = repository.getKnowledgeBase(kbId)
        kb = loaded
        name = loaded?.name.orEmpty()
        description = loaded?.description.orEmpty()
        modelRef = loaded?.embeddingModelRef.orEmpty()
    }

    if (showModelPicker) {
        ModalBottomSheet(onDismissRequest = { showModelPicker = false }) {
            ModelPickerScreen(
                repository = repository,
                modelUsage = ModelUsage.Embedding,
                onSelectModel = { ref ->
                    modelRef = ref
                    showModelPicker = false
                }
            )
        }
    }

    ModelRequirementDialog(
        issue = modelRequirementIssue,
        onDismiss = { modelRequirementIssue = null },
        onOpenModelPicker = { showModelPicker = true },
        onOpenProviderSettings = {
            parseProviderFromModelRef(modelRef)?.let(onOpenProviderModelSettings)
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.knowledge_delete_title)) },
            text = { Text(stringResource(R.string.knowledge_delete_message_generic)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.deleteKnowledgeBase(kbId)
                        onDeleted()
                    }
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.knowledge_settings), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.cd_back))
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(AppDimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spacingL)
        ) {
            if (kb == null) {
                Text(stringResource(R.string.knowledge_loading), style = MaterialTheme.typography.bodyMedium)
                return@Column
            }

            SettingsSectionCard(title = stringResource(R.string.knowledge_basic_info)) {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.cardSpacing)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.knowledge_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.knowledge_description)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }

            SettingsSectionCard(title = stringResource(R.string.knowledge_embedding_title)) {
                SettingsListItem(
                    title = stringResource(R.string.knowledge_vector_model),
                    subtitle = resolveEmbeddingLabel(context, modelOptions, modelRef),
                    onClick = { showModelPicker = true }
                )
                Text(
                    text = stringResource(R.string.knowledge_embedding_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            errorText?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.action_cancel)) }
                TextButton(onClick = {
                    val trimmedName = name.trim()
                    if (trimmedName.isBlank()) {
                        errorText = context.getString(R.string.knowledge_error_missing_name)
                        return@TextButton
                    }
                    if (modelRef.isBlank()) {
                        modelRequirementIssue = ModelRequirementIssue.MissingModel
                        errorText = context.getString(R.string.knowledge_error_missing_model)
                        return@TextButton
                    }
                    val requirementIssue = resolveKnowledgeModelRequirementIssue(repository, modelRef)
                    if (requirementIssue != null) {
                        modelRequirementIssue = requirementIssue
                        return@TextButton
                    }
                    errorText = null
                    scope.launch {
                        repository.updateKnowledgeBase(
                            kbId = kbId,
                            name = trimmedName,
                            description = description.trim().ifBlank { context.getString(R.string.knowledge_new_description_default) },
                            embeddingModelRef = modelRef
                        )
                        onUpdated()
                        onBack()
                    }
                }) { Text(stringResource(R.string.action_save)) }
            }

            TextButton(onClick = { showDeleteConfirm = true }) {
                Text(stringResource(R.string.knowledge_delete_title), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeBaseSearchScreen(
    repository: Repository,
    kbId: String,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<KnowledgeItem>>(emptyList()) }

    LaunchedEffect(kbId, query) {
        items = loadItemsByKeyword(repository, kbId, query)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.knowledge_search), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.cd_back))
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = AppDimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spacingM)
        ) {
            SearchField(
                value = query,
                onValueChange = { query = it },
                label = stringResource(R.string.knowledge_search_item_hint),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppDimens.spacingM)
            )

            LazyColumn(
                contentPadding = PaddingValues(bottom = AppDimens.screenPaddingBottom),
                verticalArrangement = Arrangement.spacedBy(AppDimens.cardSpacing)
            ) {
                items(items, key = { it.id }) { item ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppDimens.cardPadding),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = MaterialTheme.shapes.small
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(AppDimens.spacingM))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = item.meta,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildEmbeddingOptions(
    repository: Repository,
    usage: ModelUsage = ModelUsage.All
): List<EmbeddingOption> {
    val dataProvider = ModelDataProvider(repository)
    return dataProvider.getAllServiceProviders().flatMap { provider ->
        val apiUrl = dataProvider.getApiUrlForProvider(provider)
        dataProvider.getModelListForProvider(provider)
            .filter { model ->
                usage == ModelUsage.All ||
                    ModelCapabilityResolver.matchesUsage(
                        modelInfo = model,
                        providerName = provider,
                        apiUrl = apiUrl,
                        usage = usage
                    )
            }
            .map { model ->
            EmbeddingOption(
                label = "$provider · ${model.displayName}",
                ref = "$provider|${model.displayName}"
            )
        }
    }
}

private fun resolveEmbeddingLabel(context: android.content.Context, options: List<EmbeddingOption>, ref: String): String {
    return options.firstOrNull { it.ref == ref }?.label ?: ref.ifBlank { context.getString(R.string.knowledge_unselected) }
}

private fun parseProviderFromModelRef(ref: String): String? {
    val parts = ref.split("|", limit = 2)
    return parts.firstOrNull()?.takeIf { parts.size == 2 && it.isNotBlank() }
}

private fun labelForCategory(context: android.content.Context, category: KnowledgeCategory): String {
    return when (category) {
        KnowledgeCategory.Files -> context.getString(R.string.knowledge_category_files)
        KnowledgeCategory.Notes -> context.getString(R.string.knowledge_category_notes)
        KnowledgeCategory.Folders -> context.getString(R.string.knowledge_category_folders)
        KnowledgeCategory.Urls -> context.getString(R.string.knowledge_category_urls)
        KnowledgeCategory.Websites -> context.getString(R.string.knowledge_category_websites)
    }
}

private fun formatTime(context: android.content.Context, date: java.util.Date): String {
    val now = java.util.Date().time
    val diff = now - date.time
    val minutes = diff / 60000
    return when {
        minutes < 1 -> context.getString(R.string.status_just_now)
        minutes < 60 -> context.getString(R.string.time_minutes_ago, minutes.toInt())
        minutes < 1440 -> context.getString(R.string.time_hours_ago, (minutes / 60).toInt())
        else -> java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(date)
    }
}

private suspend fun loadItems(
    repository: Repository,
    kbId: String,
    category: KnowledgeCategory
): List<KnowledgeItem> {
    val items = repository.listKnowledgeItems(kbId, category, null)
    val context = repository.getContext()
    return items.map {
        val statusLabel = when (it.status) {
            KnowledgeItemStatus.Ready -> context.getString(R.string.knowledge_status_ready)
            KnowledgeItemStatus.Pending -> context.getString(R.string.knowledge_status_pending)
            KnowledgeItemStatus.Failed -> context.getString(R.string.knowledge_status_failed)
        }
        val meta = context.getString(
            R.string.knowledge_meta,
            labelForCategory(context, it.category),
            statusLabel,
            formatTime(context, it.updatedAt)
        )
        KnowledgeItem(
            id = it.id,
            title = displayTitleForItem(it),
            meta = meta,
            metaJson = it.metaJson,
            category = it.category,
            status = it.status,
            sourceUri = it.sourceUri,
            mimeType = it.mimeType,
            icon = iconForKnowledgeItem(it),
            canRetry = it.category == KnowledgeCategory.Files && it.status == KnowledgeItemStatus.Failed
        )
    }
}

private suspend fun loadItemsByKeyword(
    repository: Repository,
    kbId: String,
    keyword: String
): List<KnowledgeItem> {
    val items = repository.listKnowledgeItems(kbId, null, keyword.trim().ifBlank { null })
    val context = repository.getContext()
    return items.map {
        val statusLabel = when (it.status) {
            KnowledgeItemStatus.Ready -> context.getString(R.string.knowledge_status_ready)
            KnowledgeItemStatus.Pending -> context.getString(R.string.knowledge_status_pending)
            KnowledgeItemStatus.Failed -> context.getString(R.string.knowledge_status_failed)
        }
        val meta = context.getString(
            R.string.knowledge_meta,
            labelForCategory(context, it.category),
            statusLabel,
            formatTime(context, it.updatedAt)
        )
        KnowledgeItem(
            id = it.id,
            title = displayTitleForItem(it),
            meta = meta,
            metaJson = it.metaJson,
            category = it.category,
            status = it.status,
            sourceUri = it.sourceUri,
            mimeType = it.mimeType,
            icon = iconForKnowledgeItem(it),
            canRetry = it.category == KnowledgeCategory.Files && it.status == KnowledgeItemStatus.Failed
        )
    }
}

private fun displayTitleForItem(item: com.huajuan.aispace.data.KnowledgeItemDto): String {
    if (item.category != KnowledgeCategory.Files) return item.title
    val meta = parseKnowledgeItemMeta(item.metaJson)
    val originalName = meta?.get("originalDisplayName")?.asString
    val metaPath = meta?.get("localPath")?.asString
    val candidates = listOf(originalName, item.title, metaPath, item.sourceUri)
    return candidates.firstNotNullOfOrNull { candidate ->
        extractFileName(candidate)
    } ?: item.title
}

private fun iconForKnowledgeItem(item: com.huajuan.aispace.data.KnowledgeItemDto): ImageVector {
    return when (item.category) {
        KnowledgeCategory.Notes -> Icons.AutoMirrored.Outlined.StickyNote2
        KnowledgeCategory.Folders -> Icons.Outlined.Folder
        KnowledgeCategory.Urls -> Icons.Outlined.Language
        KnowledgeCategory.Websites -> Icons.Outlined.Language
        KnowledgeCategory.Files -> iconForFile(
            fileName = displayTitleForItem(item),
            mimeType = item.mimeType
        )
    }
}

private fun iconForFile(fileName: String, mimeType: String?): ImageVector {
    val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
    return when {
        mimeType?.startsWith("image/") == true || ext in setOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "svg") -> Icons.Outlined.Image
        mimeType?.startsWith("audio/") == true || ext in setOf("mp3", "wav", "flac", "aac", "ogg", "m4a") -> Icons.Outlined.Audiotrack
        mimeType?.startsWith("video/") == true || ext in setOf("mp4", "mkv", "mov", "avi", "webm", "m4v") -> Icons.Outlined.Movie
        ext == "pdf" -> Icons.Outlined.Description
        ext in setOf("doc", "docx", "odt", "rtf") -> Icons.AutoMirrored.Outlined.Article
        ext in setOf("xls", "xlsx", "csv", "tsv", "ods") -> Icons.Outlined.TableChart
        ext in setOf("ppt", "pptx", "key", "odp") -> Icons.Outlined.SnippetFolder
        ext in setOf("md", "txt", "log") -> Icons.AutoMirrored.Outlined.Article
        ext in setOf("json", "xml", "yaml", "yml", "toml", "ini", "properties") -> Icons.Outlined.Code
        ext in setOf("kt", "kts", "java", "js", "ts", "tsx", "jsx", "py", "c", "cc", "cpp", "h", "hpp", "go", "rs", "sh", "bat", "ps1", "swift", "rb") -> Icons.Outlined.Code
        ext in setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz") -> Icons.Outlined.Archive
        else -> Icons.AutoMirrored.Outlined.InsertDriveFile
    }
}

private fun parseKnowledgeItemMeta(metaJson: String): JsonObject? {
    return runCatching { Gson().fromJson(metaJson, JsonObject::class.java) }.getOrNull()
}

private fun contentHashForItem(item: com.huajuan.aispace.data.KnowledgeItemDto): String? {
    return parseKnowledgeItemMeta(item.metaJson)?.get("contentHash")?.asString?.takeIf { it.isNotBlank() }
}

private fun buildAttachmentForKnowledgeItem(item: KnowledgeItem): FileAttachment? {
    if (item.category != KnowledgeCategory.Files) return null
    val meta = parseKnowledgeItemMeta(item.metaJson)
    val localPath = meta?.get("localPath")?.asString
    val originalName = meta?.get("originalDisplayName")?.asString
    if (localPath.isNullOrBlank() && item.sourceUri.isBlank()) return null
    return FileAttachment(
        uri = item.sourceUri,
        localPath = localPath,
        displayName = originalName?.takeIf { it.isNotBlank() } ?: item.title,
        mimeType = item.mimeType,
        sizeBytes = localPath?.let { path -> runCatching { File(path).length() }.getOrNull() },
        isImage = item.mimeType?.startsWith("image/") == true
    )
}

private fun buildKnowledgeFileMetaJson(
    gson: Gson,
    attachment: FileAttachment,
    contentHash: String?
): String {
    val meta = JsonObject().apply {
        addProperty("localPath", attachment.localPath)
        addProperty("originalDisplayName", attachment.displayName)
        if (!contentHash.isNullOrBlank()) {
            addProperty("contentHash", contentHash)
        }
    }
    return gson.toJson(meta)
}

private fun computeFileMd5(localPath: String?): String? {
    if (localPath.isNullOrBlank()) return null
    return runCatching {
        val digest = MessageDigest.getInstance("MD5")
        FileInputStream(localPath).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read = input.read(buffer)
            while (read >= 0) {
                if (read > 0) {
                    digest.update(buffer, 0, read)
                }
                read = input.read(buffer)
            }
        }
        digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }.getOrNull()
}

private fun extractFileName(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val normalized = raw.substringBefore('?').substringBefore('#').trim()
    val pathName = runCatching { Uri.parse(normalized).lastPathSegment }.getOrNull()
    val fileName = listOf(pathName, normalized).firstNotNullOfOrNull { candidate ->
        candidate
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            ?.takeIf { it.isNotBlank() && it != ":" }
    }
    return fileName?.let { File(it).name }?.takeIf { it.isNotBlank() }
}
