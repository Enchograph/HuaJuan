package com.huajuan.aispace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.huajuan.aispace.data.ChatState
import com.huajuan.aispace.data.FileAttachment
import com.huajuan.aispace.data.Message
import com.huajuan.aispace.data.RuntimeMcpConfig
import com.huajuan.aispace.data.RuntimeWebSearchConfig
import com.huajuan.aispace.data.ThinkMeta
import com.huajuan.aispace.data.ToolExecutionRecord
import com.huajuan.aispace.data.WebCitation
import com.huajuan.aispace.data.model.ChatEvent
import com.huajuan.aispace.data.repository.ChatRepository
import com.huajuan.aispace.utils.PreviewTextUtils
import com.huajuan.aispace.utils.ThinkTagProcessor
import com.huajuan.aispace.utils.debugLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.util.Date
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class ConversationChatUiState(
    val assistantId: String? = null,
    val roleName: String = "",
    val systemPrompt: String = "",
    val chatState: ChatState = ChatState(),
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val isStreaming: Boolean = false,
    val streamingMessageId: String? = null
)

data class ChatUiState(
    val sessions: Map<String, ConversationChatUiState> = emptyMap(),
    val currentConversationId: String? = null
) {
    val currentSession: ConversationChatUiState
        get() = currentConversationId?.let { sessions[it] } ?: ConversationChatUiState()
    val roleName: String
        get() = currentSession.roleName
    val systemPrompt: String
        get() = currentSession.systemPrompt
    val chatState: ChatState
        get() = currentSession.chatState
    val isLoading: Boolean
        get() = currentSession.isLoading
}

sealed interface ChatViewEvent {
    data class Toast(val message: String) : ChatViewEvent
}

class ChatViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {
    private companion object {
        const val TAG = "ChatViewModel"
    }

    private data class ActiveStream(
        val job: Job,
        val assistantId: String,
        val assistantMessageId: String,
        val onConversationListRefresh: suspend (String) -> Unit
    )

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ChatViewEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<ChatViewEvent> = _events.asSharedFlow()

    private val dbMutex = Mutex()
    private val activeStreams = ConcurrentHashMap<String, ActiveStream>()
    private val stopRequested = ConcurrentHashMap.newKeySet<String>()

    fun bindConversation(conversationId: String, assistantId: String) {
        _uiState.update { state ->
            state.copy(
                currentConversationId = conversationId,
                sessions = state.sessions.ensureSession(
                    conversationId = conversationId,
                    assistantId = assistantId
                )
            )
        }
        val session = _uiState.value.sessions[conversationId]
        if (session == null || session.assistantId != assistantId || !session.hasLoaded) {
            loadConversation(conversationId, assistantId)
        }
    }

    fun loadConversation(conversationId: String, assistantId: String) {
        viewModelScope.launch {
            updateSession(conversationId) { session ->
                session.copy(assistantId = assistantId, isLoading = true)
            }
            val messages = chatRepository.getMessagesAsync(conversationId, assistantId)
            val roleName = chatRepository.getConversationRoleNameAsync(conversationId)
            val systemPrompt = chatRepository.getConversationSystemPromptAsync(conversationId)
            updateSession(conversationId) { session ->
                session.copy(
                    assistantId = assistantId,
                    roleName = roleName,
                    systemPrompt = systemPrompt,
                    chatState = session.chatState.copy(messages = messages),
                    isLoading = false,
                    hasLoaded = true
                )
            }
        }
    }

    fun updateInputText(conversationId: String, text: String) {
        updateSession(conversationId) { session ->
            session.copy(chatState = session.chatState.copy(inputText = text))
        }
    }

    fun updateInputText(text: String) {
        val conversationId = _uiState.value.currentConversationId ?: return
        updateInputText(conversationId, text)
    }

    fun replaceMessages(conversationId: String, messages: List<Message>) {
        updateSession(conversationId) { session ->
            session.copy(chatState = session.chatState.copy(messages = messages))
        }
    }

    fun setMessages(messages: List<Message>) {
        val conversationId = _uiState.value.currentConversationId ?: return
        replaceMessages(conversationId, messages)
    }

    fun resetInput(conversationId: String) {
        updateSession(conversationId) { session ->
            session.copy(chatState = session.chatState.copy(inputText = ""))
        }
    }

    fun resetInput() {
        val conversationId = _uiState.value.currentConversationId ?: return
        resetInput(conversationId)
    }

    fun updateMessageThinkVisibility(conversationId: String, messageId: String, showThink: Boolean) {
        val updated = sessionFor(conversationId).chatState.messages.map { message ->
            if (message.id == messageId) message.copy(showThink = showThink) else message
        }
        replaceMessages(conversationId, updated)
        viewModelScope.launch {
            chatRepository.updateMessageShowThinkAsync(messageId, showThink)
        }
    }

    fun sendMessage(
        conversationId: String,
        assistantId: String,
        text: String,
        imageUris: List<String>,
        attachments: List<FileAttachment>,
        sentImagesAndFilesLabel: String,
        sentImagesLabel: String,
        sentFilesLabel: String,
        webSearchConfig: RuntimeWebSearchConfig?,
        mcpConfig: RuntimeMcpConfig?,
        formatStreamError: (String) -> String,
        formatReplyFailed: (String) -> String,
        formatToastFailed: (String) -> String,
        onConversationListRefresh: suspend (String) -> Unit
    ): Boolean {
        val session = sessionFor(conversationId)
        if (session.isStreaming) return false
        if (text.isBlank() && imageUris.isEmpty() && attachments.isEmpty()) return false

        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            text = text,
            isUser = true,
            timestamp = Date(),
            imageUris = imageUris,
            attachments = attachments
        )
        val aiMessageId = UUID.randomUUID().toString()
        val aiMessage = Message(
            id = aiMessageId,
            text = "",
            isUser = false,
            timestamp = Date(),
            showThink = false
        )
        val updatedMessages = session.chatState.messages + userMessage + aiMessage
        updateSession(conversationId) { current ->
            current.copy(
                assistantId = assistantId,
                chatState = current.chatState.copy(
                    messages = updatedMessages,
                    inputText = ""
                ),
                isStreaming = true,
                streamingMessageId = aiMessageId,
                hasLoaded = true
            )
        }

        val job = viewModelScope.launch {
            val debugScopeId = if (chatRepository.getDebugMode()) userMessage.id else null
            val requestStartedAtMs = System.currentTimeMillis()
            debugLog(TAG) { "stream[$conversationId] send entered" }
            chatRepository.beginDebugRequestCapture(debugScopeId)
            chatRepository.setRuntimeWebSearchConfig(webSearchConfig)
            chatRepository.setRuntimeMcpConfig(mcpConfig)

            launch {
                runCatching {
                    persistMessages(conversationId, assistantId, updatedMessages)
                    updateLastMessage(
                        conversationId = conversationId,
                        assistantId = assistantId,
                        previewText = userPreviewText(
                            text = text,
                            imageUris = imageUris,
                            attachments = attachments,
                            sentImagesAndFilesLabel = sentImagesAndFilesLabel,
                            sentImagesLabel = sentImagesLabel,
                            sentFilesLabel = sentFilesLabel
                        )
                    )
                    onConversationListRefresh(assistantId)
                }.onFailure { error ->
                    debugLog(TAG) { "stream[$conversationId] initial persistence failed: ${error.message.orEmpty()}" }
                }
            }

            suspend fun appendRequestDebugSnapshotIfAny() {
                if (!chatRepository.getDebugMode()) return
                val requestSnapshot = chatRepository.consumeDebugRequestSnapshot(debugScopeId)
                if (requestSnapshot.isBlank()) return
                val nextMessages = sessionFor(conversationId).chatState.messages.map { message ->
                    if (message.id == userMessage.id) {
                        message.copy(
                            debugPromptText = requestSnapshot,
                            showDebugPrompt = true
                        )
                    } else {
                        message
                    }
                }
                replaceMessages(conversationId, nextMessages)
                persistMessages(conversationId, assistantId, nextMessages)
            }

            try {
                if (chatRepository.getDebugMode()) {
                    val withDebug = sessionFor(conversationId).chatState.messages.map { message ->
                        if (message.id == userMessage.id) message.copy(showDebugPrompt = true) else message
                    }
                    replaceMessages(conversationId, withDebug)
                    launch {
                        runCatching { persistMessages(conversationId, assistantId, withDebug) }
                            .onFailure { error ->
                                debugLog(TAG) { "stream[$conversationId] debug persistence failed: ${error.message.orEmpty()}" }
                            }
                    }
                }

                var citationsForThisTurn = emptyList<WebCitation>()
                var firstChunkLogged = false
                chatRepository.streamAIResponse(
                    messages = updatedMessages.dropLast(1),
                    conversationId = conversationId,
                    debugScopeId = debugScopeId
                ).collect { event ->
                    when (event) {
                        is ChatEvent.Chunk -> {
                            if (!firstChunkLogged) {
                                firstChunkLogged = true
                                val elapsed = System.currentTimeMillis() - requestStartedAtMs
                                debugLog(TAG) { "stream[$conversationId] first chunk after ${elapsed}ms" }
                            }
                            applyChunk(conversationId, aiMessageId, event.text)
                        }
                        is ChatEvent.SourcesResolved -> {
                            citationsForThisTurn = event.citations
                        }
                        is ChatEvent.ToolStart -> {
                            val now = Date()
                            mutateAssistantMessage(conversationId, aiMessageId) { message ->
                                val filtered = message.toolEvents.filterNot { it.id == event.id }
                                message.copy(
                                    toolEvents = filtered + ToolExecutionRecord(
                                        id = event.id,
                                        name = event.name,
                                        source = event.source,
                                        status = "running",
                                        inputPreview = event.inputPreview,
                                        outputPreview = "",
                                        timestamp = now
                                    )
                                )
                            }
                        }
                        is ChatEvent.ToolResult -> {
                            val now = Date()
                            mutateAssistantMessage(conversationId, aiMessageId) { message ->
                                val existing = message.toolEvents.find { it.id == event.id }
                                val filtered = message.toolEvents.filterNot { it.id == event.id }
                                message.copy(
                                    toolEvents = filtered + ToolExecutionRecord(
                                        id = event.id,
                                        name = event.name,
                                        source = event.source,
                                        status = "success",
                                        inputPreview = existing?.inputPreview.orEmpty(),
                                        outputPreview = event.outputPreview,
                                        timestamp = now
                                    )
                                )
                            }
                        }
                        is ChatEvent.ToolError -> {
                            val now = Date()
                            mutateAssistantMessage(conversationId, aiMessageId) { message ->
                                val existing = message.toolEvents.find { it.id == event.id }
                                val filtered = message.toolEvents.filterNot { it.id == event.id }
                                message.copy(
                                    toolEvents = filtered + ToolExecutionRecord(
                                        id = event.id,
                                        name = event.name,
                                        source = event.source,
                                        status = "error",
                                        inputPreview = existing?.inputPreview.orEmpty(),
                                        outputPreview = event.error,
                                        timestamp = now
                                    )
                                )
                            }
                        }
                        is ChatEvent.Error -> {
                            val errorText = formatStreamError(event.message)
                            mutateAssistantMessage(conversationId, aiMessageId) { message ->
                                message.copy(text = errorText)
                            }
                            appendRequestDebugSnapshotIfAny()
                            finishStream(conversationId)
                            persistMessages(conversationId, assistantId, sessionFor(conversationId).chatState.messages)
                        }
                        is ChatEvent.Done -> {
                            val aiText = sessionFor(conversationId)
                                .chatState
                                .messages
                                .firstOrNull { it.id == aiMessageId }
                                ?.text
                                .orEmpty()
                            val imageUrls = chatRepository.extractImageUrlsFromResponse(aiText)
                            val cleanedText = if (imageUrls.isNotEmpty()) {
                                chatRepository.extractAndCleanImageUrls(aiText).second
                            } else {
                                aiText
                            }

                            mutateAssistantMessage(conversationId, aiMessageId) { message ->
                                val hasThink = ThinkTagProcessor.containsThinkTag(cleanedText)
                                message.copy(
                                    text = cleanedText,
                                    showThink = if (hasThink) false else message.showThink,
                                    thinkMeta = reconcileThinkMeta(
                                        oldMeta = message.thinkMeta,
                                        content = cleanedText,
                                        fallbackStartAtMs = message.timestamp.time
                                    ),
                                    citations = citationsForThisTurn
                                )
                            }
                            appendRequestDebugSnapshotIfAny()
                            finishStream(conversationId)
                            val finalMessages = sessionFor(conversationId).chatState.messages
                            persistMessages(conversationId, assistantId, finalMessages)
                            updateLastMessage(
                                conversationId = conversationId,
                                assistantId = assistantId,
                                previewText = PreviewTextUtils.stripMarkdown(
                                    ThinkTagProcessor.firstTextSegment(cleanedText)
                                )
                            )
                            onConversationListRefresh(assistantId)

                            if (imageUrls.isNotEmpty()) {
                                val downloadedImageUris = chatRepository.processAIImageResponse(aiText).first
                                if (downloadedImageUris.isNotEmpty()) {
                                    mutateAssistantMessage(conversationId, aiMessageId) { message ->
                                        message.copy(imageUris = downloadedImageUris)
                                    }
                                    persistMessages(
                                        conversationId,
                                        assistantId,
                                        sessionFor(conversationId).chatState.messages
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (_: CancellationException) {
                if (stopRequested.remove(conversationId)) {
                    finalizeInterruptedAssistantMessage(conversationId, aiMessageId)
                    finishStream(conversationId)
                    val finalMessages = sessionFor(conversationId).chatState.messages
                    persistMessages(conversationId, assistantId, finalMessages)
                    val partialText = finalMessages.firstOrNull { it.id == aiMessageId }?.text.orEmpty()
                    updateLastMessage(
                        conversationId = conversationId,
                        assistantId = assistantId,
                        previewText = PreviewTextUtils.stripMarkdown(
                            ThinkTagProcessor.firstTextSegment(partialText)
                        )
                    )
                    onConversationListRefresh(assistantId)
                }
            } catch (e: Exception) {
                finishStream(conversationId)
                mutateAssistantMessage(conversationId, aiMessageId) { message ->
                    message.copy(text = formatReplyFailed(e.message.orEmpty()))
                }
                appendRequestDebugSnapshotIfAny()
                persistMessages(conversationId, assistantId, sessionFor(conversationId).chatState.messages)
                _events.tryEmit(ChatViewEvent.Toast(formatToastFailed(e.message.orEmpty())))
            } finally {
                chatRepository.setRuntimeWebSearchConfig(null)
                chatRepository.setRuntimeMcpConfig(null)
                chatRepository.endDebugRequestCapture(debugScopeId)
                activeStreams.remove(conversationId)
            }
        }

        activeStreams[conversationId] = ActiveStream(
            job = job,
            assistantId = assistantId,
            assistantMessageId = aiMessageId,
            onConversationListRefresh = onConversationListRefresh
        )
        return true
    }

    fun stopMessage(conversationId: String) {
        val activeStream = activeStreams[conversationId] ?: return
        finalizeInterruptedAssistantMessage(conversationId, activeStream.assistantMessageId)
        finishStream(conversationId)
        stopRequested += conversationId
        activeStream.job.cancel()
    }

    fun removeConversation(conversationId: String) {
        activeStreams.remove(conversationId)?.job?.cancel()
        stopRequested.remove(conversationId)
        _uiState.update { state ->
            state.copy(
                sessions = state.sessions - conversationId,
                currentConversationId = if (state.currentConversationId == conversationId) null else state.currentConversationId
            )
        }
    }

    private fun finishStream(conversationId: String) {
        updateSession(conversationId) { session ->
            session.copy(isStreaming = false, streamingMessageId = null)
        }
    }

    private suspend fun persistMessages(
        conversationId: String,
        assistantId: String,
        messages: List<Message>
    ) {
        dbMutex.lock()
        try {
            chatRepository.saveMessages(conversationId, assistantId, messages)
        } finally {
            dbMutex.unlock()
        }
    }

    private suspend fun updateLastMessage(
        conversationId: String,
        assistantId: String,
        previewText: String
    ) {
        dbMutex.lock()
        try {
            chatRepository.updateLastMessage(conversationId, previewText)
        } finally {
            dbMutex.unlock()
        }
    }

    private fun applyChunk(conversationId: String, assistantMessageId: String, chunk: String) {
        mutateAssistantMessage(conversationId, assistantMessageId) { message ->
            val newText = message.text + chunk
            message.copy(
                text = newText,
                thinkMeta = reconcileThinkMeta(
                    oldMeta = message.thinkMeta,
                    content = newText,
                    fallbackStartAtMs = message.timestamp.time
                )
            )
        }
    }

    private fun finalizeInterruptedAssistantMessage(conversationId: String, assistantMessageId: String) {
        mutateAssistantMessage(conversationId, assistantMessageId) { message ->
            val finalizedText = closeUnclosedThinkSegment(message.text)
            message.copy(
                text = finalizedText,
                thinkMeta = reconcileThinkMeta(
                    oldMeta = message.thinkMeta,
                    content = finalizedText,
                    fallbackStartAtMs = message.timestamp.time
                )
            )
        }
    }

    private fun mutateAssistantMessage(
        conversationId: String,
        assistantMessageId: String,
        transform: (Message) -> Message
    ) {
        updateSession(conversationId) { session ->
            session.copy(
                chatState = session.chatState.copy(
                    messages = session.chatState.messages.map { message ->
                        if (message.id == assistantMessageId) transform(message) else message
                    }
                )
            )
        }
    }

    private fun sessionFor(conversationId: String): ConversationChatUiState {
        return _uiState.value.sessions[conversationId] ?: ConversationChatUiState()
    }

    private fun updateSession(
        conversationId: String,
        transform: (ConversationChatUiState) -> ConversationChatUiState
    ) {
        _uiState.update { state ->
            val current = state.sessions[conversationId] ?: ConversationChatUiState()
            state.copy(
                sessions = state.sessions + (conversationId to transform(current))
            )
        }
    }

    private fun Map<String, ConversationChatUiState>.ensureSession(
        conversationId: String,
        assistantId: String
    ): Map<String, ConversationChatUiState> {
        val existing = this[conversationId]
        if (existing != null && existing.assistantId == assistantId) return this
        return this + (conversationId to (existing ?: ConversationChatUiState()).copy(assistantId = assistantId))
    }

    class Factory(
        private val chatRepository: ChatRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                return ChatViewModel(chatRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

private fun userPreviewText(
    text: String,
    imageUris: List<String>,
    attachments: List<FileAttachment>,
    sentImagesAndFilesLabel: String,
    sentImagesLabel: String,
    sentFilesLabel: String
): String {
    return when {
        text.isNotBlank() -> text
        imageUris.isNotEmpty() && attachments.isNotEmpty() -> sentImagesAndFilesLabel
        imageUris.isNotEmpty() -> sentImagesLabel
        attachments.isNotEmpty() -> sentFilesLabel
        else -> ""
    }
}

private fun reconcileThinkMeta(
    oldMeta: List<ThinkMeta>,
    content: String,
    fallbackStartAtMs: Long? = null,
    nowMs: Long = System.currentTimeMillis()
): List<ThinkMeta> {
    val existingByIndex = oldMeta.associateBy { it.index }
    val thinkSegments = ThinkTagProcessor
        .splitToSegments(content)
        .filterIsInstance<ThinkTagProcessor.RenderSegment.ThinkSegment>()
    return thinkSegments.map { segment ->
        val old = existingByIndex[segment.index]
        val startAt = old?.startAtMs ?: when {
            segment.isClosed && fallbackStartAtMs != null -> fallbackStartAtMs
            else -> nowMs
        }
        val isClosed = segment.isClosed
        val endAt = if (isClosed) (old?.endAtMs ?: nowMs) else null
        val durationSec = if (isClosed) {
            old?.durationSec ?: run {
                val elapsedMs = ((endAt ?: nowMs) - startAt).coerceAtLeast(0L)
                if (elapsedMs == 0L) 0 else ((elapsedMs + 999L) / 1000L).toInt()
            }
        } else {
            null
        }
        ThinkMeta(
            index = segment.index,
            label = segment.label,
            startAtMs = startAt,
            endAtMs = endAt,
            durationSec = durationSec,
            isClosed = isClosed
        )
    }
}

private fun closeUnclosedThinkSegment(content: String): String {
    if (content.isBlank()) return content
    val openTags = "<think>".toRegex(RegexOption.IGNORE_CASE).findAll(content).count()
    val closedTags = "</think>".toRegex(RegexOption.IGNORE_CASE).findAll(content).count()
    return if (openTags > closedTags) {
        content + "</think>"
    } else {
        content
    }
}
