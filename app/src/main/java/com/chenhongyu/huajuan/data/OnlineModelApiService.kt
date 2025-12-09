package com.chenhongyu.huajuan.data

import android.util.Log
import com.chenhongyu.huajuan.network.Message
import com.chenhongyu.huajuan.network.OpenAiApiService
import com.chenhongyu.huajuan.network.OpenAiRequest
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 在线模型API服务实现
 */
class OnlineModelApiService(private val repository: Repository) : ModelApiService {
    companion object {
        private const val TAG = "OnlineModelApiService"
    }
    
    override fun isAvailable(): Boolean {
        // 在线模型始终可用（只要有网络）
        Log.d(TAG, "Online model service is always considered available")
        return true
    }
    
    override suspend fun getAIResponse(messages: List<Message>, modelInfo: ModelInfo): String {
        try {
            val apiKey = repository.getApiKey()
            if (apiKey.isEmpty()) {
                return "错误：API密钥未设置"
            }
            
            val openAiMessages = messages.map {
                Message(
                    role = if (it.role == "user") "user" else "assistant",
                    content = it.content
                )
            }
            
            // 构造请求对象
            val request = OpenAiRequest(
                model = modelInfo.apiCode,
                messages = openAiMessages,
                temperature = 0.7f
            )
            
            val apiService = createApiService()
            val response = apiService.getChatCompletion(
                authorization = "Bearer $apiKey",
                request = request
            )
            
            if (response.isSuccessful) {
                val openAiResponse = response.body()
                return openAiResponse?.choices?.firstOrNull()?.message?.content ?: ""
            } else {
                return "错误：HTTP ${response.code()} - ${response.message()}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting AI response from online model", e)
            return "错误：${e.message ?: e.javaClass.simpleName}"
        }
    }
    
    // 创建OkHttpClient实例
    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }
    
    // 创建API服务
    private fun createApiService(): OpenAiApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(getBaseUrl())
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        return retrofit.create(OpenAiApiService::class.java)
    }
    
    // 获取基础URL
    private fun getBaseUrl(): String {
        val serviceProvider = repository.getServiceProvider()
        // 对于预定义的服务提供商，使用ModelDataProvider获取API URL
        val modelDataProvider = ModelDataProvider(repository)
        val predefinedUrl = modelDataProvider.getApiUrlForProvider(serviceProvider)
        if (predefinedUrl.isNotEmpty()) {
            // 预定义的服务商URL已经是正确的格式，直接返回
            // 确保URL以斜杠结尾（满足Retrofit要求）
            return if (predefinedUrl.endsWith("/")) predefinedUrl else "$predefinedUrl/"
        }
        
        // 对于自定义服务提供商，使用存储的URL并进行规范化处理
        val customUrl = when (serviceProvider) {
            "自定义" -> repository.getCustomApiUrl().ifEmpty { "https://api.openai.com/v1/chat/completions" }
            else -> repository.getCustomProviderApiUrl(serviceProvider).ifEmpty { "https://api.openai.com/v1/chat/completions" }
        }
        
        return normalizeBaseUrl(customUrl)
    }
    
    /**
     * 规范化基础URL，确保它以正确的格式结尾，适用于OpenAI API格式
     * 仅对用户自定义的服务商生效
     * 处理各种可能的输入格式：
     * - https://ai.soruxgpt.com
     * - https://ai.soruxgpt.com/
     * - https://ai.soruxgpt.com/v1/chat/completions
     * - https://ai.soruxgpt.com/v1/chat/completions/
     */
    private fun normalizeBaseUrl(url: String): String {
        if (url.isBlank()) return "https://api.openai.com/v1/chat/completions/"
        
        var normalizedUrl = url.trim()
        
        // 移除末尾的斜杠
        while (normalizedUrl.endsWith("/")) {
            normalizedUrl = normalizedUrl.dropLast(1)
        }
        
        // 检查是否以标准的OpenAI API路径结尾
        if (normalizedUrl.endsWith("/v1/chat/completions") ||
            normalizedUrl.endsWith("/v1/completions") ||
            normalizedUrl.endsWith("/v1/embeddings")) {
            // 已经是完整的API端点URL，直接返回
            return "$normalizedUrl/"
        } else if (normalizedUrl.contains("/v1/")) {
            // 如果包含其他v1端点，也直接返回
            return "$normalizedUrl/"
        }
        
        // 不包含v1路径，需要添加默认的chat completions路径
        // 确保URL以斜杠结尾
        if (!normalizedUrl.endsWith("/")) {
            normalizedUrl += "/"
        }
        
        // 确保URL以https://开头
        if (!normalizedUrl.startsWith("https://") && !normalizedUrl.startsWith("http://")) {
            normalizedUrl = "https://$normalizedUrl"
        }
        
        // 添加默认的v1 chat completions路径
        return "${normalizedUrl}v1/chat/completions/"
    }
}