#include <android/log.h>
#include <jni.h>
#include <string>
#include <vector>
#include "llm_session.h"
#include "mls_log.h"
#include "nlohmann/json.hpp"

using json = nlohmann::json;

namespace {

jobject createLong(JNIEnv* env, jlong value) {
    jclass longClass = env->FindClass("java/lang/Long");
    jmethodID ctor = env->GetMethodID(longClass, "<init>", "(J)V");
    return env->NewObject(longClass, ctor, value);
}

jobject createResultMap(JNIEnv* env) {
    jclass hashMapClass = env->FindClass("java/util/HashMap");
    jmethodID hashMapInit = env->GetMethodID(hashMapClass, "<init>", "()V");
    return env->NewObject(hashMapClass, hashMapInit);
}

void putLong(JNIEnv* env, jobject map, const char* key, jlong value) {
    jclass hashMapClass = env->GetObjectClass(map);
    jmethodID putMethod = env->GetMethodID(
        hashMapClass,
        "put",
        "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
    );
    jstring javaKey = env->NewStringUTF(key);
    jobject javaValue = createLong(env, value);
    env->CallObjectMethod(map, putMethod, javaKey, javaValue);
    env->DeleteLocalRef(javaKey);
    env->DeleteLocalRef(javaValue);
    env->DeleteLocalRef(hashMapClass);
}

void putString(JNIEnv* env, jobject map, const char* key, const std::string& value) {
    jclass hashMapClass = env->GetObjectClass(map);
    jmethodID putMethod = env->GetMethodID(
        hashMapClass,
        "put",
        "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
    );
    jstring javaKey = env->NewStringUTF(key);
    jstring javaValue = env->NewStringUTF(value.c_str());
    env->CallObjectMethod(map, putMethod, javaKey, javaValue);
    env->DeleteLocalRef(javaKey);
    env->DeleteLocalRef(javaValue);
    env->DeleteLocalRef(hashMapClass);
}

}  // namespace

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, "MNN_DEBUG", "JNI_OnLoad");
    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM* vm, void* reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, "MNN_DEBUG", "JNI_OnUnload");
}

JNIEXPORT jlong JNICALL
Java_com_huajuan_aispace_network_LlmSession_initNative(
        JNIEnv* env,
        jobject thiz,
        jstring modelDir,
        jobject chatHistory,
        jstring mergeConfigStr,
        jstring configJsonStr) {
    const char* modelDirChars = env->GetStringUTFChars(modelDir, nullptr);
    const char* mergeConfigChars = env->GetStringUTFChars(mergeConfigStr, nullptr);
    const char* configJsonChars = env->GetStringUTFChars(configJsonStr, nullptr);

    std::string modelDirString = modelDirChars;
    json mergedConfig = json::parse(mergeConfigChars);
    json extraConfig = json::parse(configJsonChars);

    env->ReleaseStringUTFChars(modelDir, modelDirChars);
    env->ReleaseStringUTFChars(mergeConfigStr, mergeConfigChars);
    env->ReleaseStringUTFChars(configJsonStr, configJsonChars);

    std::vector<std::string> history;
    if (chatHistory != nullptr) {
        jclass listClass = env->GetObjectClass(chatHistory);
        jmethodID sizeMethod = env->GetMethodID(listClass, "size", "()I");
        jmethodID getMethod = env->GetMethodID(listClass, "get", "(I)Ljava/lang/Object;");
        jint listSize = env->CallIntMethod(chatHistory, sizeMethod);
        history.reserve(static_cast<size_t>(listSize));
        for (jint index = 0; index < listSize; ++index) {
            jobject element = env->CallObjectMethod(chatHistory, getMethod, index);
            const char* entryChars = env->GetStringUTFChars(static_cast<jstring>(element), nullptr);
            history.emplace_back(entryChars);
            env->ReleaseStringUTFChars(static_cast<jstring>(element), entryChars);
            env->DeleteLocalRef(element);
        }
        env->DeleteLocalRef(listClass);
    }

    MNN_DEBUG("createLLM BeginLoad %s", modelDirString.c_str());
    auto* llmSession = new mls::LlmSession(modelDirString, mergedConfig, extraConfig, history);
    llmSession->Load();
    MNN_DEBUG("LIFECYCLE: LlmSession CREATED at %p", llmSession);
    MNN_DEBUG("createLLM EndLoad %ld", reinterpret_cast<jlong>(llmSession));
    return reinterpret_cast<jlong>(llmSession);
}

JNIEXPORT jobject JNICALL
Java_com_huajuan_aispace_network_LlmSession_submitNative(
        JNIEnv* env,
        jobject thiz,
        jlong llmPtr,
        jstring inputStr,
        jboolean keepHistory,
        jobject progressListener) {
    jobject resultMap = createResultMap(env);
    auto* llm = reinterpret_cast<mls::LlmSession*>(llmPtr);
    if (!llm) {
        putString(env, resultMap, "error", "LLM session is not ready");
        return resultMap;
    }

    const char* inputChars = env->GetStringUTFChars(inputStr, nullptr);
    jclass progressListenerClass = env->GetObjectClass(progressListener);
    jmethodID onProgressMethod = env->GetMethodID(progressListenerClass, "onProgress", "(Ljava/lang/String;)Z");
    if (!onProgressMethod) {
        MNN_DEBUG("ProgressListener onProgress method not found.");
    }

    auto* context = llm->Response(inputChars, [&, progressListener, onProgressMethod](
            const std::string& response, bool isEop) {
        if (!progressListener || !onProgressMethod) {
            return false;
        }
        jstring javaString = isEop ? nullptr : env->NewStringUTF(response.c_str());
        jboolean userStopRequested = env->CallBooleanMethod(progressListener, onProgressMethod, javaString);
        if (javaString != nullptr) {
            env->DeleteLocalRef(javaString);
        }
        return static_cast<bool>(userStopRequested);
    });

    env->ReleaseStringUTFChars(inputStr, inputChars);
    env->DeleteLocalRef(progressListenerClass);

    if (context == nullptr) {
        putString(env, resultMap, "error", "Native response returned null context");
        return resultMap;
    }

    putLong(env, resultMap, "prompt_len", context->prompt_len);
    putLong(env, resultMap, "decode_len", context->gen_seq_len);
    putLong(env, resultMap, "vision_time", context->vision_us);
    putLong(env, resultMap, "audio_time", context->audio_us);
    putLong(env, resultMap, "prefill_time", context->prefill_us);
    putLong(env, resultMap, "decode_time", context->decode_us);
    return resultMap;
}

JNIEXPORT void JNICALL
Java_com_huajuan_aispace_network_LlmSession_resetNative(
        JNIEnv* env,
        jobject thiz,
        jlong objectPtr) {
    auto* llm = reinterpret_cast<mls::LlmSession*>(objectPtr);
    if (llm != nullptr) {
        MNN_DEBUG("RESET");
        llm->Reset();
    }
}

JNIEXPORT void JNICALL
Java_com_huajuan_aispace_network_LlmSession_releaseNative(
        JNIEnv* env,
        jobject thiz,
        jlong objectPtr) {
    MNN_DEBUG("LIFECYCLE: About to DESTROY LlmSession at %p", reinterpret_cast<void*>(objectPtr));
    auto* llm = reinterpret_cast<mls::LlmSession*>(objectPtr);
    delete llm;
    MNN_DEBUG("LIFECYCLE: LlmSessionInner DESTROYED at %p", reinterpret_cast<void*>(objectPtr));
}

}  // extern "C"
