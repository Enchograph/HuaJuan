package com.chenhongyu.huajuan.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesByConversationId(conversationId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :messageId")
    fun getMessageById(messageId: Long): MessageEntity?

    @Insert
    fun insertMessage(message: MessageEntity): Long

    @Insert
    fun insertMessages(messages: List<MessageEntity>): List<Long>

    @Update
    fun updateMessage(message: MessageEntity): Int

    @Delete
    fun deleteMessage(message: MessageEntity): Int

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    fun deleteMessagesByConversationId(conversationId: Long): Int
}