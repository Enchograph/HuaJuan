package com.chenhongyu.huajuan.data

import android.util.Log

/**
 * 模型API服务工厂类
 * 根据配置创建相应的模型API服务实例
 */
class ModelApiFactory(private val repository: Repository) {
    companion object {
        private const val TAG = "ModelApiFactory"
    }
    
    /**
     * 创建模型API服务实例
     * @return ModelApiService 实例
     */
    fun createModelApiService(): ModelApiService {
        return if (repository.getUseCloudModel()) {
            // 使用在线模型
            Log.d(TAG, "Creating online model API service")
            OnlineModelApiService(repository)
        } else {
            // 使用本地模型
            Log.d(TAG, "Creating local model API service")
            LocalModelApiService(repository)
        }
    }
    
    /**
     * 获取当前激活的模型API服务实例
     * 会检查服务是否可用，如果不可用则尝试回退到另一种实现
     * @return ModelApiService 实例
     */
    fun getCurrentModelApiService(): ModelApiService {
        val preferCloud = repository.getUseCloudModel()
        val service = createModelApiService()
        
        // 检查首选服务是否可用
        if (service.isAvailable()) {
            Log.d(TAG, "Preferred model service is available")
            return service
        }
        
        // 如果首选服务不可用，尝试另一种服务
        val fallbackService = if (preferCloud) {
            Log.d(TAG, "Preferred cloud service not available, falling back to local")
            LocalModelApiService(repository)
        } else {
            Log.d(TAG, "Preferred local service not available, falling back to cloud")
            OnlineModelApiService(repository)
        }
        
        if (fallbackService.isAvailable()) {
            Log.d(TAG, "Fallback model service is available")
            return fallbackService
        }
        
        // 如果两种服务都不可用，返回首选服务并让调用者处理错误
        Log.d(TAG, "Both model services are unavailable, returning preferred service")
        return service
    }
}