package com.huajuan.aispace.data

/**
 * 模型API服务工厂类
 * 根据配置创建相应的模型API服务实例
 */
class ModelApiFactory(private val repository: Repository) {
    
    /**
     * 创建模型API服务实例
     * @return ModelApiService 实例
     */
    fun createModelApiService(assistantId: String = repository.getCurrentAssistantId()): ModelApiService {
        val config = repository.getAssistantModelConfig(assistantId)
        val useCloud = config.useCloudModel
        val serviceProvider = config.serviceProvider
        val resolved = ModelRequestResolver.resolve(repository, assistantId)
        
        return when {
            // 图像生成模型走专用服务
            resolved.isImageGenerationService -> {
                ImageGenerationApiService(repository, assistantId)
            }
            useCloud -> {
                // 使用在线模型
                OnlineModelApiService(repository, assistantId)
            }
            else -> {
                // 使用本地模型
                LocalModelApiService(repository)
            }
        }
    }
    
    /**
     * 获取当前激活的模型API服务实例
     * @return ModelApiService 实例
     */
    fun getCurrentModelApiService(): ModelApiService {
        return createModelApiService()
    }

    fun getModelApiServiceFor(assistantId: String): ModelApiService {
        return createModelApiService(assistantId)
    }
}
