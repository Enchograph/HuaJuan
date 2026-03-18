package com.huajuan.aispace.screens.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huajuan.aispace.R
import com.huajuan.aispace.data.Repository
import com.huajuan.aispace.components.settings.SettingsItemDivider
import com.huajuan.aispace.components.settings.SettingsListItem
import com.huajuan.aispace.components.settings.SettingsSectionCard
import com.huajuan.aispace.ui.theme.AppDimens

enum class AssistantSettingsSection {
    Model,
    Prompt,
    KnowledgeBase,
    Mcp,
    CommonPhrases,
    GlobalMemory
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantSettingsHomeScreen(
    repository: Repository,
    assistantId: String,
    onBack: () -> Unit,
    onOpenAdvanced: () -> Unit,
    onSwitchAssistant: (String) -> Unit,
    onDeleteAssistant: (String) -> Unit = {}
) {
    val assistant = repository.getAssistantById(assistantId)
    val isCustomAssistant = assistantId.startsWith("custom_")
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = assistant?.name ?: stringResource(R.string.assistant_settings), fontWeight = FontWeight.SemiBold) },
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
        AssistantSettingsPanel(
            repository = repository,
            assistantId = assistantId,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(top = AppDimens.screenPadding),
            onOpenKnowledgeBase = {},
            onOpenMcpSettings = {},
            onOpenGlobalMemory = {},
            promptSettingsFirst = true,
            showModelAdvancedSettings = false,
            showKnowledgeBase = false,
            showMcpSettings = false,
            showCommonPhrases = false,
            showGlobalMemory = false,
            footerContent = {
                SettingsSectionCard(title = stringResource(R.string.assistant_advanced_settings)) {
                    SettingsListItem(
                        title = stringResource(R.string.settings_more_title),
                        subtitle = stringResource(R.string.assistant_advanced_settings_subtitle),
                        leading = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                        onClick = onOpenAdvanced
                    )
                }
                Spacer(modifier = Modifier.height(AppDimens.spacingS))
                Button(
                    onClick = { onSwitchAssistant(assistantId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.assistant_confirm_this))
                }
                if (isCustomAssistant) {
                    Spacer(modifier = Modifier.height(AppDimens.spacingS))
                    Button(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(stringResource(R.string.assistant_delete))
                    }
                }
            }
        )
    }

    if (showDeleteConfirm && assistant != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.assistant_delete)) },
            text = { Text(stringResource(R.string.assistant_delete_confirm, assistant.name)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDeleteAssistant(assistantId)
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantSettingsAdvancedScreen(
    repository: Repository,
    assistantId: String,
    onBack: () -> Unit,
    onOpenSection: (AssistantSettingsSection) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.assistant_advanced_settings), fontWeight = FontWeight.SemiBold) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(AppDimens.listSpacing),
            contentPadding = PaddingValues(
                horizontal = AppDimens.screenPadding,
                vertical = AppDimens.screenPadding
            )
        ) {
            item {
                SettingsSectionCard(title = stringResource(R.string.assistant_settings_items)) {
                    SettingsListItem(
                        title = stringResource(R.string.assistant_model_settings),
                        subtitle = stringResource(R.string.assistant_model_settings_subtitle),
                        leading = { Icon(Icons.Outlined.Tune, contentDescription = null) },
                        onClick = { onOpenSection(AssistantSettingsSection.Model) }
                    )
                    SettingsItemDivider()
                    SettingsListItem(
                        title = stringResource(R.string.assistant_knowledge_settings),
                        subtitle = stringResource(R.string.assistant_knowledge_settings_subtitle),
                        leading = { Icon(Icons.Outlined.Storage, contentDescription = null) },
                        onClick = { onOpenSection(AssistantSettingsSection.KnowledgeBase) }
                    )
                    SettingsItemDivider()
                    SettingsListItem(
                        title = stringResource(R.string.assistant_mcp_settings),
                        subtitle = stringResource(R.string.assistant_mcp_settings_subtitle),
                        leading = { Icon(Icons.Outlined.Build, contentDescription = null) },
                        onClick = { onOpenSection(AssistantSettingsSection.Mcp) }
                    )
                    SettingsItemDivider()
                    SettingsListItem(
                        title = stringResource(R.string.assistant_common_phrases),
                        subtitle = stringResource(R.string.assistant_common_phrases_subtitle),
                        leading = { Icon(Icons.Outlined.TextFields, contentDescription = null) },
                        onClick = { onOpenSection(AssistantSettingsSection.CommonPhrases) }
                    )
                    SettingsItemDivider()
                    SettingsListItem(
                        title = stringResource(R.string.assistant_global_memory),
                        subtitle = stringResource(R.string.assistant_global_memory_subtitle),
                        leading = { Icon(Icons.Outlined.Memory, contentDescription = null) },
                        onClick = { onOpenSection(AssistantSettingsSection.GlobalMemory) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantSettingsDetailScreen(
    repository: Repository,
    assistantId: String,
    section: AssistantSettingsSection,
    onBack: () -> Unit,
    onOpenKnowledgeBase: () -> Unit,
    onOpenMcpSettings: () -> Unit,
    onOpenGlobalMemory: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resolveAssistantSettingsSectionTitle(section), fontWeight = FontWeight.SemiBold) },
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
        AssistantSettingsPanel(
            repository = repository,
            assistantId = assistantId,
            modifier = Modifier.padding(paddingValues),
            onOpenKnowledgeBase = onOpenKnowledgeBase,
            onOpenMcpSettings = onOpenMcpSettings,
            onOpenGlobalMemory = onOpenGlobalMemory,
            showModelSettings = section == AssistantSettingsSection.Model,
            showPromptSettings = section == AssistantSettingsSection.Prompt,
            showKnowledgeBase = section == AssistantSettingsSection.KnowledgeBase,
            showMcpSettings = section == AssistantSettingsSection.Mcp,
            showCommonPhrases = section == AssistantSettingsSection.CommonPhrases,
            showGlobalMemory = section == AssistantSettingsSection.GlobalMemory
        )
    }
}

@Composable
private fun resolveAssistantSettingsSectionTitle(section: AssistantSettingsSection): String {
    return when (section) {
        AssistantSettingsSection.Model -> stringResource(R.string.assistant_model_settings)
        AssistantSettingsSection.Prompt -> stringResource(R.string.assistant_prompt_settings)
        AssistantSettingsSection.KnowledgeBase -> stringResource(R.string.assistant_knowledge_settings)
        AssistantSettingsSection.Mcp -> stringResource(R.string.assistant_mcp_settings)
        AssistantSettingsSection.CommonPhrases -> stringResource(R.string.assistant_common_phrases)
        AssistantSettingsSection.GlobalMemory -> stringResource(R.string.assistant_global_memory)
    }
}
