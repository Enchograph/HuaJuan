package com.huajuan.aispace.utils

import android.util.Log

inline fun debugLog(tag: String, message: () -> String) {
    if (Log.isLoggable(tag, Log.DEBUG)) {
        Log.d(tag, message())
    }
}
