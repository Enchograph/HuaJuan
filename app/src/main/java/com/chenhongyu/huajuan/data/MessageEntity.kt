package com.chenhongyu.huajuan.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Date,
    val imageUrisJson: String = "[]" // 存储图片URI列表为JSON字符串
) {
    // 将JSON字符串转换为图片URI列表
    fun getImageUris(): List<String> {
        return try {
            val listType = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(imageUrisJson, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // 从图片URI列表创建JSON字符串
    fun setImageUris(uris: List<String>): MessageEntity {
        val json = Gson().toJson(uris)
        return this.copy(imageUrisJson = json)
    }
}