package com.chenhongyu.huajuan.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.chenhongyu.huajuan.network.Message
import com.chenhongyu.huajuan.network.OpenAiRequest
import com.chenhongyu.huajuan.network.OpenAiApiService
import com.google.gson.JsonParser
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class Repository(private val context: Context) {
    private val appPreferences = AppPreferences(context)
    
    companion object {
        private const val TAG = "Repository"
    }

    fun getUseCloudModel(): Boolean = appPreferences.useCloudModel
    fun getServiceProvider(): String = if (appPreferences.serviceProvider.isEmpty()) "OpenAI" else appPreferences.serviceProvider
    fun getCustomApiUrl(): String = appPreferences.customApiUrl
    fun getApiKey(): String = appPreferences.apiKey
    fun getSelectedModel(): String = if (appPreferences.selectedModel.isEmpty()) "GPT-3.5 Turbo" else appPreferences.selectedModel
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
    
    // 流式调用AI API获取回复
    fun streamAIResponse(prompt: String): Flow<String> = flow {
        // 检查网络连接
        if (!isNetworkAvailable()) {
            emit("错误：无网络连接，请检查网络设置")
            return@flow
        }

        if (!appPreferences.useCloudModel) {
            // 如果不使用云模型，则返回本地模拟回复
            val response = "这是本地模型的回复: $prompt"
            for (i in response.indices) {
                emit(response[i].toString())
                kotlinx.coroutines.delay(10) // 模拟打字效果
            }
            return@flow
        }

        val apiKey = appPreferences.apiKey
        if (apiKey.isEmpty()) {
            emit("错误：API密钥为空，请在设置中配置API密钥")
            return@flow
        }

        try {
            val service = getApiService()
            val request = OpenAiRequest(
                model = appPreferences.selectedModel,
                messages = listOf(Message("user", prompt)),
                stream = true
            )
            
            val response = service.streamChatCompletion(
                authorization = "Bearer $apiKey",
                request = request
            )

            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    processStreamResponse(responseBody, this)
                } else {
                    emit("无法从AI获取有效回复")
                }
            } else {
                emit("API调用失败: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling AI API", e)
            emit("请求异常: ${e.message}")
        }
    }
    
    // 处理流式响应
    private suspend fun processStreamResponse(responseBody: ResponseBody, flow: kotlinx.coroutines.flow.FlowCollector<String>) {
        BufferedReader(InputStreamReader(responseBody.byteStream())).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let {
                    if (it.startsWith("data: ")) {
                        val data = it.substring(6) // 移除 "data: " 前缀
                        if (data != "[DONE]") {
                            try {
                                // 解析JSON数据以提取内容
                                val content = extractContentFromSSEData(data)
                                if (content != null) {
                                    flow.emit(content)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing SSE data: $data", e)
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 从SSE数据中提取内容
    private fun extractContentFromSSEData(data: String): String? {
        try {
            val jsonObject: JsonObject = JsonParser().parse(data).asJsonObject
            val choices: JsonArray = jsonObject.getAsJsonArray("choices")
            if (choices.size() > 0) {
                val choice: JsonObject = choices[0].asJsonObject
                val delta: JsonObject = choice.getAsJsonObject("delta")
                if (delta.has("content")) {
                    val content: JsonElement = delta.get("content")
                    return content.asString
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting content from SSE data: $data", e)
        }
        return null
    }
}