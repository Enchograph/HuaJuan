package com.huajuan.aispace.data

enum class WebSearchMode(val id: String) {
    Disabled("disabled"),
    ModelNative("model_native"),
    ExternalProvider("external_provider");

    companion object {
        fun fromId(value: String?): WebSearchMode {
            return entries.firstOrNull { it.id == value } ?: Disabled
        }
    }
}

data class WebSearchPlan(
    val mode: WebSearchMode,
    val providerId: String? = null,
    val failureMessage: String = ""
) {
    val isEnabled: Boolean
        get() = mode != WebSearchMode.Disabled
}

data class WebSearchPlannerInputs(
    val runtimeConfig: RuntimeWebSearchConfig?,
    val supportsModelNative: Boolean
)

object WebSearchPlanner {
    fun plan(inputs: WebSearchPlannerInputs): WebSearchPlan {
        val runtime = inputs.runtimeConfig
        if (runtime?.enabled != true) {
            return WebSearchPlan(WebSearchMode.Disabled)
        }

        val selection = normalizeSelection(runtime.providerId)
        return when (selection) {
            WebSearchProviderIds.Builtin -> modelNativePlan(inputs.supportsModelNative)
            else -> externalPlan(selection)
        }
    }

    fun normalizeSelection(selection: String?): String {
        return WebSearchProviders.normalizeSelection(selection)
    }

    private fun modelNativePlan(supportsModelNative: Boolean): WebSearchPlan {
        return if (supportsModelNative) {
            WebSearchPlan(WebSearchMode.ModelNative)
        } else {
            WebSearchPlan(
                mode = WebSearchMode.Disabled,
                failureMessage = "Current model does not support native web search"
            )
        }
    }

    private fun externalPlan(providerId: String): WebSearchPlan {
        return if (WebSearchProviders.isSupportedExternalProvider(providerId)) {
            WebSearchPlan(WebSearchMode.ExternalProvider, providerId = providerId)
        } else {
            WebSearchPlan(
                mode = WebSearchMode.Disabled,
                failureMessage = "Unsupported external web search provider"
            )
        }
    }
}
