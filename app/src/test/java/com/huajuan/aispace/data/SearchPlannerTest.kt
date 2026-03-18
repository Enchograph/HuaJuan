package com.huajuan.aispace.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchPlannerTest {
    @Test
    fun `disabled runtime search returns disabled plan`() {
        val plan = WebSearchPlanner.plan(
            WebSearchPlannerInputs(
                runtimeConfig = null,
                supportsModelNative = true
            )
        )

        assertEquals(WebSearchMode.Disabled, plan.mode)
    }

    @Test
    fun `builtin selection resolves to model native when supported`() {
        val plan = WebSearchPlanner.plan(
            WebSearchPlannerInputs(
                runtimeConfig = RuntimeWebSearchConfig(
                    enabled = true,
                    providerId = WebSearchProviderIds.Builtin
                ),
                supportsModelNative = true
            )
        )

        assertEquals(WebSearchMode.ModelNative, plan.mode)
    }

    @Test
    fun `builtin selection disables when native support is missing`() {
        val plan = WebSearchPlanner.plan(
            WebSearchPlannerInputs(
                runtimeConfig = RuntimeWebSearchConfig(
                    enabled = true,
                    providerId = WebSearchProviderIds.Builtin
                ),
                supportsModelNative = false
            )
        )

        assertEquals(WebSearchMode.Disabled, plan.mode)
        assertEquals("Current model does not support native web search", plan.failureMessage)
    }

    @Test
    fun `external provider selection resolves to external mode`() {
        val plan = WebSearchPlanner.plan(
            WebSearchPlannerInputs(
                runtimeConfig = RuntimeWebSearchConfig(
                    enabled = true,
                    providerId = WebSearchProviderIds.Tavily
                ),
                supportsModelNative = true
            )
        )

        assertEquals(WebSearchMode.ExternalProvider, plan.mode)
        assertEquals(WebSearchProviderIds.Tavily, plan.providerId)
    }

    @Test
    fun `legacy html providers normalize to tavily`() {
        val plan = WebSearchPlanner.plan(
            WebSearchPlannerInputs(
                runtimeConfig = RuntimeWebSearchConfig(
                    enabled = true,
                    providerId = "Bing"
                ),
                supportsModelNative = true
            )
        )

        assertEquals(WebSearchMode.ExternalProvider, plan.mode)
        assertEquals(WebSearchProviderIds.Tavily, plan.providerId)
    }
}
