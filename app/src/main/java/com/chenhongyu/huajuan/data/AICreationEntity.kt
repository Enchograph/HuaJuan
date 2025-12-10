package com.chenhongyu.huajuan.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_creations")
data class AICreationEntity(
    @PrimaryKey
    val id: String,
    val username: String?,
    val userSignature: String?,
    val aiRoleName: String?,
    val aiModelName: String?,
    val title: String? = null, // 帖子标题
    val commentary: String? = null, // 帖子吐槽内容 / 用户感想
    val promptHtml: String,
    val promptJson: String?,
    val conversationText: String? = null, // 原始对话文本存档
    val conversationAt: Long? = null, // 对话时间（开始时间）
    val publishedAt: Long? = null, // 帖子发布时间
    val imageFileName: String?,
    val width: Int = 1024,
    val height: Int = 1024,
    val status: String = "PENDING",
    val createdAt: Long,
    val updatedAt: Long,
    val extraJson: String? = null
)
