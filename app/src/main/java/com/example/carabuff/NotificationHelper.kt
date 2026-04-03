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

    private const val CHANNEL_ID = "carabuff_channel"
    private const val CHANNEL_NAME = "Carabuff Notifications"
    private const val CHANNEL_DESCRIPTION = "Notifications for Carabuff updates and reminders"

    private fun shouldDedupe(type: String): Boolean {
        return when (type.lowercase()) {
            "daily_summary",
            "daily_reminder",
            "welcome" -> true
            else -> false
        }
    }

    private fun saveToFirestore(
        title: String,
        message: String,
        type: String,
        target: String,
        summaryDate: String? = null,
        dedupeKey: String? = null
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val normalizedType = type.trim().lowercase()
        val normalizedTarget = target.trim().lowercase()
        val useDedupe = shouldDedupe(normalizedType)

        if (!useDedupe) {
            val data = hashMapOf(
                "userId" to userId,
                "title" to title.trim(),
                "message" to message.trim(),
                "type" to normalizedType,
                "target" to normalizedTarget,
                "timestamp" to System.currentTimeMillis(),
                "isRead" to false,
                "summaryDate" to (summaryDate ?: "").trim(),
                "dedupeKey" to ""
            )

            db.collection("notifications").add(data)
            return
        }

        val finalDedupeKey = dedupeKey ?: buildDefaultDedupeKey(
            userId = userId,
            title = title,
            message = message,
            type = normalizedType,
            target = normalizedTarget,
            summaryDate = summaryDate
        )

        db.collection("notifications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("dedupeKey", finalDedupeKey)
            .get()
            .addOnSuccessListener { existing ->
                if (!existing.isEmpty) return@addOnSuccessListener

                val data = hashMapOf(
                    "userId" to userId,
                    "title" to title.trim(),
                    "message" to message.trim(),
                    "type" to normalizedType,
                    "target" to normalizedTarget,
                    "timestamp" to System.currentTimeMillis(),
                    "isRead" to false,
                    "summaryDate" to (summaryDate ?: "").trim(),
                    "dedupeKey" to finalDedupeKey
                )

                db.collection("notifications").add(data)
            }
    }

    private fun buildDefaultDedupeKey(
        userId: String,
        title: String,
        message: String,
        type: String,
        target: String,
        summaryDate: String?
    ): String {
        return listOf(
            userId.trim(),
            title.trim(),
            message.trim(),
            type.trim().lowercase(),
            target.trim().lowercase(),
            (summaryDate ?: "").trim()
        ).joinToString("|")
    }

    private fun createChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildIntentForTarget(
        context: Context,
        target: String,
        type: String,
        summaryDate: String?,
        notifId: Int
    ): Intent {
        val normalizedTarget = target.trim().lowercase()
        val normalizedType = type.trim().lowercase()

        val intent = when {
            normalizedType == "daily_summary" ||
                    normalizedTarget == "daily_summary" ||
                    normalizedTarget == "summary" -> {
                Intent(context, DailySummaryActivity::class.java)
            }

            normalizedTarget == "notifications" ||
                    normalizedTarget == "notification" -> {
                Intent(context, NotificationActivity::class.java)
            }

            normalizedTarget == "profile" -> {
                Intent(context, ProfileActivity::class.java)
            }

            normalizedTarget == "analytics" -> {
                Intent(context, AnalyticsActivity::class.java)
            }

            else -> {
                Intent(context, HomeActivity::class.java)
            }
        }

        intent.putExtra("notifId", notifId)
        intent.putExtra("notif_type", normalizedType)
        intent.putExtra("from_navbar", true)

        if (!summaryDate.isNullOrEmpty()) {
            intent.putExtra("date", summaryDate)
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        return intent
    }

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        type: String = "general",
        target: String = "home",
        saveToDb: Boolean = true,
        summaryDate: String? = null,
        dedupeKey: String? = null
    ) {
        val normalizedType = type.trim().lowercase()
        val normalizedTarget = target.trim().lowercase()

        if (saveToDb) {
            saveToFirestore(
                title = title,
                message = message,
                type = normalizedType,
                target = normalizedTarget,
                summaryDate = summaryDate,
                dedupeKey = dedupeKey
            )
        }

        val notifId = Random.nextInt(100000, 999999)
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createChannel(manager)

        val intent = buildIntentForTarget(
            context = context,
            target = normalizedTarget,
            type = normalizedType,
            summaryDate = summaryDate,
            notifId = notifId
        )

        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
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