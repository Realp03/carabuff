package com.example.carabuff

data class NotificationModel(
    val id: String = "", // 🔥 IMPORTANT (Firestore document ID)
    val title: String = "",
    val message: String = "",
    val type: String = "",
    val timestamp: Long = 0,
    val isRead: Boolean = false
)