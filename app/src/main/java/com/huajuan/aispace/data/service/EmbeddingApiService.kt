package com.huajuan.aispace.data

import com.huajuan.aispace.R
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import com.huajuan.aispace.data.ProviderType

class EmbeddingApiService(
    private val repository: Repository,
    private val provider: String
) {
    suspend fun getEmbeddings(inputs: List<String>, modelInfo: ModelInfo): List<List<Float>> {
        if (inputs.isEmpty()) return emptyList()
        val providerType = repository.getProviderType(provider)
        if (providerType == ProviderType.Anthropic) {
            throw IllegalStateException(repository.getContext().getString(R.string.error_embedding_not_supported))
        }
        if (providerType == ProviderType.Gemini) {
            return getGeminiEmbeddings(inputs, modelInfo)
        }
        val apiKey = repository.getApiKeyForProvider(provider)
        if (apiKey.isEmpty()) {
            throw IllegalStateException(repository.getContext().getString(R.string.error_api_key_missing))
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
        val url = ApiUrlResolver.resolveEmbeddingsUrlForProvider(repository, provider)
            ?: throw IllegalStateException(repository.getContext().getString(R.string.error_invalid_api_url))

        val req = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val resp = client.newCall(req).execute()
        resp.use {
            if (!it.isSuccessful) {
                val errorBody = it.body?.string()
                throw IllegalStateException(ModelErrorParser.parseHttpError(it.code, it.message, errorBody))
            }
            val body = it.body?.string().orEmpty()
            return parseEmbeddingsResponse(body)
        }
    }

    private fun getGeminiEmbeddings(inputs: List<String>, modelInfo: ModelInfo): List<List<Float>> {
        val apiKey = repository.getApiKeyForProvider(provider)
        if (apiKey.isEmpty()) {
            throw IllegalStateException(repository.getContext().getString(R.string.error_api_key_missing))
        }
        val baseUrl = ApiUrlResolver.resolveBaseUrlForProvider(repository, provider)
        val endpoint = resolveGeminiEmbedUrl(baseUrl, modelInfo.apiCode, apiKey)
            ?: throw IllegalStateException(repository.getContext().getString(R.string.error_invalid_api_url))

        val payload = JsonObject().apply {
            val contents = JsonArray()
            inputs.forEach { input ->
                val content = JsonObject()
                val parts = JsonArray()
                parts.add(JsonObject().apply { addProperty("text", input) })
                content.add("parts", parts)
                contents.add(content)
            }
            add("contents", contents)
        }
        val requestBody = payload.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val req = Request.Builder()
            .url(endpoint)
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val resp = client.newCall(req).execute()
        resp.use {
            if (!it.isSuccessful) {
                val errorBody = it.body?.string()
                throw IllegalStateException(ModelErrorParser.parseHttpError(it.code, it.message, errorBody))
            }
            val body = it.body?.string().orEmpty()
            return parseGeminiEmbeddings(body)
        }
    }

    private fun resolveGeminiEmbedUrl(baseUrl: String, modelId: String, apiKey: String): HttpUrl? {
        val base = baseUrl.trim().ifBlank { "https://generativelanguage.googleapis.com" }.trimEnd('/')
        val id = modelId.removePrefix("models/")
        val url = "$base/v1beta/models/$id:embedContent?key=$apiKey"
        return url.toHttpUrlOrNull()
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

    private fun parseEmbeddingsResponse(json: String): List<List<Float>> {
        val root = JsonParser.parseString(json).asJsonObject
        val data = root.getAsJsonArray("data") ?: return emptyList()
        return data.mapNotNull { item ->
            if (!item.isJsonObject) return@mapNotNull null
            val vector = item.asJsonObject.getAsJsonArray("embedding") ?: return@mapNotNull null
            vector.mapNotNull { it.takeIf { e -> e.isJsonPrimitive }?.asFloat }
        }
    }
}
