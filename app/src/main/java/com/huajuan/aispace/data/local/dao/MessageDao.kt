package com.huajuan.aispace.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.Upsert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesByConversationId(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesByConversationIdSync(conversationId: String): List<MessageEntity>

    @Query(
        "SELECT messages.* FROM messages " +
            "JOIN messages_fts ON messages.rowid = messages_fts.rowid " +
            "WHERE messages.conversationId = :conversationId " +
            "AND messages_fts MATCH :query " +
            "ORDER BY messages.timestamp ASC"
    )
    fun searchMessagesByConversationIdSync(conversationId: String, query: String): List<MessageEntity>

    @Query(
        "SELECT messages.id AS messageId, messages.conversationId AS conversationId, " +
            "conversations.assistantId AS assistantId, conversations.title AS conversationTitle, " +
            "messages.text AS messageText, " +
            "messages.timestamp AS timestamp " +
            "FROM messages " +
            "JOIN messages_fts ON messages.rowid = messages_fts.rowid " +
            "JOIN conversations ON conversations.id = messages.conversationId " +
            "WHERE messages_fts MATCH :query " +
            "ORDER BY messages.timestamp DESC " +
            "LIMIT :limit"
    )
    fun searchAllMessagesSync(query: String, limit: Int): List<MessageSearchResult>

    @Query(
        "SELECT messages.id AS messageId, messages.conversationId AS conversationId, " +
            "conversations.assistantId AS assistantId, conversations.title AS conversationTitle, " +
            "messages.text AS messageText, " +
            "messages.timestamp AS timestamp " +
            "FROM messages " +
            "JOIN messages_fts ON messages.rowid = messages_fts.rowid " +
            "JOIN conversations ON conversations.id = messages.conversationId " +
            "WHERE conversations.assistantId = :assistantId " +
            "AND messages_fts MATCH :query " +
            "ORDER BY messages.timestamp DESC " +
            "LIMIT :limit"
    )
    fun searchAllMessagesByAssistantSync(assistantId: String, query: String, limit: Int): List<MessageSearchResult>

    @Query("SELECT id FROM messages WHERE conversationId = :conversationId")
    fun getMessageIdsByConversationIdSync(conversationId: String): List<String>

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

    @Query("DELETE FROM messages WHERE id IN (:ids)")
    fun deleteMessagesByIds(ids: List<String>): Int

    @Query("UPDATE messages SET showThink = :showThink WHERE id = :messageId")
    fun updateMessageShowThink(messageId: String, showThink: Boolean): Int

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    fun deleteMessagesByConversationId(conversationId: String): Int
}
