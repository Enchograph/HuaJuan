package com.huajuan.aispace.screens.settings.sections

import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huajuan.aispace.R
import com.huajuan.aispace.components.settings.SettingsSectionCard
import com.huajuan.aispace.components.settings.SettingsSwitchItem
import com.huajuan.aispace.data.Repository
import com.huajuan.aispace.ui.theme.AppDimens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun DebugSettingSection(
    debugMode: Boolean,
    tokenAnimationFixedDurationEnabled: Boolean,
    tokenAnimationFixedDurationMs: Int,
    repository: Repository,
    scope: CoroutineScope,
    onDebugModeChange: (Boolean) -> Unit = {},
    onTokenAnimationFixedDurationEnabledChange: (Boolean) -> Unit = {},
    onTokenAnimationFixedDurationMsChange: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    var showClearConfirm by remember { mutableStateOf(false) }

    SettingsSectionCard {
        SettingsSwitchItem(
            title = stringResource(R.string.settings_debug_title),
            subtitle = stringResource(if (debugMode) R.string.state_enabled else R.string.state_disabled),
            checked = debugMode,
            onCheckedChange = {
                onDebugModeChange(it)
                repository.setDebugMode(it)
            }
        )

        if (debugMode) {
            Spacer(Modifier.height(AppDimens.spacingM))

            SettingsSwitchItem(
                title = stringResource(R.string.settings_debug_token_animation_title),
                subtitle = if (tokenAnimationFixedDurationEnabled) {
                    stringResource(R.string.settings_debug_token_animation_enabled, tokenAnimationFixedDurationMs)
                } else {
                    stringResource(R.string.settings_debug_token_animation_disabled)
                },
                checked = tokenAnimationFixedDurationEnabled,
                onCheckedChange = {
                    onTokenAnimationFixedDurationEnabledChange(it)
                    repository.setTokenAnimationFixedDurationEnabled(it)
                }
            )

            if (tokenAnimationFixedDurationEnabled) {
                Spacer(Modifier.height(AppDimens.spacingS))
                Text(
                    text = stringResource(R.string.settings_debug_token_animation_value, tokenAnimationFixedDurationMs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = tokenAnimationFixedDurationMs.toFloat(),
                    onValueChange = { value ->
                        val duration = value.toInt().coerceIn(40, 800)
                        onTokenAnimationFixedDurationMsChange(duration)
                        repository.setTokenAnimationFixedDurationMs(duration)
                    },
                    valueRange = 40f..800f
                )
            }

            Spacer(Modifier.height(AppDimens.spacingM))

            Button(
                onClick = {
                    scope.launch {
                        try {
                            val assistantId = repository.getCurrentAssistantId()
                            val assistantName = repository.getAssistantName(assistantId)
                            val conversations = repository.getConversationsAsync(assistantId)
                            conversations.forEach { conversation ->
                                repository.deleteConversation(conversation.id)
                            }
                            Toast.makeText(context, context.getString(R.string.settings_debug_cleared_current_assistant, assistantName), Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, context.getString(R.string.settings_debug_clear_conversation_failed, e.message), Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = AppDimens.spacingS),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(R.string.settings_debug_clear_current_assistant_conversations))
            }

            Button(
                onClick = {
                    scope.launch {
                        try {
                            val agents = repository.getCustomAssistants() +
                                com.huajuan.aispace.data.AgentProvider(repository.getContext()).getAgents()
                            agents.forEach { agent ->
                                val conversations = repository.getConversationsAsync(agent.id)
                                conversations.forEach { conversation ->
                                    repository.deleteConversation(conversation.id)
                                }
                            }
                            Toast.makeText(context, context.getString(R.string.settings_debug_cleared_all_assistants), Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, context.getString(R.string.settings_debug_clear_conversation_failed, e.message), Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = AppDimens.spacingS),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(R.string.settings_debug_clear_all_conversations))
            }

            Button(
                onClick = { showClearConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(R.string.settings_debug_delete_local_data))
            }

            if (showClearConfirm) {
                AlertDialog(
                    onDismissRequest = { showClearConfirm = false },
                    title = { Text(stringResource(R.string.settings_debug_delete_local_data_confirm_title)) },
                    text = { Text(stringResource(R.string.settings_debug_delete_local_data_confirm_message)) },
                    confirmButton = {
                        TextButton(onClick = {
                            showClearConfirm = false
                            scope.launch {
                                try {
                                    repository.clearUserDataExceptApiKeys()
                                    Toast.makeText(context, context.getString(R.string.settings_debug_local_data_cleared), Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, context.getString(R.string.settings_debug_delete_failed, e.message), Toast.LENGTH_LONG).show()
                                }
                            }
                        }) { Text(stringResource(R.string.settings_confirm)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
                    }
                )
            }
        }
    }
}
