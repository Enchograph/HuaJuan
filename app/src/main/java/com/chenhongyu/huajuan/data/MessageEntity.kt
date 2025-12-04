package com.chenhongyu.huajuan.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: Long,
    val conversationId: Long,
    val text: String,
    val isUser: Boolean,
    val timestamp: Date
)