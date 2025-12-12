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
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
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

            val retrofit = Retrofit.Builder()
                .baseUrl(repository.getBaseUrl())
                .client(createOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val apiService = retrofit.create(OpenAiApiService::class.java)

            val request = OpenAiRequest(
                model = modelInfo.apiCode,
                messages = messages
            )

            val response = apiService.getChatCompletion("Bearer $apiKey", "application/json", request)
            if (!response.isSuccessful) {
                return "错误：${response.code()} ${response.message()}"
            }
            val body = response.body()
            return body?.choices?.firstOrNull()?.message?.content ?: "错误：无返回内容"
        } catch (e: Exception) {
            return "错误：${e.message ?: e.javaClass.simpleName}"
        }
    }

    override fun streamAIResponse(messages: List<Message>, modelInfo: ModelInfo): Flow<ChatEvent> = channelFlow {
        val apiKey = repository.getApiKey()
        if (apiKey.isEmpty()) {
            trySend(ChatEvent.Error("错误：API密钥未设置"))
            close()
            return@channelFlow
        }

        val client = createOkHttpClient()

        // Build JSON payload with stream=true
        val gson = com.google.gson.Gson()
        val json = com.google.gson.JsonObject().apply {
            addProperty("model", modelInfo.apiCode)
            add("messages", gson.toJsonTree(messages))
            addProperty("stream", true)
        }
        val mediaType = "application/json".toMediaTypeOrNull()
        val requestBody = gson.toJson(json).toRequestBody(mediaType)

        // 使用 HttpUrl 确保 URL 格式稳定且保留末尾斜杠
        val base = repository.getBaseUrl()
        val httpUrl: HttpUrl = base.toHttpUrlOrNull()
            ?: (if (base.endsWith("/")) base else "$base/").toHttpUrlOrNull()
            ?: run {
                trySend(ChatEvent.Error("错误：无效的API地址"))
                close()
                return@channelFlow
            }

        val req = Request.Builder()
            .url(httpUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Accept", "text/event-stream")
            .addHeader("Cache-Control", "no-cache")
            .post(requestBody)
            .build()

        val call = client.newCall(req)
        withContext(Dispatchers.IO) {
            val resp = call.execute()
            if (!resp.isSuccessful) {
                trySend(ChatEvent.Error("错误：${resp.code} ${resp.message}"))
                close()
                return@withContext
            }

            val body = resp.body ?: run {
                trySend(ChatEvent.Error("错误：响应为空"))
                close()
                return@withContext
            }

            val contentType = resp.header("Content-Type") ?: ""
            // If it's not SSE, read entire JSON once and emit parsed text only
            if (!contentType.contains("text/event-stream", ignoreCase = true)) {
                try {
                    val full = body.string()
                    val text = extractTextFromJson(full)
                    if (text.isNotEmpty()) {
                        trySend(ChatEvent.Chunk(text))
                    } else {
                        trySend(ChatEvent.Error("错误：解析响应失败"))
                    }
                    trySend(ChatEvent.Done)
                } catch (e: Exception) {
                    trySend(ChatEvent.Error("错误：${e.message}"))
                } finally {
                    body.close()
                    close()
                }
                return@withContext
            }

            // SSE streaming parsing
            val reader = BufferedReader(InputStreamReader(body.byteStream()))
            try {
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val raw = line?.trim() ?: continue
                    if (raw.isEmpty()) continue

                    // Expect lines like: "data: {json}"
                    val payload = when {
                        raw.startsWith("data:") -> raw.substringAfter("data:").trim()
                        // Some providers may send pure JSON lines without the prefix
                        raw.startsWith("{") || raw.startsWith("[") -> raw
                        else -> null
                    } ?: continue

                    if (payload == "[DONE]" || payload == "{\"done\":true}" ) {
                        trySend(ChatEvent.Done)
                        break
                    }

                    // Parse JSON and extract incremental content
                    val piece = try {
                        extractDeltaFromSseJson(payload)
                    } catch (e: Exception) {
                        // As a fallback, try full message extractor
                        extractTextFromJson(payload)
                    }
                    if (piece.isNotEmpty()) {
                        trySend(ChatEvent.Chunk(piece))
                    }
                }
                // If loop ends without explicit done, send Done
                trySend(ChatEvent.Done)
            } catch (e: Exception) {
                trySend(ChatEvent.Error("错误：${e.message}"))
            } finally {
                reader.close()
                body.close()
                close()
            }
        }

        awaitClose { call.cancel() }
    }

    // Extract full text from a non-stream JSON response
    private fun extractTextFromJson(json: String): String {
        return try {
            val element = com.google.gson.JsonParser.parseString(json)
            if (!element.isJsonObject) return ""
            val obj = element.asJsonObject

            // Common OpenAI-style chat completion
            obj.getAsJsonArray("choices")?.firstOrNull()?.asJsonObject?.let { choice ->
                // If already full message
                choice.getAsJsonObject("message")?.get("content")?.asString?.let { return it }
                // Some providers put text directly
                choice.get("text")?.asString?.let { return it }
            }

            // Some providers may use top-level "content" or "output_text"
            obj.get("content")?.asString ?: obj.get("output_text")?.asString ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    // Extract delta text piece from SSE JSON chunk for chat.completions-like APIs
    private fun extractDeltaFromSseJson(payload: String): String {
        return try {
            val element = com.google.gson.JsonParser.parseString(payload)
            if (!element.isJsonObject) return ""
            val obj = element.asJsonObject
            val choices = obj.getAsJsonArray("choices") ?: return ""
            val first = choices.firstOrNull()?.asJsonObject ?: return ""

            // OpenAI-compatible streaming: choices[].delta.content
            first.getAsJsonObject("delta")?.get("content")?.asString
                ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS) // allow indefinite read for streaming
            .addInterceptor(loggingInterceptor)
            .build()
    }
}
