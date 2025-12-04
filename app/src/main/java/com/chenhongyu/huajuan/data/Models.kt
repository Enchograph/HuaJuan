package com.chenhongyu.huajuan.data

import java.util.Date

data class Message(
    val id: Long,
    val text: String,
    val isUser: Boolean,
    val timestamp: Date
)

data class UserInfo(
    val username: String = "用户名",
    val signature: String = "个性签名",
    val avatar: String = "U"
)

data class Conversation(
    val id: Long,
    val title: String,
    val lastMessage: String,
    val timestamp: Date
)

data class LocalModel(
    val id: String,
    val name: String,
    val size: String,
    val isDownloaded: Boolean = false,
    val downloadProgress: Int = 0
)

data class AppState(
    val conversations: List<Conversation> = listOf(
        Conversation(
            id = 1,
            title = "历史对话 1",
            lastMessage = "你好，请介绍一下你能做什么？",
            timestamp = Date(System.currentTimeMillis() - 3600000) // 1小时前
        ),
        Conversation(
            id = 2,
            title = "历史对话 2",
            lastMessage = "如何学习Jetpack Compose？",
            timestamp = Date(System.currentTimeMillis() - 86400000) // 1天前
        )
    ),
    var currentConversationId: Long? = 1
)

data class ChatState(
    val messages: List<Message> = listOf(
        Message(
            id = 1,
            text = "你好！我是花卷AI助手，有什么我可以帮你的吗？",
            isUser = false,
            timestamp = Date()
        ),
        Message(
            id = 2,
            text = "你好，请介绍一下你能做什么？",
            isUser = true,
            timestamp = Date(System.currentTimeMillis() - 120000) // 2分钟前
        )
    ),
    val inputText: String = ""
)