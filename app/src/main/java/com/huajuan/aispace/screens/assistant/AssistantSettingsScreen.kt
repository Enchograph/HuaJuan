package com.huajuan.aispace.screens.assistant

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.huajuan.aispace.data.ModelRequestResolver
import com.huajuan.aispace.data.ModelSelectionValidator
import com.huajuan.aispace.data.Repository
import com.huajuan.aispace.components.settings.SettingsItemDivider
import com.huajuan.aispace.components.settings.SettingsListItem
import com.huajuan.aispace.components.settings.SettingsSectionCard
import com.huajuan.aispace.ui.theme.AppDimens

@Composable
fun AssistantSettingsScreen(
    repository: Repository,
    assistantId: String,
    modifier: Modifier = Modifier,
    onSwitchToAssistant: () -> Unit,
    onOpenMcpSettings: () -> Unit,
    onOpenGlobalMemory: () -> Unit
) {
    var isCurrent by remember { mutableStateOf(assistantId == repository.getCurrentAssistantId()) }

    AssistantSettingsPanel(
        repository = repository,
        assistantId = assistantId,
        modifier = modifier,
        onOpenKnowledgeBase = {},
        onOpenMcpSettings = onOpenMcpSettings,
        onOpenGlobalMemory = onOpenGlobalMemory,
        headerContent = {
            val assistant = repository.getAssistantById(assistantId)
            val resolved = ModelRequestResolver.resolve(repository, assistantId)
            val modelConfig = repository.getAssistantModelConfig(assistantId)
            val modelMissing = ModelSelectionValidator.isModelMissing(repository, assistantId)
            SettingsSectionCard(title = stringResource(R.string.assistant_overview)) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDimens.cardPadding),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = assistant?.emoji ?: "✨",
                                style = MaterialTheme.typography.titleLarge,
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(AppDimens.spacingM))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = assistant?.name ?: stringResource(R.string.default_assistant_name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = assistant?.description ?: stringResource(R.string.assistant_new_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.size(AppDimens.spacingM))
                Button(
                    onClick = {
                        onSwitchToAssistant()
                        isCurrent = true
                    },
                    enabled = !isCurrent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isCurrent) stringResource(R.string.assistant_current_in_use) else stringResource(R.string.assistant_switch_to_this))
                }
            }

            SettingsSectionCard(title = stringResource(R.string.assistant_model_details)) {
                SettingsListItem(
                    title = stringResource(R.string.assistant_model_type),
                    subtitle = if (modelConfig.useCloudModel) stringResource(R.string.settings_cloud_models) else stringResource(R.string.settings_local_models)
                )
                SettingsItemDivider()
                SettingsListItem(
                    title = stringResource(R.string.settings_provider_name),
                    subtitle = if (modelConfig.useCloudModel) resolved.serviceProvider else stringResource(R.string.settings_local_models)
                )
                SettingsItemDivider()
                SettingsListItem(
                    title = stringResource(R.string.settings_model_name),
                    subtitle = if (modelMissing) stringResource(R.string.chat_model_unselected) else resolved.selectedModelDisplayName
                )
                SettingsItemDivider()
                SettingsListItem(
                    title = stringResource(R.string.assistant_model_identifier),
                    subtitle = resolved.modelInfo.apiCode
                )
                if (resolved.modelInfo.modelPath.isNotBlank()) {
                    SettingsItemDivider()
                    SettingsListItem(
                        title = stringResource(R.string.assistant_local_path),
                        subtitle = resolved.modelInfo.modelPath
                    )
                }
            }
        },
        showKnowledgeBase = false,
        showMcpSettings = true,
        showGlobalMemory = true
    )
}
