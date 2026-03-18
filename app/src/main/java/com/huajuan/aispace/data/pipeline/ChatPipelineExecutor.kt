package com.huajuan.aispace.data.pipeline

import com.huajuan.aispace.R
import com.huajuan.aispace.data.Agent
import com.huajuan.aispace.data.Message
import com.huajuan.aispace.data.ModelRequestResolver
import com.huajuan.aispace.data.Repository
import com.huajuan.aispace.data.RuntimeMcpConfig
import com.huajuan.aispace.data.KnowledgeContextPayload
import com.huajuan.aispace.data.WebCitation
import com.huajuan.aispace.data.WebSearchContextPayload
import com.huajuan.aispace.data.WebSearchMode
import com.huajuan.aispace.data.WebSearchPlan
import com.huajuan.aispace.data.model.ChatEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

internal class ChatPipelineExecutor(
    private val repository: Repository,
    private val mcpGateway: McpGateway = McpGateway()
) {
    internal enum class StreamStrategy {
        Default,
        CloudLowLatency
    }

    private val parameterBuilder = CherryParameterBuilder(repository)

    fun stream(
        messages: List<Message>,
        conversationId: String,
        systemPromptOverride: String?,
        debugScopeId: String?,
        strategy: StreamStrategy = StreamStrategy.Default,
        modelInvoker: (messages: List<Message>, conversationId: String, systemPromptOverride: String?, debugScopeId: String?) -> Flow<ChatEvent>
    ): Flow<ChatEvent> = flow {
        val userText = messages.lastOrNull { it.isUser }?.text.orEmpty().trim()
        val assistant = repository.getAssistantById(repository.getCurrentAssistantId())
        val params = parameterBuilder.build(assistant)
        val ctx = CherryRequestContext(
            messages = messages,
            conversationId = conversationId,
            userText = userText,
            assistant = assistant,
            params = params,
            debugScopeId = debugScopeId
        )
        val shouldRunSearchPreflight = when (strategy) {
            StreamStrategy.Default -> true
            StreamStrategy.CloudLowLatency -> explicitToolingRequiresPreflight(
                webSearchMode = params.webSearchPlan.mode,
                runtimeMcpEnabled = repository.getRuntimeMcpConfig()?.enabled == true,
                hasKnowledgeSelection = repository.getConversationKbSelection(conversationId).isNotEmpty()
            )
        }

        val plugins = listOf(
            SearchOrchestrationPlugin(repository, mcpGateway),
            MemoryWriteBackPlugin(repository)
        )

        plugins.forEach { plugin ->
            runCatching { plugin.onRequestStart(ctx) }
        }
        plugins.forEach { plugin ->
            runCatching {
                if (shouldRunPreflight(plugin, shouldRunSearchPreflight)) {
                    plugin.transformParams(ctx)
                }
            }
        }

        val systemPrompt = composeSystemPrompt(systemPromptOverride, conversationId, ctx)
        if (ctx.citations.isNotEmpty()) {
            emit(ChatEvent.SourcesResolved(ctx.citations.toList()))
        }
        emitModelStream(
            modelInvoker = modelInvoker,
            messages = ctx.messages,
            conversationId = conversationId,
            systemPrompt = systemPrompt,
            debugScopeId = debugScopeId,
            emitEvent = ::emit
        )

        plugins.forEach { plugin ->
            runCatching { plugin.onRequestEnd(ctx) }
        }
    }

    private suspend fun composeSystemPrompt(
        systemPromptOverride: String?,
        conversationId: String,
        ctx: CherryRequestContext
    ): String? {
        val base = systemPromptOverride ?: repository.getConversationSystemPromptAsync(conversationId)
        val reasoningDirective = if (ctx.params.enableReasoning) {
            val effort = ctx.params.reasoningEffort ?: "medium"
            repository.getContext().getString(R.string.chat_reasoning_directive, effort)
        } else {
            ""
        }
        return mergePromptBlocks(base, reasoningDirective, ctx.contextBlocks)
    }
}

internal fun ChatPipelineExecutor.StreamStrategy.shouldRunSearchPreflight(): Boolean {
    return this == ChatPipelineExecutor.StreamStrategy.Default
}

internal fun explicitToolingRequiresPreflight(
    webSearchMode: WebSearchMode,
    runtimeMcpEnabled: Boolean,
    hasKnowledgeSelection: Boolean
): Boolean {
    return webSearchMode == WebSearchMode.ExternalProvider || runtimeMcpEnabled || hasKnowledgeSelection
}

private fun shouldRunPreflight(
    plugin: CherryPlugin,
    shouldRunSearchPreflight: Boolean
): Boolean {
    return if (plugin is SearchOrchestrationPlugin) {
        shouldRunSearchPreflight
    } else {
        true
    }
}

internal fun mergePromptBlocks(
    base: String?,
    reasoningDirective: String,
    contextBlocks: List<String>
): String? {
    val blocks = mutableListOf<String>()
    if (!base.isNullOrBlank()) blocks += base
    if (reasoningDirective.isNotBlank()) blocks += reasoningDirective
    blocks += contextBlocks.filter { it.isNotBlank() }
    return blocks.joinToString("\n\n").ifBlank { null }
}

internal suspend fun emitModelStream(
    modelInvoker: (messages: List<Message>, conversationId: String, systemPromptOverride: String?, debugScopeId: String?) -> Flow<ChatEvent>,
    messages: List<Message>,
    conversationId: String,
    systemPrompt: String?,
    debugScopeId: String?,
    emitEvent: suspend (ChatEvent) -> Unit
) {
    modelInvoker(messages, conversationId, systemPrompt, debugScopeId).collect { event ->
        emitEvent(event)
    }
}

private data class CherryPipelineParams(
    val enableReasoning: Boolean,
    val reasoningEffort: String?,
    val webSearchPlan: WebSearchPlan,
    val enableKnowledgeSearch: Boolean,
    val enableMemorySearch: Boolean,
    val enableMcp: Boolean
)

private class CherryParameterBuilder(
    private val repository: Repository
) {
    companion object {
        const val TOOL_TIMEOUT_MS = 2_000L
    }

    fun build(assistant: Agent?): CherryPipelineParams {
        val requestSettings = repository.getAssistantRequestSettings()
        val resolved = ModelRequestResolver.resolve(repository)
        val modelHint = "${resolved.modelInfo.displayName} ${resolved.modelInfo.apiCode}".lowercase()
        val reasoningEnabledByModel = listOf("thinking", "reasoning", "o1", "o3", "r1", "deepseek").any { modelHint.contains(it) }
        val reasoningEffort = assistant?.reasoningEffort ?: requestSettings.reasoningEffort
        val webSearchPlan = repository.resolveWebSearchPlan()
        val enableKnowledgeSearch = assistant?.knowledgeRecognition != "off"
        val enableMemorySearch = repository.getGlobalMemoryEnabled() && assistant?.enableMemory == true
        val enableMcp = repository.getRuntimeMcpConfig()?.enabled == true &&
            repository.getMcpToolsEnabled() &&
            repository.getMcpServerUrls().isNotEmpty()
        return CherryPipelineParams(
            enableReasoning = reasoningEffort != null || reasoningEnabledByModel,
            reasoningEffort = reasoningEffort,
            webSearchPlan = webSearchPlan,
            enableKnowledgeSearch = enableKnowledgeSearch,
            enableMemorySearch = enableMemorySearch,
            enableMcp = enableMcp
        )
    }
}

private data class CherryRequestContext(
    var messages: List<Message>,
    val conversationId: String,
    val userText: String,
    val assistant: Agent?,
    val params: CherryPipelineParams,
    val debugScopeId: String?,
    val tools: LinkedHashMap<String, CherryTool> = linkedMapOf(),
    val contextBlocks: MutableList<String> = mutableListOf(),
    val citations: MutableList<WebCitation> = mutableListOf()
) {
    fun nextCitationIndex(): Int = citations.size + 1
}

private interface CherryPlugin {
    suspend fun onRequestStart(ctx: CherryRequestContext)
    suspend fun transformParams(ctx: CherryRequestContext)
    suspend fun onRequestEnd(ctx: CherryRequestContext)
}

private interface CherryTool {
    val name: String
    val source: String
    suspend fun execute(query: String, ctx: CherryRequestContext): CherryToolResult
}

private data class CherryToolResult(
    val success: Boolean,
    val preview: String,
    val promptContext: String,
    val citations: List<WebCitation> = emptyList()
)

private class SearchOrchestrationPlugin(
    private val repository: Repository,
    private val mcpGateway: McpGateway
) : CherryPlugin {
    override suspend fun onRequestStart(ctx: CherryRequestContext) {
        if (ctx.userText.isBlank()) return
    }

    override suspend fun transformParams(ctx: CherryRequestContext) {
        val kbIds = repository.getConversationKbSelection(ctx.conversationId)
        if (ctx.params.webSearchPlan.mode == WebSearchMode.ExternalProvider) {
            ctx.tools["builtin_web_search"] = BuiltinWebSearchTool(repository)
        }
        if (ctx.params.enableKnowledgeSearch && kbIds.isNotEmpty()) {
            ctx.tools["builtin_knowledge_search"] = BuiltinKnowledgeSearchTool(repository, kbIds)
        }
        if (ctx.params.enableMemorySearch) {
            ctx.tools["builtin_memory_search"] = BuiltinMemorySearchTool(repository)
        }
        if (ctx.params.enableMcp) {
            ctx.tools["builtin_mcp_search"] = BuiltinMcpSearchTool(repository, mcpGateway)
        }

        val query = ctx.userText
        if (query.isBlank()) return
        val preflightOrder = listOf(
            "builtin_knowledge_search",
            "builtin_web_search",
            "builtin_memory_search",
            "builtin_mcp_search"
        )
        val results = coroutineScope {
            preflightOrder.mapNotNull { toolName ->
                val tool = ctx.tools[toolName] ?: return@mapNotNull null
                async {
                    toolName to withTimeoutOrNull(CherryParameterBuilder.TOOL_TIMEOUT_MS) {
                        runCatching { tool.execute(query, ctx) }.getOrNull()
                    }
                }
            }.awaitAll().toMap()
        }
        preflightOrder.forEach { toolName ->
            val result = results[toolName] ?: return@forEach
            if (result.success && result.promptContext.isNotBlank()) {
                ctx.contextBlocks += result.promptContext
            }
            if (result.success && result.citations.isNotEmpty()) {
                ctx.citations += result.citations
            }
        }
    }

    override suspend fun onRequestEnd(ctx: CherryRequestContext) {
        // Search orchestration cleanup is handled by request-scoped context lifecycle.
    }
}

private class MemoryWriteBackPlugin(
    private val repository: Repository
) : CherryPlugin {
    override suspend fun onRequestStart(ctx: CherryRequestContext) = Unit

    override suspend fun transformParams(ctx: CherryRequestContext) = Unit

    override suspend fun onRequestEnd(ctx: CherryRequestContext) {
        if (!repository.getGlobalMemoryEnabled()) return
        val user = ctx.messages.lastOrNull { it.isUser }?.text?.trim().orEmpty()
        if (user.isBlank()) return
        MemoryShadowStore.addRecord(
            assistantId = repository.getCurrentAssistantId(),
            text = user
        )
    }
}

private object MemoryShadowStore {
    private val cache = ConcurrentHashMap<String, MutableList<Pair<Long, String>>>()

    fun addRecord(assistantId: String, text: String) {
        val list = cache.getOrPut(assistantId) { mutableListOf() }
        list += System.currentTimeMillis() to text
        if (list.size > 200) {
            list.subList(0, list.size - 200).clear()
        }
    }

    fun search(assistantId: String, query: String, limit: Int): List<String> {
        val list = cache[assistantId].orEmpty()
        if (query.isBlank()) return list.takeLast(limit).map { it.second }
        return list
            .asReversed()
            .filter { it.second.contains(query, ignoreCase = true) }
            .take(limit)
            .map { it.second }
    }
}

private class BuiltinWebSearchTool(
    private val repository: Repository
) : CherryTool {
    override val name: String = "builtin_web_search"
    override val source: String = "builtin"

    override suspend fun execute(query: String, ctx: CherryRequestContext): CherryToolResult {
        if (query.isBlank()) return CherryToolResult(false, "empty query", "")
        val providerId = ctx.params.webSearchPlan.providerId
            ?: return CherryToolResult(false, "no web search provider", "")
        val payload: WebSearchContextPayload = repository.buildWebSearchContext(
            query = query,
            providerId = providerId,
            startIndex = ctx.nextCitationIndex()
        )
        return if (payload.contextText.isNotBlank()) {
            CherryToolResult(
                success = true,
                preview = "citations=${payload.citations.size}",
                promptContext = payload.contextText,
                citations = payload.citations
            )
        } else if (payload.failureMessage.isNotBlank()) {
            CherryToolResult(success = false, preview = payload.failureMessage, promptContext = "")
        } else {
            CherryToolResult(success = true, preview = "no results", promptContext = "")
        }
    }
}

private class BuiltinKnowledgeSearchTool(
    private val repository: Repository,
    private val kbIds: List<String>
) : CherryTool {
    override val name: String = "builtin_knowledge_search"
    override val source: String = "builtin"

    override suspend fun execute(query: String, ctx: CherryRequestContext): CherryToolResult {
        if (query.isBlank() || kbIds.isEmpty()) return CherryToolResult(false, "no knowledge query", "")
        val payload: KnowledgeContextPayload = repository.buildKnowledgeContext(
            kbIds = kbIds,
            query = query,
            startIndex = ctx.nextCitationIndex()
        )
        return if (payload.contextText.isNotBlank()) {
            CherryToolResult(
                success = true,
                preview = "kb matched",
                promptContext = repository.getContext().getString(
                    R.string.chat_pipeline_knowledge_context_intro,
                    payload.contextText
                ),
                citations = payload.citations
            )
        } else {
            CherryToolResult(success = true, preview = "no results", promptContext = "")
        }
    }
}

private class BuiltinMemorySearchTool(
    private val repository: Repository
) : CherryTool {
    override val name: String = "builtin_memory_search"
    override val source: String = "builtin"

    override suspend fun execute(query: String, ctx: CherryRequestContext): CherryToolResult {
        val assistantId = repository.getCurrentAssistantId()
        val local = MemoryShadowStore.search(assistantId, query, 5)
        val history = runCatching { repository.searchAllMessagesAsync(query, 8) }
            .getOrDefault(emptyList())
            .map { it.messageText }
            .filter { it.isNotBlank() }
        val merged = (local + history).distinct().take(8)
        return if (merged.isEmpty()) {
            CherryToolResult(true, "no memory", "")
        } else {
            CherryToolResult(
                success = true,
                preview = "memory=${merged.size}",
                promptContext = buildString {
                    append(repository.getContext().getString(R.string.chat_pipeline_memory_context_intro))
                    append('\n')
                    merged.forEachIndexed { index, item ->
                        append(index + 1)
                        append(". ")
                        append(item.take(300))
                        append('\n')
                    }
                }.trim()
            )
        }
    }
}

private class BuiltinMcpSearchTool(
    private val repository: Repository,
    private val mcpGateway: McpGateway
) : CherryTool {
    override val name: String = "builtin_mcp_search"
    override val source: String = "mcp"

    override suspend fun execute(query: String, ctx: CherryRequestContext): CherryToolResult {
        if (query.isBlank()) return CherryToolResult(false, "empty query", "")
        val allowed = ctx.assistant?.allowedTools.orEmpty()
        val servers = repository.getMcpServerUrls().toList()
        for (server in servers) {
            val tools = runCatching { mcpGateway.listTools(server) }.getOrDefault(emptyList())
            if (tools.isEmpty()) continue
            val searchTool = tools.firstOrNull { tool ->
                if (allowed.isNotEmpty() && !allowed.contains(tool.name)) return@firstOrNull false
                val n = tool.name.lowercase()
                n.contains("search") || n.contains("web") || n.contains("query")
            } ?: continue
            val result = runCatching {
                mcpGateway.callTool(server, searchTool.name, mapOf("query" to query, "q" to query))
            }.getOrElse { e -> McpCallResult(false, e.message ?: "mcp call failed") }
            if (result.success && result.outputText.isNotBlank()) {
                return CherryToolResult(
                    success = true,
                    preview = "${searchTool.name} ok",
                    promptContext = repository.getContext().getString(
                        R.string.chat_pipeline_mcp_context_intro,
                        searchTool.name,
                        result.outputText
                    )
                )
            }
        }
        return CherryToolResult(success = true, preview = "no mcp result", promptContext = "")
    }
}
