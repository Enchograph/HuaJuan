package com.huajuan.aispace.data

import android.content.Context

/**
 * 智能体提供者
 * 提供20个覆盖效率、编程、写作、生活、学习、娱乐等高频场景的实用Agent
 */
class AgentProvider(
    private val context: Context
) {
    fun getAgents(): List<Agent> {
        return BuiltInAssistants.resolveAgents(context)
    }

    fun getCategories(): List<String> {
        return BuiltInAssistants.categoryIds()
    }

    fun getCategoryLabel(categoryId: String): String {
        return BuiltInAssistants.resolveCategoryLabel(context, categoryId)
    }
}
