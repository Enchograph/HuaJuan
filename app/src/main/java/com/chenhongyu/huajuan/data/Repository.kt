package com.chenhongyu.huajuan.data

import android.content.Context
import com.chenhongyu.huajuan.network.OpenAiApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.chenhongyu.huajuan.data.ModelDataProvider
import com.chenhongyu.huajuan.data.ModelInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class Repository(private val context: Context) {
    // 存储暗色模式设置的键名
    private val DARK_MODE_KEY = "dark_mode"
    
    // 调试模式设置的键名
    private val DEBUG_MODE_KEY = "debug_mode"
    
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
    
    /**
     * 获取调试模式设置
     */
    fun getDebugMode(): Boolean {
        return prefs.getBoolean(DEBUG_MODE_KEY, false)
    }
    
    /**
     * 设置调试模式
     */
    fun setDebugMode(isDebugMode: Boolean) {
        prefs.edit().putBoolean(DEBUG_MODE_KEY, isDebugMode).apply()
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
        val stored = prefs.getString("api_key_$provider", "") ?: ""
        if (stored.isNotEmpty()) return stored
        return if (provider == "应用试用") "sk-1IUNxNlOafLp2zzkJIayMaMJTXL1zvMvYZq4OCmjzOvQz1hu" else ""
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

    // 用户信息持久化（用户名、签名、头像字符）
    private val _userInfoFlow: MutableStateFlow<UserInfo> = MutableStateFlow(
        run {
            val username = prefs.getString("user_name", "用户名") ?: "用户名"
            val signature = prefs.getString("user_signature", "个性签名") ?: "个性签名"
            val avatar = prefs.getString("user_avatar", "U") ?: "U"
            UserInfo(username = username, signature = signature, avatar = avatar)
        }
    )
    val userInfoFlow: StateFlow<UserInfo> = _userInfoFlow

    fun getUserInfo(): UserInfo {
        val username = prefs.getString("user_name", "用户名") ?: "用户名"
        val signature = prefs.getString("user_signature", "个性签名") ?: "个性签名"
        val avatar = prefs.getString("user_avatar", "U") ?: "U"
        return UserInfo(username = username, signature = signature, avatar = avatar)
    }

    fun setUserInfo(userInfo: UserInfo) {
        prefs.edit()
            .putString("user_name", userInfo.username)
            .putString("user_signature", userInfo.signature)
            .putString("user_avatar", userInfo.avatar)
            .apply()
        // update flow so UI can react
        _userInfoFlow.value = userInfo
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
                    timestamp = entity.timestamp,
                    roleName = entity.roleName,
                    systemPrompt = entity.systemPrompt
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
                        timestamp = java.util.Date(),
                        roleName = "默认助手",
                        systemPrompt = "你是一个AI助手"
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
                    timestamp = java.util.Date(),
                    roleName = "默认助手",
                    systemPrompt = "你是一个AI助手"
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
    
    suspend fun createNewConversation(title: String, roleName: String = "默认助手", systemPrompt: String = "你是一个AI助手"): Conversation {
        // 在IO线程中执行数据库操作
        return withContext(Dispatchers.IO) {
            val newId = java.util.UUID.randomUUID().toString()
            val newConversationEntity = ConversationEntity(
                id = newId,
                title = title,
                lastMessage = "",
                timestamp = java.util.Date(),
                roleName = roleName,
                systemPrompt = systemPrompt
            )
            
            conversationDao.insertConversation(newConversationEntity)
            
            Conversation(
                id = newId,
                title = title,
                lastMessage = "",
                timestamp = newConversationEntity.timestamp,
                roleName = roleName,
                systemPrompt = systemPrompt
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
     * 更新对话标题
     */
    suspend fun updateConversationTitle(conversationId: String, title: String) {
        // 在IO线程中执行数据库操作
        withContext(Dispatchers.IO) {
            val conversation = conversationDao.getConversationById(conversationId)
            if (conversation != null) {
                val updatedConversation = conversation.copy(
                    title = title,
                    timestamp = java.util.Date()
                )
                conversationDao.updateConversation(updatedConversation)
            }
        }
    }
    
    /**
     * 获取对话的角色名称
     */
    fun getConversationRoleName(conversationId: String): String {
        return runBlocking {
            withContext(Dispatchers.IO) {
                val conversation = conversationDao.getConversationById(conversationId)
                conversation?.roleName ?: "默认助手"
            }
        }
    }
    
    /**
     * 获取对话的系统提示词
     */
    fun getConversationSystemPrompt(conversationId: String): String {
        return runBlocking {
            withContext(Dispatchers.IO) {
                val conversation = conversationDao.getConversationById(conversationId)
                conversation?.systemPrompt ?: "你是一个AI助手"
            }
        }
    }
    
    /**
     * 更新对话的角色名称和系统提示词
     */
    suspend fun updateConversationRole(conversationId: String, roleName: String, systemPrompt: String) {
        withContext(Dispatchers.IO) {
            val conversation = conversationDao.getConversationById(conversationId)
            if (conversation != null) {
                val updatedConversation = conversation.copy(
                    roleName = roleName,
                    systemPrompt = systemPrompt,
                    timestamp = java.util.Date()
                )
                conversationDao.updateConversation(updatedConversation)
            }
        }
    }
    
    // 创建OkHttpClient实例
    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // Avoid BODY level for general client to prevent accidental buffering of streaming responses.
            level = HttpLoggingInterceptor.Level.HEADERS
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
    fun getBaseUrl(): String {
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
     * 处理各种可能的输入格式
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
    suspend fun getAIResponse(messages: List<Message>, conversationId: String): String {
        try {
            val modelApiFactory = ModelApiFactory(this)
            val modelApiService = modelApiFactory.getCurrentModelApiService()
            val useCloud = getUseCloudModel()
            val serviceProvider = getServiceProvider()
            val selectedModelDisplayName = if (useCloud) getSelectedModel() else getLocalSelectedModel()
            println("DEBUG: getAIResponse useCloudModel=$useCloud, serviceProvider='$serviceProvider', selectedModelDisplayName='$selectedModelDisplayName'")
            val modelInfo = if (useCloud) {
                val modelDataProvider = ModelDataProvider(this)
                val modelList = modelDataProvider.getModelListForProvider(serviceProvider)
                modelList.find { it.displayName == selectedModelDisplayName }
                    ?: ModelInfo(selectedModelDisplayName, selectedModelDisplayName)
            } else {
                ModelInfo(selectedModelDisplayName, selectedModelDisplayName)
            }
            println("DEBUG: getAIResponse resolved modelInfo.displayName='${modelInfo.displayName}', apiCode='${modelInfo.apiCode}'")
            val systemPrompt = getConversationSystemPrompt(conversationId)
            val networkMessages = listOf(
                com.chenhongyu.huajuan.network.Message(
                    role = "system",
                    content = systemPrompt
                )
            ) + messages.map {
                com.chenhongyu.huajuan.network.Message(
                    role = if (it.isUser) "user" else "assistant",
                    content = it.text
                )
            }
            return modelApiService.getAIResponse(networkMessages, modelInfo)
        } catch (e: Exception) {
            return "错误：${e.message ?: e.javaClass.simpleName}"
        }
    }

    // 兼容性的流式输出包装：如果后端/本地模型不支持原生流式API，
    // 我们仍然可以将完整回复按词或固定大小分块后以Flow的方式逐步发出，
    // 从而为UI提供平滑的流式渲染体验。
    fun streamAIResponse(messages: List<Message>, conversationId: String): Flow<com.chenhongyu.huajuan.stream.ChatEvent> {
        val modelApiFactory = ModelApiFactory(this)
        val modelApiService = modelApiFactory.getCurrentModelApiService()
        val useCloud = getUseCloudModel()
        val serviceProvider = getServiceProvider()
        val selectedModelDisplayName = if (useCloud) getSelectedModel() else getLocalSelectedModel()
        println("DEBUG: useCloudModel=$useCloud, serviceProvider='$serviceProvider', selectedModelDisplayName='$selectedModelDisplayName'")
        val modelInfo = if (useCloud) {
            val modelDataProvider = ModelDataProvider(this)
            val modelList = modelDataProvider.getModelListForProvider(serviceProvider)
            modelList.find { it.displayName == selectedModelDisplayName }
                ?: ModelInfo(selectedModelDisplayName, selectedModelDisplayName)
        } else {
            // 本地模型直接使用displayName作为识别码
            ModelInfo(selectedModelDisplayName, selectedModelDisplayName)
        }
        println("DEBUG: Resolved modelInfo.displayName='${modelInfo.displayName}', apiCode='${modelInfo.apiCode}'")
        val systemPrompt = getConversationSystemPrompt(conversationId)
        val networkMessages = listOf(
            com.chenhongyu.huajuan.network.Message(
                role = "system",
                content = systemPrompt
            )
        ) + messages.map {
            com.chenhongyu.huajuan.network.Message(
                role = if (it.isUser) "user" else "assistant",
                content = it.text
            )
        }

        return modelApiService.streamAIResponse(networkMessages, modelInfo)
    }

    suspend fun createAICreationFromMessage(
        title: String,
        username: String?,
        userSignature: String?,
        aiRoleName: String?,
        aiModelName: String?,
        promptHtml: String,
        promptJson: String?,
        conversationText: String? = null,
        conversationAt: Long? = null,
        publishedAt: Long? = null,
        commentary: String? = null,
        width: Int = 1024,
        height: Int = 1024
    ): String {
        return withContext(Dispatchers.IO) {
            val id = java.util.UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val entity = AICreationEntity(
                id = id,
                username = username,
                userSignature = userSignature,
                aiRoleName = aiRoleName,
                aiModelName = aiModelName,
                title = title,
                commentary = commentary,
                promptHtml = promptHtml,
                promptJson = promptJson,
                conversationText = conversationText,
                conversationAt = conversationAt,
                publishedAt = publishedAt,
                imageFileName = null,
                width = width,
                height = height,
                status = "PENDING",
                createdAt = now,
                updatedAt = now,
                extraJson = null
            )
            val dao = AppDatabase.getDatabase(context).aiCreationDao()
            dao.insertCreation(entity)
            id
        }
    }

    /**
     * Trigger generation synchronously (suspend). This uses GenerationManager internally.
     */
    suspend fun generateAICreationNow(id: String): Boolean {
        return try {
            com.chenhongyu.huajuan.workers.GenerationManager.generateForId(context, id)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Enqueue background generation job for an AI creation.
     * Current implementation launches a background coroutine that calls GenerationManager directly.
     * This avoids WorkManager usage in environments where WorkManager symbols may not be resolvable.
     */
    fun enqueueAICreationGeneration(id: String) {
        try {
            // Prefer WorkManager in production; below is a direct coroutine fallback for this repo environment.
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    com.chenhongyu.huajuan.workers.GenerationManager.generateForId(context, id)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            /*
            // WorkManager implementation (kept for reference):
            val workManager = androidx.work.WorkManager.getInstance(context.applicationContext)
            val data = androidx.work.Data.Builder().putString("ai_creation_id", id).build()
            val request = androidx.work.OneTimeWorkRequestBuilder<com.chenhongyu.huajuan.workers.GenerationWorkerImpl>()
                .setInputData(data)
                .build()
            workManager.enqueue(request)
            */
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteAICreation(id: String) {
        withContext(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(context).aiCreationDao()
            // delete image files if present
            val entity = dao.getCreationById(id)
            try {
                entity?.imageFileName?.let { path ->
                    try { java.io.File(path).delete() } catch (_: Exception) {}
                    // delete thumb
                    entity.extraJson?.let { extra ->
                        // naive parse to find "thumb":"path"
                        val thumbKey = "\"thumb\":"
                        val idx = extra.indexOf(thumbKey)
                        if (idx >= 0) {
                            val sub = extra.substring(idx + thumbKey.length).trimStart()
                            val end = sub.indexOf('"', 1)
                            if (end > 0) {
                                val thumbPath = sub.substring(1, end)
                                try { java.io.File(thumbPath).delete() } catch (_: Exception) {}
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            dao.deleteCreationById(id)
        }
    }

    fun getAICreationsFlow(): Flow<List<AICreationEntity>> {
        return AppDatabase.getDatabase(context).aiCreationDao().getAllCreations()
    }

    fun getAICreations(): List<AICreationEntity> {
        return runBlocking {
            AppDatabase.getDatabase(context).aiCreationDao().getAllCreations().first()
        }
    }

    /**
     * Mark an AI creation as published/done. Updates publishedAt and status.
     */
    suspend fun publishAICreation(id: String) {
        withContext(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(context).aiCreationDao()
            val ent = dao.getCreationById(id) ?: return@withContext
            val now = System.currentTimeMillis()
            val publishedAtVal = ent.publishedAt ?: now
            val updated = ent.copy(
                publishedAt = publishedAtVal,
                status = "DONE",
                updatedAt = now
            )
            dao.updateCreation(updated)
        }
    }

    /**
     * Convenience (non-suspending) wrapper for UI: triggers publish in background.
     */
    fun publishAICreationNow(id: String) {
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            try {
                publishAICreation(id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 新增：清除数据库中除API和密钥相关以外的所有用户数据（须在debug或受保护路径调用）
    suspend fun clearUserDataExceptApiKeys() {
        withContext(Dispatchers.IO) {
            try {
                // Delete all messages
                try {
                    // messages table: no deleteAll query, use conversation loop to delete by conversation
                    val convs = conversationDao.getAllConversations().first()
                    convs.forEach { conv ->
                        try { messageDao.deleteMessagesByConversationId(conv.id) } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}

                // Delete all conversations
                try {
                    val convs2 = conversationDao.getAllConversations().first()
                    convs2.forEach { conv -> conversationDao.deleteConversationById(conv.id) }
                } catch (_: Exception) {}

                // Delete all AI creations and remove their image files
                try {
                    val aiDao = database.aiCreationDao()
                    val creations = aiDao.getAllCreations().first()
                    creations.forEach { c ->
                        // delete image files if present
                        try { c.imageFileName?.let { java.io.File(it).delete() } } catch (_: Exception) {}
                        // also delete thumbnail if present
                        try {
                            c.imageFileName?.let {
                                val thumb = java.io.File(it).parentFile?.let { p ->
                                    java.io.File(p, "thumb_${java.io.File(it).nameWithoutExtension}.webp")
                                }
                                thumb?.delete()
                            }
                        } catch (_: Exception) {}
                        
                        try {
                            val aiDao = database.aiCreationDao()
                            aiDao.deleteCreationById(c.id)
                        } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}

                // Optionally remove other non-API prefs (keep api_key_*, service_provider and selected_model_* etc.)
                try {
                    val keysToKeepPrefixes = listOf("api_key_", "service_provider", "selected_model_", "custom_service_providers", "custom_provider_url_")
                    val allKeys = prefs.all.keys
                    val editor = prefs.edit()
                    allKeys.forEach { k ->
                        val keep = keysToKeepPrefixes.any { pref -> k.startsWith(pref) } || k == DARK_MODE_KEY || k == DEBUG_MODE_KEY || k == "user_name" || k == "user_signature" || k == "user_avatar"
                        if (!keep) {
                            editor.remove(k)
                        }
                    }
                    editor.apply()
                } catch (_: Exception) {}

                // Clear image storage directory used by ImageStorage
                try {
                    // we can attempt to delete external files dir subfolder used earlier
                    val dir = java.io.File(context.getExternalFilesDir(null), "ai_creations")
                    if (dir.exists() && dir.isDirectory) {
                        dir.listFiles()?.forEach { f ->
                            try {
                                if (f.isDirectory) {
                                    f.listFiles()?.forEach { it.delete() }
                                }
                                f.delete()
                            } catch (_: Exception) {}
                        }
                    }
                } catch (_: Exception) {}

            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
    }

    fun getLocalSelectedModel(): String {
        // 本地模型默认选择为 Qwen3-0.6B-MNN
        return prefs.getString("local_selected_model", "Qwen3-0.6B-MNN") ?: "Qwen3-0.6B-MNN"
    }

    fun setLocalSelectedModel(model: String) {
        prefs.edit().putString("local_selected_model", model).apply()
    }

}
