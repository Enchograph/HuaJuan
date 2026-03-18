package com.huajuan.aispace.data

object ToolIds {
    const val ImageGeneration = "tool_image_generation"
    const val Translation = "tool_translation"

    val toolConversationIds: Set<String> = setOf(ImageGeneration, Translation)
}
