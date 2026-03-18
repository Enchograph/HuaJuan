plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.kapt")
}

val enableNativeBuild = (findProperty("enableNativeBuild") as String?)
    ?.toBooleanStrictOrNull()
    ?: false

android {
    namespace = "com.huajuan.aispace"
    compileSdk {
        version = release(36)
    }
    if (enableNativeBuild) {
        ndkVersion = "27.2.12479018"
    }

    defaultConfig {
        applicationId = "com.huajuan.aispace"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        if (enableNativeBuild) {
            // 仅在显式开启时执行原生编译，默认使用预编译 so
            externalNativeBuild {
                cmake {
                    cppFlags("-std=c++17")
                    arguments("-DANDROID_STL=c++_shared")
                }
            }
        }
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        // 启用viewBinding以支持原生库调用
        viewBinding = true
    }
    
    if (enableNativeBuild) {
        // 配置外部原生构建
        externalNativeBuild {
            cmake {
                path = file("src/main/cpp/CMakeLists.txt")
                version = "3.22.1"
            }
        }
    }
}

val prebuiltMnnLlmSo = layout.projectDirectory.file("src/main/jniLibs/arm64-v8a/libmnnllmapp.so")
val verifyPrebuiltNativeLib by tasks.registering {
    group = "verification"
    description = "Verify required prebuilt native libraries exist when native build is disabled."
    doLast {
        if (!prebuiltMnnLlmSo.asFile.exists()) {
            throw GradleException(
                "缺少预编译库: ${prebuiltMnnLlmSo.asFile}. " +
                    "请先执行一次 -PenableNativeBuild=true 构建并将 libmnnllmapp.so 放入该目录。"
            )
        }
    }
}

if (!enableNativeBuild) {
    tasks.named("preBuild").configure {
        dependsOn(verifyPrebuiltNativeLib)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.documentfile:documentfile:1.0.1")
    
    // 添加网络请求库
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    
    // 添加Gson库用于JSON序列化
    implementation("com.google.code.gson:gson:2.10.1")
    
    // 添加Markdown渲染库
    implementation("com.mikepenz:multiplatform-markdown-renderer:0.29.0")
    implementation("com.mikepenz:multiplatform-markdown-renderer-m3:0.29.0")
    implementation("com.mikepenz:multiplatform-markdown-renderer-coil3:0.29.0")
    
    // Coil 3 图片加载库（完整版，带OkHttp网络支持）
    implementation("io.coil-kt.coil3:coil:3.0.4")
    implementation("io.coil-kt.coil3:coil-compose:3.0.4")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")
    
    // 添加Room数据库依赖
    implementation("androidx.room:room-runtime:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")
    kapt("androidx.room:room-compiler:2.7.0")
    
    // WorkManager for background generation
    implementation("androidx.work:work-runtime-ktx:2.8.1")

    // 添加模糊效果库
    implementation("com.github.skydoves:cloudy:0.2.7")
    
    testImplementation(libs.junit)
    testImplementation("com.squareup.okhttp3:mockwebserver:4.9.3")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
