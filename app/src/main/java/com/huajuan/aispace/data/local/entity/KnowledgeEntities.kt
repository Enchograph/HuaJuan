package com.huajuan.aispace.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "knowledge_bases")
data class KnowledgeBaseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val embeddingModelRef: String,
    val updatedAt: Date,
    val createdAt: Date
)

@Entity(
    tableName = "knowledge_items",
    indices = [
        Index(value = ["kbId", "category", "updatedAt"]),
        Index(value = ["kbId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = KnowledgeBaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["kbId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class KnowledgeItemEntity(
    @PrimaryKey val id: String,
    val kbId: String,
    val category: String,
    val title: String,
    val sourceUri: String,
    val mimeType: String?,
    val status: String,
    val metaJson: String,
    val updatedAt: Date,
    val createdAt: Date
)

@Entity(
    tableName = "knowledge_chunks",
    indices = [
        Index(value = ["kbId"]),
        Index(value = ["itemId", "chunkIndex"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = KnowledgeItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = KnowledgeBaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["kbId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class KnowledgeChunkEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val kbId: String,
    val chunkIndex: Int,
    val text: String,
    val tokenEstimate: Int,
    val hash: String,
    val createdAt: Date
)

@Entity(
    tableName = "knowledge_embeddings",
    foreignKeys = [
        ForeignKey(
            entity = KnowledgeChunkEntity::class,
            parentColumns = ["id"],
            childColumns = ["chunkId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class KnowledgeEmbeddingEntity(
    @PrimaryKey val chunkId: String,
    val dim: Int,
    val vectorBlob: ByteArray
)

@Entity(tableName = "conversation_kb_manual_selection")
data class ConversationKbSelectionEntity(
    @PrimaryKey val conversationId: String,
    val kbIdsJson: String,
    val updatedAt: Date
)

