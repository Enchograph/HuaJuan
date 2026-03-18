package com.huajuan.aispace.data

import android.content.Context
import androidx.annotation.StringRes
import com.huajuan.aispace.R
import com.huajuan.aispace.i18n.LocalizedResources

data class WebSearchProvider(
    val id: String,
    @StringRes val labelRes: Int?,
    val requiresApiKey: Boolean,
    val isLocal: Boolean
) {
    val label: String get() = id

    fun label(context: Context): String = labelRes?.let { LocalizedResources.getString(context, it) } ?: id
}

object WebSearchProviderIds {
    const val Tavily = "Tavily"
    const val Searxng = "Searxng"
    const val Exa = "Exa"
    const val ExaMcp = "ExaMCP"
    const val Bocha = "Bocha"
    const val Zhipu = "Zhipu"
    const val Builtin = "Builtin"
}

object WebSearchProviders {
    private val legacyProviders = setOf("Google", "Bing", "Baidu", "UnsupportedLegacy")
    const val defaultExternalProviderId: String = WebSearchProviderIds.Tavily
    const val defaultSelectionId: String = defaultExternalProviderId

    val apiProviders: List<WebSearchProvider> = listOf(
        WebSearchProvider(id = WebSearchProviderIds.Tavily, labelRes = null, requiresApiKey = true, isLocal = false),
        WebSearchProvider(id = WebSearchProviderIds.Searxng, labelRes = null, requiresApiKey = true, isLocal = false),
        WebSearchProvider(id = WebSearchProviderIds.Exa, labelRes = null, requiresApiKey = true, isLocal = false),
        WebSearchProvider(id = WebSearchProviderIds.ExaMcp, labelRes = null, requiresApiKey = false, isLocal = false),
        WebSearchProvider(id = WebSearchProviderIds.Bocha, labelRes = null, requiresApiKey = true, isLocal = false),
        WebSearchProvider(id = WebSearchProviderIds.Zhipu, labelRes = null, requiresApiKey = true, isLocal = false)
    )

    private val modelNativeProvider = WebSearchProvider(
        id = WebSearchProviderIds.Builtin,
        labelRes = R.string.web_search_provider_builtin,
        requiresApiKey = false,
        isLocal = true
    )

    val localProviders: List<WebSearchProvider> = listOf(
        modelNativeProvider
    )

    val all: List<WebSearchProvider> = localProviders + apiProviders

    fun findById(id: String): WebSearchProvider? = all.firstOrNull { it.id == id }

    fun isSupportedExternalProvider(id: String): Boolean =
        apiProviders.any { it.id == id }

    fun isSupportedSelection(id: String): Boolean =
        id == WebSearchProviderIds.Builtin || isSupportedExternalProvider(id)

    fun normalizeSelection(id: String?): String {
        val value = id?.trim().orEmpty()
        return when {
            value.isBlank() -> defaultSelectionId
            isSupportedSelection(value) -> value
            legacyProviders.contains(value) -> defaultSelectionId
            else -> defaultSelectionId
        }
    }
}
