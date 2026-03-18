package com.huajuan.aispace.data.repository.internal

import android.os.Looper
import androidx.annotation.WorkerThread
import com.huajuan.aispace.data.Conversation
import com.huajuan.aispace.data.MessageSearchResult
import com.huajuan.aispace.data.Message
import com.huajuan.aispace.data.AppDatabase
import com.huajuan.aispace.data.ConversationDao
import com.huajuan.aispace.data.ConversationEntity
import com.huajuan.aispace.data.Agent
import com.huajuan.aispace.data.ImageGenerationConversationState
import com.huajuan.aispace.data.MessageDao
import com.huajuan.aispace.data.MessageEntity
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Date

internal class ConversationLocalStore(
    database: AppDatabase
) {
    private val conversationDao: ConversationDao = database.conversationDao()
    private val messageDao: MessageDao = database.messageDao()

    suspend fun repairAssistantBindings(
        agents: List<Agent>,
        fallbackAssistantId: String,
        reservedAssistantIds: Set<String> = emptySet()
    ) {
        withContext(Dispatchers.IO) {
            val agentIds = agents.map { it.id }.toSet()
            val agentByName = agents.associateBy { it.name }
            val all = conversationDao.getAllConversationsSync()
            all.forEach { entity ->
                val currentId = entity.assistantId
                if (reservedAssistantIds.contains(currentId)) return@forEach
                if (currentId.isBlank() || !agentIds.contains(currentId)) {
                    val mapped = agentByName[entity.roleName]?.id ?: fallbackAssistantId
                    if (mapped.isNotBlank() && mapped != currentId) {
                        conversationDao.updateConversation(entity.copy(assistantId = mapped))
                    }
                }
            }
        }
    }

    private fun ensureNotOnMainThread(apiName: String) {
        check(!Looper.getMainLooper().isCurrentThread) {
            "$apiName cannot run on the main thread. Use the corresponding async API instead."
        }
    }

    fun getConversationsFlow(assistantId: String): Flow<List<ConversationEntity>> =
        conversationDao.getConversationsByAssistant(assistantId)

    @WorkerThread
    fun getConversations(assistantId: String): List<Conversation> {
        ensureNotOnMainThread("getConversations")
        return conversationDao.getConversationsByAssistantSync(assistantId).map { entity ->
            Conversation(
                id = entity.id,
                assistantId = entity.assistantId,
                title = entity.title,
                lastMessage = entity.lastMessage,
                timestamp = entity.timestamp,
                roleName = entity.roleName,
                systemPrompt = entity.systemPrompt
            )
        }
    }

    suspend fun getConversationsAsync(assistantId: String): List<Conversation> = withContext(Dispatchers.IO) {
        conversationDao.getConversationsByAssistantSync(assistantId).map { entity ->
            Conversation(
                id = entity.id,
                assistantId = entity.assistantId,
                title = entity.title,
                lastMessage = entity.lastMessage,
                timestamp = entity.timestamp,
                roleName = entity.roleName,
                systemPrompt = entity.systemPrompt
            )
        }
    }

    fun getMessagesFlow(conversationId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesByConversationId(conversationId)

    @WorkerThread
    fun getMessages(conversationId: String, assistantId: String): List<Message> {
        ensureNotOnMainThread("getMessages")
        ensureConversationExists(conversationId, assistantId)
        return messageDao.getMessagesByConversationIdSync(conversationId).map { entity ->
            Message(
                id = entity.id,
                text = entity.text,
                isUser = entity.isUser,
                timestamp = entity.timestamp,
                showThink = entity.showThink,
                showDebugPrompt = entity.showDebugPrompt,
                debugPromptText = entity.debugPromptText,
                thinkMeta = entity.getThinkMeta(),
                imageUris = entity.getImageUris(),
                attachments = entity.getAttachments(),
                citations = entity.getCitations(),
                toolEvents = entity.getToolEvents()
            )
        }
    }

    suspend fun getMessagesAsync(conversationId: String, assistantId: String): List<Message> = withContext(Dispatchers.IO) {
        ensureConversationExists(conversationId, assistantId)
        messageDao.getMessagesByConversationIdSync(conversationId).map { entity ->
            Message(
                id = entity.id,
                text = entity.text,
                isUser = entity.isUser,
                timestamp = entity.timestamp,
                showThink = entity.showThink,
                showDebugPrompt = entity.showDebugPrompt,
                debugPromptText = entity.debugPromptText,
                thinkMeta = entity.getThinkMeta(),
                imageUris = entity.getImageUris(),
                attachments = entity.getAttachments(),
                citations = entity.getCitations(),
                toolEvents = entity.getToolEvents()
            )
        }
    }

    suspend fun searchMessagesAsync(conversationId: String, assistantId: String, query: String): List<Message> =
        withContext(Dispatchers.IO) {
        ensureConversationExists(conversationId, assistantId)
        messageDao.searchMessagesByConversationIdSync(conversationId, query).map { entity ->
            Message(
                id = entity.id,
                text = entity.text,
                isUser = entity.isUser,
                timestamp = entity.timestamp,
                showThink = entity.showThink,
                showDebugPrompt = entity.showDebugPrompt,
                debugPromptText = entity.debugPromptText,
                thinkMeta = entity.getThinkMeta(),
                imageUris = entity.getImageUris(),
                attachments = entity.getAttachments(),
                citations = entity.getCitations(),
                toolEvents = entity.getToolEvents()
            )
        }
    }

    suspend fun searchAllMessagesAsync(assistantId: String, query: String, limit: Int): List<MessageSearchResult> =
        withContext(Dispatchers.IO) {
        messageDao.searchAllMessagesByAssistantSync(assistantId, query, limit)
    }

    suspend fun searchAllMessagesAsync(query: String, limit: Int): List<MessageSearchResult> =
        withContext(Dispatchers.IO) {
        messageDao.searchAllMessagesSync(query, limit)
    }

    suspend fun getConversationEntityAsync(conversationId: String): ConversationEntity? = withContext(Dispatchers.IO) {
        conversationDao.getConversationById(conversationId)
    }
    
    suspend fun saveMessages(conversationId: String, assistantId: String, messages: List<Message>) {
        withContext(Dispatchers.IO) {
            val conversationExists = conversationDao.getAllConversations().first().any { it.id == conversationId }
            if (!conversationExists) {
                conversationDao.insertConversation(newDefaultConversation(conversationId, assistantId))
            }

            if (messages.isEmpty()) {
                messageDao.deleteMessagesByConversationId(conversationId)
                return@withContext
            }

            val existingIds = messageDao.getMessageIdsByConversationIdSync(conversationId).toSet()
            val newIds = messages.map { it.id }.toSet()
            val toDelete = existingIds.subtract(newIds).toList()
            if (toDelete.isNotEmpty()) {
                messageDao.deleteMessagesByIds(toDelete)
            }
            val entities = messages.map { message ->
                MessageEntity(
                    id = message.id,
                    conversationId = conversationId,
                    text = message.text,
                    isUser = message.isUser,
                    timestamp = message.timestamp,
                    showThink = message.showThink,
                    showDebugPrompt = message.showDebugPrompt,
                    debugPromptText = message.debugPromptText
                ).setThinkMeta(message.thinkMeta)
                    .setImageUris(message.imageUris)
                    .setAttachments(message.attachments)
                    .setCitations(message.citations)
                    .setToolEvents(message.toolEvents)
            }
            messageDao.upsertMessages(entities)
        }
    }

    suspend fun updateMessageShowThinkAsync(messageId: String, showThink: Boolean) {
        withContext(Dispatchers.IO) {
            messageDao.updateMessageShowThink(messageId, showThink)
        }
    }

    suspend fun createNewConversation(
        assistantId: String,
        title: String,
        roleName: String = "",
        systemPrompt: String = ""
    ): Conversation = withContext(Dispatchers.IO) {
        val newId = java.util.UUID.randomUUID().toString()
        val entity = ConversationEntity(
            id = newId,
            assistantId = assistantId,
            title = title,
            lastMessage = "",
            timestamp = Date(),
            roleName = roleName,
            systemPrompt = systemPrompt
        )
        conversationDao.insertConversation(entity)
        Conversation(
            id = newId,
            assistantId = assistantId,
            title = title,
            lastMessage = "",
            timestamp = entity.timestamp,
            roleName = roleName,
            systemPrompt = systemPrompt
        )
    }

    suspend fun deleteConversation(conversationId: String) {
        withContext(Dispatchers.IO) {
            messageDao.deleteMessagesByConversationId(conversationId)
            conversationDao.deleteConversationById(conversationId)
        }
    }

    suspend fun updateLastMessage(conversationId: String, lastMessage: String) {
        withContext(Dispatchers.IO) {
            val conversation = conversationDao.getConversationById(conversationId) ?: return@withContext
            conversationDao.updateConversation(
                conversation.copy(lastMessage = lastMessage, timestamp = Date())
            )
        }
    }

    suspend fun updateConversationTitle(conversationId: String, title: String) {
        withContext(Dispatchers.IO) {
            val conversation = conversationDao.getConversationById(conversationId) ?: return@withContext
            conversationDao.updateConversation(conversation.copy(title = title, timestamp = Date()))
        }
    }

    suspend fun getImageGenerationState(conversationId: String): ImageGenerationConversationState? =
        withContext(Dispatchers.IO) {
            val raw = conversationDao.getConversationById(conversationId)?.imageGenerationStateJson.orEmpty()
            if (raw.isBlank()) return@withContext null
            runCatching { Gson().fromJson(raw, ImageGenerationConversationState::class.java) }.getOrNull()
        }

    suspend fun updateImageGenerationState(conversationId: String, state: ImageGenerationConversationState?) {
        withContext(Dispatchers.IO) {
            val conversation = conversationDao.getConversationById(conversationId) ?: return@withContext
            conversationDao.updateConversation(
                conversation.copy(
                    imageGenerationStateJson = state?.let { Gson().toJson(it) }.orEmpty(),
                    timestamp = Date()
                )
            )
        }
    }

    @WorkerThread
    fun getConversationRoleName(conversationId: String): String {
        ensureNotOnMainThread("getConversationRoleName")
        return conversationDao.getConversationRoleNameById(conversationId).orEmpty()
    }

    suspend fun getConversationRoleNameAsync(conversationId: String): String = withContext(Dispatchers.IO) {
        conversationDao.getConversationRoleNameById(conversationId).orEmpty()
    }

    @WorkerThread
    fun getConversationSystemPrompt(conversationId: String): String {
        ensureNotOnMainThread("getConversationSystemPrompt")
        return conversationDao.getConversationSystemPromptById(conversationId).orEmpty()
    }

    suspend fun getConversationSystemPromptAsync(conversationId: String): String = withContext(Dispatchers.IO) {
        conversationDao.getConversationSystemPromptById(conversationId).orEmpty()
    }

    suspend fun updateConversationRole(conversationId: String, roleName: String, systemPrompt: String) {
        withContext(Dispatchers.IO) {
            val conversation = conversationDao.getConversationById(conversationId) ?: return@withContext
            conversationDao.updateConversation(
                conversation.copy(roleName = roleName, systemPrompt = systemPrompt, timestamp = Date())
            )
        }
    }

    private fun ensureConversationExists(conversationId: String, assistantId: String) {
        if (conversationDao.getConversationById(conversationId) == null) {
            conversationDao.insertConversation(newDefaultConversation(conversationId, assistantId))
        }
    }

    private fun newDefaultConversation(id: String, assistantId: String): ConversationEntity =
        ConversationEntity(
            id = id,
            assistantId = assistantId,
            title = "",
            lastMessage = "",
            timestamp = Date(),
            roleName = "",
            systemPrompt = ""
        )
}
