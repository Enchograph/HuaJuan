package com.huajuan.aispace.data

import android.content.Context
import com.huajuan.aispace.network.Message as NetworkMessage
import com.huajuan.aispace.network.createImageMessage
import com.huajuan.aispace.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NetworkMessageBuilder {
    suspend fun build(
        context: Context,
        messages: List<Message>,
        systemPrompt: String,
        isImageGenerationService: Boolean
    ): List<NetworkMessage> = withContext(Dispatchers.IO) {
        if (isImageGenerationService) {
            val userMessages = messages.filter { it.isUser }
            if (userMessages.isEmpty()) {
                return@withContext listOf(NetworkMessage(role = "user", content = ""))
            }

            val allTexts = userMessages.joinToString(" ") { it.text }.trim().ifEmpty { "" }
            val allImageUris = userMessages.flatMap { it.imageUris }.distinct()
            return@withContext if (allImageUris.isNotEmpty()) {
                val imageUrls = allImageUris.mapNotNull { ImageUtils.prepareImageUrl(context, it) }
                listOf(createImageMessage(role = "user", text = allTexts, imageUris = imageUrls))
            } else {
                listOf(NetworkMessage(role = "user", content = allTexts))
            }
        }

        val networkMessages = mutableListOf<NetworkMessage>()
        if (systemPrompt.isNotBlank()) {
            networkMessages.add(NetworkMessage(role = "system", content = systemPrompt))
        }
        val currentUserMessage = messages.lastOrNull { it.isUser }
        val lastAssistantImageMessage = messages.lastOrNull { !it.isUser && it.imageUris.isNotEmpty() }
        for (message in messages) {
            val role = if (message.isUser) "user" else "assistant"
            if (message.isUser && message == currentUserMessage) {
                val userImages = message.imageUris
                val assistantImages = lastAssistantImageMessage?.imageUris ?: emptyList()
                val combinedImages = userImages + assistantImages
                val imageUrls = combinedImages.mapNotNull { ImageUtils.prepareImageUrl(context, it) }
                if (imageUrls.isNotEmpty()) {
                    val imageHint = buildString {
                        append("\n\n[Images]\n")
                        append("User images: ")
                        append(userImages.size)
                        append(", shown first in order.\n")
                        append("Assistant images: ")
                        append(assistantImages.size)
                        append(", shown after user images in order.\n")
                        append("Please distinguish them accordingly.")
                    }
                    networkMessages.add(
                        createImageMessage(
                            role = role,
                            text = message.text + imageHint,
                            imageUris = imageUrls
                        )
                    )
                } else {
                    networkMessages.add(NetworkMessage(role = role, content = message.text))
                }
            } else {
                networkMessages.add(NetworkMessage(role = role, content = message.text))
            }
        }
        return@withContext networkMessages
    }
}
