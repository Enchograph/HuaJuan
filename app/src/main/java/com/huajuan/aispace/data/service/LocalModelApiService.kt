package com.huajuan.aispace.data

import android.content.Context
import android.util.Log
import com.huajuan.aispace.R
import com.huajuan.aispace.network.Message
import com.huajuan.aispace.data.model.ChatEvent
import com.huajuan.aispace.network.LlmSession
import com.huajuan.aispace.network.GenerateProgressListener
import com.huajuan.aispace.utils.debugLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 本地模型API服务实现
 * 根据所选本地模型调用打包在应用中的模型进行推理。
 */
class LocalModelApiService(private val repository: Repository) : ModelApiService {
    private companion object {
        const val TAG = "LocalModelApiService"
    }

    private var llmSession: LlmSession? = null

    override fun isAvailable(): Boolean {
        // 检查本地模型环境是否可用
        return true  // 简化实现，实际中应检查模型文件是否存在
    }

    override suspend fun getAIResponse(
        messages: List<Message>,
        modelInfo: ModelInfo,
        debugScopeId: String?
    ): String {
        try {
            // 初始化LLM会话（如果尚未初始化） - 在IO线程中执行
            if (llmSession == null) {
                withContext(Dispatchers.IO) {
                    val modelPath = modelInfo.modelPath.takeIf { it.isNotEmpty() } ?: getLocalModelPath(modelInfo.displayName)
                    debugLog(TAG) { "初始化本地模型，名称=${modelInfo.displayName}, 路径=$modelPath" }
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
                        debugLog(TAG) { "本地模型加载完成" }
                    } catch (e: Exception) {
                        Log.e(TAG, "本地模型加载失败", e)
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
            return result?.get("response") as? String
                ?: ModelErrorParser.normalizeUserError(repository.getContext().getString(R.string.error_no_content))
        } catch (e: Exception) {
            Log.e(TAG, "本地模型AI响应失败", e)
            return ModelErrorParser.parseThrowable(e)
        }
    }

    override fun streamAIResponse(
        messages: List<Message>,
        modelInfo: ModelInfo,
        debugScopeId: String?
    ): Flow<ChatEvent> = callbackFlow {
        val cancelRequested = AtomicBoolean(false)
        // 在IO线程中初始化LLM会话（如果尚未初始化）
        if (llmSession == null) {
            withContext(Dispatchers.IO) {
                val modelPath = modelInfo.modelPath.takeIf { it.isNotEmpty() } ?: getLocalModelPath(modelInfo.displayName)
                debugLog(TAG) { "初始化本地模型（流式），名称=${modelInfo.displayName}, 路径=$modelPath" }
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
                    debugLog(TAG) { "本地模型（流式）加载完成" }
                } catch (e: Exception) {
                    Log.e(TAG, "本地模型（流式）加载失败", e)
                    trySendBlocking(
                        ChatEvent.Error(
                            ModelErrorParser.normalizeUserError(
                                repository.getContext().getString(
                                    R.string.error_local_model_load_failed,
                                    e.message.orEmpty()
                                )
                            )
                        )
                    )
                    close()
                    return@withContext
                }
            }
        }

        val prompt = buildPromptFromMessages(messages)
        var emittedTextSoFar = ""
        var streamMode = 0 // 0 unknown, 1 incremental, 2 cumulative

        val progressListener = object : GenerateProgressListener {
            override fun onProgress(progress: String?): Boolean {
                if (cancelRequested.get()) {
                    return true
                }
                if (progress != null && progress != "<eop>") {
                    try {
                        val delta = when {
                            emittedTextSoFar.isEmpty() -> {
                                progress
                            }
                            streamMode == 2 -> {
                                if (progress.startsWith(emittedTextSoFar)) {
                                    progress.removePrefix(emittedTextSoFar)
                                } else {
                                    ""
                                }
                            }
                            streamMode == 1 -> {
                                progress
                            }
                            progress.startsWith(emittedTextSoFar) -> {
                                streamMode = 2
                                progress.removePrefix(emittedTextSoFar)
                            }
                            else -> {
                                streamMode = 1
                                progress
                            }
                        }
                        if (delta.isNotEmpty()) {
                            emittedTextSoFar += delta
                            trySendBlocking(ChatEvent.Chunk(delta))
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not send progress chunk: ${e.message}")
                    }
                }
                return cancelRequested.get()
            }
        }

        val generationJob = launch {
            try {
                withContext(Dispatchers.IO) {
                    llmSession?.generate(prompt, emptyMap(), progressListener)
                }
                if (!cancelRequested.get()) {
                    trySendBlocking(ChatEvent.Done)
                }
            } catch (e: Exception) {
                if (!cancelRequested.get()) {
                    Log.e(TAG, "本地模型流式AI响应失败", e)
                    trySendBlocking(ChatEvent.Error(ModelErrorParser.parseThrowable(e)))
                }
            }
        }

        awaitClose {
            cancelRequested.set(true)
            generationJob.cancel()
        }
    }

    override suspend fun getEmbeddings(inputs: List<String>, modelInfo: ModelInfo): List<List<Float>> {
        throw UnsupportedOperationException("Local model embeddings are not supported")
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
