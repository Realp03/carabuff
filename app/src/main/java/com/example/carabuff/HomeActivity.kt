package com.example.carabuff

import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var carabuff: ImageView
    private lateinit var thoughtText: TextView
    private lateinit var dateText: TextView

    private lateinit var btnAddWorkout: Button
    private lateinit var btnAddFood: Button

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

        // 🔥 CANCEL NOTIFICATION (FIX)
        val notifId = intent.getIntExtra("notifId", -1)
        if (notifId != -1) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.cancel(notifId)
        }

        // 🔥 SMART DAILY NOTIFICATION
        val helper = NotificationHelper(this)

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val lastOpen = prefs.getString("last_open_date", "")

        if (lastOpen != today) {

            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

            val message = when (hour) {
                in 5..11 -> listOf(
                    "Start your day strong 💪",
                    "Time to hit your goals 🔥",
                    "Let’s make today productive 🚀"
                ).random()

                in 12..17 -> listOf(
                    "Stay on track this afternoon 💯",
                    "Don’t forget your meals 🍔",
                    "Keep pushing, you're doing great 🔥"
                ).random()

                else -> listOf(
                    "Wrap up your day strong 🌙",
                    "Check your progress today 📊",
                    "One last push before rest 💪"
                ).random()
            }

            val title = when (hour) {
                in 5..11 -> "Good Morning ☀️"
                in 12..17 -> "Good Afternoon 🌤️"
                else -> "Good Evening 🌙"
            }

            helper.sendNotification(title, message, "daily_reminder")

            prefs.edit().putString("last_open_date", today).apply()
        }

        carabuff = findViewById(R.id.carabuffHome)
        thoughtText = findViewById(R.id.carabuffThought)
        dateText = findViewById(R.id.dateText)

        btnAddWorkout = findViewById(R.id.btnAddWorkout)
        btnAddFood = findViewById(R.id.btnAddFood)

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

        setupNavbar()
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

    override fun onResume() {
        super.onResume()
        checkDailyReset()
        loadData()

        checkMealReminder()
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

                progressWorkout.max = workoutMinutes
                progressWorkout.progress = workoutDone

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

                progressCalories.max = maxCalories
                progressCalories.progress = calories
            }
    }

    private fun loadCaloriesBurned() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
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

        val helper = NotificationHelper(this)

        // BREAKFAST
        if (hour in 6..10 && !prefs.getBoolean("breakfast_$today", false)) {
            helper.sendNotification(
                "Breakfast Reminder 🍳",
                "Don't forget to log your breakfast!",
                "meal"
            )
            prefs.edit().putBoolean("breakfast_$today", true).apply()
        }

        // LUNCH
        if (hour in 12..16 && !prefs.getBoolean("lunch_$today", false)) {
            helper.sendNotification(
                "Lunch Reminder 🍛",
                "Don't forget to log your lunch!",
                "meal"
            )
            prefs.edit().putBoolean("lunch_$today", true).apply()
        }

        // DINNER
        if (hour in 18..23 && !prefs.getBoolean("dinner_$today", false)) {
            helper.sendNotification(
                "Dinner Reminder 🍽",
                "Don't forget to log your dinner!",
                "meal"
            )
            prefs.edit().putBoolean("dinner_$today", true).apply()
        }
    }
}