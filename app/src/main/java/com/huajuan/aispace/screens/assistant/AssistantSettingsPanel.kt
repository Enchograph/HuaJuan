package com.huajuan.aispace.screens.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huajuan.aispace.R
import com.huajuan.aispace.screens.assistant.components.EmojiPickerBottomSheet
import com.huajuan.aispace.data.AssistantRequestSettings
import com.huajuan.aispace.data.ModelSelectionValidator
import com.huajuan.aispace.data.Repository
import com.huajuan.aispace.screens.settings.sections.ModelPickerScreen
import com.huajuan.aispace.components.settings.SettingsListItem
import com.huajuan.aispace.components.settings.SettingsSectionCard
import com.huajuan.aispace.components.settings.SettingsSwitchItem
import com.huajuan.aispace.ui.theme.AppDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantSettingsPanel(
    repository: Repository,
    assistantId: String,
    modifier: Modifier = Modifier,
    onOpenKnowledgeBase: () -> Unit,
    onOpenMcpSettings: () -> Unit,
    onOpenGlobalMemory: () -> Unit,
    headerContent: (@Composable () -> Unit)? = null,
    footerContent: (@Composable () -> Unit)? = null,
    promptSettingsFirst: Boolean = false,
    showModelSettings: Boolean = true,
    showModelAdvancedSettings: Boolean = true,
    showPromptSettings: Boolean = true,
    showKnowledgeBase: Boolean = true,
    showMcpSettings: Boolean = true,
    showCommonPhrases: Boolean = true,
    showGlobalMemory: Boolean = true
) {
    var showModelPicker by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var requestSettings by remember { mutableStateOf(repository.getAssistantRequestSettings(assistantId)) }
    var assistantName by remember { mutableStateOf(repository.getAssistantName(assistantId)) }
    var assistantEmoji by remember { mutableStateOf(repository.getAssistantEmoji(assistantId)) }
    var assistantPrompt by remember { mutableStateOf(repository.getAssistantSystemPrompt(assistantId)) }
    var commonPhrases by remember { mutableStateOf(repository.getAssistantCommonPhrases(assistantId)) }
    var newPhrase by remember { mutableStateOf("") }
    var modelLabel by remember { mutableStateOf(resolveModelLabel(repository, assistantId)) }

    LaunchedEffect(assistantId) {
        requestSettings = repository.getAssistantRequestSettings(assistantId)
        assistantName = repository.getAssistantName(assistantId)
        assistantEmoji = repository.getAssistantEmoji(assistantId)
        assistantPrompt = repository.getAssistantSystemPrompt(assistantId)
        commonPhrases = repository.getAssistantCommonPhrases(assistantId)
        modelLabel = resolveModelLabel(repository, assistantId)
    }

    fun updateRequestSettings(next: AssistantRequestSettings) {
        requestSettings = next
        repository.setAssistantRequestSettings(assistantId, next)
    }

    if (showModelPicker) {
        ModalBottomSheet(onDismissRequest = { showModelPicker = false }) {
            ModelPickerScreen(
                repository = repository,
                onSelectModel = { ref ->
                    applyModelSelection(repository, assistantId, ref)
                    modelLabel = resolveModelLabel(repository, assistantId)
                    showModelPicker = false
                }
            )
        }
    }

    if (showEmojiPicker) {
        EmojiPickerBottomSheet(
            currentEmoji = assistantEmoji,
            onSelect = { emoji ->
                assistantEmoji = emoji
                repository.updateAssistantPromptSettings(assistantId, assistantName, assistantEmoji, assistantPrompt)
                showEmojiPicker = false
            },
            onDismiss = { showEmojiPicker = false }
        )
    }

    fun LazyListScope.modelSettingsItems() {
        if (!showModelSettings) return
        item {
            SettingsSectionCard(title = stringResource(R.string.assistant_model_settings)) {
                SettingsListItem(
                    title = stringResource(R.string.assistant_default_model),
                    subtitle = modelLabel,
                    leading = { Icon(Icons.Outlined.Tune, contentDescription = null) },
                    onClick = { showModelPicker = true }
                )
            }
        }

        if (showModelAdvancedSettings) {
            item {
                SettingsSectionCard(title = stringResource(R.string.assistant_sampling_settings)) {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.cardSpacing)) {
                        Text(stringResource(R.string.assistant_model_temperature, "%.2f".format(requestSettings.temperature)))
                        Slider(
                            value = requestSettings.temperature,
                            onValueChange = {
                                updateRequestSettings(requestSettings.copy(temperature = it))
                            },
                            valueRange = 0f..2f
                        )
                        Text(stringResource(R.string.assistant_model_top_p, "%.2f".format(requestSettings.topP)))
                        Slider(
                            value = requestSettings.topP,
                            onValueChange = {
                                updateRequestSettings(requestSettings.copy(topP = it))
                            },
                            valueRange = 0f..1f
                        )
                    }
                }
            }

            item {
                SettingsSectionCard(title = stringResource(R.string.assistant_context_and_length)) {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.cardSpacing)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.assistant_context_message_count))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = {
                                    val next = (requestSettings.contextMessageCount - 2).coerceAtLeast(2)
                                    updateRequestSettings(requestSettings.copy(contextMessageCount = next))
                                }) {
                                    Text("-")
                                }
                                Text(requestSettings.contextMessageCount.toString(), modifier = Modifier.width(36.dp))
                                TextButton(onClick = {
                                    val next = (requestSettings.contextMessageCount + 2).coerceAtMost(50)
                                    updateRequestSettings(requestSettings.copy(contextMessageCount = next))
                                }) {
                                    Text("+")
                                }
                            }
                        }
                        SettingsSwitchItem(
                            title = stringResource(R.string.assistant_limit_max_tokens),
                            subtitle = stringResource(if (requestSettings.maxTokensEnabled) R.string.state_enabled else R.string.assistant_not_enabled),
                            checked = requestSettings.maxTokensEnabled,
                            onCheckedChange = { enabled ->
                                updateRequestSettings(requestSettings.copy(maxTokensEnabled = enabled))
                            }
                        )
                        if (requestSettings.maxTokensEnabled) {
                            OutlinedTextField(
                                value = requestSettings.maxTokens.toString(),
                                onValueChange = {
                                    val next = it.toIntOrNull() ?: requestSettings.maxTokens
                                    updateRequestSettings(requestSettings.copy(maxTokens = next.coerceIn(64, 8192)))
                                },
                                label = { Text(stringResource(R.string.assistant_max_tokens)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            item {
                SettingsSectionCard(title = stringResource(R.string.assistant_output_and_tools)) {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.cardSpacing)) {
                        SettingsSwitchItem(
                            title = stringResource(R.string.assistant_stream_output),
                            subtitle = stringResource(if (requestSettings.streamEnabled) R.string.state_enabled else R.string.state_disabled),
                            checked = requestSettings.streamEnabled,
                            onCheckedChange = { enabled ->
                                updateRequestSettings(requestSettings.copy(streamEnabled = enabled))
                            }
                        )
                        Text(stringResource(R.string.assistant_tool_call_mode))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            listOf(
                                "function" to stringResource(R.string.assistant_tool_call_function),
                                "prompt" to stringResource(R.string.assistant_tool_call_prompt)
                            ).forEach { (value, label) ->
                                SegmentedButton(
                                    selected = requestSettings.toolCallMode == value,
                                    onClick = {
                                        updateRequestSettings(requestSettings.copy(toolCallMode = value))
                                    },
                                    shape = MaterialTheme.shapes.small,
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                SettingsSectionCard(
                    title = stringResource(R.string.assistant_custom_params),
                    description = stringResource(R.string.assistant_custom_params_description)
                ) {
                    OutlinedTextField(
                        value = requestSettings.customParams,
                        onValueChange = { updateRequestSettings(requestSettings.copy(customParams = it)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        placeholder = { Text("{\"frequency_penalty\": 0.2}") }
                    )
                    Spacer(modifier = Modifier.height(AppDimens.spacingS))
                    Button(
                        onClick = {
                            repository.resetAssistantRequestSettings(assistantId)
                            requestSettings = repository.getAssistantRequestSettings(assistantId)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.assistant_reset_model_params))
                    }
                }
            }
        }
    }

    fun LazyListScope.promptSettingsItems() {
        if (!showPromptSettings) return
        item {
            SettingsSectionCard(title = stringResource(R.string.assistant_prompt_settings)) {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.cardSpacing)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            modifier = Modifier.width(96.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline,
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .clickable { showEmojiPicker = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = assistantEmoji,
                                    fontSize = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(AppDimens.spacingM))
                        OutlinedTextField(
                            value = assistantName,
                            onValueChange = {
                                assistantName = it
                                repository.updateAssistantPromptSettings(assistantId, assistantName, assistantEmoji, assistantPrompt)
                            },
                            label = { Text(stringResource(R.string.assistant_name)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = assistantPrompt,
                        onValueChange = {
                            assistantPrompt = it
                            repository.updateAssistantPromptSettings(assistantId, assistantName, assistantEmoji, assistantPrompt)
                        },
                        label = { Text(stringResource(R.string.assistant_prompt_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        placeholder = { Text(stringResource(R.string.assistant_prompt_hint)) }
                    )
                    Text(
                        text = stringResource(R.string.assistant_prompt_token_estimate, estimateTokenCount(assistantPrompt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AppDimens.spacingL),
        contentPadding = PaddingValues(bottom = AppDimens.screenPaddingBottom)
    ) {
        if (headerContent != null) {
            item {
                headerContent()
            }
        }

        if (promptSettingsFirst) {
            promptSettingsItems()
            modelSettingsItems()
        } else {
            modelSettingsItems()
            promptSettingsItems()
        }

        if (showKnowledgeBase) {
            item {
                SettingsSectionCard(title = stringResource(R.string.assistant_knowledge_settings)) {
                    SettingsListItem(
                        title = stringResource(R.string.drawer_knowledge_base),
                        subtitle = stringResource(R.string.assistant_knowledge_settings_subtitle),
                        leading = { Icon(Icons.Outlined.Storage, contentDescription = null) },
                        onClick = onOpenKnowledgeBase
                    )
                }
            }
        }

        if (showMcpSettings) {
            item {
                SettingsSectionCard(title = stringResource(R.string.assistant_mcp_settings)) {
                    SettingsListItem(
                        title = stringResource(R.string.settings_mcp_title),
                        subtitle = stringResource(R.string.assistant_mcp_settings_subtitle),
                        leading = { Icon(Icons.Outlined.Build, contentDescription = null) },
                        onClick = onOpenMcpSettings
                    )
                }
            }
        }

        if (showCommonPhrases) {
            item {
                SettingsSectionCard(title = stringResource(R.string.assistant_common_phrases)) {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.cardSpacing)) {
                        OutlinedTextField(
                            value = newPhrase,
                            onValueChange = { newPhrase = it },
                            label = { Text(stringResource(R.string.assistant_add_phrase)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                val trimmed = newPhrase.trim()
                                if (trimmed.isNotEmpty()) {
                                    commonPhrases = commonPhrases + trimmed
                                    repository.setAssistantCommonPhrases(assistantId, commonPhrases)
                                    newPhrase = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.action_add))
                        }
                        if (commonPhrases.isEmpty()) {
                            Text(stringResource(R.string.assistant_no_common_phrases), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            commonPhrases.forEachIndexed { index, phrase ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(AppDimens.cardSpacing),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = phrase,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    IconButton(onClick = {
                                        commonPhrases = commonPhrases.filterNot { it == phrase }
                                        repository.setAssistantCommonPhrases(assistantId, commonPhrases)
                                    }) {
                                        Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.assistant_delete_phrase))
                                    }
                                }
                                if (index != commonPhrases.lastIndex) {
                                    Spacer(modifier = Modifier.height(AppDimens.spacingS))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showGlobalMemory) {
            item {
                SettingsSectionCard(title = stringResource(R.string.assistant_global_memory)) {
                    SettingsListItem(
                        title = stringResource(R.string.settings_global_memory_title),
                        subtitle = stringResource(R.string.assistant_global_memory_subtitle),
                        leading = { Icon(Icons.Outlined.Memory, contentDescription = null) },
                        onClick = onOpenGlobalMemory
                    )
                }
            }
        }

        if (footerContent != null) {
            item {
                footerContent()
            }
        }
    }
}

private fun resolveModelLabel(repository: Repository, assistantId: String): String {
    if (ModelSelectionValidator.isModelMissing(repository, assistantId)) {
        return repository.getContext().getString(R.string.chat_model_unselected)
    }
    val config = repository.getAssistantModelConfig(assistantId)
    return if (config.useCloudModel) {
        "${config.serviceProvider} · ${config.selectedModelName}"
    } else {
        repository.getContext().getString(R.string.assistant_local_model_format, config.localSelectedModelName)
    }
}

private fun applyModelSelection(repository: Repository, assistantId: String, ref: String) {
    val parts = ref.split("|")
    if (parts.size != 2) return
    val provider = parts[0]
    val model = parts[1]
    if (provider == "local") {
        repository.setAssistantUseCloudModel(assistantId, false)
        repository.setAssistantLocalSelectedModel(assistantId, model)
    } else {
        repository.setAssistantUseCloudModel(assistantId, true)
        repository.setAssistantServiceProvider(assistantId, provider)
        repository.setAssistantSelectedModel(assistantId, model)
    }
}

private fun estimateTokenCount(text: String): Int {
    if (text.isBlank()) return 0
    val trimmed = text.trim()
    val asciiTokens = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
    val nonAsciiCount = trimmed.count { it.code > 127 }
    return asciiTokens.size + nonAsciiCount
}
