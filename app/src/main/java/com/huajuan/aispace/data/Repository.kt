package com.huajuan.aispace.data

import android.content.Context
import com.huajuan.aispace.R
import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.*
import com.huajuan.aispace.data.repository.internal.AppPreferenceStore
import com.huajuan.aispace.data.repository.internal.AppSettingsStore
import com.huajuan.aispace.data.repository.internal.AssistantProfileStore
import com.huajuan.aispace.data.repository.internal.ConversationLocalStore
import com.huajuan.aispace.data.repository.internal.FeatureSettingsStore
import com.huajuan.aispace.data.repository.internal.ImageAssetStore
import com.huajuan.aispace.data.repository.internal.KnowledgeLocalStore
import com.huajuan.aispace.data.repository.internal.ModelGateway
import com.huajuan.aispace.data.repository.internal.ModelCatalogStore
import com.huajuan.aispace.data.repository.internal.ModelPreferenceStore
import com.huajuan.aispace.data.repository.internal.UserDataCleaner
import com.google.gson.Gson
import com.huajuan.aispace.data.AssistantModelConfig
import com.huajuan.aispace.i18n.LocalizedResources

internal interface WebSearchConfigSource {
    fun getWebSearchResultCount(): Int
    fun getWebSearchIncludeDate(): Boolean
    fun getWebSearchCompression(): String
    fun getWebSearchBlacklist(): Set<String>
    fun getWebSearchProviderApiKey(providerId: String): String
    fun getWebSearchApiKey(): String
    fun getContext(): Context
}

open class Repository(private val context: Context) : WebSearchConfigSource {
    private companion object {
        const val TAG = "Repository"
    }
    // Room数据库实例
    private val database = AppDatabase.getDatabase(context)
    
    // 模型管理器
    private val modelManager = ModelManager(context)
    private val preferenceStore = AppPreferenceStore(context)
    private val gson = Gson()
    private val conversationStore = ConversationLocalStore(database)
    private val imageAssetStore = ImageAssetStore(context, database)
    private val knowledgeStore = KnowledgeLocalStore(this, context, database.knowledgeDao())
    private val userDataCleaner = UserDataCleaner(database, preferenceStore)
    private val modelCatalogStore = ModelCatalogStore(preferenceStore, modelManager)
    private val modelGateway = ModelGateway(this)
    private val agentProvider = AgentProvider(context)
    private val appSettingsStore = AppSettingsStore(preferenceStore)
    private val assistantProfileStore = AssistantProfileStore(context, preferenceStore, agentProvider, gson)
    private val featureSettingsStore = FeatureSettingsStore(preferenceStore)
    private val modelPreferenceStore = ModelPreferenceStore(preferenceStore, modelCatalogStore)
    private val webSearchService = WebSearchService(this)
    private val assistantCatalogVersion = MutableStateFlow(0)
    @Volatile
    private var runtimeWebSearchConfig: RuntimeWebSearchConfig? = null
    @Volatile
    private var runtimeMcpConfig: RuntimeMcpConfig? = null
    private val debugRequestSnapshots = java.util.concurrent.ConcurrentHashMap<String, String>()
    
    /**
     * 获取暗色模式设置
     */
    fun getDarkMode(): Boolean {
        return appSettingsStore.getDarkMode()
    }
    
    /**
     * 设置暗色模式
     */
    fun setDarkMode(isDarkMode: Boolean) {
        appSettingsStore.setDarkMode(isDarkMode)
    }

    fun getThemeMode(): String {
        return appSettingsStore.getThemeMode()
    }

    fun setThemeMode(themeMode: String) {
        appSettingsStore.setThemeMode(themeMode)
    }

    fun getFontScale(): Float {
        return appSettingsStore.getFontScale()
    }

    fun setFontScale(fontScale: Float) {
        appSettingsStore.setFontScale(fontScale)
    }

    fun getTransparencyLevel(): String {
        return appSettingsStore.getTransparencyLevel()
    }

    fun setTransparencyLevel(level: String) {
        appSettingsStore.setTransparencyLevel(level)
    }
    
    /**
     * 获取调试模式设置
     */
    fun getDebugMode(): Boolean {
        return appSettingsStore.getDebugMode()
    }
    
    /**
     * 设置调试模式
     */
    fun setDebugMode(isDebugMode: Boolean) {
        appSettingsStore.setDebugMode(isDebugMode)
    }

    fun getTokenAnimationFixedDurationEnabled(): Boolean {
        return appSettingsStore.getTokenAnimationFixedDurationEnabled()
    }

    fun setTokenAnimationFixedDurationEnabled(enabled: Boolean) {
        appSettingsStore.setTokenAnimationFixedDurationEnabled(enabled)
    }

    fun getTokenAnimationFixedDurationMs(): Int {
        return appSettingsStore.getTokenAnimationFixedDurationMs()
    }

    fun setTokenAnimationFixedDurationMs(durationMs: Int) {
        appSettingsStore.setTokenAnimationFixedDurationMs(durationMs)
    }

    fun getLanguage(): String {
        return appSettingsStore.getLanguage()
    }

    fun setLanguage(language: String) {
        appSettingsStore.setLanguage(language)
    }

    fun getNotificationsEnabled(): Boolean {
        return appSettingsStore.getNotificationsEnabled()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        appSettingsStore.setNotificationsEnabled(enabled)
    }
    
    // 其他方法...
    fun getUseCloudModel(): Boolean {
        return modelPreferenceStore.getUseCloudModel()
    }
    
    fun setUseCloudModel(useCloudModel: Boolean) {
        modelPreferenceStore.setUseCloudModel(useCloudModel)
    }
    
    fun getServiceProvider(): String {
        return modelPreferenceStore.getServiceProvider()
    }
    
    fun setServiceProvider(serviceProvider: String) {
        modelPreferenceStore.setServiceProvider(serviceProvider)
    }
    
    // 获取指定服务商的API密钥
    fun getApiKeyForProvider(provider: String): String {
        return modelPreferenceStore.getApiKeyForProvider(provider)
    }

    fun getProviderType(provider: String): ProviderType {
        val modelDataProvider = ModelDataProvider(this)
        return modelDataProvider.getProviderTypeForProvider(provider)
    }
    
    // 设置指定服务商的API密钥
    fun setApiKeyForProvider(provider: String, apiKey: String) {
        modelPreferenceStore.setApiKeyForProvider(provider, apiKey)
    }
    
    // 获取当前服务商的API密钥
    fun getApiKey(): String {
        return modelPreferenceStore.getApiKey()
    }
    
    // 设置当前服务商的API密钥
    fun setApiKey(apiKey: String) {
        modelPreferenceStore.setApiKey(apiKey)
    }
    
    // 获取指定服务商选中的模型
    fun getSelectedModelForProvider(provider: String): String {
        return modelPreferenceStore.getSelectedModelForProvider(provider)
    }
    
    // 获取当前服务商选中的模型
    fun getSelectedModel(): String {
        return modelPreferenceStore.getSelectedModel()
    }

    fun getCustomApiUrl(): String {
        return appSettingsStore.getCustomApiUrl()
    }
    
    fun setCustomApiUrl(customApiUrl: String) {
        appSettingsStore.setCustomApiUrl(customApiUrl)
    }
    
    // 自定义服务提供商相关方法
    fun getCustomServiceProviders(): Set<String> {
        return modelPreferenceStore.getCustomServiceProviders()
    }
    
    fun addCustomServiceProvider(
        name: String,
        apiUrl: String,
        providerType: ProviderType = ProviderType.OpenAI
    ) {
        modelPreferenceStore.addCustomServiceProvider(name, apiUrl, providerType)
    }
    
    fun removeCustomServiceProvider(name: String) {
        modelPreferenceStore.removeCustomServiceProvider(name)
    }

    fun getCustomProviderType(name: String): ProviderType {
        return modelPreferenceStore.getCustomProviderType(name)
    }
    
    fun getCustomProviderApiUrl(name: String): String {
        return modelPreferenceStore.getCustomProviderApiUrl(name)
    }
    
    // 自定义模型相关方法
    fun getCustomModelsForProvider(providerName: String): Set<String> {
        return modelPreferenceStore.getCustomModelsForProvider(providerName)
    }
    
    fun addCustomModelToProvider(providerName: String, modelName: String, apiCode: String) {
        modelPreferenceStore.addCustomModelToProvider(
            providerName = providerName,
            modelName = modelName,
            apiCode = apiCode,
            capabilities = emptySet()
        )
    }

    fun addCustomModelToProvider(
        providerName: String,
        modelName: String,
        apiCode: String,
        capabilities: Set<ModelCapability>
    ) {
        modelPreferenceStore.addCustomModelToProvider(providerName, modelName, apiCode, capabilities)
    }
    
    fun removeCustomModelFromProvider(providerName: String, modelName: String) {
        modelPreferenceStore.removeCustomModelFromProvider(providerName, modelName)
    }
    
    fun getCustomModelApiCode(providerName: String, modelName: String): String {
        return modelPreferenceStore.getCustomModelApiCode(providerName, modelName)
    }

    fun getCustomModelCapabilities(providerName: String, modelName: String): Set<ModelCapability> {
        return modelPreferenceStore.getCustomModelCapabilities(providerName, modelName)
    }

    fun getImageGenerationProbeRecord(providerName: String, modelApiCode: String): ImageGenerationProbeRecord? {
        return modelPreferenceStore.getImageGenerationProbeRecord(providerName, modelApiCode)
    }

    fun setImageGenerationProbeRecord(providerName: String, modelApiCode: String, record: ImageGenerationProbeRecord) {
        modelPreferenceStore.setImageGenerationProbeRecord(providerName, modelApiCode, record)
    }

    fun clearImageGenerationProbeRecord(providerName: String, modelApiCode: String) {
        modelPreferenceStore.clearImageGenerationProbeRecord(providerName, modelApiCode)
    }

    fun getImageGenerationSupport(assistantId: String): ImageGenerationSupport {
        val resolved = ModelRequestResolver.resolve(this, assistantId)
        return ImageGenerationApiService(this, assistantId).getSupport(resolved.modelInfo)
    }

    suspend fun probeImageGenerationSupport(assistantId: String): ImageGenerationSupport {
        val resolved = ModelRequestResolver.resolve(this, assistantId)
        return ImageGenerationApiService(this, assistantId).probeImageGenerationSupport(resolved.modelInfo)
    }

    suspend fun generateImage(
        assistantId: String,
        conversationId: String,
        request: ImageGenerationRequest
    ): ImageGenerationResult {
        val resolved = ModelRequestResolver.resolve(this, assistantId)
        val service = ImageGenerationApiService(this, assistantId)
        val payload = service.generateImagePayload(request, resolved.modelInfo)
        if (ModelErrorParser.isErrorMessage(payload.displayText)) {
            return ImageGenerationResult(
                imageIds = emptyList(),
                displayText = payload.displayText,
                appliedRequest = request
            )
        }
        val support = service.getSupport(resolved.modelInfo)
        val applied = request.copy(count = request.count.coerceIn(1, support.maxCount.coerceAtLeast(1)))
        val stored = imageAssetStore.saveImageGenerationPayload(payload)
        updateImageGenerationConversationState(
            conversationId,
            ImageGenerationConversationState(
                request = applied,
                providerId = resolved.serviceProvider,
                modelDisplayName = resolved.selectedModelDisplayName,
                modelApiCode = resolved.modelInfo.apiCode
            )
        )
        return ImageGenerationResult(
            imageIds = stored.first,
            displayText = stored.second,
            appliedRequest = applied
        )
    }

    suspend fun fetchRemoteModels(providerName: String): List<ModelInfo> {
        return RemoteModelCatalogService(this).fetchModels(providerName)
    }

    fun getDocumentParseEnabled(): Boolean {
        return featureSettingsStore.getDocumentParseEnabled()
    }

    fun getKnowledgeSettings(): KnowledgeSettings {
        return featureSettingsStore.getKnowledgeSettings()
    }

    fun setKnowledgeSettings(settings: KnowledgeSettings) {
        featureSettingsStore.setKnowledgeSettings(settings)
    }

    fun setDocumentParseEnabled(enabled: Boolean) {
        featureSettingsStore.setDocumentParseEnabled(enabled)
    }

    fun getDocumentSummaryEnabled(): Boolean {
        return featureSettingsStore.getDocumentSummaryEnabled()
    }

    fun setDocumentSummaryEnabled(enabled: Boolean) {
        featureSettingsStore.setDocumentSummaryEnabled(enabled)
    }

    fun getDocumentAutoOcrEnabled(): Boolean {
        return featureSettingsStore.getDocumentAutoOcrEnabled()
    }

    fun setDocumentAutoOcrEnabled(enabled: Boolean) {
        featureSettingsStore.setDocumentAutoOcrEnabled(enabled)
    }

    fun getDocumentPreferEpub(): Boolean {
        return featureSettingsStore.getDocumentPreferEpub()
    }

    fun setDocumentPreferEpub(enabled: Boolean) {
        featureSettingsStore.setDocumentPreferEpub(enabled)
    }

    fun getDocumentRenderQuality(): String {
        return featureSettingsStore.getDocumentRenderQuality()
    }

    fun setDocumentRenderQuality(quality: String) {
        featureSettingsStore.setDocumentRenderQuality(quality)
    }

    fun getDocumentRemoveBackground(): Boolean {
        return featureSettingsStore.getDocumentRemoveBackground()
    }

    fun setDocumentRemoveBackground(enabled: Boolean) {
        featureSettingsStore.setDocumentRemoveBackground(enabled)
    }

    fun getSearchEngine(): String {
        return featureSettingsStore.getSearchEngine()
    }

    fun setSearchEngine(engine: String) {
        featureSettingsStore.setSearchEngine(WebSearchProviders.normalizeSelection(engine))
    }

    override fun getWebSearchApiKey(): String {
        return featureSettingsStore.getWebSearchApiKey()
    }

    fun setWebSearchApiKey(apiKey: String) {
        featureSettingsStore.setWebSearchApiKey(apiKey)
    }

    override fun getWebSearchProviderApiKey(providerId: String): String {
        return featureSettingsStore.getWebSearchProviderApiKey(providerId)
    }

    fun setWebSearchProviderApiKey(providerId: String, apiKey: String) {
        featureSettingsStore.setWebSearchProviderApiKey(providerId, apiKey)
    }

    override fun getWebSearchResultCount(): Int {
        return featureSettingsStore.getWebSearchResultCount()
    }

    fun setWebSearchResultCount(count: Int) {
        featureSettingsStore.setWebSearchResultCount(count)
    }

    override fun getWebSearchIncludeDate(): Boolean {
        return featureSettingsStore.getWebSearchIncludeDate()
    }

    fun setWebSearchIncludeDate(enabled: Boolean) {
        featureSettingsStore.setWebSearchIncludeDate(enabled)
    }

    override fun getWebSearchCompression(): String {
        return featureSettingsStore.getWebSearchCompression()
    }

    fun setWebSearchCompression(mode: String) {
        featureSettingsStore.setWebSearchCompression(mode)
    }

    override fun getWebSearchBlacklist(): Set<String> {
        return featureSettingsStore.getWebSearchBlacklist()
    }

    fun setWebSearchBlacklist(patterns: Set<String>) {
        featureSettingsStore.setWebSearchBlacklist(patterns)
    }

    fun getConversationWebSearchEngine(conversationId: String, fallback: String): String {
        return preferenceStore.getConversationWebSearchEngine(
            conversationId,
            WebSearchProviders.normalizeSelection(fallback)
        )
    }

    fun setConversationWebSearchEngine(conversationId: String, engine: String) {
        preferenceStore.setConversationWebSearchEngine(
            conversationId,
            WebSearchProviders.normalizeSelection(engine)
        )
    }

    fun setRuntimeWebSearchConfig(config: RuntimeWebSearchConfig?) {
        runtimeWebSearchConfig = config
    }

    fun getRuntimeWebSearchConfig(): RuntimeWebSearchConfig? = runtimeWebSearchConfig

    fun setRuntimeMcpConfig(config: RuntimeMcpConfig?) {
        runtimeMcpConfig = config
    }

    fun getRuntimeMcpConfig(): RuntimeMcpConfig? = runtimeMcpConfig

    fun beginDebugRequestCapture(scopeId: String?) {
        if (scopeId.isNullOrBlank()) return
        debugRequestSnapshots.remove(scopeId)
    }

    fun captureModelRequestDebugSnapshot(scopeId: String?, snapshot: String) {
        if (scopeId.isNullOrBlank()) return
        debugRequestSnapshots[scopeId] = snapshot
    }

    fun consumeDebugRequestSnapshot(scopeId: String?): String {
        if (scopeId.isNullOrBlank()) return ""
        return debugRequestSnapshots.remove(scopeId).orEmpty()
    }

    fun endDebugRequestCapture(scopeId: String?) {
        if (scopeId.isNullOrBlank()) return
        debugRequestSnapshots.remove(scopeId)
    }

    suspend fun buildWebSearchContext(
        query: String,
        providerId: String,
        startIndex: Int = 1
    ): WebSearchContextPayload = webSearchService.searchAndBuildContext(query, providerId, startIndex)

    fun resolveWebSearchPlan(
        assistantId: String = getCurrentAssistantId(),
        runtimeConfig: RuntimeWebSearchConfig? = getRuntimeWebSearchConfig()
    ): WebSearchPlan {
        return WebSearchPlanner.plan(
            WebSearchPlannerInputs(
                runtimeConfig = runtimeConfig,
                supportsModelNative = supportsBuiltinWebSearch(assistantId)
            )
        )
    }

    fun supportsBuiltinWebSearch(assistantId: String = getCurrentAssistantId()): Boolean {
        val resolved = ModelRequestResolver.resolve(this, assistantId)
        return when (resolved.providerType) {
            ProviderType.Gemini, ProviderType.Anthropic -> true
            ProviderType.OpenAI, ProviderType.AzureOpenAI -> {
                val haystack = "${resolved.modelInfo.displayName} ${resolved.modelInfo.apiCode}".lowercase()
                val hints = listOf("search", "web", "sonar", "perplexity", "grok", "grounding")
                hints.any { haystack.contains(it) }
            }
        }
    }

    fun getMcpToolsEnabled(): Boolean {
        return featureSettingsStore.getMcpToolsEnabled()
    }

    fun setMcpToolsEnabled(enabled: Boolean) {
        featureSettingsStore.setMcpToolsEnabled(enabled)
    }

    fun getMcpPermissionLogEnabled(): Boolean {
        return featureSettingsStore.getMcpPermissionLogEnabled()
    }

    fun setMcpPermissionLogEnabled(enabled: Boolean) {
        featureSettingsStore.setMcpPermissionLogEnabled(enabled)
    }

    fun getMcpEnabledTools(): Set<String> {
        return featureSettingsStore.getMcpEnabledTools()
    }

    fun setMcpEnabledTools(tools: Set<String>) {
        featureSettingsStore.setMcpEnabledTools(tools)
    }

    fun getMcpServerUrls(): Set<String> {
        return featureSettingsStore.getMcpServerUrls()
    }

    fun setMcpServerUrls(urls: Set<String>) {
        featureSettingsStore.setMcpServerUrls(urls)
    }

    fun getGlobalMemoryEnabled(): Boolean {
        return featureSettingsStore.getGlobalMemoryEnabled()
    }

    fun setGlobalMemoryEnabled(enabled: Boolean) {
        featureSettingsStore.setGlobalMemoryEnabled(enabled)
    }

    fun getGlobalMemoryRetentionDays(): Int {
        return featureSettingsStore.getGlobalMemoryRetentionDays()
    }

    fun setGlobalMemoryRetentionDays(days: Int) {
        featureSettingsStore.setGlobalMemoryRetentionDays(days)
    }

    fun getGlobalMemoryMode(): String {
        return featureSettingsStore.getGlobalMemoryMode()
    }

    fun setGlobalMemoryMode(mode: String) {
        featureSettingsStore.setGlobalMemoryMode(mode)
    }

    fun getGlobalMemoryLastClearedAt(): Long {
        return featureSettingsStore.getGlobalMemoryLastClearedAt()
    }

    fun setGlobalMemoryLastClearedAt(timestamp: Long) {
        featureSettingsStore.setGlobalMemoryLastClearedAt(timestamp)
    }

    fun getCurrentAssistantId(): String {
        return assistantProfileStore.getCurrentAssistantId()
    }

    fun setCurrentAssistantId(assistantId: String) {
        assistantProfileStore.setCurrentAssistantId(assistantId)
    }

    fun getAssistantCatalogVersionFlow(): StateFlow<Int> =
        assistantCatalogVersion.asStateFlow()

    fun getStarredAssistantIds(): Set<String> =
        assistantProfileStore.getStarredAssistantIds()

    fun setStarredAssistantIds(ids: Set<String>) {
        assistantProfileStore.setStarredAssistantIds(ids)
        assistantCatalogVersion.value += 1
    }

    fun getStarredServiceProviders(): Set<String> =
        assistantProfileStore.getStarredServiceProviders()

    fun setStarredServiceProviders(providers: Set<String>) {
        assistantProfileStore.setStarredServiceProviders(providers)
    }

    fun getServiceProviderEnabled(provider: String): Boolean =
        assistantProfileStore.getServiceProviderEnabled(provider)

    fun setServiceProviderEnabled(provider: String, enabled: Boolean) {
        assistantProfileStore.setServiceProviderEnabled(provider, enabled)
    }

    fun getCustomAssistants(): List<Agent> {
        return assistantProfileStore.getCustomAssistants()
    }

    fun setCustomAssistants(list: List<Agent>) {
        assistantProfileStore.setCustomAssistants(list)
        assistantCatalogVersion.value += 1
    }

    fun deleteCustomAssistant(assistantId: String): String? {
        if (!assistantId.startsWith("custom_")) return null

        val customAssistants = assistantProfileStore.getCustomAssistants()
        if (customAssistants.none { it.id == assistantId }) return null

        assistantProfileStore.setCustomAssistants(
            customAssistants.filterNot { it.id == assistantId }
        )
        assistantProfileStore.setStarredAssistantIds(
            assistantProfileStore.getStarredAssistantIds() - assistantId
        )
        preferenceStore.clearAssistantScopedState(assistantId)

        val currentAssistantId = assistantProfileStore.getCurrentAssistantId()
        val fallbackAssistantId = if (currentAssistantId == assistantId) {
            agentProvider.getAgents().firstOrNull()?.id ?: "default"
        } else {
            null
        }
        if (fallbackAssistantId != null) {
            assistantProfileStore.setCurrentAssistantId(fallbackAssistantId)
        }

        assistantCatalogVersion.value += 1
        return fallbackAssistantId
    }

    fun getAssistantById(assistantId: String): Agent? {
        return assistantProfileStore.getAssistantById(assistantId)
    }

    fun getAssistantModelConfig(assistantId: String): AssistantModelConfig {
        return preferenceStore.getAssistantModelConfig(
            assistantId = assistantId,
            fallbackServiceProvider = getServiceProvider(),
            fallbackSelectedModel = getSelectedModel(),
            fallbackUseCloud = getUseCloudModel(),
            fallbackLocalSelected = getLocalSelectedModel(assistantId)
        )
    }

    fun setAssistantUseCloudModel(assistantId: String, enabled: Boolean) {
        preferenceStore.setAssistantUseCloudModel(assistantId, enabled)
    }

    fun setAssistantServiceProvider(assistantId: String, provider: String) {
        preferenceStore.setAssistantServiceProvider(assistantId, provider)
    }

    fun setAssistantSelectedModel(assistantId: String, modelName: String) {
        preferenceStore.setAssistantSelectedModel(assistantId, modelName)
    }

    fun setAssistantLocalSelectedModel(assistantId: String, modelName: String) {
        preferenceStore.setAssistantLocalSelectedModel(assistantId, modelName)
    }

    fun getAssistantReasoningEnabled(assistantId: String = getCurrentAssistantId()): Boolean {
        val fallback = assistantId != ToolIds.Translation
        return preferenceStore.getAssistantReasoningEnabled(assistantId, fallback)
    }

    fun setAssistantReasoningEnabled(assistantId: String, enabled: Boolean) {
        preferenceStore.setAssistantReasoningEnabled(assistantId, enabled)
    }

    fun getAssistantRequestSettings(assistantId: String = getCurrentAssistantId()): AssistantRequestSettings {
        return AssistantRequestSettings(
            temperature = preferenceStore.getAssistantTemperature(assistantId, 0.7f),
            topP = preferenceStore.getAssistantTopP(assistantId, 0.9f),
            enableTemperature = true,
            enableTopP = true,
            contextMessageCount = preferenceStore.getAssistantContextLimit(assistantId, 12),
            maxTokensEnabled = preferenceStore.getAssistantMaxTokensEnabled(assistantId, false),
            maxTokens = preferenceStore.getAssistantMaxTokens(assistantId, 1024),
            reasoningEffort = null,
            streamEnabled = preferenceStore.getAssistantStreamEnabled(assistantId, true),
            toolCallMode = preferenceStore.getAssistantToolCallMode(assistantId, "function"),
            maxToolSteps = 20,
            customParams = preferenceStore.getAssistantCustomParams(assistantId, "")
        )
    }

    fun setAssistantRequestSettings(assistantId: String, settings: AssistantRequestSettings) {
        preferenceStore.setAssistantTemperature(assistantId, settings.temperature)
        preferenceStore.setAssistantTopP(assistantId, settings.topP)
        preferenceStore.setAssistantContextLimit(assistantId, settings.contextMessageCount)
        preferenceStore.setAssistantMaxTokensEnabled(assistantId, settings.maxTokensEnabled)
        preferenceStore.setAssistantMaxTokens(assistantId, settings.maxTokens)
        preferenceStore.setAssistantStreamEnabled(assistantId, settings.streamEnabled)
        preferenceStore.setAssistantToolCallMode(assistantId, settings.toolCallMode)
        preferenceStore.setAssistantCustomParams(assistantId, settings.customParams)
    }

    fun resetAssistantRequestSettings(assistantId: String) {
        preferenceStore.clearAssistantRequestSettings(assistantId)
    }

    fun getAssistantName(assistantId: String): String {
        return assistantProfileStore.getAssistantName(assistantId)
    }

    fun getAssistantEmoji(assistantId: String): String {
        return assistantProfileStore.getAssistantEmoji(assistantId)
    }

    fun getAssistantSystemPrompt(assistantId: String): String {
        return assistantProfileStore.getAssistantSystemPrompt(assistantId)
    }

    fun getCurrentDefaultConversationTitle(): String =
        LocalizedResources.getString(context, R.string.conversation_new)

    fun getDefaultAssistantName(): String =
        LocalizedResources.getString(context, R.string.default_assistant_name)

    fun getDefaultAssistantPrompt(): String =
        LocalizedResources.getString(context, R.string.default_assistant_prompt)

    fun updateAssistantPromptSettings(
        assistantId: String,
        name: String,
        emoji: String,
        systemPrompt: String
    ) {
        assistantProfileStore.updateAssistantPromptSettings(
            assistantId = assistantId,
            name = name,
            emoji = emoji,
            systemPrompt = systemPrompt
        )
    }

    fun clearAssistantPromptSettings(assistantId: String) {
        assistantProfileStore.clearAssistantPromptSettings(assistantId)
    }

    fun getAssistantCommonPhrases(assistantId: String): List<String> {
        return assistantProfileStore.getAssistantCommonPhrases(assistantId)
    }

    fun setAssistantCommonPhrases(assistantId: String, phrases: List<String>) {
        assistantProfileStore.setAssistantCommonPhrases(assistantId, phrases)
    }
    
    fun getConversationsFlow(assistantId: String): Flow<List<ConversationEntity>> {
        return conversationStore.getConversationsFlow(assistantId)
    }
    
    @WorkerThread
    @Deprecated("Use getConversationsAsync() to avoid main-thread database access.")
    fun getConversations(assistantId: String): List<Conversation> {
        return conversationStore.getConversations(assistantId)
    }

    suspend fun getConversationsAsync(assistantId: String): List<Conversation> =
        conversationStore.getConversationsAsync(assistantId)
    
    fun getMessagesFlow(conversationId: String): Flow<List<MessageEntity>> {
        return conversationStore.getMessagesFlow(conversationId)
    }
    
    @WorkerThread
    @Deprecated("Use getMessagesAsync() to avoid main-thread database access.")
    fun getMessages(conversationId: String, assistantId: String): List<Message> {
        return conversationStore.getMessages(conversationId, assistantId)
    }

    suspend fun getMessagesAsync(conversationId: String, assistantId: String): List<Message> =
        conversationStore.getMessagesAsync(conversationId, assistantId)

    suspend fun searchMessagesAsync(conversationId: String, assistantId: String, query: String): List<Message> =
        conversationStore.searchMessagesAsync(conversationId, assistantId, query)

    suspend fun updateMessageShowThinkAsync(messageId: String, showThink: Boolean) =
        conversationStore.updateMessageShowThinkAsync(messageId, showThink)

    suspend fun searchAllMessagesAsync(assistantId: String, query: String, limit: Int = 100): List<MessageSearchResult> =
        conversationStore.searchAllMessagesAsync(assistantId, query, limit)

    suspend fun searchAllMessagesAsync(query: String, limit: Int = 100): List<MessageSearchResult> =
        conversationStore.searchAllMessagesAsync(query, limit)
    
    suspend fun saveMessages(conversationId: String, assistantId: String, messages: List<Message>) {
        conversationStore.saveMessages(conversationId, assistantId, messages)
    }
    
    suspend fun createNewConversation(
        assistantId: String,
        title: String,
        roleName: String = "",
        systemPrompt: String = ""
    ): Conversation {
        val resolvedRoleName = roleName.ifBlank { getDefaultAssistantName() }
        val resolvedSystemPrompt = systemPrompt.ifBlank { getDefaultAssistantPrompt() }
        return conversationStore.createNewConversation(
            assistantId = assistantId,
            title = title,
            roleName = resolvedRoleName,
            systemPrompt = resolvedSystemPrompt
        )
    }
    
    suspend fun deleteConversation(conversationId: String) {
        conversationStore.deleteConversation(conversationId)
    }
    
    suspend fun updateLastMessage(conversationId: String, lastMessage: String) {
        conversationStore.updateLastMessage(conversationId, lastMessage)
    }
    
    /**
     * 更新对话标题
     */
    suspend fun updateConversationTitle(conversationId: String, title: String) {
        conversationStore.updateConversationTitle(conversationId, title)
    }

    suspend fun getImageGenerationConversationState(conversationId: String): ImageGenerationConversationState? {
        return conversationStore.getImageGenerationState(conversationId)
    }

    suspend fun updateImageGenerationConversationState(
        conversationId: String,
        state: ImageGenerationConversationState?
    ) {
        conversationStore.updateImageGenerationState(conversationId, state)
    }
    
    /**
     * 获取对话的角色名称
     */
    @WorkerThread
    @Deprecated("Use getConversationRoleNameAsync() to avoid main-thread database access.")
    fun getConversationRoleName(conversationId: String): String {
        return conversationStore.getConversationRoleName(conversationId)
    }

    suspend fun getConversationRoleNameAsync(conversationId: String): String =
        resolveLocalizedConversationRole(
            conversationStore.getConversationEntityAsync(conversationId)
        )
    
    /**
     * 获取对话的系统提示词
     */
    @WorkerThread
    @Deprecated("Use getConversationSystemPromptAsync() to avoid main-thread database access.")
    fun getConversationSystemPrompt(conversationId: String): String {
        return conversationStore.getConversationSystemPrompt(conversationId)
    }

    suspend fun getConversationSystemPromptAsync(conversationId: String): String =
        resolveLocalizedConversationSystemPrompt(
            conversationStore.getConversationEntityAsync(conversationId)
        )
    
    /**
     * 更新对话的角色名称和系统提示词
     */
    suspend fun updateConversationRole(conversationId: String, roleName: String, systemPrompt: String) {
        conversationStore.updateConversationRole(conversationId, roleName, systemPrompt)
    }
    
    // 获取基础URL
    fun getBaseUrl(): String {
        return modelGateway.getBaseUrl()
    }

    // 获取AI响应
    suspend fun getAIResponse(
        messages: List<Message>,
        conversationId: String,
        debugScopeId: String? = null
    ): String {
        return modelGateway.getAIResponse(messages, conversationId, debugScopeId)
    }

    // 兼容性的流式输出包装：如果后端/本地模型不支持原生流式API，
    // 我们仍然可以将完整回复按词或固定大小分块后以Flow的方式逐步发出，
    // 从而为UI提供平滑的流式渲染体验。
    fun streamAIResponse(
        messages: List<Message>,
        conversationId: String,
        debugScopeId: String? = null
    ): Flow<com.huajuan.aispace.data.model.ChatEvent> {
        return modelGateway.streamAIResponse(messages, conversationId, debugScopeId = debugScopeId)
    }

    fun streamAIResponseWithSystemPrompt(
        messages: List<Message>,
        conversationId: String,
        systemPromptOverride: String,
        debugScopeId: String? = null
    ): Flow<com.huajuan.aispace.data.model.ChatEvent> {
        return modelGateway.streamAIResponse(messages, conversationId, systemPromptOverride, debugScopeId)
    }

    suspend fun buildModelInputTextSnapshot(
        messages: List<Message>,
        conversationId: String,
        systemPromptOverride: String? = null
    ): String {
        return modelGateway.buildModelInputTextSnapshot(messages, conversationId, systemPromptOverride)
    }

    // 新增：清除数据库中除API和密钥相关以外的所有用户数据（须在debug或受保护路径调用）
    suspend fun clearUserDataExceptApiKeys() {
        userDataCleaner.clearUserDataExceptApiKeys()
    }

    fun getLocalSelectedModel(assistantId: String = getCurrentAssistantId()): String {
        return modelPreferenceStore.getLocalSelectedModel(assistantId)
    }

    fun setLocalSelectedModel(model: String, assistantId: String = getCurrentAssistantId()) {
        modelPreferenceStore.setLocalSelectedModel(model, assistantId)
    }

    /**
     * 获取本地模型路径
     */
    fun getLocalModelPath(modelName: String): String {
        return modelPreferenceStore.getLocalModelPath(modelName)
    }

    /**
     * 获取本地模型列表
     */
    fun getLocalModelList(): List<ModelInfo> {
        return modelPreferenceStore.getLocalModelList()
    }

    fun getModelTemperature(provider: String, model: String): Float {
        return modelPreferenceStore.getModelTemperature(provider, model)
    }

    fun setModelTemperature(provider: String, model: String, temperature: Float) {
        modelPreferenceStore.setModelTemperature(provider, model, temperature)
    }

    fun getModelTopP(provider: String, model: String): Float {
        return modelPreferenceStore.getModelTopP(provider, model)
    }

    fun setModelTopP(provider: String, model: String, topP: Float) {
        modelPreferenceStore.setModelTopP(provider, model, topP)
    }

    fun resetSettingsToDefaults() {
        featureSettingsStore.resetSettingsToDefaults()
    }

    override fun getContext(): Context {
        return context
    }

    suspend fun repairAssistantBindings() {
        val agents = agentProvider.getAgents() + getCustomAssistants()
        val fallback = agents.firstOrNull()?.id ?: "default"
        conversationStore.repairAssistantBindings(
            agents = agents,
            fallbackAssistantId = fallback,
            reservedAssistantIds = ToolIds.toolConversationIds
        )
    }

    suspend fun processAIImageResponse(responseText: String): Pair<List<String>, String> {
        return imageAssetStore.processAIImageResponse(responseText)
    }

    /**
     * 从数据库获取图片数据
     * @param imageId 图片ID
     * @return 图片字节数据，不存在返回null
     */
    suspend fun getImageData(imageId: String): ByteArray? {
        return imageAssetStore.getImageData(imageId)
    }

    /**
     * 从数据库获取图片并转换为可用于显示的URI
     * 使用 content:// 协议或临时文件路径
     */
    suspend fun getImageUri(imageId: String): String? {
        return imageAssetStore.getImageUri(imageId)
    }

    /**
     * 批量获取图片URI
     */
    suspend fun getImageUris(imageIds: List<String>): List<String> {
        return imageAssetStore.getImageUris(imageIds)
    }

    fun extractImageUrlsFromResponse(responseText: String): List<String> {
        return imageAssetStore.extractImageUrlsFromResponse(responseText)
    }

    fun extractAndCleanImageUrls(responseText: String): Pair<List<String>, String> {
        return imageAssetStore.extractAndCleanImageUrls(responseText)
    }

    suspend fun deleteImageById(imageId: String): Boolean {
        return imageAssetStore.deleteImageById(imageId)
    }

    suspend fun removeAttachmentFromMessage(
        assistantId: String,
        conversationId: String,
        messageId: String,
        attachmentId: String? = null,
        imageUri: String? = null
    ): Boolean {
        val messages = getMessagesAsync(conversationId, assistantId)
        val updated = messages.map { message ->
            if (message.id != messageId) return@map message
            val newAttachments = if (attachmentId != null) {
                message.attachments.filterNot { it.id == attachmentId }
            } else {
                message.attachments
            }
            val newImageUris = if (imageUri != null) {
                message.imageUris.filterNot { it == imageUri }
            } else {
                message.imageUris
            }
            message.copy(attachments = newAttachments, imageUris = newImageUris)
        }
        if (updated == messages) return false
        saveMessages(conversationId, assistantId, updated)
        return true
    }

    suspend fun listKnowledgeBases(): List<KnowledgeBaseDto> =
        knowledgeStore.listKnowledgeBases()

    suspend fun createKnowledgeBase(
        name: String,
        description: String,
        embeddingModelRef: String
    ): KnowledgeBaseDto =
        knowledgeStore.createKnowledgeBase(name, description, embeddingModelRef)

    suspend fun updateKnowledgeBaseEmbedding(kbId: String, embeddingModelRef: String): KnowledgeBaseDto? =
        knowledgeStore.updateKnowledgeBaseEmbedding(kbId, embeddingModelRef)

    suspend fun getKnowledgeBase(kbId: String): KnowledgeBaseDto? =
        knowledgeStore.getKnowledgeBase(kbId)

    suspend fun updateKnowledgeBase(
        kbId: String,
        name: String,
        description: String,
        embeddingModelRef: String
    ): KnowledgeBaseDto? =
        knowledgeStore.updateKnowledgeBase(kbId, name, description, embeddingModelRef)

    suspend fun deleteKnowledgeBase(kbId: String) =
        knowledgeStore.deleteKnowledgeBase(kbId)

    suspend fun listKnowledgeItems(
        kbId: String,
        category: KnowledgeCategory?,
        keyword: String?
    ): List<KnowledgeItemDto> =
        knowledgeStore.listKnowledgeItems(kbId, category, keyword)

    suspend fun upsertKnowledgeItem(input: KnowledgeItemUpsert): KnowledgeItemDto =
        knowledgeStore.upsertKnowledgeItem(input)

    suspend fun removeKnowledgeItem(itemId: String) =
        knowledgeStore.removeKnowledgeItem(itemId)

    suspend fun rebuildKnowledgeItemIndex(itemId: String): IndexBuildResult =
        knowledgeStore.rebuildKnowledgeItemIndex(itemId)

    suspend fun fetchWebsiteAndIndex(kbId: String, url: String): IndexBuildResult =
        knowledgeStore.fetchWebsiteAndIndex(kbId, url)

    suspend fun searchKnowledgeChunks(
        kbIds: List<String>,
        query: String,
        topK: Int = 8
    ): List<KnowledgeChunkHit> =
        knowledgeStore.searchKnowledgeChunks(kbIds, query, topK)

    suspend fun getKnowledgeBaseStats(kbId: String): Map<String, Int> =
        knowledgeStore.getKnowledgeBaseStats(kbId)

    suspend fun getConversationKbSelection(conversationId: String): List<String> =
        knowledgeStore.getConversationKbSelection(conversationId)

    suspend fun setConversationKbSelection(conversationId: String, kbIds: List<String>) =
        knowledgeStore.setConversationKbSelection(conversationId, kbIds)

    fun getAssistantDefaultKbSelection(assistantId: String): List<String> =
        preferenceStore.getAssistantDefaultKbSelection(assistantId)

    fun setAssistantDefaultKbSelection(assistantId: String, kbIds: List<String>) =
        preferenceStore.setAssistantDefaultKbSelection(assistantId, kbIds)

    fun getKnowledgeIndexJobsFlow(): StateFlow<List<KnowledgeIndexJobDto>> =
        knowledgeStore.getKnowledgeIndexJobsFlow()

    suspend fun buildKnowledgeContext(
        kbIds: List<String>,
        query: String,
        startIndex: Int = 1
    ): KnowledgeContextPayload {
        val settings = getKnowledgeSettings()
        val hits = searchKnowledgeChunks(kbIds, query, topK = settings.searchTopK)
        if (hits.isEmpty()) return KnowledgeContextPayload("", emptyList())
        val top = hits.take(settings.searchContextLimit.coerceAtLeast(1))
        val sb = StringBuilder()
        val citations = mutableListOf<WebCitation>()
        sb.append(LocalizedResources.getString(context, R.string.knowledge_context_intro))
        sb.append('\n')
        top.forEachIndexed { index, hit ->
            if (sb.length >= settings.searchContextMaxChars) return@forEachIndexed
            val citationIndex = startIndex + index
            citations += WebCitation(
                index = citationIndex,
                title = hit.itemTitle.ifBlank { hit.sourceUri },
                url = hit.sourceUri,
                snippet = hit.chunkText.take(200),
                sourceType = "knowledge"
            )
            sb.append(
                LocalizedResources.getString(
                    context,
                    R.string.knowledge_context_snippet,
                    null,
                    citationIndex,
                    hit.itemTitle
                )
            )
            sb.append('\n')
            sb.append(hit.chunkText.take(settings.searchContextMaxChars))
            sb.append("\n\n")
        }
        return KnowledgeContextPayload(
            contextText = sb.toString().take(settings.searchContextMaxChars).trim(),
            citations = citations
        )
    }

    private fun resolveLocalizedConversationRole(entity: ConversationEntity?): String {
        if (entity == null) return getDefaultAssistantName()
        val builtInDefaults = assistantProfileStore.getBuiltInDefaultValues(entity.assistantId)
        if (builtInDefaults.isEmpty()) {
            return entity.roleName.ifBlank { getDefaultAssistantName() }
        }
        val matchedDefault = builtInDefaults.any { it.name == entity.roleName }
        return if (matchedDefault || entity.roleName.isBlank()) {
            getAssistantName(entity.assistantId)
        } else {
            entity.roleName
        }
    }

    private fun resolveLocalizedConversationSystemPrompt(entity: ConversationEntity?): String {
        if (entity == null) return getDefaultAssistantPrompt()
        val builtInDefaults = assistantProfileStore.getBuiltInDefaultValues(entity.assistantId)
        if (builtInDefaults.isEmpty()) {
            return entity.systemPrompt.ifBlank { getDefaultAssistantPrompt() }
        }
        val matchedDefault = builtInDefaults.any { it.systemPrompt == entity.systemPrompt }
        return if (matchedDefault || entity.systemPrompt.isBlank()) {
            getAssistantSystemPrompt(entity.assistantId)
        } else {
            entity.systemPrompt
        }
    }

}
