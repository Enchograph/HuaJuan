package com.huajuan.aispace.data.model

import com.huajuan.aispace.data.WebCitation

sealed class ChatEvent {
    data class Chunk(val text: String) : ChatEvent()
    data class SourcesResolved(val citations: List<WebCitation>) : ChatEvent()
    data class ToolStart(
        val id: String,
        val name: String,
        val source: String,
        val inputPreview: String = ""
    ) : ChatEvent()
    data class ToolResult(
        val id: String,
        val name: String,
        val source: String,
        val outputPreview: String = ""
    ) : ChatEvent()
    data class ToolError(
        val id: String,
        val name: String,
        val source: String,
        val error: String
    ) : ChatEvent()
    data class Error(val message: String) : ChatEvent()
    object Done : ChatEvent()
}

