package com.huajuan.aispace.data.repository.internal

internal class FeatureSettingsStore(
    private val preferenceStore: AppPreferenceStore
) {
    fun getKnowledgeSettings() = preferenceStore.getKnowledgeSettings()

    fun setKnowledgeSettings(settings: com.huajuan.aispace.data.KnowledgeSettings) {
        preferenceStore.setKnowledgeSettings(settings)
    }

    fun getDocumentParseEnabled(): Boolean {
        return preferenceStore.getDocumentParseEnabled()
    }

    fun setDocumentParseEnabled(enabled: Boolean) {
        preferenceStore.setDocumentParseEnabled(enabled)
    }

    fun getDocumentSummaryEnabled(): Boolean {
        return preferenceStore.getDocumentSummaryEnabled()
    }

    fun setDocumentSummaryEnabled(enabled: Boolean) {
        preferenceStore.setDocumentSummaryEnabled(enabled)
    }

    fun getDocumentAutoOcrEnabled(): Boolean {
        return preferenceStore.getDocumentAutoOcrEnabled()
    }

    fun setDocumentAutoOcrEnabled(enabled: Boolean) {
        preferenceStore.setDocumentAutoOcrEnabled(enabled)
    }

    fun getDocumentPreferEpub(): Boolean {
        return preferenceStore.getDocumentPreferEpub()
    }

    fun setDocumentPreferEpub(enabled: Boolean) {
        preferenceStore.setDocumentPreferEpub(enabled)
    }

    fun getDocumentRenderQuality(): String {
        return preferenceStore.getDocumentRenderQuality()
    }

    fun setDocumentRenderQuality(quality: String) {
        preferenceStore.setDocumentRenderQuality(quality)
    }

    fun getDocumentRemoveBackground(): Boolean {
        return preferenceStore.getDocumentRemoveBackground()
    }

    fun setDocumentRemoveBackground(enabled: Boolean) {
        preferenceStore.setDocumentRemoveBackground(enabled)
    }

    fun getSearchEngine(): String {
        return preferenceStore.getSearchEngine()
    }

    fun setSearchEngine(engine: String) {
        preferenceStore.setSearchEngine(engine)
    }

    fun getWebSearchApiKey(): String {
        return preferenceStore.getWebSearchApiKey()
    }

    fun setWebSearchApiKey(apiKey: String) {
        preferenceStore.setWebSearchApiKey(apiKey)
    }

    fun getWebSearchProviderApiKey(providerId: String): String {
        return preferenceStore.getWebSearchProviderApiKey(providerId)
    }

    fun setWebSearchProviderApiKey(providerId: String, apiKey: String) {
        preferenceStore.setWebSearchProviderApiKey(providerId, apiKey)
    }

    fun getWebSearchResultCount(): Int {
        return preferenceStore.getWebSearchResultCount()
    }

    fun setWebSearchResultCount(count: Int) {
        preferenceStore.setWebSearchResultCount(count)
    }

    fun getWebSearchIncludeDate(): Boolean {
        return preferenceStore.getWebSearchIncludeDate()
    }

    fun setWebSearchIncludeDate(enabled: Boolean) {
        preferenceStore.setWebSearchIncludeDate(enabled)
    }

    fun getWebSearchCompression(): String {
        return preferenceStore.getWebSearchCompression()
    }

    fun setWebSearchCompression(mode: String) {
        preferenceStore.setWebSearchCompression(mode)
    }

    fun getWebSearchBlacklist(): Set<String> {
        return preferenceStore.getWebSearchBlacklist()
    }

    fun setWebSearchBlacklist(patterns: Set<String>) {
        preferenceStore.setWebSearchBlacklist(patterns)
    }

    fun getMcpToolsEnabled(): Boolean {
        return preferenceStore.getMcpToolsEnabled()
    }

    fun setMcpToolsEnabled(enabled: Boolean) {
        preferenceStore.setMcpToolsEnabled(enabled)
    }

    fun getMcpPermissionLogEnabled(): Boolean {
        return preferenceStore.getMcpPermissionLogEnabled()
    }

    fun setMcpPermissionLogEnabled(enabled: Boolean) {
        preferenceStore.setMcpPermissionLogEnabled(enabled)
    }

    fun getMcpEnabledTools(): Set<String> {
        return preferenceStore.getMcpEnabledTools()
    }

    fun setMcpEnabledTools(tools: Set<String>) {
        preferenceStore.setMcpEnabledTools(tools)
    }

    fun getMcpServerUrls(): Set<String> {
        return preferenceStore.getMcpServerUrls()
    }

    fun setMcpServerUrls(urls: Set<String>) {
        preferenceStore.setMcpServerUrls(urls)
    }

    fun getGlobalMemoryEnabled(): Boolean {
        return preferenceStore.getGlobalMemoryEnabled()
    }

    fun setGlobalMemoryEnabled(enabled: Boolean) {
        preferenceStore.setGlobalMemoryEnabled(enabled)
    }

    fun getGlobalMemoryRetentionDays(): Int {
        return preferenceStore.getGlobalMemoryRetentionDays()
    }

    fun setGlobalMemoryRetentionDays(days: Int) {
        preferenceStore.setGlobalMemoryRetentionDays(days)
    }

    fun getGlobalMemoryMode(): String {
        return preferenceStore.getGlobalMemoryMode()
    }

    fun setGlobalMemoryMode(mode: String) {
        preferenceStore.setGlobalMemoryMode(mode)
    }

    fun getGlobalMemoryLastClearedAt(): Long {
        return preferenceStore.getGlobalMemoryLastClearedAt()
    }

    fun setGlobalMemoryLastClearedAt(timestamp: Long) {
        preferenceStore.setGlobalMemoryLastClearedAt(timestamp)
    }

    fun resetSettingsToDefaults() {
        preferenceStore.resetToDefaults()
    }
}
