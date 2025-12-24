package com.chenhongyu.huajuan.network

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import java.io.*

/**
 * LLM会话包装类 - 与CPP实现的直接耦合
 */
class LlmSession(
    private val modelId: String,
    var sessionId: String,
    private val configPath: String,  // 这里应该是模型文件路径
    var savedHistory: List<ChatDataItem>? = null
) {
    private var nativePtr: Long = 0
    private var modelLoading = false
    private var generating = false
    private var releaseRequested = false
    private var keepHistory = true

    /**
     * 加载模型
     */
    fun load(context: Context) {
        // 首先检查模型路径是否是assets路径，如果是则需要复制到内部存储
        val actualModelPath = if (configPath.startsWith("assets/")) {
            extractModelFromAssets(context, configPath)
        } else {
            configPath
        }
        
        // 从模型文件路径提取模型目录路径
        val actualModelDir = actualModelPath.substringBeforeLast("/")
        
        Log.d(TAG, "开始加载模型: $actualModelDir, 实际路径: $actualModelPath")
        modelLoading = true

        // 转换历史消息为字符串列表
        val historyStringList = savedHistory?.map { it.text }?.filterNotNull()

        // 加载模型配置
        val configJson = loadModelConfig(actualModelPath)
        val extraConfig = createExtraConfig()

        // 调用JNI初始化方法，传递模型目录路径
        nativePtr = initNative(
            actualModelDir,  // 传递实际的模型目录路径
            historyStringList,
            extraConfig,
            configJson
        )

        modelLoading = false
        if (releaseRequested) {
            release()
        }

        Log.d(TAG, "模型加载完成，nativePtr: $nativePtr")
    }
    
    /**
     * 从assets目录提取模型文件到内部存储
     */
    private fun extractModelFromAssets(context: Context, assetsPath: String): String {
        try {
            // 从assets路径获取目录和文件名
            val assetDir = assetsPath.substringBeforeLast("/")
            val assetFileName = assetsPath.substringAfterLast("/")
            
            // 从assets路径中移除"assets/"前缀，因为context.getAssets().list()方法需要相对于assets根目录的路径
            val relativeAssetDir = if (assetDir.startsWith("assets/")) {
                assetDir.substringAfter("assets/")
            } else {
                assetDir
            }
            
            // 创建目标目录，保留完整的目录结构
            val modelDir = File(context.getDir("models", Context.MODE_PRIVATE), relativeAssetDir)
            if (!modelDir.exists()) {
                Log.d(TAG, "创建模型目录: ${modelDir.absolutePath}")
                modelDir.mkdirs()
            }

            // 递归复制整个模型目录中的所有文件
            copyAssetDir(context, relativeAssetDir, modelDir)

            // 返回模型文件的完整路径（.mnn文件）
            val modelFile = File(modelDir, assetFileName)
            
            // 验证模型文件是否存在且可读
            if (!modelFile.exists()) {
                Log.e(TAG, "模型文件不存在: ${modelFile.absolutePath}")
                throw FileNotFoundException("模型文件不存在: ${modelFile.absolutePath}")
            }
            
            if (!modelFile.canRead()) {
                Log.e(TAG, "模型文件不可读: ${modelFile.absolutePath}")
                throw IOException("模型文件不可读: ${modelFile.absolutePath}")
            }
            
            Log.d(TAG, "模型文件提取完成: ${modelFile.absolutePath}, 大小: ${modelFile.length()} bytes")
            return modelFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "从assets提取模型失败: $assetsPath", e)
            throw e
        }
    }
    
    /**
     * 递归复制assets目录到目标目录
     */
    private fun copyAssetDir(context: Context, assetDir: String, targetDir: File) {
        try {
            Log.d(TAG, "开始复制assets目录: $assetDir -> ${targetDir.absolutePath}")
            val assets = context.assets.list(assetDir) ?: run {
                Log.w(TAG, "assets目录为空或不存在: $assetDir")
                return
            }
            
            Log.d(TAG, "assets目录内容: ${assets.joinToString(", ")}")
            
            for (fileName in assets) {
                val assetPath = "$assetDir/$fileName"
                
                // 检查是否是子目录 - 改进的判断逻辑
                val isDirectory = try {
                    val subAssets = context.assets.list(assetPath)
                    // 如果能够列出子项目，说明是目录
                    subAssets != null && subAssets.isNotEmpty()
                } catch (e: Exception) {
                    // 如果list()抛出异常，说明是文件
                    false
                }
                
                if (isDirectory) {
                    // 是子目录，递归复制
                    val subDir = File(targetDir, fileName)
                    if (!subDir.exists()) {
                        Log.d(TAG, "创建子目录: ${subDir.absolutePath}")
                        subDir.mkdirs()
                    }
                    copyAssetDir(context, assetPath, subDir)
                } else {
                    // 是文件，直接复制
                    val destFile = File(targetDir, fileName)
                    
                    // 检查目标文件是否已存在且大小相同，避免重复复制
                    if (!destFile.exists() || destFile.length() == 0L) {
                        Log.d(TAG, "正在复制模型文件: $assetPath -> ${destFile.absolutePath}")
                        context.assets.open(assetPath).use { inputStream ->
                            FileOutputStream(destFile).use { outputStream ->
                                // 使用缓冲区进行复制，而不是依赖available()方法
                                val buffer = ByteArray(8192) // 8KB缓冲区
                                var totalBytes: Long = 0
                                var bytesRead: Int
                                
                                // 使用缓冲区读取，直到文件结束
                                while (true) {
                                    bytesRead = inputStream.read(buffer)
                                    if (bytesRead == -1) break
                                    
                                    outputStream.write(buffer, 0, bytesRead)
                                    totalBytes += bytesRead
                                }
                                
                                outputStream.flush()
                                Log.d(TAG, "文件复制完成: ${destFile.absolutePath}, 大小: ${destFile.length()} bytes, copied: $totalBytes bytes")
                                
                                // 验证复制的字节数与目标文件大小是否一致
                                if (totalBytes != destFile.length()) {
                                    Log.e(TAG, "文件复制不完整: expected=$totalBytes, actual=${destFile.length()}")
                                    throw IOException("文件复制不完整: $fileName")
                                }
                                
                                // 额外验证：重新打开文件检查大小
                                val verifiedSize = destFile.length()
                                Log.d(TAG, "验证文件大小: ${destFile.absolutePath}, size: $verifiedSize bytes")
                                
                                // 如果是.mnn或权重文件，特别验证
                                if (fileName.endsWith(".mnn") || fileName.contains(".mnn.weight")) {
                                    Log.d(TAG, "复制了关键模型文件: $fileName, size: $verifiedSize bytes")
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "模型文件已存在，跳过: ${destFile.absolutePath}, 大小: ${destFile.length()} bytes")
                        
                        // 验证已存在的文件是否完整 - 仅对.mnn或权重文件进行验证
                        if (fileName.endsWith(".mnn") || fileName.contains(".mnn.weight")) {
                            // 通过复制到内存流来检查原始文件大小（不实际保存到内存，只计算大小）
                            val originalSize = context.assets.open(assetPath).use { inputStream ->
                                val buffer = ByteArray(8192)
                                var total: Long = 0
                                var bytesRead: Int
                                while (true) {
                                    bytesRead = inputStream.read(buffer)
                                    if (bytesRead == -1) break
                                    total += bytesRead
                                }
                                total
                            }
                            
                            if (destFile.length() != originalSize) {
                                Log.e(TAG, "已存在文件大小不匹配: expected=$originalSize, actual=${destFile.length()}")
                                destFile.delete() // 删除不完整文件，重新复制
                                Log.d(TAG, "删除不完整文件，将重新复制: ${destFile.absolutePath}")
                                
                                // 重新复制文件
                                context.assets.open(assetPath).use { inputStream ->
                                    FileOutputStream(destFile).use { outputStream ->
                                        val buffer = ByteArray(8192) // 8KB缓冲区
                                        var totalBytes: Long = 0
                                        var bytesRead: Int
                                        
                                        while (true) {
                                            bytesRead = inputStream.read(buffer)
                                            if (bytesRead == -1) break
                                            
                                            outputStream.write(buffer, 0, bytesRead)
                                            totalBytes += bytesRead
                                        }
                                        
                                        outputStream.flush()
                                        Log.d(TAG, "重新复制完成: ${destFile.absolutePath}, 大小: ${destFile.length()} bytes, copied: $totalBytes bytes")
                                        
                                        if (totalBytes != destFile.length()) {
                                            Log.e(TAG, "重新复制仍然不完整: expected=$totalBytes, actual=${destFile.length()}")
                                            throw IOException("重新复制仍然不完整: $fileName")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Log.d(TAG, "完成复制assets目录: $assetDir")
        } catch (e: Exception) {
            Log.e(TAG, "复制assets目录失败: $assetDir", e)
            throw e
        }
    }

    /**
     * 加载模型 (向后兼容)
     */
    fun load() {
        // 为了向后兼容，这里需要一个Context，但暂时抛出异常
        throw IllegalStateException("使用带Context参数的load方法来加载assets中的模型")
    }

    /**
     * 生成响应
     */
    fun generate(
        prompt: String,
        params: Map<String, Any>,
        progressListener: GenerateProgressListener
    ): HashMap<String, Any> {
        Log.d(TAG, "开始生成响应: $prompt")
        synchronized(this) {
            generating = true
            val result = submitNative(nativePtr, prompt, keepHistory, progressListener)
            generating = false
            if (releaseRequested) {
                release()
            }
            return result
        }
    }

    /**
     * 重置会话
     */
    fun reset(): String {
        synchronized(this) {
            resetNative(nativePtr)
            sessionId = System.currentTimeMillis().toString()
            return sessionId
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        synchronized(this) {
            Log.d(TAG, "释放LLM会话资源: nativePtr=$nativePtr, generating=$generating")
            if (!generating && !modelLoading) {
                releaseInner()
            } else {
                releaseRequested = true
                while (generating || modelLoading) {
                    try {
                        (this as Object).wait()
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        Log.e(TAG, "释放等待被中断", e)
                    }
                }
                releaseInner()
            }
        }
    }

    private fun releaseInner() {
        if (nativePtr != 0L) {
            releaseNative(nativePtr)
            nativePtr = 0
            (this as Object).notifyAll()
        }
    }

    // JNI方法声明
    private external fun initNative(
        configPath: String?,  // 模型目录路径
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

    private external fun resetNative(instanceId: Long)
    private external fun releaseNative(instanceId: Long)

    /**
     * 创建额外配置
     */
    private fun createExtraConfig(): String {
        val extraConfig = mapOf(
            "is_r1" to false,
            "mmap_dir" to getMmapDir(modelId),
            "keep_history" to keepHistory
        )
        return Gson().toJson(extraConfig)
    }

    /**
     * 加载模型配置
     */
    private fun loadModelConfig(configPath: String): String {
        // 对于MNN模型，configPath是模型文件路径
        // 这里返回基本配置，实际配置由JNI层处理
        val lastSlashIndex = configPath.lastIndexOf('/')
        val modelDir = if (lastSlashIndex != -1) {
            configPath.substring(0, lastSlashIndex)
        } else {
            configPath
        }
        val config = mapOf(
            "model_path" to modelDir,
            "use_mmap" to false
        )
        return Gson().toJson(config)
    }

    /**
     * 获取MMap目录
     */
    private fun getMmapDir(modelId: String): String {
        // 返回应用内部存储的缓存路径
        // 注意：这里需要通过应用上下文获取正确的缓存路径
        // 由于没有上下文，暂时返回空字符串，表示不使用mmap
        return ""  // 不使用mmap功能，避免路径问题
    }

    companion object {
        private const val TAG = "LlmSession"

        init {
            System.loadLibrary("mnnllmapp")  // 加载C++库
        }
    }
}

/**
 * 聊天数据项类，用于存储对话历史
 */
data class ChatDataItem(
    val role: String,
    val text: String
)