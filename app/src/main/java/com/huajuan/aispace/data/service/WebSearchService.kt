package com.huajuan.aispace.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.huajuan.aispace.R
import com.huajuan.aispace.i18n.LocalizedResources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

internal class WebSearchService(
    private val configSource: WebSearchConfigSource,
    private val client: OkHttpClient = defaultClient(),
    private val tavilyBaseUrl: String = DEFAULT_TAVILY_BASE_URL
) {
    suspend fun searchAndBuildContext(
        query: String,
        providerId: String,
        startIndex: Int = 1
    ): WebSearchContextPayload = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext WebSearchContextPayload("", emptyList())

        val count = configSource.getWebSearchResultCount().coerceIn(1, 20)
        val includeDate = configSource.getWebSearchIncludeDate()
        val compression = configSource.getWebSearchCompression().ifBlank { "none" }
        val blacklist = configSource.getWebSearchBlacklist().toList()
        val fetchCount = (count + blacklist.size.coerceAtMost(5)).coerceIn(1, 20)
        val datedQuery = if (includeDate) {
            val day = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            "$query today is $day"
        } else {
            query
        }

        val raw = searchByProvider(providerId, datedQuery, fetchCount)
        val filtered = raw.items.filterNot { isBlacklisted(it.url, blacklist) }
        val compressed = compress(filtered, query, compression, count)
        if (compressed.isEmpty()) {
            return@withContext WebSearchContextPayload(
                contextText = "",
                citations = emptyList(),
                failureMessage = raw.failureMessage
            )
        }

        val citations = compressed.mapIndexed { index, item ->
            WebCitation(
                index = startIndex + index,
                title = item.title.ifBlank { item.url },
                url = item.url,
                snippet = item.content.take(200),
                sourceType = "web"
            )
        }
        val context = buildString {
            append(LocalizedResources.getString(configSource.getContext(), R.string.web_search_context_intro))
            append('\n')
            compressed.forEachIndexed { index, item ->
                append("[").append(startIndex + index).append("] ")
                append(item.title.ifBlank { "Untitled" }).append('\n')
                append("URL: ").append(item.url).append('\n')
                append(item.content.take(1200)).append("\n\n")
            }
        }.trim()
        WebSearchContextPayload(
            contextText = context,
            citations = citations,
            failureMessage = raw.failureMessage
        )
    }

    private fun compress(
        results: List<WebSearchResultItem>,
        query: String,
        compression: String,
        maxCount: Int
    ): List<WebSearchResultItem> {
        if (results.isEmpty()) return emptyList()
        return when (compression) {
            "truncate" -> {
                val per = (6000 / results.size.coerceAtLeast(1)).coerceIn(300, 1600)
                results.take(maxCount).map { it.copy(content = it.content.take(per)) }
            }
            "rag" -> {
                val tokens = tokenize(query)
                results.asSequence()
                    .map { item ->
                        val hay = "${item.title} ${item.content}".lowercase(Locale.getDefault())
                        val score = tokens.sumOf { token -> if (hay.contains(token)) 1 else 0 }
                        item to score
                    }
                    .sortedByDescending { it.second }
                    .take(maxCount)
                    .map { (item, _) -> item.copy(content = item.content.take(1200)) }
                    .toList()
            }
            else -> results.take(maxCount)
        }
    }

    private fun tokenize(text: String): List<String> =
        text.lowercase(Locale.getDefault())
            .replace(Regex("[^\\p{L}\\p{Nd}]+"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 2 }

    private fun isBlacklisted(url: String, rules: List<String>): Boolean {
        if (url.isBlank() || rules.isEmpty()) return false
        val lower = url.lowercase(Locale.getDefault())
        val host = runCatching { java.net.URI(url).host.orEmpty().lowercase(Locale.getDefault()) }.getOrDefault("")
        return rules.any { raw ->
            val rule = raw.trim()
            when {
                rule.isBlank() -> false
                rule.startsWith("/") && rule.endsWith("/") && rule.length > 2 -> {
                    runCatching { Regex(rule.substring(1, rule.length - 1), RegexOption.IGNORE_CASE) }.getOrNull()
                        ?.containsMatchIn(url) == true
                }
                rule.contains("*") -> {
                    val re = rule.replace(".", "\\.").replace("*", ".*")
                    runCatching { Regex(re, RegexOption.IGNORE_CASE) }.getOrNull()?.containsMatchIn(lower) == true
                }
                rule.contains("://") -> lower.contains(rule.lowercase(Locale.getDefault()))
                else -> host.contains(rule.lowercase(Locale.getDefault()))
            }
        }
    }

    private fun searchByProvider(providerId: String, query: String, count: Int): ProviderSearchResult {
        return when (providerId) {
            WebSearchProviderIds.Tavily -> searchTavily(query, count)
            WebSearchProviderIds.Searxng -> searchSearxng(query, count)
            WebSearchProviderIds.Exa -> searchExa(query, count)
            WebSearchProviderIds.ExaMcp -> searchExaMcp(query, count)
            WebSearchProviderIds.Bocha -> searchBocha(query, count)
            WebSearchProviderIds.Zhipu -> searchZhipu(query, count)
            WebSearchProviderIds.Builtin -> ProviderSearchResult.failure("Built-in web search does not use the external search service")
            else -> ProviderSearchResult.failure("Unsupported external web search provider")
        }
    }

    private fun providerKey(providerId: String): String {
        val direct = configSource.getWebSearchProviderApiKey(providerId).trim()
        if (direct.isNotEmpty()) return direct
        return configSource.getWebSearchApiKey().trim()
    }

    private fun searchTavily(query: String, count: Int): ProviderSearchResult {
        val key = providerKey(WebSearchProviderIds.Tavily)
        if (key.isBlank()) return ProviderSearchResult.failure("Tavily API key is not configured")
        val body = JsonObject().apply {
            addProperty("query", query)
            addProperty("max_results", count)
            addProperty("search_depth", "basic")
        }
        val req = Request.Builder()
            .url("${tavilyBaseUrl.trimEnd('/')}/search")
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        return executeJson(req) { obj ->
            ProviderSearchResult(items = obj.getAsJsonArray("results")?.toItems(count) ?: emptyList())
        }
    }

    private fun searchExa(query: String, count: Int): ProviderSearchResult {
        val key = providerKey(WebSearchProviderIds.Exa)
        if (key.isBlank()) return ProviderSearchResult.failure("Exa API key is not configured")
        val body = JsonObject().apply {
            addProperty("query", query)
            addProperty("numResults", count)
            add("contents", JsonObject().apply { addProperty("text", true) })
        }
        val req = Request.Builder()
            .url("https://api.exa.ai/search")
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        return executeJson(req) { obj ->
            ProviderSearchResult(items = obj.getAsJsonArray("results")?.toItems(count, contentKey = "text") ?: emptyList())
        }
    }

    private fun searchBocha(query: String, count: Int): ProviderSearchResult {
        val key = providerKey(WebSearchProviderIds.Bocha)
        if (key.isBlank()) return ProviderSearchResult.failure("Bocha API key is not configured")
        val body = JsonObject().apply {
            addProperty("query", query)
            addProperty("count", count)
            addProperty("summary", true)
            addProperty("page", 1)
        }
        val req = Request.Builder()
            .url("https://api.bochaai.com/v1/web-search")
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        return executeJson(req) { obj ->
            val pages = obj.getAsJsonObject("data")?.getAsJsonObject("webPages")?.getAsJsonArray("value")
            ProviderSearchResult(
                items = pages?.mapNotNull { el ->
                    if (!el.isJsonObject) return@mapNotNull null
                    val item = el.asJsonObject
                    val url = item.get("url")?.asString.orEmpty()
                    if (url.isBlank()) return@mapNotNull null
                    WebSearchResultItem(
                        title = item.get("name")?.asString.orEmpty(),
                        content = item.get("summary")?.asString ?: item.get("snippet")?.asString.orEmpty(),
                        url = url
                    )
                }?.take(count) ?: emptyList()
            )
        }
    }

    private fun searchZhipu(query: String, count: Int): ProviderSearchResult {
        val key = providerKey(WebSearchProviderIds.Zhipu)
        if (key.isBlank()) return ProviderSearchResult.failure("Zhipu API key is not configured")
        val body = JsonObject().apply {
            addProperty("search_query", query)
            addProperty("search_engine", "search_std")
            addProperty("search_intent", false)
        }
        val req = Request.Builder()
            .url("https://open.bigmodel.cn/api/paas/v4/web_search")
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        return executeJson(req) { obj ->
            ProviderSearchResult(
                items = obj.getAsJsonArray("search_result")?.mapNotNull { el ->
                    if (!el.isJsonObject) return@mapNotNull null
                    val item = el.asJsonObject
                    val url = item.get("link")?.asString.orEmpty()
                    if (url.isBlank()) return@mapNotNull null
                    WebSearchResultItem(
                        title = item.get("title")?.asString.orEmpty(),
                        content = item.get("content")?.asString.orEmpty(),
                        url = url
                    )
                }?.take(count) ?: emptyList()
            )
        }
    }

    private fun searchSearxng(query: String, count: Int): ProviderSearchResult {
        val raw = providerKey(WebSearchProviderIds.Searxng)
        if (raw.isBlank()) return ProviderSearchResult.failure("Searxng endpoint is not configured")

        val cfg = parseSearxngConfig(raw)
        val encoded = URLEncoder.encode(query, "UTF-8")
        val req = Request.Builder()
            .url("${cfg.host.trimEnd('/')}/search?q=$encoded&format=json")
            .apply {
                if (!cfg.username.isNullOrBlank()) {
                    addHeader("Authorization", Credentials.basic(cfg.username, cfg.password.orEmpty()))
                }
            }
            .build()
        return executeJson(req) { obj ->
            ProviderSearchResult(items = obj.getAsJsonArray("results")?.toItems(count, contentKey = "content") ?: emptyList())
        }
    }

    private data class SearxngConfig(
        val host: String,
        val username: String? = null,
        val password: String? = null
    )

    private fun parseSearxngConfig(raw: String): SearxngConfig {
        val value = raw.trim()
        if (value.isBlank()) return SearxngConfig("https://searx.be")

        if (value.startsWith("http://") || value.startsWith("https://")) {
            return SearxngConfig(host = value)
        }

        if (value.contains("|")) {
            val parts = value.split("|", limit = 3).map { it.trim() }
            val host = parts.getOrNull(0).orEmpty().ifBlank { "https://searx.be" }
            val normalizedHost = if (host.startsWith("http://") || host.startsWith("https://")) host else "https://$host"
            return SearxngConfig(
                host = normalizedHost,
                username = parts.getOrNull(1)?.takeIf { it.isNotBlank() },
                password = parts.getOrNull(2)?.takeIf { it.isNotBlank() }
            )
        }

        if (value.contains(":")) {
            val pair = value.split(":", limit = 2)
            return SearxngConfig(
                host = "https://searx.be",
                username = pair.getOrNull(0)?.takeIf { it.isNotBlank() },
                password = pair.getOrNull(1)?.takeIf { it.isNotBlank() }
            )
        }

        val normalizedHost = if (value.startsWith("http://") || value.startsWith("https://")) value else "https://$value"
        return SearxngConfig(host = normalizedHost)
    }

    private fun searchExaMcp(query: String, count: Int): ProviderSearchResult {
        val body = JsonObject().apply {
            addProperty("jsonrpc", "2.0")
            addProperty("id", 1)
            addProperty("method", "tools/call")
            add("params", JsonObject().apply {
                addProperty("name", "web_search_exa")
                add("arguments", JsonObject().apply {
                    addProperty("query", query)
                    addProperty("numResults", count)
                    addProperty("type", "auto")
                    addProperty("livecrawl", "fallback")
                })
            })
        }
        val req = Request.Builder()
            .url("https://mcp.exa.ai/mcp")
            .addHeader("Accept", "application/json, text/event-stream")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        val response = executeText(req)
        if (response.failureMessage.isNotBlank()) return ProviderSearchResult.failure(response.failureMessage)
        return ProviderSearchResult(items = parseExaMcpText(response.body).take(count))
    }

    private fun executeText(request: Request): HttpTextResult {
        return runCatching {
            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val detail = body.take(160).ifBlank { "HTTP ${resp.code}" }
                    HttpTextResult.failure("Web search request failed (${resp.code}): $detail")
                } else {
                    HttpTextResult(body = body)
                }
            }
        }.getOrElse { error ->
            HttpTextResult.failure(error.message ?: "Web search request failed")
        }
    }

    private fun executeJson(request: Request, parser: (JsonObject) -> ProviderSearchResult): ProviderSearchResult {
        val response = executeText(request)
        if (response.failureMessage.isNotBlank()) return ProviderSearchResult.failure(response.failureMessage)
        return runCatching {
            val element = JsonParser.parseString(response.body)
            if (!element.isJsonObject) {
                ProviderSearchResult.failure("Web search returned an invalid JSON payload")
            } else {
                parser(element.asJsonObject)
            }
        }.getOrElse { error ->
            ProviderSearchResult.failure(error.message ?: "Failed to parse web search response")
        }
    }

    private fun JsonArray.toItems(limit: Int, contentKey: String = "content"): List<WebSearchResultItem> {
        return mapNotNull { el ->
            if (!el.isJsonObject) return@mapNotNull null
            val item = el.asJsonObject
            val url = item.get("url")?.asString.orEmpty()
            if (url.isBlank()) return@mapNotNull null
            WebSearchResultItem(
                title = item.get("title")?.asString.orEmpty(),
                content = item.get(contentKey)?.asString.orEmpty(),
                url = url
            )
        }.take(limit)
    }

    private fun parseExaMcpText(text: String): List<WebSearchResultItem> {
        val blocks = mutableListOf<WebSearchResultItem>()
        val lineRegex = Regex("Title:\\s*(.*?)\\n.*?URL:\\s*(.*?)\\nText:\\s*(.*?)(?=\\n\\nTitle:|$)", setOf(RegexOption.DOT_MATCHES_ALL))
        lineRegex.findAll(text).forEach { match ->
            val title = match.groupValues.getOrElse(1) { "" }.trim()
            val url = match.groupValues.getOrElse(2) { "" }.trim()
            val content = match.groupValues.getOrElse(3) { "" }.trim()
            if (url.isNotBlank()) {
                blocks.add(WebSearchResultItem(title = title, content = content, url = url))
            }
        }
        return blocks
    }

    internal data class ProviderSearchResult(
        val items: List<WebSearchResultItem> = emptyList(),
        val failureMessage: String = ""
    ) {
        companion object {
            fun failure(message: String): ProviderSearchResult = ProviderSearchResult(failureMessage = message)
        }
    }

    internal data class HttpTextResult(
        val body: String = "",
        val failureMessage: String = ""
    ) {
        companion object {
            fun failure(message: String): HttpTextResult = HttpTextResult(failureMessage = message)
        }
    }

    private companion object {
        const val DEFAULT_TAVILY_BASE_URL = "https://api.tavily.com"

        fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .build()
        }
    }
}
