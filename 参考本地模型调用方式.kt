package com.alibaba.mnnllm.simple

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.alibaba.mls.api.download.ModelDownloadManager
import com.alibaba.mnnllm.android.llm.LlmSession
import com.alibaba.mnnllm.android.llm.GenerateProgressListener
import timber.log.Timber
import java.io.File

class SimpleChatActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val TAG = "SimpleChatActivity"
    }

    private lateinit var inputEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var outputTextView: TextView

    // LLM会话实例
    private var llmSession: LlmSession? = null
    private var isModelLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        setContentView(com.alibaba.mnnllm.android.R.layout.activity_simple_chat)

        // 初始化UI组件
        initViews()

        // 检查并请求存储权限
        checkPermissions()
    }

    private fun checkPermissions() {
        Log.d(TAG, "checkPermissions called")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d(TAG, "Android version >= M, checking permissions")
            val permissions = mutableListOf<String>()

            // 对于 Android 10 及以上版本，使用更合适的权限检查
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11 及以上版本推荐使用 MANAGE_EXTERNAL_STORAGE 权限来访问外部存储
                // 但我们仍需要检查传统的存储权限以确保兼容性
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    Log.d(TAG, "READ_EXTERNAL_STORAGE permission not granted, adding to request list")
                }
            } else {
                // Android 6.0 到 Android 10，检查传统存储权限
                val readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                Log.d(TAG, "READ_EXTERNAL_STORAGE permission check result: $readPermission")
                if (readPermission != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    Log.d(TAG, "READ_EXTERNAL_STORAGE permission not granted, adding to request list")
                }

                val writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                Log.d(TAG, "WRITE_EXTERNAL_STORAGE permission check result: $writePermission")
                if (writePermission != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    Log.d(TAG, "WRITE_EXTERNAL_STORAGE permission not granted, adding to request list")
                }
            }

            if (permissions.isNotEmpty()) {
                Log.d(TAG, "Requesting permissions: $permissions")
                ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
            } else {
                // 权限已授予，初始化LLM会话
                Log.d(TAG, "All permissions already granted, initializing LLM session")
                appendToOutput("权限已授予，正在初始化模型...\n")
                initLlmSession()
            }
        } else {
            // Android 6.0以下版本不需要动态申请权限
            Log.d(TAG, "Android version < M, no need to request permissions")
            initLlmSession()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Log.d(TAG, "onRequestPermissionsResult called with requestCode: $requestCode")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            Log.d(TAG, "Processing our permission request")
            var allGranted = true
            for ((index, result) in grantResults.withIndex()) {
                Log.d(TAG, "Permission ${permissions[index]} result: $result")
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    Log.d(TAG, "Permission ${permissions[index]} not granted")
                }
            }

            if (allGranted) {
                // 权限已授予，初始化LLM会话
                Log.d(TAG, "All permissions granted, initializing LLM session")
                appendToOutput("权限已授予，正在初始化模型...\n")
                initLlmSession()
            } else {
                Log.d(TAG, "Some permissions not granted")
                Toast.makeText(this, "需要存储权限来访问模型文件", Toast.LENGTH_LONG).show()
                appendToOutput("需要存储权限来访问模型文件\n")
                appendToOutput("请在设置中授予权限，然后重启应用\n")

                // 显示更多信息帮助用户理解为什么需要权限
                appendToOutput("说明：此应用需要存储权限来读取下载的模型文件\n")
                appendToOutput("请前往设置->应用->MNN LLM Chat->权限，开启存储权限\n\n")
            }
        } else {
            Log.d(TAG, "Not our permission request, ignoring")
        }
    }

    private fun initViews() {
        Log.d(TAG, "initViews called")
        inputEditText = findViewById(com.alibaba.mnnllm.android.R.id.inputEditText)
        sendButton = findViewById(com.alibaba.mnnllm.android.R.id.sendButton)
        outputTextView = findViewById(com.alibaba.mnnllm.android.R.id.outputTextView)

        sendButton.setOnClickListener {
            val inputText = inputEditText.text.toString().trim()
            if (inputText.isNotEmpty()) {
                sendPrompt(inputText)
                inputEditText.setText("")
            } else {
                Toast.makeText(this, "请输入消息内容", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initLlmSession() {
        Log.d(TAG, "initLlmSession called")
        appendToOutput("正在查找已下载的模型...\n")

        // 使用ModelDownloadManager获取已下载的模型
        val modelDownloadManager = ModelDownloadManager.getInstance(this)

        // 尝试获取几个常见的模型
        val modelNames = arrayOf("Qwen3-1.7B-MNN", "Qwen3-0.5B-MNN", "Qwen3-4B-MNN", "Qwen2.5-0.5B-Instruct-MNN")
        var downloadedFile: File? = null
        var modelName: String = ""

        for (name in modelNames) {
            Log.d(TAG, "Checking for model: $name")
            val file = modelDownloadManager.getDownloadedFile(name)
            if (file != null && file.exists()) {
                downloadedFile = file
                modelName = name
                Log.d(TAG, "Found model: $name at ${file.absolutePath}")
                break
            } else {
                Log.d(TAG, "Model $name not found or doesn't exist")
            }
        }

        var modelPath: String? = null
        if (downloadedFile != null && downloadedFile.exists()) {
            modelPath = File(downloadedFile, "config.json").absolutePath
            appendToOutput("找到模型: $modelName\n")
            Log.d(TAG, "Model found at: $modelPath")
        }

        // 如果没找到现有模型，则使用默认路径作为示例
        if (modelPath == null) {
            modelPath = "/data/data/com.alibaba.mnnllm.android/files/tmps/Qwen3-1.7B-MNN/config.json"
            appendToOutput("未找到已下载的模型，将使用默认路径\n")
            Log.d(TAG, "Model not found, using default path: $modelPath")
        }

        appendToOutput("模型配置路径: $modelPath\n\n")

        try {
            // 创建LLM会话
            Log.d(TAG, "Creating LLM session with config path: $modelPath")
            llmSession = LlmSession(
                modelId = "simple_model",
                sessionId = System.currentTimeMillis().toString(),
                configPath = modelPath,
                savedHistory = null
            )

            appendToOutput("正在加载模型...\n")

            // 在后台线程加载模型
            Thread {
                try {
                    Log.d(TAG, "Loading model in background thread")
                    llmSession?.load()
                    isModelLoaded = true
                    runOnUiThread {
                        appendToOutput("模型加载成功！现在可以开始对话了。\n\n")
                        Log.d(TAG, "Model loaded successfully")
                    }
                    Timber.d("LLM Session initialized successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize LLM session", e)
                    Timber.e(e, "Failed to initialize LLM session")
                    runOnUiThread {
                        appendToOutput("模型加载失败: ${e.message}\n")
                        appendToOutput("请确保模型文件完整且路径正确\n\n")
                    }
                }
            }.start()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create LLM session", e)
            Timber.e(e, "Failed to create LLM session")
            appendToOutput("创建会话失败: ${e.message}\n")
        }
    }

    private fun sendPrompt(prompt: String) {
        Log.d(TAG, "sendPrompt called with: $prompt")
        if (!isModelLoaded) {
            appendToOutput("模型还在加载中，请稍候...\n\n")
            return
        }

        llmSession?.let { session ->
            // 显示用户输入
            appendToOutput("User: $prompt\n")

            // 在后台线程中生成回复
            Thread {
                try {
                    val params = mapOf<String, Any>()
                    session.generate(prompt, params, object : GenerateProgressListener {
                        override fun onProgress(progress: String?): Boolean {
                            runOnUiThread {
                                if (!progress.isNullOrEmpty()) {
                                    appendToOutput(progress)
                                }
                            }
                            // 返回false表示继续生成，返回true表示中断生成
                            return false
                        }
                    })

                    // 生成完成后添加换行
                    runOnUiThread {
                        appendToOutput("\n\n")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error generating response", e)
                    Timber.e(e, "Error generating response")
                    runOnUiThread {
                        appendToOutput("\n错误: ${e.message}\n\n")
                    }
                }
            }.start()
        } ?: run {
            appendToOutput("模型会话未初始化\n\n")
        }
    }

    private fun appendToOutput(text: String) {
        runOnUiThread {
            outputTextView.append(text)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        super.onDestroy()
        // 释放LLM资源
        llmSession?.release()
        llmSession = null
    }
}
