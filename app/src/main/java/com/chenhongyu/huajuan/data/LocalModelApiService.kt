package com.chenhongyu.huajuan.data

import android.content.Context
import com.chenhongyu.huajuan.network.Message
import com.chenhongyu.huajuan.stream.ChatEvent
import com.chenhongyu.huajuan.network.LlmSession
import com.chenhongyu.huajuan.network.ChatDataItem
import com.chenhongyu.huajuan.network.GenerateProgressListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.trySendBlocking

/**
 * 本地模型API服务实现
 * 根据所选本地模型调用打包在应用中的模型进行推理。
 */
class LocalModelApiService(private val repository: Repository) : ModelApiService {

    private var llmSession: LlmSession? = null

    override fun isAvailable(): Boolean {
        // 检查本地模型环境是否可用
        return true  // 简化实现，实际中应检查模型文件是否存在
    }

    override suspend fun getAIResponse(messages: List<Message>, modelInfo: ModelInfo): String {
        try {
            // 初始化LLM会话（如果尚未初始化） - 在IO线程中执行
            if (llmSession == null) {
                withContext(Dispatchers.IO) {
                    val modelPath = modelInfo.modelPath.takeIf { it.isNotEmpty() } ?: getLocalModelPath(modelInfo.displayName)
                    println("DEBUG: 初始化本地模型，模型名称=${modelInfo.displayName}, 模型路径=$modelPath")
                    llmSession = LlmSession(
                        modelId = modelInfo.apiCode,
                        sessionId = System.currentTimeMillis().toString(),
                        configPath = modelPath,
                        savedHistory = null
                    )
                    // 从Repository获取Context并加载模型
                    val context = getContextFromRepository()
                    try {
                        llmSession?.load(context) // 加载模型
                        println("DEBUG: 本地模型加载完成")
                    } catch (e: Exception) {
                        println("ERROR: 本地模型加载失败: ${e.message}")
                        e.printStackTrace()
                        throw e
                    }
                }
            }

            // 构建提示词，将消息历史转换为字符串
            val prompt = buildPromptFromMessages(messages)
            
            // 调用CPP LLM会话生成响应
            val result = llmSession?.generate(prompt, emptyMap(), object : GenerateProgressListener {
                override fun onProgress(progress: String?): Boolean {
                    // 进度回调处理（可选）
                    return false // 不中断生成
                }
            })

            // 提取响应文本
            return result?.get("response") as? String ?: "生成失败"
        } catch (e: Exception) {
            e.printStackTrace()
            println("ERROR: 本地模型AI响应失败: ${e.message}")
            return "错误：${e.message}"
        }
    }

    override fun streamAIResponse(messages: List<Message>, modelInfo: ModelInfo): Flow<ChatEvent> = callbackFlow {
        // 在IO线程中初始化LLM会话（如果尚未初始化）
        if (llmSession == null) {
            withContext(Dispatchers.IO) {
                val modelPath = modelInfo.modelPath.takeIf { it.isNotEmpty() } ?: getLocalModelPath(modelInfo.displayName)
                println("DEBUG: 初始化本地模型（流式），模型名称=${modelInfo.displayName}, 模型路径=$modelPath")
                llmSession = LlmSession(
                    modelId = modelInfo.apiCode,
                    sessionId = System.currentTimeMillis().toString(),
                    configPath = modelPath,
                    savedHistory = null
                )
                // 从Repository获取Context并加载模型
                val context = getContextFromRepository()
                try {
                    llmSession?.load(context)
                    println("DEBUG: 本地模型（流式）加载完成")
                } catch (e: Exception) {
                    println("ERROR: 本地模型（流式）加载失败: ${e.message}")
                    e.printStackTrace()
                    trySendBlocking(ChatEvent.Error("错误：本地模型加载失败 ${e.message}"))
                    close()
                    return@withContext
                }
            }
        }

        val prompt = buildPromptFromMessages(messages)

        val progressListener = object : GenerateProgressListener {
            override fun onProgress(progress: String?): Boolean {
                if (progress != null && progress != "<eop>") {
                    try {
                        // 发送进度到流
                        trySendBlocking(ChatEvent.Chunk(progress))
                    } catch (e: Exception) {
                        println("Warning: Could not send progress chunk: ${e.message}")
                    }
                }
                return false // 不中断生成
            }
        }

        try {
            // 在IO线程中执行生成
            withContext(Dispatchers.IO) {
                llmSession?.generate(prompt, emptyMap(), progressListener)
            }
            // 发送完成事件
            trySendBlocking(ChatEvent.Done)
        } catch (e: Exception) {
            e.printStackTrace()
            println("ERROR: 本地模型流式AI响应失败: ${e.message}")
            trySendBlocking(ChatEvent.Error("错误：${e.message}"))
        } finally {
            // 关闭通道
            close()
        }
    }
    
    /**
     * 释放LLM会话资源
     */
    fun release() {
        llmSession?.release()
        llmSession = null
    }
    
    /**
     * 从消息列表构建提示词
     */
    private fun buildPromptFromMessages(messages: List<Message>): String {
        return messages.joinToString("\n") { message ->
            "${message.role}: ${message.content}"
        }
    }
    
    /**
     * 根据模型名称获取本地模型路径
     */
    private fun getLocalModelPath(modelName: String): String {
        // 从assets目录获取模型文件
        return repository.getLocalModelPath(modelName)
    }
    
    /**
     * 从Repository获取Context
     */
    private fun getContextFromRepository(): Context {
        return repository.getContext()
    }
}