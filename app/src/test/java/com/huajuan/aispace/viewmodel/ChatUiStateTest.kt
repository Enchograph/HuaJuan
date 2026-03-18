package com.huajuan.aispace.viewmodel

import com.huajuan.aispace.data.ChatState
import com.huajuan.aispace.data.Message
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.Date

class ChatUiStateTest {

    @Test
    fun `currentSession returns selected conversation state`() {
        val first = ConversationChatUiState(
            roleName = "A",
            chatState = ChatState(
                messages = listOf(
                    Message(text = "first", isUser = true, timestamp = Date())
                )
            ),
            isStreaming = true
        )
        val second = ConversationChatUiState(
            roleName = "B",
            chatState = ChatState(
                messages = listOf(
                    Message(text = "second", isUser = false, timestamp = Date())
                )
            ),
            isStreaming = false
        )

        val state = ChatUiState(
            sessions = mapOf(
                "conv-a" to first,
                "conv-b" to second
            ),
            currentConversationId = "conv-b"
        )

        assertEquals("B", state.currentSession.roleName)
        assertEquals("second", state.currentSession.chatState.messages.single().text)
        assertFalse(state.currentSession.isStreaming)
    }

    @Test
    fun `compatibility getters mirror current session`() {
        val session = ConversationChatUiState(
            roleName = "Assistant",
            systemPrompt = "prompt",
            chatState = ChatState(inputText = "hello"),
            isLoading = true
        )

        val state = ChatUiState(
            sessions = mapOf("conv" to session),
            currentConversationId = "conv"
        )

        assertEquals("Assistant", state.roleName)
        assertEquals("prompt", state.systemPrompt)
        assertEquals("hello", state.chatState.inputText)
        assertEquals(true, state.isLoading)
    }
}
