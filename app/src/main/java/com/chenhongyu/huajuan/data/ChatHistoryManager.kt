package com.chenhongyu.huajuan.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class ChatHistoryManager(private val context: Context) {
    private val gson = Gson()
    private val prefs = context.getSharedPreferences("chat_history", Context.MODE_PRIVATE)
    
    companion object {
        private const val CONVERSATIONS_KEY = "conversations"
        private const val MESSAGES_PREFIX = "messages_"
    }
    
    // 保存对话列表
    fun saveConversations(conversations: List<Conversation>) {
        val json = gson.toJson(conversations)
        prefs.edit().putString(CONVERSATIONS_KEY, json).apply()
    }
    
    // 获取对话列表
    fun getConversations(): List<Conversation> {
        val json = prefs.getString(CONVERSATIONS_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<List<Conversation>>() {}.type
            gson.fromJson(json, type)
        } else {
            // 默认返回空列表
            emptyList()
        }
    }
    
    // 保存特定对话的消息
    fun saveMessages(conversationId: String, messages: List<Message>) {
        val json = gson.toJson(messages)
        prefs.edit().putString("${MESSAGES_PREFIX}$conversationId", json).apply()
    }
    
    // 获取特定对话的消息
    fun getMessages(conversationId: String): List<Message> {
        val json = prefs.getString("${MESSAGES_PREFIX}$conversationId", null)
        return if (json != null) {
            val type = object : TypeToken<List<Message>>() {}.type
            gson.fromJson(json, type)
        } else {
            // 默认返回空列表
            emptyList()
        }
    }
    
    // 删除特定对话及其消息
    fun deleteConversation(conversationId: String) {
        prefs.edit().remove("${MESSAGES_PREFIX}$conversationId").apply()
        
        // 更新对话列表
        val conversations = getConversations().toMutableList()
        val iterator = conversations.iterator()
        while (iterator.hasNext()) {
            val conversation = iterator.next()
            if (conversation.id == conversationId) {
                iterator.remove()
                break
            }
        }
        saveConversations(conversations)
    }
    
    // 创建新对话
    fun createNewConversation(title: String): Conversation {
        val conversations = getConversations().toMutableList()
        val newId = java.util.UUID.randomUUID().toString()
        val newConversation = Conversation(
            id = newId,
            title = title,
            lastMessage = "",
            timestamp = Date()
        )
        conversations.add(newConversation)
        saveConversations(conversations)
        return newConversation
    }
    
    // 更新对话的最后消息
    fun updateLastMessage(conversationId: String, lastMessage: String) {
        val conversations = getConversations().toMutableList()
        val updatedConversations = conversations.map { conversation ->
            if (conversation.id == conversationId) {
                conversation.copy(
                    lastMessage = lastMessage,
                    timestamp = Date()
                )
            } else {
                conversation
            }
        }
        saveConversations(updatedConversations)
    }
}