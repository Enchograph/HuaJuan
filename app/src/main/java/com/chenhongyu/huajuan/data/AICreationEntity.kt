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
    val promptHtml: String,
    val promptJson: String?,
    val imageFileName: String?,
    val width: Int = 1024,
    val height: Int = 1024,
    val status: String = "PENDING",
    val createdAt: Long,
    val updatedAt: Long,
    val extraJson: String? = null
)
