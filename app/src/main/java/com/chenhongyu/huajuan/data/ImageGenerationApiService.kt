package com.chenhongyu.huajuan.data

import com.chenhongyu.huajuan.network.Message
import com.chenhongyu.huajuan.stream.ChatEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.awaitClose
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName

/**
 * 图像生成API服务实现
 * 专门处理图像生成API，支持不同提供商的特定格式
 */
class ImageGenerationApiService(private val repository: Repository) : ModelApiService {

    override fun isAvailable(): Boolean {
        // 在线模型始终可用（只要有网络）
        return true
    }

    override suspend fun getAIResponse(messages: List<Message>, modelInfo: ModelInfo): String {
        try {
            val apiKey = repository.getApiKey()
            if (apiKey.isEmpty()) {
                return "错误：API密钥未设置"
            }

            // 根据服务提供商决定使用哪种图像生成API
            val serviceProvider = repository.getServiceProvider()
            val result = when {
                serviceProvider.contains("硅基流动-生图") -> generateImageWithSiliconFlow(apiKey, modelInfo, messages)
                serviceProvider.contains("豆包生图") -> generateImageWithDoubao(apiKey, modelInfo, messages)
                else -> generateImageWithStandardApi(apiKey, modelInfo, messages)
            }
            
            // 如果返回的是JSON格式的图像生成结果，解析并提取图片URL
            if (result.startsWith("{")) { // 检查是否为JSON格式
                return parseImageGenerationResult(result)
            }
            
            return result
        } catch (e: Exception) {
            return "错误：${e.message ?: e.javaClass.simpleName}"
        }
    }
    
    /**
     * 解析图像生成结果JSON，提取图片URL并格式化为可显示格式
     */
    private suspend fun parseImageGenerationResult(jsonResult: String): String {
        return try {
            val jsonObject = JsonParser.parseString(jsonResult).asJsonObject
            
            val imageUrls = mutableListOf<String>()
            
            // 检查 "images" 数组
            if (jsonObject.has("images") && jsonObject.get("images").isJsonArray) {
                val imagesArray = jsonObject.getAsJsonArray("images")
                for (element in imagesArray) {
                    if (element.isJsonObject) {
                        val imageObj = element.asJsonObject
                        if (imageObj.has("url")) {
                            val url = imageObj.get("url").asString
                            imageUrls.add(url)
                        }
                    }
                }
            }
            
            // 检查 "data" 数组（某些API使用这个字段）
            if (jsonObject.has("data") && jsonObject.get("data").isJsonArray) {
                val dataArray = jsonObject.getAsJsonArray("data")
                for (element in dataArray) {
                    if (element.isJsonObject) {
                        val dataObj = element.asJsonObject
                        if (dataObj.has("url")) {
                            val url = dataObj.get("url").asString
                            imageUrls.add(url)
                        }
                    }
                }
            }
            
            // 返回包含图片URL的格式化字符串，使用特殊标记让UI识别
            if (imageUrls.isNotEmpty()) {
                // 为了兼容UI的图片显示，返回图片URL列表，每行一个URL
                imageUrls.joinToString("\n")
            } else {
                "图像生成结果：$jsonResult"
            }
        } catch (e: Exception) {
            "图像生成完成，但解析结果失败：${e.message}，原始结果：$jsonResult"
        }
    }

    override fun streamAIResponse(messages: List<Message>, modelInfo: ModelInfo): Flow<ChatEvent> = channelFlow {
        try {
            val apiKey = repository.getApiKey()
            if (apiKey.isEmpty()) {
                trySend(ChatEvent.Error("错误：API密钥未设置"))
                close()
                return@channelFlow
            }

            // 图像生成通常不使用流式响应，所以直接发送完成事件
            val result = getAIResponse(messages, modelInfo)
            if (result.startsWith("错误：")) {
                trySend(ChatEvent.Error(result))
            } else {
                trySend(ChatEvent.Chunk(result))
            }
            trySend(ChatEvent.Done)
        } catch (e: Exception) {
            trySend(ChatEvent.Error("错误：${e.message}"))
        } finally {
            close()
        }
    }

    /**
     * 为硅基流动API生成图像（特殊JSON格式）
     */
    private suspend fun generateImageWithSiliconFlow(apiKey: String, modelInfo: ModelInfo, messages: List<Message>): String {
        return withContext(Dispatchers.IO) {
            val client = createOkHttpClient()

            // 提取提示词，通常在最后一条消息中
            val prompt = extractPromptFromMessages(messages)
            
            // 提取图像URL用于图像编辑
            val imageUrls = extractImageUrlsFromMessages(messages)

            // 构建硅基流动特有的JSON请求体，按照您提供的格式
            val gson = Gson()
            val json = JsonObject().apply {
                addProperty("model", modelInfo.apiCode)
                addProperty("prompt", prompt)
                addProperty("negative_prompt", "") // 使用空字符串而不是<stirng>占位符
                addProperty("image_size", "1024x1024") // 使用实际的图像尺寸
                addProperty("batch_size", 1)
                addProperty("seed", 4999999999L) // 按照API示例格式
                addProperty("num_inference_steps", 20)
                addProperty("guidance_scale", 7.5)
                addProperty("cfg", 10.05) // 按照API示例格式
                
                // 如果有图像URL，则添加到请求中用于图像编辑
                if (imageUrls.isNotEmpty()) {
                    addProperty("image", imageUrls[0]) // 使用第一个图像作为主图像
                    if (imageUrls.size > 1) addProperty("image2", imageUrls[1]) // 第二个图像
                    if (imageUrls.size > 2) addProperty("image3", imageUrls[2]) // 第三个图像
                }
            }

            val mediaType = "application/json".toMediaTypeOrNull()
            val requestBody = gson.toJson(json).toRequestBody(mediaType)

            val req = Request.Builder()
                .url("https://api.siliconflow.cn/v1/images/generations")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val call = client.newCall(req)
            val resp = call.execute()
            if (!resp.isSuccessful) {
                "错误：${resp.code} ${resp.message}"
            } else {
                val body = resp.body?.string()
                resp.body?.close()
                body ?: "错误：响应体为空"
            }
        }
    }

    /**
     * 为豆包API生成图像
     */
    private suspend fun generateImageWithDoubao(apiKey: String, modelInfo: ModelInfo, messages: List<Message>): String {
        return withContext(Dispatchers.IO) {
            val client = createOkHttpClient()

            // 提取提示词
            val prompt = extractPromptFromMessages(messages)

            // 构建豆包特有的JSON请求体
            val gson = Gson()
            val json = JsonObject().apply {
                addProperty("model", modelInfo.apiCode)
                addProperty("prompt", prompt)
                addProperty("n", 1) // 生成数量
                addProperty("size", "1024x1024") // 图像尺寸
            }

            val mediaType = "application/json".toMediaTypeOrNull()
            val requestBody = gson.toJson(json).toRequestBody(mediaType)

            // 豆包图像生成API的URL
            val req = Request.Builder()
                .url(repository.getBaseUrl())
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val call = client.newCall(req)
            val resp = call.execute()
            if (!resp.isSuccessful) {
                "错误：${resp.code} ${resp.message}"
            } else {
                val body = resp.body?.string()
                resp.body?.close()
                body ?: "错误：响应体为空"
            }
        }
    }

    /**
     * 使用标准图像生成API格式
     */
    private suspend fun generateImageWithStandardApi(apiKey: String, modelInfo: ModelInfo, messages: List<Message>): String {
        return withContext(Dispatchers.IO) {
            val client = createOkHttpClient()

            // 提取提示词
            val prompt = extractPromptFromMessages(messages)

            // 构建标准图像生成JSON请求体
            val gson = Gson()
            val json = JsonObject().apply {
                addProperty("model", modelInfo.apiCode)
                addProperty("prompt", prompt)
                addProperty("n", 1) // 生成数量
                addProperty("size", "1024x1024") // 图像尺寸
            }

            val mediaType = "application/json".toMediaTypeOrNull()
            val requestBody = gson.toJson(json).toRequestBody(mediaType)

            val req = Request.Builder()
                .url(repository.getBaseUrl())
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val call = client.newCall(req)
            val resp = call.execute()
            if (!resp.isSuccessful) {
                "错误：${resp.code} ${resp.message}"
            } else {
                val body = resp.body?.string()
                resp.body?.close()
                body ?: "错误：响应体为空"
            }
        }
    }

    /**
     * 从消息列表中提取提示词
     */
    private fun extractPromptFromMessages(messages: List<Message>): String {
        // 通常最后一条消息包含提示词
        return messages.lastOrNull()?.let { message ->
            when (val content = message.content) {
                is String -> content
                is List<*> -> {
                    // 如果是内容数组，提取文本内容
                    content.filterIsInstance<Map<*, *>>()
                        .filter { it["type"] == "text" }
                        .map { it["text"] as? String ?: "" }
                        .joinToString(" ")
                }
                else -> ""
            }
        } ?: ""
    }

    /**
     * 从消息列表中提取图像URL（用于图像编辑）
     */
    private fun extractImageUrlsFromMessages(messages: List<Message>): List<String> {
        val imageUrls = mutableListOf<String>()
        
        for (message in messages) {
            when (val content = message.content) {
                is String -> {
                    // 如果是字符串但包含URL，可以考虑提取
                    // 这里暂时不处理纯文本中的URL
                }
                is List<*> -> {
                    // 如果是内容数组，提取图像URL
                    content.filterIsInstance<Map<*, *>>()
                        .filter { it["type"] == "image_url" }
                        .forEach { item ->
                            val imageUrl = item["image_url"] as? Map<*, *>
                            var url = imageUrl?.get("url") as? String
                            if (url != null) {
                                // 如果是本地文件路径，需要转换为base64
                                if (!url.startsWith("http") && !url.startsWith("data:")) {
                                    val context = repository.getContext()
                                    val base64Url = com.chenhongyu.huajuan.utils.ImageUtils.convertImageToDataUrl(context, url)
                                    if (base64Url != null) {
                                        imageUrls.add(base64Url)
                                    }
                                } else {
                                    imageUrls.add(url)
                                }
                            }
                        }
                }
            }
        }
        
        return imageUrls
    }

    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS) // 图像生成可能需要更长时间
            .addInterceptor(loggingInterceptor)
            .build()
    }
}