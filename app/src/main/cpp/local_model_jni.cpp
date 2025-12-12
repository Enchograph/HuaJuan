#include <android/asset_manager_jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <jni.h>
#include <string>
#include <utility>
#include <vector>
#include <thread>
#include <mutex>
#include <ostream>
#include <sstream>
#include <chrono>
#include "mls_log.h"
#include "MNN/expr/ExecutorScope.hpp"
#include "nlohmann/json.hpp"
#include "llm_stream_buffer.hpp"
#include "utf8_stream_processor.hpp"
#include "llm_session.h"

using MNN::Transformer::Llm;
using json = nlohmann::json;

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_chenhongyu_huajuan_data_LocalModelApiService_initNative(JNIEnv *env,
                                                                jobject thiz,
                                                                jstring configPath,
                                                                jobject history,
                                                                jstring mergedConfigStr,
                                                                jstring configJsonStr) {
    const char *model_dir = env->GetStringUTFChars(configPath, nullptr);
    auto model_dir_str = std::string(model_dir);
    const char *config_json_cstr = env->GetStringUTFChars(configJsonStr, nullptr);
    const char *merged_config_cstr = env->GetStringUTFChars(mergedConfigStr, nullptr);
    json merged_config = json::parse(merged_config_cstr);
    json extra_json_config = json::parse(config_json_cstr);
    env->ReleaseStringUTFChars(configPath, model_dir);
    env->ReleaseStringUTFChars(configJsonStr, config_json_cstr);
    env->ReleaseStringUTFChars(mergedConfigStr, merged_config_cstr);
    MNN_DEBUG("createLLM BeginLoad %s", model_dir);
    std::vector<std::string> history_vec;
    history_vec.clear();
    if (history != nullptr) {
        jclass listClass = env->GetObjectClass(history);
        jmethodID sizeMethod = env->GetMethodID(listClass, "size", "()I");
        jmethodID getMethod = env->GetMethodID(listClass, "get", "(I)Ljava/lang/Object;");
        jint listSize = env->CallIntMethod(history, sizeMethod);
        for (jint i = 0; i < listSize; i++) {
            jobject element = env->CallObjectMethod(history, getMethod, i);
            const char *elementCStr = env->GetStringUTFChars((jstring) element, nullptr);
            history_vec.emplace_back(elementCStr);
            env->ReleaseStringUTFChars((jstring) element, elementCStr);
            env->DeleteLocalRef(element);
        }
    }
    auto llm_session = new mls::LlmSession(model_dir_str, merged_config, extra_json_config,
                                           history_vec);
    llm_session->Load();
    MNN_DEBUG("LIFECYCLE: LlmSession CREATED at %p", llm_session);
    MNN_DEBUG("createLLM EndLoad %ld ", reinterpret_cast<jlong>(llm_session));
    return reinterpret_cast<jlong>(llm_session);
}

JNIEXPORT jobject JNICALL
Java_com_chenhongyu_huajuan_data_LocalModelApiService_submitNative(JNIEnv *env,
                                                                  jobject thiz,
                                                                  jlong llmPtr,
                                                                  jstring inputStr,
                                                                  jboolean keepHistory,
                                                                  jobject progressListener) {
    auto *llm = reinterpret_cast<mls::LlmSession *>(llmPtr);
    if (!llm) {
        return env->NewStringUTF("Failed, Chat is not ready!");
    }
    const char *input_str = env->GetStringUTFChars(inputStr, nullptr);
    jclass progressListenerClass = env->GetObjectClass(progressListener);
    jmethodID onProgressMethod = env->GetMethodID(progressListenerClass, "onProgress",
                                                  "(Ljava/lang/String;)Z");
    if (!onProgressMethod) {
        MNN_DEBUG("ProgressListener onProgress method not found.");
    }
    auto *context = llm->Response(input_str, [&, progressListener, onProgressMethod](
            const std::string &response, bool is_eop) {
        if (progressListener && onProgressMethod) {
            jstring javaString = is_eop ? nullptr : env->NewStringUTF(response.c_str());
            jboolean user_stop_requested = env->CallBooleanMethod(progressListener,
                                                                  onProgressMethod, javaString);
            env->DeleteLocalRef(javaString);
            return (bool) user_stop_requested;
        } else {
            return true;
        }
    });
    int64_t prompt_len = 0;
    int64_t decode_len = 0;
    int64_t vision_time = 0;
    int64_t audio_time = 0;
    int64_t prefill_time = 0;
    int64_t decode_time = 0;
    prompt_len += context->prompt_len;
    decode_len += context->gen_seq_len;
    vision_time += context->vision_us;
    audio_time += context->audio_us;
    prefill_time += context->prefill_us;
    decode_time += context->decode_us;
    jclass hashMapClass = env->FindClass("java/util/HashMap");
    jmethodID hashMapInit = env->GetMethodID(hashMapClass, "<init>", "()V");
    jmethodID putMethod = env->GetMethodID(hashMapClass, "put",
                                           "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    jobject hashMap = env->NewObject(hashMapClass, hashMapInit);

    // Add metrics to the HashMap
    env->CallObjectMethod(hashMap, putMethod, env->NewStringUTF("prompt_len"),
                          env->NewObject(env->FindClass("java/lang/Long"),
                                         env->GetMethodID(env->FindClass("java/lang/Long"),
                                                          "<init>", "(J)V"), prompt_len));
    env->CallObjectMethod(hashMap, putMethod, env->NewStringUTF("decode_len"),
                          env->NewObject(env->FindClass("java/lang/Long"),
                                         env->GetMethodID(env->FindClass("java/lang/Long"),
                                                          "<init>", "(J)V"), decode_len));
    env->CallObjectMethod(hashMap, putMethod, env->NewStringUTF("vision_time"),
                          env->NewObject(env->FindClass("java/lang/Long"),
                                         env->GetMethodID(env->FindClass("java/lang/Long"),
                                                          "<init>", "(J)V"), vision_time));
    env->CallObjectMethod(hashMap, putMethod, env->NewStringUTF("audio_time"),
                          env->NewObject(env->FindClass("java/lang/Long"),
                                         env->GetMethodID(env->FindClass("java/lang/Long"),
                                                          "<init>", "(J)V"), audio_time));
    env->CallObjectMethod(hashMap, putMethod, env->NewStringUTF("prefill_time"),
                          env->NewObject(env->FindClass("java/lang/Long"),
                                         env->GetMethodID(env->FindClass("java/lang/Long"),
                                                          "<init>", "(J)V"), prefill_time));
    env->CallObjectMethod(hashMap, putMethod, env->NewStringUTF("decode_time"),
                          env->NewObject(env->FindClass("java/lang/Long"),
                                         env->GetMethodID(env->FindClass("java/lang/Long"),
                                                          "<init>", "(J)V"), decode_time));
    return hashMap;
}

JNIEXPORT void JNICALL
Java_com_chenhongyu_huajuan_data_LocalModelApiService_releaseNative(JNIEnv *env,
                                                                   jobject thiz,
                                                                   jlong objectPtr) {
    MNN_DEBUG("LIFECYCLE: About to DESTROY LlmSession at %p", reinterpret_cast<void*>(objectPtr));
    auto *llm = reinterpret_cast<mls::LlmSession *>(objectPtr);
    delete llm;
    MNN_DEBUG("LIFECYCLE: LlmSessionInner DESTROYED at %p", reinterpret_cast<void*>(objectPtr));
}

} // extern "C"