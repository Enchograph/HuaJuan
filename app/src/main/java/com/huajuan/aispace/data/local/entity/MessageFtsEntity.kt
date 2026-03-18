package com.huajuan.aispace.data

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = MessageEntity::class)
@Entity(tableName = "messages_fts")
data class MessageFtsEntity(
    val text: String,
    val attachmentsJson: String,
    val imageUrisJson: String,
    val conversationId: String
)
