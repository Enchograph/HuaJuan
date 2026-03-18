package com.huajuan.aispace.data.repository.internal

import com.huajuan.aispace.data.ImageGenerationProbeRecord
import com.huajuan.aispace.data.ModelCapability
import com.huajuan.aispace.data.ModelInfo
import com.huajuan.aispace.data.ProviderType

internal class ModelPreferenceStore(
    private val preferenceStore: AppPreferenceStore,
    private val modelCatalogStore: ModelCatalogStore
) {
    fun getUseCloudModel(): Boolean {
        return preferenceStore.getUseCloudModel()
    }

    fun setUseCloudModel(useCloudModel: Boolean) {
        preferenceStore.setUseCloudModel(useCloudModel)
    }

    fun getServiceProvider(): String {
        return preferenceStore.getServiceProvider()
    }

    fun setServiceProvider(serviceProvider: String) {
        preferenceStore.setServiceProvider(serviceProvider)
    }

    fun getApiKeyForProvider(provider: String): String {
        return preferenceStore.getApiKeyForProvider(provider)
    }

    fun setApiKeyForProvider(provider: String, apiKey: String) {
        preferenceStore.setApiKeyForProvider(provider, apiKey)
    }

    fun getApiKey(): String {
        val currentProvider = getServiceProvider()
        return getApiKeyForProvider(currentProvider)
    }

    fun setApiKey(apiKey: String) {
        val currentProvider = getServiceProvider()
        setApiKeyForProvider(currentProvider, apiKey)
    }

    fun getSelectedModelForProvider(provider: String): String {
        return preferenceStore.getSelectedModelForProvider(provider)
    }

    fun getSelectedModel(): String {
        val currentProvider = getServiceProvider()
        return getSelectedModelForProvider(currentProvider)
    }

    fun getCustomServiceProviders(): Set<String> {
        return preferenceStore.getCustomServiceProviders()
    }

    fun addCustomServiceProvider(name: String, apiUrl: String, providerType: ProviderType) {
        preferenceStore.addCustomServiceProvider(name, apiUrl, providerType)
    }

    fun removeCustomServiceProvider(name: String) {
        preferenceStore.removeCustomServiceProvider(name)
    }

    fun getCustomProviderApiUrl(name: String): String {
        return preferenceStore.getCustomProviderApiUrl(name)
    }

    fun getCustomProviderType(name: String): ProviderType {
        return preferenceStore.getCustomProviderType(name)
    }

    fun getCustomModelsForProvider(providerName: String): Set<String> {
        return preferenceStore.getCustomModelsForProvider(providerName)
    }

    fun addCustomModelToProvider(
        providerName: String,
        modelName: String,
        apiCode: String,
        capabilities: Set<ModelCapability>
    ) {
        preferenceStore.addCustomModelToProvider(providerName, modelName, apiCode, capabilities)
    }

    fun removeCustomModelFromProvider(providerName: String, modelName: String) {
        preferenceStore.removeCustomModelFromProvider(providerName, modelName)
    }

    fun getCustomModelApiCode(providerName: String, modelName: String): String {
        return preferenceStore.getCustomModelApiCode(providerName, modelName)
    }

    fun getCustomModelCapabilities(providerName: String, modelName: String): Set<ModelCapability> {
        return preferenceStore.getCustomModelCapabilities(providerName, modelName)
    }

    fun getImageGenerationProbeRecord(providerName: String, modelApiCode: String): ImageGenerationProbeRecord? {
        return preferenceStore.getImageGenerationProbeRecord(providerName, modelApiCode)
    }

    fun setImageGenerationProbeRecord(providerName: String, modelApiCode: String, record: ImageGenerationProbeRecord) {
        preferenceStore.setImageGenerationProbeRecord(providerName, modelApiCode, record)
    }

    fun clearImageGenerationProbeRecord(providerName: String, modelApiCode: String) {
        preferenceStore.clearImageGenerationProbeRecord(providerName, modelApiCode)
    }

    fun getLocalSelectedModel(assistantId: String): String {
        return modelCatalogStore.getLocalSelectedModel(assistantId)
    }

    fun setLocalSelectedModel(model: String, assistantId: String) {
        modelCatalogStore.setLocalSelectedModel(assistantId, model)
    }

    fun getLocalModelPath(modelName: String): String {
        return modelCatalogStore.getLocalModelPath(modelName)
    }

    fun getLocalModelList(): List<ModelInfo> {
        return modelCatalogStore.getLocalModelList()
    }

    fun getModelTemperature(provider: String, model: String): Float {
        return preferenceStore.getModelTemperature(provider, model)
    }

    fun setModelTemperature(provider: String, model: String, temperature: Float) {
        preferenceStore.setModelTemperature(provider, model, temperature)
    }

    fun getModelTopP(provider: String, model: String): Float {
        return preferenceStore.getModelTopP(provider, model)
    }

    fun setModelTopP(provider: String, model: String, topP: Float) {
        preferenceStore.setModelTopP(provider, model, topP)
    }
}
