package com.huajuan.aispace.utils

import com.huajuan.aispace.data.Message
import java.util.Date

sealed class ExpandedMessageItem {
    abstract val originalMessageId: String
    abstract val isUser: Boolean
    abstract val timestamp: Date

    data class TextItem(
        val message: Message,
        override val originalMessageId: String = message.id,
        override val isUser: Boolean = message.isUser,
        override val timestamp: Date = message.timestamp
    ) : ExpandedMessageItem()

    data class ImageItemData(
        val imageUri: String,
        val index: Int,
        override val originalMessageId: String,
        override val isUser: Boolean,
        override val timestamp: Date
    ) : ExpandedMessageItem()

    data class FileItemData(
        val attachment: com.huajuan.aispace.data.FileAttachment,
        val index: Int,
        override val originalMessageId: String,
        override val isUser: Boolean,
        override val timestamp: Date
    ) : ExpandedMessageItem()
}

fun expandMessages(messages: List<Message>): List<ExpandedMessageItem> {
    val expandedItems = mutableListOf<ExpandedMessageItem>()

    for (message in messages) {
        val hasText = message.text.isNotBlank()
        val hasImages = message.imageUris.isNotEmpty()
        val hasFiles = message.attachments.isNotEmpty()

        when {
            hasText && (hasImages || hasFiles) -> {
                expandedItems.add(ExpandedMessageItem.TextItem(message))
                message.imageUris.forEachIndexed { index, uri ->
                    expandedItems.add(
                        ExpandedMessageItem.ImageItemData(
                            imageUri = uri,
                            index = index,
                            originalMessageId = message.id,
                            isUser = message.isUser,
                            timestamp = message.timestamp
                        )
                    )
                }
                message.attachments.forEachIndexed { index, attachment ->
                    expandedItems.add(
                        ExpandedMessageItem.FileItemData(
                            attachment = attachment,
                            index = index,
                            originalMessageId = message.id,
                            isUser = message.isUser,
                            timestamp = message.timestamp
                        )
                    )
                }
            }
            hasText -> {
                expandedItems.add(ExpandedMessageItem.TextItem(message))
            }
            hasImages || hasFiles -> {
                message.imageUris.forEachIndexed { index, uri ->
                    expandedItems.add(
                        ExpandedMessageItem.ImageItemData(
                            imageUri = uri,
                            index = index,
                            originalMessageId = message.id,
                            isUser = message.isUser,
                            timestamp = message.timestamp
                        )
                    )
                }
                message.attachments.forEachIndexed { index, attachment ->
                    expandedItems.add(
                        ExpandedMessageItem.FileItemData(
                            attachment = attachment,
                            index = index,
                            originalMessageId = message.id,
                            isUser = message.isUser,
                            timestamp = message.timestamp
                        )
                    )
                }
            }
        }
    }

    return expandedItems
}
