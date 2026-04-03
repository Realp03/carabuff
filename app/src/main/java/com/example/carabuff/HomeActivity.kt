package com.example.carabuff

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var carabuff: ImageView
    private lateinit var thoughtText: TextView
    private lateinit var dayText: TextView
    private lateinit var dateText: TextView

    private lateinit var btnAddWorkout: Button
    private lateinit var btnAddFood: Button
    private lateinit var btnAskCarabuff: LinearLayout

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

    private lateinit var tvCalories: TextView
    private lateinit var tvProtein: TextView
    private lateinit var tvCarbs: TextView
    private lateinit var tvFats: TextView
    private lateinit var tvWorkout: TextView
    private lateinit var tvCaloriesBurned: TextView

    private lateinit var progressCalories: ProgressBar
    private lateinit var progressWorkout: ProgressBar

    private val textHandler = Handler(Looper.getMainLooper())
    private val animHandler = Handler(Looper.getMainLooper())

    private var isTalking = false
    private var userName: String = "User"
    private var hasPlayedGreeting = false

    private var typingRunnable: Runnable? = null
    private var deletingRunnable: Runnable? = null
    private var animationEndRunnable: Runnable? = null

    private var currentAnim: AnimationDrawable? = null

    companion object {
        private val STATIC_CARABUFF = R.drawable.carabuff1
        private val ANIM_TALK = R.drawable.carabuff_talk_anim
        private val ANIM_POINT = R.drawable.carabuff_point
        private val ANIM_VIBE = R.drawable.carabuff_vibe

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
        setContentView(R.layout.activity_home)

        cancelOpenedNotificationIfNeeded()

        carabuff = findViewById(R.id.carabuffHome)
        thoughtText = findViewById(R.id.carabuffThought)
        dayText = findViewById(R.id.dayText)
        dateText = findViewById(R.id.dateText)

        btnAddWorkout = findViewById(R.id.btnAddWorkout)
        btnAddFood = findViewById(R.id.btnAddFood)
        btnAskCarabuff = findViewById(R.id.btnAskCarabuff)

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

        tvCalories = findViewById(R.id.tvCalories)
        tvProtein = findViewById(R.id.tvProtein)
        tvCarbs = findViewById(R.id.tvCarbs)
        tvFats = findViewById(R.id.tvFats)
        tvWorkout = findViewById(R.id.tvWorkout)
        tvCaloriesBurned = findViewById(R.id.tvCaloriesBurned)

        progressCalories = findViewById(R.id.progressCalories)
        progressWorkout = findViewById(R.id.progressWorkout)

        updateCurrentDateUI()

        requestNotificationPermission()
        fetchAndSaveFcmToken()

        showStaticCarabuff()

        checkDailyReset()
        loadData()

        carabuff.setOnClickListener {
            if (isTalking) return@setOnClickListener

            val messages = listOf(
                "Stay consistent 💪",
                "Hit your macros today 🔥",
                "Let's go! 🚀",
                "You got this 😤"
            )

            playRandomCarabuffAnimation(messages.random())
        }

        btnAddWorkout.setOnClickListener {
            startActivity(Intent(this, AddWorkoutActivity::class.java))
            overridePendingTransition(0, 0)
        }

        btnAddFood.setOnClickListener {
            startActivity(Intent(this, AddFoodActivity::class.java))
            overridePendingTransition(0, 0)
        }

        btnAskCarabuff.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
            overridePendingTransition(0, 0)
        }

        setupNavbar()

        val fromNavbar = intent.getBooleanExtra("from_navbar", false)
        if (fromNavbar) {
            prepareContentForFade()
        } else {
            showContentImmediately()
        }
    }

    override fun onResume() {
        super.onResume()

        updateCurrentDateUI()
        checkDailyReset()
        loadData()
        checkMealReminder()

        setActiveNav("home", animate = false)
        enableAllNavButtons()

        if (!isTalking) {
            showStaticCarabuff()
        }

        val fromNavbar = intent.getBooleanExtra("from_navbar", false)
        if (fromNavbar) {
            animatePageContentOnly()
            intent.removeExtra("from_navbar")
        } else {
            showContentImmediately()
        }
    }

    private fun updateCurrentDateUI() {
        val now = Calendar.getInstance().time
        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

        dayText.text = dayFormat.format(now)
        dateText.text = dateFormat.format(now)
    }

    private fun cancelOpenedNotificationIfNeeded() {
        val notifId = intent.getIntExtra("notifId", -1)
        if (notifId != -1) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(notifId)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    private fun fetchAndSaveFcmToken() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e("FCM_DEBUG", "FAILED TO GET TOKEN", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result
                Log.d("FCM_DEBUG", "TOKEN: $token")

                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid == null) {
                    Log.e("FCM_DEBUG", "NO USER LOGGED IN")
                    return@addOnCompleteListener
                }

                val data = hashMapOf(
                    "fcmToken" to token,
                    "tokenUpdatedAt" to FieldValue.serverTimestamp()
                )

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("FCM_DEBUG", "TOKEN SAVED!")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FCM_DEBUG", "FAILED TO SAVE TOKEN", e)
                    }
            }
    }

    private fun setupNavbar() {
        setActiveNav("home", animate = false)

        navHomeContainer.setOnClickListener {
            setActiveNav("home", animate = true)
        }

        navAnalyticsContainer.setOnClickListener {
            openTab("analytics", AnalyticsActivity::class.java)
        }

        navNotifContainer.setOnClickListener {
            openTab("notif", NotificationActivity::class.java)
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

    private fun prepareContentForFade() {
        getContentViews().forEach { it.alpha = 0f }
    }

    private fun animatePageContentOnly() {
        getContentViews().forEachIndexed { index, view ->
            view.animate().cancel()
            view.animate()
                .alpha(1f)
                .setStartDelay(index * 18L)
                .setDuration(CONTENT_FADE_DURATION)
                .start()
        }
    }

    private fun showContentImmediately() {
        getContentViews().forEach { view ->
            view.animate().cancel()
            view.alpha = 1f
        }
    }

    private fun getContentViews(): List<View> {
        return listOf(
            dayText,
            dateText,
            carabuff,
            thoughtText,
            btnAddWorkout,
            btnAddFood,
            btnAskCarabuff,
            tvCalories,
            tvProtein,
            tvCarbs,
            tvFats,
            tvWorkout,
            tvCaloriesBurned,
            progressCalories,
            progressWorkout
        )
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

    private fun checkDailyReset() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val userRef = db.collection("users").document(userId)

        userRef.get().addOnSuccessListener { doc ->
            val plan = doc.get("plan") as? Map<*, *>
            val lastReset = plan?.get("lastResetDate") as? String

            if (lastReset != today) {
                userRef.update(
                    mapOf(
                        "plan.workoutDone" to 0,
                        "plan.lastResetDate" to today
                    )
                )
            }
        }
    }

    private fun loadData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->

                userName = document.getString("name") ?: "User"
                val plan = document.get("plan") as? Map<*, *>

                val workoutMinutes = (plan?.get("workoutMinutes") as? Long ?: 0).toInt()
                val workoutDone = (plan?.get("workoutDone") as? Long ?: 0).toInt()

                tvWorkout.text = "$workoutDone / $workoutMinutes mins"

                progressWorkout.max = if (workoutMinutes > 0) workoutMinutes else 1
                progressWorkout.progress = workoutDone.coerceAtMost(progressWorkout.max)

                loadFoodIntake()
                loadCaloriesBurned()
                updateNotificationDot()

                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val greeting = when (hour) {
                    in 0..11 -> "Good morning"
                    in 12..17 -> "Good afternoon"
                    else -> "Good evening"
                }

                if (!isTalking && !hasPlayedGreeting) {
                    hasPlayedGreeting = true
                    playRandomCarabuffAnimation("$greeting, $userName 👋")
                }
            }
    }

    private fun updateNotificationDot() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("notifications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { result ->
                navNotifDot.visibility = if (result.isEmpty) View.GONE else View.VISIBLE
            }
            .addOnFailureListener {
                navNotifDot.visibility = View.GONE
            }
    }

    private fun loadFoodIntake() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        db.collection("users")
            .document(userId)
            .collection("foodLogs")
            .whereGreaterThan("timestamp", startOfDay)
            .get()
            .addOnSuccessListener { result ->

                var totalCalories = 0
                var totalProtein = 0.0
                var totalCarbs = 0.0
                var totalFats = 0.0

                for (doc in result) {
                    totalCalories += (doc.getLong("calories") ?: 0).toInt()
                    totalProtein += doc.getDouble("protein") ?: 0.0
                    totalCarbs += doc.getDouble("carbs") ?: 0.0
                    totalFats += doc.getDouble("fats") ?: 0.0
                }

                updateFoodUI(totalCalories, totalProtein, totalCarbs, totalFats)
            }
    }

    private fun updateFoodUI(
        calories: Int,
        protein: Double,
        carbs: Double,
        fats: Double
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->

                val plan = doc.get("plan") as? Map<*, *>

                val maxCalories = (plan?.get("calories") as? Long ?: 0).toInt()
                val maxProtein = (plan?.get("protein") as? Long ?: 0).toInt()
                val maxCarbs = (plan?.get("carbs") as? Long ?: 0).toInt()
                val maxFats = (plan?.get("fats") as? Long ?: 0).toInt()

                tvCalories.text = "$calories / $maxCalories Calories"
                tvProtein.text = "Protein: ${protein.toInt()} / ${maxProtein}g"
                tvCarbs.text = "Carbs: ${carbs.toInt()} / ${maxCarbs}g"
                tvFats.text = "Fats: ${fats.toInt()} / ${maxFats}g"

                progressCalories.max = if (maxCalories > 0) maxCalories else 1
                progressCalories.progress = calories.coerceAtMost(progressCalories.max)
            }
    }

    private fun loadCaloriesBurned() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        db.collection("users")
            .document(userId)
            .collection("workouts")
            .whereGreaterThan("timestamp", startOfDay)
            .get()
            .addOnSuccessListener { result ->

                var totalCalories = 0

                for (doc in result) {
                    val cal = doc.getLong("caloriesBurned") ?: 0
                    totalCalories += cal.toInt()
                }

                tvCaloriesBurned.text = "Burned: $totalCalories kcal 🔥"
            }
    }

    private fun showStaticCarabuff() {
        stopCurrentAnimation()
        carabuff.setImageResource(STATIC_CARABUFF)
    }

    private fun playAnimationOnce(resId: Int) {
        stopCurrentAnimation()
        carabuff.setImageResource(resId)

        val drawable = carabuff.drawable
        if (drawable is AnimationDrawable) {
            drawable.isOneShot = true
            currentAnim = drawable
            drawable.start()
        } else {
            currentAnim = null
        }
    }

    private fun stopCurrentAnimation() {
        animationEndRunnable?.let { animHandler.removeCallbacks(it) }
        currentAnim?.stop()
        currentAnim = null
        carabuff.clearAnimation()
    }

    private fun getAnimationDuration(resId: Int): Long {
        val drawable = ContextCompat.getDrawable(this, resId)
        return if (drawable is AnimationDrawable) {
            var total = 0
            for (i in 0 until drawable.numberOfFrames) {
                total += drawable.getDuration(i)
            }
            total.toLong()
        } else {
            1000L
        }
    }

    private fun playRandomCarabuffAnimation(message: String) {
        isTalking = true
        textHandler.removeCallbacksAndMessages(null)
        thoughtText.text = ""

        val randomAnim = listOf(ANIM_TALK, ANIM_POINT, ANIM_VIBE).random()
        playAnimationOnce(randomAnim)
        typeText(message)

        animationEndRunnable = Runnable {
            showStaticCarabuff()
        }
        animHandler.postDelayed(animationEndRunnable!!, getAnimationDuration(randomAnim))
    }

    private fun typeText(text: String) {
        thoughtText.text = ""
        var index = 0

        typingRunnable = object : Runnable {
            override fun run() {
                if (index < text.length) {
                    thoughtText.text = text.substring(0, index + 1)
                    index++
                    textHandler.postDelayed(this, 40L)
                } else {
                    deletingRunnable = Runnable {
                        deleteText(text)
                    }
                    textHandler.postDelayed(deletingRunnable!!, 1500L)
                }
            }
        }

        textHandler.post(typingRunnable!!)
    }

    private fun deleteText(text: String) {
        var index = text.length

        deletingRunnable = object : Runnable {
            override fun run() {
                if (index > 0) {
                    index--
                    thoughtText.text = text.substring(0, index)
                    textHandler.postDelayed(this, 25L)
                } else {
                    thoughtText.text = "..."
                    isTalking = false
                    showStaticCarabuff()
                }
            }
        }

        textHandler.post(deletingRunnable!!)
    }

    private fun checkMealReminder() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val prefs = getSharedPreferences("meal_reminder", MODE_PRIVATE)
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

        if (hour in 6..10 && !prefs.getBoolean("breakfast_$today", false)) {
            NotificationHelper.showNotification(
                context = this,
                title = "Breakfast Reminder 🍳",
                message = "Don't forget to log your breakfast!",
                type = "meal",
                target = "home",
                saveToDb = true
            )
            prefs.edit().putBoolean("breakfast_$today", true).apply()
            updateNotificationDot()
        }

        if (hour in 12..16 && !prefs.getBoolean("lunch_$today", false)) {
            NotificationHelper.showNotification(
                context = this,
                title = "Lunch Reminder 🍛",
                message = "Don't forget to log your lunch!",
                type = "meal",
                target = "home",
                saveToDb = true
            )
            prefs.edit().putBoolean("lunch_$today", true).apply()
            updateNotificationDot()
        }

        if (hour in 18..23 && !prefs.getBoolean("dinner_$today", false)) {
            NotificationHelper.showNotification(
                context = this,
                title = "Dinner Reminder 🍽",
                message = "Don't forget to log your dinner!",
                type = "meal",
                target = "home",
                saveToDb = true
            )
            prefs.edit().putBoolean("dinner_$today", true).apply()
            updateNotificationDot()
        }
    }

    override fun onPause() {
        super.onPause()
        stopCurrentAnimation()
        if (!isTalking) {
            showStaticCarabuff()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textHandler.removeCallbacksAndMessages(null)
        animHandler.removeCallbacksAndMessages(null)
        stopCurrentAnimation()
    }
}