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

object NotificationHelper {

    private fun saveToFirestore(
        title: String,
        message: String,
        type: String,
        target: String,
        summaryDate: String? = null
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val data = hashMapOf(
            "userId" to userId,
            "title" to title,
            "message" to message,
            "type" to type,
            "target" to target,
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false
        )

        if (summaryDate != null) {
            data["summaryDate"] = summaryDate
        }

        db.collection("notifications").add(data)
    }

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        type: String = "general",
        target: String = "home",
        saveToDb: Boolean = true,
        summaryDate: String? = null
    ) {
        if (saveToDb) {
            saveToFirestore(title, message, type, target, summaryDate)
        }

        val notifId = Random.nextInt(100000, 999999)
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "carabuff_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Carabuff Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val intent = when (target) {
            "daily_summary" -> Intent(context, DailySummaryActivity::class.java)
            "notifications" -> Intent(context, NotificationActivity::class.java)
            "profile" -> Intent(context, ProfileActivity::class.java)
            "analytics" -> Intent(context, AnalyticsActivity::class.java)
            else -> Intent(context, HomeActivity::class.java)
        }

        intent.putExtra("notifId", notifId)
        intent.putExtra("notif_type", type)

        if (summaryDate != null) {
            intent.putExtra("date", summaryDate)
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(notifId, notification)
    }
}