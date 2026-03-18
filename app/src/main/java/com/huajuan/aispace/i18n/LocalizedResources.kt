package com.huajuan.aispace.i18n

import android.content.Context
import androidx.annotation.StringRes

object LocalizedResources {
    fun getString(
        context: Context,
        @StringRes resId: Int,
        localeOverride: String? = null,
        vararg args: Any
    ): String {
        val localizedContext = AppLocaleManager.getLocalizedContext(context, localeOverride)
        return localizedContext.getString(resId, *args)
    }
}
