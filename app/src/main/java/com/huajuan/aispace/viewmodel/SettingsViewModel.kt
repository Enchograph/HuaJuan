package com.huajuan.aispace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.huajuan.aispace.data.BuiltInServiceProviders
import com.huajuan.aispace.data.KnowledgeSettings
import com.huajuan.aispace.data.ModelCapability
import com.huajuan.aispace.data.ProviderType
import com.huajuan.aispace.data.repository.SettingsRepository
import com.huajuan.aispace.i18n.AppLocaleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val language: String = "system",
    val notificationsEnabled: Boolean = true,
    val themeMode: String = "system",
    val fontScale: Float = 1.0f,
    val transparencyLevel: String = "medium",
    val darkMode: Boolean = false,
    val useCloudModel: Boolean = false,
    val serviceProvider: String = BuiltInServiceProviders.SiliconFlow,
    val customApiUrl: String = "",
    val apiKey: String = "",
    val documentAutoOcrEnabled: Boolean = false,
    val documentPreferEpub: Boolean = false,
    val documentRenderQuality: String = "medium",
    val documentRemoveBackground: Boolean = false,
    val searchEngine: String = com.huajuan.aispace.data.WebSearchProviders.defaultSelectionId,
    val searchApiKey: String = "",
    val searchResultCount: Int = 5,
    val searchIncludeDate: Boolean = false,
    val searchCompression: String = "none",
    val searchBlacklist: List<String> = emptyList(),
    val searchProviderApiKeys: Map<String, String> = emptyMap(),
    val knowledgeSettings: KnowledgeSettings = KnowledgeSettings(),
    val mcpToolsEnabled: Boolean = false,
    val globalMemoryEnabled: Boolean = false,
    val globalMemoryRetentionDays: Int = 30,
    val globalMemoryMode: String = "context",
    val globalMemoryLastClearedAt: Long = 0L,
    val debugMode: Boolean = false,
    val tokenAnimationFixedDurationEnabled: Boolean = false,
    val tokenAnimationFixedDurationMs: Int = 120
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(loadState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private fun loadState(): SettingsUiState {
        val serviceProvider = settingsRepository.getServiceProvider()
        return SettingsUiState(
            language = settingsRepository.getLanguage(),
            notificationsEnabled = settingsRepository.getNotificationsEnabled(),
            themeMode = settingsRepository.getThemeMode(),
            fontScale = settingsRepository.getFontScale(),
            transparencyLevel = settingsRepository.getTransparencyLevel(),
            darkMode = settingsRepository.getDarkMode(),
            useCloudModel = settingsRepository.getUseCloudModel(),
            serviceProvider = serviceProvider,
            customApiUrl = settingsRepository.getCustomApiUrl(),
            apiKey = settingsRepository.getApiKeyForProvider(serviceProvider),
            documentAutoOcrEnabled = settingsRepository.getDocumentAutoOcrEnabled(),
            documentPreferEpub = settingsRepository.getDocumentPreferEpub(),
            documentRenderQuality = settingsRepository.getDocumentRenderQuality(),
            documentRemoveBackground = settingsRepository.getDocumentRemoveBackground(),
            searchEngine = settingsRepository.getSearchEngine(),
            searchApiKey = settingsRepository.getWebSearchApiKey(),
            searchResultCount = settingsRepository.getWebSearchResultCount(),
            searchIncludeDate = settingsRepository.getWebSearchIncludeDate(),
            searchCompression = settingsRepository.getWebSearchCompression(),
            searchBlacklist = settingsRepository.getWebSearchBlacklist().toList(),
            searchProviderApiKeys = com.huajuan.aispace.data.WebSearchProviders.apiProviders
                .associate { provider ->
                    val stored = settingsRepository.getWebSearchProviderApiKey(provider.id)
                    val fallback = if (provider.id == settingsRepository.getSearchEngine()) {
                        settingsRepository.getWebSearchApiKey()
                    } else {
                        ""
                    }
                    provider.id to (if (stored.isBlank()) fallback else stored)
                },
            knowledgeSettings = settingsRepository.getKnowledgeSettings(),
            mcpToolsEnabled = settingsRepository.getMcpToolsEnabled(),
            globalMemoryEnabled = settingsRepository.getGlobalMemoryEnabled(),
            globalMemoryRetentionDays = settingsRepository.getGlobalMemoryRetentionDays(),
            globalMemoryMode = settingsRepository.getGlobalMemoryMode(),
            globalMemoryLastClearedAt = settingsRepository.getGlobalMemoryLastClearedAt(),
            debugMode = settingsRepository.getDebugMode(),
            tokenAnimationFixedDurationEnabled = settingsRepository.getTokenAnimationFixedDurationEnabled(),
            tokenAnimationFixedDurationMs = settingsRepository.getTokenAnimationFixedDurationMs()
        )
    }

    private fun refreshState() {
        _uiState.value = loadState()
    }

    fun setDarkMode(enabled: Boolean) {
        settingsRepository.setDarkMode(enabled)
        _uiState.value = _uiState.value.copy(darkMode = enabled)
    }

    fun setThemeMode(mode: String, resolvedDarkMode: Boolean) {
        settingsRepository.setThemeMode(mode)
        settingsRepository.setDarkMode(resolvedDarkMode)
        _uiState.value = _uiState.value.copy(themeMode = mode, darkMode = resolvedDarkMode)
    }

    fun setFontScale(fontScale: Float) {
        settingsRepository.setFontScale(fontScale)
        _uiState.value = _uiState.value.copy(fontScale = fontScale)
    }

    fun setTransparencyLevel(level: String) {
        settingsRepository.setTransparencyLevel(level)
        _uiState.value = _uiState.value.copy(transparencyLevel = level)
    }

    fun setLanguage(language: String) {
        val normalized = AppLocaleManager.normalizeLanguage(language)
        settingsRepository.setLanguage(normalized)
        AppLocaleManager.applyAppLanguage(settingsRepository.getContext(), normalized)
        _uiState.value = _uiState.value.copy(language = normalized)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        settingsRepository.setNotificationsEnabled(enabled)
        _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
    }

    fun setUseCloudModel(enabled: Boolean) {
        settingsRepository.setUseCloudModel(enabled)
        _uiState.value = _uiState.value.copy(useCloudModel = enabled)
    }

    fun setServiceProvider(provider: String) {
        settingsRepository.setServiceProvider(provider)
        _uiState.value = _uiState.value.copy(
            serviceProvider = provider,
            apiKey = settingsRepository.getApiKeyForProvider(provider)
        )
    }

    fun setCustomApiUrl(url: String) {
        settingsRepository.setCustomApiUrl(url)
        _uiState.value = _uiState.value.copy(customApiUrl = url)
    }

    fun setApiKey(apiKey: String) {
        val provider = _uiState.value.serviceProvider
        settingsRepository.setApiKeyForProvider(provider, apiKey)
        _uiState.value = _uiState.value.copy(apiKey = apiKey)
    }

    fun setApiKeyForProvider(provider: String, apiKey: String) {
        settingsRepository.setApiKeyForProvider(provider, apiKey)
        if (_uiState.value.serviceProvider == provider) {
            _uiState.value = _uiState.value.copy(apiKey = apiKey)
        }
    }

    fun setDocumentAutoOcrEnabled(enabled: Boolean) {
        settingsRepository.setDocumentAutoOcrEnabled(enabled)
        _uiState.value = _uiState.value.copy(documentAutoOcrEnabled = enabled)
    }

    fun setDocumentPreferEpub(enabled: Boolean) {
        settingsRepository.setDocumentPreferEpub(enabled)
        _uiState.value = _uiState.value.copy(documentPreferEpub = enabled)
    }

    fun setDocumentRenderQuality(quality: String) {
        settingsRepository.setDocumentRenderQuality(quality)
        _uiState.value = _uiState.value.copy(documentRenderQuality = quality)
    }

    fun setDocumentRemoveBackground(enabled: Boolean) {
        settingsRepository.setDocumentRemoveBackground(enabled)
        _uiState.value = _uiState.value.copy(documentRemoveBackground = enabled)
    }

    fun setSearchEngine(engine: String) {
        settingsRepository.setSearchEngine(engine)
        _uiState.value = _uiState.value.copy(searchEngine = settingsRepository.getSearchEngine())
    }

    fun setSearchApiKey(apiKey: String) {
        settingsRepository.setWebSearchApiKey(apiKey)
        _uiState.value = _uiState.value.copy(searchApiKey = apiKey)
    }

    fun setSearchResultCount(count: Int) {
        settingsRepository.setWebSearchResultCount(count)
        _uiState.value = _uiState.value.copy(searchResultCount = count)
    }

    fun setSearchIncludeDate(enabled: Boolean) {
        settingsRepository.setWebSearchIncludeDate(enabled)
        _uiState.value = _uiState.value.copy(searchIncludeDate = enabled)
    }

    fun setSearchCompression(mode: String) {
        settingsRepository.setWebSearchCompression(mode)
        _uiState.value = _uiState.value.copy(searchCompression = mode)
    }

    fun setSearchBlacklist(patterns: List<String>) {
        settingsRepository.setWebSearchBlacklist(patterns.toSet())
        _uiState.value = _uiState.value.copy(searchBlacklist = patterns)
    }

    fun setSearchProviderApiKey(providerId: String, apiKey: String) {
        settingsRepository.setWebSearchProviderApiKey(providerId, apiKey)
        val updated = _uiState.value.searchProviderApiKeys.toMutableMap()
        updated[providerId] = apiKey
        _uiState.value = _uiState.value.copy(searchProviderApiKeys = updated)
    }

    fun updateKnowledgeSettings(transform: (KnowledgeSettings) -> KnowledgeSettings) {
        val updated = transform(_uiState.value.knowledgeSettings)
        settingsRepository.setKnowledgeSettings(updated)
        _uiState.value = _uiState.value.copy(knowledgeSettings = updated)
    }

    fun setMcpToolsEnabled(enabled: Boolean) {
        settingsRepository.setMcpToolsEnabled(enabled)
        _uiState.value = _uiState.value.copy(mcpToolsEnabled = enabled)
    }

    fun setGlobalMemoryEnabled(enabled: Boolean) {
        settingsRepository.setGlobalMemoryEnabled(enabled)
        _uiState.value = _uiState.value.copy(globalMemoryEnabled = enabled)
    }

    fun setGlobalMemoryRetentionDays(days: Int) {
        settingsRepository.setGlobalMemoryRetentionDays(days)
        _uiState.value = _uiState.value.copy(globalMemoryRetentionDays = days)
    }

    fun setGlobalMemoryMode(mode: String) {
        settingsRepository.setGlobalMemoryMode(mode)
        _uiState.value = _uiState.value.copy(globalMemoryMode = mode)
    }

    fun clearGlobalMemory() {
        val now = System.currentTimeMillis()
        settingsRepository.setGlobalMemoryLastClearedAt(now)
        _uiState.value = _uiState.value.copy(globalMemoryLastClearedAt = now)
    }

    fun setDebugMode(enabled: Boolean) {
        settingsRepository.setDebugMode(enabled)
        _uiState.value = _uiState.value.copy(debugMode = enabled)
    }

    fun setTokenAnimationFixedDurationEnabled(enabled: Boolean) {
        settingsRepository.setTokenAnimationFixedDurationEnabled(enabled)
        _uiState.value = _uiState.value.copy(tokenAnimationFixedDurationEnabled = enabled)
    }

    fun setTokenAnimationFixedDurationMs(durationMs: Int) {
        val clamped = durationMs.coerceIn(40, 800)
        settingsRepository.setTokenAnimationFixedDurationMs(clamped)
        _uiState.value = _uiState.value.copy(tokenAnimationFixedDurationMs = clamped)
    }

    fun setModelTemperature(provider: String, model: String, temperature: Float) {
        settingsRepository.setModelTemperature(provider, model, temperature)
    }

    fun setModelTopP(provider: String, model: String, topP: Float) {
        settingsRepository.setModelTopP(provider, model, topP)
    }

    fun addCustomServiceProvider(
        name: String,
        apiUrl: String,
        providerType: ProviderType = ProviderType.OpenAI
    ) {
        settingsRepository.addCustomServiceProvider(name, apiUrl, providerType)
    }

    fun removeCustomServiceProvider(name: String) {
        settingsRepository.removeCustomServiceProvider(name)
    }

    fun getCustomProviderType(name: String): ProviderType {
        return settingsRepository.getCustomProviderType(name)
    }

    fun addCustomModelToProvider(
        provider: String,
        modelName: String,
        apiCode: String,
        capabilities: Set<ModelCapability> = setOf(ModelCapability.Chat)
    ) {
        settingsRepository.addCustomModelToProvider(provider, modelName, apiCode, capabilities)
    }

    fun removeCustomModelFromProvider(provider: String, modelName: String) {
        settingsRepository.removeCustomModelFromProvider(provider, modelName)
    }

    fun resetSettingsToDefaults() {
        settingsRepository.resetSettingsToDefaults()
        refreshState()
    }

    class Factory(
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(settingsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
