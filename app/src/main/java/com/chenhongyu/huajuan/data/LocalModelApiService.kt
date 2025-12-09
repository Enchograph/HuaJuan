package com.chenhongyu.huajuan.data

import com.chenhongyu.huajuan.network.Message
import com.chenhongyu.huajuan.stream.ChatEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

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

    override fun streamAIResponse(messages: List<Message>, modelInfo: ModelInfo): Flow<ChatEvent> = flow {
        // 简单模拟：先调用同步接口，然后按词/符号分块发出
        try {
            val full = runCatching { kotlinx.coroutines.runBlocking { getAIResponse(messages, modelInfo) } }
                .getOrElse { throwable ->
                    emit(ChatEvent.Error(throwable.message ?: throwable.javaClass.simpleName))
                    emit(ChatEvent.Done)
                    return@flow
                }

            if (full.startsWith("错误：")) {
                emit(ChatEvent.Error(full))
                emit(ChatEvent.Done)
                return@flow
            }

            val tokens = full.split(Regex("(?<=\\.|。|\n|\\s)"))
            for (t in tokens) {
                if (t.isNotBlank()) {
                    emit(ChatEvent.Chunk(t))
                    delay(40) // 模拟网络延迟
                }
            }
            emit(ChatEvent.Done)
        } catch (e: Exception) {
            emit(ChatEvent.Error(e.message ?: e.javaClass.simpleName))
            emit(ChatEvent.Done)
        }
    }
}