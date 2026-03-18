package com.huajuan.aispace.utils

import android.content.Context
import com.huajuan.aispace.R
import com.huajuan.aispace.i18n.LocalizedResources
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatTime(date: Date): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(date)
}

fun formatTimeAgo(context: Context, epochMillis: Long): String {
    val diff = System.currentTimeMillis() - epochMillis
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        days > 0 -> LocalizedResources.getString(context, R.string.time_days_ago, null, days.toInt())
        hours > 0 -> LocalizedResources.getString(context, R.string.time_hours_ago, null, hours.toInt())
        minutes > 0 -> LocalizedResources.getString(context, R.string.time_minutes_ago, null, minutes.toInt())
        else -> LocalizedResources.getString(context, R.string.status_just_now)
    }
}

fun formatConversationTime(context: Context, date: Date): String {
    val now = Date()
    val diff = now.time - date.time
    val minutes = diff / 60000
    val hours = diff / 3600000
    val days = diff / 86400000
    return when {
        days > 0 -> LocalizedResources.getString(context, R.string.time_days_ago, null, days.toInt())
        hours > 0 -> LocalizedResources.getString(context, R.string.time_hours_ago, null, hours.toInt())
        minutes > 0 -> LocalizedResources.getString(context, R.string.time_minutes_ago, null, minutes.toInt())
        else -> LocalizedResources.getString(context, R.string.status_just_now)
    }
}
