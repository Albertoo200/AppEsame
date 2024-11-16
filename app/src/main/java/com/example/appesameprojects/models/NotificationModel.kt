package com.example.appesameprojects.models

data class NotificationModel(
    val notificationId: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),

    val closed : String = "false",

    val receiverId: String = "",
    val senderId:  String = "",
)
