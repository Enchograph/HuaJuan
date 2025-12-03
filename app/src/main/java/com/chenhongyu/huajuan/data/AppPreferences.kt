package com.chenhongyu.huajuan.data

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // 云服务设置
    var useCloudModel: Boolean
        get() = prefs.getBoolean("use_cloud_model", true)
        set(value) = prefs.edit().putBoolean("use_cloud_model", value).apply()

    // 服务提供商
    var serviceProvider: String
        get() = prefs.getString("service_provider", "OpenAI") ?: "OpenAI"
        set(value) = prefs.edit().putString("service_provider", value).apply()

    // 自定义API地址
    var customApiUrl: String
        get() = prefs.getString("custom_api_url", "") ?: ""
        set(value) = prefs.edit().putString("custom_api_url", value).apply()

    // API密钥
    var apiKey: String
        get() = prefs.getString("api_key", "") ?: ""
        set(value) = prefs.edit().putString("api_key", value).apply()

    // 选择的模型
    var selectedModel: String
        get() = prefs.getString("selected_model", "gpt-3.5-turbo") ?: "gpt-3.5-turbo"
        set(value) = prefs.edit().putString("selected_model", value).apply()

    // 深色模式
    var darkMode: Boolean
        get() = prefs.getBoolean("dark_mode", false)
        set(value) = prefs.edit().putBoolean("dark_mode", value).apply()
}