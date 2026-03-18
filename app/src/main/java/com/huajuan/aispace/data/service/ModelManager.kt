package com.huajuan.aispace.data

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
    val quantization: String,
    val capabilities: List<ModelCapability> = emptyList()
)

class ModelManager(private val context: Context) {
    private val gson = Gson()
    
    fun getLocalModels(): List<ModelInfo> {
        val modelConfigs = loadModelConfigs()
        return modelConfigs.map { config ->
            val caps = config.capabilities.ifEmpty { listOf(ModelCapability.Chat) }.toSet()
            ModelInfo(
                displayName = config.name,
                apiCode = config.id,
                modelPath = config.path,
                capabilities = caps
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
            emptyList()
        }
    }

    fun getModelPath(modelName: String): String {
        val configs = loadModelConfigs()
        val config = configs.find { it.name == modelName }
        return config?.path.orEmpty()
    }
}
