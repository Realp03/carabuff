package com.example.carabuff

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

class NotificationActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private val notifList = mutableListOf<NotificationModel>()

    private lateinit var emptyState: LinearLayout

    private lateinit var navHome: ImageView
    private lateinit var navAnalytics: ImageView
    private lateinit var navNotif: ImageView
    private lateinit var navProfile: ImageView

    private lateinit var btnRefresh: TextView
    private lateinit var btnReadAll: TextView
    private lateinit var btnDeleteSelected: TextView
    private lateinit var btnCancelSelection: TextView
    private lateinit var btnSelectAll: TextView
    private lateinit var txtSelectionCount: TextView

    private var notificationListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        recyclerView = findViewById(R.id.recyclerNotifications)
        emptyState = findViewById(R.id.emptyState)

        btnRefresh = findViewById(R.id.btnRefresh)
        btnReadAll = findViewById(R.id.btnReadAll)
        btnDeleteSelected = findViewById(R.id.btnDeleteSelected)
        btnCancelSelection = findViewById(R.id.btnCancelSelection)
        btnSelectAll = findViewById(R.id.btnSelectAll)
        txtSelectionCount = findViewById(R.id.txtSelectionCount)

        navHome = findViewById(R.id.navHome)
        navAnalytics = findViewById(R.id.navAnalytics)
        navNotif = findViewById(R.id.navNotif)
        navProfile = findViewById(R.id.navProfile)

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = NotificationAdapter(
            notifList,
            onClick = { notif ->
                handleClick(notif)
            },
            onLongClick = {
                updateSelectionUI()
            },
            onSelectionChanged = {
                updateSelectionUI()
            }
        )

        recyclerView.adapter = adapter

        setupNavbar()
        setupActions()
        listenNotifications()
        updateSelectionUI()
    }

    private fun setupActions() {
        btnRefresh.setOnClickListener {
            listenNotifications()
            Toast.makeText(this, "Notifications refreshed", Toast.LENGTH_SHORT).show()
        }

        btnReadAll.setOnClickListener {
            readAllNotifications()
        }

        btnDeleteSelected.setOnClickListener {
            deleteSelectedNotifications()
        }

        btnCancelSelection.setOnClickListener {
            adapter.clearSelection()
        }

        btnSelectAll.setOnClickListener {
            if (notifList.isEmpty()) {
                Toast.makeText(this, "No notifications found", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            adapter.selectAll()
        }
    }

    private fun listenNotifications() {
        notificationListener?.remove()

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        notificationListener = FirebaseFirestore.getInstance()
            .collection("notifications")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshots, error ->

                if (error != null) {
                    error.printStackTrace()
                    Toast.makeText(this, "Failed: ${error.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                notifList.clear()

                if (snapshots != null && !snapshots.isEmpty) {
                    val tempList = mutableListOf<NotificationModel>()

                    for (doc in snapshots.documents) {
                        val notif = doc.toObject(NotificationModel::class.java)
                        if (notif != null) {
                            tempList.add(notif.copy(id = doc.id))
                        }
                    }

                    tempList.sortByDescending { it.timestamp }
                    notifList.addAll(tempList)
                }

                adapter.notifyDataSetChanged()
                toggleEmptyState()
                updateSelectionUI()
            }
    }

    private fun toggleEmptyState() {
        if (notifList.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun handleClick(notif: NotificationModel) {
        val db = FirebaseFirestore.getInstance()

        if (notif.id.isNotEmpty() && !notif.isRead) {
            db.collection("notifications")
                .document(notif.id)
                .update("isRead", true)
        }

        adapter.updateItem(notif.copy(isRead = true))

        when (notif.type) {
            "daily_summary" -> {
                val intent = Intent(this, DailySummaryActivity::class.java)
                val summaryDate = if (notif.summaryDate.isNotEmpty()) {
                    notif.summaryDate
                } else {
                    getYesterdayDate()
                }
                intent.putExtra("date", summaryDate)
                startActivity(intent)
            }

            "meal", "food", "workout", "daily_reminder", "security", "profile_update" -> {
                startActivity(Intent(this, HomeActivity::class.java))
            }

            "achievement" -> {
                startActivity(Intent(this, AnalyticsActivity::class.java))
            }

            "welcome" -> {
                startActivity(Intent(this, ProfileActivity::class.java))
            }

            else -> {
                startActivity(Intent(this, HomeActivity::class.java))
            }
        }
    }

    private fun readAllNotifications() {
        if (notifList.isEmpty()) {
            Toast.makeText(this, "No notifications to mark as read", Toast.LENGTH_SHORT).show()
            return
        }

        val unreadList = notifList.filter { !it.isRead && it.id.isNotEmpty() }

        if (unreadList.isEmpty()) {
            Toast.makeText(this, "All notifications are already read", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()

        for (notif in unreadList) {
            val docRef = db.collection("notifications").document(notif.id)
            batch.update(docRef, "isRead", true)
        }

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Read all failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun deleteSelectedNotifications() {
        val selectedIds = adapter.getSelectedIds()

        if (selectedIds.isEmpty()) {
            Toast.makeText(this, "No selected notifications", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()

        for (id in selectedIds) {
            if (id.isNotEmpty()) {
                batch.delete(db.collection("notifications").document(id))
            }
        }

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(this, "Selected notifications deleted", Toast.LENGTH_SHORT).show()
                adapter.clearSelection()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Delete failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateSelectionUI() {
        if (adapter.selectionMode) {
            btnDeleteSelected.visibility = View.VISIBLE
            btnCancelSelection.visibility = View.VISIBLE
            btnSelectAll.visibility = View.VISIBLE
            txtSelectionCount.visibility = View.VISIBLE

            txtSelectionCount.text = "${adapter.getSelectedCount()} selected"
            btnSelectAll.text = if (adapter.areAllSelected()) "☑ All" else "☐ Select All"
        } else {
            btnDeleteSelected.visibility = View.GONE
            btnCancelSelection.visibility = View.GONE
            btnSelectAll.visibility = View.GONE
            txtSelectionCount.visibility = View.GONE
        }
    }

    private fun getYesterdayDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(calendar.time)
    }

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
        }

        navProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationListener?.remove()
    }

    override fun onBackPressed() {
        if (adapter.selectionMode) {
            adapter.clearSelection()
        } else {
            super.onBackPressed()
        }
    }
}