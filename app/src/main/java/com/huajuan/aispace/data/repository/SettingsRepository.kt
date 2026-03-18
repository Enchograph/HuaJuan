package com.huajuan.aispace.data.repository

import com.huajuan.aispace.data.Repository
import com.huajuan.aispace.data.ModelCapability
import com.huajuan.aispace.data.ProviderType
import android.content.Context
class SettingsRepository(
    private val repository: Repository
) {
    fun getContext(): Context = repository.getContext()

    fun getDarkMode(): Boolean = repository.getDarkMode()

    fun setDarkMode(isDarkMode: Boolean) = repository.setDarkMode(isDarkMode)

    fun getThemeMode(): String = repository.getThemeMode()

    fun setThemeMode(themeMode: String) = repository.setThemeMode(themeMode)

    fun getFontScale(): Float = repository.getFontScale()

    fun setFontScale(fontScale: Float) = repository.setFontScale(fontScale)

    fun getTransparencyLevel(): String = repository.getTransparencyLevel()

    fun setTransparencyLevel(level: String) = repository.setTransparencyLevel(level)

    fun getDebugMode(): Boolean = repository.getDebugMode()

    fun setDebugMode(isDebugMode: Boolean) = repository.setDebugMode(isDebugMode)

    fun getTokenAnimationFixedDurationEnabled(): Boolean =
        repository.getTokenAnimationFixedDurationEnabled()

    fun setTokenAnimationFixedDurationEnabled(enabled: Boolean) =
        repository.setTokenAnimationFixedDurationEnabled(enabled)

    fun getTokenAnimationFixedDurationMs(): Int =
        repository.getTokenAnimationFixedDurationMs()

    fun setTokenAnimationFixedDurationMs(durationMs: Int) =
        repository.setTokenAnimationFixedDurationMs(durationMs)

    fun getLanguage(): String = repository.getLanguage()

    fun setLanguage(language: String) = repository.setLanguage(language)

    fun getNotificationsEnabled(): Boolean = repository.getNotificationsEnabled()

    fun setNotificationsEnabled(enabled: Boolean) = repository.setNotificationsEnabled(enabled)

    fun getUseCloudModel(): Boolean = repository.getUseCloudModel()

    fun setUseCloudModel(useCloudModel: Boolean) = repository.setUseCloudModel(useCloudModel)

    fun getServiceProvider(): String = repository.getServiceProvider()

    fun setServiceProvider(serviceProvider: String) = repository.setServiceProvider(serviceProvider)

    fun getCustomApiUrl(): String = repository.getCustomApiUrl()

    fun setCustomApiUrl(customApiUrl: String) = repository.setCustomApiUrl(customApiUrl)

    fun getApiKeyForProvider(provider: String): String = repository.getApiKeyForProvider(provider)

    fun setApiKeyForProvider(provider: String, apiKey: String) =
        repository.setApiKeyForProvider(provider, apiKey)

    fun getCustomServiceProviders(): Set<String> = repository.getCustomServiceProviders()

    fun addCustomServiceProvider(name: String, apiUrl: String, providerType: ProviderType = ProviderType.OpenAI) =
        repository.addCustomServiceProvider(name, apiUrl, providerType)

    fun removeCustomServiceProvider(name: String) = repository.removeCustomServiceProvider(name)

    fun getCustomProviderType(name: String): ProviderType = repository.getCustomProviderType(name)

    fun getCustomProviderApiUrl(name: String): String = repository.getCustomProviderApiUrl(name)

    fun getCustomModelsForProvider(providerName: String): Set<String> =
        repository.getCustomModelsForProvider(providerName)

    fun addCustomModelToProvider(
        providerName: String,
        modelName: String,
        apiCode: String,
        capabilities: Set<ModelCapability> = setOf(ModelCapability.Chat)
    ) = repository.addCustomModelToProvider(providerName, modelName, apiCode, capabilities)

    fun removeCustomModelFromProvider(providerName: String, modelName: String) =
        repository.removeCustomModelFromProvider(providerName, modelName)

    fun getCustomModelApiCode(providerName: String, modelName: String): String =
        repository.getCustomModelApiCode(providerName, modelName)

    fun getServiceProviderEnabled(provider: String): Boolean =
        repository.getServiceProviderEnabled(provider)

    fun setServiceProviderEnabled(provider: String, enabled: Boolean) =
        repository.setServiceProviderEnabled(provider, enabled)

    fun getDocumentParseEnabled(): Boolean = repository.getDocumentParseEnabled()

    fun getKnowledgeSettings() = repository.getKnowledgeSettings()

    fun setKnowledgeSettings(settings: com.huajuan.aispace.data.KnowledgeSettings) =
        repository.setKnowledgeSettings(settings)

    fun setDocumentParseEnabled(enabled: Boolean) = repository.setDocumentParseEnabled(enabled)

    fun getDocumentSummaryEnabled(): Boolean = repository.getDocumentSummaryEnabled()

    fun setDocumentSummaryEnabled(enabled: Boolean) = repository.setDocumentSummaryEnabled(enabled)

    fun getDocumentAutoOcrEnabled(): Boolean = repository.getDocumentAutoOcrEnabled()

    fun setDocumentAutoOcrEnabled(enabled: Boolean) =
        repository.setDocumentAutoOcrEnabled(enabled)

    fun getDocumentPreferEpub(): Boolean = repository.getDocumentPreferEpub()

    fun setDocumentPreferEpub(enabled: Boolean) = repository.setDocumentPreferEpub(enabled)

    fun getDocumentRenderQuality(): String = repository.getDocumentRenderQuality()

    fun setDocumentRenderQuality(quality: String) = repository.setDocumentRenderQuality(quality)

    fun getDocumentRemoveBackground(): Boolean = repository.getDocumentRemoveBackground()

    fun setDocumentRemoveBackground(enabled: Boolean) =
        repository.setDocumentRemoveBackground(enabled)

    fun getSearchEngine(): String = repository.getSearchEngine()

    fun setSearchEngine(engine: String) = repository.setSearchEngine(engine)

    fun getWebSearchApiKey(): String = repository.getWebSearchApiKey()

    fun setWebSearchApiKey(apiKey: String) = repository.setWebSearchApiKey(apiKey)

    fun getWebSearchProviderApiKey(providerId: String): String =
        repository.getWebSearchProviderApiKey(providerId)

    fun setWebSearchProviderApiKey(providerId: String, apiKey: String) =
        repository.setWebSearchProviderApiKey(providerId, apiKey)

    fun getWebSearchResultCount(): Int = repository.getWebSearchResultCount()

    fun setWebSearchResultCount(count: Int) = repository.setWebSearchResultCount(count)

    fun getWebSearchIncludeDate(): Boolean = repository.getWebSearchIncludeDate()

    fun setWebSearchIncludeDate(enabled: Boolean) = repository.setWebSearchIncludeDate(enabled)

    fun getWebSearchCompression(): String = repository.getWebSearchCompression()

    fun setWebSearchCompression(mode: String) = repository.setWebSearchCompression(mode)

    fun getWebSearchBlacklist(): Set<String> = repository.getWebSearchBlacklist()

    fun setWebSearchBlacklist(patterns: Set<String>) = repository.setWebSearchBlacklist(patterns)

    fun getMcpToolsEnabled(): Boolean = repository.getMcpToolsEnabled()

    fun setMcpToolsEnabled(enabled: Boolean) = repository.setMcpToolsEnabled(enabled)

    fun getMcpPermissionLogEnabled(): Boolean = repository.getMcpPermissionLogEnabled()

    fun setMcpPermissionLogEnabled(enabled: Boolean) =
        repository.setMcpPermissionLogEnabled(enabled)

    fun getMcpEnabledTools(): Set<String> = repository.getMcpEnabledTools()

    fun setMcpEnabledTools(tools: Set<String>) = repository.setMcpEnabledTools(tools)

    fun getMcpServerUrls(): Set<String> = repository.getMcpServerUrls()

    fun setMcpServerUrls(urls: Set<String>) = repository.setMcpServerUrls(urls)

    fun getGlobalMemoryEnabled(): Boolean = repository.getGlobalMemoryEnabled()

    fun setGlobalMemoryEnabled(enabled: Boolean) = repository.setGlobalMemoryEnabled(enabled)

    fun getGlobalMemoryRetentionDays(): Int = repository.getGlobalMemoryRetentionDays()

    fun setGlobalMemoryRetentionDays(days: Int) =
        repository.setGlobalMemoryRetentionDays(days)

    fun getGlobalMemoryMode(): String = repository.getGlobalMemoryMode()

    fun setGlobalMemoryMode(mode: String) = repository.setGlobalMemoryMode(mode)

    fun getGlobalMemoryLastClearedAt(): Long = repository.getGlobalMemoryLastClearedAt()

    fun setGlobalMemoryLastClearedAt(timestamp: Long) =
        repository.setGlobalMemoryLastClearedAt(timestamp)

    fun getModelTemperature(provider: String, model: String): Float =
        repository.getModelTemperature(provider, model)

    fun setModelTemperature(provider: String, model: String, temperature: Float) =
        repository.setModelTemperature(provider, model, temperature)

    fun getModelTopP(provider: String, model: String): Float =
        repository.getModelTopP(provider, model)

    fun setModelTopP(provider: String, model: String, topP: Float) =
        repository.setModelTopP(provider, model, topP)

    fun resetSettingsToDefaults() = repository.resetSettingsToDefaults()
}
