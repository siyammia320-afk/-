package com.example.data.model

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderPhoto: String = "",
    val receiverId: String = "",
    val chatId: String = "global",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val seen: Boolean = false,
    val readBy: Map<String, Boolean> = emptyMap(),
    val replyToId: String = "",
    val replyToSender: String = "",
    val replyToText: String = ""
) {
    val isReplied: Boolean get() = replyToId.isNotBlank() && replyToText.isNotBlank()
}
