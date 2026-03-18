package com.huajuan.aispace.data

import android.content.Context
import androidx.annotation.StringRes
import com.huajuan.aispace.R
import com.huajuan.aispace.i18n.LocalizedResources

object BuiltInServiceProviders {
    const val LegacyCustom = "\u81ea\u5b9a\u4e49"
    const val SiliconFlow = "siliconflow"
    const val Volcengine = "volcengine"
    const val SoruxGpt = "soruxgpt"
    const val OpenAi = "openai"
    const val Anthropic = "anthropic"

    val providers: Map<String, ServiceProviderInfo> = mapOf(
        SiliconFlow to ServiceProviderInfo(
            id = SiliconFlow,
            displayNameRes = R.string.provider_siliconflow,
            baseUrl = "https://api.siliconflow.cn",
            consoleUrl = "https://cloud.siliconflow.cn",
            providerType = ProviderType.OpenAI,
            models = listOf(
                ModelInfo("DeepSeek-V3.2", "deepseek-ai/DeepSeek-V3.2", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("DeepSeek-V3.1-Terminus", "deepseek-ai/DeepSeek-V3.1-Terminus", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("DeepSeek-V3.2-Exp", "deepseek-ai/DeepSeek-V3.2-Exp", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("DeepSeek-R1", "deepseek-ai/DeepSeek-R1", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("DeepSeek-V3", "deepseek-ai/DeepSeek-V3", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("MiniMax-M2.5", "Pro/MiniMaxAI/MiniMax-M2.5", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("GLM-5", "Pro/zai-org/GLM-5", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("GLM-4.7", "Pro/zai-org/GLM-4.7", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("Kimi-K2.5", "Pro/moonshotai/Kimi-K2.5", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("BGE-M3", "BAAI/bge-m3", capabilities = setOf(ModelCapability.Embedding)),
                ModelInfo("Qwen-Image", "Qwen/Qwen-Image", capabilities = setOf(ModelCapability.ImageGeneration)),
                ModelInfo("Qwen-Image-Edit-2509", "Qwen/Qwen-Image-Edit-2509", capabilities = setOf(ModelCapability.ImageGeneration))
            )
        ),
        Volcengine to ServiceProviderInfo(
            id = Volcengine,
            displayNameRes = R.string.provider_volcengine,
            baseUrl = "https://ark.cn-beijing.volces.com/api/v3",
            consoleUrl = "https://ark.cn-beijing.volces.com",
            providerType = ProviderType.OpenAI,
            models = listOf(
                ModelInfo("DeepSeek-V3.1", "DeepSeek-V3.1", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("Kimi-K2", "kimi-k2-thinking-251104", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("DeepSeek-R1", "DeepSeek-R1", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("DeepSeek-V3", "DeepSeek-V3", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("Doubao-1.5-pro", "Doubao-1.5-pro-32k", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("Doubao-1.5-lite", "Doubao-1.5-lite-32k", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("Seedream 4.5", "ep-20251224183505-qkhjf", capabilities = setOf(ModelCapability.ImageGeneration)),
                ModelInfo("Seedream 4.0", "doubao-seedream-4-0-250828", capabilities = setOf(ModelCapability.ImageGeneration)),
                ModelInfo("Seedream 3.0", "Doubao-Seedream-3.0-t2i", capabilities = setOf(ModelCapability.ImageGeneration))
            )
        ),
        SoruxGpt to ServiceProviderInfo(
            id = SoruxGpt,
            displayNameRes = R.string.provider_soruxgpt,
            baseUrl = "https://ai.soruxgpt.com",
            consoleUrl = "https://www.soruxgpt.com",
            providerType = ProviderType.OpenAI,
            models = listOf(
                ModelInfo("GPT-3.5-Turbo", "gpt-3.5-turbo", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("ChatGPT-4o", "chatgpt-4o-latest", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("Claude-Sonnet-4", "claude-sonnet-4-all", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("Claude-Sonnet-4-5", "claude-sonnet-4-5-all", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("DeepSeek-V3", "deepseek-v3", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("DeepSeek-R1", "deepseek-r1", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("Gemini-2.5-Flash", "gemini-2.5-flash-nothinking", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("Gemini-2.5-Pro", "gemini-2.5-pro-nothinking", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("Gemini-3", "gemini-3-pro-all", capabilities = setOf(ModelCapability.Chat))
            )
        ),
        OpenAi to ServiceProviderInfo(
            id = OpenAi,
            displayNameRes = R.string.provider_openai,
            baseUrl = "https://api.openai.com",
            consoleUrl = "https://platform.openai.com",
            providerType = ProviderType.OpenAI,
            models = listOf(
                ModelInfo("GPT-4o", "GPT-4o", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("GPT-4o-mini", "GPT-4o-mini", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("GPT-4.5-Preview", "gpt-4.5-preview", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("O1-mini", "o1-mini", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("O1-preview", "o1-preview", capabilities = setOf(ModelCapability.Chat))
            )
        ),
        Anthropic to ServiceProviderInfo(
            id = Anthropic,
            displayNameRes = R.string.provider_anthropic,
            baseUrl = "https://api.anthropic.com",
            consoleUrl = "https://platform.claude.com",
            providerType = ProviderType.Anthropic,
            models = listOf(
                ModelInfo("Claude 3 Opus", "Claude 3 Opus", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("Claude 3.5 Sonnet", "Claude 3.5 Sonnet", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("Claude 3.5 Haiku", "Claude 3.5 Haiku", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("Claude 3.7 Sonnet", "Claude 3.7 Sonnet", capabilities = setOf(ModelCapability.Chat)),
                ModelInfo("Claude Sonnet 4", "Claude Sonnet 4", capabilities = setOf(ModelCapability.Chat))
            )
        )
    )

    private val legacyNames: Map<String, List<String>> = mapOf(
        SiliconFlow to listOf("\u7845\u57fa\u6d41\u52a8"),
        Volcengine to listOf("\u706b\u5c71\u5f15\u64ce"),
        SoruxGpt to listOf("SoruxGPT"),
        OpenAi to listOf("OpenAI"),
        Anthropic to listOf("Anthropic")
    )

    fun normalizeId(raw: String?): String {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return SiliconFlow
        if (providers.containsKey(value)) return value
        return legacyNames.entries.firstOrNull { (_, aliases) -> value in aliases }?.key ?: value
    }

    fun getLegacyNames(providerId: String): List<String> = legacyNames[normalizeId(providerId)].orEmpty()

    fun isBuiltIn(providerId: String): Boolean = providers.containsKey(normalizeId(providerId))

    fun isLegacyCustom(providerId: String): Boolean = providerId.trim() == LegacyCustom

    fun displayName(context: Context, providerId: String, localeOverride: String? = null): String {
        val normalized = normalizeId(providerId)
        val info = providers[normalized] ?: return providerId
        return LocalizedResources.getString(context, info.displayNameRes, localeOverride)
    }

    @StringRes
    fun displayNameRes(providerId: String): Int? = providers[normalizeId(providerId)]?.displayNameRes
}
