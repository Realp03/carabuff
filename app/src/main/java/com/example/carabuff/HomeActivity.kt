package com.example.carabuff

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AnimationDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var carabuff: ImageView
    private lateinit var thoughtText: TextView
    private lateinit var dateText: TextView

    private lateinit var btnAddWorkout: Button
    private lateinit var btnAddFood: Button
    private lateinit var btnAskCarabuff: LinearLayout

    private lateinit var navHome: ImageView
    private lateinit var navAnalytics: ImageView
    private lateinit var navNotif: ImageView
    private lateinit var navProfile: ImageView

    private lateinit var tvCalories: TextView
    private lateinit var tvProtein: TextView
    private lateinit var tvCarbs: TextView
    private lateinit var tvFats: TextView
    private lateinit var tvWorkout: TextView
    private lateinit var tvCaloriesBurned: TextView

    private lateinit var progressCalories: ProgressBar
    private lateinit var progressWorkout: ProgressBar

    private val handler = Handler(Looper.getMainLooper())
    private var isTalking = false
    private var userName: String = "User"

    private var typingRunnable: Runnable? = null
    private var deletingRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        cancelOpenedNotificationIfNeeded()

        carabuff = findViewById(R.id.carabuffHome)
        thoughtText = findViewById(R.id.carabuffThought)
        dateText = findViewById(R.id.dateText)

        btnAddWorkout = findViewById(R.id.btnAddWorkout)
        btnAddFood = findViewById(R.id.btnAddFood)
        btnAskCarabuff = findViewById(R.id.btnAskCarabuff)

        navHome = findViewById(R.id.navHome)
        navAnalytics = findViewById(R.id.navAnalytics)
        navNotif = findViewById(R.id.navNotif)
        navProfile = findViewById(R.id.navProfile)

        tvCalories = findViewById(R.id.tvCalories)
        tvProtein = findViewById(R.id.tvProtein)
        tvCarbs = findViewById(R.id.tvCarbs)
        tvFats = findViewById(R.id.tvFats)
        tvWorkout = findViewById(R.id.tvWorkout)
        tvCaloriesBurned = findViewById(R.id.tvCaloriesBurned)

        progressCalories = findViewById(R.id.progressCalories)
        progressWorkout = findViewById(R.id.progressWorkout)

        val currentDate = SimpleDateFormat(
            "MMMM dd, yyyy\nEEEE",
            Locale.getDefault()
        ).format(Date())

        dateText.text = currentDate
        carabuff.setImageResource(R.drawable.carabuff1)

        requestNotificationPermission()
        fetchAndSaveFcmToken()

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

            playTalkAnimation()
            typeText(messages.random())
        }

        btnAddWorkout.setOnClickListener {
            startActivity(Intent(this, AddWorkoutActivity::class.java))
        }

        btnAddFood.setOnClickListener {
            startActivity(Intent(this, AddFoodActivity::class.java))
        }

        btnAskCarabuff.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }

        setupNavbar()
    }

    override fun onResume() {
        super.onResume()
        checkDailyReset()
        loadData()

        // Local reminder lang ito habang bukas ang app
        checkMealReminder()
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
        navHome.setOnClickListener {}
        navAnalytics.setOnClickListener {
            startActivity(Intent(this, AnalyticsActivity::class.java))
        }
        navNotif.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }
        navProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
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

                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val greeting = when (hour) {
                    in 0..11 -> "Good morning"
                    in 12..17 -> "Good afternoon"
                    else -> "Good evening"
                }

                if (!isTalking) {
                    playTalkAnimation()
                    typeText("$greeting, $userName 👋")
                }
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

    private fun playTalkAnimation() {
        carabuff.setImageResource(R.drawable.carabuff_talk_anim)
        val anim = carabuff.drawable as AnimationDrawable
        anim.start()

        val totalDuration = (0 until anim.numberOfFrames).sumOf {
            anim.getDuration(it)
        }

        carabuff.postDelayed({
            carabuff.setImageResource(R.drawable.carabuff1)
        }, totalDuration.toLong())
    }

    private fun typeText(text: String) {
        handler.removeCallbacksAndMessages(null)
        isTalking = true
        thoughtText.text = ""

        var index = 0

        typingRunnable = object : Runnable {
            override fun run() {
                if (index < text.length) {
                    thoughtText.text = text.substring(0, index + 1)
                    index++
                    handler.postDelayed(this, 40)
                } else {
                    deletingRunnable = Runnable {
                        deleteText(text)
                    }
                    handler.postDelayed(deletingRunnable!!, 1500)
                }
            }
        }

        handler.post(typingRunnable!!)
    }

    private fun deleteText(text: String) {
        var index = text.length

        deletingRunnable = object : Runnable {
            override fun run() {
                if (index > 0) {
                    index--
                    thoughtText.text = text.substring(0, index)
                    handler.postDelayed(this, 25)
                } else {
                    thoughtText.text = "..."
                    isTalking = false
                }
            }
        }

        handler.post(deletingRunnable!!)
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
                saveToDb = false
            )
            prefs.edit().putBoolean("breakfast_$today", true).apply()
        }

        if (hour in 12..16 && !prefs.getBoolean("lunch_$today", false)) {
            NotificationHelper.showNotification(
                context = this,
                title = "Lunch Reminder 🍛",
                message = "Don't forget to log your lunch!",
                type = "meal",
                target = "home",
                saveToDb = false
            )
            prefs.edit().putBoolean("lunch_$today", true).apply()
        }

        if (hour in 18..23 && !prefs.getBoolean("dinner_$today", false)) {
            NotificationHelper.showNotification(
                context = this,
                title = "Dinner Reminder 🍽",
                message = "Don't forget to log your dinner!",
                type = "meal",
                target = "home",
                saveToDb = false
            )
            prefs.edit().putBoolean("dinner_$today", true).apply()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}