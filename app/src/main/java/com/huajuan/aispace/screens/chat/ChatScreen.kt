package com.huajuan.aispace.screens.chat

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.huajuan.aispace.screens.chat.BottomInputArea
import com.huajuan.aispace.screens.chat.BottomInputSecondaryStatusItem
import com.huajuan.aispace.screens.chat.ChatContentArea
import com.huajuan.aispace.screens.chat.ChatTopBar
import com.huajuan.aispace.screens.chat.components.FullScreenImageViewer
import com.huajuan.aispace.R
import com.huajuan.aispace.data.AppState
import com.huajuan.aispace.data.FileAttachment
import com.huajuan.aispace.data.KnowledgeBaseDto
import com.huajuan.aispace.data.Message
import com.huajuan.aispace.data.Repository
import com.huajuan.aispace.data.RuntimeMcpConfig
import com.huajuan.aispace.data.RuntimeWebSearchConfig
import com.huajuan.aispace.data.repository.ChatRepository
import com.huajuan.aispace.data.ModelSelectionValidator
import com.huajuan.aispace.utils.AttachmentUtils
import com.huajuan.aispace.utils.ThinkTagProcessor
import com.huajuan.aispace.components.model.ModelRequirementDialog
import com.huajuan.aispace.components.model.ModelRequirementIssue
import com.huajuan.aispace.components.model.resolveModelRequirementIssue
import com.huajuan.aispace.navigation.DrawerUiResetEffect
import com.huajuan.aispace.viewmodel.ChatViewEvent
import com.huajuan.aispace.viewmodel.ChatViewModel
import com.huajuan.aispace.data.ModelRequestResolver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

/**
 * 聊天界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onMenuClick: () -> Unit,
    appState: AppState,
    onAppStateChange: (AppState) -> Unit,
    refreshConversations: suspend (String) -> Unit,
    repository: Repository,
    drawerUiResetRequest: Int = 0,
    collapseInputRequest: Int = 0,
    onInputOverlayVisibleChange: (Boolean) -> Unit = {},
    onOpenAssistantSettings: () -> Unit = {},
    onOpenProviderModelSettings: (String) -> Unit = {},
    deletedConversationId: String? = null,
    deletedConversationVersion: Int = 0,
    targetMessageId: String? = null,
    onTargetMessageConsumed: () -> Unit = {},
    onOpenImageSelector: () -> Unit = {},
    selectedImageUris: List<String> = emptyList(),
    onSelectedImageUrisChange: (List<String>) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val vm: ChatViewModel = viewModel(
        factory = ChatViewModel.Factory(ChatRepository(repository))
    )
    val uiState by vm.uiState.collectAsState()
    val currentSession = uiState.currentSession
    val focusManager = LocalFocusManager.current
    val assistantId = appState.currentAssistantId ?: repository.getCurrentAssistantId()

    var isExpanded by remember { mutableStateOf(false) }
    var pendingExpand by remember { mutableStateOf(false) }
    var selectedFileAttachments by remember { mutableStateOf<List<FileAttachment>>(emptyList()) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Message>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var knowledgeBases by remember { mutableStateOf<List<KnowledgeBaseDto>>(emptyList()) }
    var selectedKnowledgeBaseIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var showKnowledgePicker by remember { mutableStateOf(false) }
    var modelRequirementIssue by remember { mutableStateOf<ModelRequirementIssue?>(null) }
    var showWebSearchPicker by remember { mutableStateOf(false) }
    var selectedWebSearchProvider by remember { mutableStateOf(repository.getSearchEngine()) }
    var webSearchArmed by remember { mutableStateOf(false) }
    var showMcpPicker by remember { mutableStateOf(false) }
    var mcpArmed by remember { mutableStateOf(false) }
    var showQuickPhrasePicker by remember { mutableStateOf(false) }
    var quickPhrases by remember { mutableStateOf<List<String>>(emptyList()) }
    // Image viewer state: when non-null, show full-screen image viewer
    var viewingImageUris by remember { mutableStateOf<List<String>>(emptyList()) }
    var viewingImageIndex by remember { mutableStateOf(0) }

    val context = LocalContext.current
    val authority = "${context.packageName}.fileprovider"

    val currentConversationTitle = appState.conversations
        .firstOrNull { it.id == appState.currentConversationId }
        ?.title
        ?: context.getString(R.string.conversation_new)
    val modelDisplayName = ModelRequestResolver.resolve(repository, assistantId).modelInfo.displayName

    fun collapseInputArea() {
        if (isExpanded) {
            isExpanded = false
        }
        pendingExpand = false
        focusManager.clearFocus()
    }

    LaunchedEffect(vm) {
        vm.events.collectLatest { event ->
            when (event) {
                is ChatViewEvent.Toast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LaunchedEffect(deletedConversationVersion) {
        val conversationId = deletedConversationId ?: return@LaunchedEffect
        if (deletedConversationVersion > 0) {
            vm.removeConversation(conversationId)
        }
    }

    DrawerUiResetEffect(drawerUiResetRequest) {
        collapseInputArea()
        showSearch = false
        searchQuery = ""
        searchResults = emptyList()
        isSearching = false
        showKnowledgePicker = false
        showWebSearchPicker = false
        showMcpPicker = false
        showQuickPhrasePicker = false
        modelRequirementIssue = null
        viewingImageUris = emptyList()
    }

    fun ensureModelAccessReady(): Boolean {
        val issue = resolveModelRequirementIssue(repository, assistantId)
        if (issue != null) {
            modelRequirementIssue = issue
            return false
        }
        return true
    }

    fun supportsImageUpload(): Boolean {
        if (!ensureModelAccessReady()) return false
        val resolved = ModelRequestResolver.resolve(repository, assistantId)
        if (!repository.getAssistantModelConfig(assistantId).useCloudModel) return false
        if (resolved.isImageGenerationService) return false
        val name = "${resolved.selectedModelDisplayName} ${resolved.modelInfo.apiCode} ${resolved.serviceProvider}"
            .lowercase()
        val keywords = listOf(
            "vl", "vision", "image", "gemini", "gpt-4o", "gpt-4.5", "gpt-4",
            "claude", "qwen", "doubao", "seedream", "omni"
        )
        return keywords.any { name.contains(it) }
    }

    fun supportsFileUpload(): Boolean {
        if (!ensureModelAccessReady()) return false
        val resolved = ModelRequestResolver.resolve(repository, assistantId)
        if (!repository.getAssistantModelConfig(assistantId).useCloudModel) return false
        if (resolved.isImageGenerationService) return false
        return true
    }

    fun getWebSearchUnavailableReason(providerId: String = selectedWebSearchProvider): String? {
        val provider = com.huajuan.aispace.data.WebSearchProviders.findById(providerId)
            ?: return context.getString(R.string.chat_search_provider_unavailable)
        if (provider.id == com.huajuan.aispace.data.WebSearchProviderIds.Builtin) {
            if (!repository.supportsBuiltinWebSearch(assistantId)) {
                return context.getString(R.string.chat_search_builtin_unsupported)
            }
            return null
        }
        if (provider.requiresApiKey) {
            val hasProviderKey = repository.getWebSearchProviderApiKey(provider.id).isNotBlank()
            val hasFallbackKey = repository.getWebSearchApiKey().isNotBlank()
            if (!hasProviderKey && !hasFallbackKey) {
                return context.getString(R.string.chat_search_provider_requires_config, provider.label(context))
            }
        }
        return null
    }

    fun canEnableWebSearch(providerId: String = selectedWebSearchProvider): Boolean {
        return getWebSearchUnavailableReason(providerId) == null
    }

    fun getMcpUnavailableReason(): String? {
        if (!repository.getMcpToolsEnabled()) {
            return context.getString(R.string.chat_mcp_tools_disabled)
        }
        if (repository.getMcpServerUrls().isEmpty()) {
            return context.getString(R.string.chat_mcp_global_config_incomplete)
        }
        return null
    }

    fun canEnableMcp(): Boolean = getMcpUnavailableReason() == null

    val cameraUriState = remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = cameraUriState.value
        if (success && uri != null) {
            onSelectedImageUrisChange(selectedImageUris + uri.toString())
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val tempFile = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, authority, tempFile)
            cameraUriState.value = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, context.getString(R.string.chat_camera_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        if (!supportsFileUpload()) {
            Toast.makeText(context, context.getString(R.string.chat_model_not_support_file_upload), Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        val attachments = uris.mapNotNull { AttachmentUtils.buildAttachmentFromUri(context, it) }
        val (images, files) = attachments.partition { it.isImage }
        if (images.isNotEmpty()) {
            if (!supportsImageUpload()) {
                Toast.makeText(context, context.getString(R.string.chat_model_not_support_image_upload), Toast.LENGTH_SHORT).show()
            } else {
                val imageUris = images.mapNotNull { it.localPath ?: it.uri }
                onSelectedImageUrisChange(selectedImageUris + imageUris)
            }
        }
        if (files.isNotEmpty()) {
            val supportedFiles = files.filter { it.mimeType?.startsWith("text/") == true }
            val rejected = files.size - supportedFiles.size
            if (rejected > 0) {
                Toast.makeText(context, context.getString(R.string.chat_only_text_file_supported), Toast.LENGTH_SHORT).show()
            }
            if (supportedFiles.isNotEmpty()) {
                selectedFileAttachments = selectedFileAttachments + supportedFiles
            }
        }
    }

    ModelRequirementDialog(
        issue = modelRequirementIssue,
        onDismiss = { modelRequirementIssue = null },
        onOpenModelPicker = onOpenAssistantSettings,
        onOpenProviderSettings = {
            val provider = repository.getAssistantModelConfig(assistantId).serviceProvider
            onOpenProviderModelSettings(provider)
        }
    )

    // Image viewer dialog
    if (viewingImageUris.isNotEmpty()) {
        FullScreenImageViewer(
            imageUris = viewingImageUris,
            initialIndex = viewingImageIndex,
            repository = repository,
            onDismiss = { viewingImageUris = emptyList() }
        )
    }

    if (showKnowledgePicker) {
        ModalBottomSheet(onDismissRequest = { showKnowledgePicker = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.chat_choose_knowledge_base), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (knowledgeBases.isEmpty()) {
                    Text(stringResource(R.string.chat_no_knowledge_base), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    knowledgeBases.forEach { kb ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val next = selectedKnowledgeBaseIds.toMutableSet().apply {
                                        if (contains(kb.id)) remove(kb.id) else add(kb.id)
                                    }.toList()
                                    selectedKnowledgeBaseIds = next
                                    val conversationId = appState.currentConversationId
                                    if (conversationId != null) {
                                        scope.launch {
                                            repository.setConversationKbSelection(conversationId, next)
                                        }
                                    }
                                },
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(kb.name, fontWeight = FontWeight.Medium)
                                Text(
                                    kb.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Checkbox(
                                checked = selectedKnowledgeBaseIds.contains(kb.id),
                                onCheckedChange = { checked ->
                                    val next = selectedKnowledgeBaseIds.toMutableSet().apply {
                                        if (checked) add(kb.id) else remove(kb.id)
                                    }.toList()
                                    selectedKnowledgeBaseIds = next
                                    val conversationId = appState.currentConversationId
                                    if (conversationId != null) {
                                        scope.launch {
                                            repository.setConversationKbSelection(conversationId, next)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(appState.currentConversationId, assistantId) {
        val conversationId = appState.currentConversationId ?: return@LaunchedEffect
        showSearch = false
        searchQuery = ""
        searchResults = emptyList()
        isSearching = false
        vm.bindConversation(conversationId, assistantId)
        knowledgeBases = repository.listKnowledgeBases()
        val stored = repository.getConversationKbSelection(conversationId)
        val filtered = stored.filter { id -> knowledgeBases.any { it.id == id } }
        selectedKnowledgeBaseIds = filtered
        selectedWebSearchProvider = repository.getConversationWebSearchEngine(
            conversationId,
            repository.getSearchEngine()
        )
        webSearchArmed = false
        mcpArmed = false
        quickPhrases = repository.getAssistantCommonPhrases(assistantId)
        repository.setRuntimeWebSearchConfig(null)
        repository.setRuntimeMcpConfig(null)
    }

    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val fallbackLineHeightPx = with(density) {
        val style = MaterialTheme.typography.bodyLarge
        if (style.lineHeight.isSpecified) {
            style.lineHeight.toPx()
        } else {
            style.fontSize.toPx() * 1.4f
        }
    }

    var shouldStickToBottom by rememberSaveable { mutableStateOf(true) }
    var isProgrammaticScroll by remember { mutableStateOf(false) }
    var isPrefetchLineReady by remember { mutableStateOf(false) }
    val autoScrollEnabled = searchQuery.isBlank()
    var trackedTailMessageId by remember { mutableStateOf<String?>(null) }
    var trackedTailMessageSizePx by remember { mutableIntStateOf(0) }
    val tokenAnimationUseFixedDuration = repository.getTokenAnimationFixedDurationEnabled()
    val tokenAnimationFixedDurationMs = repository.getTokenAnimationFixedDurationMs().coerceIn(40, 800)

    val isNearBottom by remember(listState) {
        derivedStateOf {
            val layout = listState.layoutInfo
            val total = layout.totalItemsCount
            if (total == 0) return@derivedStateOf true
            val lastVisible = layout.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf true
            val lastIndex = total - 1
            val withinLastItem = lastVisible.index >= lastIndex
            val distanceFromBottom = layout.viewportEndOffset - (lastVisible.offset + lastVisible.size)
            withinLastItem && distanceFromBottom <= 96
        }
    }

    if (showWebSearchPicker) {
        ModalBottomSheet(onDismissRequest = { showWebSearchPicker = false }) {
            val providers = com.huajuan.aispace.data.WebSearchProviders.all
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.chat_web_search_dialog_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.chat_only_affects_current_conversation),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                providers.forEach { provider ->
                    val isSelected = webSearchArmed && selectedWebSearchProvider == provider.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isSelected) {
                                    webSearchArmed = false
                                    repository.setRuntimeWebSearchConfig(null)
                                } else {
                                    val reason = getWebSearchUnavailableReason(provider.id)
                                    if (reason != null) {
                                        Toast.makeText(context, reason, Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }
                                    val conversationId = appState.currentConversationId
                                    if (conversationId != null) {
                                        repository.setConversationWebSearchEngine(conversationId, provider.id)
                                    }
                                    selectedWebSearchProvider = provider.id
                                    webSearchArmed = true
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(provider.label, fontWeight = FontWeight.Medium)
                            val hint = if (provider.requiresApiKey) {
                                stringResource(R.string.chat_requires_api_key)
                            } else {
                                stringResource(R.string.settings_search_builtin_available)
                            }
                            Text(
                                hint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                if (!checked) {
                                    webSearchArmed = false
                                    repository.setRuntimeWebSearchConfig(null)
                                } else {
                                    val reason = getWebSearchUnavailableReason(provider.id)
                                    if (reason != null) {
                                        Toast.makeText(context, reason, Toast.LENGTH_SHORT).show()
                                        return@Checkbox
                                    }
                                    val conversationId = appState.currentConversationId
                                    if (conversationId != null) {
                                        repository.setConversationWebSearchEngine(conversationId, provider.id)
                                    }
                                    selectedWebSearchProvider = provider.id
                                    webSearchArmed = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showMcpPicker) {
        ModalBottomSheet(onDismissRequest = { showMcpPicker = false }) {
            val mcpGloballyAvailable = repository.getMcpToolsEnabled() && repository.getMcpServerUrls().isNotEmpty()
            val enabledTools = repository.getMcpEnabledTools().toList().sorted()
            val serverUrls = repository.getMcpServerUrls().toList().sorted()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.chat_mcp_dialog_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (!mcpGloballyAvailable) {
                    Text(
                        stringResource(R.string.chat_mcp_global_config_incomplete),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.chat_mcp_enable), fontWeight = FontWeight.Medium)
                            Text(
                                stringResource(R.string.chat_only_affects_current_conversation),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = mcpArmed,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    val reason = getMcpUnavailableReason()
                                    if (reason != null) {
                                        Toast.makeText(context, reason, Toast.LENGTH_SHORT).show()
                                    } else {
                                        mcpArmed = true
                                    }
                                } else {
                                    mcpArmed = false
                                    repository.setRuntimeMcpConfig(null)
                                }
                            }
                        )
                    }
                    Text(
                        text = stringResource(R.string.chat_enabled_tools, enabledTools.joinToString().ifBlank { stringResource(R.string.chat_none) }),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.chat_configured_servers, serverUrls.joinToString().ifBlank { stringResource(R.string.chat_none) }),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showMcpPicker = false }) { Text(stringResource(R.string.action_done)) }
                }
            }
        }
    }

    if (showQuickPhrasePicker) {
        ModalBottomSheet(onDismissRequest = { showQuickPhrasePicker = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.chat_quick_phrase_dialog_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (quickPhrases.isEmpty()) {
                    Text(
                        stringResource(R.string.chat_no_quick_phrases_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    quickPhrases.forEach { phrase ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val currentText = uiState.chatState.inputText
                                    val nextText = if (currentText.isBlank()) phrase else "$currentText\n$phrase"
                                    vm.updateInputText(nextText)
                                    showQuickPhrasePicker = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(text = phrase, modifier = Modifier.weight(1f))
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showQuickPhrasePicker = false }) { Text(stringResource(R.string.action_close)) }
                }
            }
        }
    }

    val overlaySignature by remember {
        derivedStateOf { "${if (imeVisible) 1 else 0}-${if (isExpanded) 1 else 0}-${if (pendingExpand) 1 else 0}" }
    }
    var lastOverlaySignature by remember { mutableStateOf(overlaySignature) }
    var lastNearBottom by remember { mutableStateOf(true) }

    suspend fun scrollToBottom(immediate: Boolean) {
        val firstTarget = listState.layoutInfo.totalItemsCount - 1
        if (firstTarget < 0) return
        isProgrammaticScroll = true
        try {
            if (immediate) {
                listState.scrollToItem(firstTarget)
            } else {
                listState.animateScrollToItem(firstTarget)
            }
            // Re-align after next frame for dynamic content height changes (markdown/images).
            withFrameNanos { }
            val secondTarget = listState.layoutInfo.totalItemsCount - 1
            if (secondTarget >= 0) {
                listState.scrollToItem(secondTarget)
            }
        } catch (_: Exception) {
        } finally {
            isProgrammaticScroll = false
        }
    }

    suspend fun scrollByCurrentOutputLineIfNeeded() {
        if (isNearBottom) return
        val total = listState.layoutInfo.totalItemsCount
        if (total <= 0) return
        val currentTailId = currentSession.chatState.messages.lastOrNull()?.id
        val tailInfo = currentTailId?.let { targetId ->
            listState.layoutInfo.visibleItemsInfo.lastOrNull { info -> info.key == targetId }
        }
        val stepPx = when {
            tailInfo == null -> fallbackLineHeightPx
            trackedTailMessageId != currentTailId -> {
                trackedTailMessageId = currentTailId
                trackedTailMessageSizePx = tailInfo.size
                fallbackLineHeightPx
            }
            else -> {
                val growth = (tailInfo.size - trackedTailMessageSizePx).coerceAtLeast(0)
                trackedTailMessageSizePx = tailInfo.size
                if (growth > 0) growth.toFloat() else fallbackLineHeightPx
            }
        }
        isProgrammaticScroll = true
        try {
            listState.scrollBy(stepPx)
        } catch (_: Exception) {
        } finally {
            isProgrammaticScroll = false
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { isNearBottom }
            .collectLatest { atBottom ->
                if (!isProgrammaticScroll) {
                    shouldStickToBottom = atBottom
                }
            }
    }

    val lastMessageRenderKey by remember(currentSession.chatState.messages) {
        derivedStateOf {
            val last = currentSession.chatState.messages.lastOrNull() ?: return@derivedStateOf ""
            buildString {
                append(last.id)
                append(':')
                append(last.text.length)
                append(':')
                append(last.imageUris.size)
                append(':')
                append(last.attachments.size)
                append(':')
                append(last.showThink)
            }
        }
    }

    LaunchedEffect(currentSession.chatState.messages.size) {
        if (autoScrollEnabled && shouldStickToBottom) {
            scrollToBottom(immediate = true)
        }
    }

    LaunchedEffect(lastMessageRenderKey) {
        if (autoScrollEnabled && shouldStickToBottom) {
            scrollByCurrentOutputLineIfNeeded()
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { Pair(isNearBottom, overlaySignature) }
            .collectLatest { (nearBottom, signature) ->
                if (signature != lastOverlaySignature) {
                    if (autoScrollEnabled && lastNearBottom) {
                        scrollToBottom(immediate = true)
                    }
                    lastOverlaySignature = signature
                }
                lastNearBottom = nearBottom
            }
    }

    // 函数：更新消息的think可见性状态
    fun updateMessageThinkVisibility(messageId: String, showThink: Boolean) {
        val conversationId = appState.currentConversationId ?: return
        vm.updateMessageThinkVisibility(conversationId, messageId, showThink)
    }

    fun updateMessageDebugPromptVisibility(messageId: String, show: Boolean) {
        val updated = currentSession.chatState.messages.map { message ->
            if (message.id == messageId) message.copy(showDebugPrompt = show) else message
        }
        val conversationId = appState.currentConversationId ?: return
        vm.replaceMessages(conversationId, updated)
        scope.launch {
            repository.saveMessages(conversationId, assistantId, updated)
        }
    }

    LaunchedEffect(targetMessageId, currentSession.chatState.messages) {
        val target = targetMessageId ?: return@LaunchedEffect
        val expanded = com.huajuan.aispace.utils.expandMessages(currentSession.chatState.messages)
        val index = expanded.indexOfFirst { it.originalMessageId == target }
        if (index >= 0) {
            try {
                listState.scrollToItem(index)
            } catch (_: Exception) {
            } finally {
                onTargetMessageConsumed()
            }
        } else if (currentSession.chatState.messages.isNotEmpty()) {
            onTargetMessageConsumed()
        }
    }

    LaunchedEffect(searchQuery, appState.currentConversationId, assistantId) {
        val conversationId = appState.currentConversationId ?: return@LaunchedEffect
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        val currentQuery = searchQuery.trim()
        isSearching = true
        kotlinx.coroutines.delay(250)
        if (currentQuery.isNotBlank() && currentQuery == searchQuery.trim()) {
            val tokens = currentQuery
                .replace(Regex("[^\\p{L}\\p{Nd}]+"), " ")
                .trim()
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }
            if (tokens.isEmpty()) {
                searchResults = emptyList()
                isSearching = false
                return@LaunchedEffect
            }
            val ftsQuery = tokens.joinToString(" AND ") { "$it*" }
            val results = repository.searchMessagesAsync(conversationId, assistantId, ftsQuery)
            searchResults = results
            isSearching = false
        }
    }

    LaunchedEffect(imeVisible) {
        if (imeVisible && isExpanded) {
            isExpanded = false
        }
        if (!imeVisible && pendingExpand) {
            isExpanded = true
            pendingExpand = false
        }
    }

    val inputOverlayVisible = isExpanded || pendingExpand || imeVisible
    LaunchedEffect(inputOverlayVisible) {
        onInputOverlayVisibleChange(inputOverlayVisible)
    }

    LaunchedEffect(collapseInputRequest) {
        if (collapseInputRequest > 0) {
            collapseInputArea()
        }
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                conversationTitle = currentConversationTitle,
                onMenuClick = onMenuClick,
                onCreateConversation = {
                    scope.launch {
                        val agent = repository.getAssistantById(assistantId)
                        val newConversation = repository.createNewConversation(
                            assistantId = assistantId,
                            title = context.getString(R.string.conversation_new),
                            roleName = agent?.name ?: context.getString(R.string.default_assistant_name),
                            systemPrompt = agent?.systemPrompt ?: context.getString(R.string.default_assistant_prompt)
                        )

                        repository.saveMessages(newConversation.id, assistantId, emptyList())
                        if (repository.getKnowledgeSettings().inheritAssistantDefaultsToConversation) {
                            val defaults = repository.getAssistantDefaultKbSelection(assistantId)
                            if (defaults.isNotEmpty()) {
                                repository.setConversationKbSelection(newConversation.id, defaults)
                            }
                        }

                        onAppStateChange(
                            appState.copy(
                                currentConversationId = newConversation.id,
                                conversations = repository.getConversationsAsync(assistantId)
                            )
                        )
                        vm.setMessages(emptyList())
                        vm.resetInput()
                    }
                },
                onToggleSearch = {
                    showSearch = !showSearch
                    if (!showSearch) {
                        searchQuery = ""
                        searchResults = emptyList()
                    }
                }
            )
        },
        bottomBar = {
            val providerLabel = com.huajuan.aispace.data.WebSearchProviders
                .findById(selectedWebSearchProvider)
                ?.label(context)
                ?: selectedWebSearchProvider
            val searchProviderLabel = if (webSearchArmed) {
                context.getString(R.string.chat_web_search_with_state, providerLabel)
            } else {
                providerLabel
            }
            val selectedKnowledgeBaseNames = remember(selectedKnowledgeBaseIds, knowledgeBases) {
                selectedKnowledgeBaseIds.mapNotNull { selectedId ->
                    knowledgeBases.firstOrNull { it.id == selectedId }?.name
                }
            }
            val secondaryStatusItems = remember(
                selectedKnowledgeBaseIds,
                selectedKnowledgeBaseNames,
                webSearchArmed,
                selectedWebSearchProvider,
                mcpArmed
            ) {
                buildList {
                    if (selectedKnowledgeBaseIds.isNotEmpty()) {
                        val label = if (selectedKnowledgeBaseNames.size == 1) {
                            context.getString(
                                R.string.chat_status_knowledge_single,
                                selectedKnowledgeBaseNames.first()
                            )
                        } else {
                            context.getString(
                                R.string.chat_status_knowledge_count,
                                selectedKnowledgeBaseIds.size
                            )
                        }
                        add(
                            BottomInputSecondaryStatusItem(
                                key = "knowledge",
                                label = label,
                                icon = Icons.Outlined.Storage,
                                onClick = {
                                    scope.launch { knowledgeBases = repository.listKnowledgeBases() }
                                    showKnowledgePicker = true
                                },
                                onClear = {
                                    selectedKnowledgeBaseIds = emptyList()
                                    val conversationId = appState.currentConversationId
                                    if (conversationId != null) {
                                        scope.launch {
                                            repository.setConversationKbSelection(conversationId, emptyList())
                                        }
                                    }
                                }
                            )
                        )
                    }
                    if (webSearchArmed && canEnableWebSearch()) {
                        add(
                            BottomInputSecondaryStatusItem(
                                key = "web_search",
                                label = context.getString(
                                    R.string.chat_status_web_search,
                                    providerLabel
                                ),
                                icon = Icons.Outlined.Search,
                                onClick = { showWebSearchPicker = true },
                                onClear = {
                                    webSearchArmed = false
                                    repository.setRuntimeWebSearchConfig(null)
                                }
                            )
                        )
                    }
                    if (mcpArmed && canEnableMcp()) {
                        add(
                            BottomInputSecondaryStatusItem(
                                key = "mcp",
                                label = context.getString(
                                    R.string.chat_status_mcp,
                                    repository.getMcpServerUrls().size
                                ),
                                icon = Icons.Outlined.Extension,
                                onClick = { showMcpPicker = true },
                                onClear = {
                                    mcpArmed = false
                                    repository.setRuntimeMcpConfig(null)
                                }
                            )
                        )
                    }
                }
            }
            BottomInputArea(
                isExpanded = isExpanded,
                isPendingExpand = pendingExpand,
                onExpandChange = { expand ->
                    if (expand) {
                        if (imeVisible) {
                            pendingExpand = true
                            focusManager.clearFocus()
                        } else {
                            pendingExpand = false
                            isExpanded = true
                            focusManager.clearFocus()
                        }
                    } else {
                        pendingExpand = false
                        isExpanded = false
                    }
                },
                inputText = currentSession.chatState.inputText,
                onInputTextChanged = { newText ->
                    val conversationId = appState.currentConversationId ?: return@BottomInputArea
                    vm.updateInputText(conversationId, newText)
                },
                onOpenKnowledgeBasePicker = {
                    scope.launch { knowledgeBases = repository.listKnowledgeBases() }
                    showKnowledgePicker = true
                },
                onOpenSearchSettings = {
                    showWebSearchPicker = true
                },
                onOpenMcpSettings = {
                    showMcpPicker = true
                },
                onOpenQuickPhrasePicker = {
                    quickPhrases = repository.getAssistantCommonPhrases(assistantId)
                    showQuickPhrasePicker = true
                },
                currentKnowledgeBaseLabel = if (selectedKnowledgeBaseIds.isNotEmpty()) {
                    context.getString(R.string.chat_status_knowledge_on)
                } else {
                    context.getString(R.string.chat_status_knowledge_off)
                },
                currentSearchProviderLabel = if (webSearchArmed) {
                    context.getString(R.string.chat_status_web_search_simple_on)
                } else {
                    context.getString(R.string.chat_status_web_search_simple_off)
                },
                isStreaming = currentSession.isStreaming,
                onStopMessage = {
                    val conversationId = appState.currentConversationId ?: return@BottomInputArea
                    vm.stopMessage(conversationId)
                },
                onSendMessage = { text ->
                    if (!ensureModelAccessReady()) return@BottomInputArea
                    val conversationId = appState.currentConversationId ?: return@BottomInputArea
                    val pendingImageUris = selectedImageUris
                    val pendingFileAttachments = selectedFileAttachments
                    val isFirstPendingUserMessage = currentSession.chatState.messages.isEmpty()
                    val sent = vm.sendMessage(
                        conversationId = conversationId,
                        assistantId = assistantId,
                        text = text,
                        imageUris = pendingImageUris,
                        attachments = pendingFileAttachments,
                        sentImagesAndFilesLabel = context.getString(R.string.chat_sent_images_and_files),
                        sentImagesLabel = context.getString(R.string.chat_sent_images),
                        sentFilesLabel = context.getString(R.string.chat_sent_files),
                        webSearchConfig = if (webSearchArmed && text.isNotBlank()) {
                            RuntimeWebSearchConfig(enabled = true, providerId = selectedWebSearchProvider)
                        } else {
                            null
                        },
                        mcpConfig = if (mcpArmed) RuntimeMcpConfig(enabled = true) else null,
                        formatStreamError = { message ->
                            val errorPrefix = context.getString(R.string.error_prefix, "").removeSuffix("")
                            if (message.startsWith(errorPrefix)) message else context.getString(R.string.error_prefix, message)
                        },
                        formatReplyFailed = { message ->
                            context.getString(R.string.chat_reply_failed, message)
                        },
                        formatToastFailed = { message ->
                            context.getString(R.string.chat_ai_reply_failed, message)
                        },
                        onConversationListRefresh = refreshConversations
                    )
                    if (!sent) return@BottomInputArea

                    onSelectedImageUrisChange(emptyList())
                    selectedFileAttachments = emptyList()

                    if (isFirstPendingUserMessage && text.isNotBlank()) {
                        scope.launch {
                            try {
                                waitForConversationStreamToFinish(
                                    vm = vm,
                                    conversationId = conversationId
                                )
                                val resolved = ModelRequestResolver.resolve(repository, assistantId)
                                if (!resolved.isImageGenerationService) {
                                    val defaultTitle = repository.getCurrentDefaultConversationTitle()
                                    val existingTitle = repository
                                        .getConversationsAsync(assistantId)
                                        .firstOrNull { it.id == conversationId }
                                        ?.title
                                        .orEmpty()
                                    if (existingTitle.isNotBlank() && existingTitle != defaultTitle) {
                                        return@launch
                                    }
                                    val prompt = """
                                        请根据下面这段用户的第一条消息生成一个简短的对话标题。
                                        要求：不超过12个字，只返回标题文字，不要加引号或其他说明。

                                        用户消息：
                                        $text
                                    """.trimIndent()

                                    val titleResult = repository.getAIResponse(
                                        messages = listOf(
                                            Message(
                                                id = java.util.UUID.randomUUID().toString(),
                                                text = prompt,
                                                isUser = true,
                                                timestamp = java.util.Date()
                                            )
                                        ),
                                        conversationId = conversationId
                                    )

                                    val cleaned = com.huajuan.aispace.utils.ThinkTagProcessor.removeThinkTags(titleResult)
                                        .replace(Regex("<[^>]+>"), " ")
                                        .replace("\n", " ")
                                        .replace(Regex("\\s+"), " ")
                                        .trim()
                                        .trim('"', '“', '”', '\'')
                                        .take(20)

                                    if (cleaned.isNotBlank() && !cleaned.startsWith(context.getString(R.string.error_prefix, "").removeSuffix(""))) {
                                        repository.updateConversationTitle(conversationId, cleaned)
                                        refreshConversations(assistantId)
                                    }
                                }
                            } catch (_: Exception) {
                            }
                        }
                    }
                },
                onOpenImageSelector = {
                    if (supportsImageUpload()) {
                        onOpenImageSelector()
                    } else {
                        if (modelRequirementIssue == null) {
                            Toast.makeText(context, context.getString(R.string.chat_model_not_support_image_upload), Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onOpenCamera = {
                    if (!supportsImageUpload()) {
                        if (modelRequirementIssue == null) {
                            Toast.makeText(context, context.getString(R.string.chat_model_not_support_image_upload), Toast.LENGTH_SHORT).show()
                        }
                        return@BottomInputArea
                    }
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.CAMERA
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        val tempFile = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
                        val uri = FileProvider.getUriForFile(context, authority, tempFile)
                        cameraUriState.value = uri
                        cameraLauncher.launch(uri)
                    } else {
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                },
                onOpenFilePicker = {
                    if (supportsFileUpload()) {
                        filePickerLauncher.launch(arrayOf("*/*"))
                    } else {
                        if (modelRequirementIssue == null) {
                            Toast.makeText(context, context.getString(R.string.chat_model_not_support_file_upload), Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onQuickPickImage = { uri ->
                    if (supportsImageUpload()) {
                        val next = if (selectedImageUris.contains(uri)) {
                            selectedImageUris
                        } else {
                            selectedImageUris + uri
                        }
                        onSelectedImageUrisChange(next)
                    } else {
                        if (modelRequirementIssue == null) {
                            Toast.makeText(context, context.getString(R.string.chat_model_not_support_image_upload), Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                selectedImageUris = selectedImageUris,
                onSelectedImageUrisChange = { uris -> onSelectedImageUrisChange(uris) },
                selectedFileAttachments = selectedFileAttachments,
                onSelectedFileAttachmentsChange = { attachments -> selectedFileAttachments = attachments },
                secondaryStatusItems = secondaryStatusItems
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        // Opt-out of all automatic window insets to avoid double application
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            AnimatedVisibility(
                visible = showSearch,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.chat_search_placeholder)) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_clear))
                                }
                            }
                        },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                        ),
                        singleLine = true
                    )
                    if (searchQuery.isNotBlank()) {
                        Text(
                            text = if (isSearching) {
                                stringResource(R.string.chat_searching)
                            } else {
                                stringResource(R.string.chat_search_result_count, searchResults.size)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                        )
                    }
                }
            }

            val displayedMessages =
                if (searchQuery.isBlank()) currentSession.chatState.messages else searchResults

            if (searchQuery.isNotBlank() && !isSearching && displayedMessages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.chat_search_no_result),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    ChatContentArea(
                        messages = displayedMessages,
                        repository = repository,
                        roleName = uiState.roleName,
                        modelDisplayName = modelDisplayName,
                        systemPrompt = if (searchQuery.isBlank()) uiState.systemPrompt else "",
                        prefetchNextLine = autoScrollEnabled && shouldStickToBottom && currentSession.isStreaming,
                        streamingMessageId = currentSession.streamingMessageId,
                        tokenAnimationUseFixedDuration = tokenAnimationUseFixedDuration,
                        tokenAnimationFixedDurationMs = tokenAnimationFixedDurationMs,
                        onPrefetchLineReadyChange = { ready -> isPrefetchLineReady = ready },
                        listState = listState,
                        conversationId = appState.currentConversationId,
                        modifier = Modifier.fillMaxSize(),
                        onUpdateMessageThinkVisibility = { messageId, showThink ->
                            updateMessageThinkVisibility(messageId, showThink)
                        },
                        onUpdateMessageDebugPromptVisibility = { messageId, show ->
                            updateMessageDebugPromptVisibility(messageId, show)
                        },
                        onImageClick = { uris, index ->
                            viewingImageUris = uris
                            viewingImageIndex = index.coerceIn(0, uris.lastIndex)
                        },
                        onBackgroundTap = { collapseInputArea() },
                        onOpenAssistantSettings = onOpenAssistantSettings,
                        drawerUiResetRequest = drawerUiResetRequest
                    )
                }
            }
        }
    }
}

private suspend fun waitForConversationStreamToFinish(
    vm: ChatViewModel,
    conversationId: String
) {
    var streamStarted = false
    repeat(400) {
        val isStreaming = vm.uiState.value.sessions[conversationId]?.isStreaming == true
        if (isStreaming) {
            streamStarted = true
        } else if (streamStarted) {
            return
        }
        delay(50)
    }
}
