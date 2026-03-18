package com.huajuan.aispace.data.repository.internal

internal class AppSettingsStore(
    private val preferenceStore: AppPreferenceStore
) {
    fun getDarkMode(): Boolean = preferenceStore.getDarkMode()

    fun setDarkMode(isDarkMode: Boolean) {
        preferenceStore.setDarkMode(isDarkMode)
    }

    fun getThemeMode(): String = preferenceStore.getThemeMode()

    fun setThemeMode(themeMode: String) {
        preferenceStore.setThemeMode(themeMode)
    }

    fun getFontScale(): Float = preferenceStore.getFontScale()

    fun setFontScale(fontScale: Float) {
        preferenceStore.setFontScale(fontScale)
    }

    fun getTransparencyLevel(): String = preferenceStore.getTransparencyLevel()

    fun setTransparencyLevel(level: String) {
        preferenceStore.setTransparencyLevel(level)
    }

    fun getDebugMode(): Boolean = preferenceStore.getDebugMode()

    fun setDebugMode(isDebugMode: Boolean) {
        preferenceStore.setDebugMode(isDebugMode)
    }

    fun getTokenAnimationFixedDurationEnabled(): Boolean =
        preferenceStore.getTokenAnimationFixedDurationEnabled()

    fun setTokenAnimationFixedDurationEnabled(enabled: Boolean) {
        preferenceStore.setTokenAnimationFixedDurationEnabled(enabled)
    }

    fun getTokenAnimationFixedDurationMs(): Int =
        preferenceStore.getTokenAnimationFixedDurationMs()

    fun setTokenAnimationFixedDurationMs(durationMs: Int) {
        preferenceStore.setTokenAnimationFixedDurationMs(durationMs)
    }

    fun getLanguage(): String = preferenceStore.getLanguage()

    fun setLanguage(language: String) {
        preferenceStore.setLanguage(language)
    }

    fun getNotificationsEnabled(): Boolean = preferenceStore.getNotificationsEnabled()

    fun setNotificationsEnabled(enabled: Boolean) {
        preferenceStore.setNotificationsEnabled(enabled)
    }

    fun getCustomApiUrl(): String = preferenceStore.getCustomApiUrl()

    fun setCustomApiUrl(customApiUrl: String) {
        preferenceStore.setCustomApiUrl(customApiUrl)
    }
}
