package com.huajuan.aispace.data

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.huajuan.aispace.R
import com.huajuan.aispace.data.model.ChatEvent
import com.huajuan.aispace.network.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale
import java.util.concurrent.TimeUnit

class ImageGenerationApiService(
    private val repository: Repository,
    private val assistantId: String = repository.getCurrentAssistantId()
) : ModelApiService {

    override fun isAvailable(): Boolean = true

    override suspend fun getAIResponse(
        messages: List<Message>,
        modelInfo: ModelInfo,
        debugScopeId: String?
    ): String {
        val support = getSupport(modelInfo)
        if (!support.canGenerate) {
            return ModelErrorParser.normalizeUserError(
                support.message.ifBlank {
                    repository.getContext().getString(R.string.error_image_generation_not_supported)
                }
            )
        }
        val request = extractRequestFromMessages(messages)
        val payload = generateImagePayload(request, modelInfo)
        return when {
            payload.remoteUrls.isNotEmpty() -> payload.remoteUrls.joinToString("\n")
            payload.base64Images.isNotEmpty() -> ""
            payload.displayText.isNotBlank() -> payload.displayText
            else -> ModelErrorParser.normalizeUserError(repository.getContext().getString(R.string.error_no_content))
        }
    }

    override fun streamAIResponse(
        messages: List<Message>,
        modelInfo: ModelInfo,
        debugScopeId: String?
    ): Flow<ChatEvent> = channelFlow {
        val result = getAIResponse(messages, modelInfo, debugScopeId)
        if (ModelErrorParser.isErrorMessage(result)) {
            trySend(ChatEvent.Error(result))
        } else {
            trySend(ChatEvent.Chunk(result))
        }
        trySend(ChatEvent.Done)
        close()
    }

    override suspend fun getEmbeddings(inputs: List<String>, modelInfo: ModelInfo): List<List<Float>> {
        throw UnsupportedOperationException("Image generation service does not support embeddings")
    }

    suspend fun generateImagePayload(
        request: ImageGenerationRequest,
        modelInfo: ModelInfo
    ): ImageGenerationResponsePayload {
        val config = repository.getAssistantModelConfig(assistantId)
        val apiKey = repository.getApiKeyForProvider(config.serviceProvider)
        if (apiKey.isEmpty()) {
            return ImageGenerationResponsePayload(
                displayText = ModelErrorParser.normalizeUserError(
                    repository.getContext().getString(R.string.error_api_key_missing)
                )
            )
        }
        val support = getSupport(modelInfo)
        if (!support.canGenerate) {
            return ImageGenerationResponsePayload(
                displayText = ModelErrorParser.normalizeUserError(
                    support.message.ifBlank {
                        repository.getContext().getString(R.string.error_image_generation_not_supported)
                    }
                )
            )
        }
        val normalizedRequest = normalizeRequest(request, support)
        val endpoint = ApiUrlResolver.resolveImageGenerationUrlForProvider(repository, config.serviceProvider)
            ?: return ImageGenerationResponsePayload(
                displayText = ModelErrorParser.normalizeUserError(
                    repository.getContext().getString(R.string.error_image_generation_not_supported)
                )
            )

        return withContext(Dispatchers.IO) {
            runCatching {
                val client = createOkHttpClient()
                val payload = buildRequestJson(config.serviceProvider, modelInfo, normalizedRequest, support)
                val req = Request.Builder()
                    .url(endpoint)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(Gson().toJson(payload).toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        val errorBody = resp.body?.string()
                        return@use ImageGenerationResponsePayload(
                            displayText = ModelErrorParser.parseHttpError(resp.code, resp.message, errorBody)
                        )
                    }
                    val body = resp.body?.string().orEmpty()
                    parseImageGenerationPayload(body)
                }
            }.getOrElse { throwable ->
                ImageGenerationResponsePayload(
                    displayText = ModelErrorParser.parseThrowable(throwable)
                )
            }
        }
    }

    suspend fun probeImageGenerationSupport(modelInfo: ModelInfo): ImageGenerationSupport {
        val current = getSupport(modelInfo)
        if (current.status != ImageGenerationCapabilityStatus.Unknown) {
            return current
        }
        val request = ImageGenerationRequest(prompt = "capability probe", width = 1024, height = 1024, count = 1)
        val payload = generateImagePayload(request, modelInfo)
        val record = when {
            payload.remoteUrls.isNotEmpty() || payload.base64Images.isNotEmpty() ->
                ImageGenerationProbeRecord(ImageGenerationCapabilityStatus.Supported, message = "")
            isUnsupportedProbeMessage(payload.displayText) ->
                ImageGenerationProbeRecord(
                    ImageGenerationCapabilityStatus.Unsupported,
                    message = repository.getContext().getString(R.string.error_image_generation_not_supported)
                )
            else ->
                ImageGenerationProbeRecord(ImageGenerationCapabilityStatus.Unknown, message = payload.displayText)
        }
        repository.setImageGenerationProbeRecord(
            repository.getAssistantModelConfig(assistantId).serviceProvider,
            modelInfo.apiCode,
            record
        )
        return getSupport(modelInfo)
    }

    fun getSupport(modelInfo: ModelInfo): ImageGenerationSupport {
        val config = repository.getAssistantModelConfig(assistantId)
        if (!config.useCloudModel) {
            return ImageGenerationSupport(
                status = ImageGenerationCapabilityStatus.Unsupported,
                canProbe = false,
                message = repository.getContext().getString(R.string.error_image_generation_not_supported)
            )
        }

        val serviceProvider = config.serviceProvider
        val probe = repository.getImageGenerationProbeRecord(serviceProvider, modelInfo.apiCode)
        if (probe?.status == ImageGenerationCapabilityStatus.Unsupported) {
            return ImageGenerationSupport(
                status = ImageGenerationCapabilityStatus.Unsupported,
                message = probe.message.ifBlank {
                    repository.getContext().getString(R.string.error_image_generation_not_supported)
                }
            )
        }

        val modelKey = modelInfo.apiCode.lowercase(Locale.ROOT)
        val providerKey = BuiltInServiceProviders.normalizeId(serviceProvider)
        val explicitCaps = modelInfo.capabilities
        val explicitImageCap = explicitCaps.contains(ModelCapability.ImageGeneration)

        if (providerKey == BuiltInServiceProviders.SiliconFlow && modelKey.contains("qwen-image")) {
            return ImageGenerationSupport(
                status = ImageGenerationCapabilityStatus.Supported,
                supportsNegativePrompt = true,
                supportsSteps = true,
                supportsGuidanceScale = true,
                supportsSeed = true,
                maxCount = 4
            )
        }
        if (providerKey == BuiltInServiceProviders.Volcengine && modelKey.contains("seedream")) {
            return ImageGenerationSupport(
                status = ImageGenerationCapabilityStatus.Supported,
                maxCount = if (modelKey.contains("seedream-3") || modelKey.contains("seedream 3")) 1 else 4
            )
        }
        if (explicitImageCap || probe?.status == ImageGenerationCapabilityStatus.Supported) {
            return ImageGenerationSupport(
                status = ImageGenerationCapabilityStatus.Supported,
                maxCount = 4,
                canProbe = false
            )
        }
        if (explicitCaps.isNotEmpty()) {
            return ImageGenerationSupport(
                status = ImageGenerationCapabilityStatus.Unsupported,
                message = repository.getContext().getString(R.string.error_image_generation_not_supported)
            )
        }
        val providerType = repository.getProviderType(serviceProvider)
        val canProbe = providerType == ProviderType.OpenAI || providerType == ProviderType.AzureOpenAI
        return ImageGenerationSupport(
            status = ImageGenerationCapabilityStatus.Unknown,
            canProbe = canProbe,
            message = repository.getContext().getString(R.string.image_generation_capability_unknown)
        )
    }

    private fun buildRequestJson(
        serviceProvider: String,
        modelInfo: ModelInfo,
        request: ImageGenerationRequest,
        support: ImageGenerationSupport
    ): JsonObject {
        val providerKey = BuiltInServiceProviders.normalizeId(serviceProvider)
        val modelKey = modelInfo.apiCode.lowercase(Locale.ROOT)
        return JsonObject().apply {
            addProperty("model", modelInfo.apiCode)
            addProperty("prompt", request.prompt)
            when {
                providerKey == BuiltInServiceProviders.SiliconFlow && modelKey.contains("qwen-image") -> {
                    addProperty("negative_prompt", request.negativePrompt)
                    addProperty("image_size", "${request.width}x${request.height}")
                    addProperty("batch_size", request.count)
                    addProperty("num_inference_steps", request.steps)
                    addProperty("guidance_scale", request.guidanceScale)
                    addProperty("cfg", request.guidanceScale)
                    request.seed?.let { addProperty("seed", it) }
                }
                providerKey == BuiltInServiceProviders.Volcengine && modelKey.contains("seedream") -> {
                    addProperty("size", "${request.width}x${request.height}")
                    addProperty("n", request.count.coerceAtMost(support.maxCount))
                }
                else -> {
                    addProperty("n", request.count.coerceAtMost(support.maxCount))
                    addProperty("size", "${request.width}x${request.height}")
                    addProperty("response_format", "url")
                    if (support.supportsNegativePrompt && request.negativePrompt.isNotBlank()) {
                        addProperty("negative_prompt", request.negativePrompt)
                    }
                    if (support.supportsSteps) {
                        addProperty("num_inference_steps", request.steps)
                    }
                    if (support.supportsGuidanceScale) {
                        addProperty("guidance_scale", request.guidanceScale)
                    }
                    if (support.supportsSeed) {
                        request.seed?.let { addProperty("seed", it) }
                    }
                }
            }
        }
    }

    private fun parseImageGenerationPayload(jsonResult: String): ImageGenerationResponsePayload {
        return try {
            val jsonObject = JsonParser.parseString(jsonResult).asJsonObject
            val remoteUrls = mutableListOf<String>()
            val base64Images = mutableListOf<String>()

            fun consumeArray(name: String) {
                val arr = jsonObject.getAsJsonArray(name) ?: return
                for (element in arr) {
                    val obj = element?.asJsonObject ?: continue
                    obj.get("url")?.asString?.takeIf { it.isNotBlank() }?.let(remoteUrls::add)
                    obj.get("b64_json")?.asString?.takeIf { it.isNotBlank() }?.let(base64Images::add)
                }
            }

            consumeArray("images")
            consumeArray("data")

            if (remoteUrls.isNotEmpty() || base64Images.isNotEmpty()) {
                ImageGenerationResponsePayload(
                    remoteUrls = remoteUrls,
                    base64Images = base64Images
                )
            } else {
                ImageGenerationResponsePayload(
                    displayText = ModelErrorParser.extractErrorFromJson(jsonResult)
                        ?.let { ModelErrorParser.normalizeUserError(it) }
                        ?: repository.getContext().getString(R.string.image_generation_result_fallback, jsonResult)
                )
            }
        } catch (e: Exception) {
            ImageGenerationResponsePayload(
                displayText = ModelErrorParser.normalizeUserError(
                    repository.getContext().getString(
                        R.string.error_image_generation_parse_failed,
                        e.message.orEmpty()
                    )
                )
            )
        }
    }

    private fun normalizeRequest(
        request: ImageGenerationRequest,
        support: ImageGenerationSupport
    ): ImageGenerationRequest {
        val width = request.width.coerceAtLeast(64)
        val height = request.height.coerceAtLeast(64)
        return request.copy(
            width = width,
            height = height,
            count = request.count.coerceIn(1, support.maxCount.coerceAtLeast(1)),
            negativePrompt = if (support.supportsNegativePrompt) request.negativePrompt else "",
            steps = if (support.supportsSteps) request.steps.coerceIn(10, 60) else 20,
            guidanceScale = if (support.supportsGuidanceScale) request.guidanceScale.coerceIn(1f, 15f) else 7.5f,
            seed = if (support.supportsSeed) request.seed else null
        )
    }

    private fun extractRequestFromMessages(messages: List<Message>): ImageGenerationRequest {
        val content = messages.lastOrNull()?.content as? String ?: ""
        return runCatching {
            val obj = JsonParser.parseString(content).asJsonObject
            ImageGenerationRequest(
                prompt = obj.get("prompt")?.asString ?: content,
                negativePrompt = obj.get("negative_prompt")?.asString ?: "",
                width = obj.get("width")?.asInt ?: obj.get("image_width")?.asInt ?: 1024,
                height = obj.get("height")?.asInt ?: obj.get("image_height")?.asInt ?: 1024,
                count = obj.get("batch_size")?.asInt ?: obj.get("n")?.asInt ?: 1,
                steps = obj.get("num_inference_steps")?.asInt ?: 20,
                guidanceScale = obj.get("guidance_scale")?.asFloat ?: 7.5f,
                seed = obj.get("seed")?.asLong
            )
        }.getOrElse {
            ImageGenerationRequest(prompt = content)
        }
    }

    private fun isUnsupportedProbeMessage(message: String): Boolean {
        val normalized = message.lowercase(Locale.ROOT)
        if (!ModelErrorParser.isErrorMessage(message)) return false
        return listOf(
            "image generation",
            "images/generations",
            "unsupported",
            "not supported",
            "invalid model",
            "model not found",
            "does not exist"
        ).any { normalized.contains(it) }
    }

    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build()
    }
}
