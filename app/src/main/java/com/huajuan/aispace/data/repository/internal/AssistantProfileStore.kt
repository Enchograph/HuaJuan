package com.huajuan.aispace.data.repository.internal

import android.content.Context
import com.huajuan.aispace.data.Agent
import com.huajuan.aispace.data.AgentProvider
import com.huajuan.aispace.data.BuiltInAssistants
import com.huajuan.aispace.data.ToolIds
import com.huajuan.aispace.R
import com.huajuan.aispace.i18n.LocalizedResources
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

internal class AssistantProfileStore(
    private val context: Context,
    private val preferenceStore: AppPreferenceStore,
    private val agentProvider: AgentProvider,
    private val gson: Gson
) {
    fun getCurrentAssistantId(): String {
        val stored = preferenceStore.getCurrentAssistantId()
        val availableIds = (agentProvider.getAgents() + getCustomAssistants()).map { it.id }.toSet()
        if (
            stored.isNotBlank() &&
            !ToolIds.toolConversationIds.contains(stored) &&
            stored in availableIds
        ) return stored
        val fallback = agentProvider.getAgents().firstOrNull()?.id ?: "default"
        preferenceStore.setCurrentAssistantId(fallback)
        return fallback
    }

    fun setCurrentAssistantId(assistantId: String) {
        if (ToolIds.toolConversationIds.contains(assistantId)) return
        preferenceStore.setCurrentAssistantId(assistantId)
    }

    fun getStarredAssistantIds(): Set<String> =
        preferenceStore.getStarredAssistantIds()

    fun setStarredAssistantIds(ids: Set<String>) {
        preferenceStore.setStarredAssistantIds(ids)
    }

    fun getStarredServiceProviders(): Set<String> =
        preferenceStore.getStarredServiceProviders()

    fun setStarredServiceProviders(providers: Set<String>) {
        preferenceStore.setStarredServiceProviders(providers)
    }

    fun getServiceProviderEnabled(provider: String): Boolean =
        preferenceStore.getServiceProviderEnabled(provider)

    fun setServiceProviderEnabled(provider: String, enabled: Boolean) {
        preferenceStore.setServiceProviderEnabled(provider, enabled)
    }

    fun getCustomAssistants(): List<Agent> {
        val json = preferenceStore.getCustomAssistantsJson()
        return try {
            val type = object : TypeToken<List<Agent>>() {}.type
            gson.fromJson<List<Agent>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun setCustomAssistants(list: List<Agent>) {
        preferenceStore.setCustomAssistantsJson(gson.toJson(list))
    }

    fun getAssistantById(assistantId: String): Agent? {
        val custom = getCustomAssistants().firstOrNull { it.id == assistantId }
        if (custom != null) return custom
        val builtIn = agentProvider.getAgents().firstOrNull { it.id == assistantId } ?: return null
        val nameOverride = preferenceStore.getAssistantNameOverride(assistantId)
        val emojiOverride = preferenceStore.getAssistantEmojiOverride(assistantId)
        val promptOverride = preferenceStore.getAssistantSystemPromptOverride(assistantId)
        return builtIn.copy(
            name = nameOverride.ifBlank { builtIn.name },
            emoji = emojiOverride.ifBlank { builtIn.emoji },
            systemPrompt = if (preferenceStore.hasAssistantSystemPromptOverride(assistantId)) {
                promptOverride
            } else {
                builtIn.systemPrompt
            }
        )
    }

    fun getAssistantName(assistantId: String): String {
        val assistant = getAssistantById(assistantId)
        return assistant?.name ?: LocalizedResources.getString(context, R.string.default_assistant_name)
    }

    fun getAssistantEmoji(assistantId: String): String {
        val assistant = getAssistantById(assistantId)
        return assistant?.emoji ?: "✨"
    }

    fun getAssistantSystemPrompt(assistantId: String): String {
        val assistant = getAssistantById(assistantId)
        return assistant?.systemPrompt ?: LocalizedResources.getString(context, R.string.default_assistant_prompt)
    }

    fun getBuiltInDefaultValues(assistantId: String): List<com.huajuan.aispace.data.BuiltInAssistantDefaults> {
        return BuiltInAssistants.allLocalizedDefaultValues(context, assistantId)
    }

    fun updateAssistantPromptSettings(
        assistantId: String,
        name: String,
        emoji: String,
        systemPrompt: String
    ) {
        val custom = getCustomAssistants().firstOrNull { it.id == assistantId }
        if (custom != null) {
            val updated = getCustomAssistants().map { agent ->
                if (agent.id == assistantId) {
                    agent.copy(
                        name = name,
                        emoji = emoji,
                        systemPrompt = systemPrompt
                    )
                } else {
                    agent
                }
            }
            setCustomAssistants(updated)
            return
        }

        preferenceStore.setAssistantNameOverride(assistantId, name)
        preferenceStore.setAssistantEmojiOverride(assistantId, emoji)
        preferenceStore.setAssistantSystemPromptOverride(assistantId, systemPrompt)
    }

    fun clearAssistantPromptSettings(assistantId: String) {
        val custom = getCustomAssistants().firstOrNull { it.id == assistantId }
        if (custom != null) {
            val builtIn = agentProvider.getAgents().firstOrNull { it.id == assistantId }
            val updated = getCustomAssistants().map { agent ->
                if (agent.id == assistantId) {
                    val fallback = builtIn ?: agent
                    agent.copy(
                        name = fallback.name,
                        emoji = fallback.emoji,
                        systemPrompt = fallback.systemPrompt
                    )
                } else {
                    agent
                }
            }
            setCustomAssistants(updated)
        } else {
            preferenceStore.clearAssistantPromptOverrides(assistantId)
        }
    }

    fun getAssistantCommonPhrases(assistantId: String): List<String> {
        val json = preferenceStore.getAssistantCommonPhrasesJson(assistantId)
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun setAssistantCommonPhrases(assistantId: String, phrases: List<String>) {
        preferenceStore.setAssistantCommonPhrasesJson(assistantId, gson.toJson(phrases))
    }
}
