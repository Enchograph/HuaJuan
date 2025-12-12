package com.chenhongyu.huajuan.data

import android.content.Context

data class ModelInfo(
    val displayName: String,
    val apiCode: String
)

data class ServiceProviderInfo(
    val name: String,
    val apiUrl: String,
    val models: List<ModelInfo>
)

class ModelDataProvider(private val repository: Repository) {
    
    fun getModelListForProvider(providerName: String): List<ModelInfo> {
        // 先检查是否是预定义的服务提供商
        Companion.predefinedServiceProviders[providerName]?.let { 
            println("获取到预定义服务提供商 $providerName 的模型列表，共 ${it.models.size} 个模型")
            return it.models 
        }
        
        // 如果不是预定义的，则从自定义提供商中查找
        val customModels = repository.getCustomModelsForProvider(providerName)
        println("获取到自定义服务提供商 $providerName 的模型列表，共 ${customModels.size} 个模型")
        return customModels.map { modelName ->
            val apiCode = repository.getCustomModelApiCode(providerName, modelName)
            ModelInfo(modelName, apiCode)
        }.sortedBy { it.displayName } // 按名称排序，确保一致性
    }

    fun getApiUrlForProvider(providerName: String): String {
        // 先检查是否是预定义的服务提供商
        Companion.predefinedServiceProviders[providerName]?.let { 
            println("获取到预定义服务提供商 $providerName 的API URL: ${it.apiUrl}")
            return it.apiUrl 
        }
        
        // 如果不是预定义的，则从自定义提供商中查找
        val customUrl = repository.getCustomProviderApiUrl(providerName)
        println("获取到自定义服务提供商 $providerName 的API URL: $customUrl")
        return customUrl
    }
    
    fun getAllServiceProviders(): List<String> {
        val predefined = Companion.predefinedServiceProviders.keys.toList()
        val custom = repository.getCustomServiceProviders().toList()
        val allProviders = (predefined + custom).sorted() // 排序确保一致性
        println("获取到所有服务提供商列表，共 ${allProviders.size} 个: $allProviders")
        return allProviders
    }

    companion object {
        val predefinedServiceProviders = mapOf(
            "应用试用" to ServiceProviderInfo(
                "应用试用",
                "https://api.siliconflow.cn/v1/chat/completions/",
                listOf(
                    ModelInfo("DeepSeek-V3.2", "deepseek-ai/DeepSeek-V3.2"),
                    ModelInfo("DeepSeek-V3.1-Terminus", "deepseek-ai/DeepSeek-V3.1-Terminus"),
                    ModelInfo("DeepSeek-V3.2-Exp", "deepseek-ai/DeepSeek-V3.2-Exp"),
                    ModelInfo("DeepSeek-R1", "deepseek-ai/DeepSeek-R1"),
                    ModelInfo("DeepSeek-V3", "deepseek-ai/DeepSeek-V3"),
                    ModelInfo("Qwen3-VL-32B-Instruct", "Qwen/Qwen3-VL-32B-Instruct"),
                    ModelInfo("Qwen3-VL-32B-Thinking", "Qwen/Qwen3-VL-32B-Thinking"),
                    ModelInfo("Qwen3-VL-8B-Instruct", "Qwen/Qwen3-VL-8B-Instruct"),
                    ModelInfo("Qwen3-VL-8B-Thinking", "Qwen/Qwen3-VL-8B-Thinking"),
                    ModelInfo("Qwen3-VL-30B-A3B-Instruct", "Qwen/Qwen3-VL-30B-A3B-Instruct"),
                    ModelInfo("Qwen3-VL-30B-A3B-Thinking", "Qwen/Qwen3-VL-30B-A3B-Thinking"),
                    ModelInfo("Qwen3-VL-235B-A22B-Instruct", "Qwen/Qwen3-VL-235B-A22B-Instruct"),
                    ModelInfo("Qwen3-VL-235B-A22B-Thinking", "Qwen/Qwen3-VL-235B-A22B-Thinking"),
                    ModelInfo("Qwen3-Omni-30B-A3B-Instruct", "Qwen/Qwen3-Omni-30B-A3B-Instruct"),
                    ModelInfo("Qwen3-Omni-30B-A3B-Thinking", "Qwen/Qwen3-Omni-30B-A3B-Thinking"),
                    ModelInfo("Qwen3-Next-80B-A3B-Instruct", "Qwen/Qwen3-Next-80B-A3B-Instruct"),
                    ModelInfo("Qwen3-Next-80B-A3B-Thinking", "Qwen/Qwen3-Next-80B-A3B-Thinking"),
                    ModelInfo("Qwen-Image-Edit-2509", "Qwen/Qwen-Image-Edit-2509"),
                    ModelInfo("Qwen3-Coder-30B-A3B-Instruct", "Qwen/Qwen3-Coder-30B-A3B-Instruct"),
                    ModelInfo("Qwen3-Coder-480B-A35B-Instruct", "Qwen/Qwen3-Coder-480B-A35B-Instruct"),
                    ModelInfo("Qwen3-30B-A3B-Thinking-2507", "Qwen/Qwen3-30B-A3B-Thinking-2507"),
                    ModelInfo("Qwen3-30B-A3B-Instruct-2507", "Qwen/Qwen3-30B-A3B-Instruct-2507"),
                    ModelInfo("Qwen3-235B-A22B-Thinking-2507", "Qwen/Qwen3-235B-A22B-Thinking-2507"),
                    ModelInfo("Qwen3-235B-A22B-Instruct-2507", "Qwen/Qwen3-235B-A22B-Instruct-2507"),
                    ModelInfo("QwenLong-L1-32B", "Tongyi-Zhiwen/QwenLong-L1-32B"),
                    ModelInfo("Qwen3-32B", "Qwen/Qwen3-32B"),
                    ModelInfo("Qwen3-14B", "Qwen/Qwen3-14B"),
                    ModelInfo("Qwen2.5-VL-72B-Instruct", "Qwen/Qwen2.5-VL-72B-Instruct")
                )
            ),
            "硅基流动" to ServiceProviderInfo(
                "硅基流动",
                "https://api.siliconflow.cn/v1/chat/completions/",
                listOf(
                    ModelInfo("DeepSeek-V3.2", "deepseek-ai/DeepSeek-V3.2"),
                    ModelInfo("DeepSeek-V3.1-Terminus", "deepseek-ai/DeepSeek-V3.1-Terminus"),
                    ModelInfo("DeepSeek-V3.2-Exp", "deepseek-ai/DeepSeek-V3.2-Exp"),
                    ModelInfo("DeepSeek-R1", "deepseek-ai/DeepSeek-R1"),
                    ModelInfo("DeepSeek-V3", "deepseek-ai/DeepSeek-V3"),
                    ModelInfo("Qwen3-VL-32B-Instruct", "Qwen/Qwen3-VL-32B-Instruct"),
                    ModelInfo("Qwen3-VL-32B-Thinking", "Qwen/Qwen3-VL-32B-Thinking"),
                    ModelInfo("Qwen3-VL-8B-Instruct", "Qwen/Qwen3-VL-8B-Instruct"),
                    ModelInfo("Qwen3-VL-8B-Thinking", "Qwen/Qwen3-VL-8B-Thinking"),
                    ModelInfo("Qwen3-VL-30B-A3B-Instruct", "Qwen/Qwen3-VL-30B-A3B-Instruct"),
                    ModelInfo("Qwen3-VL-30B-A3B-Thinking", "Qwen/Qwen3-VL-30B-A3B-Thinking"),
                    ModelInfo("Qwen3-VL-235B-A22B-Instruct", "Qwen/Qwen3-VL-235B-A22B-Instruct"),
                    ModelInfo("Qwen3-VL-235B-A22B-Thinking", "Qwen/Qwen3-VL-235B-A22B-Thinking"),
                    ModelInfo("Qwen3-Omni-30B-A3B-Instruct", "Qwen/Qwen3-Omni-30B-A3B-Instruct"),
                    ModelInfo("Qwen3-Omni-30B-A3B-Thinking", "Qwen/Qwen3-Omni-30B-A3B-Thinking"),
                    ModelInfo("Qwen3-Next-80B-A3B-Instruct", "Qwen/Qwen3-Next-80B-A3B-Instruct"),
                    ModelInfo("Qwen3-Next-80B-A3B-Thinking", "Qwen/Qwen3-Next-80B-A3B-Thinking"),
                    ModelInfo("Qwen-Image-Edit-2509", "Qwen/Qwen-Image-Edit-2509"),
                    ModelInfo("Qwen3-Coder-30B-A3B-Instruct", "Qwen/Qwen3-Coder-30B-A3B-Instruct"),
                    ModelInfo("Qwen3-Coder-480B-A35B-Instruct", "Qwen/Qwen3-Coder-480B-A35B-Instruct"),
                    ModelInfo("Qwen3-30B-A3B-Thinking-2507", "Qwen/Qwen3-30B-A3B-Thinking-2507"),
                    ModelInfo("Qwen3-30B-A3B-Instruct-2507", "Qwen/Qwen3-30B-A3B-Instruct-2507"),
                    ModelInfo("Qwen3-235B-A22B-Thinking-2507", "Qwen/Qwen3-235B-A22B-Thinking-2507"),
                    ModelInfo("Qwen3-235B-A22B-Instruct-2507", "Qwen/Qwen3-235B-A22B-Instruct-2507"),
                    ModelInfo("QwenLong-L1-32B", "Tongyi-Zhiwen/QwenLong-L1-32B"),
                    ModelInfo("Qwen3-32B", "Qwen/Qwen3-32B"),
                    ModelInfo("Qwen3-14B", "Qwen/Qwen3-14B"),
                    ModelInfo("Qwen2.5-VL-72B-Instruct", "Qwen/Qwen2.5-VL-72B-Instruct")
                )
            ),
            "火山引擎" to ServiceProviderInfo(
                "火山引擎",
                "https://ark.cn-beijing.volces.com/api/v3/chat/completions/",
                listOf(
                    ModelInfo("Seedream 4.5", "Doubao-Seedream-4.5"),
                    ModelInfo("Seedream 4.0", "Doubao-Seedream-4.0"),
                    ModelInfo("DeepSeek-V3.1", "DeepSeek-V3.1"),
                    ModelInfo("Kimi-K2", "Kimi-K2"),
                    ModelInfo("DeepSeek-R1", "DeepSeek-R1"),
                    ModelInfo("DeepSeek-V3", "DeepSeek-V3"),
                    ModelInfo("Seedream 3.0", "Doubao-Seedream-3.0-t2i"),
                    ModelInfo("Doubao-1.5-pro", "Doubao-1.5-pro-32k"),
                    ModelInfo("Doubao-1.5-lite", "Doubao-1.5-lite-32k")
                )
            ),
            "SoruxGPT" to ServiceProviderInfo(
                "SoruxGPT",
                "https://ai.soruxgpt.com/v1/chat/completions/",
                listOf(
                    ModelInfo("ChatGPT-4o", "chatgpt-4o"),
                    ModelInfo("Claude-Sonnet", "claude-sonnet"),
                    ModelInfo("Claude-Sonnet-4-5-All", "claude-sonnet-4-5-all"),
                    ModelInfo("DeepSeek", "deepseek"),
                    ModelInfo("DeepSeek-Reasoner", "deepseek-reasoner"),
                    ModelInfo("DeepSeek-V3.2-Exp", "deepseek-v3.2-exp"),
                    ModelInfo("Net-DeepSeek-R1", "net-deepseek-r1"),
                    ModelInfo("Gemini-2.5", "gemini-2.5"),
                    ModelInfo("Gemini-2.5-Flash-Thinking", "gemini-2.5-flash-thinking"),
                    ModelInfo("Gemini-2.5-Pro-Thinking", "gemini-2.5-pro-thinking"),
                    ModelInfo("Gemini-3", "gemini-3"),
                    ModelInfo("Gemini-3-Pro-Preview-Thinking", "gemini-3-pro-preview-thinking"),
                    ModelInfo("GPT-3.5-Turbo", "gpt-3.5-turbo")
                )
            ),
            "OpenAI" to ServiceProviderInfo(
                "OpenAI",
                "https://api.openai.com/v1/responses/",
                listOf(
                    ModelInfo("GPT-4o", "GPT-4o"),
                    ModelInfo("GPT-4o-mini", "GPT-4o-mini"),
                    ModelInfo("GPT-4.5-Preview", "gpt-4.5-preview"),
                    ModelInfo("O1-mini", "o1-mini"),
                    ModelInfo("O1-preview", "o1-preview")
                )
            ),
            "Anthropic" to ServiceProviderInfo(
                "Anthropic",
                "https://api.anthropic.com/v1/messages/",
                listOf(
                    ModelInfo("Claude 3 Opus", "Claude 3 Opus"),
                    ModelInfo("Claude 3.5 Sonnet", "Claude 3.5 Sonnet"),
                    ModelInfo("Claude 3.5 Haiku", "Claude 3.5 Haiku"),
                    ModelInfo("Claude 3.7 Sonnet", "Claude 3.7 Sonnet"),
                    ModelInfo("Claude Sonnet 4", "Claude Sonnet 4")
                )
            )
        )
    }
}