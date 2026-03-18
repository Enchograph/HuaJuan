package com.huajuan.aispace.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface KnowledgeDao {
    @Query("SELECT * FROM knowledge_bases ORDER BY updatedAt DESC")
    fun listKnowledgeBasesSync(): List<KnowledgeBaseEntity>

    @Query("SELECT * FROM knowledge_bases WHERE id = :kbId LIMIT 1")
    fun getKnowledgeBaseSync(kbId: String): KnowledgeBaseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertKnowledgeBase(entity: KnowledgeBaseEntity)

    @Query("DELETE FROM knowledge_bases WHERE id = :kbId")
    fun deleteKnowledgeBase(kbId: String): Int

    @Query(
        "SELECT * FROM knowledge_items " +
            "WHERE kbId = :kbId " +
            "AND (:category IS NULL OR category = :category) " +
            "AND (:keyword IS NULL OR title LIKE '%' || :keyword || '%' OR sourceUri LIKE '%' || :keyword || '%') " +
            "ORDER BY updatedAt DESC"
    )
    fun listKnowledgeItemsSync(kbId: String, category: String?, keyword: String?): List<KnowledgeItemEntity>

    @Query("SELECT * FROM knowledge_items WHERE id = :itemId LIMIT 1")
    fun getKnowledgeItemSync(itemId: String): KnowledgeItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertKnowledgeItem(entity: KnowledgeItemEntity)

    @Update
    fun updateKnowledgeItem(entity: KnowledgeItemEntity): Int

    @Query("DELETE FROM knowledge_items WHERE id = :itemId")
    fun deleteKnowledgeItem(itemId: String): Int

    @Query("SELECT * FROM knowledge_chunks WHERE itemId = :itemId ORDER BY chunkIndex ASC")
    fun listChunksByItemSync(itemId: String): List<KnowledgeChunkEntity>

    @Query("SELECT * FROM knowledge_chunks WHERE kbId IN (:kbIds)")
    fun listChunksByKbIdsSync(kbIds: List<String>): List<KnowledgeChunkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertChunks(chunks: List<KnowledgeChunkEntity>)

    @Query("DELETE FROM knowledge_chunks WHERE itemId = :itemId")
    fun deleteChunksByItem(itemId: String): Int

    @Query("SELECT * FROM knowledge_embeddings WHERE chunkId IN (:chunkIds)")
    fun listEmbeddingsByChunkIdsSync(chunkIds: List<String>): List<KnowledgeEmbeddingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertEmbeddings(embeddings: List<KnowledgeEmbeddingEntity>)

    @Query("DELETE FROM knowledge_embeddings WHERE chunkId IN (:chunkIds)")
    fun deleteEmbeddingsByChunkIds(chunkIds: List<String>): Int

    @Query("SELECT * FROM conversation_kb_manual_selection WHERE conversationId = :conversationId LIMIT 1")
    fun getConversationKbSelectionSync(conversationId: String): ConversationKbSelectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertConversationKbSelection(entity: ConversationKbSelectionEntity)

    @Query("DELETE FROM conversation_kb_manual_selection WHERE conversationId = :conversationId")
    fun clearConversationKbSelection(conversationId: String): Int

    @Transaction
    fun replaceItemIndex(
        itemId: String,
        chunks: List<KnowledgeChunkEntity>,
        embeddings: List<KnowledgeEmbeddingEntity>
    ) {
        val existingChunks = listChunksByItemSync(itemId)
        if (existingChunks.isNotEmpty()) {
            deleteEmbeddingsByChunkIds(existingChunks.map { it.id })
        }
        deleteChunksByItem(itemId)
        if (chunks.isNotEmpty()) {
            upsertChunks(chunks)
        }
        if (embeddings.isNotEmpty()) {
            upsertEmbeddings(embeddings)
        }
    }
}

