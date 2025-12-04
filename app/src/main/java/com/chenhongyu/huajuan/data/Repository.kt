package com.chenhongyu.huajuan.data

import android.content.Context
import com.chenhongyu.huajuan.network.OpenAiApiService
import com.chenhongyu.huajuan.network.OpenAiRequest
import com.chenhongyu.huajuan.network.Message as NetworkMessage
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class Repository(private val context: Context) {
    // 存储暗色模式设置的键名
    private val DARK_MODE_KEY = "dark_mode"
    
    // SharedPreferences的名字
    private val PREFS_NAME = "hua_juan_prefs"
    
    // 获取SharedPreferences实例
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // 聊天历史管理器
    private val chatHistoryManager = ChatHistoryManager(context)
    
    /**
     * 获取暗色模式设置
     */
    fun getDarkMode(): Boolean {
        return prefs.getBoolean(DARK_MODE_KEY, false)
    }
    
    /**
     * 设置暗色模式
     */
    fun setDarkMode(isDarkMode: Boolean) {
        prefs.edit().putBoolean(DARK_MODE_KEY, isDarkMode).apply()
    }
    
    // 其他方法...
    fun getUseCloudModel(): Boolean {
        return prefs.getBoolean("use_cloud_model", false)
    }
    
    fun setUseCloudModel(useCloudModel: Boolean) {
        prefs.edit().putBoolean("use_cloud_model", useCloudModel).apply()
    }
    
    fun getServiceProvider(): String {
        return prefs.getString("service_provider", "OpenAI") ?: "OpenAI"
    }
    
    fun setServiceProvider(serviceProvider: String) {
        prefs.edit().putString("service_provider", serviceProvider).apply()
    }
    
    fun getCustomApiUrl(): String {
        return prefs.getString("custom_api_url", "") ?: ""
    }
    
    fun setCustomApiUrl(customApiUrl: String) {
        prefs.edit().putString("custom_api_url", customApiUrl).apply()
    }
    
    fun getApiKey(): String {
        return prefs.getString("api_key", "") ?: ""
    }
    
    fun setApiKey(apiKey: String) {
        prefs.edit().putString("api_key", apiKey).apply()
    }
    
    fun getSelectedModel(): String {
        return prefs.getString("selected_model", "GPT-3.5 Turbo") ?: "GPT-3.5 Turbo"
    }
    
    fun setSelectedModel(selectedModel: String) {
        prefs.edit().putString("selected_model", selectedModel).apply()
    }
    
    // 聊天历史相关方法
    fun saveConversations(conversations: List<Conversation>) {
        chatHistoryManager.saveConversations(conversations)
    }
    
    fun getConversations(): List<Conversation> {
        return chatHistoryManager.getConversations()
    }
    
    fun saveMessages(conversationId: Long, messages: List<Message>) {
        chatHistoryManager.saveMessages(conversationId, messages)
    }
    
    fun getMessages(conversationId: Long): List<Message> {
        return chatHistoryManager.getMessages(conversationId)
    }
    
    fun createNewConversation(title: String): Conversation {
        return chatHistoryManager.createNewConversation(title)
    }
    
    fun deleteConversation(conversationId: Long) {
        chatHistoryManager.deleteConversation(conversationId)
    }
    
    fun updateLastMessage(conversationId: Long, lastMessage: String) {
        chatHistoryManager.updateLastMessage(conversationId, lastMessage)
    }
    
    // 创建Retrofit实例
    private fun createRetrofitInstance(baseUrl: String): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
        
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    // 获取API服务实例
    private fun getApiService(): OpenAiApiService {
        val serviceProvider = getServiceProvider()
        val baseUrl = when (serviceProvider) {
            "OpenAI" -> "https://api.openai.com/"
            "Azure" -> "https://YOUR_RESOURCE_NAME.openai.azure.com/" // 需要用户在Azure门户获取
            "Anthropic" -> "https://api.anthropic.com/"
            "自定义" -> getCustomApiUrl().ifEmpty { "https://api.openai.com/" }
            else -> "https://api.openai.com/"
        }
        
        return createRetrofitInstance(baseUrl).create(OpenAiApiService::class.java)
    }
    
    // 流式获取AI响应
    suspend fun streamAIResponse(messages: List<Message>): kotlinx.coroutines.flow.Flow<String> {
        return kotlinx.coroutines.flow.flow {
            try {
                val apiKey = getApiKey()
                if (apiKey.isEmpty()) {
                    emit("错误：API密钥未设置")
                    return@flow
                }
                
                val selectedModel = getSelectedModel()
                val openAiMessages = messages.map { 
                    NetworkMessage(
                        role = if (it.isUser) "user" else "assistant",
                        content = it.text
                    )
                }
                
                val request = OpenAiRequest(
                    model = when (selectedModel) {
                        "GPT-4" -> "gpt-4"
                        "GPT-3.5 Turbo" -> "gpt-3.5-turbo"
                        "Claude 2" -> "claude-2"
                        "Claude Instant" -> "claude-instant"
                        else -> "gpt-3.5-turbo"
                    },
                    messages = openAiMessages,
                    temperature = 0.7f,
                    stream = true
                )
                
                val apiService = getApiService()
                val response = apiService.streamChatCompletion("Bearer $apiKey", request = request)
                
                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!
                    val source = responseBody.source()
                    
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line()
                        line?.let {
                            if (it.startsWith("data: ")) {
                                val jsonData = it.substring(6)
                                if (jsonData == "[DONE]") {
                                    return@flow
                                }
                                
                                try {
                                    // 简化的解析，实际应该用JSON解析库
                                    // 这里只是示例，实际需要更完善的JSON解析
                                    val content = parseContentFromJson(jsonData)
                                    if (content.isNotEmpty()) {
                                        emit(content)
                                    }
                                } catch (e: Exception) {
                                    // 忽略解析错误
                                }
                            }
                        }
                    }
                } else {
                    emit("错误：${response.message()}")
                }
            } catch (e: Exception) {
                emit("错误：${e.message}")
            }
        }
    }
    
    // 解析OpenAI流式响应中的内容
    private fun parseContentFromJson(jsonData: String): String {
        // 简化的JSON解析，实际应该用专门的JSON库
        // 查找 "content":"..." 模式
        val contentPattern = """"content"\s*:\s*"([^"]*)""""
        val regex = Regex(contentPattern)
        val matchResult = regex.find(jsonData)
        return matchResult?.groups?.get(1)?.value?.replace("\\n", "\n") ?: ""
    }
    
    suspend fun getAIResponse(prompt: String): String {
        kotlinx.coroutines.delay(1000) // 模拟网络延迟
        return "这是模拟的AI回复: $prompt"
    }
}