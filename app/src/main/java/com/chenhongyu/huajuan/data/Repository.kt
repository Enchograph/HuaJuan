package com.chenhongyu.huajuan.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.chenhongyu.huajuan.network.Message
import com.chenhongyu.huajuan.network.OpenAiRequest
import com.chenhongyu.huajuan.network.OpenAiApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class Repository(private val context: Context) {
    private val appPreferences = AppPreferences(context)
    
    companion object {
        private const val TAG = "Repository"
    }

    fun getUseCloudModel(): Boolean = appPreferences.useCloudModel
    fun getServiceProvider(): String = appPreferences.serviceProvider
    fun getCustomApiUrl(): String = appPreferences.customApiUrl
    fun getApiKey(): String = appPreferences.apiKey
    fun getSelectedModel(): String = appPreferences.selectedModel
    fun getDarkMode(): Boolean = appPreferences.darkMode

    fun setUseCloudModel(value: Boolean) { appPreferences.useCloudModel = value }
    fun setServiceProvider(value: String) { appPreferences.serviceProvider = value }
    fun setCustomApiUrl(value: String) { appPreferences.customApiUrl = value }
    fun setApiKey(value: String) { appPreferences.apiKey = value }
    fun setSelectedModel(value: String) { appPreferences.selectedModel = value }
    fun setDarkMode(value: Boolean) { appPreferences.darkMode = value }

    // 检查网络连接
    private fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                val isValidated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
                
                Log.d(TAG, "Network capabilities - Internet: $hasInternet, Validated: $isValidated")
                return hasInternet
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                @Suppress("DEPRECATION")
                val isConnected = networkInfo?.isConnected == true
                Log.d(TAG, "Network connected (deprecated API): $isConnected")
                return isConnected
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network availability", e)
            false
        }
    }

    // 获取API服务实例
    fun getApiService(): OpenAiApiService {
        val baseUrl = when (appPreferences.serviceProvider) {
            "OpenAI" -> "https://api.openai.com/"
            "Azure" -> "https://azure-openai-api.example.com/" // 示例URL，需替换为实际的Azure端点
            "Anthropic" -> "https://api.anthropic.com/" // 示例URL，需替换为实际的Anthropic端点
            "自定义" -> if (appPreferences.customApiUrl.isNotEmpty()) appPreferences.customApiUrl else "https://api.openai.com/"
            else -> "https://api.openai.com/"
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(OpenAiApiService::class.java)
    }

    // 调用AI API获取回复
    suspend fun getAIResponse(prompt: String): String {
        // 检查网络连接
        if (!isNetworkAvailable()) {
            return "错误：无网络连接，请检查网络设置"
        }

        if (!appPreferences.useCloudModel) {
            // 如果不使用云模型，则返回本地模拟回复
            return "这是本地模型的回复: $prompt"
        }

        val apiKey = appPreferences.apiKey
        if (apiKey.isEmpty()) {
            return "错误：API密钥为空，请在设置中配置API密钥"
        }

        return try {
            val service = getApiService()
            val request = OpenAiRequest(
                model = appPreferences.selectedModel,
                messages = listOf(Message("user", prompt))
            )
            
            val response = service.getChatCompletion(
                authorization = "Bearer $apiKey",
                request = request
            )

            if (response.isSuccessful) {
                response.body()?.choices?.firstOrNull()?.message?.content
                    ?: "无法从AI获取有效回复"
            } else {
                "API调用失败: ${response.code()} ${response.message()}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling AI API", e)
            "请求异常: ${e.message}"
        }
    }
}