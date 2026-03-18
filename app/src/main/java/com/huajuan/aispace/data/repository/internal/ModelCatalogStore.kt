package com.huajuan.aispace.data.repository.internal

import com.huajuan.aispace.data.ModelInfo
import com.huajuan.aispace.data.ModelManager

internal class ModelCatalogStore(
    private val preferenceStore: AppPreferenceStore,
    private val modelManager: ModelManager
) {
    fun getLocalSelectedModel(assistantId: String): String {
        val fallback = preferenceStore.getLocalSelectedModel()
        val selectedModel = preferenceStore.getAssistantLocalSelectedModel(assistantId, fallback)
        val localModels = modelManager.getLocalModels()
        if (selectedModel.isNotBlank() && localModels.any { it.displayName == selectedModel }) {
            return selectedModel
        }
        if (selectedModel.isNotBlank()) {
            if (selectedModel == fallback) {
                preferenceStore.setLocalSelectedModel("")
            }
            preferenceStore.setAssistantLocalSelectedModel(assistantId, "")
        }
        return ""
    }

    fun setLocalSelectedModel(assistantId: String, model: String) {
        preferenceStore.setLocalSelectedModel(model)
        preferenceStore.setAssistantLocalSelectedModel(assistantId, model)
    }

    fun getLocalModelPath(modelName: String): String {
        return modelManager.getModelPath(modelName)
    }

    fun getLocalModelList(): List<ModelInfo> {
        return modelManager.getLocalModels()
    }
}
