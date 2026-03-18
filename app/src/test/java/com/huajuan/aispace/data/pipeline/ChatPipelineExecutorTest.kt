package com.huajuan.aispace.data.pipeline

import com.huajuan.aispace.data.Message
import com.huajuan.aispace.data.WebCitation
import com.huajuan.aispace.data.WebSearchMode
import com.huajuan.aispace.data.model.ChatEvent
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

class ChatPipelineExecutorTest {
    @Test
    fun `default stream strategy keeps search preflight enabled`() {
        assertEquals(true, ChatPipelineExecutor.StreamStrategy.Default.shouldRunSearchPreflight())
    }

    @Test
    fun `cloud low latency strategy disables search preflight`() {
        assertEquals(false, ChatPipelineExecutor.StreamStrategy.CloudLowLatency.shouldRunSearchPreflight())
    }

    @Test
    fun `explicit tooling requires preflight when web search is enabled`() {
        assertEquals(
            true,
            explicitToolingRequiresPreflight(
                webSearchMode = WebSearchMode.ExternalProvider,
                runtimeMcpEnabled = false,
                hasKnowledgeSelection = false
            )
        )
    }

    @Test
    fun `model native web search does not require external preflight`() {
        assertEquals(
            false,
            explicitToolingRequiresPreflight(
                webSearchMode = WebSearchMode.ModelNative,
                runtimeMcpEnabled = false,
                hasKnowledgeSelection = false
            )
        )
    }

    @Test
    fun `explicit tooling requires preflight when mcp is enabled`() {
        assertEquals(
            true,
            explicitToolingRequiresPreflight(
                webSearchMode = WebSearchMode.Disabled,
                runtimeMcpEnabled = true,
                hasKnowledgeSelection = false
            )
        )
    }

    @Test
    fun `explicit tooling requires preflight when knowledge is selected`() {
        assertEquals(
            true,
            explicitToolingRequiresPreflight(
                webSearchMode = WebSearchMode.Disabled,
                runtimeMcpEnabled = false,
                hasKnowledgeSelection = true
            )
        )
    }

    @Test
    fun `cloud low latency keeps preflight disabled without explicit tooling`() {
        assertEquals(
            false,
            explicitToolingRequiresPreflight(
                webSearchMode = WebSearchMode.Disabled,
                runtimeMcpEnabled = false,
                hasKnowledgeSelection = false
            )
        )
    }

    @Test
    fun `mergePromptBlocks joins non blank sections in order`() {
        val merged = mergePromptBlocks(
            base = "base",
            reasoningDirective = "reasoning",
            contextBlocks = listOf("", "knowledge", "memory")
        )

        assertEquals("base\n\nreasoning\n\nknowledge\n\nmemory", merged)
    }

    @Test
    fun `mergePromptBlocks returns null when all sections blank`() {
        val merged = mergePromptBlocks(
            base = " ",
            reasoningDirective = "",
            contextBlocks = listOf("", "   ")
        )

        assertNull(merged)
    }

    @Test
    fun `emitModelStream forwards chunks immediately instead of buffering`() = runBlocking {
        val delivered = mutableListOf<String>()
        val messages = listOf(
            Message(
                text = "根据知识库回答",
                isUser = true,
                timestamp = Date()
            )
        )

        emitModelStream(
            modelInvoker = { _, _, _, _ ->
                flow {
                    emit(
                        ChatEvent.SourcesResolved(
                            listOf(
                                WebCitation(
                                    index = 1,
                                    title = "Doc",
                                    url = "file:///doc.md",
                                    sourceType = "knowledge"
                                )
                            )
                        )
                    )
                    emit(ChatEvent.Chunk("A"))
                    assertEquals(listOf("<sources>", "A"), delivered)
                    emit(ChatEvent.Chunk("B"))
                    assertEquals(listOf("<sources>", "A", "B"), delivered)
                    emit(ChatEvent.Done)
                }
            },
            messages = messages,
            conversationId = "conv",
            systemPrompt = "knowledge context",
            debugScopeId = null,
            emitEvent = { event ->
                when (event) {
                    is ChatEvent.SourcesResolved -> delivered += "<sources>"
                    is ChatEvent.Chunk -> delivered += event.text
                    ChatEvent.Done -> delivered += "<done>"
                    else -> Unit
                }
            }
        )

        assertEquals(listOf("<sources>", "A", "B", "<done>"), delivered)
    }
}
