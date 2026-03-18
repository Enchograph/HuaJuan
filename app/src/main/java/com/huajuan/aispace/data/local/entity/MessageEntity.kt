package com.huajuan.aispace.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Date,
    val showThink: Boolean = true,
    val showDebugPrompt: Boolean = false,
    val debugPromptText: String = "",
    val thinkMetaJson: String = "[]",
    val imageUrisJson: String = "[]", // 存储图片URI列表为JSON字符串
    val attachmentsJson: String = "[]", // 存储附件列表为JSON字符串
    val citationsJson: String = "[]",
    val toolEventsJson: String = "[]"
) {
    // 将JSON字符串转换为图片URI列表
    fun getImageUris(): List<String> {
        return try {
            val listType = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(imageUrisJson, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // 从图片URI列表创建JSON字符串
    fun setImageUris(uris: List<String>): MessageEntity {
        val json = Gson().toJson(uris)
        return this.copy(imageUrisJson = json)
    }

    fun getAttachments(): List<FileAttachment> {
        return try {
            val listType = object : TypeToken<List<FileAttachment>>() {}.type
            Gson().fromJson(attachmentsJson, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setAttachments(attachments: List<FileAttachment>): MessageEntity {
        val json = Gson().toJson(attachments)
        return this.copy(attachmentsJson = json)
    }

    fun getCitations(): List<WebCitation> {
        return try {
            val listType = object : TypeToken<List<WebCitation>>() {}.type
            Gson().fromJson(citationsJson, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setCitations(citations: List<WebCitation>): MessageEntity {
        val json = Gson().toJson(citations)
        return this.copy(citationsJson = json)
    }

    fun getToolEvents(): List<ToolExecutionRecord> {
        return try {
            val listType = object : TypeToken<List<ToolExecutionRecord>>() {}.type
            Gson().fromJson(toolEventsJson, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setToolEvents(events: List<ToolExecutionRecord>): MessageEntity {
        val json = Gson().toJson(events)
        return this.copy(toolEventsJson = json)
    }

    fun getThinkMeta(): List<ThinkMeta> {
        return try {
            val listType = object : TypeToken<List<ThinkMeta>>() {}.type
            Gson().fromJson(thinkMetaJson, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setThinkMeta(thinkMeta: List<ThinkMeta>): MessageEntity {
        val json = Gson().toJson(thinkMeta)
        return this.copy(thinkMetaJson = json)
    }
}
