package com.huajuan.aispace.data

import com.huajuan.aispace.network.Message
import com.huajuan.aispace.data.model.ChatEvent
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
    suspend fun getAIResponse(
        messages: List<Message>,
        modelInfo: ModelInfo,
        debugScopeId: String? = null
    ): String

    /**
     * 流式获取AI响应，发出Chunk/Error/Done事件
     * 实现者应在IO线程执行网络/本地模型调用
     */
    fun streamAIResponse(
        messages: List<Message>,
        modelInfo: ModelInfo,
        debugScopeId: String? = null
    ): Flow<ChatEvent>

    /**
     * 获取文本向量
     * @param inputs 待向量化文本列表
     * @param modelInfo 向量模型信息
     * @return 与输入一一对应的向量列表
     */
    suspend fun getEmbeddings(inputs: List<String>, modelInfo: ModelInfo): List<List<Float>>

    /**
     * 检查服务是否可用
     * @return 是否可用
     */
    fun isAvailable(): Boolean
}
