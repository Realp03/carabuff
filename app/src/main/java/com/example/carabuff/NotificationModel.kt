package com.example.carabuff

data class NotificationModel(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "",
    val target: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false,
    val summaryDate: String = ""
)