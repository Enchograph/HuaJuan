package com.huajuan.aispace.screens.settings

import android.content.Context
import com.huajuan.aispace.R

sealed interface SettingsSubRoute {
    fun resolveTitle(context: Context): String

    data object Common : SettingsSubRoute {
        override fun resolveTitle(context: Context) = context.getString(R.string.settings_common_title)
    }
    data object ModelManagement : SettingsSubRoute {
        override fun resolveTitle(context: Context) = context.getString(R.string.settings_model_management_title)
    }
    data class VendorModels(val provider: String) : SettingsSubRoute {
        override fun resolveTitle(context: Context) = provider
    }
    data class NewModelForm(val provider: String) : SettingsSubRoute {
        override fun resolveTitle(context: Context) = context.getString(R.string.settings_new_model_title)
    }
    data object NewProviderForm : SettingsSubRoute {
        override fun resolveTitle(context: Context) = context.getString(R.string.settings_new_provider_title)
    }
    data object ToolSettings : SettingsSubRoute {
        override fun resolveTitle(context: Context) = context.getString(R.string.settings_tools_title)
    }
    data object Advanced : SettingsSubRoute {
        override fun resolveTitle(context: Context) = context.getString(R.string.settings_more_title)
    }
    data object Translation : SettingsSubRoute {
        override fun resolveTitle(context: Context) = context.getString(R.string.settings_translation_title)
    }
    data object DocumentProcessing : SettingsSubRoute {
        override fun resolveTitle(context: Context) = context.getString(R.string.settings_document_processing_title)
    }
    data object KnowledgeBase : SettingsSubRoute {
        override fun resolveTitle(context: Context) = context.getString(R.string.settings_knowledge_base_title)
    }
    data object KnowledgeBaseBasic : SettingsSubRoute {
        override fun resolveTitle(context: Context) = context.getString(R.string.settings_basic_title)
    }
    data object KnowledgeBaseIndexing : SettingsSubRoute {
        override fun resolveTitle(context: Context) = context.getString(R.string.settings_knowledge_indexing_title)
    }
    data object KnowledgeBaseRetrieval : SettingsSubRoute {
        override fun resolveTitle(context: Context) = context.getString(R.string.settings_knowledge_retrieval_title)
    }
    data object KnowledgeBaseAdvanced : SettingsSubRoute {
        override fun resolveTitle(context: Context) = context.getString(R.string.settings_knowledge_advanced_title)
    }
    data object KnowledgeBaseTaskCenter : SettingsSubRoute {
        override fun resolveTitle(context: Context) = context.getString(R.string.settings_knowledge_task_center)
    }
    data object WebSearch : SettingsSubRoute {
        override fun resolveTitle(context: Context) = context.getString(R.string.settings_web_search_title)
    }
    data object McpTools : SettingsSubRoute {
        override fun resolveTitle(context: Context) = context.getString(R.string.settings_mcp_title)
    }
    data object GlobalMemory : SettingsSubRoute {
        override fun resolveTitle(context: Context) = context.getString(R.string.settings_global_memory_title)
    }
    data object AssistantSettings : SettingsSubRoute {
        override fun resolveTitle(context: Context) = context.getString(R.string.assistant_settings)
    }
    data object About : SettingsSubRoute {
        override fun resolveTitle(context: Context) = context.getString(R.string.settings_about_title)
    }
    data object Debug : SettingsSubRoute {
        override fun resolveTitle(context: Context) = context.getString(R.string.settings_debug_title)
    }
    data object ModelDownload : SettingsSubRoute {
        override fun resolveTitle(context: Context) = context.getString(R.string.settings_model_download_title)
    }
}
