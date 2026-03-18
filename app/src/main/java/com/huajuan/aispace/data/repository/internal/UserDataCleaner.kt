package com.huajuan.aispace.data.repository.internal

import android.util.Log
import com.huajuan.aispace.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

internal class UserDataCleaner(
    private val database: AppDatabase,
    private val preferenceStore: AppPreferenceStore
) {
    private companion object {
        const val TAG = "UserDataCleaner"
    }

    suspend fun clearUserDataExceptApiKeys() {
        withContext(Dispatchers.IO) {
            try {
                clearMessagesAndConversations()
                clearPrefsExceptApiAndCoreSettings()
            } catch (e: Exception) {
                Log.e(TAG, "clearUserDataExceptApiKeys failed", e)
                throw e
            }
        }
    }

    private suspend fun clearMessagesAndConversations() {
        val conversationDao = database.conversationDao()
        val messageDao = database.messageDao()
        runCatching {
            val convs = conversationDao.getAllConversations().first()
            convs.forEach { conv ->
                runCatching { messageDao.deleteMessagesByConversationId(conv.id) }
            }
        }
        runCatching {
            val convs = conversationDao.getAllConversations().first()
            convs.forEach { conv ->
                runCatching { conversationDao.deleteConversationById(conv.id) }
            }
        }
    }


    private fun clearPrefsExceptApiAndCoreSettings() {
        val keysToKeepPrefixes = listOf(
            "api_key_",
            "service_provider",
            "custom_service_providers",
            "custom_provider_url_"
        )
        val fixedKeys = setOf(
            "dark_mode",
            "debug_mode",
            "token_animation_fixed_duration_enabled",
            "token_animation_fixed_duration_ms"
        )
        preferenceStore.removeKeys { key ->
            val keep = keysToKeepPrefixes.any { pref -> key.startsWith(pref) } || fixedKeys.contains(key)
            !keep
        }
    }

}
