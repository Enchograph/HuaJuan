package com.huajuan.aispace.data

import android.content.Context
import com.huajuan.aispace.utils.AttachmentUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AttachmentPromptBuilder {
    suspend fun enrichMessages(messages: List<Message>, context: Context): List<Message> = withContext(Dispatchers.IO) {
        messages.map { message ->
            if (message.attachments.isEmpty()) {
                message
            } else {
                val attachmentText = buildAttachmentsText(context, message.attachments)
                val merged = listOf(message.text, attachmentText).filter { it.isNotBlank() }.joinToString("\n\n")
                message.copy(text = merged)
            }
        }
    }

    private suspend fun buildAttachmentsText(context: Context, attachments: List<FileAttachment>): String {
        val filtered = attachments.filterNot { it.isImage }
        if (filtered.isEmpty()) return ""
        val blocks = filtered.map { attachment ->
            AttachmentUtils.buildAttachmentPrompt(context, attachment)
        }
        return blocks.joinToString("\n\n")
    }
}
