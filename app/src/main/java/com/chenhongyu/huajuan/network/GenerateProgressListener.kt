package com.chenhongyu.huajuan.network

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