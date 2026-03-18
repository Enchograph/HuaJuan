package com.huajuan.aispace.data

import java.util.Date

enum class KnowledgeCategory(val value: String) {
    Files("file"),
    Notes("note"),
    Folders("folder"),
    Urls("url"),
    Websites("website");

    companion object {
        fun fromValue(raw: String): KnowledgeCategory {
            return entries.firstOrNull { it.value == raw } ?: Files
        }
    }
}

enum class KnowledgeItemStatus(val value: String) {
    Ready("ready"),
    Pending("pending"),
    Failed("failed");

    companion object {
        fun fromValue(raw: String): KnowledgeItemStatus {
            return entries.firstOrNull { it.value == raw } ?: Pending
        }
    }
}

data class KnowledgeBaseDto(
    val id: String,
    val name: String,
    val description: String,
    val embeddingModelRef: String,
    val updatedAt: Date,
    val createdAt: Date
)

data class KnowledgeItemDto(
    val id: String,
    val kbId: String,
    val category: KnowledgeCategory,
    val title: String,
    val sourceUri: String,
    val mimeType: String?,
    val status: KnowledgeItemStatus,
    val metaJson: String,
    val updatedAt: Date,
    val createdAt: Date
)

data class KnowledgeItemUpsert(
    val id: String? = null,
    val kbId: String,
    val category: KnowledgeCategory,
    val title: String,
    val sourceUri: String,
    val mimeType: String? = null,
    val status: KnowledgeItemStatus = KnowledgeItemStatus.Pending,
    val metaJson: String = "{}"
)

data class KnowledgeChunkHit(
    val kbId: String,
    val itemId: String,
    val chunkId: String,
    val chunkText: String,
    val score: Float,
    val itemTitle: String,
    val sourceUri: String
)

data class IndexBuildResult(
    val success: Boolean,
    val chunks: Int,
    val message: String
)

data class KnowledgeSettings(
    val inheritAssistantDefaultsToConversation: Boolean = true,
    val skipUnchangedContent: Boolean = true,
    val deleteMissingFilesOnRefresh: Boolean = true,
    val includeHiddenFiles: Boolean = false,
    val maxFolderDepth: Int = 8,
    val allowedFileExtensions: List<String> = listOf("txt", "md", "json", "csv", "html", "htm"),
    val embeddingInputMaxChars: Int = 6000,
    val embeddingInputMaxTokens: Int = 1800,
    val chunkTargetChars: Int = 1400,
    val chunkOverlapChars: Int = 180,
    val minChunkChars: Int = 120,
    val maxBatchChunks: Int = 8,
    val maxBatchChars: Int = 12000,
    val embeddingParallelism: Int = 1,
    val embeddingRetryCount: Int = 2,
    val searchCandidateLimit: Int = 24,
    val searchTopK: Int = 8,
    val searchContextLimit: Int = 4,
    val searchContextMaxChars: Int = 5000,
    val searchMinScore: Float = 0.15f,
    val mergeAdjacentChunks: Boolean = true,
    val webMaxChars: Int = 20000,
    val taskHistoryLimit: Int = 20
)

enum class KnowledgeJobStage {
    Queued,
    Reading,
    Chunking,
    Embedding,
    Writing,
    Completed,
    Failed
}

data class KnowledgeIndexJobDto(
    val id: String,
    val itemId: String,
    val kbId: String,
    val title: String,
    val stage: KnowledgeJobStage,
    val processedUnits: Int = 0,
    val totalUnits: Int = 0,
    val processedBatches: Int = 0,
    val totalBatches: Int = 0,
    val message: String = "",
    val startedAt: Date,
    val updatedAt: Date,
    val finishedAt: Date? = null,
    val success: Boolean? = null
) {
    val progress: Float
        get() = if (totalUnits > 0) {
            (processedUnits.toFloat() / totalUnits.toFloat()).coerceIn(0f, 1f)
        } else if (totalBatches > 0) {
            (processedBatches.toFloat() / totalBatches.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}
