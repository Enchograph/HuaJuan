package com.huajuan.aispace.i18n

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

object AppLocaleManager {
    const val SYSTEM = "system"
    const val ZH_HANS = "zh-Hans"
    const val EN = "en"
    const val JA = "ja"

    val supportedLanguages: Set<String> = setOf(SYSTEM, ZH_HANS, EN, JA)

    fun normalizeLanguage(raw: String?): String {
        return when (raw?.trim()) {
            null, "", SYSTEM -> SYSTEM
            "zh", "zh-CN", "zh-Hans", "zh-Hant", "zh-TW", "zh-HK" -> ZH_HANS
            "en", "en-US", "en-GB" -> EN
            "ja", "ja-JP" -> JA
            else -> if (supportedLanguages.contains(raw)) raw else SYSTEM
        }
    }

    fun applyAppLanguage(context: Context, rawLanguage: String?) {
        val language = normalizeLanguage(rawLanguage)
        val localeManager = context.getSystemService(LocaleManager::class.java) ?: return
        localeManager.applicationLocales = when (language) {
            SYSTEM -> LocaleList.getEmptyLocaleList()
            else -> LocaleList.forLanguageTags(toLanguageTag(language))
        }
    }

    fun getLocalizedContext(context: Context, rawLanguage: String? = null): Context {
        val language = normalizeLanguage(rawLanguage)
        if (language == SYSTEM) return context
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(Locale.forLanguageTag(toLanguageTag(language))))
        return context.createConfigurationContext(config)
    }

    fun supportedContentLanguages(): List<String> = listOf(ZH_HANS, EN, JA)

    private fun toLanguageTag(language: String): String = when (language) {
        ZH_HANS -> "zh-Hans"
        EN -> "en"
        JA -> "ja"
        else -> Locale.forLanguageTag(language).toLanguageTag()
    }
}
