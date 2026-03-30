package com.example.carabuff

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

class NotificationHelper(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    fun sendNotification(title: String, message: String, type: String) {

        if (userId == null) return

        val timestamp = System.currentTimeMillis()

        // 🔥 MAS SAFE NOTIFICATION ID (iwas duplicate/crash)
        val notifId = Random.nextInt(100000, 999999)

        val data = hashMapOf(
            "userId" to userId,
            "title" to title,
            "message" to message,
            "type" to type,
            "timestamp" to timestamp,
            "isRead" to false
        )

        // 🔥 SAVE TO FIREBASE
        db.collection("notifications")
            .add(data)
            .addOnSuccessListener {
                // optional log
            }
            .addOnFailureListener {
                // optional error handling
            }

        // 🔥 SHOW LOCAL NOTIFICATION
        showLocalNotification(title, message, type, notifId)
    }

    private fun showLocalNotification(
        title: String,
        message: String,
        type: String,
        notifId: Int
    ) {

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "carabuff_channel"

        // 🔥 CREATE CHANNEL (ANDROID 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Carabuff Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        // 🔥 NAVIGATION INTENT
        val intent = when (type) {
            "daily_summary" -> Intent(context, DailySummaryActivity::class.java)
            "achievement" -> Intent(context, AnalyticsActivity::class.java)
            "workout", "food" -> Intent(context, HomeActivity::class.java)
            "welcome" -> Intent(context, ProfileActivity::class.java)
            else -> Intent(context, HomeActivity::class.java)
        }

        // 🔥 PASS NOTIF ID
        intent.putExtra("notifId", notifId)

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 🔥 BUILD NOTIFICATION (IMPROVED)
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // 🔥 SHOW NOTIFICATION
        manager.notify(notifId, notification)
    }
}