package com.chenhongyu.huajuan.data

import com.chenhongyu.huajuan.network.Message
import com.chenhongyu.huajuan.network.OpenAiApiService
import com.chenhongyu.huajuan.network.OpenAiRequest
import com.chenhongyu.huajuan.stream.ChatEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.awaitClose
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * 在线模型API服务实现
 */
class OnlineModelApiService(private val repository: Repository) : ModelApiService {

    override fun isAvailable(): Boolean {
        // 在线模型始终可用（只要有网络）
        return true
    }

    override suspend fun getAIResponse(messages: List<Message>, modelInfo: ModelInfo): String {
        try {
            val apiKey = repository.getApiKey()
            if (apiKey.isEmpty()) {
                return "错误：API密钥未设置"
            }

            val openAiMessages = messages.map {
                Message(
                    role = if (it.role == "user") "user" else "assistant",
                    content = it.content
                )
            }

            // 构造请求对象
            val request = OpenAiRequest(
                model = modelInfo.apiCode,
                messages = openAiMessages,
                temperature = 0.7f
            )

            val apiService = createApiService()
            val response = apiService.getChatCompletion(
                authorization = "Bearer $apiKey",
                request = request
            )

            if (response.isSuccessful) {
                val openAiResponse = response.body()
                return openAiResponse?.choices?.firstOrNull()?.message?.content ?: ""
            } else {
                return "错误：HTTP ${response.code()} - ${response.message()}"
            }
        } catch (e: Exception) {
            return "错误：${e.message ?: e.javaClass.simpleName}"
        }
    }

    override fun streamAIResponse(messages: List<Message>, modelInfo: ModelInfo): Flow<ChatEvent> = channelFlow {
        // Use channelFlow so we can safely emit from IO dispatcher while collector may be on UI.
        val apiKey = repository.getApiKey()
        if (apiKey.isEmpty()) {
            trySend(ChatEvent.Error("API key not set"))
            trySend(ChatEvent.Done)
            return@channelFlow
        }

        val client = createOkHttpClient()

        // Build JSON body manually to support streaming param if server uses OpenAI-like API
        val openAiMessages = messages.map {
            Message(
                role = if (it.role == "user") "user" else "assistant",
                content = it.content
            )
        }

        val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val gson = com.google.gson.Gson()
        val bodyMap = mapOf(
            "model" to modelInfo.apiCode,
            "messages" to openAiMessages,
            "temperature" to 0.7f,
            // request streaming where supported; many providers use "stream": true
            "stream" to true
        )
        val requestBody = gson.toJson(bodyMap).toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(getBaseUrl())
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        // Perform blocking network IO on Dispatchers.IO, but send events via trySend so it's safe.
        var call: Call? = null
        try {
            withContext(Dispatchers.IO) {
                val localCall = client.newCall(request)
                call = localCall
                localCall.execute().use { resp ->
                    val now = System.currentTimeMillis()
                    //println("STREAM-DEBUG: response received status=${resp.code} protocol=${resp.protocol} at=$now thread=${Thread.currentThread().name}")
                    // Log some headers that indicate streaming behavior
                    //println("STREAM-DEBUG: headers=${resp.headers}")

                    if (!resp.isSuccessful) {
                        trySend(ChatEvent.Error("HTTP ${resp.code} - ${resp.message}"))
                        trySend(ChatEvent.Done)
                        return@withContext
                    }

                    val body = resp.body
                    if (body == null) {
                        trySend(ChatEvent.Error("Empty response body"))
                        trySend(ChatEvent.Done)
                        return@withContext
                    }

                    val reader = BufferedReader(InputStreamReader(body.byteStream()))
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        val readTime = System.currentTimeMillis()
                        val raw = line!!.trim()
                        //println("STREAM-DEBUG: readLine at=$readTime thread=${Thread.currentThread().name} raw='${raw}'")
                        if (raw.isEmpty()) continue

                        if (raw.startsWith("data:")) {
                            val payload = raw.removePrefix("data:").trim()
                            //println("STREAM-DEBUG: data-payload at=$readTime payload='${payload.take(200)}'")
                            if (payload == "[DONE]") {
                                trySend(ChatEvent.Done)
                                break
                            }

                            try {
                                val json = gson.fromJson(payload, com.google.gson.JsonObject::class.java)
                                var textChunk: String? = null
                                if (json.has("choices")) {
                                    val choices = json.getAsJsonArray("choices")
                                    if (choices.size() > 0) {
                                        val first = choices[0].asJsonObject
                                        if (first.has("delta")) {
                                            val delta = first.getAsJsonObject("delta")
                                            if (delta.has("content")) textChunk = delta.get("content").asString
                                        } else if (first.has("text")) {
                                            textChunk = first.get("text").asString
                                        }
                                    }
                                } else if (json.has("text")) {
                                    textChunk = json.get("text").asString
                                }

                                if (!textChunk.isNullOrEmpty()) {
                                    //println("STREAM-DEBUG: emitting chunk at=${System.currentTimeMillis()} len=${textChunk.length} thread=${Thread.currentThread().name} textPreview='${textChunk.take(200)}'")
                                    trySend(ChatEvent.Chunk(textChunk))
                                }
                            } catch (je: Exception) {
                                je.printStackTrace()
                                //println("STREAM-DEBUG: emitting raw payload at=${System.currentTimeMillis()}")
                                trySend(ChatEvent.Chunk(payload))
                            }
                        } else {
                            try {
                                val json = gson.fromJson(raw, com.google.gson.JsonObject::class.java)
                                if (json.has("text")) {
                                    //println("STREAM-DEBUG: emitting text field at=${System.currentTimeMillis()}")
                                    trySend(ChatEvent.Chunk(json.get("text").asString))
                                } else {
                                    //println("STREAM-DEBUG: emitting raw line at=${System.currentTimeMillis()}")
                                    trySend(ChatEvent.Chunk(raw))
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                //println("STREAM-DEBUG: emitting raw fallback at=${System.currentTimeMillis()}")
                                trySend(ChatEvent.Chunk(raw))
                            }
                        }
                    }

                    trySend(ChatEvent.Done)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(ChatEvent.Error(e.message ?: e.javaClass.simpleName))
            trySend(ChatEvent.Done)
        }

        // Cancel underlying OkHttp call if the collector cancels the Flow
        awaitClose { call?.cancel() }
    }

    // 创建OkHttpClient实例
    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // For streaming responses we must not use Level.BODY because it may read/consume the
            // response body (buffering the entire stream) which prevents incremental processing.
            // Use HEADERS for safe debugging; increase verbosity only when reproducing not-in-prod.
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS) // allow indefinite read for streaming
            .addInterceptor(loggingInterceptor)
            .build()
    }

    // 创建API服务
    private fun createApiService(): OpenAiApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(getBaseUrl())
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(OpenAiApiService::class.java)
    }

    // 获取基础URL
    private fun getBaseUrl(): String {
        val serviceProvider = repository.getServiceProvider()
        // 对于预定义的服务提供商，使用ModelDataProvider获取API URL
        val modelDataProvider = ModelDataProvider(repository)
        val predefinedUrl = modelDataProvider.getApiUrlForProvider(serviceProvider)
        if (predefinedUrl.isNotEmpty()) {
            // 预定义的服务商URL已经是正确的格式，直接返回
            // 确保URL以斜杠结尾（满足Retrofit要求）
            return if (predefinedUrl.endsWith("/")) predefinedUrl else "$predefinedUrl/"
        }

        // 对于自定义服务提供商，使用存储的URL并进行规范化处理
        val customUrl = when (serviceProvider) {
            "自定义" -> repository.getCustomApiUrl().ifEmpty { "https://api.openai.com/v1/chat/completions" }
            else -> repository.getCustomProviderApiUrl(serviceProvider).ifEmpty { "https://api.openai.com/v1/chat/completions" }
        }

        return normalizeBaseUrl(customUrl)
    }

    /**
     * 规范化基础URL，确保它以正确的格式结尾，适用于OpenAI API格式
     * 仅对用户自定义的服务商生效
     * 处理各种可能的输入格式：
     * - https://ai.soruxgpt.com
     * - https://ai.soruxgpt.com/
     * - https://ai.soruxgpt.com/v1/chat/completions
     * - https://ai.soruxgpt.com/v1/chat/completions/
     */
    private fun normalizeBaseUrl(url: String): String {
        if (url.isBlank()) return "https://api.openai.com/v1/chat/completions/"

        var normalizedUrl = url.trim()

        // 移除末尾的斜杠
        while (normalizedUrl.endsWith("/")) {
            normalizedUrl = normalizedUrl.dropLast(1)
        }

        // 检查是否以标准的OpenAI API路径结尾
        if (normalizedUrl.endsWith("/v1/chat/completions") ||
            normalizedUrl.endsWith("/v1/completions") ||
            normalizedUrl.endsWith("/v1/embeddings")) {
            // 已经是完整的API端点URL，直接返回
            return "$normalizedUrl/"
        } else if (normalizedUrl.contains("/v1/")) {
            // 如果包含其他v1端点，也直接返回
            return "$normalizedUrl/"
        }

        // 不包含v1路径，需要添加默认的chat completions路径
        // 确保URL以斜杠结尾
        if (!normalizedUrl.endsWith("/")) {
            normalizedUrl += "/"
        }

        // 确保URL以https://开头
        if (!normalizedUrl.startsWith("https://") && !normalizedUrl.startsWith("http://")) {
            normalizedUrl = "https://$normalizedUrl"
        }

        // 添加默认的v1 chat completions路径
        return "${normalizedUrl}v1/chat/completions/"
    }
}
