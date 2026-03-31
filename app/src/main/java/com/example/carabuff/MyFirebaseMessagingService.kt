package com.example.carabuff

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("FCM", "Message received")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Carabuff"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["message"]
            ?: "New notification"

        val type = remoteMessage.data["type"] ?: "general"
        val target = remoteMessage.data["target"] ?: "home"
        val summaryDate = remoteMessage.data["summaryDate"]

        NotificationHelper.showNotification(
            context = applicationContext,
            title = title,
            message = body,
            type = type,
            target = target,
            saveToDb = false,
            summaryDate = summaryDate
        )
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        Log.d("FCM_TOKEN", "New token: $token")
        saveTokenToFirestore(token)
    }

    private fun saveTokenToFirestore(token: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        val data = hashMapOf(
            "fcmToken" to token,
            "tokenUpdatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("users")
            .document(user.uid)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("FCM_TOKEN", "Token saved")
            }
            .addOnFailureListener { e ->
                Log.e("FCM_TOKEN", "Failed to save token", e)
            }
    }
}