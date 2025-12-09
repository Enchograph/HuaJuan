# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.

# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name. The project name can be accessed via ${ PROJECT_NAME},
# Since this is the top level CMakeLists.txt, the project name is also accessible
# with ${CMAKE_PROJECT_NAME} (both CMake variables are in-sync within the top level
# build script scope).
project("mnnllmapp")

add_library(${CMAKE_PROJECT_NAME} SHARED
        # List C/C++ source files with relative paths to this CMakeLists.txt.
        llm_mnn_jni.cpp
        diffusion_jni.cpp
        diffusion_session.cpp
        llm_session.cpp
        crash_util.cpp
)

# Add 16KB page size support (required for Android 15+ devices)
target_link_options(${CMAKE_PROJECT_NAME} PRIVATE "-Wl,-z,max-page-size=16384")

#mnn
#set (MNN_SOURCE_ROOT "${CMAKE_SOURCE_DIR}/../../../../../../../")
set(MNN_SOURCE_ROOT "${CMAKE_SOURCE_DIR}/mnn")
#set (MNN_SOURCE_ROOT "/Users/songjinde/git/DAI/AliNNPrivate")
set(MNN_INSTALL_ROOT "${MNN_SOURCE_ROOT}/project/android/build_64")
# set (MNN_INSTALL_ROOT "/Users/songjinde/MNN/project/android/build_64")

#set (MNN_ROOT "${CMAKE_SOURCE_DIR}/../../../../../../../")
message(INFO "MNN_SOURCE_ROOT: ${MNN_SOURCE_ROOT}")
message(INFO "MNN_INSTALL_ROOT: ${MNN_INSTALL_ROOT}")

include_directories("${MNN_SOURCE_ROOT}/include/")

#diffusion
include_directories("${MNN_SOURCE_ROOT}/transformers/diffusion/engine/include/")

#llm
include_directories("${MNN_SOURCE_ROOT}/transformers/llm/engine/include")
#include_directories("${CMAKE_SOURCE_DIR}/include")

#audio
include_directories("${MNN_SOURCE_ROOT}/tools/audio/include")
include_directories("${CMAKE_SOURCE_DIR}/third_party")

# Add library paths
set(LIB_PATH "${MNN_INSTALL_ROOT}/lib")

# Link the shared library

add_library(MNN SHARED IMPORTED)
#set_target_properties(MNN PROPERTIES IMPORTED_LOCATION "${LIB_PATH}/libMNN.so")
set_target_properties(
        MNN
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libMNN.so
)

#add_library(llm SHARED IMPORTED)
#set_target_properties(llm PROPERTIES IMPORTED_LOCATION "${LIB_PATH}/libllm.so")
#
#add_library(diffusion SHARED IMPORTED)
#set_target_properties(diffusion PROPERTIES IMPORTED_LOCATION "${LIB_PATH}/libdiffusion.so")
#
#add_library(mnn_cl SHARED IMPORTED)
#set_target_properties(mnn_cl PROPERTIES IMPORTED_LOCATION "${LIB_PATH}/libMNN_CL.so")
#
#add_library(mnn_audio SHARED IMPORTED)
#set_target_properties(mnn_audio PROPERTIES IMPORTED_LOCATION "${LIB_PATH}/libMNNAudio.so")
#
#add_library(mnn_cv SHARED IMPORTED)
#set_target_properties(mnn_cv PROPERTIES IMPORTED_LOCATION "${LIB_PATH}/libMNNOpenCV.so")
#
#add_library(mnn_Express SHARED IMPORTED)
#set_target_properties(mnn_Express PROPERTIES IMPORTED_LOCATION "${LIB_PATH}/libMNN_Express.so")


# Link your native library with the `.so` file

# Specifies libraries CMake should link to your target library. You
# can link libraries from various origins, such as libraries defined in this
# build script, prebuilt third-party libraries, or Android system libraries.
target_link_libraries(${CMAKE_PROJECT_NAME}
        # List libraries link to the target library
        android
        log
        MNN
)


# ==========================================
# MNN TTS libmnn_tts.so
# ==========================================

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

option(BUILD_BERTVITS2 "Build BertVit2 TTS " ON)
option(BUILD_PIPER "Build PIPER TTS " OFF)
option(BUILD_ANDROID "Build for Android" OFF)

if(ANDROID)
    set(BUILD_ANDROID ON CACHE BOOL "Build for Android" FORCE)
endif()


set(MNN_TTS_SOURCE_ROOT "${CMAKE_SOURCE_DIR}/mnn_tts")

include_directories(
        ${MNN_TTS_SOURCE_ROOT}/include
        ${CMAKE_SOURCE_DIR}/third_party/include
)

if(BUILD_BERTVITS2)
    include_directories(
            ${MNN_TTS_SOURCE_ROOT}/include/bertvits2
    )
endif()

set(SHARED_SOURCE_FILES
        ${MNN_TTS_SOURCE_ROOT}/src/mnn_tts_config.cpp
        ${MNN_TTS_SOURCE_ROOT}/src/mnn_tts_sdk.cpp
        ${MNN_TTS_SOURCE_ROOT}/src/mnn_tts_logger.cpp
)

set(BERTVITS2_SOURCE_FILES
        ${MNN_TTS_SOURCE_ROOT}/src/bertvits2/an_to_cn.cpp
        ${MNN_TTS_SOURCE_ROOT}/src/bertvits2/utils.cpp
        ${MNN_TTS_SOURCE_ROOT}/src/bertvits2/mnn_bertvits2_tts_impl.cpp
        ${MNN_TTS_SOURCE_ROOT}/src/bertvits2/text_preprocessor.cpp
        ${MNN_TTS_SOURCE_ROOT}/src/bertvits2/pinyin.cpp
        ${MNN_TTS_SOURCE_ROOT}/src/bertvits2/chinese_g2p.cpp
        ${MNN_TTS_SOURCE_ROOT}/src/bertvits2/chinese_bert.cpp
        ${MNN_TTS_SOURCE_ROOT}/src/bertvits2/english_bert.cpp
        ${MNN_TTS_SOURCE_ROOT}/src/bertvits2/english_g2p.cpp
        ${MNN_TTS_SOURCE_ROOT}/src/bertvits2/tone_adjuster.cpp
        ${MNN_TTS_SOURCE_ROOT}/src/bertvits2/tts_generator.cpp
        ${MNN_TTS_SOURCE_ROOT}/src/bertvits2/word_spliter.cpp
)


set(ANDROID_SOURCE_FILES
        ${MNN_TTS_SOURCE_ROOT}/src/android/tts_service.cpp
        ${MNN_TTS_SOURCE_ROOT}/src/android/tts_service_jni.cpp
)


add_library(mnn_tts SHARED ${BERTVITS2_SOURCE_FILES} ${SHARED_SOURCE_FILES} ${ANDROID_SOURCE_FILES})

target_link_libraries(mnn_tts log MNN)

