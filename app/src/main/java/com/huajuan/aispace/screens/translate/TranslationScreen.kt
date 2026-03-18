package com.huajuan.aispace.screens.translate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.huajuan.aispace.data.Message
import com.huajuan.aispace.data.ModelApiFactory
import com.huajuan.aispace.data.Repository
import com.huajuan.aispace.data.model.ChatEvent
import com.huajuan.aispace.network.Message as NetworkMessage
import com.huajuan.aispace.components.model.ModelRequirementDialog
import com.huajuan.aispace.components.model.ModelRequirementIssue
import com.huajuan.aispace.components.model.resolveModelRequirementIssue
import com.huajuan.aispace.navigation.DrawerUiResetEffect
import com.huajuan.aispace.utils.ThinkTagProcessor
import com.huajuan.aispace.components.settings.SettingsListItem
import com.huajuan.aispace.ui.theme.AppDimens
import com.huajuan.aispace.R
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationScreen(
    onMenuClick: () -> Unit,
    drawerUiResetRequest: Int = 0,
    repository: Repository,
    assistantId: String,
    currentConversationId: String?,
    onCurrentConversationChange: (String?) -> Unit,
    onOpenTranslationSettings: () -> Unit = {},
    onOpenProviderModelSettings: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val languageOptions = remember { mutableStateListOf(*defaultTranslationLanguages().toTypedArray()) }
    var sourceLang by remember { mutableStateOf<TranslationLanguage>(TranslationLanguage.AutoDetect) }
    var targetLang by remember { mutableStateOf<TranslationLanguage>(TranslationLanguage.AutoTarget) }
    var sourceText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var activeLanguagePicker by remember { mutableStateOf<LanguagePickerTarget?>(null) }
    val defaultLanguage = remember { resolveDefaultLanguage(repository.getLanguage()) }
    val scope = rememberCoroutineScope()
    var isTranslating by remember { mutableStateOf(false) }
    var activePanel by remember { mutableStateOf(TranslationPanel.Source) }
    var modelRequirementIssue by remember { mutableStateOf<ModelRequirementIssue?>(null) }
    val pageScrollState = rememberScrollState()

    DrawerUiResetEffect(drawerUiResetRequest) {
        activeLanguagePicker = null
        modelRequirementIssue = null
    }

    LaunchedEffect(currentConversationId, assistantId) {
        if (currentConversationId.isNullOrBlank()) {
            sourceText = ""
            translatedText = ""
            activePanel = TranslationPanel.Source
            return@LaunchedEffect
        }
        val messages = repository.getMessagesAsync(currentConversationId, assistantId)
        sourceText = messages.lastOrNull { it.isUser }?.text.orEmpty()
        translatedText = messages.lastOrNull { !it.isUser }?.text.orEmpty()
    }

    val resolvedTarget = resolveAutoTarget(sourceLang, targetLang, sourceText, defaultLanguage)

    ModelRequirementDialog(
        issue = modelRequirementIssue,
        onDismiss = { modelRequirementIssue = null },
        onOpenModelPicker = onOpenTranslationSettings,
        onOpenProviderSettings = {
            val provider = repository.getAssistantModelConfig(assistantId).serviceProvider
            onOpenProviderModelSettings(provider)
        }
    )

    if (activeLanguagePicker != null) {
        val pickerTarget = activeLanguagePicker
        val options = when (pickerTarget) {
            LanguagePickerTarget.Source -> listOf(TranslationLanguage.AutoDetect) + languageOptions
            LanguagePickerTarget.Target -> listOf(TranslationLanguage.AutoTarget) + languageOptions
            null -> emptyList()
        }
        LanguagePickerSheet(
            title = if (pickerTarget == LanguagePickerTarget.Source) {
                stringResource(R.string.translation_pick_source)
            } else {
                stringResource(R.string.translation_pick_target)
            },
            options = options,
            current = if (pickerTarget == LanguagePickerTarget.Source) sourceLang else targetLang,
            onSelect = { selected ->
                if (pickerTarget == LanguagePickerTarget.Source) {
                    sourceLang = selected
                } else {
                    targetLang = selected
                }
                activeLanguagePicker = null
            },
            onAddCustom = { custom ->
                if (!languageOptions.contains(custom)) {
                    languageOptions.add(custom)
                }
                if (pickerTarget == LanguagePickerTarget.Source) {
                    sourceLang = custom
                } else {
                    targetLang = custom
                }
                activeLanguagePicker = null
            },
            onDismiss = { activeLanguagePicker = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.translation_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Outlined.Menu, contentDescription = stringResource(R.string.cd_open_drawer))
                    }
                },
                actions = {
                    IconButton(onClick = onOpenTranslationSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.translation_settings_title))
                    }
                    IconButton(
                        onClick = {
                            if (isTranslating) return@IconButton
                            scope.launch {
                                val conversationId = createTranslationConversation(
                                    repository = repository,
                                    assistantId = assistantId,
                                    title = context.getString(R.string.translation_new)
                                )
                                sourceText = ""
                                translatedText = ""
                                activePanel = TranslationPanel.Source
                                onCurrentConversationChange(conversationId)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.translation_new))
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
                .padding(horizontal = AppDimens.screenPadding)
                .padding(bottom = AppDimens.screenPaddingBottom)
                .verticalScroll(pageScrollState),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spacingL)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageChip(
                    label = sourceLang.label(context),
                    modifier = Modifier.weight(1f),
                    onClick = { activeLanguagePicker = LanguagePickerTarget.Source }
                )
                IconButton(
                    onClick = {
                        val temp = sourceLang
                        sourceLang = targetLang
                        targetLang = temp
                    },
                    modifier = Modifier.padding(horizontal = AppDimens.spacingS)
                ) {
                    Icon(Icons.Default.Cached, contentDescription = stringResource(R.string.translation_swap_languages))
                }
                LanguageChip(
                    label = targetLang.label(context),
                    modifier = Modifier.weight(1f),
                    onClick = { activeLanguagePicker = LanguagePickerTarget.Target }
                )
            }

            TranslationActionBar(
                isTranslating = isTranslating,
                onTranslate = {
                    if (sourceText.isBlank() || isTranslating) return@TranslationActionBar
                    val issue = resolveModelRequirementIssue(repository, assistantId)
                    if (issue != null) {
                        modelRequirementIssue = issue
                        return@TranslationActionBar
                    }
                    val reasoningEnabled = repository.getAssistantReasoningEnabled(assistantId)
                    activePanel = TranslationPanel.Result
                    scope.launch {
                        isTranslating = true
                        translatedText = ""
                        val conversationId = ensureTranslationConversation(
                            repository = repository,
                            assistantId = assistantId,
                            currentConversationId = currentConversationId,
                            sourceText = sourceText
                        )
                        onCurrentConversationChange(conversationId)
                        val userMessage = Message(
                            id = UUID.randomUUID().toString(),
                            text = sourceText,
                            isUser = true,
                            timestamp = Date()
                        )
                        val existing = repository.getMessagesAsync(conversationId, assistantId)
                        val updatedMessages = existing + userMessage
                        repository.saveMessages(conversationId, assistantId, updatedMessages)
                        repository.updateLastMessage(conversationId, sourceText)

                        val systemPrompt = buildTranslationPrompt(
                            context = context,
                            sourceLang = sourceLang,
                            targetLang = resolvedTarget,
                            defaultLang = defaultLanguage,
                            reasoningEnabled = reasoningEnabled
                        )
                        val requestMessages = listOf(
                            NetworkMessage(role = "system", content = systemPrompt),
                            NetworkMessage(role = "user", content = sourceText)
                        )
                        val apiService = ModelApiFactory(repository).getModelApiServiceFor(assistantId)
                        val resultBuffer = StringBuilder()
                        apiService.streamAIResponse(
                            messages = requestMessages,
                            modelInfo = resolveModelInfo(repository, assistantId)
                        ).collect { event ->
                            when (event) {
                                is ChatEvent.Chunk -> {
                                    resultBuffer.append(event.text)
                                    translatedText = ThinkTagProcessor.removeThinkTags(resultBuffer.toString()).trim()
                                }
                                is ChatEvent.Error -> {
                                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                                }
                                is ChatEvent.SourcesResolved,
                                is ChatEvent.ToolStart,
                                is ChatEvent.ToolResult,
                                is ChatEvent.ToolError -> {
                                    // 翻译场景忽略工具事件
                                }
                                ChatEvent.Done -> {
                                    // handled after loop
                                }
                            }
                        }
                        val finalText = ThinkTagProcessor
                            .removeThinkTags(translatedText.ifBlank { resultBuffer.toString() })
                            .trim()
                        if (finalText.isNotBlank()) {
                            val aiMessage = Message(
                                id = UUID.randomUUID().toString(),
                                text = finalText,
                                isUser = false,
                                timestamp = Date()
                            )
                            repository.saveMessages(conversationId, assistantId, updatedMessages + aiMessage)
                            repository.updateLastMessage(conversationId, finalText)
                        }
                        isTranslating = false
                    }
                }
            )

            TranslationPanels(
                sourceText = sourceText,
                onSourceTextChange = { sourceText = it },
                translatedText = translatedText,
                activePanel = activePanel,
                onPanelChange = { activePanel = it }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
    ) {
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TranslationActionBar(
    isTranslating: Boolean,
    onTranslate: () -> Unit
) {
    Button(
        onClick = onTranslate,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Translate, contentDescription = null)
        Spacer(modifier = Modifier.width(AppDimens.spacingS))
        Text(if (isTranslating) stringResource(R.string.translation_translating) else stringResource(R.string.translation_start))
    }
}

@Composable
private fun TranslationPanels(
    sourceText: String,
    onSourceTextChange: (String) -> Unit,
    translatedText: String,
    activePanel: TranslationPanel,
    onPanelChange: (TranslationPanel) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isWideLayout = maxWidth >= 720.dp
        val panelModifier = Modifier.fillMaxWidth()

        if (isWideLayout) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spacingL)
            ) {
                SourcePanel(
                    sourceText = sourceText,
                    onSourceTextChange = onSourceTextChange,
                    modifier = panelModifier.weight(1f)
                )
                ResultPanel(
                    translatedText = translatedText,
                    modifier = panelModifier.weight(1f)
                )
            }
        } else {
            MobileTranslationPanel(
                sourceText = sourceText,
                onSourceTextChange = onSourceTextChange,
                translatedText = translatedText,
                activePanel = activePanel,
                onPanelChange = onPanelChange,
                modifier = panelModifier
            )
        }
    }
}

@Composable
private fun MobileTranslationPanel(
    sourceText: String,
    onSourceTextChange: (String) -> Unit,
    translatedText: String,
    activePanel: TranslationPanel,
    onPanelChange: (TranslationPanel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppDimens.cardSpacing)
    ) {
        TranslationPanelTabs(
            activePanel = activePanel,
            onPanelChange = onPanelChange
        )
        when (activePanel) {
            TranslationPanel.Source -> SourcePanelContent(
                sourceText = sourceText,
                onSourceTextChange = onSourceTextChange,
                modifier = Modifier.fillMaxWidth()
            )
            TranslationPanel.Result -> ResultPanelContent(
                translatedText = translatedText,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SourcePanel(
    sourceText: String,
    onSourceTextChange: (String) -> Unit,
    modifier: Modifier
) {
    SourcePanelContent(
        sourceText = sourceText,
        onSourceTextChange = onSourceTextChange,
        modifier = modifier
    )
}

@Composable
private fun SourcePanelContent(
    sourceText: String,
    onSourceTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppDimens.cardSpacing)
    ) {

        OutlinedTextField(
            value = sourceText,
            onValueChange = onSourceTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp),
            placeholder = { Text(stringResource(R.string.translation_source_hint)) },
            minLines = 8
        )
    }
}

@Composable
private fun ResultPanel(
    translatedText: String,
    modifier: Modifier
) {
    ResultPanelContent(
        translatedText = translatedText,
        modifier = modifier
    )
}

@Composable
private fun ResultPanelContent(
    translatedText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppDimens.cardSpacing)
    ) {

        OutlinedTextField(
            value = translatedText,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp),
            placeholder = {
                Text(
                    text = stringResource(R.string.translation_result_hint),
                    textAlign = TextAlign.Start
                )
            },
            minLines = 8
        )
    }
}

private enum class TranslationPanel {
    Source,
    Result
}

@Composable
private fun TranslationPanelTabs(
    activePanel: TranslationPanel,
    onPanelChange: (TranslationPanel) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val options = listOf(
                TranslationPanel.Source to stringResource(R.string.translation_source),
                TranslationPanel.Result to stringResource(R.string.translation_result)
            )
            options.forEach { (panel, label) ->
                SegmentedButton(
                    selected = activePanel == panel,
                    onClick = { onPanelChange(panel) },
                    shape = MaterialTheme.shapes.small,
                    label = { Text(label) }
                )
            }
        }
    }
}

private enum class LanguagePickerTarget {
    Source,
    Target
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePickerSheet(
    title: String,
    options: List<TranslationLanguage>,
    current: TranslationLanguage,
    onSelect: (TranslationLanguage) -> Unit,
    onAddCustom: (TranslationLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    var customLanguage by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spacingS)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            options.forEach { option ->
                SettingsListItem(
                    title = option.label(LocalContext.current),
                    subtitle = if (option == current) stringResource(R.string.translation_current) else null,
                    onClick = { onSelect(option) }
                )
            }
            Text(
                text = stringResource(R.string.translation_custom_language),
                style = MaterialTheme.typography.labelLarge
            )
            OutlinedTextField(
                value = customLanguage,
                onValueChange = { customLanguage = it },
                placeholder = { Text(stringResource(R.string.translation_custom_language_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(
                onClick = {
                    val cleaned = customLanguage.trim()
                    if (cleaned.isNotBlank()) {
                        onAddCustom(TranslationLanguage.custom(cleaned))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.translation_add_and_select))
            }
        }
    }
}

private fun resolveDefaultLanguage(lang: String): TranslationLanguage {
    return when {
        lang.startsWith("ja") -> TranslationLanguage.Japanese
        lang.startsWith("zh") -> TranslationLanguage.Chinese
        else -> TranslationLanguage.English
    }
}

private fun resolveAutoTarget(
    sourceLang: TranslationLanguage,
    targetLang: TranslationLanguage,
    text: String,
    defaultLang: TranslationLanguage
): TranslationLanguage {
    if (sourceLang != TranslationLanguage.AutoDetect && targetLang != TranslationLanguage.AutoTarget) return targetLang
    val isDefault = isLikelyLanguage(text, defaultLang)
    return if (!isDefault) {
        defaultLang
    } else {
        when (defaultLang) {
            TranslationLanguage.Chinese -> TranslationLanguage.English
            TranslationLanguage.English -> TranslationLanguage.Chinese
            TranslationLanguage.Japanese -> TranslationLanguage.Chinese
            else -> TranslationLanguage.English
        }
    }
}

private fun isLikelyLanguage(text: String, target: TranslationLanguage): Boolean {
    if (text.isBlank()) return true
    val hasCjk = text.any { it.code in 0x4E00..0x9FFF }
    return when (target) {
        TranslationLanguage.Chinese,
        TranslationLanguage.Japanese -> hasCjk
        TranslationLanguage.English -> !hasCjk
        else -> true
    }
}

private fun buildTranslationPrompt(
    context: android.content.Context,
    sourceLang: TranslationLanguage,
    targetLang: TranslationLanguage,
    defaultLang: TranslationLanguage,
    reasoningEnabled: Boolean
): String {
    return buildString {
        append(
            context.getString(
                R.string.translation_prompt_template,
                sourceLang.promptLabel(context),
                targetLang.promptLabel(context),
                defaultLang.promptLabel(context)
            )
        )
        if (!reasoningEnabled) {
            append(' ')
            append(context.getString(R.string.translation_prompt_no_reasoning_suffix))
        }
    }
}

private suspend fun ensureTranslationConversation(
    repository: Repository,
    assistantId: String,
    currentConversationId: String?,
    sourceText: String
): String {
    if (!currentConversationId.isNullOrBlank()) return currentConversationId
    return createTranslationConversation(
        repository = repository,
        assistantId = assistantId,
        title = sourceText.take(12).ifBlank { repository.getContext().getString(R.string.translation_new) }
    )
}

private suspend fun createTranslationConversation(
    repository: Repository,
    assistantId: String,
    title: String
): String {
    val agent = repository.getAssistantById(assistantId)
    val conversation = repository.createNewConversation(
        assistantId = assistantId,
        title = title,
        roleName = agent?.name ?: repository.getContext().getString(R.string.translation_title_fallback),
        systemPrompt = agent?.systemPrompt ?: repository.getContext().getString(R.string.translation_system_prompt_fallback)
    )
    repository.saveMessages(conversation.id, assistantId, emptyList())
    return conversation.id
}

private sealed class TranslationLanguage {
    data object AutoDetect : TranslationLanguage()
    data object AutoTarget : TranslationLanguage()
    data object Chinese : TranslationLanguage()
    data object English : TranslationLanguage()
    data object Japanese : TranslationLanguage()
    data object Korean : TranslationLanguage()
    data object French : TranslationLanguage()
    data class Custom(val value: String) : TranslationLanguage()

    fun label(context: android.content.Context): String = when (this) {
        AutoDetect -> context.getString(R.string.translation_language_auto_detect)
        AutoTarget -> context.getString(R.string.translation_language_auto_target)
        Chinese -> context.getString(R.string.translation_language_chinese)
        English -> context.getString(R.string.translation_language_english)
        Japanese -> context.getString(R.string.translation_language_japanese)
        Korean -> context.getString(R.string.translation_language_korean)
        French -> context.getString(R.string.translation_language_french)
        is Custom -> value
    }

    fun promptLabel(context: android.content.Context): String = when (this) {
        AutoDetect -> context.getString(R.string.translation_language_auto_detect)
        AutoTarget -> context.getString(R.string.translation_language_auto_target)
        Chinese -> context.getString(R.string.translation_language_chinese)
        English -> context.getString(R.string.translation_language_english)
        Japanese -> context.getString(R.string.translation_language_japanese)
        Korean -> context.getString(R.string.translation_language_korean)
        French -> context.getString(R.string.translation_language_french)
        is Custom -> value
    }

    companion object {
        fun custom(value: String): TranslationLanguage = Custom(value)
    }
}

private fun defaultTranslationLanguages(): List<TranslationLanguage> = listOf(
    TranslationLanguage.Chinese,
    TranslationLanguage.English,
    TranslationLanguage.Japanese,
    TranslationLanguage.Korean,
    TranslationLanguage.French
)

private fun resolveModelInfo(repository: Repository, assistantId: String): com.huajuan.aispace.data.ModelInfo {
    val config = repository.getAssistantModelConfig(assistantId)
    return if (config.useCloudModel) {
        val provider = config.serviceProvider
        val modelName = config.selectedModelName
        val modelDataProvider = com.huajuan.aispace.data.ModelDataProvider(repository)
        modelDataProvider.getModelListForProvider(provider).firstOrNull { it.displayName == modelName }
            ?: com.huajuan.aispace.data.ModelInfo(modelName, modelName)
    } else {
        repository.getLocalModelList().firstOrNull { it.displayName == config.localSelectedModelName }
            ?: com.huajuan.aispace.data.ModelInfo(config.localSelectedModelName, config.localSelectedModelName)
    }
}
