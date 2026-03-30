package com.example.carabuff

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("FCM", "Message received")

        // 🔥 GET TITLE
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Carabuff"

        // 🔥 GET MESSAGE
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["message"]
            ?: "New notification"

        // 🔥 GET TYPE (IMPORTANT)
        val type = remoteMessage.data["type"] ?: "general"

        // 🔥 USE MAIN SYSTEM (SAVES TO FIRESTORE + SHOW NOTIF)
        val helper = NotificationHelper(this)
        helper.sendNotification(title, body, type)
    }

    // 🔥 OPTIONAL: TOKEN (FOR DEBUG / FUTURE)
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", token)
    }
}