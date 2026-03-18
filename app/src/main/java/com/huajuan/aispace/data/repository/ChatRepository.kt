package com.huajuan.aispace.data.repository

import com.huajuan.aispace.data.Conversation
import com.huajuan.aispace.data.Message
import com.huajuan.aispace.data.Repository
import com.huajuan.aispace.data.RuntimeMcpConfig
import com.huajuan.aispace.data.RuntimeWebSearchConfig
import com.huajuan.aispace.data.model.ChatEvent
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val repository: Repository
) {
    suspend fun getConversationsAsync(assistantId: String): List<Conversation> =
        repository.getConversationsAsync(assistantId)

    suspend fun getMessagesAsync(conversationId: String, assistantId: String): List<Message> =
        repository.getMessagesAsync(conversationId, assistantId)

    suspend fun searchMessagesAsync(conversationId: String, assistantId: String, query: String): List<Message> =
        repository.searchMessagesAsync(conversationId, assistantId, query)

    suspend fun updateMessageShowThinkAsync(messageId: String, showThink: Boolean) =
        repository.updateMessageShowThinkAsync(messageId, showThink)

    suspend fun saveMessages(conversationId: String, assistantId: String, messages: List<Message>) =
        repository.saveMessages(conversationId, assistantId, messages)

    suspend fun createNewConversation(
        assistantId: String,
        title: String,
        roleName: String = "",
        systemPrompt: String = ""
    ): Conversation = repository.createNewConversation(assistantId, title, roleName, systemPrompt)

    suspend fun updateLastMessage(conversationId: String, lastMessage: String) =
        repository.updateLastMessage(conversationId, lastMessage)

    suspend fun getConversationRoleNameAsync(conversationId: String): String =
        repository.getConversationRoleNameAsync(conversationId)

    suspend fun getConversationSystemPromptAsync(conversationId: String): String =
        repository.getConversationSystemPromptAsync(conversationId)

    suspend fun updateConversationRole(conversationId: String, roleName: String, systemPrompt: String) =
        repository.updateConversationRole(conversationId, roleName, systemPrompt)

    fun streamAIResponse(
        messages: List<Message>,
        conversationId: String,
        debugScopeId: String? = null
    ): Flow<ChatEvent> = repository.streamAIResponse(messages, conversationId, debugScopeId)

    fun extractImageUrlsFromResponse(responseText: String): List<String> =
        repository.extractImageUrlsFromResponse(responseText)

    fun extractAndCleanImageUrls(responseText: String): Pair<List<String>, String> =
        repository.extractAndCleanImageUrls(responseText)

    suspend fun processAIImageResponse(responseText: String): Pair<List<String>, String> =
        repository.processAIImageResponse(responseText)

    suspend fun updateConversationTitle(conversationId: String, title: String) =
        repository.updateConversationTitle(conversationId, title)

    suspend fun getAIResponse(
        messages: List<Message>,
        conversationId: String,
        debugScopeId: String? = null
    ): String = repository.getAIResponse(messages, conversationId, debugScopeId)

    fun getDebugMode(): Boolean = repository.getDebugMode()

    fun beginDebugRequestCapture(scopeId: String?) =
        repository.beginDebugRequestCapture(scopeId)

    fun consumeDebugRequestSnapshot(scopeId: String?): String =
        repository.consumeDebugRequestSnapshot(scopeId)

    fun endDebugRequestCapture(scopeId: String?) =
        repository.endDebugRequestCapture(scopeId)

    fun setRuntimeWebSearchConfig(config: RuntimeWebSearchConfig?) =
        repository.setRuntimeWebSearchConfig(config)

    fun setRuntimeMcpConfig(config: RuntimeMcpConfig?) =
        repository.setRuntimeMcpConfig(config)
}
