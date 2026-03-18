package com.huajuan.aispace.data

object ModelSelectionValidator {
    fun isModelMissing(config: AssistantModelConfig): Boolean {
        return if (config.useCloudModel) {
            config.selectedModelName.isBlank()
        } else {
            config.localSelectedModelName.isBlank()
        }
    }

    fun isModelMissing(repository: Repository, assistantId: String = repository.getCurrentAssistantId()): Boolean {
        val config = repository.getAssistantModelConfig(assistantId)
        if (isModelMissing(config)) return true
        return if (config.useCloudModel) {
            val provider = config.serviceProvider
            val modelName = config.selectedModelName
            val modelList = ModelDataProvider(repository).getModelListForProvider(provider)
            val exists = modelList.any { it.displayName == modelName }
            if (!exists) {
                repository.setAssistantSelectedModel(assistantId, "")
            }
            !exists
        } else {
            val modelName = config.localSelectedModelName
            val exists = repository.getLocalModelList().any { it.displayName == modelName }
            if (!exists) {
                repository.setAssistantLocalSelectedModel(assistantId, "")
            }
            !exists
        }
    }

    fun missingModelPrompt(): String = ModelErrorParser.normalizeUserError(
        com.huajuan.aispace.i18n.LocalizedResources.getString(
            com.huajuan.aispace.HuaJuanApplication.instance,
            com.huajuan.aispace.R.string.model_requirement_missing_model_message
        )
    ).removePrefix(
        com.huajuan.aispace.i18n.LocalizedResources.getString(
            com.huajuan.aispace.HuaJuanApplication.instance,
            com.huajuan.aispace.R.string.error_prefix,
            null,
            ""
        )
    )

    fun missingModelError(): String = ModelErrorParser.normalizeUserError(missingModelPrompt())

    fun isApiKeyMissing(repository: Repository, assistantId: String = repository.getCurrentAssistantId()): Boolean {
        val config = repository.getAssistantModelConfig(assistantId)
        if (!config.useCloudModel) return false
        val apiKey = repository.getApiKeyForProvider(config.serviceProvider)
        return apiKey.isBlank()
    }

    fun missingApiKeyPrompt(): String = ModelErrorParser.normalizeUserError(
        com.huajuan.aispace.i18n.LocalizedResources.getString(
            com.huajuan.aispace.HuaJuanApplication.instance,
            com.huajuan.aispace.R.string.model_requirement_missing_api_key_message
        )
    ).removePrefix(
        com.huajuan.aispace.i18n.LocalizedResources.getString(
            com.huajuan.aispace.HuaJuanApplication.instance,
            com.huajuan.aispace.R.string.error_prefix,
            null,
            ""
        )
    )

    fun missingApiKeyError(): String = ModelErrorParser.normalizeUserError(missingApiKeyPrompt())
}
