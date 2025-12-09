package com.chenhongyu.huajuan.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val lastMessage: String,
    val timestamp: Date,
    val roleName: String = "默认助手", // 添加角色名称字段，默认为"默认助手"
    val systemPrompt: String = "你是一个AI助手" // 添加系统提示词字段，默认为"你是一个AI助手"
)