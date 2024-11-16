package com.example.appesameprojects.models

data class UserModel(
    var fcmToken: String = "",
    var userId: String = "",
    val name: String? = null,
    val email: String? = null,
    val accountType: String? = null,
)
