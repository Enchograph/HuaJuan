package com.huajuan.aispace.data

object ModelCapabilityResolver {
    fun matchesUsage(
        modelInfo: ModelInfo,
        providerName: String,
        apiUrl: String? = null,
        usage: ModelUsage
    ): Boolean {
        if (usage == ModelUsage.All) return true
        val capability = when (usage) {
            ModelUsage.ImageGeneration -> ModelCapability.ImageGeneration
            ModelUsage.Embedding -> ModelCapability.Embedding
            ModelUsage.All -> null
        } ?: return true
        val capabilities = modelInfo.capabilities
        if (usage == ModelUsage.ImageGeneration && capabilities.isEmpty()) {
            // Only cloud models without explicit metadata are probeable for image generation.
            return !apiUrl.isNullOrBlank()
        }
        val resolvedCapabilities = capabilities.ifEmpty { setOf(ModelCapability.Chat) }
        return resolvedCapabilities.contains(capability)
    }
}
