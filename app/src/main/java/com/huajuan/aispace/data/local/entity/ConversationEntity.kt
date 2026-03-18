package com.huajuan.aispace.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val assistantId: String = "default", // 对话隶属的助手ID
    val title: String,
    val lastMessage: String,
    val timestamp: Date,
    val roleName: String = "", // 添加角色名称字段
    val systemPrompt: String = "", // 添加系统提示词字段
    val imageGenerationStateJson: String = ""
)
