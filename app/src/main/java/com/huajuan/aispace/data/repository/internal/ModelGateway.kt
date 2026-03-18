package com.huajuan.aispace.data.repository.internal

import com.huajuan.aispace.data.ModelApiFactory
import com.huajuan.aispace.data.ModelRequestResolver
import com.huajuan.aispace.data.NetworkMessageBuilder
import com.huajuan.aispace.data.Repository
import com.huajuan.aispace.data.AttachmentPromptBuilder
import com.huajuan.aispace.data.model.ChatEvent
import com.huajuan.aispace.data.Message
import com.huajuan.aispace.data.ApiUrlResolver
import com.huajuan.aispace.data.ModelErrorParser
import com.huajuan.aispace.data.ModelSelectionValidator
import com.huajuan.aispace.data.pipeline.ChatPipelineExecutor
import com.huajuan.aispace.data.pipeline.ChatPipelineExecutor.StreamStrategy
import com.huajuan.aispace.network.ContentItem
import com.huajuan.aispace.network.Message as NetworkMessage
import com.huajuan.aispace.utils.debugLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

internal class ModelGateway(
    private val repository: Repository
) {
    private companion object {
        const val TAG = "ModelGateway"
    }
    private val pipelineExecutor = ChatPipelineExecutor(repository)

    fun getBaseUrl(): String = ApiUrlResolver.resolveChatUrl(repository) ?: ""

    suspend fun getAIResponse(
        messages: List<Message>,
        conversationId: String,
        debugScopeId: String? = null
    ): String {
        return try {
            if (ModelSelectionValidator.isModelMissing(repository)) {
                return ModelSelectionValidator.missingModelError()
            }
            if (ModelSelectionValidator.isApiKeyMissing(repository)) {
                return ModelSelectionValidator.missingApiKeyError()
            }
            val modelApiFactory = ModelApiFactory(repository)
            val modelApiService = modelApiFactory.getCurrentModelApiService()
            val resolved = ModelRequestResolver.resolve(repository)
            debugLog(TAG) {
                "getAIResponse provider='${resolved.serviceProvider}', selected='${resolved.selectedModelDisplayName}'"
            }
            debugLog(TAG) { "resolved model='${resolved.modelInfo.displayName}', apiCode='${resolved.modelInfo.apiCode}'" }

            val networkMessages = prepareNetworkMessages(
                messages = messages,
                conversationId = conversationId,
                systemPromptOverride = null,
                isImageGenerationService = resolved.isImageGenerationService
            )

            modelApiService.getAIResponse(networkMessages, resolved.modelInfo, debugScopeId)
        } catch (e: Exception) {
            ModelErrorParser.parseThrowable(e)
        }
    }

    fun streamAIResponse(
        messages: List<Message>,
        conversationId: String,
        debugScopeId: String? = null
    ): Flow<ChatEvent> {
        return streamAIResponse(messages, conversationId, null, debugScopeId)
    }

    fun streamAIResponse(
        messages: List<Message>,
        conversationId: String,
        systemPromptOverride: String?,
        debugScopeId: String? = null
    ): Flow<ChatEvent> {
        return flow {
            if (ModelSelectionValidator.isModelMissing(repository)) {
                emit(ChatEvent.Error(ModelSelectionValidator.missingModelError()))
                emit(ChatEvent.Done)
                return@flow
            }
            if (ModelSelectionValidator.isApiKeyMissing(repository)) {
                emit(ChatEvent.Error(ModelSelectionValidator.missingApiKeyError()))
                emit(ChatEvent.Done)
                return@flow
            }
            val resolved = ModelRequestResolver.resolve(repository)
            val requestSettings = repository.getAssistantRequestSettings()
            val streamStrategy = resolveStreamStrategy(
                useCloudModel = repository.getAssistantModelConfig(repository.getCurrentAssistantId()).useCloudModel,
                streamEnabled = requestSettings.streamEnabled
            )
            debugLog(TAG) {
                "streamAIResponse strategy='$streamStrategy', provider='${resolved.serviceProvider}', selected='${resolved.selectedModelDisplayName}'"
            }
            emitAll(
                pipelineExecutor.stream(
                    messages = messages,
                    conversationId = conversationId,
                    systemPromptOverride = systemPromptOverride,
                    debugScopeId = debugScopeId,
                    strategy = streamStrategy,
                    modelInvoker = ::streamModelOnly
                )
            )
        }
    }

    private fun streamModelOnly(
        messages: List<Message>,
        conversationId: String,
        systemPromptOverride: String?,
        debugScopeId: String?
    ): Flow<ChatEvent> = flow {
        val modelApiFactory = ModelApiFactory(repository)
        val modelApiService = modelApiFactory.getCurrentModelApiService()
        val resolved = ModelRequestResolver.resolve(repository)
        debugLog(TAG) {
            "streamModelOnly provider='${resolved.serviceProvider}', selected='${resolved.selectedModelDisplayName}'"
        }
        debugLog(TAG) { "resolved model='${resolved.modelInfo.displayName}', apiCode='${resolved.modelInfo.apiCode}'" }

        val networkMessages = prepareNetworkMessages(
            messages = messages,
            conversationId = conversationId,
            systemPromptOverride = systemPromptOverride,
            isImageGenerationService = resolved.isImageGenerationService
        )
        val requestSettings = repository.getAssistantRequestSettings()

        if (requestSettings.streamEnabled) {
            emitAll(modelApiService.streamAIResponse(networkMessages, resolved.modelInfo, debugScopeId))
        } else {
            val fullText = modelApiService.getAIResponse(networkMessages, resolved.modelInfo, debugScopeId)
            emit(ChatEvent.Chunk(fullText))
            emit(ChatEvent.Done)
        }
    }

    suspend fun buildModelInputTextSnapshot(
        messages: List<Message>,
        conversationId: String,
        systemPromptOverride: String?
    ): String {
        val resolved = ModelRequestResolver.resolve(repository)
        val networkMessages = prepareNetworkMessages(
            messages = messages,
            conversationId = conversationId,
            systemPromptOverride = systemPromptOverride,
            isImageGenerationService = resolved.isImageGenerationService
        )
        return buildString {
            networkMessages.forEachIndexed { index, message ->
                append('[')
                append(message.role)
                append("]\n")
                append(extractTextFromContent(message.content).ifBlank { "(empty)" })
                if (index != networkMessages.lastIndex) {
                    append("\n\n")
                }
            }
        }
    }

    private suspend fun prepareNetworkMessages(
        messages: List<Message>,
        conversationId: String,
        systemPromptOverride: String?,
        isImageGenerationService: Boolean
    ): List<NetworkMessage> {
        val systemPrompt = systemPromptOverride
            ?: repository.getConversationSystemPromptAsync(conversationId)
        val requestSettings = repository.getAssistantRequestSettings()
        val trimmedMessages = trimMessages(messages, requestSettings.contextMessageCount)
        val enrichedMessages = AttachmentPromptBuilder.enrichMessages(trimmedMessages, repository.getContext())
        return NetworkMessageBuilder.build(
            context = repository.getContext(),
            messages = enrichedMessages,
            systemPrompt = systemPrompt,
            isImageGenerationService = isImageGenerationService
        )
    }

    private fun extractTextFromContent(content: Any): String {
        return when (content) {
            is String -> content
            is List<*> -> {
                content.mapNotNull { item ->
                    when (item) {
                        is ContentItem -> if (item.type == "text") item.text else null
                        is Map<*, *> -> if (item["type"] == "text") item["text"] as? String else null
                        else -> null
                    }
                }.joinToString(" ")
            }
            else -> ""
        }
    }

    private fun trimMessages(messages: List<Message>, limit: Int): List<Message> {
        if (limit <= 0 || messages.size <= limit) return messages
        return messages.takeLast(limit)
    }

    private fun resolveStreamStrategy(
        useCloudModel: Boolean,
        streamEnabled: Boolean
    ): StreamStrategy {
        return if (useCloudModel && streamEnabled) {
            StreamStrategy.CloudLowLatency
        } else {
            StreamStrategy.Default
        }
    }
}
