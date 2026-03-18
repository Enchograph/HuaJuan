package com.huajuan.aispace.data

import java.util.Date
import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Date,
    var showThink: Boolean = true,
    val showDebugPrompt: Boolean = false,
    val debugPromptText: String = "",
    val thinkMeta: List<ThinkMeta> = emptyList(),
    val imageUris: List<String> = emptyList(), // 添加图片URI列表
    val attachments: List<FileAttachment> = emptyList(), // 文件附件
    val citations: List<WebCitation> = emptyList(),
    val toolEvents: List<ToolExecutionRecord> = emptyList()
)

data class ToolExecutionRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val source: String,
    val status: String,
    val inputPreview: String = "",
    val outputPreview: String = "",
    val timestamp: Date = Date()
)

data class WebCitation(
    val index: Int,
    val title: String,
    val url: String,
    val snippet: String = "",
    val sourceType: String? = "web"
)

data class WebSearchResultItem(
    val title: String,
    val content: String,
    val url: String
)

data class WebSearchProviderResponse(
    val query: String,
    val results: List<WebSearchResultItem>
)

data class WebSearchContextPayload(
    val contextText: String,
    val citations: List<WebCitation>,
    val failureMessage: String = ""
)

data class KnowledgeContextPayload(
    val contextText: String,
    val citations: List<WebCitation>
)

data class RuntimeWebSearchConfig(
    val enabled: Boolean = false,
    val providerId: String = WebSearchProviderIds.Tavily
)

data class RuntimeMcpConfig(
    val enabled: Boolean = false
)

data class ThinkMeta(
    val index: Int,
    val label: String,
    val startAtMs: Long? = null,
    val endAtMs: Long? = null,
    val durationSec: Int? = null,
    val isClosed: Boolean = false
)

data class FileAttachment(
    val id: String = UUID.randomUUID().toString(),
    val uri: String,
    val localPath: String? = null,
    val displayName: String,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val isImage: Boolean = false
)

data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val assistantId: String = "default",
    val title: String,
    val lastMessage: String,
    val timestamp: Date,
    val roleName: String = "",
    val systemPrompt: String = ""
)

data class LocalModel(
    val id: String,
    val name: String,
    val size: String,
    val isDownloaded: Boolean = false,
    val downloadProgress: Int = 0
)

data class AppState(
    val conversations: List<Conversation> = listOf(),
    val currentConversationId: String? = null,
    val currentAssistantId: String? = null,
    val currentTranslationConversationId: String? = null,
    val currentImageGenerationConversationId: String? = null
)

data class ChatState(
    val messages: List<Message> = listOf(),
    val inputText: String = ""
)

data class MessageSearchResult(
    val messageId: String,
    val conversationId: String,
    val assistantId: String,
    val conversationTitle: String,
    val messageText: String,
    val timestamp: Date
)

data class AssistantModelConfig(
    val useCloudModel: Boolean,
    val serviceProvider: String,
    val selectedModelName: String,
    val localSelectedModelName: String
)

data class AssistantRequestSettings(
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val enableTemperature: Boolean = true,
    val enableTopP: Boolean = true,
    val contextMessageCount: Int = 12,
    val maxTokensEnabled: Boolean = false,
    val maxTokens: Int = 1024,
    val reasoningEffort: String? = null,
    val streamEnabled: Boolean = true,
    val toolCallMode: String = "function",
    val maxToolSteps: Int = 20,
    val customParams: String = ""
)
