# MNN LLM Chat 项目CPP架构耦合详解

本文档详细说明了MNN LLM Chat项目是如何与CPP架构相耦合的，为您的另一个项目提供完全的指导。另一个项目是直接把CPP部分拷贝过去，意图效仿这个项目实现本地模型最简化的调用。

## 1. 项目整体架构

### 1.1 项目目录结构
```
MnnLlmChat/
├── app/
│   ├── src/main/
│   │   ├── cpp/                 # C++核心实现
│   │   │   ├── llm_session.h    # C++ LLM会话类定义
│   │   │   ├── llm_session.cpp  # C++ LLM会话类实现
│   │   │   ├── llm_mnn_jni.cpp  # JNI接口层
│   │   │   ├── CMakeLists.txt   # C++构建配置
│   │   │   ├── mnn/            # MNN库源码
│   │   │   └── mnn_tts/        # MNN TTS模块
│   │   ├── java/               # Java/Kotlin层
│   │   │   └── com/alibaba/mnnllm/android/llm/
│   │   │       ├── LlmSession.kt # Kotlin接口类
│   │   │       └── ChatSession.kt # 会话接口定义
│   │   └── jniLibs/            # 预编译的MNN库文件
│   │       ├── arm64-v8a/
│   │       └── armeabi-v7a/
```

## 2. C++核心组件详解

### 2.1 llm_session.h - C++ LLM会话类定义

这是整个架构的核心，定义了LLM会话的接口和数据结构：

```cpp
namespace mls {
using PromptItem = std::pair<std::string, std::string>;

class LlmSession {
public:
    // 构造函数
    LlmSession(std::string, json config, json extra_config, std::vector<std::string> string_history);
    
    // 核心方法
    void Load(); // 加载模型
    const MNN::Transformer::LlmContext *Response(const std::string &prompt, 
        const std::function<bool(const std::string &, bool is_eop)> &on_progress);
    
    // 历史管理
    void Reset();
    void clearHistory(int numToKeep = 1);
    
    // 配置管理
    void updateConfig(const std::string& config_json);
    void SetMaxNewTokens(int i);
    void setSystemPrompt(std::string system_prompt);
    
    // 音频输出
    void SetWavformCallback(std::function<bool(const float*, size_t, bool)> callback);
    void enableAudioOutput(bool b);
    
    // 基准测试
    struct BenchmarkResult {
        bool success;
        std::string error_message;
        std::vector<int64_t> prefill_times_us;
        std::vector<int64_t> decode_times_us;
        std::vector<int64_t> sample_times_us;
        int prompt_tokens;
        int generate_tokens;
        int repeat_count;
        bool kv_cache_enabled;
    };
    
    BenchmarkResult runBenchmark(int backend, int threads, bool useMmap, int power, 
                                int precision, int memory, int dynamicOption, int nPrompt, 
                                int nGenerate, int nRepeat, bool kvCache, 
                                const BenchmarkCallback& callback);

private:
    std::string model_path_;
    std::vector<PromptItem> history_;
    json extra_config_{};
    json config_{};
    Llm* llm_{nullptr}; // MNN LLM核心对象
    int max_new_tokens_{2048};
    std::string system_prompt_;
};
}
```

### 2.2 llm_session.cpp - C++ LLM会话实现

这个文件实现了LLM会话的所有核心功能：

- **模型加载**：`Load()`方法使用MNN库加载模型
- **对话历史管理**：维护用户和助手的对话历史
- **流式响应处理**：实现流式文本生成和处理
- **配置管理**：动态更新模型配置
- **基准测试**：实现性能基准测试功能

关键实现方法：

```cpp
void LlmSession::Load() {
    std::string root_cache_dir_str = extra_config_["mmap_dir"];
    bool use_mmap = !extra_config_["mmap_dir"].get<std::string>().empty();
    
    // 创建MNN执行器
    auto executor = MNN::Express::Executor::newExecutor(MNN_FORWARD_CPU, backendConfig, 1);
    MNN::Express::ExecutorScope s(executor);
    
    // 创建LLM对象
    llm_ = Llm::createLLM(model_path_);
    
    // 设置配置
    json config = config_;
    config["use_mmap"] = use_mmap;
    if (use_mmap) {
        std::string temp_dir = root_cache_dir_str;
        config["tmp_path"] = temp_dir;
    }
    
    auto config_str = config.dump();
    llm_->set_config(config_str);
    llm_->load(); // 实际加载模型
}

const MNN::Transformer::LlmContext * LlmSession::Response(const std::string &prompt,
    const std::function<bool(const std::string&, bool is_eop)>& on_progress) {
    // 添加用户输入到历史
    history_.emplace_back("user", getUserString(prompt.c_str(), false, is_r1_));
    
    // 设置流式处理器
    mls::Utf8StreamProcessor processor([&response_buffer, &on_progress, this](const std::string& utf8Char) {
        // 处理每个字符，调用进度回调
        if (on_progress) {
            bool user_stop_requested = on_progress(utf8Char, is_eop);
            generate_text_end_ = is_eop;
            stop_requested_ = user_stop_requested;
        }
    });
    
    // 执行推理
    llm_->response(history_, &output_ostream, "<eop>", 1);
    
    // 生成循环
    while (!stop_requested_ && !generate_text_end_ && current_size < max_new_tokens_) {
        llm_->generate(1);
        current_size++;
    }
    
    return llm_->getContext();
}
```

## 3. JNI接口层 - llm_mnn_jni.cpp

### 3.1 JNI函数映射

JNI层是Java/Kotlin与C++之间的桥梁，主要函数包括：

```
// Java_com_alibaba_mnnllm_android_llm_LlmSession_initNative
JNIEXPORT jlong JNICALL Java_com_alibaba_mnnllm_android_llm_LlmSession_initNative(JNIEnv *env,
                                                                                  jobject thiz,
                                                                                  jstring modelDir,
                                                                                  jobject chat_history,
                                                                                  jstring mergeConfigStr,
                                                                                  jstring configJsonStr);

// Java_com_alibaba_mnnllm_android_llm_LlmSession_submitNative
JNIEXPORT jobject JNICALL Java_com_alibaba_mnnllm_android_llm_LlmSession_submitNative(JNIEnv *env,
                                                                                      jobject thiz,
                                                                                      jlong llmPtr,
                                                                                      jstring inputStr,
                                                                                      jboolean keepHistory,
                                                                                      jobject progressListener);

// Java_com_alibaba_mnnllm_android_llm_LlmSession_resetNative
JNIEXPORT void JNICALL Java_com_alibaba_mnnllm_android_llm_LlmSession_resetNative(JNIEnv *env, 
                                                                                  jobject thiz,
                                                                                  jlong object_ptr);

// Java_com_alibaba_mnnllm_android_llm_LlmSession_releaseNative
JNIEXPORT void JNICALL Java_com_alibaba_mnnllm_android_llm_LlmSession_releaseNative(JNIEnv *env,
                                                                                    jobject thiz,
                                                                                    jlong objecPtr);
```

### 3.2 JNI接口实现细节

JNI接口实现的主要职责：

1. **类型转换**：Java/Kotlin类型与C++类型的转换
2. **指针管理**：C++对象指针在Java层的传递
3. **回调机制**：Java回调函数在C++中的调用
4. **内存管理**：字符串和对象的内存管理

示例实现：
```
JNIEXPORT jlong JNICALL Java_com_alibaba_mnnllm_android_llm_LlmSession_initNative(JNIEnv *env,
                                                                                  jobject thiz,
                                                                                  jstring modelDir,
                                                                                  jobject chat_history,
                                                                                  jstring mergeConfigStr,
                                                                                  jstring configJsonStr) {
    // 转换Java字符串到C++字符串
    const char *model_dir = env->GetStringUTFChars(modelDir, nullptr);
    auto model_dir_str = std::string(model_dir);
    const char *config_json_cstr = env->GetStringUTFChars(configJsonStr, nullptr);
    const char *merged_config_cstr = env->GetStringUTFChars(mergeConfigStr, nullptr);
    
    // 解析JSON配置
    json merged_config = json::parse(merged_config_cstr);
    json extra_json_config = json::parse(config_json_cstr);
    
    // 释放Java字符串
    env->ReleaseStringUTFChars(modelDir, model_dir);
    env->ReleaseStringUTFChars(configJsonStr, config_json_cstr);
    env->ReleaseStringUTFChars(mergeConfigStr, merged_config_cstr);
    
    // 解析聊天历史
    std::vector<std::string> history;
    if (chat_history != nullptr) {
        jclass listClass = env->GetObjectClass(chat_history);
        jmethodID sizeMethod = env->GetMethodID(listClass, "size", "()I");
        jmethodID getMethod = env->GetMethodID(listClass, "get", "(I)Ljava/lang/Object;");
        jint listSize = env->CallIntMethod(chat_history, sizeMethod);
        for (jint i = 0; i < listSize; i++) {
            jobject element = env->CallObjectMethod(chat_history, getMethod, i);
            const char *elementCStr = env->GetStringUTFChars((jstring) element, nullptr);
            history.emplace_back(elementCStr);
            env->ReleaseStringUTFChars((jstring) element, elementCStr);
            env->DeleteLocalRef(element);
        }
    }
    
    // 创建C++ LLM会话对象
    auto llm_session = new mls::LlmSession(model_dir_str, merged_config, extra_json_config, history);
    llm_session->Load(); // 加载模型
    
    // 返回C++对象指针给Java层
    return reinterpret_cast<jlong>(llm_session);
}
```

## 4. CMakeLists.txt 配置

### 4.1 库和头文件配置

```
# 设置MNN库路径
set(MNN_SOURCE_ROOT "${CMAKE_SOURCE_DIR}/mnn")
set(MNN_INSTALL_ROOT "${MNN_SOURCE_ROOT}/project/android/build_64")

# 包含头文件目录
include_directories("${MNN_SOURCE_ROOT}/include/")
include_directories("${MNN_SOURCE_ROOT}/transformers/llm/engine/include")
include_directories("${MNN_SOURCE_ROOT}/tools/audio/include")
include_directories("${CMAKE_SOURCE_DIR}/third_party")

# 导入MNN库
add_library(MNN SHARED IMPORTED)
set_target_properties(
        MNN
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libMNN.so
)

# 链接库
target_link_libraries(${CMAKE_PROJECT_NAME}
        android
        log
        MNN
)
```

### 4.2 TTS模块配置

```
# MNN TTS配置
set(MNN_TTS_SOURCE_ROOT "${CMAKE_SOURCE_DIR}/mnn_tts")

include_directories(
        ${MNN_TTS_SOURCE_ROOT}/include
        ${CMAKE_SOURCE_DIR}/third_party/include
)

add_library(mnn_tts SHARED ${BERTVITS2_SOURCE_FILES} ${SHARED_SOURCE_FILES} ${ANDROID_SOURCE_FILES})

target_link_libraries(mnn_tts log MNN)
```

## 5. Java/Kotlin层实现

### 5.1 LlmSession.kt

Java/Kotlin层是用户直接交互的接口：

``kotlin
class LlmSession (
    private val modelId: String,
    override var sessionId: String,
    private val configPath: String,
    var savedHistory: List<ChatDataItem>?
): ChatSession{
    private var nativePtr: Long = 0  // C++对象指针
    
    override fun load() {
        // 加载模型配置
        val config = ModelConfig.loadMergedConfig(configPath, getExtraConfigFile(modelId))!!
        var rootCacheDir: String? = ""
        if (config.useMmap == true) {
            rootCacheDir = MmapUtils.getMmapDir(modelId)
            File(rootCacheDir).mkdirs()
        }
        
        // 调用JNI初始化
        nativePtr = initNative(
                configPath,
                historyStringList,
                Gson().toJson(extraConfig),
                Gson().toJson(configMap)
        )
    }
    
    override fun generate(prompt: String, params: Map<String, Any>, 
                         progressListener: GenerateProgressListener): HashMap<String, Any> {
        // 调用JNI生成方法
        return submitNative(nativePtr, prompt, keepHistory, progressListener)
    }
    
    // JNI声明
    private external fun initNative(
            configPath: String?,
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
    
    // 其他JNI方法声明...
}
```

### 5.2 库加载

``kotlin
companion object {
    const val TAG: String = "LlmSession"

    init {
        System.loadLibrary("mnnllmapp")  // 加载C++库
    }
}
```

## 6. 在新项目中实现与CPP文件的耦合

### 6.1 项目结构设置

在新项目中，您需要创建以下目录结构：

```
YourProject/
├── app/
│   ├── src/main/
│   │   ├── cpp/                 # 拷贝的C++代码
│   │   │   ├── llm_session.h    # 拷贝自MNN LLM Chat
│   │   │   ├── llm_session.cpp  # 拷贝自MNN LLM Chat
│   │   │   ├── llm_mnn_jni.cpp  # 拷贝自MNN LLM Chat
│   │   │   ├── CMakeLists.txt   # 拷贝并修改自MNN LLM Chat
│   │   │   ├── mnn/             # MNN库源码（部分）
│   │   │   ├── mnn_tts/         # MNN TTS模块（如果需要）
│   │   │   └── third_party/     # 第三方库（如nlohmann json）
│   │   ├── java/                # Java/Kotlin接口层
│   │   │   └── com/yourpackage/
│   │   │       └── LocalModelApiService.kt # 新的API服务实现
│   │   └── jniLibs/             # MNN预编译库
│   │       ├── arm64-v8a/
│   │       │   └── libMNN.so
│   │       └── armeabi-v7a/
│   │           └── libMNN.so
```

### 6.2 配置新项目的CMakeLists.txt

修改拷贝的CMakeLists.txt以适应新项目：

```
# 设置最小CMake版本
cmake_minimum_required(VERSION 3.22.1)

# 声明项目名称
project("yourprojectname")

# 添加库
add_library(${CMAKE_PROJECT_NAME} SHARED
        # 列出C/C++源文件
        llm_mnn_jni.cpp
        llm_session.cpp
)

# 添加16KB页面大小支持（Android 15+需要）
target_link_options(${CMAKE_PROJECT_NAME} PRIVATE "-Wl,-z,max-page-size=16384")

# 设置MNN库路径
set(MNN_SOURCE_ROOT "${CMAKE_SOURCE_DIR}/mnn")
set(MNN_INSTALL_ROOT "${MNN_SOURCE_ROOT}/project/android/build_64")

# 输出路径信息用于调试
message(INFO "MNN_SOURCE_ROOT: ${MNN_SOURCE_ROOT}")
message(INFO "MNN_INSTALL_ROOT: ${MNN_INSTALL_ROOT}")

# 包含头文件目录
include_directories("${MNN_SOURCE_ROOT}/include/")
include_directories("${MNN_SOURCE_ROOT}/transformers/llm/engine/include")
include_directories("${CMAKE_SOURCE_DIR}/third_party")

# 设置库路径
set(LIB_PATH "${MNN_INSTALL_ROOT}/lib")

# 导入MNN库
add_library(MNN SHARED IMPORTED)
set_target_properties(
        MNN
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libMNN.so
)

# 链接库
target_link_libraries(${CMAKE_PROJECT_NAME}
        android
        log
        MNN
)
```

### 6.3 创建新项目的API接口层

基于您提供的参考文件，创建与CPP耦合的API服务：

```
// LocalModelApiService.kt - 与CPP LLM会话的耦合实现
package com.yourpackage.data

import com.chenhongyu.huajuan.network.Message
import com.chenhongyu.huajuan.stream.ChatEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 本地模型API服务实现 - 与CPP LLM会话耦合
 * 实现与拷贝的CPP代码的完整集成
 */
class LocalModelApiService(private val repository: Repository) : ModelApiService {
    
    // CPP LLM会话实例
    private var llmSession: LlmSession? = null

    override fun isAvailable(): Boolean {
        // 检查CPP LLM会话是否可用
        return llmSession != null
    }

    override suspend fun getAIResponse(messages: List<Message>, modelInfo: ModelInfo): String {
        try {
            // 初始化LLM会话（如果尚未初始化）
            if (llmSession == null) {
                llmSession = LlmSession(
                    modelId = modelInfo.apiCode,
                    sessionId = System.currentTimeMillis().toString(),
                    configPath = modelInfo.modelPath, // 模型路径
                    savedHistory = null
                )
                llmSession?.load() // 加载模型
            }

            // 构建提示词，将消息历史转换为字符串
            val prompt = buildPromptFromMessages(messages)
            
            // 调用CPP LLM会话生成响应
            val result = llmSession?.generate(prompt, emptyMap(), object : GenerateProgressListener {
                override fun onProgress(progress: String?): Boolean {
                    // 进度回调处理（可选）
                    return false // 不中断生成
                }
            })

            // 提取响应文本
            return result?.get("response") as? String ?: "生成失败"
        } catch (e: Exception) {
            return "错误：${e.message}"
        }
    }

    override fun streamAIResponse(messages: List<Message>, modelInfo: ModelInfo): Flow<ChatEvent> = flow {
        try {
            // 初始化LLM会话（如果尚未初始化）
            if (llmSession == null) {
                llmSession = LlmSession(
                    modelId = modelInfo.apiCode,
                    sessionId = System.currentTimeMillis().toString(),
                    configPath = modelInfo.modelPath,
                    savedHistory = null
                )
                llmSession?.load()
            }

            val prompt = buildPromptFromMessages(messages)

            // 创建进度监听器用于流式响应
            val progressListener = object : GenerateProgressListener {
                override fun onProgress(progress: String?): Boolean {
                    if (progress != null && progress != "<eop>") {
                        // 发送流式数据块
                        emit(ChatEvent.Chunk(progress))
                    }
                    return false // 不中断生成
                }
            }

            // 调用CPP LLM会话生成流式响应
            val result = llmSession?.generate(prompt, emptyMap(), progressListener)
            
            // 完成流式传输
            emit(ChatEvent.Done)
        } catch (e: Exception) {
            emit(ChatEvent.Error("错误：${e.message}"))
        }
    }

    /**
     * 从消息列表构建提示词
     */
    private fun buildPromptFromMessages(messages: List<Message>): String {
        return messages.joinToString("\n") { message ->
            "${message.role}: ${message.content}"
        }
    }

    /**
     * 释放LLM会话资源
     */
    fun release() {
        llmSession?.release()
        llmSession = null
    }
}
```

### 6.4 创建LLM会话包装类

```
// LlmSession.kt - CPP LLM会话的Kotlin包装类
package com.yourpackage

import android.util.Log

/**
 * LLM会话包装类 - 与CPP实现的直接耦合
 */
class LlmSession(
    private val modelId: String,
    var sessionId: String,
    private val configPath: String,
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
    fun load() {
        Log.d(TAG, "开始加载模型: $configPath")
        modelLoading = true
        
        // 转换历史消息为字符串列表
        val historyStringList = savedHistory?.map { it.text }?.filterNotNull()
        
        // 加载模型配置
        val configJson = loadModelConfig(configPath)
        val extraConfig = createExtraConfig()
        
        // 调用JNI初始化方法
        nativePtr = initNative(
            configPath,
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
        configPath: String?,
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
        return android.util.JsonWriter(android.content.Context()).toString() // 伪代码，实际使用Gson
    }

    /**
     * 加载模型配置
     */
    private fun loadModelConfig(configPath: String): String {
        // 实现配置加载逻辑
        return "{}" // 伪代码
    }

    /**
     * 获取MMap目录
     */
    private fun getMmapDir(modelId: String): String {
        // 实现MMap目录获取逻辑
        return ""
    }

    companion object {
        private const val TAG = "LlmSession"

        init {
            System.loadLibrary("yourprojectname") // 加载C++库
        }
    }
}
```

### 6.5 创建生成进度监听器接口

```
// GenerateProgressListener.kt
package com.yourpackage

/**
 * 生成进度监听器 - 用于CPP到Java的回调
 */
interface GenerateProgressListener {
    /**
     * 进度回调
     * @param progress 进度信息，可能为null
     * @return true表示中断生成，false表示继续
     */
    fun onProgress(progress: String?): Boolean
}
```

### 6.6 更新CMakeLists.txt中的JNI实现

在llm_mnn_jni.cpp中，您需要根据新项目的包名调整JNI函数签名：

```
// 根据新项目包名调整JNI函数名
JNIEXPORT jlong JNICALL Java_com_yourpackage_LlmSession_initNative(JNIEnv *env,
                                                                  jobject thiz,
                                                                  jstring modelDir,
                                                                  jobject chat_history,
                                                                  jstring mergeConfigStr,
                                                                  jstring configJsonStr) {
    // 保持原有实现不变
}
```

### 6.7 集成到ModelApiFactory

```
// ModelApiFactory.kt
package com.yourpackage.data

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
        return if (repository.getUseCloudModel()) {
            // 使用在线模型
            OnlineModelApiService(repository)
        } else {
            // 使用本地模型 - 现在使用CPP LLM会话
            LocalModelApiService(repository)
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
```

## 7. 最简化调用实现指南

### 7.1 拷贝CPP文件到新项目

1. **拷贝CPP源码**：
   - 将MNN LLM Chat项目的`app/src/main/cpp`目录下的所有文件拷贝到新项目的对应位置
   - 确保包含所有头文件、源文件和依赖库

2. **配置build.gradle**：
   ```gradle
   android {
       compileSdk 34

       defaultConfig {
           // ... 其他配置
           externalNativeBuild {
               cmake {
                   cppFlags "-std=c++17"
                   arguments "-DANDROID_STL=c++_shared"
               }
           }
           ndk {
               abiFilters 'arm64-v8a', 'armeabi-v7a'
           }
       }

       externalNativeBuild {
           cmake {
               path file('src/main/cpp/CMakeLists.txt')
               version '3.22.1'
           }
       }
   }
   ```

### 7.2 初始化和使用

```
// 在您的Activity或Service中
class MainActivity : AppCompatActivity() {
    private lateinit var modelApiService: ModelApiService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 创建API服务
        val factory = ModelApiFactory(repository)
        modelApiService = factory.createModelApiService()
        
        // 检查可用性
        if (modelApiService.isAvailable()) {
            // 使用服务
            val message = Message("user", "Hello, how are you?")
            val modelInfo = ModelInfo(
                displayName = "Local Model",
                apiCode = "local-model",
                modelPath = "/path/to/your/model" // 模型文件路径
            )
            
            // 同步调用
            lifecycleScope.launch {
                val response = modelApiService.getAIResponse(
                    listOf(message), 
                    modelInfo
                )
                println("Response: $response")
            }
            
            // 流式调用
            lifecycleScope.launch {
                modelApiService.streamAIResponse(
                    listOf(message), 
                    modelInfo
                ).collect { event ->
                    when (event) {
                        is ChatEvent.Chunk -> {
                            // 处理响应块
                            print(event.content)
                        }
                        is ChatEvent.Done -> {
                            // 响应完成
                            println("\nGeneration completed")
                        }
                        is ChatEvent.Error -> {
                            // 处理错误
                            println("Error: ${event.message}")
                        }
                    }
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 释放资源
        if (modelApiService is LocalModelApiService) {
            (modelApiService as LocalModelApiService).release()
        }
    }
}
```

## 8. 关键注意事项

### 8.1 内存管理
- C++对象的生命周期管理非常重要
- JNI层必须正确释放C++对象以避免内存泄漏
- 在应用生命周期结束时确保释放LLM会话资源

### 8.2 线程安全
- LLM推理通常是单线程的
- 在多线程环境中需要适当的同步机制
- JNI回调需要特别注意线程安全

### 8.3 错误处理
- 实现完善的错误处理机制
- 提供详细的错误信息用于调试
- 确保程序在错误情况下能优雅恢复

### 8.4 性能优化
- 合理设置模型参数以平衡性能和质量
- 使用MMap优化模型加载时间
- 实现流式处理以提供更好的用户体验

这个架构设计通过JNI作为桥梁，将C++的高性能LLM推理能力与Java/Kotlin的易用性相结合，实现了高效的本地模型调用。