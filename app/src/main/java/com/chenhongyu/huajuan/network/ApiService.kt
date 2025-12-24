package com.chenhongyu.huajuan.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Header

// OpenAI API 接口
interface OpenAiApiService {
    @POST(".")
    suspend fun getChatCompletion(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: OpenAiRequest
    ): Response<OpenAiResponse>
}

// 请求数据类
data class OpenAiRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Float = 0.7f
)

// 注意：这里的Message类与Repository中的Message类不同
// 支持多模态内容（文本和图片）
data class Message(
    val role: String,
    val content: Any // 支持字符串或内容数组
)

// 内容项数据类（支持文本和图片）
data class ContentItem(
    val type: String, // "text" 或 "image_url"
    val text: String? = null,
    val image_url: ImageUrl? = null
)

// 图片URL数据类
data class ImageUrl(
    val url: String
)

// 响应数据类
data class OpenAiResponse(
    val id: String,
    val choices: List<Choice>,
    val created: Long,
    val model: String,
    val systemFingerprint: String?,
    val `object`: String,
    val usage: Usage?
)

data class Choice(
    val index: Int,
    val message: MessageResponse,
    val finishReason: String
)

// 用于响应的消息类，专门处理AI返回的内容
data class MessageResponse(
    val role: String,
    val content: String // 简化为String类型，避免循环引用
)

data class Usage(
    val promptTokens: Int?,
    val completionTokens: Int?,
    val totalTokens: Int?
)

// 便利构造函数，用于创建仅文本的消息
fun createTextMessage(role: String, content: String): Message {
    return Message(role, content)
}

// 便利构造函数，用于创建带图片的消息
fun createImageMessage(role: String, text: String, imageUris: List<String>): Message {
    val contentList = mutableListOf<ContentItem>()
    // 添加文本内容
    contentList.add(ContentItem(type = "text", text = text))
    
    // 添加图片内容
    for (imageUri in imageUris) {
        // 对于本地文件，需要先上传或转换为base64
        val imageUrl = if (imageUri.startsWith("http") || imageUri.startsWith("data:")) {
            imageUri  // 已经是URL或data URL
        } else {
            imageUri
        }
        contentList.add(ContentItem(type = "image_url", image_url = ImageUrl(url = imageUrl)))
    }
    
    return Message(role, contentList)
}