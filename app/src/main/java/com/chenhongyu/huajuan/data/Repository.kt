package com.chenhongyu.huajuan.data

import android.content.Context
import com.chenhongyu.huajuan.network.OpenAiApiService
import com.chenhongyu.huajuan.network.OpenAiRequest
import com.chenhongyu.huajuan.network.Message as NetworkMessage
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import androidx.room.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class Repository(private val context: Context) {
    // 存储暗色模式设置的键名
    private val DARK_MODE_KEY = "dark_mode"
    
    // SharedPreferences的名字
    private val PREFS_NAME = "hua_juan_prefs"
    
    // 获取SharedPreferences实例
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Room数据库实例
    private val database = AppDatabase.getDatabase(context)
    private val conversationDao = database.conversationDao()
    private val messageDao = database.messageDao()
    
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
    
    // 聊天历史相关方法 - 使用Room数据库
    fun getConversationsFlow(): Flow<List<ConversationEntity>> {
        return conversationDao.getAllConversations()
    }
    
    fun getConversations(): List<Conversation> {
        // 为了向后兼容，仍然返回旧的Conversation类型
        return runBlocking {
            conversationDao.getAllConversations().first().map { entity ->
                Conversation(
                    id = entity.id,
                    title = entity.title,
                    lastMessage = entity.lastMessage,
                    timestamp = entity.timestamp
                )
            }
        }
    }
    
    fun getMessagesFlow(conversationId: String): Flow<List<MessageEntity>> {
        return messageDao.getMessagesByConversationId(conversationId)
    }
    
    fun getMessages(conversationId: String): List<Message> {
        println("DEBUG: Repository.getMessages called for conversationId: $conversationId")
        // 为了向后兼容，仍然返回旧的Message类型
        return runBlocking {
            withContext(Dispatchers.IO) {
                conversationDao.getAllConversations().first().find { it.id == conversationId } ?: run {
                    // 如果没有找到对应的对话，创建一个新的
                    println("DEBUG: Conversation not found, creating new one for ID: $conversationId")
                    val newConversation = ConversationEntity(
                        id = conversationId,
                        title = "对话",
                        lastMessage = "",
                        timestamp = java.util.Date()
                    )
                    conversationDao.insertConversation(newConversation)
                }
                
                val messages = messageDao.getMessagesByConversationId(conversationId).first().map { entity ->
                    Message(
                        id = entity.id,
                        text = entity.text,
                        isUser = entity.isUser,
                        timestamp = entity.timestamp
                    )
                }
                println("DEBUG: Returning ${messages.size} messages for conversationId: $conversationId")
                messages
            }
        }
    }
    
    suspend fun saveMessages(conversationId: String, messages: List<Message>) {
        // 在IO线程中执行数据库操作
        withContext(Dispatchers.IO) {
            // 确保对话存在
            val conversationExists = conversationDao.getAllConversations().first().any { it.id == conversationId }
            if (!conversationExists) {
                val newConversation = ConversationEntity(
                    id = conversationId,
                    title = "对话",
                    lastMessage = "",
                    timestamp = java.util.Date()
                )
                conversationDao.insertConversation(newConversation)
            }
            
            // 删除该对话的所有现有消息
            messageDao.deleteMessagesByConversationId(conversationId)
            
            // 插入新消息
            val messageEntities = messages.map { message ->
                MessageEntity(
                    id = message.id,
                    conversationId = conversationId,
                    text = message.text,
                    isUser = message.isUser,
                    timestamp = message.timestamp
                )
            }
            messageDao.insertMessages(messageEntities)
        }
    }
    
    suspend fun createNewConversation(title: String): Conversation {
        // 在IO线程中执行数据库操作
        return withContext(Dispatchers.IO) {
            val newId = java.util.UUID.randomUUID().toString()
            val newConversationEntity = ConversationEntity(
                id = newId,
                title = title,
                lastMessage = "",
                timestamp = java.util.Date()
            )
            
            conversationDao.insertConversation(newConversationEntity)
            
            Conversation(
                id = newId,
                title = title,
                lastMessage = "",
                timestamp = newConversationEntity.timestamp
            )
        }
    }
    
    suspend fun deleteConversation(conversationId: String) {
        // 在IO线程中执行数据库操作
        withContext(Dispatchers.IO) {
            // 删除对话中的所有消息
            messageDao.deleteMessagesByConversationId(conversationId)
            
            // 删除对话本身
            conversationDao.deleteConversationById(conversationId)
        }
    }
    
    suspend fun updateLastMessage(conversationId: String, lastMessage: String) {
        // 在IO线程中执行数据库操作
        withContext(Dispatchers.IO) {
            val conversation = conversationDao.getConversationById(conversationId)
            if (conversation != null) {
                val updatedConversation = conversation.copy(
                    lastMessage = lastMessage,
                    timestamp = java.util.Date()
                )
                conversationDao.updateConversation(updatedConversation)
            }
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
        val serviceProvider = getServiceProvider()
        return when (serviceProvider) {
            "OpenAI" -> "https://api.openai.com/"
            "Azure" -> "https://YOUR_RESOURCE_NAME.openai.azure.com/" // 需要用户在Azure门户获取
            "Anthropic" -> "https://api.anthropic.com/"
            "自定义" -> getCustomApiUrl().ifEmpty { "https://api.openai.com/" }
            else -> "https://api.openai.com/"
        }
    }
    
    // 获取AI响应
    suspend fun getAIResponse(messages: List<Message>): String {
        try {
            val apiKey = getApiKey()
            if (apiKey.isEmpty()) {
                return "错误：API密钥未设置"
            }
            
            val selectedModel = getSelectedModel()
            val openAiMessages = messages.map { 
                NetworkMessage(
                    role = if (it.isUser) "user" else "assistant",
                    content = it.text
                )
            }
            
            // 构造请求对象
            val request = OpenAiRequest(
                model = when (selectedModel) {
                    "GPT-4" -> "gpt-4"
                    "GPT-3.5 Turbo" -> "gpt-3.5-turbo"
                    else -> "gpt-3.5-turbo"
                },
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
            return "错误：${e.message ?: e.javaClass.simpleName}"
        }
    }
    

}