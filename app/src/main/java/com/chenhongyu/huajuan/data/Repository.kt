package com.chenhongyu.huajuan.data

import android.content.Context

class Repository(private val context: Context) {
    // 存储暗色模式设置的键名
    private val DARK_MODE_KEY = "dark_mode"
    
    // SharedPreferences的名字
    private val PREFS_NAME = "hua_juan_prefs"
    
    // 获取SharedPreferences实例
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
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
    
    // 模拟AI API调用
    suspend fun streamAIResponse(prompt: String): kotlinx.coroutines.flow.Flow<String> {
        return kotlinx.coroutines.flow.flow {
            // 模拟流式响应
            val response = "这是模拟的AI回复: $prompt"
            response.forEach { char ->
                emit(char.toString())
                kotlinx.coroutines.delay(50) // 模拟网络延迟
            }
        }
    }
    
    suspend fun getAIResponse(prompt: String): String {
        kotlinx.coroutines.delay(1000) // 模拟网络延迟
        return "这是模拟的AI回复: $prompt"
    }
}