package com.huajuan.aispace.data

import android.os.SystemClock
import com.huajuan.aispace.R
import com.huajuan.aispace.network.ContentItem
import com.huajuan.aispace.network.ImageUrl
import com.huajuan.aispace.network.Message
import com.huajuan.aispace.data.model.ChatEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.awaitClose
import okhttp3.*
import okhttp3.HttpUrl
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.huajuan.aispace.utils.ImageUtils
import com.huajuan.aispace.utils.debugLog

/**
 * 在线模型API服务实现
 */
class OnlineModelApiService(
    private val repository: Repository,
    private val assistantId: String = repository.getCurrentAssistantId()
) : ModelApiService {
    private companion object {
        const val TAG = "OnlineModelApiService"
    }

    private data class PreparedOpenAiRequest(
        val processedMessages: List<Message>,
        val payload: JsonObject,
        val request: Request,
        val endpoint: HttpUrl
    )

    private class RequestTrace(private val enabled: Boolean, private val label: String) {
        private val startedAt = SystemClock.elapsedRealtime()

        fun mark(stage: String) {
            if (!enabled) return
            val elapsed = SystemClock.elapsedRealtime() - startedAt
            debugLog(TAG) { "$label $stage at ${elapsed}ms" }
        }
    }

    private fun logRequestBodyIfDebug(
        provider: String,
        endpoint: HttpUrl?,
        payload: JsonObject,
        debugScopeId: String? = null
    ) {
        if (!repository.getDebugMode()) return
        repository.captureModelRequestDebugSnapshot(debugScopeId, payload.toString())
        debugLog(TAG) { "request provider=$provider endpoint=${endpoint ?: "unknown"}\n$payload" }
    }


    private fun logInputTextIfDebug(provider: String, messages: List<Message>) {
        if (!repository.getDebugMode()) return
        val text = messages.joinToString("\n") { message ->
            "[${message.role}] ${extractTextFromContent(message.content)}"
        }
        debugLog(TAG) { "model input text provider=$provider\n$text" }
    }
    private fun isBuiltinWebSearchEnabled(): Boolean {
        return repository.resolveWebSearchPlan().mode == WebSearchMode.ModelNative
    }

    override fun isAvailable(): Boolean {
        // 在线模型始终可用（只要有网络）
        return true
    }

    override suspend fun getAIResponse(
        messages: List<Message>,
        modelInfo: ModelInfo,
        debugScopeId: String?
    ): String {
        val config = repository.getAssistantModelConfig(assistantId)
        val providerType = repository.getProviderType(config.serviceProvider)
        if (providerType == ProviderType.Anthropic) {
            return getAnthropicResponse(messages, modelInfo, config, debugScopeId)
        }
        if (providerType == ProviderType.Gemini) {
            return getGeminiResponse(messages, modelInfo, config, debugScopeId)
        }
        try {
            val apiKey = repository.getApiKeyForProvider(config.serviceProvider)
            if (apiKey.isEmpty()) {
                return ModelErrorParser.normalizeUserError(repository.getContext().getString(R.string.error_api_key_missing))
            }
            val prepared = withContext(Dispatchers.IO) {
                prepareOpenAiRequest(
                    messages = messages,
                    modelInfo = modelInfo,
                    config = config,
                    apiKey = apiKey,
                    stream = false,
                    debugScopeId = debugScopeId
                )
            } ?: return ModelErrorParser.normalizeUserError(repository.getContext().getString(R.string.error_invalid_api_url))

            val client = createOkHttpClient(streaming = false)
            return withContext(Dispatchers.IO) {
                client.newCall(prepared.request).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        val errorBody = resp.body?.string()
                        return@withContext ModelErrorParser.parseHttpError(resp.code, resp.message, errorBody)
                    }
                    val body = resp.body ?: return@withContext ModelErrorParser.normalizeUserError(
                        repository.getContext().getString(R.string.error_empty_response)
                    )
                    val text = body.string()
                    ModelErrorParser.extractErrorFromJson(text)?.let { modelError ->
                        return@withContext ModelErrorParser.normalizeUserError(modelError)
                    }
                    val result = extractTextFromJson(text)
                    if (result.isNotBlank()) {
                        result
                    } else {
                        ModelErrorParser.normalizeUserError(repository.getContext().getString(R.string.error_no_content))
                    }
                }
            }
        } catch (e: Exception) {
            return ModelErrorParser.parseThrowable(e)
        }
    }

    override fun streamAIResponse(
        messages: List<Message>,
        modelInfo: ModelInfo,
        debugScopeId: String?
    ): Flow<ChatEvent> {
        val config = repository.getAssistantModelConfig(assistantId)
        val providerType = repository.getProviderType(config.serviceProvider)
        return when (providerType) {
            ProviderType.Anthropic -> streamAnthropicResponse(messages, modelInfo, config, debugScopeId)
            ProviderType.Gemini -> streamGeminiResponse(messages, modelInfo, config, debugScopeId)
            else -> streamOpenAIResponse(messages, modelInfo, config, debugScopeId)
        }
    }

    private fun streamOpenAIResponse(
        messages: List<Message>,
        modelInfo: ModelInfo,
        config: AssistantModelConfig,
        debugScopeId: String?
    ): Flow<ChatEvent> = channelFlow {
        val apiKey = repository.getApiKeyForProvider(config.serviceProvider)
        if (apiKey.isEmpty()) {
            trySend(ChatEvent.Error(ModelErrorParser.normalizeUserError(repository.getContext().getString(R.string.error_api_key_missing))))
            close()
            return@channelFlow
        }
        val trace = RequestTrace(repository.getDebugMode(), "stream[$assistantId]")
        val callRef = AtomicReference<Call?>()
        val worker: Job = launch(Dispatchers.IO) {
            trace.mark("request prep started")
            val prepared = prepareOpenAiRequest(
                messages = messages,
                modelInfo = modelInfo,
                config = config,
                apiKey = apiKey,
                stream = true,
                debugScopeId = debugScopeId
            )
            if (prepared == null) {
                trySend(ChatEvent.Error(ModelErrorParser.normalizeUserError(repository.getContext().getString(R.string.error_invalid_api_url))))
                close()
                return@launch
            }
            trace.mark("request prepared")

            val client = createOkHttpClient(streaming = true)
            val call = client.newCall(prepared.request)
            callRef.set(call)
            trace.mark("http execute started")

            call.execute().use { resp ->
                if (!resp.isSuccessful) {
                    val errorBody = resp.body?.string()
                    trySend(ChatEvent.Error(ModelErrorParser.parseHttpError(resp.code, resp.message, errorBody)))
                    close()
                    return@use
                }

                val body = resp.body ?: run {
                    trySend(ChatEvent.Error(ModelErrorParser.normalizeUserError(repository.getContext().getString(R.string.error_empty_response))))
                    close()
                    return@use
                }

                val contentType = resp.header("Content-Type") ?: ""
                if (!contentType.contains("text/event-stream", ignoreCase = true)) {
                    try {
                        trace.mark("non-sse response started")
                        val full = body.string()
                        ModelErrorParser.extractErrorFromJson(full)?.let { modelError ->
                            trySend(ChatEvent.Error(ModelErrorParser.normalizeUserError(modelError)))
                            return@use
                        }
                        val text = extractTextFromJson(full)
                        if (text.isNotEmpty()) {
                            trace.mark("first chunk received")
                            trySend(ChatEvent.Chunk(text))
                        } else {
                            trySend(ChatEvent.Error(ModelErrorParser.normalizeUserError(repository.getContext().getString(R.string.error_parse_response_failed))))
                        }
                        trySend(ChatEvent.Done)
                    } catch (e: Exception) {
                        trySend(ChatEvent.Error(ModelErrorParser.parseThrowable(e)))
                    } finally {
                        body.close()
                        close()
                    }
                    return@use
                }

                parseSseStream(this@channelFlow, body, trace)
            }
        }

        awaitClose {
            callRef.get()?.cancel()
            worker.cancel()
        }
    }

    // Extract full text from a non-stream JSON response
    private fun extractTextFromJson(json: String): String {
        return try {
            val element = JsonParser.parseString(json)
            if (!element.isJsonObject) return ""
            val obj = element.asJsonObject

            // Common OpenAI-style chat completion
            obj.getAsJsonArray("choices")?.firstOrNull()?.asJsonObject?.let { choice ->
                val message = choice.getAsJsonObject("message")
                val reasoning = message?.get("reasoning_content")?.asString
                    ?: message?.get("reasoning")?.asString
                    ?: message?.get("thinking")?.asString
                val content = message.get("content")
                
                if (content != null && content.isJsonArray) {
                    // 如果是内容数组，提取所有文本内容
                    val contentArray = content.asJsonArray
                    val textContents = mutableListOf<String>()
                    for (item in contentArray) {
                        if (item.isJsonObject) {
                            val itemObj = item.asJsonObject
                            val type = itemObj.get("type")?.asString
                            if (type == "text") {
                                val text = itemObj.get("text")?.asString
                                if (text != null) {
                                    textContents.add(text)
                                }
                            }
                        }
                    }
                    val answer = textContents.joinToString("\n")
                    return joinReasoningAndAnswer(reasoning, answer)
                } else if (content != null && content.isJsonPrimitive) {
                    // 如果是简单字符串
                    return joinReasoningAndAnswer(reasoning, content.asString)
                }
                
                // Some providers put text directly
                choice.get("text")?.asString?.let { return joinReasoningAndAnswer(reasoning, it) }
            }

            // Some providers may use top-level "content" or "output_text"
            obj.get("content")?.asString ?: obj.get("output_text")?.asString ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    override suspend fun getEmbeddings(inputs: List<String>, modelInfo: ModelInfo): List<List<Float>> {
        if (inputs.isEmpty()) return emptyList()
        val config = repository.getAssistantModelConfig(assistantId)
        val providerType = repository.getProviderType(config.serviceProvider)
        if (providerType == ProviderType.Anthropic) {
            throw IllegalStateException(repository.getContext().getString(R.string.error_embedding_not_supported))
        }
        if (providerType == ProviderType.Gemini) {
            return getGeminiEmbeddings(inputs, modelInfo, config)
        }
        val apiKey = repository.getApiKeyForProvider(config.serviceProvider)
        if (apiKey.isEmpty()) {
            throw IllegalStateException(repository.getContext().getString(R.string.error_api_key_missing))
        }

        if (repository.getDebugMode()) {
            debugLog(TAG) { "embedding input text provider=${config.serviceProvider}\n${inputs.joinToString("\n")}" }
        }

        val payload = JsonObject().apply {
            addProperty("model", modelInfo.apiCode)
            if (inputs.size == 1) {
                addProperty("input", inputs.first())
            } else {
                val arr = JsonArray()
                inputs.forEach { arr.add(it) }
                add("input", arr)
            }
        }

        val requestBody = payload.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val endpoint = ApiUrlResolver.resolveEmbeddingsUrlForProvider(repository, config.serviceProvider)
            ?.toHttpUrlOrNull()
            ?: throw IllegalStateException(repository.getContext().getString(R.string.error_invalid_api_url))
        logRequestBodyIfDebug(config.serviceProvider, endpoint, payload, null)

        val req = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val client = createOkHttpClient(streaming = false)
        return withContext(Dispatchers.IO) {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val errorBody = resp.body?.string()
                    throw IllegalStateException(ModelErrorParser.parseHttpError(resp.code, resp.message, errorBody))
                }
                val body = resp.body?.string().orEmpty()
                parseEmbeddingsResponse(body)
            }
        }
    }

    private data class DeltaParts(
        val reasoning: String = "",
        val content: String = "",
        val finishReason: String? = null
    )

    // Extract delta text piece from SSE JSON chunk for chat.completions-like APIs
    private fun extractDeltaPartsFromSseJson(payload: String): DeltaParts {
        return try {
            val element = JsonParser.parseString(payload)
            if (!element.isJsonObject) return DeltaParts()
            val obj = element.asJsonObject
            val choices = obj.getAsJsonArray("choices") ?: return DeltaParts()
            val first = choices.firstOrNull()?.asJsonObject ?: return DeltaParts()

            fun pickText(value: JsonElement?): String {
                if (value == null || value.isJsonNull) return ""
                if (value.isJsonPrimitive) return value.asString
                if (value.isJsonArray) {
                    val out = StringBuilder()
                    for (item in value.asJsonArray) {
                        out.append(pickText(item))
                    }
                    return out.toString()
                }
                if (value.isJsonObject) {
                    val o = value.asJsonObject
                    val type = o.get("type")?.asString
                    // For typed multimodal blocks, only consume explicit text blocks.
                    if (type != null && type != "text") {
                        return ""
                    }
                    o.get("text")?.let {
                        val text = pickText(it)
                        if (text.isNotEmpty()) return text
                    }
                    o.get("content")?.let {
                        val text = pickText(it)
                        if (text.isNotEmpty()) return text
                    }
                    o.get("value")?.let {
                        val text = pickText(it)
                        if (text.isNotEmpty()) return text
                    }
                }
                return ""
            }

            val delta = first.getAsJsonObject("delta")
            val reasoning = listOf("reasoning_content", "reasoning", "thinking")
                .asSequence()
                .map { key -> pickText(delta?.get(key)) }
                .firstOrNull { it.isNotEmpty() } ?: ""

            val content = listOf("content", "text")
                .asSequence()
                .map { key -> pickText(delta?.get(key)) }
                .firstOrNull { it.isNotEmpty() } ?: ""

            // Keep only lightweight per-chunk fallback; avoid full-message snapshot fields
            // (e.g. message.content) that can corrupt streaming order.
            val fallbackContent = if (content.isEmpty()) {
                val choiceFallbacks = listOf(first.get("text"))
                choiceFallbacks
                    .asSequence()
                    .map { candidate -> pickText(candidate) }
                    .firstOrNull { it.isNotEmpty() } ?: ""
            } else {
                content
            }

            val finishReason = first.get("finish_reason")
                ?.takeIf { !it.isJsonNull && it.isJsonPrimitive }
                ?.asString

            DeltaParts(
                reasoning = reasoning,
                content = fallbackContent,
                finishReason = finishReason
            )
        } catch (e: Exception) {
            DeltaParts()
        }
    }

    private fun joinReasoningAndAnswer(reasoning: String?, answer: String): String {
        val r = reasoning?.trim().orEmpty()
        val a = answer.trim()
        return when {
            r.isNotEmpty() && a.isNotEmpty() -> "<think>$r</think>\n$a"
            r.isNotEmpty() -> "<think>$r</think>"
            else -> a
        }
    }

    private suspend fun prepareOpenAiRequest(
        messages: List<Message>,
        modelInfo: ModelInfo,
        config: AssistantModelConfig,
        apiKey: String,
        stream: Boolean,
        debugScopeId: String?
    ): PreparedOpenAiRequest? {
        val processedMessages = preprocessMessages(messages)
        logInputTextIfDebug(config.serviceProvider, processedMessages)

        val requestSettings = repository.getAssistantRequestSettings(assistantId)
        val payload = JsonObject().apply {
            addProperty("model", modelInfo.apiCode)
            add("messages", buildMessagesJson(processedMessages))
            if (stream) {
                addProperty("stream", true)
            }
            addProperty("temperature", requestSettings.temperature)
            addProperty("top_p", requestSettings.topP)
            if (requestSettings.maxTokensEnabled) {
                addProperty("max_tokens", requestSettings.maxTokens)
            }
            if (isBuiltinWebSearchEnabled()) {
                add("web_search_options", JsonObject().apply {
                    addProperty("search_context_size", "medium")
                })
            }
            applyCustomParams(this, requestSettings.customParams)
        }

        val endpoint = ApiUrlResolver.resolveChatUrlForProvider(repository, config.serviceProvider)
        val httpUrl = endpoint?.toHttpUrlOrNull() ?: return null
        logRequestBodyIfDebug(config.serviceProvider, httpUrl, payload, debugScopeId)

        val requestBody = Gson().toJson(payload).toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(httpUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .apply {
                if (stream) {
                    addHeader("Accept", "text/event-stream")
                    addHeader("Cache-Control", "no-cache")
                }
            }
            .post(requestBody)
            .build()
        return PreparedOpenAiRequest(processedMessages, payload, request, httpUrl)
    }

    private suspend fun preprocessMessages(messages: List<Message>): List<Message> = withContext(Dispatchers.IO) {
        val needsImageNormalization = messages.any { message ->
            val content = message.content
            when (content) {
                is List<*> -> content.any { item ->
                    when (item) {
                        is ContentItem -> item.type == "image_url" &&
                            item.image_url?.url?.let { url -> !url.startsWith("http") && !url.startsWith("data:") } == true
                        is Map<*, *> -> {
                            val type = item["type"] as? String
                            val imageUrl = item["image_url"] as? Map<*, *>
                            val url = imageUrl?.get("url") as? String
                            type == "image_url" && !url.isNullOrBlank() &&
                                !url.startsWith("http") &&
                                !url.startsWith("data:")
                        }
                        else -> false
                    }
                }
                else -> false
            }
        }
        if (!needsImageNormalization) {
            return@withContext messages
        }
        messages.map { message ->
            when (val content = message.content) {
                is List<*> -> {
                    val processedContent = content.map { item ->
                        when (item) {
                            is ContentItem -> {
                                if (item.type != "image_url") return@map item
                                val url = item.image_url?.url
                                val converted = url?.takeUnless { it.startsWith("http") || it.startsWith("data:") }
                                    ?.let { ImageUtils.convertImageToDataUrl(repository.getContext(), it) }
                                if (converted != null) item.copy(image_url = ImageUrl(url = converted)) else item
                            }
                            is Map<*, *> -> {
                                val type = item["type"] as? String
                                if (type != "image_url") return@map item
                                val imageUrl = item["image_url"] as? Map<*, *> ?: return@map item
                                val url = imageUrl["url"] as? String
                                val converted = url?.takeUnless { it.startsWith("http") || it.startsWith("data:") }
                                    ?.let { ImageUtils.convertImageToDataUrl(repository.getContext(), it) }
                                if (converted != null) {
                                    mapOf(
                                        "type" to "image_url",
                                        "image_url" to mapOf("url" to converted)
                                    )
                                } else {
                                    item
                                }
                            }
                            else -> item
                        }
                    }
                    Message(message.role, processedContent)
                }
                else -> message
            }
        }
    }

    private fun parseSseStream(scope: ProducerScope<ChatEvent>, body: ResponseBody, trace: RequestTrace) {
        val reader = BufferedReader(InputStreamReader(body.byteStream()))
        try {
            var line: String?
            val dataLines = mutableListOf<String>()
            var shouldStop = false
            var emittedTextSoFar = ""
            var streamMode = 0
            var thinkOpen = false
            var firstChunkSeen = false

            fun emitChunk(text: String) {
                if (text.isBlank()) return
                if (!firstChunkSeen) {
                    firstChunkSeen = true
                    trace.mark("first chunk received")
                }
                scope.trySend(ChatEvent.Chunk(text))
            }

            fun handleEvent(lines: List<String>) {
                if (lines.isEmpty()) return
                val payload = lines.joinToString("\n")
                if (payload == "[DONE]" || payload == "{\"done\":true}") {
                    if (thinkOpen) {
                        emitChunk("</think>")
                        thinkOpen = false
                    }
                    scope.trySend(ChatEvent.Done)
                    shouldStop = true
                    return
                }
                ModelErrorParser.extractErrorFromJson(payload)?.let { modelError ->
                    scope.trySend(ChatEvent.Error(ModelErrorParser.normalizeUserError(modelError)))
                    shouldStop = true
                    return
                }
                val deltaParts = try {
                    extractDeltaPartsFromSseJson(payload)
                } catch (_: Exception) {
                    DeltaParts(content = extractTextFromJson(payload))
                }
                if (deltaParts.reasoning.isNotEmpty()) {
                    if (!thinkOpen) {
                        emitChunk("<think>")
                        thinkOpen = true
                    }
                    emitChunk(deltaParts.reasoning)
                }
                val piece = deltaParts.content
                if (piece.isNotEmpty()) {
                    if (thinkOpen) {
                        emitChunk("</think>")
                        thinkOpen = false
                    }
                    val delta = when {
                        emittedTextSoFar.isEmpty() -> piece
                        streamMode == 2 -> if (piece.startsWith(emittedTextSoFar)) piece.removePrefix(emittedTextSoFar) else ""
                        streamMode == 1 -> piece
                        piece.startsWith(emittedTextSoFar) -> {
                            streamMode = 2
                            piece.removePrefix(emittedTextSoFar)
                        }
                        else -> {
                            streamMode = 1
                            piece
                        }
                    }
                    if (delta.isNotEmpty()) {
                        emittedTextSoFar += delta
                        emitChunk(delta)
                    }
                }
                if (deltaParts.finishReason != null && thinkOpen) {
                    emitChunk("</think>")
                    thinkOpen = false
                }
            }

            while (reader.readLine().also { line = it } != null) {
                val raw = line ?: continue
                if (raw.isBlank()) {
                    handleEvent(dataLines.toList())
                    dataLines.clear()
                    if (shouldStop) break
                    continue
                }
                if (raw.startsWith("data:")) {
                    val payload = raw.substringAfter("data:").let {
                        if (it.startsWith(" ")) it.substring(1) else it
                    }
                    dataLines.add(payload)
                    val completePayload =
                        payload == "[DONE]" ||
                            payload == "{\"done\":true}" ||
                            (payload.startsWith("{") && payload.endsWith("}")) ||
                            (payload.startsWith("[") && payload.endsWith("]"))
                    if (completePayload) {
                        handleEvent(dataLines.toList())
                        dataLines.clear()
                        if (shouldStop) break
                    }
                } else if (raw.startsWith("{") || raw.startsWith("[")) {
                    handleEvent(listOf(raw))
                    if (shouldStop) break
                }
            }
            if (!shouldStop && dataLines.isNotEmpty()) {
                handleEvent(dataLines.toList())
            }
            if (!shouldStop) {
                if (thinkOpen) {
                    emitChunk("</think>")
                }
                scope.trySend(ChatEvent.Done)
            }
        } catch (e: Exception) {
            scope.trySend(ChatEvent.Error(ModelErrorParser.parseThrowable(e)))
        } finally {
            reader.close()
            scope.close()
        }
    }

    private fun createOkHttpClient(streaming: Boolean): OkHttpClient {
        return OnlineHttpClientProvider.get(streaming = streaming, debugLogging = repository.getDebugMode())
    }

    private fun parseEmbeddingsResponse(json: String): List<List<Float>> {
        val root = JsonParser.parseString(json).asJsonObject
        val data = root.getAsJsonArray("data") ?: return emptyList()
        return data.mapNotNull { item ->
            if (!item.isJsonObject) return@mapNotNull null
            val vector = item.asJsonObject.getAsJsonArray("embedding") ?: return@mapNotNull null
            vector.mapNotNull { it.takeIf { e -> e.isJsonPrimitive }?.asFloat }
        }
    }

    private suspend fun getAnthropicResponse(
        messages: List<Message>,
        modelInfo: ModelInfo,
        config: AssistantModelConfig,
        debugScopeId: String?
    ): String {
        return try {
            val apiKey = repository.getApiKeyForProvider(config.serviceProvider)
            if (apiKey.isEmpty()) {
                return ModelErrorParser.normalizeUserError(repository.getContext().getString(R.string.error_api_key_missing))
            }
            val baseUrl = ApiUrlResolver.resolveBaseUrlForProvider(repository, config.serviceProvider)
            val endpoint = resolveAnthropicUrl(baseUrl)
                ?: return ModelErrorParser.normalizeUserError(repository.getContext().getString(R.string.error_invalid_api_url))

            logInputTextIfDebug(config.serviceProvider, messages)
            val requestSettings = repository.getAssistantRequestSettings(assistantId)
            val payload = buildAnthropicPayload(messages, modelInfo, requestSettings, stream = false)
            val requestBody = payload.toString().toRequestBody("application/json".toMediaTypeOrNull())
            logRequestBodyIfDebug(config.serviceProvider, endpoint, payload, debugScopeId)

            val req = Request.Builder()
                .url(endpoint)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val client = createOkHttpClient(streaming = false)
            withContext(Dispatchers.IO) {
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        val errorBody = resp.body?.string()
                        return@withContext ModelErrorParser.parseHttpError(resp.code, resp.message, errorBody)
                    }
                    val body = resp.body?.string().orEmpty()
                    ModelErrorParser.extractErrorFromJson(body)?.let { err ->
                        return@withContext ModelErrorParser.normalizeUserError(err)
                    }
                    extractAnthropicText(body).ifBlank {
                        ModelErrorParser.normalizeUserError(repository.getContext().getString(R.string.error_no_content))
                    }
                }
            }
        } catch (e: Exception) {
            ModelErrorParser.parseThrowable(e)
        }
    }

    private suspend fun getGeminiResponse(
        messages: List<Message>,
        modelInfo: ModelInfo,
        config: AssistantModelConfig,
        debugScopeId: String?
    ): String {
        return try {
            val apiKey = repository.getApiKeyForProvider(config.serviceProvider)
            if (apiKey.isEmpty()) {
                return ModelErrorParser.normalizeUserError(repository.getContext().getString(R.string.error_api_key_missing))
            }
            val baseUrl = ApiUrlResolver.resolveBaseUrlForProvider(repository, config.serviceProvider)
            val endpoint = resolveGeminiGenerateUrl(baseUrl, modelInfo.apiCode, apiKey)
                ?: return ModelErrorParser.normalizeUserError(repository.getContext().getString(R.string.error_invalid_api_url))

            logInputTextIfDebug(config.serviceProvider, messages)
            val requestSettings = repository.getAssistantRequestSettings(assistantId)
            val payload = buildGeminiPayload(messages, assistantId, requestSettings, stream = false)
            val requestBody = payload.toString().toRequestBody("application/json".toMediaTypeOrNull())
            logRequestBodyIfDebug(config.serviceProvider, endpoint, payload, debugScopeId)

            val req = Request.Builder()
                .url(endpoint)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val client = createOkHttpClient(streaming = false)
            withContext(Dispatchers.IO) {
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        val errorBody = resp.body?.string()
                        return@withContext ModelErrorParser.parseHttpError(resp.code, resp.message, errorBody)
                    }
                    val body = resp.body?.string().orEmpty()
                    ModelErrorParser.extractErrorFromJson(body)?.let { err ->
                        return@withContext ModelErrorParser.normalizeUserError(err)
                    }
                    extractGeminiText(body).ifBlank {
                        ModelErrorParser.normalizeUserError(repository.getContext().getString(R.string.error_no_content))
                    }
                }
            }
        } catch (e: Exception) {
            ModelErrorParser.parseThrowable(e)
        }
    }

    private fun streamAnthropicResponse(
        messages: List<Message>,
        modelInfo: ModelInfo,
        config: AssistantModelConfig,
        debugScopeId: String?
    ): Flow<ChatEvent> = channelFlow {
        val apiKey = repository.getApiKeyForProvider(config.serviceProvider)
        if (apiKey.isEmpty()) {
            trySend(ChatEvent.Error(ModelErrorParser.normalizeUserError(repository.getContext().getString(R.string.error_api_key_missing))))
            close()
            return@channelFlow
        }
        val baseUrl = ApiUrlResolver.resolveBaseUrlForProvider(repository, config.serviceProvider)
        val endpoint = resolveAnthropicUrl(baseUrl)
            ?: run {
                trySend(ChatEvent.Error(ModelErrorParser.normalizeUserError(repository.getContext().getString(R.string.error_invalid_api_url))))
                close()
                return@channelFlow
            }

        val requestSettings = repository.getAssistantRequestSettings(assistantId)
        val payload = buildAnthropicPayload(messages, modelInfo, requestSettings, stream = true)
        val requestBody = payload.toString().toRequestBody("application/json".toMediaTypeOrNull())
        logRequestBodyIfDebug(config.serviceProvider, endpoint, payload, debugScopeId)

        val req = Request.Builder()
            .url(endpoint)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Accept", "text/event-stream")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val client = createOkHttpClient(streaming = true)
        val call = client.newCall(req)
        withContext(Dispatchers.IO) {
            call.execute().use { resp ->
                if (!resp.isSuccessful) {
                    val errorBody = resp.body?.string()
                    trySend(ChatEvent.Error(ModelErrorParser.parseHttpError(resp.code, resp.message, errorBody)))
                    close()
                    return@withContext
                }
                val body = resp.body ?: run {
                    trySend(ChatEvent.Error(ModelErrorParser.normalizeUserError(repository.getContext().getString(R.string.error_empty_response))))
                    close()
                    return@withContext
                }
                val reader = BufferedReader(InputStreamReader(body.byteStream()))
                try {
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val raw = line ?: continue
                        if (!raw.startsWith("data:")) continue
                        val payloadLine = raw.substringAfter("data:").trim()
                        if (payloadLine.isBlank()) continue
                        if (payloadLine == "[DONE]") {
                            trySend(ChatEvent.Done)
                            break
                        }
                        val element = runCatching { JsonParser.parseString(payloadLine) }.getOrNull()
                        if (element == null || !element.isJsonObject) continue
                        val obj = element.asJsonObject
                        val type = obj.get("type")?.asString.orEmpty()
                        if (type == "content_block_delta") {
                            val delta = obj.getAsJsonObject("delta")
                            val text = delta?.get("text")?.asString.orEmpty()
                            if (text.isNotEmpty()) {
                                trySend(ChatEvent.Chunk(text))
                            }
                        } else if (type == "message_stop") {
                            trySend(ChatEvent.Done)
                            break
                        } else if (type == "error") {
                            val message = obj.getAsJsonObject("error")?.get("message")?.asString
                            trySend(ChatEvent.Error(ModelErrorParser.normalizeUserError(message)))
                            break
                        }
                    }
                } catch (e: Exception) {
                    trySend(ChatEvent.Error(ModelErrorParser.parseThrowable(e)))
                } finally {
                    reader.close()
                    close()
                }
            }
        }

        awaitClose { call.cancel() }
    }

    private fun streamGeminiResponse(
        messages: List<Message>,
        modelInfo: ModelInfo,
        config: AssistantModelConfig,
        debugScopeId: String?
    ): Flow<ChatEvent> = channelFlow {
        val apiKey = repository.getApiKeyForProvider(config.serviceProvider)
        if (apiKey.isEmpty()) {
            trySend(ChatEvent.Error(ModelErrorParser.normalizeUserError(repository.getContext().getString(R.string.error_api_key_missing))))
            close()
            return@channelFlow
        }
        val baseUrl = ApiUrlResolver.resolveBaseUrlForProvider(repository, config.serviceProvider)
        val endpoint = resolveGeminiGenerateUrl(baseUrl, modelInfo.apiCode, apiKey, stream = true)
            ?: run {
                trySend(ChatEvent.Error(ModelErrorParser.normalizeUserError(repository.getContext().getString(R.string.error_invalid_api_url))))
                close()
                return@channelFlow
            }

        val requestSettings = repository.getAssistantRequestSettings(assistantId)
        val payload = buildGeminiPayload(messages, assistantId, requestSettings, stream = true)
        val requestBody = payload.toString().toRequestBody("application/json".toMediaTypeOrNull())
        logRequestBodyIfDebug(config.serviceProvider, endpoint, payload, debugScopeId)

        val req = Request.Builder()
            .url(endpoint)
            .addHeader("Accept", "text/event-stream")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val client = createOkHttpClient(streaming = true)
        val call = client.newCall(req)
        withContext(Dispatchers.IO) {
            call.execute().use { resp ->
                if (!resp.isSuccessful) {
                    val errorBody = resp.body?.string()
                    trySend(ChatEvent.Error(ModelErrorParser.parseHttpError(resp.code, resp.message, errorBody)))
                    close()
                    return@withContext
                }
                val body = resp.body ?: run {
                    trySend(ChatEvent.Error(ModelErrorParser.normalizeUserError(repository.getContext().getString(R.string.error_empty_response))))
                    close()
                    return@withContext
                }
                val reader = BufferedReader(InputStreamReader(body.byteStream()))
                var emittedTextSoFar = ""
                var streamMode = 0
                try {
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val raw = line ?: continue
                        if (!raw.startsWith("data:")) continue
                        val payloadLine = raw.substringAfter("data:").trim()
                        if (payloadLine.isBlank()) continue
                        if (payloadLine == "[DONE]") {
                            trySend(ChatEvent.Done)
                            break
                        }
                        val text = extractGeminiText(payloadLine)
                        if (text.isNotEmpty()) {
                            val delta = when {
                                emittedTextSoFar.isEmpty() -> text
                                streamMode == 2 -> {
                                    if (text.startsWith(emittedTextSoFar)) {
                                        text.removePrefix(emittedTextSoFar)
                                    } else {
                                        ""
                                    }
                                }
                                streamMode == 1 -> text
                                text.startsWith(emittedTextSoFar) -> {
                                    streamMode = 2
                                    text.removePrefix(emittedTextSoFar)
                                }
                                else -> {
                                    streamMode = 1
                                    text
                                }
                            }
                            if (delta.isNotEmpty()) {
                                emittedTextSoFar += delta
                                trySend(ChatEvent.Chunk(delta))
                            }
                        }
                    }
                    trySend(ChatEvent.Done)
                } catch (e: Exception) {
                    trySend(ChatEvent.Error(ModelErrorParser.parseThrowable(e)))
                } finally {
                    reader.close()
                    close()
                }
            }
        }

        awaitClose { call.cancel() }
    }

    private suspend fun getGeminiEmbeddings(
        inputs: List<String>,
        modelInfo: ModelInfo,
        config: AssistantModelConfig
    ): List<List<Float>> {
        val apiKey = repository.getApiKeyForProvider(config.serviceProvider)
        if (apiKey.isEmpty()) {
            throw IllegalStateException(repository.getContext().getString(R.string.error_api_key_missing))
        }
        val baseUrl = ApiUrlResolver.resolveBaseUrlForProvider(repository, config.serviceProvider)
        val endpoint = resolveGeminiEmbedUrl(baseUrl, modelInfo.apiCode, apiKey)
            ?: throw IllegalStateException(repository.getContext().getString(R.string.error_invalid_api_url))

        if (repository.getDebugMode()) {
            debugLog(TAG) { "embedding input text provider=${config.serviceProvider}\n${inputs.joinToString("\n")}" }
        }

        val payload = JsonObject().apply {
            val contents = JsonArray()
            if (inputs.size == 1) {
                val content = JsonObject()
                val parts = JsonArray()
                parts.add(JsonObject().apply { addProperty("text", inputs.first()) })
                content.add("parts", parts)
                contents.add(content)
            } else {
                inputs.forEach { input ->
                    val content = JsonObject()
                    val parts = JsonArray()
                    parts.add(JsonObject().apply { addProperty("text", input) })
                    content.add("parts", parts)
                    contents.add(content)
                }
            }
            add("contents", contents)
        }

        val requestBody = payload.toString().toRequestBody("application/json".toMediaTypeOrNull())
        logRequestBodyIfDebug(config.serviceProvider, endpoint, payload)
        val req = Request.Builder()
            .url(endpoint)
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val client = createOkHttpClient(streaming = false)
        return withContext(Dispatchers.IO) {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val errorBody = resp.body?.string()
                    throw IllegalStateException(ModelErrorParser.parseHttpError(resp.code, resp.message, errorBody))
                }
                val body = resp.body?.string().orEmpty()
                parseGeminiEmbeddings(body)
            }
        }
    }

    private fun buildMessagesJson(messages: List<Message>): JsonArray {
        val messagesArray = JsonArray()
        for (message in messages) {
            val messageObj = JsonObject()
            messageObj.addProperty("role", message.role)
            when (val content = message.content) {
                is String -> {
                    messageObj.addProperty("content", content)
                }
                is List<*> -> {
                    val contentArray = JsonArray()
                    for (item in content) {
                        when (item) {
                            is ContentItem -> {
                                val contentItem = JsonObject()
                                contentItem.addProperty("type", item.type)
                                when (item.type) {
                                    "text" -> contentItem.addProperty("text", item.text ?: "")
                                    "image_url" -> {
                                        val imageUrlObj = JsonObject()
                                        imageUrlObj.addProperty("url", item.image_url?.url ?: "")
                                        contentItem.add("image_url", imageUrlObj)
                                    }
                                }
                                contentArray.add(contentItem)
                            }
                            is Map<*, *> -> {
                                val contentItem = JsonObject()
                                val type = item["type"] as? String
                                contentItem.addProperty("type", type)
                                when (type) {
                                    "text" -> {
                                        val text = item["text"] as? String
                                        contentItem.addProperty("text", text)
                                    }
                                    "image_url" -> {
                                        val imageUrl = item["image_url"] as? Map<*, *>
                                        if (imageUrl != null) {
                                            val url = imageUrl["url"] as? String
                                            val imageUrlObj = JsonObject()
                                            imageUrlObj.addProperty("url", url)
                                            contentItem.add("image_url", imageUrlObj)
                                        }
                                    }
                                }
                                contentArray.add(contentItem)
                            }
                        }
                    }
                    messageObj.add("content", contentArray)
                }
            }
            messagesArray.add(messageObj)
        }
        return messagesArray
    }

    private fun extractTextFromContent(content: Any): String {
        return when (content) {
            is String -> content
            is List<*> -> {
                val textItems = content.mapNotNull { item ->
                    when (item) {
                        is ContentItem -> if (item.type == "text") item.text else null
                        is Map<*, *> -> if (item["type"] == "text") item["text"] as? String else null
                        else -> null
                    }
                }
                textItems.joinToString(" ")
            }
            else -> ""
        }
    }

    private fun buildAnthropicPayload(
        messages: List<Message>,
        modelInfo: ModelInfo,
        requestSettings: AssistantRequestSettings,
        stream: Boolean
    ): JsonObject {
        val systemText = StringBuilder()
        val anthropicMessages = JsonArray()
        messages.forEach { message ->
            val text = extractTextFromContent(message.content)
            if (text.isBlank()) return@forEach
            if (message.role == "system") {
                if (systemText.isNotEmpty()) systemText.append("\n")
                systemText.append(text)
            } else {
                val msg = JsonObject()
                msg.addProperty("role", message.role)
                val content = JsonArray()
                content.add(JsonObject().apply {
                    addProperty("type", "text")
                    addProperty("text", text)
                })
                msg.add("content", content)
                anthropicMessages.add(msg)
            }
        }
        return JsonObject().apply {
            addProperty("model", modelInfo.apiCode)
            if (systemText.isNotEmpty()) {
                addProperty("system", systemText.toString())
            }
            add("messages", anthropicMessages)
            addProperty("stream", stream)
            addProperty("temperature", requestSettings.temperature)
            addProperty("top_p", requestSettings.topP)
            addProperty("max_tokens", if (requestSettings.maxTokensEnabled) requestSettings.maxTokens else 1024)
            if (isBuiltinWebSearchEnabled()) {
                add("tools", JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("type", "web_search_20250305")
                        addProperty("name", "web_search")
                        addProperty("max_uses", repository.getWebSearchResultCount().coerceIn(1, 20))
                    })
                })
            }
            applyCustomParams(this, requestSettings.customParams)
        }
    }

    private fun buildGeminiPayload(
        messages: List<Message>,
        assistantId: String,
        requestSettings: AssistantRequestSettings,
        stream: Boolean
    ): JsonObject {
        val contents = JsonArray()
        var systemInstruction: String? = null
        messages.forEach { message ->
            val text = extractTextFromContent(message.content)
            if (text.isBlank()) return@forEach
            if (message.role == "system") {
                systemInstruction = if (systemInstruction == null) text else "${systemInstruction}\n$text"
            } else {
                val content = JsonObject()
                content.addProperty("role", if (message.role == "assistant") "model" else "user")
                val parts = JsonArray()
                parts.add(JsonObject().apply { addProperty("text", text) })
                content.add("parts", parts)
                contents.add(content)
            }
        }
        return JsonObject().apply {
            add("contents", contents)
            systemInstruction?.let { instruction ->
                add("systemInstruction", JsonObject().apply {
                    val parts = JsonArray()
                    parts.add(JsonObject().apply { addProperty("text", instruction) })
                    add("parts", parts)
                })
            }
            add("generationConfig", JsonObject().apply {
                addProperty("temperature", requestSettings.temperature)
                addProperty("topP", requestSettings.topP)
                if (requestSettings.maxTokensEnabled) {
                    addProperty("maxOutputTokens", requestSettings.maxTokens)
                }
            })
            if (!repository.getAssistantReasoningEnabled(assistantId)) {
                add("thinkingConfig", JsonObject().apply {
                    addProperty("thinkingBudget", 0)
                })
            }
            if (isBuiltinWebSearchEnabled()) {
                add("tools", JsonArray().apply {
                    add(JsonObject().apply {
                        add("googleSearch", JsonObject())
                    })
                })
            }
            if (stream) {
                addProperty("stream", true)
            }
            applyCustomParams(this, requestSettings.customParams)
        }
    }

    private fun resolveAnthropicUrl(baseUrl: String): HttpUrl? {
        val base = baseUrl.trim().ifBlank { "https://api.anthropic.com/v1/messages" }
        val normalized = base.trimEnd('/')
        val resolved = when {
            normalized.endsWith("/v1/messages") -> normalized
            normalized.contains("/v1/") -> "$normalized/messages"
            else -> "$normalized/v1/messages"
        }
        return resolved.toHttpUrlOrNull()
    }

    private fun resolveGeminiGenerateUrl(
        baseUrl: String,
        modelId: String,
        apiKey: String,
        stream: Boolean = false
    ): HttpUrl? {
        val base = baseUrl.trim().ifBlank { "https://generativelanguage.googleapis.com" }.trimEnd('/')
        val id = modelId.removePrefix("models/")
        val path = if (stream) ":streamGenerateContent" else ":generateContent"
        val url = "$base/v1beta/models/$id$path?key=$apiKey"
        return url.toHttpUrlOrNull()
    }

    private fun resolveGeminiEmbedUrl(baseUrl: String, modelId: String, apiKey: String): HttpUrl? {
        val base = baseUrl.trim().ifBlank { "https://generativelanguage.googleapis.com" }.trimEnd('/')
        val id = modelId.removePrefix("models/")
        val url = "$base/v1beta/models/$id:embedContent?key=$apiKey"
        return url.toHttpUrlOrNull()
    }

    private fun extractAnthropicText(json: String): String {
        return try {
            val element = JsonParser.parseString(json)
            if (!element.isJsonObject) return ""
            val obj = element.asJsonObject
            val content = obj.getAsJsonArray("content") ?: return ""
            val out = StringBuilder()
            content.forEach { item ->
                if (item.isJsonObject) {
                    val text = item.asJsonObject.get("text")?.asString
                    if (!text.isNullOrBlank()) {
                        if (out.isNotEmpty()) out.append("\n")
                        out.append(text)
                    }
                }
            }
            out.toString()
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractGeminiText(json: String): String {
        return try {
            val element = JsonParser.parseString(json)
            if (!element.isJsonObject) return ""
            val obj = element.asJsonObject
            val candidates = obj.getAsJsonArray("candidates") ?: return ""
            val first = candidates.firstOrNull()?.asJsonObject ?: return ""
            val content = first.getAsJsonObject("content") ?: return ""
            val parts = content.getAsJsonArray("parts") ?: return ""
            val out = StringBuilder()
            parts.forEach { part ->
                if (part.isJsonObject) {
                    val text = part.asJsonObject.get("text")?.asString
                    if (!text.isNullOrBlank()) {
                        out.append(text)
                    }
                }
            }
            out.toString()
        } catch (_: Exception) {
            ""
        }
    }

    private fun parseGeminiEmbeddings(json: String): List<List<Float>> {
        val root = JsonParser.parseString(json).asJsonObject
        val embeddings = root.getAsJsonArray("embeddings") ?: return emptyList()
        return embeddings.mapNotNull { item ->
            if (!item.isJsonObject) return@mapNotNull null
            val values = item.asJsonObject.getAsJsonArray("values") ?: return@mapNotNull null
            values.mapNotNull { it.takeIf { e -> e.isJsonPrimitive }?.asFloat }
        }
    }

    private fun applyCustomParams(target: JsonObject, customParams: String) {
        if (customParams.isBlank()) return
        try {
            val element = JsonParser.parseString(customParams)
            if (!element.isJsonObject) return
            val obj = element.asJsonObject
            obj.entrySet().forEach { (key, value) ->
                target.add(key, value)
            }
        } catch (_: Exception) {
        }
    }
}

private object OnlineHttpClientProvider {
    private val clients = ConcurrentHashMap<String, OkHttpClient>()

    fun get(streaming: Boolean, debugLogging: Boolean): OkHttpClient {
        val key = "${if (streaming) "stream" else "default"}:${if (debugLogging) "debug" else "plain"}"
        return clients.getOrPut(key) {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(if (streaming) 0 else 30, TimeUnit.SECONDS)
                .apply {
                    if (debugLogging) {
                        addInterceptor(
                            HttpLoggingInterceptor().apply {
                                level = HttpLoggingInterceptor.Level.HEADERS
                                redactHeader("Authorization")
                            }
                        )
                    }
                }
                .build()
        }
    }
}
