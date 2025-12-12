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
data class Message(
    val role: String,
    val content: String
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
    val message: Message,
    val finishReason: String
)

data class Usage(
    val promptTokens: Int?,
    val completionTokens: Int?,
    val totalTokens: Int?
)