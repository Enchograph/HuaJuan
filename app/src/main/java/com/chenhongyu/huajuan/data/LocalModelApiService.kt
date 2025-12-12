package com.chenhongyu.huajuan.data

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.chenhongyu.huajuan.network.Message
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import android.util.Pair
import com.google.gson.Gson
import java.util.HashMap
import java.util.stream.Collectors

/**
 * 本地模型API服务实现
 */
class LocalModelApiService(private val repository: Repository) : ModelApiService {
    companion object {
        private const val TAG = "LocalModelApiService"
        private const val MODEL_PATH = "model/notQwen3/config.json"
        
        init {
            System.loadLibrary("mnnllmapp")
        }
    }

    private var nativePtr: Long = 0
    private var modelLoading = false
    private var generating = false
    private var releaseRequested = false
    private var sessionId: String = ""
    private var keepHistory = false

    override fun isAvailable(): Boolean {
        // 检查assets目录中是否存在模型文件
        return try {
            val context = repository.getContext()
            val assetManager = context.assets
            assetManager.open(MODEL_PATH).use { 
                Log.d(TAG, "Model file found in assets: $MODEL_PATH")
                true
            }
        } catch (e: Exception) {
            Log.d(TAG, "Model file not found in assets: $MODEL_PATH", e)
            false
        }
    }
    
    override suspend fun getAIResponse(messages: List<Message>, modelInfo: ModelInfo): String {
        return try {
            // 初始化模型会话（如果尚未初始化）
            if (nativePtr == 0L) {
                initializeModelSession()
            }
            
            // 等待模型加载完成
            if (!modelLoading) {
                loadModel()
            }
            
            // 获取最新的用户消息作为提示
            val latestUserMessage = messages.lastOrNull { it.role == "user" }?.content ?: ""
            if (latestUserMessage.isEmpty()) {
                return "没有找到用户输入消息"
            }
            
            // 生成回复
            generateResponse(latestUserMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting AI response from local model", e)
            "本地模型调用出错: ${e.message ?: e.javaClass.simpleName}"
        }
    }
    
    private suspend fun initializeModelSession() {
        // 获取应用内部存储中的模型路径
        val context = repository.getContext()
        val modelPath = File(context.filesDir, MODEL_PATH).absolutePath
        sessionId = System.currentTimeMillis().toString()
        
        // 加载模型配置
        val configFile = File(modelPath)
        val configParent = configFile.parent
        val configFileName = configFile.name
        
        nativePtr = suspendCancellableCoroutine { continuation ->
            try {
                val ptr = initNative(
                    modelPath,
                    null, // history
                    "{}", // mergedConfigStr
                    Gson().toJson(
                        HashMap<String, Any>().apply {
                            put("is_r1", false)
                            put("mmap_dir", "")
                            put("keep_history", keepHistory)
                        }
                    ) // configJsonStr
                )
                continuation.resume(ptr)
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }
    
    private suspend fun loadModel() {
        if (nativePtr == 0L) throw IllegalStateException("模型会话未初始化")
        
        suspendCancellableCoroutine<Unit> { continuation ->
            Thread {
                try {
                    modelLoading = true
                    // 模型已经在initializeModelSession中通过initNative加载
                    modelLoading = false
                    
                    if (releaseRequested) {
                        releaseInner()
                    }
                    continuation.resume(Unit)
                } catch (e: Exception) {
                    modelLoading = false
                    continuation.resumeWithException(e)
                }
            }.start()
        }
    }
    
    private suspend fun generateResponse(prompt: String): String {
        if (nativePtr == 0L) throw IllegalStateException("模型会话未初始化")
        
        return suspendCancellableCoroutine { continuation ->
            val responseBuilder = StringBuilder()
            
            Thread {
                try {
                    val params = HashMap<String, Any>()
                    val result = submitNative(
                        nativePtr,
                        prompt,
                        keepHistory,
                        object : GenerateProgressListener {
                            override fun onProgress(progress: String?): Boolean {
                                if (!progress.isNullOrEmpty()) {
                                    responseBuilder.append(progress)
                                }
                                // 返回false表示继续生成
                                return false
                            }
                        }
                    )
                    
                    continuation.resume(responseBuilder.toString())
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }.start()
        }
    }
    
    private fun releaseInner() {
        if (nativePtr != 0L) {
            releaseNative(nativePtr)
            nativePtr = 0
        }
    }
    
    fun release() {
        synchronized(this) {
            Log.d(
                TAG,
                "MNN_DEBUG release nativePtr: $nativePtr mGenerating: $generating"
            )
            if (!generating && !modelLoading) {
                releaseInner()
            } else {
                releaseRequested = true
                while (generating || modelLoading) {
                    try {
                        (this as Object).wait()
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        Log.e(TAG, "Thread interrupted while waiting for release", e)
                    }
                }
                releaseInner()
            }
        }
    }
    
    private external fun initNative(
        configPath: String?,
        history: List<String>?,
        mergedConfigStr: String?,
        configJsonStr: String?
    ): Long

    private external fun submitNative(
        instanceId: Long,
        input: String,
        keepHistory: Boolean,
        listener: GenerateProgressListener
    ): HashMap<String, Any>

    private external fun releaseNative(instanceId: Long)
    
    interface GenerateProgressListener {
        fun onProgress(progress: String?): Boolean
    }
}