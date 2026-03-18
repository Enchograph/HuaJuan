package com.huajuan.aispace.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.huajuan.aispace.R
import com.huajuan.aispace.components.SearchField
import com.huajuan.aispace.data.ModelDataProvider
import com.huajuan.aispace.data.KnowledgeIndexJobDto
import com.huajuan.aispace.data.KnowledgeJobStage
import com.huajuan.aispace.data.KnowledgeSettings
import com.huajuan.aispace.data.Repository
import com.huajuan.aispace.data.repository.SettingsRepository
import com.huajuan.aispace.screens.settings.sections.DebugSettingSection
import com.huajuan.aispace.screens.settings.sections.ModelPickerScreen
import com.huajuan.aispace.components.settings.SettingsListItem
import com.huajuan.aispace.components.settings.SettingsOption
import com.huajuan.aispace.components.settings.SettingsOptionDialog
import com.huajuan.aispace.components.settings.SettingsSectionCard
import com.huajuan.aispace.components.settings.SettingsSwitchItem
import com.huajuan.aispace.data.ToolIds
import com.huajuan.aispace.screens.assistant.AssistantSettingsScreen
import com.huajuan.aispace.viewmodel.SettingsUiState
import com.huajuan.aispace.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun DocumentProcessingScreen(
    modifier: Modifier,
    uiState: SettingsUiState,
    onAutoOcrChange: (Boolean) -> Unit,
    onPreferEpubChange: (Boolean) -> Unit,
    onRenderQualityChange: (String) -> Unit,
    onRemoveBackgroundChange: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            SettingsSectionCard(title = stringResource(R.string.settings_document_recognition_and_format)) {
                SettingsSwitchItem(
                    title = stringResource(R.string.settings_document_auto_ocr),
                    subtitle = stringResource(if (uiState.documentAutoOcrEnabled) R.string.state_enabled else R.string.state_disabled),
                    leading = { Icon(Icons.Outlined.DocumentScanner, contentDescription = null) },
                    checked = uiState.documentAutoOcrEnabled,
                    onCheckedChange = onAutoOcrChange
                )
                HorizontalDivider()
                SettingsSwitchItem(
                    title = stringResource(R.string.settings_document_prefer_epub),
                    subtitle = stringResource(if (uiState.documentPreferEpub) R.string.state_enabled else R.string.state_disabled),
                    leading = { Icon(Icons.Outlined.Description, contentDescription = null) },
                    checked = uiState.documentPreferEpub,
                    onCheckedChange = onPreferEpubChange
                )
            }
        }
        item {
            SettingsSectionCard(title = stringResource(R.string.settings_document_render)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf(
                        "low" to stringResource(R.string.level_low),
                        "medium" to stringResource(R.string.level_medium),
                        "high" to stringResource(R.string.level_high)
                    )
                    options.forEach { (value, label) ->
                        SegmentedButton(
                            selected = uiState.documentRenderQuality == value,
                            onClick = { onRenderQualityChange(value) },
                            shape = MaterialTheme.shapes.small,
                            label = { Text(label) }
                        )
                    }
                }
                HorizontalDivider()
                SettingsSwitchItem(
                    title = stringResource(R.string.settings_document_remove_background),
                    subtitle = stringResource(if (uiState.documentRemoveBackground) R.string.state_enabled else R.string.state_disabled),
                    leading = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) },
                    checked = uiState.documentRemoveBackground,
                    onCheckedChange = onRemoveBackgroundChange
                )
            }
        }
    }
}

@Composable
internal fun WebSearchScreen(
    modifier: Modifier,
    uiState: SettingsUiState,
    onSearchEngineChange: (String) -> Unit,
    onSearchResultCountChange: (Int) -> Unit,
    onSearchIncludeDateChange: (Boolean) -> Unit,
    onSearchCompressionChange: (String) -> Unit,
    onSearchBlacklistChange: (List<String>) -> Unit,
    onSearchProviderApiKeyChange: (String, String) -> Unit
) {
    val context = LocalContext.current
    var showEngineDialog by remember { mutableStateOf(false) }
    var blacklistInput by remember { mutableStateOf("") }
    val engineOptions = remember(context) {
        com.huajuan.aispace.data.WebSearchProviders.all.map { provider ->
            SettingsOption(provider.id, provider.label(context))
        }
    }
    val apiProviders = remember { com.huajuan.aispace.data.WebSearchProviders.apiProviders }
    val localProviders = remember {
        com.huajuan.aispace.data.WebSearchProviders.localProviders
    }
    val defaultEngineLabel = com.huajuan.aispace.data.WebSearchProviders
        .findById(uiState.searchEngine)
        ?.label(context)
        ?: uiState.searchEngine

    if (showEngineDialog) {
        SettingsOptionDialog(
            title = stringResource(R.string.settings_search_default_engine),
            options = engineOptions,
            selectedValue = uiState.searchEngine,
            onSelect = { onSearchEngineChange(it) },
            onDismiss = { showEngineDialog = false }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            SettingsSectionCard(title = stringResource(R.string.settings_search_provider_config)) {
                SettingsListItem(
                    title = stringResource(R.string.settings_search_default_engine),
                    subtitle = defaultEngineLabel,
                    leading = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    onClick = { showEngineDialog = true }
                )
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Text(stringResource(R.string.settings_search_api_providers), fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    apiProviders.forEachIndexed { index, provider ->
                        OutlinedTextField(
                            value = uiState.searchProviderApiKeys[provider.id].orEmpty(),
                            onValueChange = { onSearchProviderApiKeyChange(provider.id, it) },
                            label = {
                                val inputLabel = when (provider.id) {
                                    "Searxng" -> stringResource(R.string.settings_search_searxng_hint)
                                    "ExaMCP" -> stringResource(R.string.settings_search_examcp_hint)
                                    else -> stringResource(R.string.settings_search_api_key_label, provider.label(context))
                                }
                                Text(inputLabel)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (index != apiProviders.lastIndex) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Text(stringResource(R.string.settings_search_local_providers), fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    localProviders.forEachIndexed { index, provider ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(provider.label(context), modifier = Modifier.weight(1f))
                            Text(stringResource(R.string.settings_search_builtin_available), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (index != localProviders.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
        item {
            SettingsSectionCard(title = stringResource(R.string.settings_basic_title)) {
                SettingsSwitchItem(
                    title = stringResource(R.string.settings_search_include_date),
                    subtitle = stringResource(if (uiState.searchIncludeDate) R.string.state_enabled else R.string.state_disabled),
                    leading = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    checked = uiState.searchIncludeDate,
                    onCheckedChange = onSearchIncludeDateChange
                )
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Text(stringResource(R.string.settings_search_result_count), fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val options = listOf(1, 5, 20)
                        options.forEach { value ->
                            SegmentedButton(
                                selected = uiState.searchResultCount == value,
                                onClick = { onSearchResultCountChange(value) },
                                shape = MaterialTheme.shapes.small,
                                label = { Text(value.toString()) }
                            )
                        }
                    }
                }
            }
        }
        item {
            SettingsSectionCard(title = stringResource(R.string.settings_search_result_compression)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf(
                        "none" to stringResource(R.string.settings_search_no_compression),
                        "truncate" to stringResource(R.string.settings_search_truncate),
                        "rag" to "RAG"
                    )
                    options.forEach { (value, label) ->
                        SegmentedButton(
                            selected = uiState.searchCompression == value,
                            onClick = { onSearchCompressionChange(value) },
                            shape = MaterialTheme.shapes.small,
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
        item {
            SettingsSectionCard(title = stringResource(R.string.settings_search_blacklist)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    OutlinedTextField(
                        value = blacklistInput,
                        onValueChange = { blacklistInput = it },
                        label = { Text(stringResource(R.string.settings_search_add_rule)) },
                        placeholder = { Text(stringResource(R.string.settings_search_rule_placeholder)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val trimmed = blacklistInput.trim()
                            if (trimmed.isNotBlank()) {
                                val next = (uiState.searchBlacklist + trimmed).distinct()
                                onSearchBlacklistChange(next)
                                blacklistInput = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_search_add_rule))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (uiState.searchBlacklist.isEmpty()) {
                        Text(stringResource(R.string.settings_search_no_rules), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        uiState.searchBlacklist.forEachIndexed { index, pattern ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(pattern, modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    val next = uiState.searchBlacklist.filterNot { it == pattern }
                                    onSearchBlacklistChange(next)
                                }) { Text(stringResource(R.string.settings_search_remove_rule)) }
                            }
                            if (index != uiState.searchBlacklist.lastIndex) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun KnowledgeBaseSettingsScreen(
    modifier: Modifier,
    onNavigate: (SettingsSubRoute) -> Unit
) {
    val entries = listOf(
        HomeEntry(
            title = stringResource(R.string.settings_basic_title),
            subtitle = stringResource(R.string.settings_knowledge_basic_subtitle),
            route = SettingsSubRoute.KnowledgeBaseBasic
        ),
        HomeEntry(
            title = stringResource(R.string.settings_knowledge_indexing_title),
            subtitle = stringResource(R.string.settings_knowledge_indexing_subtitle),
            route = SettingsSubRoute.KnowledgeBaseIndexing
        ),
        HomeEntry(
            title = stringResource(R.string.settings_knowledge_retrieval_title),
            subtitle = stringResource(R.string.settings_knowledge_retrieval_subtitle),
            route = SettingsSubRoute.KnowledgeBaseRetrieval
        ),
        HomeEntry(
            title = stringResource(R.string.settings_knowledge_advanced_title),
            subtitle = stringResource(R.string.settings_knowledge_advanced_subtitle),
            route = SettingsSubRoute.KnowledgeBaseAdvanced
        ),
        HomeEntry(
            title = stringResource(R.string.settings_knowledge_task_center),
            subtitle = stringResource(R.string.settings_knowledge_task_center_subtitle),
            route = SettingsSubRoute.KnowledgeBaseTaskCenter
        )
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            SettingsSectionCard {
                entries.forEachIndexed { index, entry ->
                    val icon = when (entry.route) {
                        SettingsSubRoute.KnowledgeBaseBasic -> Icons.Outlined.Storage
                        SettingsSubRoute.KnowledgeBaseIndexing -> Icons.Outlined.Tune
                        SettingsSubRoute.KnowledgeBaseRetrieval -> Icons.Outlined.Search
                        SettingsSubRoute.KnowledgeBaseAdvanced -> Icons.Outlined.Bolt
                        SettingsSubRoute.KnowledgeBaseTaskCenter -> Icons.Outlined.Info
                        else -> Icons.Outlined.Storage
                    }
                    SettingsListItem(
                        title = entry.title,
                        subtitle = entry.subtitle,
                        leading = { Icon(icon, contentDescription = null) },
                        onClick = { onNavigate(entry.route) }
                    )
                    if (index != entries.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
internal fun KnowledgeBaseBasicSettingsScreen(
    modifier: Modifier,
    uiState: SettingsUiState,
    onUpdateSettings: (transform: (KnowledgeSettings) -> KnowledgeSettings) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            SettingsSectionCard {
                SettingsSwitchItem(
                    title = stringResource(R.string.settings_knowledge_inherit_defaults),
                    subtitle = stringResource(if (uiState.knowledgeSettings.inheritAssistantDefaultsToConversation) R.string.state_enabled else R.string.state_disabled),
                    leading = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    checked = uiState.knowledgeSettings.inheritAssistantDefaultsToConversation,
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(inheritAssistantDefaultsToConversation = checked) }
                    }
                )
                HorizontalDivider()
                SettingsSwitchItem(
                    title = stringResource(R.string.settings_knowledge_skip_unchanged),
                    subtitle = stringResource(if (uiState.knowledgeSettings.skipUnchangedContent) R.string.state_enabled else R.string.state_disabled),
                    leading = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) },
                    checked = uiState.knowledgeSettings.skipUnchangedContent,
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(skipUnchangedContent = checked) }
                    }
                )
                HorizontalDivider()
                SettingsSwitchItem(
                    title = stringResource(R.string.settings_knowledge_delete_missing),
                    subtitle = stringResource(if (uiState.knowledgeSettings.deleteMissingFilesOnRefresh) R.string.state_enabled else R.string.state_disabled),
                    leading = { Icon(Icons.Outlined.Backup, contentDescription = null) },
                    checked = uiState.knowledgeSettings.deleteMissingFilesOnRefresh,
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(deleteMissingFilesOnRefresh = checked) }
                    }
                )
            }
        }
    }
}

@Composable
internal fun KnowledgeBaseIndexingSettingsScreen(
    modifier: Modifier,
    uiState: SettingsUiState,
    onUpdateSettings: (transform: (KnowledgeSettings) -> KnowledgeSettings) -> Unit
) {
    fun updateInt(current: String, apply: (Int) -> Unit) {
        current.toIntOrNull()?.let(apply)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            SettingsSectionCard {
                NumericSettingField(
                    title = stringResource(R.string.settings_knowledge_embedding_max_chars),
                    value = uiState.knowledgeSettings.embeddingInputMaxChars.toString(),
                    onValueCommitted = { updateInt(it) { value -> onUpdateSettings { s -> s.copy(embeddingInputMaxChars = value.coerceAtLeast(512)) } } }
                )
                HorizontalDivider()
                NumericSettingField(
                    title = stringResource(R.string.settings_knowledge_embedding_max_tokens),
                    value = uiState.knowledgeSettings.embeddingInputMaxTokens.toString(),
                    onValueCommitted = { updateInt(it) { value -> onUpdateSettings { s -> s.copy(embeddingInputMaxTokens = value.coerceAtLeast(256)) } } }
                )
                HorizontalDivider()
                NumericSettingField(
                    title = stringResource(R.string.settings_knowledge_chunk_target_chars),
                    value = uiState.knowledgeSettings.chunkTargetChars.toString(),
                    onValueCommitted = { updateInt(it) { value -> onUpdateSettings { s -> s.copy(chunkTargetChars = value.coerceAtLeast(256)) } } }
                )
                HorizontalDivider()
                NumericSettingField(
                    title = stringResource(R.string.settings_knowledge_chunk_overlap_chars),
                    value = uiState.knowledgeSettings.chunkOverlapChars.toString(),
                    onValueCommitted = { updateInt(it) { value -> onUpdateSettings { s -> s.copy(chunkOverlapChars = value.coerceAtLeast(0)) } } }
                )
                HorizontalDivider()
                NumericSettingField(
                    title = stringResource(R.string.settings_knowledge_min_chunk_chars),
                    value = uiState.knowledgeSettings.minChunkChars.toString(),
                    onValueCommitted = { updateInt(it) { value -> onUpdateSettings { s -> s.copy(minChunkChars = value.coerceAtLeast(32)) } } }
                )
                HorizontalDivider()
                NumericSettingField(
                    title = stringResource(R.string.settings_knowledge_max_batch_chunks),
                    value = uiState.knowledgeSettings.maxBatchChunks.toString(),
                    onValueCommitted = { updateInt(it) { value -> onUpdateSettings { s -> s.copy(maxBatchChunks = value.coerceAtLeast(1)) } } }
                )
                HorizontalDivider()
                NumericSettingField(
                    title = stringResource(R.string.settings_knowledge_max_batch_chars),
                    value = uiState.knowledgeSettings.maxBatchChars.toString(),
                    onValueCommitted = { updateInt(it) { value -> onUpdateSettings { s -> s.copy(maxBatchChars = value.coerceAtLeast(1024)) } } }
                )
                HorizontalDivider()
                NumericSettingField(
                    title = stringResource(R.string.settings_knowledge_embedding_retries),
                    value = uiState.knowledgeSettings.embeddingRetryCount.toString(),
                    onValueCommitted = { updateInt(it) { value -> onUpdateSettings { s -> s.copy(embeddingRetryCount = value.coerceAtLeast(0)) } } }
                )
            }
        }
    }
}

@Composable
internal fun KnowledgeBaseRetrievalSettingsScreen(
    modifier: Modifier,
    uiState: SettingsUiState,
    onUpdateSettings: (transform: (KnowledgeSettings) -> KnowledgeSettings) -> Unit
) {
    fun updateInt(current: String, apply: (Int) -> Unit) {
        current.toIntOrNull()?.let(apply)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            SettingsSectionCard {
                NumericSettingField(
                    title = stringResource(R.string.settings_knowledge_search_top_k),
                    value = uiState.knowledgeSettings.searchTopK.toString(),
                    onValueCommitted = { updateInt(it) { value -> onUpdateSettings { s -> s.copy(searchTopK = value.coerceAtLeast(1)) } } }
                )
                HorizontalDivider()
                NumericSettingField(
                    title = stringResource(R.string.settings_knowledge_context_limit),
                    value = uiState.knowledgeSettings.searchContextLimit.toString(),
                    onValueCommitted = { updateInt(it) { value -> onUpdateSettings { s -> s.copy(searchContextLimit = value.coerceAtLeast(1)) } } }
                )
                HorizontalDivider()
                NumericSettingField(
                    title = stringResource(R.string.settings_knowledge_context_max_chars),
                    value = uiState.knowledgeSettings.searchContextMaxChars.toString(),
                    onValueCommitted = { updateInt(it) { value -> onUpdateSettings { s -> s.copy(searchContextMaxChars = value.coerceAtLeast(512)) } } }
                )
                HorizontalDivider()
                SliderSettingRow(
                    title = stringResource(R.string.settings_knowledge_min_score),
                    value = uiState.knowledgeSettings.searchMinScore,
                    valueRange = 0f..0.9f,
                    onValueChange = { value ->
                        onUpdateSettings { it.copy(searchMinScore = String.format(Locale.US, "%.2f", value).toFloat()) }
                    }
                )
                HorizontalDivider()
                SettingsSwitchItem(
                    title = stringResource(R.string.settings_knowledge_merge_adjacent),
                    subtitle = stringResource(if (uiState.knowledgeSettings.mergeAdjacentChunks) R.string.state_enabled else R.string.state_disabled),
                    leading = { Icon(Icons.Outlined.Bolt, contentDescription = null) },
                    checked = uiState.knowledgeSettings.mergeAdjacentChunks,
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(mergeAdjacentChunks = checked) }
                    }
                )
            }
        }
    }
}

@Composable
internal fun KnowledgeBaseAdvancedSettingsScreen(
    modifier: Modifier,
    uiState: SettingsUiState,
    onUpdateSettings: (transform: (KnowledgeSettings) -> KnowledgeSettings) -> Unit
) {
    var extensionInput by remember { mutableStateOf("") }

    fun updateInt(current: String, apply: (Int) -> Unit) {
        current.toIntOrNull()?.let(apply)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            SettingsSectionCard {
                SettingsSwitchItem(
                    title = stringResource(R.string.settings_knowledge_include_hidden),
                    subtitle = stringResource(if (uiState.knowledgeSettings.includeHiddenFiles) R.string.state_enabled else R.string.state_disabled),
                    leading = { Icon(Icons.Outlined.Info, contentDescription = null) },
                    checked = uiState.knowledgeSettings.includeHiddenFiles,
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(includeHiddenFiles = checked) }
                    }
                )
                HorizontalDivider()
                NumericSettingField(
                    title = stringResource(R.string.settings_knowledge_max_folder_depth),
                    value = uiState.knowledgeSettings.maxFolderDepth.toString(),
                    onValueCommitted = { updateInt(it) { value -> onUpdateSettings { s -> s.copy(maxFolderDepth = value.coerceAtLeast(1)) } } }
                )
                HorizontalDivider()
                NumericSettingField(
                    title = stringResource(R.string.settings_knowledge_web_max_chars),
                    value = uiState.knowledgeSettings.webMaxChars.toString(),
                    onValueCommitted = { updateInt(it) { value -> onUpdateSettings { s -> s.copy(webMaxChars = value.coerceAtLeast(1024)) } } }
                )
                HorizontalDivider()
                NumericSettingField(
                    title = stringResource(R.string.settings_knowledge_task_history_limit),
                    value = uiState.knowledgeSettings.taskHistoryLimit.toString(),
                    onValueCommitted = { updateInt(it) { value -> onUpdateSettings { s -> s.copy(taskHistoryLimit = value.coerceAtLeast(1)) } } }
                )
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    OutlinedTextField(
                        value = extensionInput,
                        onValueChange = { extensionInput = it },
                        label = { Text(stringResource(R.string.settings_knowledge_allowed_extensions)) },
                        placeholder = { Text(uiState.knowledgeSettings.allowedFileExtensions.joinToString(", ")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val updated = extensionInput.split(",")
                                .map { it.trim().removePrefix(".").lowercase(Locale.US) }
                                .filter { it.isNotBlank() }
                                .distinct()
                            if (updated.isNotEmpty()) {
                                onUpdateSettings { it.copy(allowedFileExtensions = updated) }
                                extensionInput = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                }
            }
        }
    }
}

@Composable
internal fun KnowledgeBaseTaskCenterScreen(
    modifier: Modifier,
    repository: Repository
) {
    val jobs by repository.getKnowledgeIndexJobsFlow().collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            SettingsSectionCard {
                if (jobs.isEmpty()) {
                    Text(stringResource(R.string.settings_knowledge_no_tasks), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    jobs.forEachIndexed { index, job ->
                        KnowledgeJobRow(job = job)
                        if (index != jobs.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumericSettingField(
    title: String,
    value: String,
    onValueCommitted: (String) -> Unit
) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onValueCommitted(it)
        },
        label = { Text(title) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    )
}

@Composable
private fun SliderSettingRow(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text("$title (${String.format(Locale.US, "%.2f", value)})", fontWeight = FontWeight.SemiBold)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

@Composable
private fun KnowledgeJobRow(job: KnowledgeIndexJobDto) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(job.title, fontWeight = FontWeight.SemiBold)
        Text(job.message.ifBlank { knowledgeStageLabel(job.stage) }, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (job.stage == KnowledgeJobStage.Embedding || job.totalUnits > 0 || job.totalBatches > 0) {
            LinearProgressIndicator(
                progress = { if (job.success == true) 1f else job.progress },
                modifier = Modifier.fillMaxWidth()
            )
        }
        val detail = buildString {
            if (job.totalUnits > 0) {
                append(job.processedUnits)
                append("/")
                append(job.totalUnits)
            }
            if (job.totalBatches > 0) {
                if (isNotEmpty()) append(" · ")
                append(job.processedBatches)
                append("/")
                append(job.totalBatches)
                append(" batches")
            }
        }
        if (detail.isNotBlank()) {
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun knowledgeStageLabel(stage: KnowledgeJobStage): String = when (stage) {
    KnowledgeJobStage.Queued -> "Queued"
    KnowledgeJobStage.Reading -> "Reading"
    KnowledgeJobStage.Chunking -> "Chunking"
    KnowledgeJobStage.Embedding -> "Embedding"
    KnowledgeJobStage.Writing -> "Writing"
    KnowledgeJobStage.Completed -> "Completed"
    KnowledgeJobStage.Failed -> "Failed"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TranslationSettingsScreen(
    modifier: Modifier,
    repository: Repository
) {
    var showModelPicker by remember { mutableStateOf(false) }
    var modelLabel by remember { mutableStateOf(resolveTranslationModelLabel(repository)) }
    var reasoningEnabled by remember {
        mutableStateOf(repository.getAssistantReasoningEnabled(ToolIds.Translation))
    }

    if (showModelPicker) {
        ModalBottomSheet(onDismissRequest = { showModelPicker = false }) {
            ModelPickerScreen(
                repository = repository,
                onSelectModel = { ref ->
                    applyTranslationModelSelection(repository, ref)
                    modelLabel = resolveTranslationModelLabel(repository)
                    showModelPicker = false
                }
            )
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            SettingsSectionCard(title = stringResource(R.string.settings_translation_behavior)) {
                SettingsListItem(
                    title = stringResource(R.string.settings_translation_model),
                    subtitle = modelLabel,
                    leading = { Icon(Icons.Outlined.Tune, contentDescription = null) },
                    onClick = { showModelPicker = true }
                )
                HorizontalDivider()
                SettingsSwitchItem(
                    title = stringResource(R.string.settings_translation_reasoning),
                    subtitle = if (reasoningEnabled) {
                        stringResource(R.string.settings_translation_reasoning_enabled_hint)
                    } else {
                        stringResource(R.string.settings_translation_reasoning_disabled_hint)
                    },
                    leading = { Icon(Icons.Outlined.Translate, contentDescription = null) },
                    checked = reasoningEnabled,
                    onCheckedChange = {
                        reasoningEnabled = it
                        repository.setAssistantReasoningEnabled(ToolIds.Translation, it)
                    }
                )
            }
        }
    }
}

private fun resolveTranslationModelLabel(repository: Repository): String {
    val assistantId = ToolIds.Translation
    val config = repository.getAssistantModelConfig(assistantId)
    return if (config.useCloudModel) {
        "${config.serviceProvider} · ${config.selectedModelName}"
    } else {
        repository.getContext().getString(R.string.settings_local_model_format, config.localSelectedModelName)
    }.ifBlank { repository.getContext().getString(R.string.chat_model_unselected) }
}

private fun applyTranslationModelSelection(repository: Repository, ref: String) {
    val parts = ref.split("|")
    if (parts.size != 2) return
    val provider = parts[0]
    val model = parts[1]
    if (provider == "local") {
        repository.setAssistantUseCloudModel(ToolIds.Translation, false)
        repository.setAssistantLocalSelectedModel(ToolIds.Translation, model)
    } else {
        repository.setAssistantUseCloudModel(ToolIds.Translation, true)
        repository.setAssistantServiceProvider(ToolIds.Translation, provider)
        repository.setAssistantSelectedModel(ToolIds.Translation, model)
    }
}

@Composable
internal fun McpToolsScreen(
    modifier: Modifier,
    uiState: SettingsUiState,
    onToolsEnabledChange: (Boolean) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            SettingsSectionCard(title = stringResource(R.string.settings_basic_title)) {
                SettingsSwitchItem(
                    title = stringResource(R.string.settings_mcp_enable_tools),
                    subtitle = stringResource(R.string.settings_feature_in_development),
                    leading = { Icon(Icons.Outlined.Extension, contentDescription = null) },
                    checked = uiState.mcpToolsEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.settings_feature_in_development),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        onToolsEnabledChange(false)
                    }
                )
            }
        }
        item {
            SettingsSectionCard {
                Text(
                    text = stringResource(R.string.settings_feature_in_development),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun GlobalMemoryScreen(
    modifier: Modifier,
    uiState: SettingsUiState,
    onEnabledChange: (Boolean) -> Unit,
    onRetentionDaysChange: (Int) -> Unit,
    onModeChange: (String) -> Unit,
    onClearMemory: () -> Unit
) {
    var retentionText by remember { mutableStateOf(uiState.globalMemoryRetentionDays.toString()) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            SettingsSectionCard(title = stringResource(R.string.settings_global_memory_settings)) {
                SettingsSwitchItem(
                    title = stringResource(R.string.settings_global_memory_enable),
                    subtitle = stringResource(if (uiState.globalMemoryEnabled) R.string.state_enabled else R.string.state_disabled),
                    leading = { Icon(Icons.Outlined.Memory, contentDescription = null) },
                    checked = uiState.globalMemoryEnabled,
                    onCheckedChange = onEnabledChange
                )
                HorizontalDivider()
                OutlinedTextField(
                    value = retentionText,
                    onValueChange = {
                        retentionText = it
                        val days = it.toIntOrNull()
                        if (days != null) {
                            onRetentionDaysChange(days)
                        }
                    },
                    label = { Text(stringResource(R.string.settings_global_memory_retention_days)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            SettingsSectionCard(title = stringResource(R.string.settings_global_memory_mode)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val options: List<Pair<String, String>> = listOf(
                        "context" to stringResource(R.string.settings_global_memory_mode_context),
                        "summary" to stringResource(R.string.settings_global_memory_mode_summary)
                    )
                    options.forEach { (value, label) ->
                        SegmentedButton(
                            selected = uiState.globalMemoryMode == value,
                            onClick = { onModeChange(value) },
                            shape = MaterialTheme.shapes.small,
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
        item {
            SettingsSectionCard(title = stringResource(R.string.settings_global_memory_clear)) {
                Button(
                    onClick = onClearMemory,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.settings_global_memory_clear_all))
                }
            }
        }
    }
}
