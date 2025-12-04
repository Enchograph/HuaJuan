package com.chenhongyu.huajuan.data

import java.util.Date
import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(),
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
    val id: String = UUID.randomUUID().toString(),
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
    var conversations: List<Conversation> = listOf(),
    var currentConversationId: String? = null
)

data class ChatState(
    val messages: List<Message> = listOf(),
    val inputText: String = ""
)
