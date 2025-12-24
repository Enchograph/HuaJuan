package com.chenhongyu.huajuan.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

// 定义包装类来处理JSON结构
data class ModelConfigWrapper(
    val models: List<ModelConfig>
)

data class ModelConfig(
    val name: String,
    val id: String,
    val path: String,
    val description: String,
    val size: String,
    val quantization: String
)

class ModelManager(private val context: Context) {
    private val gson = Gson()
    
    fun getLocalModels(): List<ModelInfo> {
        val modelConfigs = loadModelConfigs()
        return modelConfigs.map { config ->
            ModelInfo(
                displayName = config.name,
                apiCode = config.id,
                modelPath = config.path
            )
        }
    }
    
    private fun loadModelConfigs(): List<ModelConfig> {
        return try {
            val inputStream = context.assets.open("models/model_config.json")
            val reader = InputStreamReader(inputStream)
            val type = object: TypeToken<ModelConfigWrapper>() {}.type
            val wrapper = gson.fromJson<ModelConfigWrapper>(reader, type)
            reader.close()
            wrapper.models
        } catch (e: Exception) {
            Log.e("ModelManager", "加载模型配置失败: ${e.message}")
            // 返回默认模型配置
            getDefaultModelConfigs()
        }
    }
    
    private fun getDefaultModelConfigs(): List<ModelConfig> {
        return listOf(
            ModelConfig(
                name = "Qwen3-0.6B-MNN",
                id = "qwen3-0.6b-mnn",
                path = "assets/models/qwen3_06b_mnn_model/llm.mnn",  // 更新路径指向实际模型文件
                description = "Qwen3 0.6B模型，使用MNN框架优化",
                size = "380MB",
                quantization = "int4"
            ),
            ModelConfig(
                name = "MobileLLM-125M-MNN",
                id = "mobilellm-125m-mnn",
                path = "assets/models/mobilellm_125m_mnn_model/llm.mnn",  // 更新路径指向实际模型文件
                description = "MobileLLM 125M模型，使用MNN框架优化",
                size = "245MB",
                quantization = "int4"
            )
        )
    }
    
    fun getModelPath(modelName: String): String {
        val configs = loadModelConfigs()
        val config = configs.find { it.name == modelName }
        return config?.path ?: when(modelName) {
            "Qwen3-0.6B-MNN" -> "assets/models/qwen3_06b_mnn_model/llm.mnn"
            "MobileLLM-125M-MNN" -> "assets/models/mobilellm_125m_mnn_model/llm.mnn"
            else -> "assets/models/$modelName/llm.mnn"
        }
    }
}