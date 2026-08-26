package com.example.data.model

data class User(
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val photoUrl: String = "",
    val bio: String = "Hello! I am using BD Chat.",
    val online: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

