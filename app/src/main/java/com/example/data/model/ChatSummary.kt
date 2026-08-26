package com.example.data.model

data class ChatSummary(
    val chatId: String = "",
    val otherUser: User = User(),
    val lastMessage: String = "",
    val lastTimestamp: Long = 0L,
    val unreadCount: Int = 0,
    val lastSenderId: String = ""
)
