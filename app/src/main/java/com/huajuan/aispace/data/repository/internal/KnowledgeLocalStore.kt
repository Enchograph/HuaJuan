package com.huajuan.aispace.data.repository.internal

import android.content.Context
import android.net.Uri
import com.huajuan.aispace.data.EmbeddingApiService
import com.huajuan.aispace.data.IndexBuildResult
import com.huajuan.aispace.data.KnowledgeBaseDto
import com.huajuan.aispace.data.KnowledgeBaseEntity
import com.huajuan.aispace.data.KnowledgeCategory
import com.huajuan.aispace.data.KnowledgeChunkEntity
import com.huajuan.aispace.data.KnowledgeChunkHit
import com.huajuan.aispace.data.KnowledgeDao
import com.huajuan.aispace.data.KnowledgeEmbeddingEntity
import com.huajuan.aispace.data.KnowledgeIndexJobDto
import com.huajuan.aispace.data.KnowledgeItemDto
import com.huajuan.aispace.data.KnowledgeItemEntity
import com.huajuan.aispace.data.KnowledgeJobStage
import com.huajuan.aispace.data.KnowledgeSettings
import com.huajuan.aispace.data.KnowledgeItemStatus
import com.huajuan.aispace.data.KnowledgeItemUpsert
import com.huajuan.aispace.data.ModelDataProvider
import com.huajuan.aispace.data.ModelInfo
import com.huajuan.aispace.data.Repository
import com.huajuan.aispace.data.ConversationKbSelectionEntity
import com.huajuan.aispace.R
import com.huajuan.aispace.data.BuiltInServiceProviders
import com.huajuan.aispace.i18n.LocalizedResources
import com.huajuan.aispace.data.utils.KnowledgeVectorUtils
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Date
import java.util.Locale
import java.util.UUID

internal class KnowledgeLocalStore(
    private val repository: Repository,
    private val context: Context,
    private val knowledgeDao: KnowledgeDao
) {
    private val gson = Gson()
    private val knowledgeJobs = MutableStateFlow<List<KnowledgeIndexJobDto>>(emptyList())

    fun getKnowledgeIndexJobsFlow(): StateFlow<List<KnowledgeIndexJobDto>> = knowledgeJobs.asStateFlow()

    suspend fun listKnowledgeBases(): List<KnowledgeBaseDto> = withContext(Dispatchers.IO) {
        knowledgeDao.listKnowledgeBasesSync().map { it.toDto() }
    }

    suspend fun createKnowledgeBase(
        name: String,
        description: String,
        embeddingModelRef: String
    ): KnowledgeBaseDto = withContext(Dispatchers.IO) {
        val now = Date()
        val entity = KnowledgeBaseEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            embeddingModelRef = embeddingModelRef,
            updatedAt = now,
            createdAt = now
        )
        knowledgeDao.upsertKnowledgeBase(entity)
        entity.toDto()
    }

    suspend fun updateKnowledgeBaseEmbedding(kbId: String, embeddingModelRef: String): KnowledgeBaseDto? =
        withContext(Dispatchers.IO) {
            val current = knowledgeDao.getKnowledgeBaseSync(kbId) ?: return@withContext null
            val updated = current.copy(embeddingModelRef = embeddingModelRef, updatedAt = Date())
            knowledgeDao.upsertKnowledgeBase(updated)
            updated.toDto()
        }

    suspend fun getKnowledgeBase(kbId: String): KnowledgeBaseDto? = withContext(Dispatchers.IO) {
        knowledgeDao.getKnowledgeBaseSync(kbId)?.toDto()
    }

    suspend fun updateKnowledgeBase(
        kbId: String,
        name: String,
        description: String,
        embeddingModelRef: String
    ): KnowledgeBaseDto? = withContext(Dispatchers.IO) {
        val current = knowledgeDao.getKnowledgeBaseSync(kbId) ?: return@withContext null
        val updated = current.copy(
            name = name,
            description = description,
            embeddingModelRef = embeddingModelRef,
            updatedAt = Date()
        )
        knowledgeDao.upsertKnowledgeBase(updated)
        updated.toDto()
    }

    suspend fun deleteKnowledgeBase(kbId: String) = withContext(Dispatchers.IO) {
        knowledgeDao.deleteKnowledgeBase(kbId)
    }

    suspend fun listKnowledgeItems(
        kbId: String,
        category: KnowledgeCategory?,
        keyword: String?
    ): List<KnowledgeItemDto> = withContext(Dispatchers.IO) {
        knowledgeDao.listKnowledgeItemsSync(kbId, category?.value, keyword?.takeIf { it.isNotBlank() })
            .map { it.toDto() }
    }

    suspend fun upsertKnowledgeItem(input: KnowledgeItemUpsert): KnowledgeItemDto =
        withContext(Dispatchers.IO) {
            val now = Date()
            val entity = KnowledgeItemEntity(
                id = input.id ?: UUID.randomUUID().toString(),
                kbId = input.kbId,
                category = input.category.value,
                title = input.title,
                sourceUri = input.sourceUri,
                mimeType = input.mimeType,
                status = input.status.value,
                metaJson = input.metaJson,
                updatedAt = now,
                createdAt = now
            )
            knowledgeDao.upsertKnowledgeItem(entity)
            entity.toDto()
        }

    suspend fun removeKnowledgeItem(itemId: String) = withContext(Dispatchers.IO) {
        knowledgeDao.deleteKnowledgeItem(itemId)
    }

    suspend fun getKnowledgeBaseStats(kbId: String): Map<String, Int> = withContext(Dispatchers.IO) {
        val items = knowledgeDao.listKnowledgeItemsSync(kbId, null, null)
        items.groupBy { it.category }.mapValues { it.value.size }
    }

    suspend fun rebuildKnowledgeItemIndex(itemId: String): IndexBuildResult = withContext(Dispatchers.IO) {
        val item = knowledgeDao.getKnowledgeItemSync(itemId)
            ?: return@withContext IndexBuildResult(false, 0, string(R.string.knowledge_index_item_missing))
        val kb = knowledgeDao.getKnowledgeBaseSync(item.kbId)
            ?: return@withContext IndexBuildResult(false, 0, string(R.string.knowledge_index_base_missing))
        val settings = repository.getKnowledgeSettings()
        val jobId = createJob(item)

        updateJob(jobId, KnowledgeJobStage.Reading, message = string(R.string.knowledge_job_reading))
        val content = when (KnowledgeCategory.fromValue(item.category)) {
            KnowledgeCategory.Files -> readFileText(item)
            KnowledgeCategory.Notes -> readMetaContent(item)
            KnowledgeCategory.Urls -> readMetaContent(item)
            KnowledgeCategory.Websites -> readMetaContent(item)
            KnowledgeCategory.Folders -> ""
        }

        if (content.isBlank()) {
            val updated = item.copy(status = KnowledgeItemStatus.Pending.value, updatedAt = Date())
            knowledgeDao.updateKnowledgeItem(updated)
            finishJob(jobId, success = false, stage = KnowledgeJobStage.Failed, message = string(R.string.knowledge_index_no_content))
            return@withContext IndexBuildResult(false, 0, string(R.string.knowledge_index_no_content))
        }

        updateJob(jobId, KnowledgeJobStage.Chunking, message = string(R.string.knowledge_job_chunking))
        val chunks = chunkText(content, settings)
        if (chunks.isEmpty()) {
            finishJob(jobId, success = false, stage = KnowledgeJobStage.Failed, message = string(R.string.knowledge_index_content_too_short))
            return@withContext IndexBuildResult(false, 0, string(R.string.knowledge_index_content_too_short))
        }

        val (provider, modelName) = parseModelRef(kb.embeddingModelRef)
        val modelInfo = resolveModelInfo(provider, modelName)
            ?: run {
                finishJob(jobId, success = false, stage = KnowledgeJobStage.Failed, message = string(R.string.knowledge_index_embedding_model_missing))
                return@withContext IndexBuildResult(false, 0, string(R.string.knowledge_index_embedding_model_missing))
            }

        val embeddingService = EmbeddingApiService(repository, provider)
        val vectors = try {
            val inputs = chunks.map { it.text }
            val batches = batchEmbeddingInputs(inputs, settings)
            val out = mutableListOf<List<Float>>()
            updateJob(
                jobId,
                KnowledgeJobStage.Embedding,
                totalUnits = chunks.size,
                totalBatches = batches.size,
                message = string(R.string.knowledge_job_embedding)
            )
            batches.forEachIndexed { batchIndex, batch ->
                out.addAll(requestEmbeddingBatch(embeddingService, batch, modelInfo, settings))
                updateJob(
                    jobId,
                    KnowledgeJobStage.Embedding,
                    processedUnits = minOf(out.size, chunks.size),
                    totalUnits = chunks.size,
                    processedBatches = batchIndex + 1,
                    totalBatches = batches.size,
                    message = string(
                        R.string.knowledge_job_embedding_batch,
                        batchIndex + 1,
                        batches.size
                    )
                )
            }
            out
        } catch (e: Exception) {
            knowledgeDao.updateKnowledgeItem(item.copy(status = KnowledgeItemStatus.Failed.value, updatedAt = Date()))
            finishJob(
                jobId,
                success = false,
                stage = KnowledgeJobStage.Failed,
                message = string(R.string.knowledge_index_embedding_failed, e.message.orEmpty())
            )
            return@withContext IndexBuildResult(
                false,
                0,
                string(R.string.knowledge_index_embedding_failed, e.message.orEmpty())
            )
        }
        if (vectors.size != chunks.size) {
            knowledgeDao.updateKnowledgeItem(item.copy(status = KnowledgeItemStatus.Failed.value, updatedAt = Date()))
            finishJob(jobId, success = false, stage = KnowledgeJobStage.Failed, message = string(R.string.knowledge_index_vector_count_mismatch))
            return@withContext IndexBuildResult(false, 0, string(R.string.knowledge_index_vector_count_mismatch))
        }

        val chunkEntities = chunks.mapIndexed { index, chunk ->
            KnowledgeChunkEntity(
                id = UUID.randomUUID().toString(),
                itemId = item.id,
                kbId = item.kbId,
                chunkIndex = index,
                text = chunk.text,
                tokenEstimate = chunk.tokenEstimate,
                hash = sha256(chunk.text),
                createdAt = Date()
            )
        }
        val embeddingEntities = chunkEntities.mapIndexed { index, entity ->
            KnowledgeEmbeddingEntity(
                chunkId = entity.id,
                dim = vectors[index].size,
                vectorBlob = KnowledgeVectorUtils.floatsToBytes(vectors[index])
            )
        }

        updateJob(jobId, KnowledgeJobStage.Writing, totalUnits = chunkEntities.size, message = string(R.string.knowledge_job_writing))
        knowledgeDao.replaceItemIndex(item.id, chunkEntities, embeddingEntities)
        knowledgeDao.updateKnowledgeItem(item.copy(status = KnowledgeItemStatus.Ready.value, updatedAt = Date()))
        finishJob(jobId, success = true, stage = KnowledgeJobStage.Completed, message = string(R.string.knowledge_index_completed))
        IndexBuildResult(true, chunks.size, string(R.string.knowledge_index_completed))
    }

    suspend fun fetchWebsiteAndIndex(kbId: String, url: String): IndexBuildResult = withContext(Dispatchers.IO) {
        val kb = knowledgeDao.getKnowledgeBaseSync(kbId)
            ?: return@withContext IndexBuildResult(false, 0, string(R.string.knowledge_index_base_missing))
        val content = fetchUrlText(url)
        if (content.isBlank()) {
            return@withContext IndexBuildResult(false, 0, string(R.string.knowledge_index_fetch_failed))
        }
        val meta = JsonObject().apply {
            addProperty("content", content)
            addProperty("url", url)
        }
        val item = KnowledgeItemEntity(
            id = UUID.randomUUID().toString(),
            kbId = kbId,
            category = KnowledgeCategory.Websites.value,
            title = url,
            sourceUri = url,
            mimeType = "text/html",
            status = KnowledgeItemStatus.Pending.value,
            metaJson = gson.toJson(meta),
            updatedAt = Date(),
            createdAt = Date()
        )
        knowledgeDao.upsertKnowledgeItem(item)
        rebuildKnowledgeItemIndex(item.id)
    }

    suspend fun searchKnowledgeChunks(kbIds: List<String>, query: String, topK: Int): List<KnowledgeChunkHit> =
        withContext(Dispatchers.IO) {
            if (kbIds.isEmpty() || query.isBlank()) return@withContext emptyList()
            val chunks = knowledgeDao.listChunksByKbIdsSync(kbIds)
            if (chunks.isEmpty()) return@withContext emptyList()
            val embeddings = knowledgeDao.listEmbeddingsByChunkIdsSync(chunks.map { it.id })
                .associateBy { it.chunkId }
            val kbMap = kbIds.mapNotNull { id -> knowledgeDao.getKnowledgeBaseSync(id)?.let { kb -> id to kb } }.toMap()
            val groupedKbIds = kbMap.entries.groupBy({ it.value.embeddingModelRef }, { it.key })
            val settings = repository.getKnowledgeSettings()
            val scoredItems = mutableListOf<Pair<KnowledgeChunkEntity, Float>>()

            groupedKbIds.forEach { (modelRef, groupedIds) ->
                val (provider, modelName) = parseModelRef(modelRef)
                val modelInfo = resolveModelInfo(provider, modelName) ?: return@forEach
                val embeddingService = EmbeddingApiService(repository, provider)
                val queryVector = embeddingService.getEmbeddings(listOf(query), modelInfo).firstOrNull()
                    ?: return@forEach
                chunks.asSequence()
                    .filter { it.kbId in groupedIds }
                    .mapNotNull { chunk ->
                        val emb = embeddings[chunk.id] ?: return@mapNotNull null
                        val vec = KnowledgeVectorUtils.bytesToFloats(emb.vectorBlob)
                        val score = KnowledgeVectorUtils.cosineSimilarity(vec, queryVector.toFloatArray())
                        if (score >= settings.searchMinScore) chunk to score else null
                    }
                    .forEach { scoredItems += it }
            }

            val items = scoredItems.sortedByDescending { it.second }

            val itemMap = items.map { it.first.itemId }.distinct().associateWith {
                knowledgeDao.getKnowledgeItemSync(it)
            }

            items.take(topK).mapNotNull { (chunk, score) ->
                val item = itemMap[chunk.itemId] ?: return@mapNotNull null
                KnowledgeChunkHit(
                    kbId = chunk.kbId,
                    itemId = chunk.itemId,
                    chunkId = chunk.id,
                    chunkText = chunk.text,
                    score = score,
                    itemTitle = item.title,
                    sourceUri = item.sourceUri
                )
            }
        }

    suspend fun getConversationKbSelection(conversationId: String): List<String> = withContext(Dispatchers.IO) {
        val entity = knowledgeDao.getConversationKbSelectionSync(conversationId) ?: return@withContext emptyList()
        val listType = com.google.gson.reflect.TypeToken.getParameterized(List::class.java, String::class.java).type
        gson.fromJson<List<String>>(entity.kbIdsJson, listType) ?: emptyList()
    }

    suspend fun setConversationKbSelection(conversationId: String, kbIds: List<String>) = withContext(Dispatchers.IO) {
        val json = gson.toJson(kbIds.distinct())
        knowledgeDao.upsertConversationKbSelection(
            ConversationKbSelectionEntity(
                conversationId = conversationId,
                kbIdsJson = json,
                updatedAt = Date()
            )
        )
    }

    private fun KnowledgeBaseEntity.toDto() = KnowledgeBaseDto(
        id = id,
        name = name,
        description = description,
        embeddingModelRef = embeddingModelRef,
        updatedAt = updatedAt,
        createdAt = createdAt
    )

    private fun KnowledgeItemEntity.toDto() = KnowledgeItemDto(
        id = id,
        kbId = kbId,
        category = KnowledgeCategory.fromValue(category),
        title = title,
        sourceUri = sourceUri,
        mimeType = mimeType,
        status = KnowledgeItemStatus.fromValue(status),
        metaJson = metaJson,
        updatedAt = updatedAt,
        createdAt = createdAt
    )

    private fun readMetaContent(item: KnowledgeItemEntity): String {
        return try {
            val obj = gson.fromJson(item.metaJson, JsonObject::class.java)
            obj?.get("content")?.asString ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun readFileText(item: KnowledgeItemEntity): String {
        val uri = item.sourceUri.takeIf { it.isNotBlank() } ?: return ""
        val mime = item.mimeType.orEmpty().lowercase(Locale.getDefault())
        val localPath = parseLocalPath(item.metaJson)
        val allowed = mime.startsWith("text/") ||
            uri.endsWith(".md") || uri.endsWith(".txt") || uri.endsWith(".json") || uri.endsWith(".csv") ||
            (localPath?.endsWith(".md") == true) || (localPath?.endsWith(".txt") == true) ||
            (localPath?.endsWith(".json") == true) || (localPath?.endsWith(".csv") == true)
        if (!allowed) return ""
        return runCatching {
            val input = if (!localPath.isNullOrBlank()) {
                File(localPath).inputStream()
            } else {
                context.contentResolver.openInputStream(Uri.parse(uri))
            } ?: return@runCatching ""
            input.use {
                BufferedReader(InputStreamReader(it, StandardCharsets.UTF_8)).readText()
            }
        }.getOrDefault("")
    }

    private fun fetchUrlText(url: String): String {
        val client = OkHttpClient.Builder().build()
        val req = Request.Builder().url(url).build()
        val resp = client.newCall(req).execute()
        resp.use {
            if (!it.isSuccessful) return ""
            val html = it.body?.string().orEmpty()
            return stripHtml(html)
        }
    }

    private fun stripHtml(html: String): String {
        return html
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private data class Chunk(val text: String, val tokenEstimate: Int)

    private fun chunkText(text: String, settings: KnowledgeSettings): List<Chunk> {
        val cleaned = text.replace("\r\n", "\n").trim()
        if (cleaned.isBlank()) return emptyList()
        val normalized = cleaned
            .split(Regex("\\n\\s*\\n"))
            .flatMap { paragraph ->
                splitOversizedText(
                    paragraph.trim(),
                    settings.embeddingInputMaxChars.coerceAtLeast(settings.minChunkChars)
                )
            }
            .filter { it.isNotBlank() }
        if (normalized.isEmpty()) return emptyList()

        val targetChars = settings.chunkTargetChars
            .coerceAtLeast(settings.minChunkChars)
            .coerceAtMost(settings.embeddingInputMaxChars)
        val overlapChars = settings.chunkOverlapChars.coerceAtLeast(0)
        val merged = normalized.joinToString("\n\n")
        val out = mutableListOf<Chunk>()
        var start = 0
        while (start < merged.length) {
            val roughEnd = (start + targetChars).coerceAtMost(merged.length)
            var end = roughEnd
            if (end < merged.length) {
                val breakIndex = merged.lastIndexOfAny(
                    charArrayOf('\n', '。', '！', '？', '.', '!', '?', ';', '；'),
                    startIndex = roughEnd - 1
                )
                if (breakIndex > start + settings.minChunkChars) {
                    end = breakIndex + 1
                }
            }
            val piece = merged.substring(start, end).trim()
            val safePieces = splitOversizedText(piece, settings.embeddingInputMaxChars)
            safePieces.forEach { safePiece ->
                if (safePiece.length >= settings.minChunkChars || merged.length <= settings.minChunkChars) {
                    val tokenEstimate = estimateTokenCount(safePiece)
                    if (tokenEstimate <= settings.embeddingInputMaxTokens || safePiece.length <= settings.minChunkChars) {
                        out.add(Chunk(safePiece, tokenEstimate))
                    } else {
                        splitOversizedText(safePiece, maxOf(settings.minChunkChars, settings.embeddingInputMaxChars / 2))
                            .forEach { smaller ->
                                if (smaller.isNotBlank()) {
                                    out.add(Chunk(smaller, estimateTokenCount(smaller)))
                                }
                            }
                    }
                }
            }
            if (end == merged.length) break
            start = (end - overlapChars).coerceAtLeast(0)
        }
        return out
            .distinctBy { it.text }
            .filter { it.text.isNotBlank() }
    }

    private fun estimateTokenCount(text: String): Int {
        val trimmed = text.trim()
        val asciiTokens = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        val nonAsciiCount = trimmed.count { it.code > 127 }
        return asciiTokens.size + nonAsciiCount
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(text.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun parseModelRef(ref: String): Pair<String, String> {
        val parts = ref.split("|")
        return if (parts.size == 2) parts[0] to parts[1] else (BuiltInServiceProviders.LegacyCustom to ref)
    }

    private fun string(resId: Int, vararg args: Any): String =
        LocalizedResources.getString(context, resId, null, *args)

    private fun resolveModelInfo(provider: String, modelName: String): ModelInfo? {
        val dataProvider = ModelDataProvider(repository)
        return dataProvider.getModelListForProvider(provider).firstOrNull { it.displayName == modelName }
    }

    private fun parseLocalPath(metaJson: String): String? {
        return try {
            val obj = gson.fromJson(metaJson, JsonObject::class.java)
            obj?.get("localPath")?.asString
        } catch (_: Exception) {
            null
        }
    }

    private fun batchEmbeddingInputs(inputs: List<String>, settings: KnowledgeSettings): List<List<String>> {
        if (inputs.isEmpty()) return emptyList()
        val batches = mutableListOf<MutableList<String>>()
        var current = mutableListOf<String>()
        var currentChars = 0
        inputs.forEach { input ->
            val inputSize = input.length
            val wouldExceedSize = current.size + 1 > settings.maxBatchChunks ||
                (current.isNotEmpty() && currentChars + inputSize > settings.maxBatchChars)
            if (wouldExceedSize) {
                batches.add(current)
                current = mutableListOf()
                currentChars = 0
            }
            current.add(input)
            currentChars += inputSize
        }
        if (current.isNotEmpty()) {
            batches.add(current)
        }
        return batches
    }

    private fun splitOversizedText(text: String, maxChars: Int): List<String> {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return emptyList()
        if (trimmed.length <= maxChars) return listOf(trimmed)
        val pieces = mutableListOf<String>()
        var start = 0
        while (start < trimmed.length) {
            val end = (start + maxChars).coerceAtMost(trimmed.length)
            val candidate = trimmed.substring(start, end)
            val breakIndex = candidate.lastIndexOfAny(charArrayOf('\n', '。', '！', '？', '.', '!', '?', ';', '；', ',', '，'))
            val actualEnd = if (breakIndex > maxChars / 3 && end < trimmed.length) {
                start + breakIndex + 1
            } else {
                end
            }
            pieces += trimmed.substring(start, actualEnd).trim()
            start = actualEnd
        }
        return pieces.filter { it.isNotBlank() }
    }

    private suspend fun requestEmbeddingBatch(
        embeddingService: EmbeddingApiService,
        batch: List<String>,
        modelInfo: ModelInfo,
        settings: KnowledgeSettings
    ): List<List<Float>> {
        var lastError: Exception? = null
        repeat(settings.embeddingRetryCount.coerceAtLeast(0) + 1) {
            try {
                return embeddingService.getEmbeddings(batch, modelInfo)
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("embedding failed")
    }

    private fun createJob(item: KnowledgeItemEntity): String {
        val now = Date()
        val job = KnowledgeIndexJobDto(
            id = UUID.randomUUID().toString(),
            itemId = item.id,
            kbId = item.kbId,
            title = item.title,
            stage = KnowledgeJobStage.Queued,
            message = string(R.string.knowledge_job_queued),
            startedAt = now,
            updatedAt = now
        )
        knowledgeJobs.value = (listOf(job) + knowledgeJobs.value)
            .distinctBy { it.id }
            .take(repository.getKnowledgeSettings().taskHistoryLimit.coerceAtLeast(1))
        return job.id
    }

    private fun updateJob(
        jobId: String,
        stage: KnowledgeJobStage,
        processedUnits: Int? = null,
        totalUnits: Int? = null,
        processedBatches: Int? = null,
        totalBatches: Int? = null,
        message: String? = null
    ) {
        val now = Date()
        knowledgeJobs.value = knowledgeJobs.value.map { job ->
            if (job.id != jobId) {
                job
            } else {
                job.copy(
                    stage = stage,
                    processedUnits = processedUnits ?: job.processedUnits,
                    totalUnits = totalUnits ?: job.totalUnits,
                    processedBatches = processedBatches ?: job.processedBatches,
                    totalBatches = totalBatches ?: job.totalBatches,
                    message = message ?: job.message,
                    updatedAt = now
                )
            }
        }
    }

    private fun finishJob(jobId: String, success: Boolean, stage: KnowledgeJobStage, message: String) {
        val now = Date()
        knowledgeJobs.value = knowledgeJobs.value.map { job ->
            if (job.id != jobId) {
                job
            } else {
                job.copy(
                    stage = stage,
                    message = message,
                    success = success,
                    finishedAt = now,
                    updatedAt = now,
                    processedUnits = if (success && job.totalUnits > 0) job.totalUnits else job.processedUnits,
                    processedBatches = if (success && job.totalBatches > 0) job.totalBatches else job.processedBatches
                )
            }
        }.take(repository.getKnowledgeSettings().taskHistoryLimit.coerceAtLeast(1))
    }
}
