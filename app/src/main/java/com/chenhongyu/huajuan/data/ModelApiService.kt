package com.chenhongyu.huajuan.data

import com.chenhongyu.huajuan.network.Message
import com.chenhongyu.huajuan.stream.ChatEvent
import kotlinx.coroutines.flow.Flow

/**
 * 统一的模型API服务接口
 * 用于抽象本地模型和在线模型的调用
 */
interface ModelApiService {
    /**
     * 获取AI响应
     * @param messages 消息历史
     * @param modelInfo 模型信息
     * @return AI响应文本
     */
    suspend fun getAIResponse(messages: List<Message>, modelInfo: ModelInfo): String

    /**
     * 流式获取AI响应，发出Chunk/Error/Done事件
     * 实现者应在IO线程执行网络/本地模型调用
     */
    fun streamAIResponse(messages: List<Message>, modelInfo: ModelInfo): Flow<ChatEvent>

    /**
     * 检查服务是否可用
     * @return 是否可用
     */
    fun isAvailable(): Boolean
}