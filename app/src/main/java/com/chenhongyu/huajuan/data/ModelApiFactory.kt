package com.chenhongyu.huajuan.data

/**
 * 模型API服务工厂类
 * 根据配置创建相应的模型API服务实例
 */
class ModelApiFactory(private val repository: Repository) {
    
    /**
     * 创建模型API服务实例
     * @return ModelApiService 实例
     */
    fun createModelApiService(): ModelApiService {
        val useCloud = repository.getUseCloudModel()
        val serviceProvider = repository.getServiceProvider()
        
        return when {
            // 检查是否是图像生成服务提供商
            serviceProvider.contains("生图") || serviceProvider.contains("images/generations") -> {
                ImageGenerationApiService(repository)
            }
            useCloud -> {
                // 使用在线模型
                OnlineModelApiService(repository)
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
}