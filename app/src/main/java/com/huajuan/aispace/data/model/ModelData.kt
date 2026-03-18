package com.huajuan.aispace.data

import com.huajuan.aispace.utils.debugLog
import androidx.annotation.StringRes

data class ModelInfo(
    val displayName: String,
    val apiCode: String,
    val modelPath: String = "",  // 本地模型路径，对于云端模型可以为空
    val capabilities: Set<ModelCapability> = emptySet()
)

data class ServiceProviderInfo(
    val id: String,
    @StringRes val displayNameRes: Int,
    val baseUrl: String,
    val consoleUrl: String? = null,
    val providerType: ProviderType = ProviderType.OpenAI,
    val models: List<ModelInfo>
)

class ModelDataProvider(private val repository: Repository) {

    fun getModelListForProvider(providerName: String): List<ModelInfo> {
        val normalizedProvider = BuiltInServiceProviders.normalizeId(providerName)
        // 先检查是否是预定义的服务提供商
        Companion.predefinedServiceProviders[normalizedProvider]?.let {
            debugLog(TAG) { "预定义服务提供商 $normalizedProvider 模型数=${it.models.size}" }
            return it.models 
        }
        
        // 如果不是预定义的，则从自定义提供商中查找
        val customModels = repository.getCustomModelsForProvider(providerName)
        debugLog(TAG) { "自定义服务提供商 $providerName 模型数=${customModels.size}" }
        return customModels.map { modelName ->
            val apiCode = repository.getCustomModelApiCode(providerName, modelName)
            val capabilities = repository.getCustomModelCapabilities(providerName, modelName)
            ModelInfo(
                displayName = modelName,
                apiCode = apiCode,
                capabilities = capabilities.ifEmpty { setOf(ModelCapability.Chat) }
            )
        }.sortedBy { it.displayName } // 按名称排序，确保一致性
    }

    fun getApiUrlForProvider(providerName: String): String {
        val normalizedProvider = BuiltInServiceProviders.normalizeId(providerName)
        // 先检查是否是预定义的服务提供商
        Companion.predefinedServiceProviders[normalizedProvider]?.let {
            debugLog(TAG) { "预定义服务提供商 $normalizedProvider API URL 已命中" }
            return it.baseUrl 
        }
        
        // 如果不是预定义的，则从自定义提供商中查找
        val customUrl = repository.getCustomProviderApiUrl(providerName)
        debugLog(TAG) { "自定义服务提供商 $providerName API URL 已读取" }
        return customUrl
    }

    fun getProviderTypeForProvider(providerName: String): ProviderType {
        Companion.predefinedServiceProviders[BuiltInServiceProviders.normalizeId(providerName)]?.let {
            return it.providerType 
        }
        return repository.getCustomProviderType(providerName)
    }
    
    fun getAllServiceProviders(): List<String> {
        val predefined = Companion.predefinedServiceProviders.keys.toList()
        val custom = repository.getCustomServiceProviders().toList()
        // 合并列表
        var allProviders = (predefined + custom).distinct()
            .filterNot { it == "应用试用" }
        // 保持原有的排序一致性（按名称排序）
        allProviders = allProviders.sorted()
        debugLog(TAG) { "服务提供商总数=${allProviders.size}" }
        return allProviders
    }

    companion object {
        private const val TAG = "ModelDataProvider"

        val predefinedServiceProviders = BuiltInServiceProviders.providers
    }
}
