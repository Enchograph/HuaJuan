package com.huajuan.aispace.data

data class ImageGenerationRequest(
    val prompt: String,
    val negativePrompt: String = "",
    val width: Int = 1024,
    val height: Int = 1024,
    val count: Int = 1,
    val steps: Int = 20,
    val guidanceScale: Float = 7.5f,
    val seed: Long? = null
)

enum class ImageGenerationCapabilityStatus {
    Supported,
    Unsupported,
    Unknown
}

data class ImageGenerationSupport(
    val status: ImageGenerationCapabilityStatus,
    val supportsNegativePrompt: Boolean = false,
    val supportsSteps: Boolean = false,
    val supportsGuidanceScale: Boolean = false,
    val supportsSeed: Boolean = false,
    val maxCount: Int = 1,
    val canProbe: Boolean = false,
    val message: String = ""
) {
    val canGenerate: Boolean
        get() = status == ImageGenerationCapabilityStatus.Supported
}

data class ImageGenerationResult(
    val imageIds: List<String>,
    val displayText: String,
    val appliedRequest: ImageGenerationRequest
)

data class ImageGenerationResponsePayload(
    val remoteUrls: List<String> = emptyList(),
    val base64Images: List<String> = emptyList(),
    val displayText: String = ""
)

data class ImageGenerationConversationState(
    val request: ImageGenerationRequest,
    val providerId: String,
    val modelDisplayName: String,
    val modelApiCode: String
)

data class ImageGenerationProbeRecord(
    val status: ImageGenerationCapabilityStatus,
    val checkedAt: Long = System.currentTimeMillis(),
    val message: String = ""
)
