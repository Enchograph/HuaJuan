package com.huajuan.aispace.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY timestamp DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations ORDER BY timestamp DESC")
    fun getAllConversationsSync(): List<ConversationEntity>

    @Query("SELECT * FROM conversations WHERE assistantId = :assistantId ORDER BY timestamp DESC")
    fun getConversationsByAssistant(assistantId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE assistantId = :assistantId ORDER BY timestamp DESC")
    fun getConversationsByAssistantSync(assistantId: String): List<ConversationEntity>

    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    fun getConversationById(conversationId: String): ConversationEntity?

    @Query("SELECT roleName FROM conversations WHERE id = :conversationId")
    fun getConversationRoleNameById(conversationId: String): String?

    @Query("SELECT systemPrompt FROM conversations WHERE id = :conversationId")
    fun getConversationSystemPromptById(conversationId: String): String?

    @Insert
    fun insertConversation(conversation: ConversationEntity): Long

    @Update
    fun updateConversation(conversation: ConversationEntity): Int

    @Delete
    fun deleteConversation(conversation: ConversationEntity): Int

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    fun deleteConversationById(conversationId: String): Int
}
