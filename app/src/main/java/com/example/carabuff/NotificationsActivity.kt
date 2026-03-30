package com.example.carabuff

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotificationActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private val notifList = mutableListOf<NotificationModel>()

    private lateinit var emptyState: LinearLayout

    private lateinit var navHome: ImageView
    private lateinit var navAnalytics: ImageView
    private lateinit var navNotif: ImageView
    private lateinit var navProfile: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        recyclerView = findViewById(R.id.recyclerNotifications)
        emptyState = findViewById(R.id.emptyState)

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = NotificationAdapter(notifList) { notif ->
            handleClick(notif)
        }

        recyclerView.adapter = adapter

        listenNotifications()

        navHome = findViewById(R.id.navHome)
        navAnalytics = findViewById(R.id.navAnalytics)
        navNotif = findViewById(R.id.navNotif)
        navProfile = findViewById(R.id.navProfile)

        setupNavbar()
    }

    // 🔥 REAL-TIME LISTENER (FIXED WITH DOC ID)
    private fun listenNotifications() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("notifications")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->

                if (error != null) return@addSnapshotListener

                notifList.clear()

                if (snapshots != null && !snapshots.isEmpty) {
                    for (doc in snapshots) {
                        val notif = doc.toObject(NotificationModel::class.java)
                        notifList.add(notif.copy(id = doc.id)) // 🔥 CRITICAL FIX
                    }
                }

                adapter.notifyDataSetChanged()
                toggleEmptyState()
            }
    }

    // 🔥 EMPTY STATE
    private fun toggleEmptyState() {
        if (notifList.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    // 🔥 CLICK ACTION (FULL FIXED)
    private fun handleClick(notif: NotificationModel) {

        val db = FirebaseFirestore.getInstance()

        // 🔥 MARK AS READ USING DOCUMENT ID (NO MORE TIMESTAMP BUG)
        if (notif.id.isNotEmpty()) {
            db.collection("notifications")
                .document(notif.id)
                .update("isRead", true)
        }

        // 🔥 UPDATE UI SMOOTHLY
        adapter.updateItem(notif.copy(isRead = true))

        // 🔥 NAVIGATION
        when (notif.type) {

            "daily_summary" -> {
                startActivity(Intent(this, DailySummaryActivity::class.java))
            }

            "achievement" -> {
                startActivity(Intent(this, AnalyticsActivity::class.java))
            }

            "workout", "food" -> {
                startActivity(Intent(this, HomeActivity::class.java))
            }

            "welcome" -> {
                startActivity(Intent(this, ProfileActivity::class.java))
            }

            else -> {
                startActivity(Intent(this, HomeActivity::class.java))
            }
        }
    }

    // 🔥 NAVBAR
    private fun setupNavbar() {

        navHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        navAnalytics.setOnClickListener {
            startActivity(Intent(this, AnalyticsActivity::class.java))
            finish()
        }

        navNotif.setOnClickListener {
            // current page
        }

        navProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }
}