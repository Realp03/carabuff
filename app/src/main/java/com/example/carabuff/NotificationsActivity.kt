package com.example.carabuff

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
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
import java.util.Calendar
import java.util.Locale

class NotificationActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private val notifList = mutableListOf<NotificationModel>()

    private lateinit var emptyState: LinearLayout

    private lateinit var btnRefresh: TextView
    private lateinit var btnReadAll: TextView
    private lateinit var btnDeleteSelected: TextView
    private lateinit var btnCancelSelection: TextView
    private lateinit var btnSelectAll: TextView
    private lateinit var txtSelectionCount: TextView

    private lateinit var navHomeContainer: LinearLayout
    private lateinit var navAnalyticsContainer: LinearLayout
    private lateinit var navNotifContainer: LinearLayout
    private lateinit var navProfileContainer: LinearLayout

    private lateinit var navHome: ImageView
    private lateinit var navAnalytics: ImageView
    private lateinit var navNotif: ImageView
    private lateinit var navProfile: ImageView

    private lateinit var navHomeLabel: TextView
    private lateinit var navAnalyticsLabel: TextView
    private lateinit var navNotifLabel: TextView
    private lateinit var navProfileLabel: TextView

    private lateinit var navNotifDot: View

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var prefs: SharedPreferences

    private var notificationListener: ListenerRegistration? = null
    private var isRefreshing = false
    private var hasAnimatedContent = false

    private val forcedReadIds = mutableSetOf<String>()

    companion object {
        private const val PREFS_NAME = "carabuff_notif_prefs"
        private const val KEY_FORCED_READ_IDS = "forced_read_ids"

        private const val ICON_ACTIVE_COLOR = "#111111"
        private const val ICON_INACTIVE_COLOR = "#B8C7D6"
        private const val LABEL_ACTIVE_COLOR = "#111111"
        private const val LABEL_INACTIVE_COLOR = "#8FA3B8"

        private const val NAV_ACTIVE_ALPHA_START = 0.60f
        private const val NAV_INACTIVE_ALPHA_START = 0.82f
        private const val NAV_ACTIVE_DURATION = 180L
        private const val NAV_INACTIVE_DURATION = 130L

        private const val CONTENT_FADE_DURATION = 220L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        bindViews()
        loadForcedReadIds()
        setupRecycler()
        setupNavbar()
        setupActions()
        updateSelectionUI()
        toggleEmptyState()
        prepareContentForFade()
        listenNotifications()
    }

    override fun onResume() {
        super.onResume()

        loadForcedReadIds()
        setActiveNav("notif", animate = false)
        enableAllNavButtons()

        val fromNavbar = intent.getBooleanExtra("from_navbar", false)

        if (fromNavbar) {
            prepareContentForFade()
            animatePageContentOnly()
            intent.removeExtra("from_navbar")
            hasAnimatedContent = true
        } else if (!hasAnimatedContent) {
            animatePageContentOnly()
            hasAnimatedContent = true
        }

        listenNotifications()
    }

    private fun bindViews() {
        recyclerView = findViewById(R.id.recyclerNotifications)
        emptyState = findViewById(R.id.emptyState)

        btnRefresh = findViewById(R.id.btnRefresh)
        btnReadAll = findViewById(R.id.btnReadAll)
        btnDeleteSelected = findViewById(R.id.btnDeleteSelected)
        btnCancelSelection = findViewById(R.id.btnCancelSelection)
        btnSelectAll = findViewById(R.id.btnSelectAll)
        txtSelectionCount = findViewById(R.id.txtSelectionCount)

        navHomeContainer = findViewById(R.id.navHomeContainer)
        navAnalyticsContainer = findViewById(R.id.navAnalyticsContainer)
        navNotifContainer = findViewById(R.id.navNotifContainer)
        navProfileContainer = findViewById(R.id.navProfileContainer)

        navHome = findViewById(R.id.navHome)
        navAnalytics = findViewById(R.id.navAnalytics)
        navNotif = findViewById(R.id.navNotif)
        navProfile = findViewById(R.id.navProfile)

        navHomeLabel = findViewById(R.id.navHomeLabel)
        navAnalyticsLabel = findViewById(R.id.navAnalyticsLabel)
        navNotifLabel = findViewById(R.id.navNotifLabel)
        navProfileLabel = findViewById(R.id.navProfileLabel)

        navNotifDot = findViewById(R.id.navNotifDot)
    }

    private fun setupRecycler() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

        adapter = NotificationAdapter(
            notifList,
            onClick = { notif -> handleClick(notif) },
            onLongClick = { updateSelectionUI() },
            onSelectionChanged = { updateSelectionUI() }
        )

        recyclerView.adapter = adapter
    }

    private fun setupNavbar() {
        setActiveNav("notif", animate = false)

        navHomeContainer.setOnClickListener {
            openTab("home", HomeActivity::class.java)
        }

        navAnalyticsContainer.setOnClickListener {
            openTab("analytics", AnalyticsActivity::class.java)
        }

        navNotifContainer.setOnClickListener {
            setActiveNav("notif", animate = true)
        }

        navProfileContainer.setOnClickListener {
            openTab("profile", ProfileActivity::class.java)
        }
    }

    private fun openTab(tab: String, target: Class<*>) {
        if (this::class.java == target) {
            setActiveNav(tab, animate = true)
            return
        }

        setActiveNav(tab, animate = true)
        disableAllNavButtons()

        val intent = Intent(this, target).apply {
            putExtra("from_navbar", true)
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    private fun setActiveNav(tab: String, animate: Boolean = true) {
        resetNavItem(navHomeContainer, navHome, navHomeLabel, animate)
        resetNavItem(navAnalyticsContainer, navAnalytics, navAnalyticsLabel, animate)
        resetNavItem(navNotifContainer, navNotif, navNotifLabel, animate)
        resetNavItem(navProfileContainer, navProfile, navProfileLabel, animate)

        when (tab) {
            "home" -> activateNavItem(navHomeContainer, navHome, navHomeLabel, animate)
            "analytics" -> activateNavItem(navAnalyticsContainer, navAnalytics, navAnalyticsLabel, animate)
            "notif" -> activateNavItem(navNotifContainer, navNotif, navNotifLabel, animate)
            "profile" -> activateNavItem(navProfileContainer, navProfile, navProfileLabel, animate)
        }
    }

    private fun activateNavItem(
        container: LinearLayout,
        icon: ImageView,
        label: TextView,
        animate: Boolean
    ) {
        container.animate().cancel()
        icon.animate().cancel()
        label.animate().cancel()

        container.setBackgroundResource(R.drawable.bg_nav_item_active)
        icon.setColorFilter(Color.parseColor(ICON_ACTIVE_COLOR))
        label.setTextColor(Color.parseColor(LABEL_ACTIVE_COLOR))

        if (animate) {
            container.alpha = NAV_ACTIVE_ALPHA_START
            icon.alpha = NAV_ACTIVE_ALPHA_START
            label.alpha = NAV_ACTIVE_ALPHA_START

            container.animate().alpha(1f).setDuration(NAV_ACTIVE_DURATION).start()
            icon.animate().alpha(1f).setDuration(NAV_ACTIVE_DURATION).start()
            label.animate().alpha(1f).setDuration(NAV_ACTIVE_DURATION).start()
        } else {
            container.alpha = 1f
            icon.alpha = 1f
            label.alpha = 1f
        }
    }

    private fun resetNavItem(
        container: LinearLayout,
        icon: ImageView,
        label: TextView,
        animate: Boolean
    ) {
        container.animate().cancel()
        icon.animate().cancel()
        label.animate().cancel()

        container.setBackgroundResource(R.drawable.bg_nav_item_inactive)
        icon.setColorFilter(Color.parseColor(ICON_INACTIVE_COLOR))
        label.setTextColor(Color.parseColor(LABEL_INACTIVE_COLOR))

        if (animate) {
            container.alpha = NAV_INACTIVE_ALPHA_START
            icon.alpha = NAV_INACTIVE_ALPHA_START
            label.alpha = NAV_INACTIVE_ALPHA_START

            container.animate().alpha(1f).setDuration(NAV_INACTIVE_DURATION).start()
            icon.animate().alpha(1f).setDuration(NAV_INACTIVE_DURATION).start()
            label.animate().alpha(1f).setDuration(NAV_INACTIVE_DURATION).start()
        } else {
            container.alpha = 1f
            icon.alpha = 1f
            label.alpha = 1f
        }
    }

    private fun disableAllNavButtons() {
        navHomeContainer.isEnabled = false
        navAnalyticsContainer.isEnabled = false
        navNotifContainer.isEnabled = false
        navProfileContainer.isEnabled = false
    }

    private fun enableAllNavButtons() {
        navHomeContainer.isEnabled = true
        navAnalyticsContainer.isEnabled = true
        navNotifContainer.isEnabled = true
        navProfileContainer.isEnabled = true
    }

    private fun setupActions() {
        btnRefresh.setOnClickListener {
            if (isRefreshing) return@setOnClickListener
            isRefreshing = true

            loadForcedReadIds()
            listenNotifications()

            recyclerView.postDelayed({
                isRefreshing = false
                Toast.makeText(this, "Notifications refreshed", Toast.LENGTH_SHORT).show()
            }, 400)
        }

        btnReadAll.setOnClickListener {
            readAllNotifications()
        }

        btnDeleteSelected.setOnClickListener {
            deleteSelectedNotifications()
        }

        btnCancelSelection.setOnClickListener {
            adapter.clearSelection()
            updateSelectionUI()
        }

        btnSelectAll.setOnClickListener {
            if (notifList.isEmpty()) {
                Toast.makeText(this, "No notifications found", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            adapter.selectAll()
            updateSelectionUI()
        }
    }

    private fun loadForcedReadIds() {
        forcedReadIds.clear()
        val saved = prefs.getStringSet(KEY_FORCED_READ_IDS, emptySet()) ?: emptySet()
        forcedReadIds.addAll(saved)
    }

    private fun saveForcedReadIds() {
        prefs.edit()
            .putStringSet(KEY_FORCED_READ_IDS, forcedReadIds.toSet())
            .apply()
    }

    private fun addForcedReadId(id: String) {
        if (id.isNotEmpty()) {
            forcedReadIds.add(id)
            saveForcedReadIds()
        }
    }

    private fun removeForcedReadId(id: String) {
        if (forcedReadIds.remove(id)) {
            saveForcedReadIds()
        }
    }

    private fun listenNotifications() {
        notificationListener?.remove()

        val userId = auth.currentUser?.uid ?: run {
            notifList.clear()
            adapter.clearSelection()
            toggleEmptyState()
            updateNotificationDotFromList()
            return
        }

        notificationListener = db.collection("notifications")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshots, error ->

                if (error != null) {
                    Toast.makeText(this, "Failed: ${error.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                val previousSelectedIds = adapter.getSelectedIds().toSet()
                notifList.clear()

                if (snapshots != null && !snapshots.isEmpty) {
                    val tempList = mutableListOf<NotificationModel>()

                    for (doc in snapshots.documents) {
                        val notif = doc.toObject(NotificationModel::class.java)

                        if (notif != null) {
                            val finalIsRead = notif.isRead || forcedReadIds.contains(doc.id)

                            if (notif.isRead) {
                                removeForcedReadId(doc.id)
                            }

                            tempList.add(
                                notif.copy(
                                    id = doc.id,
                                    isRead = finalIsRead,
                                    isSelected = previousSelectedIds.contains(doc.id)
                                )
                            )
                        } else {
                            val title = doc.getString("title") ?: "Notification"
                            val message = doc.getString("message") ?: ""
                            val type = doc.getString("type") ?: "system"
                            val target = doc.getString("target") ?: ""
                            val summaryDate = doc.getString("summaryDate") ?: ""
                            val dedupeKey = doc.getString("dedupeKey") ?: ""
                            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            val rawIsRead = doc.getBoolean("isRead") ?: false
                            val docUserId = doc.getString("userId") ?: userId

                            val finalIsRead = rawIsRead || forcedReadIds.contains(doc.id)

                            if (rawIsRead) {
                                removeForcedReadId(doc.id)
                            }

                            tempList.add(
                                NotificationModel(
                                    id = doc.id,
                                    userId = docUserId,
                                    title = title,
                                    message = message,
                                    type = type,
                                    target = target,
                                    timestamp = timestamp,
                                    isRead = finalIsRead,
                                    summaryDate = summaryDate,
                                    dedupeKey = dedupeKey,
                                    isSelected = previousSelectedIds.contains(doc.id)
                                )
                            )
                        }
                    }

                    tempList.sortByDescending { it.timestamp }
                    notifList.addAll(tempList)
                }

                adapter.notifyDataSetChanged()
                toggleEmptyState()
                updateSelectionUI()
                updateNotificationDotFromList()
            }
    }

    private fun updateNotificationDotFromList() {
        val hasUnread = notifList.any { !it.isRead }
        navNotifDot.visibility = if (hasUnread) View.VISIBLE else View.GONE
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
        if (adapter.selectionMode) {
            adapter.toggleSelection(notif.id)
            updateSelectionUI()
            return
        }

        if (notif.id.isNotEmpty() && !notif.isRead) {
            addForcedReadId(notif.id)
            adapter.updateItem(notif.copy(isRead = true))
            updateLocalReadState(notif.id, true)

            db.collection("notifications")
                .document(notif.id)
                .update("isRead", true)
                .addOnSuccessListener {
                    adapter.updateItem(notif.copy(isRead = true))
                    updateLocalReadState(notif.id, true)
                    openNotificationTarget(notif.copy(isRead = true))
                }
                .addOnFailureListener {
                    removeForcedReadId(notif.id)
                    adapter.updateItem(notif.copy(isRead = false))
                    updateLocalReadState(notif.id, false)
                    Toast.makeText(this, "Failed to mark as read", Toast.LENGTH_SHORT).show()
                    openNotificationTarget(notif)
                }
        } else {
            openNotificationTarget(notif)
        }
    }

    private fun updateLocalReadState(notificationId: String, isRead: Boolean) {
        val index = notifList.indexOfFirst { it.id == notificationId }
        if (index != -1) {
            notifList[index] = notifList[index].copy(isRead = isRead)
        }
        updateNotificationDotFromList()
    }

    private fun openNotificationTarget(notif: NotificationModel) {
        when (notif.type.lowercase()) {
            "daily_summary" -> {
                val intent = Intent(this, DailySummaryActivity::class.java)
                val summaryDate = if (notif.summaryDate.isNotEmpty()) {
                    notif.summaryDate
                } else {
                    getYesterdayDate()
                }
                intent.putExtra("date", summaryDate)
                startActivity(intent)
                overridePendingTransition(0, 0)
            }

            "meal", "food", "workout", "daily_reminder" -> {
                openNavbarTarget(HomeActivity::class.java)
            }

            "achievement" -> {
                openNavbarTarget(AnalyticsActivity::class.java)
            }

            "welcome", "security", "profile_update" -> {
                openNavbarTarget(ProfileActivity::class.java)
            }

            else -> {
                when (notif.target.lowercase()) {
                    "home" -> openNavbarTarget(HomeActivity::class.java)
                    "analytics" -> openNavbarTarget(AnalyticsActivity::class.java)
                    "profile" -> openNavbarTarget(ProfileActivity::class.java)
                    "notifications", "notification" -> {
                        setActiveNav("notif", animate = true)
                    }
                    "daily_summary", "summary" -> {
                        val intent = Intent(this, DailySummaryActivity::class.java)
                        val summaryDate = if (notif.summaryDate.isNotEmpty()) {
                            notif.summaryDate
                        } else {
                            getYesterdayDate()
                        }
                        intent.putExtra("date", summaryDate)
                        startActivity(intent)
                        overridePendingTransition(0, 0)
                    }
                    else -> openNavbarTarget(HomeActivity::class.java)
                }
            }
        }
    }

    private fun openNavbarTarget(target: Class<*>) {
        if (this::class.java == target) {
            setActiveNav("notif", animate = true)
            return
        }

        val intent = Intent(this, target).apply {
            putExtra("from_navbar", true)
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
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

        val batch = db.batch()

        for (notif in unreadList) {
            addForcedReadId(notif.id)
            batch.update(db.collection("notifications").document(notif.id), "isRead", true)
        }

        for (i in notifList.indices) {
            if (notifList[i].id.isNotEmpty()) {
                notifList[i] = notifList[i].copy(isRead = true)
            }
        }

        adapter.notifyDataSetChanged()
        updateSelectionUI()
        updateNotificationDotFromList()

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                unreadList.forEach { removeForcedReadId(it.id) }
                listenNotifications()
                Toast.makeText(this, "Read all failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun deleteSelectedNotifications() {
        val selectedIds = adapter.getSelectedIds()

        if (selectedIds.isEmpty()) {
            Toast.makeText(this, "No selected notifications", Toast.LENGTH_SHORT).show()
            return
        }

        val batch = db.batch()

        for (id in selectedIds) {
            if (id.isNotEmpty()) {
                removeForcedReadId(id)
                batch.delete(db.collection("notifications").document(id))
            }
        }

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(this, "Selected notifications deleted", Toast.LENGTH_SHORT).show()
                adapter.clearSelection()
                updateSelectionUI()
                listenNotifications()
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

    private fun prepareContentForFade() {
        val contentViews = listOf(
            btnRefresh,
            btnReadAll,
            btnDeleteSelected,
            btnCancelSelection,
            btnSelectAll,
            txtSelectionCount,
            recyclerView,
            emptyState
        )

        contentViews.forEach { it.alpha = 0f }
    }

    private fun animatePageContentOnly() {
        val contentViews = listOf(
            btnRefresh,
            btnReadAll,
            btnDeleteSelected,
            btnCancelSelection,
            btnSelectAll,
            txtSelectionCount,
            recyclerView,
            emptyState
        )

        contentViews.forEachIndexed { index, view ->
            view.animate().cancel()
            view.animate()
                .alpha(1f)
                .setStartDelay(index * 20L)
                .setDuration(CONTENT_FADE_DURATION)
                .start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationListener?.remove()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (adapter.selectionMode) {
            adapter.clearSelection()
            updateSelectionUI()
        } else {
            super.onBackPressed()
        }
    }
}