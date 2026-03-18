package com.huajuan.aispace.data

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebSearchServiceTest {
    @Test
    fun `tavily search sends official request and parses results`() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                          "results": [
                            {
                              "title": "Result One",
                              "url": "https://example.com/one",
                              "content": "Snippet one"
                            },
                            {
                              "title": "Result Two",
                              "url": "https://example.com/two",
                              "content": "Snippet two"
                            }
                          ]
                        }
                        """.trimIndent()
                    )
            )

            val service = WebSearchService(
                configSource = FakeWebSearchConfigSource(
                    providerKeys = mapOf(WebSearchProviderIds.Tavily to "test-key")
                ),
                client = OkHttpClient(),
                tavilyBaseUrl = server.url("/").toString()
            )

            val result = service.invokeSearchByProvider(WebSearchProviderIds.Tavily, "latest ai", 2)
            val request = server.takeRequest()
            val requestBody = request.body.readUtf8()

            assertEquals("/search", request.path)
            assertEquals("Bearer test-key", request.getHeader("Authorization"))
            assertTrue(requestBody.contains("\"query\":\"latest ai\""))
            assertTrue(requestBody.contains("\"max_results\":2"))
            assertEquals("", result.failureMessage)
            assertEquals(2, result.items.size)
            assertEquals("https://example.com/one", result.items.first().url)
        }
    }

    @Test
    fun `tavily search reports missing api key`() {
        val service = WebSearchService(
            configSource = FakeWebSearchConfigSource(),
            client = OkHttpClient(),
            tavilyBaseUrl = "https://example.com"
        )

        val payload = runBlockingSearchContext(service)

        assertEquals("", payload.contextText)
        assertTrue(payload.failureMessage.contains("Tavily API key is not configured"))
    }

    @Test
    fun `tavily search surfaces non success responses`() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setBody("""{"error":"invalid key"}""")
            )

            val service = WebSearchService(
                configSource = FakeWebSearchConfigSource(
                    providerKeys = mapOf(WebSearchProviderIds.Tavily to "bad-key")
                ),
                client = OkHttpClient(),
                tavilyBaseUrl = server.url("/").toString()
            )

            val payload = runBlockingSearchContext(service)

            assertEquals("", payload.contextText)
            assertTrue(payload.failureMessage.contains("401"))
        }
    }

    @Test
    fun `tavily search surfaces invalid json responses`() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("not-json")
            )

            val service = WebSearchService(
                configSource = FakeWebSearchConfigSource(
                    providerKeys = mapOf(WebSearchProviderIds.Tavily to "test-key")
                ),
                client = OkHttpClient(),
                tavilyBaseUrl = server.url("/").toString()
            )

            val payload = runBlockingSearchContext(service)

            assertEquals("", payload.contextText)
            assertTrue(payload.failureMessage.isNotBlank())
        }
    }

    private fun runBlockingSearchContext(service: WebSearchService): WebSearchContextPayload {
        return kotlinx.coroutines.runBlocking {
            service.searchAndBuildContext(
                query = "latest ai",
                providerId = WebSearchProviderIds.Tavily
            )
        }
    }

    private fun WebSearchService.invokeSearchByProvider(
        providerId: String,
        query: String,
        count: Int
    ): WebSearchService.ProviderSearchResult {
        val method = WebSearchService::class.java.getDeclaredMethod(
            "searchByProvider",
            String::class.java,
            String::class.java,
            Int::class.javaPrimitiveType
        )
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(this, providerId, query, count) as WebSearchService.ProviderSearchResult
    }

    private class FakeWebSearchConfigSource(
        private val providerKeys: Map<String, String> = emptyMap()
    ) : WebSearchConfigSource {
        override fun getWebSearchResultCount(): Int = 5
        override fun getWebSearchIncludeDate(): Boolean = false
        override fun getWebSearchCompression(): String = "none"
        override fun getWebSearchBlacklist(): Set<String> = emptySet()
        override fun getWebSearchProviderApiKey(providerId: String): String = providerKeys[providerId].orEmpty()
        override fun getWebSearchApiKey(): String = ""
        override fun getContext(): Context {
            throw UnsupportedOperationException("Context is not needed for these tests")
        }
    }
}
