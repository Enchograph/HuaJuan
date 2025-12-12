package com.chenhongyu.huajuan.stream

sealed class ChatEvent {
    data class Chunk(val text: String) : ChatEvent()
    data class Error(val message: String) : ChatEvent()
    object Done : ChatEvent()
}

