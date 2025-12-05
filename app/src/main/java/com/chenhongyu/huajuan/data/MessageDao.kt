package com.chenhongyu.huajuan.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesByConversationId(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :messageId")
    fun getMessageById(messageId: String): MessageEntity?

    @Insert
    fun insertMessage(message: MessageEntity): Long

    @Insert
    fun insertMessages(messages: List<MessageEntity>): List<Long>

    @Update
    fun updateMessage(message: MessageEntity): Int

    @Upsert
    fun upsertMessage(message: MessageEntity): Long
    
    @Upsert
    fun upsertMessages(messages: List<MessageEntity>): List<Long>

    @Delete
    fun deleteMessage(message: MessageEntity): Int

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    fun deleteMessagesByConversationId(conversationId: String): Int
}