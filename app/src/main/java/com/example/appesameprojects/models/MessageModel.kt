package com.example.appesameprojects.models

data class MessageModel(
    val messageId: String = "",
    val senderId: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val receiverId: String = "",
    val chatId: String = "",
)
