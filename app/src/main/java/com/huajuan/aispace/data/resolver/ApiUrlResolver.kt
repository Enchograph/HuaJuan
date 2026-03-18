package com.huajuan.aispace.data

object ApiUrlResolver {
    fun resolveBaseUrl(repository: Repository, assistantId: String = repository.getCurrentAssistantId()): String {
        val serviceProvider = repository.getAssistantModelConfig(assistantId).serviceProvider
        val modelDataProvider = ModelDataProvider(repository)
        val predefinedUrl = modelDataProvider.getApiUrlForProvider(serviceProvider)
        if (predefinedUrl.isNotEmpty()) {
            return normalizeBaseUrl(predefinedUrl)
        }

        val customUrl = when (serviceProvider) {
            BuiltInServiceProviders.LegacyCustom -> repository.getCustomApiUrl().ifEmpty { "https://api.openai.com" }
            else -> repository.getCustomProviderApiUrl(serviceProvider).ifEmpty { "https://api.openai.com" }
        }

        return normalizeBaseUrl(customUrl)
    }

    fun resolveBaseUrlForProvider(repository: Repository, provider: String): String {
        val modelDataProvider = ModelDataProvider(repository)
        val predefinedUrl = modelDataProvider.getApiUrlForProvider(provider)
        if (predefinedUrl.isNotEmpty()) {
            return normalizeBaseUrl(predefinedUrl)
        }
        val customUrl = when (provider) {
            BuiltInServiceProviders.LegacyCustom -> repository.getCustomApiUrl().ifEmpty { "https://api.openai.com" }
            else -> repository.getCustomProviderApiUrl(provider).ifEmpty { "https://api.openai.com" }
        }
        return normalizeBaseUrl(customUrl)
    }

    fun resolveChatUrl(repository: Repository, assistantId: String = repository.getCurrentAssistantId()): String? {
        val provider = repository.getAssistantModelConfig(assistantId).serviceProvider
        return resolveChatUrlForProvider(repository, provider)
    }

    fun resolveChatUrlForProvider(repository: Repository, provider: String): String? {
        val providerType = repository.getProviderType(provider)
        val base = resolveBaseUrlForProvider(repository, provider)
        return when (providerType) {
            ProviderType.OpenAI, ProviderType.AzureOpenAI -> buildOpenAiUrl(base, "chat/completions")
            ProviderType.Anthropic -> buildAnthropicUrl(base)
            ProviderType.Gemini -> null
        }
    }

    fun resolveEmbeddingsUrlForProvider(repository: Repository, provider: String): String? {
        val providerType = repository.getProviderType(provider)
        val base = resolveBaseUrlForProvider(repository, provider)
        return when (providerType) {
            ProviderType.OpenAI, ProviderType.AzureOpenAI -> buildOpenAiUrl(base, "embeddings")
            ProviderType.Gemini -> null
            ProviderType.Anthropic -> null
        }
    }

    fun resolveImageGenerationUrlForProvider(repository: Repository, provider: String): String? {
        val providerType = repository.getProviderType(provider)
        val base = resolveBaseUrlForProvider(repository, provider)
        return when (providerType) {
            ProviderType.OpenAI, ProviderType.AzureOpenAI -> buildOpenAiUrl(base, "images/generations")
            ProviderType.Anthropic, ProviderType.Gemini -> null
        }
    }

    private fun normalizeBaseUrl(url: String): String {
        if (url.isBlank()) return "https://api.openai.com"

        var normalizedUrl = url.trim()
        normalizedUrl = normalizedUrl.trimEnd('/')
        if (!normalizedUrl.startsWith("https://") && !normalizedUrl.startsWith("http://")) {
            normalizedUrl = "https://$normalizedUrl"
        }
        val stripped = stripKnownSuffixes(normalizedUrl)
        return stripped.trimEnd('/')
    }

    private fun stripKnownSuffixes(url: String): String {
        val knownSuffixes = listOf(
            "/v1/chat/completions",
            "/v1/completions",
            "/v1/embeddings",
            "/v1/images/generations",
            "/v1/messages",
            "/chat/completions",
            "/completions",
            "/embeddings",
            "/images/generations",
            "/v1"
        )
        var current = url
        for (suffix in knownSuffixes) {
            if (current.endsWith(suffix)) {
                current = current.removeSuffix(suffix)
                break
            }
        }
        if (current.contains("/v1beta/")) {
            current = current.substringBefore("/v1beta/")
        }
        return current
    }

    private fun buildOpenAiUrl(base: String, path: String): String? {
        if (base.isBlank()) return null
        val normalized = base.trimEnd('/')
        val url = when {
            normalized.endsWith("/v1") -> "$normalized/$path"
            normalized.contains("/api/v") -> "$normalized/$path"
            else -> "$normalized/v1/$path"
        }
        return url
    }

    private fun buildAnthropicUrl(base: String): String? {
        if (base.isBlank()) return null
        val normalized = base.trimEnd('/')
        return if (normalized.endsWith("/v1/messages")) normalized else "$normalized/v1/messages"
    }
}
