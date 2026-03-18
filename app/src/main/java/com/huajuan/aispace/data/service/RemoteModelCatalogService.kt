package com.huajuan.aispace.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.concurrent.TimeUnit

class RemoteModelCatalogService(
    private val repository: Repository
) {
    suspend fun fetchModels(providerName: String): List<ModelInfo> {
        val providerType = repository.getProviderType(providerName)
        return when (providerType) {
            ProviderType.Gemini -> fetchGeminiModels(providerName)
            ProviderType.Anthropic -> emptyList()
            ProviderType.AzureOpenAI -> fetchOpenAiModels(providerName)
            ProviderType.OpenAI -> fetchOpenAiModels(providerName)
        }
    }

    private suspend fun fetchOpenAiModels(providerName: String): List<ModelInfo> {
        val apiKey = repository.getApiKeyForProvider(providerName)
        if (apiKey.isBlank()) return emptyList()
        val baseUrl = ApiUrlResolver.resolveBaseUrlForProvider(repository, providerName)
        val endpoint = resolveOpenAiModelsUrl(baseUrl) ?: return emptyList()
        val req = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .get()
            .build()

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val body = resp.body?.string().orEmpty()
                parseOpenAiModels(body)
            }
        }
    }

    private suspend fun fetchGeminiModels(providerName: String): List<ModelInfo> {
        val apiKey = repository.getApiKeyForProvider(providerName)
        if (apiKey.isBlank()) return emptyList()
        val baseUrl = ApiUrlResolver.resolveBaseUrlForProvider(repository, providerName)
        val endpoint = resolveGeminiModelsUrl(baseUrl, apiKey) ?: return emptyList()
        val req = Request.Builder()
            .url(endpoint)
            .get()
            .build()

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val body = resp.body?.string().orEmpty()
                parseGeminiModels(body)
            }
        }
    }

    private fun resolveOpenAiModelsUrl(baseUrl: String): String? {
        if (baseUrl.isBlank()) return null
        val normalized = baseUrl.trim().trimEnd('/')
        val root = when {
            normalized.endsWith("/chat/completions") -> normalized.removeSuffix("/chat/completions")
            normalized.endsWith("/completions") -> normalized.removeSuffix("/completions")
            normalized.endsWith("/images/generations") -> normalized.removeSuffix("/images/generations")
            normalized.endsWith("/v1") -> normalized
            normalized.contains("/v1/") -> normalized.substringBeforeLast("/")
            else -> normalized
        }
        val url = when {
            root.endsWith("/v1") -> "$root/models"
            root.contains("/api/v") -> "$root/models"
            else -> "$root/v1/models"
        }
        return url.toHttpUrlOrNull()?.toString()
    }

    private fun resolveGeminiModelsUrl(baseUrl: String, apiKey: String): String? {
        val base = baseUrl.trim().ifBlank { "https://generativelanguage.googleapis.com" }.trimEnd('/')
        val url = "$base/v1beta/models?key=$apiKey"
        return url.toHttpUrlOrNull()?.toString()
    }

    private fun parseOpenAiModels(json: String): List<ModelInfo> {
        return try {
            val root = JsonParser.parseString(json).asJsonObject
            val data = root.getAsJsonArray("data") ?: return emptyList()
            data.mapNotNull { item ->
                if (!item.isJsonObject) return@mapNotNull null
                val id = item.asJsonObject.get("id")?.asString ?: return@mapNotNull null
                ModelInfo(
                    displayName = id,
                    apiCode = id,
                    capabilities = emptySet()
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseGeminiModels(json: String): List<ModelInfo> {
        return try {
            val root = JsonParser.parseString(json).asJsonObject
            val models = root.getAsJsonArray("models") ?: return emptyList()
            models.mapNotNull { item ->
                if (!item.isJsonObject) return@mapNotNull null
                val name = item.asJsonObject.get("name")?.asString ?: return@mapNotNull null
                val display = item.asJsonObject.get("displayName")?.asString ?: name
                val id = name.removePrefix("models/")
                val methods = item.asJsonObject.getAsJsonArray("supportedGenerationMethods")
                    ?.mapNotNull { method ->
                        method?.asString?.let { mapGeminiMethodToCapability(it) }
                    }
                    ?.toSet()
                    ?: emptySet()
                val capabilities = methods.ifEmpty { setOf(ModelCapability.Chat) }
                ModelInfo(displayName = display, apiCode = id, capabilities = capabilities)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun mapGeminiMethodToCapability(method: String): ModelCapability? {
        return when {
            method.equals("generateContent", ignoreCase = true) -> ModelCapability.Chat
            method.equals("embedContent", ignoreCase = true) -> ModelCapability.Embedding
            method.contains("image", ignoreCase = true) -> ModelCapability.ImageGeneration
            else -> null
        }
    }
}
