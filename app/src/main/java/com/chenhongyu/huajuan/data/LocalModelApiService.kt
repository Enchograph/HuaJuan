package com.chenhongyu.huajuan.data

import com.chenhongyu.huajuan.network.Message

/**
 * 本地模型API服务实现
 * TODO: 需要根据实际的本地模型调用方式进行实现
 */
class LocalModelApiService(private val repository: Repository) : ModelApiService {
    
    override fun isAvailable(): Boolean {
        // TODO: 检查本地模型是否可用
        // 例如检查模型文件是否存在、初始化是否完成等
        return false
    }
    
    override suspend fun getAIResponse(messages: List<Message>, modelInfo: ModelInfo): String {
        // TODO: 实现本地模型调用逻辑
        return "这是来自本地模型的示例响应。请根据实际的本地模型调用方式实现此方法。"
    }
}