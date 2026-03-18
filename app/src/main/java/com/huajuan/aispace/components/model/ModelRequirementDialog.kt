package com.huajuan.aispace.components.model

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.huajuan.aispace.R
import com.huajuan.aispace.data.ModelDataProvider
import com.huajuan.aispace.data.ModelSelectionValidator
import com.huajuan.aispace.data.ProviderType
import com.huajuan.aispace.data.Repository

enum class ModelRequirementIssue {
    MissingModel,
    MissingApiKey
}

fun resolveModelRequirementIssue(
    repository: Repository,
    assistantId: String
): ModelRequirementIssue? {
    return when {
        ModelSelectionValidator.isModelMissing(repository, assistantId) -> ModelRequirementIssue.MissingModel
        ModelSelectionValidator.isApiKeyMissing(repository, assistantId) -> ModelRequirementIssue.MissingApiKey
        else -> null
    }
}

fun resolveKnowledgeModelRequirementIssue(
    repository: Repository,
    modelRef: String
): ModelRequirementIssue? {
    if (modelRef.isBlank()) return ModelRequirementIssue.MissingModel
    val parts = modelRef.split("|", limit = 2)
    if (parts.size != 2) return ModelRequirementIssue.MissingModel
    val provider = parts[0]
    val modelName = parts[1]
    val exists = ModelDataProvider(repository)
        .getModelListForProvider(provider)
        .any { it.displayName == modelName }
    if (!exists) return ModelRequirementIssue.MissingModel
    val providerType = repository.getProviderType(provider)
    if (providerType != ProviderType.Anthropic && repository.getApiKeyForProvider(provider).isBlank()) {
        return ModelRequirementIssue.MissingApiKey
    }
    return null
}

@Composable
fun ModelRequirementDialog(
    issue: ModelRequirementIssue?,
    onDismiss: () -> Unit,
    onOpenModelPicker: () -> Unit,
    onOpenProviderSettings: () -> Unit
) {
    if (issue == null) return

    val title = when (issue) {
        ModelRequirementIssue.MissingModel -> stringResource(R.string.model_requirement_missing_model_title)
        ModelRequirementIssue.MissingApiKey -> stringResource(R.string.model_requirement_missing_api_key_title)
    }
    val message = when (issue) {
        ModelRequirementIssue.MissingModel -> stringResource(R.string.model_requirement_missing_model_message)
        ModelRequirementIssue.MissingApiKey -> stringResource(R.string.model_requirement_missing_api_key_message)
    }
    val confirmText = when (issue) {
        ModelRequirementIssue.MissingModel -> stringResource(R.string.action_select)
        ModelRequirementIssue.MissingApiKey -> stringResource(R.string.action_fill)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                when (issue) {
                    ModelRequirementIssue.MissingModel -> onOpenModelPicker()
                    ModelRequirementIssue.MissingApiKey -> onOpenProviderSettings()
                }
            }) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
