package com.huajuan.aispace.data

import android.content.Context
import android.content.res.AssetManager
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
    val description: String = "",
    val size: String = "",
    val quantization: String = "",
    val capabilities: List<ModelCapability> = emptyList()
)

class ModelManager(private val context: Context) {
    private companion object {
        const val TAG = "ModelManager"
        const val MODELS_ROOT = "models"
        const val MODEL_CONFIG_PATH = "$MODELS_ROOT/model_config.json"
        const val MODEL_ENTRY_FILE = "llm.mnn"
        const val MODEL_METADATA_FILE = "metadata.json"
    }

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
        loadModelConfigsFromManifest().takeIf { it.isNotEmpty() }?.let { return it }
        return loadModelConfigsFromAssetScan()
    }

    private fun loadModelConfigsFromManifest(): List<ModelConfig> {
        return try {
            val inputStream = context.assets.open(MODEL_CONFIG_PATH)
            val reader = InputStreamReader(inputStream)
            val type = object: TypeToken<ModelConfigWrapper>() {}.type
            val wrapper = gson.fromJson<ModelConfigWrapper>(reader, type)
            reader.close()
            wrapper.models
        } catch (e: Exception) {
            Log.w(TAG, "读取模型清单失败，回退到 assets 扫描: ${e.message}")
            emptyList()
        }
    }

    private fun loadModelConfigsFromAssetScan(): List<ModelConfig> {
        val children: List<String> = try {
            context.assets.list(MODELS_ROOT).orEmpty().toList()
        } catch (e: Exception) {
            Log.e(TAG, "扫描本地模型目录失败: ${e.message}", e)
            emptyList<String>()
        }
        return children
            .filterNot { it.equals("model_config.json", ignoreCase = true) }
            .mapNotNull { child -> discoverModelConfig(context.assets, child) }
            .sortedBy { it.name }
    }

    private fun discoverModelConfig(assetManager: AssetManager, modelDirName: String): ModelConfig? {
        val modelDir = "$MODELS_ROOT/$modelDirName"
        val dirEntries = try {
            assetManager.list(modelDir).orEmpty()
        } catch (e: Exception) {
            Log.w(TAG, "读取模型目录失败: $modelDir", e)
            return null
        }
        if (MODEL_ENTRY_FILE !in dirEntries) {
            return null
        }

        val metadata = loadOptionalMetadata(assetManager, "$modelDir/$MODEL_METADATA_FILE")
        val displayName = metadata?.name?.takeIf { it.isNotBlank() } ?: modelDirName
        val modelId = metadata?.id?.takeIf { it.isNotBlank() } ?: modelDirName
        return ModelConfig(
            name = displayName,
            id = modelId,
            path = "assets/$modelDir/$MODEL_ENTRY_FILE",
            description = metadata?.description.orEmpty(),
            size = metadata?.size.orEmpty(),
            quantization = metadata?.quantization.orEmpty(),
            capabilities = metadata?.capabilities.orEmpty()
        )
    }

    private fun loadOptionalMetadata(assetManager: AssetManager, metadataPath: String): ModelConfig? {
        return try {
            assetManager.open(metadataPath).use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    gson.fromJson(reader, ModelConfig::class.java)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun getModelPath(modelName: String): String {
        val configs = loadModelConfigs()
        val config = configs.find { it.name == modelName }
        return config?.path.orEmpty()
    }
}
