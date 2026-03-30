package com.example.carabuff

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.prolificinteractive.materialcalendarview.*
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var calendarView: MaterialCalendarView

    // 🔥 NAVBAR
    private lateinit var navHome: ImageView
    private lateinit var navAnalytics: ImageView
    private lateinit var navNotif: ImageView
    private lateinit var navProfile: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        pieChart = findViewById(R.id.pieChartMacros)
        calendarView = findViewById(R.id.calendarView)

        // 🔥 NAVBAR INIT
        navHome = findViewById(R.id.navHome)
        navAnalytics = findViewById(R.id.navAnalytics)
        navNotif = findViewById(R.id.navNotif)
        navProfile = findViewById(R.id.navProfile)

        setupNavbar()
        loadAnalytics()
    }

    // 🔥 NAVBAR FUNCTION
    private fun setupNavbar() {

        navHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }

        navAnalytics.setOnClickListener {
            // already here
        }

        navNotif.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }

        navProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun loadAnalytics() {

        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())

        var todayCalories = 0
        var todayBurn = 0

        var maxCalories = 0
        var minCalories = Int.MAX_VALUE

        var maxBurn = 0
        var minBurn = Int.MAX_VALUE

        var maxTime = 0
        var minTime = Int.MAX_VALUE

        var totalCalories = 0
        var totalProtein = 0.0
        var totalCarbs = 0.0
        var totalFats = 0.0

        val dateSet = mutableSetOf<String>()
        val activeDates = mutableSetOf<CalendarDay>()

        // 🍔 FOOD DATA
        db.collection("users").document(userId)
            .collection("foodLogs")
            .get()
            .addOnSuccessListener { foodResult ->

                for (doc in foodResult) {

                    val calories = (doc["calories"] as? Long)?.toInt() ?: 0
                    val protein = doc["protein"] as? Double ?: 0.0
                    val carbs = doc["carbs"] as? Double ?: 0.0
                    val fats = doc["fats"] as? Double ?: 0.0
                    val timestamp = doc["timestamp"] as? Long ?: continue

                    val date = sdf.format(Date(timestamp))

                    if (date == today) {
                        todayCalories += calories
                    }

                    totalCalories += calories
                    totalProtein += protein
                    totalCarbs += carbs
                    totalFats += fats

                    dateSet.add(date)

                    if (calories > maxCalories) maxCalories = calories
                    if (calories < minCalories) minCalories = calories
                }

                updateFoodUI(
                    todayCalories,
                    maxCalories,
                    minCalories,
                    dateSet.size,
                    totalCalories,
                    totalProtein,
                    totalCarbs,
                    totalFats
                )
            }

        // 💪 WORKOUT DATA
        db.collection("users").document(userId)
            .collection("workouts")
            .get()
            .addOnSuccessListener { workoutResult ->

                for (doc in workoutResult) {

                    val burn = (doc["caloriesBurned"] as? Long)?.toInt() ?: 0
                    val minutes = (doc["minutes"] as? Long)?.toInt() ?: 0
                    val timestamp = doc["timestamp"] as? Long ?: continue

                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = timestamp

                    val date = sdf.format(Date(timestamp))

                    if (date == today) {
                        todayBurn += burn
                    }

                    if (burn > maxBurn) maxBurn = burn
                    if (burn < minBurn) minBurn = burn

                    if (minutes > maxTime) maxTime = minutes
                    if (minutes < minTime) minTime = minutes

                    activeDates.add(
                        CalendarDay.from(
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )
                    )
                }

                updateWorkoutUI(todayCalories, todayBurn, maxBurn, minBurn, maxTime, minTime)

                // 🔥 FIXED ERROR HERE
                calendarView.addDecorator(FireDecorator(activeDates))
            }

        calendarView.setOnDateChangedListener { _, date, _ ->

            val calendar = Calendar.getInstance()
            calendar.set(date.year, date.month - 1, date.day)

            val sdfClick = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val selectedDate = sdfClick.format(calendar.time)

            val intent = Intent(this, DailySummaryActivity::class.java)
            intent.putExtra("date", selectedDate)
            startActivity(intent)
        }
    }

    private fun updateFoodUI(
        todayCalories: Int,
        maxCalories: Int,
        minCalories: Int,
        days: Int,
        totalCalories: Int,
        totalProtein: Double,
        totalCarbs: Double,
        totalFats: Double
    ) {

        val safeMinCalories = if (minCalories == Int.MAX_VALUE) 0 else minCalories

        findViewById<TextView>(R.id.tvTodayCalories).text =
            "Calories: $todayCalories"

        findViewById<TextView>(R.id.tvHighestCalories).text =
            "Highest Intake: $maxCalories kcal"

        findViewById<TextView>(R.id.tvLowestCalories).text =
            "Lowest Intake: $safeMinCalories kcal"

        val safeDays = if (days == 0) 1 else days

        val avgCalories = totalCalories / safeDays
        val avgProtein = totalProtein / safeDays
        val avgCarbs = totalCarbs / safeDays
        val avgFats = totalFats / safeDays

        if (avgCalories == 0 && avgProtein == 0.0 && avgCarbs == 0.0 && avgFats == 0.0) {
            pieChart.clear()
            pieChart.centerText = "No Data Yet 🍔"
            return
        }

        setupChart(avgCalories, avgProtein, avgCarbs, avgFats)
    }

    private fun updateWorkoutUI(
        todayCalories: Int,
        todayBurn: Int,
        maxBurn: Int,
        minBurn: Int,
        maxTime: Int,
        minTime: Int
    ) {

        val safeMinBurn = if (minBurn == Int.MAX_VALUE) 0 else minBurn
        val safeMinTime = if (minTime == Int.MAX_VALUE) 0 else minTime

        findViewById<TextView>(R.id.tvTodayBurn).text =
            "Burned: $todayBurn"

        val net = todayCalories - todayBurn

        findViewById<TextView>(R.id.tvTodayNet).text =
            "Net: $net"

        findViewById<TextView>(R.id.tvHighestBurn).text =
            "Highest Burn: $maxBurn kcal"

        findViewById<TextView>(R.id.tvLowestBurn).text =
            "Lowest Burn: $safeMinBurn kcal"

        findViewById<TextView>(R.id.tvMaxWorkout).text =
            "Longest Workout: $maxTime mins"

        findViewById<TextView>(R.id.tvMinWorkout).text =
            "Shortest Workout: $safeMinTime mins"
    }

    private fun setupChart(
        calories: Int,
        protein: Double,
        carbs: Double,
        fats: Double
    ) {

        val entries = listOf(
            PieEntry(calories.toFloat(), "Calories"),
            PieEntry(protein.toFloat(), "Protein"),
            PieEntry(carbs.toFloat(), "Carbs"),
            PieEntry(fats.toFloat(), "Fats")
        )

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW)

        val data = PieData(dataSet)
        data.setValueTextSize(14f)

        pieChart.data = data
        pieChart.isDrawHoleEnabled = true
        pieChart.holeRadius = 60f
        pieChart.centerText = "Avg / Day"
        pieChart.invalidate()
    }
}


// 🔥🔥🔥 FIXED MISSING CLASS (IMPORTANT)
class FireDecorator(private val dates: Set<CalendarDay>) : DayViewDecorator {

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return dates.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        view.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(
                android.graphics.Color.parseColor("#FFA726")
            )
        )
    }
}