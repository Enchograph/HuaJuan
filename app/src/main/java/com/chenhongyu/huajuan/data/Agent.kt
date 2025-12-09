package com.chenhongyu.huajuan.data

/**
 * 智能体（Agent）数据类
 */
data class Agent(
    val id: String,
    val name: String,  // 角色名称
    val description: String,
    val systemPrompt: String,  // 系统提示词
    val category: String,
    val iconResId: Int // 图标资源ID，暂时用Int表示，后续可以改为图片URL
)