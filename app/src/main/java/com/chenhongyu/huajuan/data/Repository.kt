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
import com.chenhongyu.huajuan.data.ModelDataProvider
import com.chenhongyu.huajuan.data.ModelInfo

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
        val defaultProvider = "硅基流动" // 更改默认服务提供商
        return prefs.getString("service_provider", defaultProvider) ?: defaultProvider
    }
    
    fun setServiceProvider(serviceProvider: String) {
        prefs.edit().putString("service_provider", serviceProvider).apply()
    }
    
    // 获取指定服务商的API密钥
    fun getApiKeyForProvider(provider: String): String {
        return prefs.getString("api_key_$provider", "") ?: ""
    }
    
    // 设置指定服务商的API密钥
    fun setApiKeyForProvider(provider: String, apiKey: String) {
        prefs.edit().putString("api_key_$provider", apiKey).apply()
    }
    
    // 获取当前服务商的API密钥（向后兼容）
    fun getApiKey(): String {
        val currentProvider = getServiceProvider()
        return getApiKeyForProvider(currentProvider)
    }
    
    // 设置当前服务商的API密钥（向后兼容）
    fun setApiKey(apiKey: String) {
        val currentProvider = getServiceProvider()
        setApiKeyForProvider(currentProvider, apiKey)
    }
    
    // 获取指定服务商选中的模型
    fun getSelectedModelForProvider(provider: String): String {
        return prefs.getString("selected_model_$provider", "") ?: ""
    }
    
    // 设置指定服务商选中的模型
    fun setSelectedModelForProvider(provider: String, selectedModel: String) {
        prefs.edit().putString("selected_model_$provider", selectedModel).apply()
    }
    
    // 获取当前服务商选中的模型（向后兼容）
    fun getSelectedModel(): String {
        val currentProvider = getServiceProvider()
        val model = getSelectedModelForProvider(currentProvider)
        return if (model.isNotEmpty()) model else "GPT-3.5 Turbo"
    }
    
    // 设置当前服务商选中的模型（向后兼容）
    fun setSelectedModel(selectedModel: String) {
        val currentProvider = getServiceProvider()
        setSelectedModelForProvider(currentProvider, selectedModel)
    }
    
    fun getCustomApiUrl(): String {
        return prefs.getString("custom_api_url", "") ?: ""
    }
    
    fun setCustomApiUrl(customApiUrl: String) {
        prefs.edit().putString("custom_api_url", customApiUrl).apply()
    }
    
    // 自定义服务提供商相关方法
    fun getCustomServiceProviders(): Set<String> {
        return prefs.getStringSet("custom_service_providers", emptySet()) ?: emptySet()
    }
    
    fun addCustomServiceProvider(name: String, apiUrl: String) {
        val customProviders = getCustomServiceProviders().toMutableSet()
        customProviders.add(name)
        prefs.edit()
            .putStringSet("custom_service_providers", customProviders)
            .putString("custom_provider_url_$name", apiUrl)
            .apply()
    }
    
    fun removeCustomServiceProvider(name: String) {
        val customProviders = getCustomServiceProviders().toMutableSet()
        customProviders.remove(name)
        prefs.edit()
            .putStringSet("custom_service_providers", customProviders)
            .remove("custom_provider_url_$name")
            .remove("custom_provider_models_$name")
            // 同时移除该服务商的API密钥和选中模型
            .remove("api_key_$name")
            .remove("selected_model_$name")
            .apply()
    }
    
    fun getCustomProviderApiUrl(name: String): String {
        return prefs.getString("custom_provider_url_$name", "") ?: ""
    }
    
    // 自定义模型相关方法
    fun getCustomModelsForProvider(providerName: String): Set<String> {
        return prefs.getStringSet("custom_provider_models_$providerName", emptySet()) ?: emptySet()
    }
    
    fun addCustomModelToProvider(providerName: String, modelName: String, apiCode: String) {
        val customModels = getCustomModelsForProvider(providerName).toMutableSet()
        customModels.add(modelName)
        prefs.edit()
            .putStringSet("custom_provider_models_$providerName", customModels)
            .putString("custom_model_code_${providerName}_$modelName", apiCode)
            .apply()
    }
    
    fun removeCustomModelFromProvider(providerName: String, modelName: String) {
        val customModels = getCustomModelsForProvider(providerName).toMutableSet()
        customModels.remove(modelName)
        prefs.edit()
            .putStringSet("custom_provider_models_$providerName", customModels)
            .remove("custom_model_code_${providerName}_$modelName")
            .apply()
    }
    
    fun getCustomModelApiCode(providerName: String, modelName: String): String {
        return prefs.getString("custom_model_code_${providerName}_$modelName", "") ?: ""
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
            
            // 使用 upsert 操作避免冲突
            messageDao.upsertMessages(messageEntities)
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
    
    /**
     * 获取上下文
     */
    fun getContext(): Context {
        return context
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
        // 对于预定义的服务提供商，使用ModelDataProvider获取API URL
        val modelDataProvider = ModelDataProvider(this)
        val predefinedUrl = modelDataProvider.getApiUrlForProvider(serviceProvider)
        if (predefinedUrl.isNotEmpty()) {
            // 预定义的服务商URL已经是正确的格式，直接返回
            // 确保URL以斜杠结尾（满足Retrofit要求）
            return if (predefinedUrl.endsWith("/")) predefinedUrl else "$predefinedUrl/"
        }
        
        // 对于自定义服务提供商，使用存储的URL并进行规范化处理
        val customUrl = when (serviceProvider) {
            "自定义" -> getCustomApiUrl().ifEmpty { "https://api.openai.com/v1/chat/completions" }
            else -> getCustomProviderApiUrl(serviceProvider).ifEmpty { "https://api.openai.com/v1/chat/completions" }
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
    
    // 获取AI响应
    suspend fun getAIResponse(messages: List<Message>): String {
        try {
            // 使用模型API工厂创建相应服务
            val modelApiFactory = ModelApiFactory(this)
            val modelApiService = modelApiFactory.getCurrentModelApiService()
            
            // 获取当前选中的模型信息
            val serviceProvider = getServiceProvider()
            val selectedModelDisplayName = getSelectedModel()
            
            val modelDataProvider = ModelDataProvider(this)
            val modelList = modelDataProvider.getModelListForProvider(serviceProvider)
            val modelInfo = modelList.find { it.displayName == selectedModelDisplayName }
                ?: ModelInfo(selectedModelDisplayName, "gpt-3.5-turbo") // 默认模型
            
            // 将消息转换为网络消息格式
            val networkMessages = messages.map { 
                com.chenhongyu.huajuan.network.Message(
                    role = if (it.isUser) "user" else "assistant",
                    content = it.text
                )
            }
            
            // 调用相应的模型API服务
            return modelApiService.getAIResponse(networkMessages, modelInfo)
        } catch (e: Exception) {
            return "错误：${e.message ?: e.javaClass.simpleName}"
        }
    }
    

}