package com.huajuan.aispace.data.repository.internal

import android.content.Context
import com.huajuan.aispace.data.BuiltInServiceProviders
import com.huajuan.aispace.data.ImageGenerationProbeRecord
import com.huajuan.aispace.data.KnowledgeSettings
import com.huajuan.aispace.data.ModelCapability
import com.huajuan.aispace.data.ProviderType
import com.huajuan.aispace.data.WebSearchProviders
import com.huajuan.aispace.i18n.AppLocaleManager
import com.google.gson.Gson

internal class AppPreferenceStore(
    context: Context
) {
    private companion object {
        const val DARK_MODE_KEY = "dark_mode"
        const val THEME_MODE_KEY = "theme_mode"
        const val FONT_SCALE_KEY = "font_scale"
        const val TRANSPARENCY_LEVEL_KEY = "transparency_level"
        const val DEBUG_MODE_KEY = "debug_mode"
        const val TOKEN_ANIMATION_FIXED_DURATION_ENABLED_KEY = "token_animation_fixed_duration_enabled"
        const val TOKEN_ANIMATION_FIXED_DURATION_MS_KEY = "token_animation_fixed_duration_ms"
        const val CURRENT_ASSISTANT_ID_KEY = "current_assistant_id"
        const val ASSISTANT_USE_CLOUD_PREFIX = "assistant_use_cloud_"
        const val ASSISTANT_PROVIDER_PREFIX = "assistant_provider_"
        const val ASSISTANT_SELECTED_MODEL_PREFIX = "assistant_selected_model_"
        const val ASSISTANT_LOCAL_MODEL_PREFIX = "assistant_local_model_"
        const val ASSISTANT_NAME_PREFIX = "assistant_name_"
        const val ASSISTANT_EMOJI_PREFIX = "assistant_emoji_"
        const val ASSISTANT_SYSTEM_PROMPT_PREFIX = "assistant_system_prompt_"
        const val ASSISTANT_TEMPERATURE_PREFIX = "assistant_temperature_"
        const val ASSISTANT_TOP_P_PREFIX = "assistant_top_p_"
        const val ASSISTANT_CONTEXT_LIMIT_PREFIX = "assistant_context_limit_"
        const val ASSISTANT_MAX_TOKENS_ENABLED_PREFIX = "assistant_max_tokens_enabled_"
        const val ASSISTANT_MAX_TOKENS_PREFIX = "assistant_max_tokens_"
        const val ASSISTANT_STREAM_ENABLED_PREFIX = "assistant_stream_enabled_"
        const val ASSISTANT_TOOL_CALL_MODE_PREFIX = "assistant_tool_call_mode_"
        const val ASSISTANT_CUSTOM_PARAMS_PREFIX = "assistant_custom_params_"
        const val ASSISTANT_COMMON_PHRASES_PREFIX = "assistant_common_phrases_"
        const val ASSISTANT_REASONING_ENABLED_PREFIX = "assistant_reasoning_enabled_"
        const val PREFS_NAME = "hua_juan_public_prefs"
        const val LANGUAGE_KEY = "language"
        const val NOTIFICATIONS_ENABLED_KEY = "notifications_enabled"
        const val DOCUMENT_PARSE_ENABLED_KEY = "document_parse_enabled"
        const val DOCUMENT_SUMMARY_ENABLED_KEY = "document_summary_enabled"
        const val DOCUMENT_AUTO_OCR_KEY = "document_auto_ocr"
        const val DOCUMENT_PREFER_EPUB_KEY = "document_prefer_epub"
        const val DOCUMENT_RENDER_QUALITY_KEY = "document_render_quality"
        const val DOCUMENT_REMOVE_BACKGROUND_KEY = "document_remove_background"
        const val SEARCH_ENGINE_KEY = "search_engine"
        const val WEB_SEARCH_API_KEY_KEY = "web_search_api_key"
        const val WEB_SEARCH_PROVIDER_API_KEY_PREFIX = "web_search_provider_api_key_"
        const val WEB_SEARCH_RESULT_COUNT_KEY = "web_search_result_count"
        const val WEB_SEARCH_INCLUDE_DATE_KEY = "web_search_include_date"
        const val WEB_SEARCH_COMPRESSION_KEY = "web_search_compression"
        const val WEB_SEARCH_BLACKLIST_KEY = "web_search_blacklist"
        const val CONVERSATION_WEB_SEARCH_ENGINE_PREFIX = "conversation_web_search_engine_"
        const val MCP_TOOLS_ENABLED_KEY = "mcp_tools_enabled"
        const val MCP_PERMISSION_LOG_ENABLED_KEY = "mcp_permission_log_enabled"
        const val MCP_ENABLED_TOOLS_KEY = "mcp_enabled_tools"
        const val MCP_SERVER_URLS_KEY = "mcp_server_urls"
        const val GLOBAL_MEMORY_ENABLED_KEY = "global_memory_enabled"
        const val GLOBAL_MEMORY_RETENTION_DAYS_KEY = "global_memory_retention_days"
        const val GLOBAL_MEMORY_MODE_KEY = "global_memory_mode"
        const val GLOBAL_MEMORY_LAST_CLEARED_AT_KEY = "global_memory_last_cleared_at"
        const val KNOWLEDGE_SETTINGS_JSON_KEY = "knowledge_settings_json"
        const val ASSISTANT_DEFAULT_KB_SELECTION_PREFIX = "assistant_default_kb_selection_"
        const val MODEL_TEMPERATURE_PREFIX = "model_temperature_"
        const val MODEL_TOP_P_PREFIX = "model_top_p_"
        const val STARRED_ASSISTANTS_KEY = "starred_assistants"
        const val STARRED_PROVIDERS_KEY = "starred_service_providers"
        const val PROVIDER_ENABLED_PREFIX = "service_provider_enabled_"
        const val CUSTOM_ASSISTANTS_KEY = "custom_assistants_json"
        const val CUSTOM_PROVIDER_TYPE_PREFIX = "custom_provider_type_"
        const val CUSTOM_MODEL_CAPS_PREFIX = "custom_model_caps_"
        const val IMAGE_GENERATION_PROBE_PREFIX = "image_generation_probe_"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDarkMode(): Boolean = prefs.getBoolean(DARK_MODE_KEY, false)

    fun setDarkMode(isDarkMode: Boolean) {
        prefs.edit().putBoolean(DARK_MODE_KEY, isDarkMode).apply()
    }

    fun getThemeMode(): String = prefs.getString(THEME_MODE_KEY, "system") ?: "system"

    fun setThemeMode(themeMode: String) {
        prefs.edit().putString(THEME_MODE_KEY, themeMode).apply()
    }

    fun getFontScale(): Float = prefs.getFloat(FONT_SCALE_KEY, 1.0f)

    fun setFontScale(fontScale: Float) {
        prefs.edit().putFloat(FONT_SCALE_KEY, fontScale).apply()
    }

    fun getTransparencyLevel(): String =
        prefs.getString(TRANSPARENCY_LEVEL_KEY, "medium") ?: "medium"

    fun setTransparencyLevel(level: String) {
        prefs.edit().putString(TRANSPARENCY_LEVEL_KEY, level).apply()
    }

    fun getDebugMode(): Boolean = prefs.getBoolean(DEBUG_MODE_KEY, false)

    fun setDebugMode(isDebugMode: Boolean) {
        prefs.edit().putBoolean(DEBUG_MODE_KEY, isDebugMode).apply()
    }

    fun getTokenAnimationFixedDurationEnabled(): Boolean =
        prefs.getBoolean(TOKEN_ANIMATION_FIXED_DURATION_ENABLED_KEY, false)

    fun setTokenAnimationFixedDurationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(TOKEN_ANIMATION_FIXED_DURATION_ENABLED_KEY, enabled).apply()
    }

    fun getTokenAnimationFixedDurationMs(): Int =
        prefs.getInt(TOKEN_ANIMATION_FIXED_DURATION_MS_KEY, 120)

    fun setTokenAnimationFixedDurationMs(durationMs: Int) {
        prefs.edit().putInt(TOKEN_ANIMATION_FIXED_DURATION_MS_KEY, durationMs.coerceIn(40, 800)).apply()
    }

    fun getLanguage(): String = AppLocaleManager.normalizeLanguage(
        prefs.getString(LANGUAGE_KEY, AppLocaleManager.SYSTEM)
    )

    fun setLanguage(language: String) {
        prefs.edit().putString(LANGUAGE_KEY, AppLocaleManager.normalizeLanguage(language)).apply()
    }

    fun getNotificationsEnabled(): Boolean = prefs.getBoolean(NOTIFICATIONS_ENABLED_KEY, true)

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(NOTIFICATIONS_ENABLED_KEY, enabled).apply()
    }

    fun getDocumentParseEnabled(): Boolean =
        prefs.getBoolean(DOCUMENT_PARSE_ENABLED_KEY, false)

    fun setDocumentParseEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(DOCUMENT_PARSE_ENABLED_KEY, enabled).apply()
    }

    fun getDocumentSummaryEnabled(): Boolean =
        prefs.getBoolean(DOCUMENT_SUMMARY_ENABLED_KEY, false)

    fun setDocumentSummaryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(DOCUMENT_SUMMARY_ENABLED_KEY, enabled).apply()
    }

    fun getDocumentAutoOcrEnabled(): Boolean =
        prefs.getBoolean(DOCUMENT_AUTO_OCR_KEY, false)

    fun setDocumentAutoOcrEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(DOCUMENT_AUTO_OCR_KEY, enabled).apply()
    }

    fun getDocumentPreferEpub(): Boolean =
        prefs.getBoolean(DOCUMENT_PREFER_EPUB_KEY, false)

    fun setDocumentPreferEpub(enabled: Boolean) {
        prefs.edit().putBoolean(DOCUMENT_PREFER_EPUB_KEY, enabled).apply()
    }

    fun getDocumentRenderQuality(): String =
        prefs.getString(DOCUMENT_RENDER_QUALITY_KEY, "medium") ?: "medium"

    fun setDocumentRenderQuality(quality: String) {
        prefs.edit().putString(DOCUMENT_RENDER_QUALITY_KEY, quality).apply()
    }

    fun getDocumentRemoveBackground(): Boolean =
        prefs.getBoolean(DOCUMENT_REMOVE_BACKGROUND_KEY, false)

    fun setDocumentRemoveBackground(enabled: Boolean) {
        prefs.edit().putBoolean(DOCUMENT_REMOVE_BACKGROUND_KEY, enabled).apply()
    }

    fun getSearchEngine(): String {
        val raw = prefs.getString(SEARCH_ENGINE_KEY, WebSearchProviders.defaultSelectionId)
        val normalized = WebSearchProviders.normalizeSelection(raw)
        if (raw != normalized) {
            prefs.edit().putString(SEARCH_ENGINE_KEY, normalized).apply()
        }
        return normalized
    }

    fun setSearchEngine(engine: String) {
        prefs.edit().putString(SEARCH_ENGINE_KEY, WebSearchProviders.normalizeSelection(engine)).apply()
    }

    fun getWebSearchApiKey(): String = prefs.getString(WEB_SEARCH_API_KEY_KEY, "") ?: ""

    fun setWebSearchApiKey(apiKey: String) {
        prefs.edit().putString(WEB_SEARCH_API_KEY_KEY, apiKey).apply()
    }

    fun getWebSearchProviderApiKey(providerId: String): String =
        prefs.getString("$WEB_SEARCH_PROVIDER_API_KEY_PREFIX$providerId", "") ?: ""

    fun setWebSearchProviderApiKey(providerId: String, apiKey: String) {
        prefs.edit().putString("$WEB_SEARCH_PROVIDER_API_KEY_PREFIX$providerId", apiKey).apply()
    }

    fun getWebSearchResultCount(): Int = prefs.getInt(WEB_SEARCH_RESULT_COUNT_KEY, 5)

    fun setWebSearchResultCount(count: Int) {
        prefs.edit().putInt(WEB_SEARCH_RESULT_COUNT_KEY, count).apply()
    }

    fun getWebSearchIncludeDate(): Boolean =
        prefs.getBoolean(WEB_SEARCH_INCLUDE_DATE_KEY, false)

    fun setWebSearchIncludeDate(enabled: Boolean) {
        prefs.edit().putBoolean(WEB_SEARCH_INCLUDE_DATE_KEY, enabled).apply()
    }

    fun getWebSearchCompression(): String =
        prefs.getString(WEB_SEARCH_COMPRESSION_KEY, "none") ?: "none"

    fun setWebSearchCompression(mode: String) {
        prefs.edit().putString(WEB_SEARCH_COMPRESSION_KEY, mode).apply()
    }

    fun getWebSearchBlacklist(): Set<String> =
        prefs.getStringSet(WEB_SEARCH_BLACKLIST_KEY, emptySet()) ?: emptySet()

    fun setWebSearchBlacklist(patterns: Set<String>) {
        prefs.edit().putStringSet(WEB_SEARCH_BLACKLIST_KEY, patterns).apply()
    }

    fun getConversationWebSearchEngine(conversationId: String, fallback: String): String {
        val key = "$CONVERSATION_WEB_SEARCH_ENGINE_PREFIX$conversationId"
        val raw = prefs.getString(key, WebSearchProviders.normalizeSelection(fallback))
        val normalized = WebSearchProviders.normalizeSelection(raw)
        if (raw != normalized) {
            prefs.edit().putString(key, normalized).apply()
        }
        return normalized
    }

    fun setConversationWebSearchEngine(conversationId: String, engine: String) {
        prefs.edit()
            .putString(
                "$CONVERSATION_WEB_SEARCH_ENGINE_PREFIX$conversationId",
                WebSearchProviders.normalizeSelection(engine)
            )
            .apply()
    }

    fun getMcpToolsEnabled(): Boolean = prefs.getBoolean(MCP_TOOLS_ENABLED_KEY, false)

    fun setMcpToolsEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(MCP_TOOLS_ENABLED_KEY, enabled)
            .putBoolean(MCP_PERMISSION_LOG_ENABLED_KEY, false)
            .putStringSet(MCP_ENABLED_TOOLS_KEY, emptySet())
            .putStringSet(MCP_SERVER_URLS_KEY, emptySet())
            .apply()
    }

    fun getMcpPermissionLogEnabled(): Boolean = false

    fun setMcpPermissionLogEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(MCP_PERMISSION_LOG_ENABLED_KEY, false).apply()
    }

    fun getMcpEnabledTools(): Set<String> = emptySet()

    fun setMcpEnabledTools(tools: Set<String>) {
        prefs.edit().putStringSet(MCP_ENABLED_TOOLS_KEY, emptySet()).apply()
    }

    fun getMcpServerUrls(): Set<String> = emptySet()

    fun setMcpServerUrls(urls: Set<String>) {
        prefs.edit().putStringSet(MCP_SERVER_URLS_KEY, emptySet()).apply()
    }

    fun getGlobalMemoryEnabled(): Boolean =
        prefs.getBoolean(GLOBAL_MEMORY_ENABLED_KEY, false)

    fun setGlobalMemoryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(GLOBAL_MEMORY_ENABLED_KEY, enabled).apply()
    }

    fun getGlobalMemoryRetentionDays(): Int =
        prefs.getInt(GLOBAL_MEMORY_RETENTION_DAYS_KEY, 30)

    fun setGlobalMemoryRetentionDays(days: Int) {
        prefs.edit().putInt(GLOBAL_MEMORY_RETENTION_DAYS_KEY, days).apply()
    }

    fun getGlobalMemoryMode(): String =
        prefs.getString(GLOBAL_MEMORY_MODE_KEY, "context") ?: "context"

    fun setGlobalMemoryMode(mode: String) {
        prefs.edit().putString(GLOBAL_MEMORY_MODE_KEY, mode).apply()
    }

    fun getGlobalMemoryLastClearedAt(): Long =
        prefs.getLong(GLOBAL_MEMORY_LAST_CLEARED_AT_KEY, 0L)

    fun setGlobalMemoryLastClearedAt(timestamp: Long) {
        prefs.edit().putLong(GLOBAL_MEMORY_LAST_CLEARED_AT_KEY, timestamp).apply()
    }

    fun getKnowledgeSettings(): KnowledgeSettings {
        val raw = prefs.getString(KNOWLEDGE_SETTINGS_JSON_KEY, null) ?: return KnowledgeSettings()
        return runCatching { Gson().fromJson(raw, KnowledgeSettings::class.java) }
            .getOrDefault(KnowledgeSettings())
    }

    fun setKnowledgeSettings(settings: KnowledgeSettings) {
        prefs.edit().putString(KNOWLEDGE_SETTINGS_JSON_KEY, Gson().toJson(settings)).apply()
    }

    fun getAssistantDefaultKbSelection(assistantId: String): List<String> {
        val raw = prefs.getString("$ASSISTANT_DEFAULT_KB_SELECTION_PREFIX$assistantId", "[]") ?: "[]"
        return runCatching {
            val type = com.google.gson.reflect.TypeToken.getParameterized(
                List::class.java,
                String::class.java
            ).type
            Gson().fromJson<List<String>>(raw, type) ?: emptyList()
        }.getOrDefault(emptyList())
    }

    fun setAssistantDefaultKbSelection(assistantId: String, kbIds: List<String>) {
        prefs.edit()
            .putString(
                "$ASSISTANT_DEFAULT_KB_SELECTION_PREFIX$assistantId",
                Gson().toJson(kbIds.distinct())
            )
            .apply()
    }

    fun getCurrentAssistantId(): String =
        prefs.getString(CURRENT_ASSISTANT_ID_KEY, "") ?: ""

    fun setCurrentAssistantId(assistantId: String) {
        prefs.edit().putString(CURRENT_ASSISTANT_ID_KEY, assistantId).apply()
    }

    fun getStarredAssistantIds(): Set<String> =
        prefs.getStringSet(STARRED_ASSISTANTS_KEY, emptySet()) ?: emptySet()

    fun setStarredAssistantIds(ids: Set<String>) {
        prefs.edit().putStringSet(STARRED_ASSISTANTS_KEY, ids).apply()
    }

    fun getStarredServiceProviders(): Set<String> =
        prefs.getStringSet(STARRED_PROVIDERS_KEY, emptySet()) ?: emptySet()

    fun setStarredServiceProviders(providers: Set<String>) {
        prefs.edit().putStringSet(STARRED_PROVIDERS_KEY, providers).apply()
    }

    fun getServiceProviderEnabled(provider: String): Boolean =
        providerStorageCandidates(provider)
            .firstNotNullOfOrNull { candidate ->
                if (prefs.contains("$PROVIDER_ENABLED_PREFIX$candidate")) {
                    prefs.getBoolean("$PROVIDER_ENABLED_PREFIX$candidate", true)
                } else {
                    null
                }
            } ?: true

    fun setServiceProviderEnabled(provider: String, enabled: Boolean) {
        prefs.edit()
            .putBoolean("$PROVIDER_ENABLED_PREFIX${BuiltInServiceProviders.normalizeId(provider)}", enabled)
            .apply()
    }

    fun getCustomAssistantsJson(): String =
        prefs.getString(CUSTOM_ASSISTANTS_KEY, "[]") ?: "[]"

    fun setCustomAssistantsJson(json: String) {
        prefs.edit().putString(CUSTOM_ASSISTANTS_KEY, json).apply()
    }

    fun getAssistantModelConfig(
        assistantId: String,
        fallbackServiceProvider: String,
        fallbackSelectedModel: String,
        fallbackUseCloud: Boolean,
        fallbackLocalSelected: String
    ): com.huajuan.aispace.data.AssistantModelConfig {
        return com.huajuan.aispace.data.AssistantModelConfig(
            useCloudModel = getAssistantUseCloudModel(assistantId, fallbackUseCloud),
            serviceProvider = getAssistantServiceProvider(assistantId, fallbackServiceProvider),
            selectedModelName = getAssistantSelectedModel(assistantId, fallbackSelectedModel),
            localSelectedModelName = getAssistantLocalSelectedModel(assistantId, fallbackLocalSelected)
        )
    }

    fun getAssistantUseCloudModel(assistantId: String, fallback: Boolean): Boolean =
        prefs.getBoolean("$ASSISTANT_USE_CLOUD_PREFIX$assistantId", fallback)

    fun setAssistantUseCloudModel(assistantId: String, enabled: Boolean) {
        prefs.edit().putBoolean("$ASSISTANT_USE_CLOUD_PREFIX$assistantId", enabled).apply()
    }

    fun getAssistantServiceProvider(assistantId: String, fallback: String): String {
        val prefKey = "$ASSISTANT_PROVIDER_PREFIX$assistantId"
        val stored = prefs.getString(prefKey, null)
            ?: providerStorageCandidates(fallback).firstNotNullOfOrNull { legacy ->
                prefs.getString(prefKey.replace(fallback, legacy), null)
            }
        return BuiltInServiceProviders.normalizeId(stored ?: fallback)
    }

    fun setAssistantServiceProvider(assistantId: String, provider: String) {
        prefs.edit()
            .putString(
                "$ASSISTANT_PROVIDER_PREFIX$assistantId",
                BuiltInServiceProviders.normalizeId(provider)
            )
            .apply()
    }

    fun getAssistantSelectedModel(assistantId: String, fallback: String): String =
        prefs.getString("$ASSISTANT_SELECTED_MODEL_PREFIX$assistantId", fallback) ?: fallback

    fun setAssistantSelectedModel(assistantId: String, modelName: String) {
        prefs.edit().putString("$ASSISTANT_SELECTED_MODEL_PREFIX$assistantId", modelName).apply()
    }

    fun getAssistantLocalSelectedModel(assistantId: String, fallback: String): String =
        prefs.getString("$ASSISTANT_LOCAL_MODEL_PREFIX$assistantId", fallback) ?: fallback

    fun setAssistantLocalSelectedModel(assistantId: String, modelName: String) {
        prefs.edit().putString("$ASSISTANT_LOCAL_MODEL_PREFIX$assistantId", modelName).apply()
    }

    fun getAssistantNameOverride(assistantId: String): String =
        prefs.getString("$ASSISTANT_NAME_PREFIX$assistantId", "") ?: ""

    fun setAssistantNameOverride(assistantId: String, name: String) {
        prefs.edit().putString("$ASSISTANT_NAME_PREFIX$assistantId", name).apply()
    }

    fun getAssistantEmojiOverride(assistantId: String): String =
        prefs.getString("$ASSISTANT_EMOJI_PREFIX$assistantId", "") ?: ""

    fun setAssistantEmojiOverride(assistantId: String, emoji: String) {
        prefs.edit().putString("$ASSISTANT_EMOJI_PREFIX$assistantId", emoji).apply()
    }

    fun getAssistantSystemPromptOverride(assistantId: String): String =
        prefs.getString("$ASSISTANT_SYSTEM_PROMPT_PREFIX$assistantId", "") ?: ""

    fun hasAssistantSystemPromptOverride(assistantId: String): Boolean =
        prefs.contains("$ASSISTANT_SYSTEM_PROMPT_PREFIX$assistantId")

    fun setAssistantSystemPromptOverride(assistantId: String, prompt: String) {
        prefs.edit().putString("$ASSISTANT_SYSTEM_PROMPT_PREFIX$assistantId", prompt).apply()
    }

    fun clearAssistantPromptOverrides(assistantId: String) {
        prefs.edit()
            .remove("$ASSISTANT_NAME_PREFIX$assistantId")
            .remove("$ASSISTANT_EMOJI_PREFIX$assistantId")
            .remove("$ASSISTANT_SYSTEM_PROMPT_PREFIX$assistantId")
            .apply()
    }

    fun getAssistantTemperature(assistantId: String, fallback: Float): Float =
        prefs.getFloat("$ASSISTANT_TEMPERATURE_PREFIX$assistantId", fallback)

    fun setAssistantTemperature(assistantId: String, value: Float) {
        prefs.edit().putFloat("$ASSISTANT_TEMPERATURE_PREFIX$assistantId", value).apply()
    }

    fun getAssistantTopP(assistantId: String, fallback: Float): Float =
        prefs.getFloat("$ASSISTANT_TOP_P_PREFIX$assistantId", fallback)

    fun setAssistantTopP(assistantId: String, value: Float) {
        prefs.edit().putFloat("$ASSISTANT_TOP_P_PREFIX$assistantId", value).apply()
    }

    fun getAssistantContextLimit(assistantId: String, fallback: Int): Int =
        prefs.getInt("$ASSISTANT_CONTEXT_LIMIT_PREFIX$assistantId", fallback)

    fun setAssistantContextLimit(assistantId: String, value: Int) {
        prefs.edit().putInt("$ASSISTANT_CONTEXT_LIMIT_PREFIX$assistantId", value).apply()
    }

    fun getAssistantMaxTokensEnabled(assistantId: String, fallback: Boolean): Boolean =
        prefs.getBoolean("$ASSISTANT_MAX_TOKENS_ENABLED_PREFIX$assistantId", fallback)

    fun setAssistantMaxTokensEnabled(assistantId: String, enabled: Boolean) {
        prefs.edit().putBoolean("$ASSISTANT_MAX_TOKENS_ENABLED_PREFIX$assistantId", enabled).apply()
    }

    fun getAssistantMaxTokens(assistantId: String, fallback: Int): Int =
        prefs.getInt("$ASSISTANT_MAX_TOKENS_PREFIX$assistantId", fallback)

    fun setAssistantMaxTokens(assistantId: String, value: Int) {
        prefs.edit().putInt("$ASSISTANT_MAX_TOKENS_PREFIX$assistantId", value).apply()
    }

    fun getAssistantStreamEnabled(assistantId: String, fallback: Boolean): Boolean =
        prefs.getBoolean("$ASSISTANT_STREAM_ENABLED_PREFIX$assistantId", fallback)

    fun setAssistantStreamEnabled(assistantId: String, enabled: Boolean) {
        prefs.edit().putBoolean("$ASSISTANT_STREAM_ENABLED_PREFIX$assistantId", enabled).apply()
    }

    fun getAssistantToolCallMode(assistantId: String, fallback: String): String =
        prefs.getString("$ASSISTANT_TOOL_CALL_MODE_PREFIX$assistantId", fallback) ?: fallback

    fun setAssistantToolCallMode(assistantId: String, mode: String) {
        prefs.edit().putString("$ASSISTANT_TOOL_CALL_MODE_PREFIX$assistantId", mode).apply()
    }

    fun getAssistantReasoningEnabled(assistantId: String, fallback: Boolean): Boolean =
        prefs.getBoolean("$ASSISTANT_REASONING_ENABLED_PREFIX$assistantId", fallback)

    fun setAssistantReasoningEnabled(assistantId: String, enabled: Boolean) {
        prefs.edit().putBoolean("$ASSISTANT_REASONING_ENABLED_PREFIX$assistantId", enabled).apply()
    }

    fun getAssistantCustomParams(assistantId: String, fallback: String): String =
        prefs.getString("$ASSISTANT_CUSTOM_PARAMS_PREFIX$assistantId", fallback) ?: fallback

    fun setAssistantCustomParams(assistantId: String, params: String) {
        prefs.edit().putString("$ASSISTANT_CUSTOM_PARAMS_PREFIX$assistantId", params).apply()
    }

    fun clearAssistantRequestSettings(assistantId: String) {
        prefs.edit()
            .remove("$ASSISTANT_TEMPERATURE_PREFIX$assistantId")
            .remove("$ASSISTANT_TOP_P_PREFIX$assistantId")
            .remove("$ASSISTANT_CONTEXT_LIMIT_PREFIX$assistantId")
            .remove("$ASSISTANT_MAX_TOKENS_ENABLED_PREFIX$assistantId")
            .remove("$ASSISTANT_MAX_TOKENS_PREFIX$assistantId")
            .remove("$ASSISTANT_STREAM_ENABLED_PREFIX$assistantId")
            .remove("$ASSISTANT_TOOL_CALL_MODE_PREFIX$assistantId")
            .remove("$ASSISTANT_REASONING_ENABLED_PREFIX$assistantId")
            .remove("$ASSISTANT_CUSTOM_PARAMS_PREFIX$assistantId")
            .apply()
    }

    fun getAssistantCommonPhrasesJson(assistantId: String): String =
        prefs.getString("$ASSISTANT_COMMON_PHRASES_PREFIX$assistantId", "[]") ?: "[]"

    fun setAssistantCommonPhrasesJson(assistantId: String, json: String) {
        prefs.edit().putString("$ASSISTANT_COMMON_PHRASES_PREFIX$assistantId", json).apply()
    }

    fun clearAssistantScopedState(assistantId: String) {
        prefs.edit()
            .remove("$ASSISTANT_USE_CLOUD_PREFIX$assistantId")
            .remove("$ASSISTANT_PROVIDER_PREFIX$assistantId")
            .remove("$ASSISTANT_SELECTED_MODEL_PREFIX$assistantId")
            .remove("$ASSISTANT_LOCAL_MODEL_PREFIX$assistantId")
            .remove("$ASSISTANT_NAME_PREFIX$assistantId")
            .remove("$ASSISTANT_EMOJI_PREFIX$assistantId")
            .remove("$ASSISTANT_SYSTEM_PROMPT_PREFIX$assistantId")
            .remove("$ASSISTANT_TEMPERATURE_PREFIX$assistantId")
            .remove("$ASSISTANT_TOP_P_PREFIX$assistantId")
            .remove("$ASSISTANT_CONTEXT_LIMIT_PREFIX$assistantId")
            .remove("$ASSISTANT_MAX_TOKENS_ENABLED_PREFIX$assistantId")
            .remove("$ASSISTANT_MAX_TOKENS_PREFIX$assistantId")
            .remove("$ASSISTANT_STREAM_ENABLED_PREFIX$assistantId")
            .remove("$ASSISTANT_TOOL_CALL_MODE_PREFIX$assistantId")
            .remove("$ASSISTANT_REASONING_ENABLED_PREFIX$assistantId")
            .remove("$ASSISTANT_CUSTOM_PARAMS_PREFIX$assistantId")
            .remove("$ASSISTANT_COMMON_PHRASES_PREFIX$assistantId")
            .apply()
    }

    fun getUseCloudModel(): Boolean = prefs.getBoolean("use_cloud_model", false)

    fun setUseCloudModel(useCloudModel: Boolean) {
        prefs.edit().putBoolean("use_cloud_model", useCloudModel).apply()
    }

    fun getServiceProvider(): String {
        val defaultProvider = BuiltInServiceProviders.SiliconFlow
        val stored = prefs.getString("service_provider", null)
        return BuiltInServiceProviders.normalizeId(stored ?: defaultProvider)
    }

    fun setServiceProvider(serviceProvider: String) {
        prefs.edit()
            .putString("service_provider", BuiltInServiceProviders.normalizeId(serviceProvider))
            .apply()
    }

    fun getApiKeyForProvider(provider: String): String =
        providerStorageCandidates(provider)
            .firstNotNullOfOrNull { candidate -> prefs.getString("api_key_$candidate", null) }
            ?: ""

    fun setApiKeyForProvider(provider: String, apiKey: String) {
        prefs.edit()
            .putString("api_key_${BuiltInServiceProviders.normalizeId(provider)}", apiKey)
            .apply()
    }

    fun getSelectedModelForProvider(provider: String): String =
        providerStorageCandidates(provider)
            .firstNotNullOfOrNull { candidate -> prefs.getString("selected_model_$candidate", null) }
            ?: ""

    fun getCustomApiUrl(): String = prefs.getString("custom_api_url", "") ?: ""

    fun setCustomApiUrl(customApiUrl: String) {
        prefs.edit().putString("custom_api_url", customApiUrl).apply()
    }

    fun getCustomServiceProviders(): Set<String> =
        prefs.getStringSet("custom_service_providers", emptySet()) ?: emptySet()

    fun addCustomServiceProvider(name: String, apiUrl: String, providerType: ProviderType) {
        val customProviders = getCustomServiceProviders().toMutableSet()
        customProviders.add(name)
        prefs.edit()
            .putStringSet("custom_service_providers", customProviders)
            .putString("custom_provider_url_$name", apiUrl)
            .putString("$CUSTOM_PROVIDER_TYPE_PREFIX$name", providerType.name)
            .apply()
    }

    fun removeCustomServiceProvider(name: String) {
        val customModels = getCustomModelsForProvider(name)
        val customProviders = getCustomServiceProviders().toMutableSet()
        customProviders.remove(name)
        val editor = prefs.edit()
        customModels.forEach { modelName ->
            editor.remove("$CUSTOM_MODEL_CAPS_PREFIX${name}_$modelName")
        }
        editor
            .putStringSet("custom_service_providers", customProviders)
            .remove("custom_provider_url_$name")
            .remove("$CUSTOM_PROVIDER_TYPE_PREFIX$name")
            .remove("custom_provider_models_$name")
            .remove("api_key_$name")
            .remove("selected_model_$name")
            .apply()
    }

    fun getCustomProviderApiUrl(name: String): String =
        prefs.getString("custom_provider_url_$name", "") ?: ""

    fun getCustomProviderType(name: String): ProviderType {
        val raw = prefs.getString("$CUSTOM_PROVIDER_TYPE_PREFIX$name", ProviderType.OpenAI.name)
            ?: ProviderType.OpenAI.name
        return runCatching { ProviderType.valueOf(raw) }.getOrDefault(ProviderType.OpenAI)
    }

    fun getCustomModelsForProvider(providerName: String): Set<String> =
        prefs.getStringSet("custom_provider_models_$providerName", emptySet()) ?: emptySet()

    fun addCustomModelToProvider(
        providerName: String,
        modelName: String,
        apiCode: String,
        capabilities: Set<ModelCapability>
    ) {
        val customModels = getCustomModelsForProvider(providerName).toMutableSet()
        customModels.add(modelName)
        val capabilityNames = capabilities.map { it.name }.toSet()
        prefs.edit()
            .putStringSet("custom_provider_models_$providerName", customModels)
            .putString("custom_model_code_${providerName}_$modelName", apiCode)
            .putStringSet("$CUSTOM_MODEL_CAPS_PREFIX${providerName}_$modelName", capabilityNames)
            .apply()
    }

    fun removeCustomModelFromProvider(providerName: String, modelName: String) {
        val customModels = getCustomModelsForProvider(providerName).toMutableSet()
        customModels.remove(modelName)
        prefs.edit()
            .putStringSet("custom_provider_models_$providerName", customModels)
            .remove("custom_model_code_${providerName}_$modelName")
            .remove("$CUSTOM_MODEL_CAPS_PREFIX${providerName}_$modelName")
            .apply()
    }

    fun getCustomModelApiCode(providerName: String, modelName: String): String =
        prefs.getString("custom_model_code_${providerName}_$modelName", "") ?: ""

    fun getCustomModelCapabilities(providerName: String, modelName: String): Set<ModelCapability> {
        val raw = prefs.getStringSet("$CUSTOM_MODEL_CAPS_PREFIX${providerName}_$modelName", emptySet())
            ?: emptySet()
        if (raw.isEmpty()) return emptySet()
        return raw.mapNotNull { name ->
            runCatching { ModelCapability.valueOf(name) }.getOrNull()
        }.toSet()
    }

    fun getImageGenerationProbeRecord(providerName: String, modelApiCode: String): ImageGenerationProbeRecord? {
        val raw = prefs.getString("$IMAGE_GENERATION_PROBE_PREFIX${providerName}_$modelApiCode", null) ?: return null
        return runCatching { Gson().fromJson(raw, ImageGenerationProbeRecord::class.java) }.getOrNull()
    }

    fun setImageGenerationProbeRecord(
        providerName: String,
        modelApiCode: String,
        record: ImageGenerationProbeRecord
    ) {
        prefs.edit()
            .putString(
                "$IMAGE_GENERATION_PROBE_PREFIX${providerName}_$modelApiCode",
                Gson().toJson(record)
            )
            .apply()
    }

    fun clearImageGenerationProbeRecord(providerName: String, modelApiCode: String) {
        prefs.edit()
            .remove("$IMAGE_GENERATION_PROBE_PREFIX${providerName}_$modelApiCode")
            .apply()
    }

    fun getLocalSelectedModel(): String =
        prefs.getString("local_selected_model", "") ?: ""

    fun setLocalSelectedModel(model: String) {
        prefs.edit().putString("local_selected_model", model).apply()
    }

    fun getModelTemperature(provider: String, model: String): Float {
        val key = "$MODEL_TEMPERATURE_PREFIX${provider}_$model"
        return prefs.getFloat(key, 0.7f)
    }

    fun setModelTemperature(provider: String, model: String, temperature: Float) {
        val key = "$MODEL_TEMPERATURE_PREFIX${provider}_$model"
        prefs.edit().putFloat(key, temperature).apply()
    }

    fun getModelTopP(provider: String, model: String): Float {
        val key = "$MODEL_TOP_P_PREFIX${provider}_$model"
        return prefs.getFloat(key, 0.9f)
    }

    fun setModelTopP(provider: String, model: String, topP: Float) {
        val key = "$MODEL_TOP_P_PREFIX${provider}_$model"
        prefs.edit().putFloat(key, topP).apply()
    }

    fun allKeys(): Set<String> = prefs.all.keys

    fun removeKeys(predicate: (String) -> Boolean) {
        val editor = prefs.edit()
        prefs.all.keys.forEach { key ->
            if (predicate(key)) {
                editor.remove(key)
            }
        }
        editor.apply()
    }

    fun resetToDefaults() {
        prefs.edit()
            .putBoolean(DARK_MODE_KEY, false)
            .putString(THEME_MODE_KEY, "system")
            .putFloat(FONT_SCALE_KEY, 1.0f)
            .putString(TRANSPARENCY_LEVEL_KEY, "medium")
            .putBoolean(DEBUG_MODE_KEY, false)
            .putBoolean(TOKEN_ANIMATION_FIXED_DURATION_ENABLED_KEY, false)
            .putInt(TOKEN_ANIMATION_FIXED_DURATION_MS_KEY, 120)
            .putString(LANGUAGE_KEY, AppLocaleManager.SYSTEM)
            .putBoolean(NOTIFICATIONS_ENABLED_KEY, true)
            .putBoolean("use_cloud_model", false)
            .putString("service_provider", BuiltInServiceProviders.SiliconFlow)
            .putString("custom_api_url", "")
            .putBoolean(DOCUMENT_PARSE_ENABLED_KEY, false)
            .putBoolean(DOCUMENT_SUMMARY_ENABLED_KEY, false)
            .putBoolean(DOCUMENT_AUTO_OCR_KEY, false)
            .putBoolean(DOCUMENT_PREFER_EPUB_KEY, false)
            .putString(DOCUMENT_RENDER_QUALITY_KEY, "medium")
            .putBoolean(DOCUMENT_REMOVE_BACKGROUND_KEY, false)
            .putString(SEARCH_ENGINE_KEY, WebSearchProviders.defaultSelectionId)
            .putString(WEB_SEARCH_API_KEY_KEY, "")
            .putInt(WEB_SEARCH_RESULT_COUNT_KEY, 5)
            .putBoolean(WEB_SEARCH_INCLUDE_DATE_KEY, false)
            .putString(WEB_SEARCH_COMPRESSION_KEY, "none")
            .putStringSet(WEB_SEARCH_BLACKLIST_KEY, emptySet())
            .putBoolean(MCP_TOOLS_ENABLED_KEY, false)
            .putBoolean(MCP_PERMISSION_LOG_ENABLED_KEY, false)
            .putStringSet(MCP_ENABLED_TOOLS_KEY, emptySet())
            .putStringSet(MCP_SERVER_URLS_KEY, emptySet())
            .putBoolean(GLOBAL_MEMORY_ENABLED_KEY, false)
            .putInt(GLOBAL_MEMORY_RETENTION_DAYS_KEY, 30)
            .putString(GLOBAL_MEMORY_MODE_KEY, "context")
            .putLong(GLOBAL_MEMORY_LAST_CLEARED_AT_KEY, 0L)
            .putString(KNOWLEDGE_SETTINGS_JSON_KEY, Gson().toJson(KnowledgeSettings()))
            .putStringSet(STARRED_ASSISTANTS_KEY, emptySet())
            .putStringSet(STARRED_PROVIDERS_KEY, emptySet())
            .apply()

        removeKeys { key ->
            key.startsWith("custom_") ||
                key.startsWith("api_key_") ||
                key.startsWith("selected_model_") ||
                key.startsWith("local_selected_model") ||
                key.startsWith(ASSISTANT_USE_CLOUD_PREFIX) ||
                key.startsWith(ASSISTANT_PROVIDER_PREFIX) ||
                key.startsWith(ASSISTANT_SELECTED_MODEL_PREFIX) ||
                key.startsWith(ASSISTANT_LOCAL_MODEL_PREFIX) ||
                key.startsWith(ASSISTANT_NAME_PREFIX) ||
                key.startsWith(ASSISTANT_EMOJI_PREFIX) ||
                key.startsWith(ASSISTANT_SYSTEM_PROMPT_PREFIX) ||
                key.startsWith(ASSISTANT_TEMPERATURE_PREFIX) ||
                key.startsWith(ASSISTANT_TOP_P_PREFIX) ||
                key.startsWith(ASSISTANT_CONTEXT_LIMIT_PREFIX) ||
                key.startsWith(ASSISTANT_MAX_TOKENS_ENABLED_PREFIX) ||
                key.startsWith(ASSISTANT_MAX_TOKENS_PREFIX) ||
                key.startsWith(ASSISTANT_STREAM_ENABLED_PREFIX) ||
                key.startsWith(ASSISTANT_TOOL_CALL_MODE_PREFIX) ||
                key.startsWith(ASSISTANT_CUSTOM_PARAMS_PREFIX) ||
                key.startsWith(ASSISTANT_COMMON_PHRASES_PREFIX) ||
                key.startsWith(ASSISTANT_DEFAULT_KB_SELECTION_PREFIX) ||
                key.startsWith(MODEL_TEMPERATURE_PREFIX) ||
                key.startsWith(MODEL_TOP_P_PREFIX) ||
                key.startsWith(PROVIDER_ENABLED_PREFIX) ||
                key.startsWith(WEB_SEARCH_PROVIDER_API_KEY_PREFIX) ||
                key.startsWith(CONVERSATION_WEB_SEARCH_ENGINE_PREFIX)
        }

    }

    private fun providerStorageCandidates(provider: String): List<String> {
        val normalized = BuiltInServiceProviders.normalizeId(provider)
        return buildList {
            add(normalized)
            addAll(BuiltInServiceProviders.getLegacyNames(normalized))
            if (provider.isNotBlank()) add(provider)
        }.distinct()
    }
}
