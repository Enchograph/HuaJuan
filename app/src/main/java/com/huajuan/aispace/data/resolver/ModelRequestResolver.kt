package com.huajuan.aispace.data

data class ResolvedModelRequest(
    val serviceProvider: String,
    val selectedModelDisplayName: String,
    val modelInfo: ModelInfo,
    val isImageGenerationService: Boolean,
    val providerType: ProviderType
)

object ModelRequestResolver {
    fun resolve(repository: Repository, assistantId: String = repository.getCurrentAssistantId()): ResolvedModelRequest {
        val assistantConfig = repository.getAssistantModelConfig(assistantId)
        val useCloud = assistantConfig.useCloudModel
        val serviceProvider = assistantConfig.serviceProvider
        val selectedModelDisplayName = if (useCloud) {
            assistantConfig.selectedModelName
        } else {
            assistantConfig.localSelectedModelName
        }

        val modelInfo = if (useCloud) {
            val modelDataProvider = ModelDataProvider(repository)
            val modelList = modelDataProvider.getModelListForProvider(serviceProvider)
            modelList.find { it.displayName == selectedModelDisplayName }
                ?: ModelInfo(selectedModelDisplayName, selectedModelDisplayName)
        } else {
            ModelInfo(
                displayName = selectedModelDisplayName,
                apiCode = selectedModelDisplayName,
                modelPath = repository.getLocalModelPath(selectedModelDisplayName)
            )
        }

        val providerType = repository.getProviderType(serviceProvider)
        val isImageGenerationService = ModelCapabilityResolver.matchesUsage(
            modelInfo = modelInfo,
            providerName = serviceProvider,
            apiUrl = ModelDataProvider(repository).getApiUrlForProvider(serviceProvider),
            usage = ModelUsage.ImageGeneration
        )

        return ResolvedModelRequest(
            serviceProvider = serviceProvider,
            selectedModelDisplayName = selectedModelDisplayName,
            modelInfo = modelInfo,
            isImageGenerationService = isImageGenerationService,
            providerType = providerType
        )
    }
}
