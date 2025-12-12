package com.chenhongyu.huajuan.data

import com.chenhongyu.huajuan.network.Message
import com.chenhongyu.huajuan.stream.ChatEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 本地模型API服务实现（代理到在线模型）
 * 将本地模型名称映射到“应用试用”服务商的云端模型，并增加调用延迟。
 */
class LocalModelApiService(private val repository: Repository) : ModelApiService {
    
    override fun isAvailable(): Boolean {
        // 本地模型改为代理网络调用，只要网络和密钥可用就认为可用
        return true
    }
    
    override suspend fun getAIResponse(messages: List<Message>, modelInfo: ModelInfo): String {
        // 将显示名映射到云端模型代号
        val mappedApiCode = when (modelInfo.displayName) {
            "Qwen3-0.6B-MNN" -> "mistral-7b-instruct"
            "MobileLLM-125M-MNN" -> "qwen-72b"
            else -> modelInfo.apiCode
        }
        // 调用前增加少许延迟
        //delay(1000)

        // 暂时切换服务商到“应用试用”，调用后恢复
        val prevProvider = repository.getServiceProvider()
        try {
            repository.setServiceProvider("应用试用")
            val online = OnlineModelApiService(repository)
            // 使用映射后的apiCode进行调用
            return online.getAIResponse(messages, ModelInfo(modelInfo.displayName, mappedApiCode))
        } finally {
            repository.setServiceProvider(prevProvider)
        }
    }

    override fun streamAIResponse(messages: List<Message>, modelInfo: ModelInfo): Flow<ChatEvent> = flow {
        // 将显示名映射到云端模型代号
        val mappedApiCode = when (modelInfo.displayName) {
            "Qwen3-0.6B-MNN" -> "qwen-72b"
            "MobileLLM-125M-MNN" -> "mistral-7b-instruct"
            else -> modelInfo.apiCode
        }
        // 调用前增加少许延迟
        //delay(1000)

        val prevProvider = repository.getServiceProvider()
        try {
            repository.setServiceProvider("应用试用")
            val online = OnlineModelApiService(repository)
            // 委托在线流式实现
            online.streamAIResponse(messages, ModelInfo(modelInfo.displayName, mappedApiCode)).collect { event ->
                emit(event)
            }
        } finally {
            repository.setServiceProvider(prevProvider)
        }
    }
}