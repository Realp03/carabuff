package com.example.carabuff

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.style.ForegroundColorSpan
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var calendarView: MaterialCalendarView
    private lateinit var lineChartWeekly: LineChart
    private lateinit var pieChartMacros: PieChart

    private lateinit var navHome: ImageView
    private lateinit var navAnalytics: ImageView
    private lateinit var navNotif: ImageView
    private lateinit var navProfile: ImageView

    private lateinit var tvTodayCalories: TextView
    private lateinit var tvTodayBurn: TextView
    private lateinit var tvTodayNet: TextView
    private lateinit var tvHighestCalories: TextView
    private lateinit var tvLowestCalories: TextView
    private lateinit var tvHighestBurn: TextView
    private lateinit var tvLowestBurn: TextView
    private lateinit var tvMaxWorkout: TextView
    private lateinit var tvMinWorkout: TextView
    private lateinit var tvStreak: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        calendarView = findViewById(R.id.calendarView)
        lineChartWeekly = findViewById(R.id.lineChartWeekly)
        pieChartMacros = findViewById(R.id.pieChartMacros)

        navHome = findViewById(R.id.navHome)
        navAnalytics = findViewById(R.id.navAnalytics)
        navNotif = findViewById(R.id.navNotif)
        navProfile = findViewById(R.id.navProfile)

        tvTodayCalories = findViewById(R.id.tvTodayCalories)
        tvTodayBurn = findViewById(R.id.tvTodayBurn)
        tvTodayNet = findViewById(R.id.tvTodayNet)
        tvHighestCalories = findViewById(R.id.tvHighestCalories)
        tvLowestCalories = findViewById(R.id.tvLowestCalories)
        tvHighestBurn = findViewById(R.id.tvHighestBurn)
        tvLowestBurn = findViewById(R.id.tvLowestBurn)
        tvMaxWorkout = findViewById(R.id.tvMaxWorkout)
        tvMinWorkout = findViewById(R.id.tvMinWorkout)
        tvStreak = findViewById(R.id.tvStreak)

        setupNavbar()
        setupCalendarStyle()
        setupWeeklyChartStyle()
        setupPieChartStyle()
        loadAnalytics()
    }

    private fun setupNavbar() {
        navHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        navAnalytics.setOnClickListener {
            // current page
        }

        navNotif.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
            finish()
        }

        navProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    private fun setupCalendarStyle() {
        calendarView.setSelectionColor(Color.parseColor("#D8BFD8"))
        calendarView.topbarVisible = true
        calendarView.setTitleFormatter { day ->
            val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            sdf.format(day.date)
        }
    }

    private fun setupWeeklyChartStyle() {
        lineChartWeekly.setBackgroundColor(Color.parseColor("#263544"))
        lineChartWeekly.description.isEnabled = false
        lineChartWeekly.legend.textColor = Color.WHITE
        lineChartWeekly.axisRight.isEnabled = false

        lineChartWeekly.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChartWeekly.xAxis.textColor = Color.WHITE
        lineChartWeekly.xAxis.granularity = 1f
        lineChartWeekly.xAxis.setDrawGridLines(false)

        lineChartWeekly.axisLeft.textColor = Color.WHITE
        lineChartWeekly.axisLeft.gridColor = Color.parseColor("#4A6572")

        lineChartWeekly.setTouchEnabled(false)
        lineChartWeekly.setPinchZoom(false)
    }

    private fun setupPieChartStyle() {
        pieChartMacros.description.isEnabled = false
        pieChartMacros.legend.textColor = Color.WHITE
        pieChartMacros.setEntryLabelColor(Color.WHITE)
        pieChartMacros.setCenterTextColor(Color.WHITE)
        pieChartMacros.isDrawHoleEnabled = true
        pieChartMacros.holeRadius = 58f
        pieChartMacros.transparentCircleRadius = 62f
        pieChartMacros.setUsePercentValues(false)
        pieChartMacros.setNoDataText("No macro data yet")
        pieChartMacros.setNoDataTextColor(Color.WHITE)
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

        var daysWithFood = 0

        val activeDates = mutableSetOf<CalendarDay>()
        val streakDates = mutableSetOf<String>()

        val weeklyCaloriesMap = linkedMapOf<String, Int>()
        val weeklyBurnMap = linkedMapOf<String, Int>()
        val weekLabels = mutableListOf<String>()

        val weekCalendar = Calendar.getInstance()
        weekCalendar.set(Calendar.HOUR_OF_DAY, 0)
        weekCalendar.set(Calendar.MINUTE, 0)
        weekCalendar.set(Calendar.SECOND, 0)
        weekCalendar.set(Calendar.MILLISECOND, 0)
        weekCalendar.add(Calendar.DAY_OF_MONTH, -6)

        for (i in 0..6) {
            val dateKey = sdf.format(weekCalendar.time)
            weeklyCaloriesMap[dateKey] = 0
            weeklyBurnMap[dateKey] = 0
            weekLabels.add(SimpleDateFormat("EEE", Locale.getDefault()).format(weekCalendar.time))
            weekCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        db.collection("users").document(userId)
            .collection("foodLogs")
            .get()
            .addOnSuccessListener { foodResult ->

                val foodDates = mutableSetOf<String>()

                for (doc in foodResult) {
                    val calories = (doc["calories"] as? Long)?.toInt() ?: 0
                    val protein = doc["protein"] as? Double ?: 0.0
                    val carbs = doc["carbs"] as? Double ?: 0.0
                    val fats = doc["fats"] as? Double ?: 0.0
                    val timestamp = doc["timestamp"] as? Long ?: continue

                    val date = sdf.format(Date(timestamp))
                    foodDates.add(date)
                    streakDates.add(date)

                    if (date == today) {
                        todayCalories += calories
                    }

                    if (weeklyCaloriesMap.containsKey(date)) {
                        weeklyCaloriesMap[date] = (weeklyCaloriesMap[date] ?: 0) + calories
                    }

                    totalCalories += calories
                    totalProtein += protein
                    totalCarbs += carbs
                    totalFats += fats

                    if (calories > maxCalories) maxCalories = calories
                    if (calories < minCalories) minCalories = calories
                }

                daysWithFood = foodDates.size

                updateFoodUI(
                    todayCalories = todayCalories,
                    maxCalories = maxCalories,
                    minCalories = minCalories
                )

                updateMacrosChart(
                    totalCalories = totalCalories,
                    totalProtein = totalProtein,
                    totalCarbs = totalCarbs,
                    totalFats = totalFats,
                    days = daysWithFood
                )

                updateWeeklyChart(
                    labels = weekLabels,
                    caloriesData = weeklyCaloriesMap.values.toList(),
                    burnData = weeklyBurnMap.values.toList()
                )

                updateStreak(streakDates)
            }

        db.collection("users").document(userId)
            .collection("workouts")
            .get()
            .addOnSuccessListener { workoutResult ->

                for (doc in workoutResult) {
                    val burn = (doc["caloriesBurned"] as? Long)?.toInt() ?: 0
                    val minutes = (doc["minutes"] as? Long)?.toInt() ?: 0
                    val timestamp = doc["timestamp"] as? Long ?: continue

                    val date = sdf.format(Date(timestamp))
                    streakDates.add(date)

                    val dayCal = Calendar.getInstance()
                    dayCal.timeInMillis = timestamp

                    activeDates.add(
                        CalendarDay.from(
                            dayCal.get(Calendar.YEAR),
                            dayCal.get(Calendar.MONTH),
                            dayCal.get(Calendar.DAY_OF_MONTH)
                        )
                    )

                    if (date == today) {
                        todayBurn += burn
                    }

                    if (weeklyBurnMap.containsKey(date)) {
                        weeklyBurnMap[date] = (weeklyBurnMap[date] ?: 0) + burn
                    }

                    if (burn > maxBurn) maxBurn = burn
                    if (burn < minBurn) minBurn = burn

                    if (minutes > maxTime) maxTime = minutes
                    if (minutes < minTime) minTime = minutes
                }

                updateWorkoutUI(
                    todayCalories = todayCalories,
                    todayBurn = todayBurn,
                    maxBurn = maxBurn,
                    minBurn = minBurn,
                    maxTime = maxTime,
                    minTime = minTime
                )

                updateWeeklyChart(
                    labels = weekLabels,
                    caloriesData = weeklyCaloriesMap.values.toList(),
                    burnData = weeklyBurnMap.values.toList()
                )

                updateStreak(streakDates)

                calendarView.removeDecorators()
                calendarView.addDecorator(FireDecorator(activeDates))
            }

        calendarView.setOnDateChangedListener { _, date, _ ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(date.year, date.month, date.day)

            val selectedDate = sdf.format(selectedCalendar.time)

            val intent = Intent(this, DailySummaryActivity::class.java)
            intent.putExtra("date", selectedDate)
            startActivity(intent)
        }
    }

    private fun updateFoodUI(
        todayCalories: Int,
        maxCalories: Int,
        minCalories: Int
    ) {
        val safeMinCalories = if (minCalories == Int.MAX_VALUE) 0 else minCalories

        tvTodayCalories.text = "Calories: $todayCalories"
        tvHighestCalories.text = "Highest Intake: $maxCalories kcal"
        tvLowestCalories.text = "Lowest Intake: $safeMinCalories kcal"
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

        tvTodayBurn.text = "Burned: $todayBurn"
        tvTodayNet.text = "Net: ${todayCalories - todayBurn}"
        tvHighestBurn.text = "Highest Burn: $maxBurn kcal"
        tvLowestBurn.text = "Lowest Burn: $safeMinBurn kcal"
        tvMaxWorkout.text = "Longest Workout: $maxTime mins"
        tvMinWorkout.text = "Shortest Workout: $safeMinTime mins"
    }

    private fun updateMacrosChart(
        totalCalories: Int,
        totalProtein: Double,
        totalCarbs: Double,
        totalFats: Double,
        days: Int
    ) {
        val safeDays = if (days <= 0) 1 else days

        val avgCalories = totalCalories.toFloat() / safeDays
        val avgProtein = totalProtein.toFloat() / safeDays
        val avgCarbs = totalCarbs.toFloat() / safeDays
        val avgFats = totalFats.toFloat() / safeDays

        if (avgCalories == 0f && avgProtein == 0f && avgCarbs == 0f && avgFats == 0f) {
            pieChartMacros.clear()
            pieChartMacros.centerText = "No Data Yet 🍔"
            pieChartMacros.invalidate()
            return
        }

        val entries = listOf(
            PieEntry(avgCalories, "Calories"),
            PieEntry(avgProtein, "Protein"),
            PieEntry(avgCarbs, "Carbs"),
            PieEntry(avgFats, "Fats")
        )

        val dataSet = PieDataSet(entries, "Average / Day").apply {
            colors = listOf(
                Color.parseColor("#4FC3F7"),
                Color.parseColor("#EF5350"),
                Color.parseColor("#66BB6A"),
                Color.parseColor("#FFCA28")
            )
            valueTextColor = Color.WHITE
            valueTextSize = 13f
            sliceSpace = 3f
        }

        val data = PieData(dataSet)
        pieChartMacros.data = data
        pieChartMacros.centerText = "Avg / Day"
        pieChartMacros.invalidate()
    }

    private fun updateWeeklyChart(
        labels: List<String>,
        caloriesData: List<Int>,
        burnData: List<Int>
    ) {
        val calorieEntries = mutableListOf<Entry>()
        val burnEntries = mutableListOf<Entry>()

        for (i in labels.indices) {
            calorieEntries.add(Entry(i.toFloat(), caloriesData[i].toFloat()))
            burnEntries.add(Entry(i.toFloat(), burnData[i].toFloat()))
        }

        val calorieDataSet = LineDataSet(calorieEntries, "Calories Intake").apply {
            color = Color.parseColor("#FFA726")
            valueTextColor = Color.WHITE
            lineWidth = 2.5f
            circleRadius = 4f
            setCircleColor(Color.parseColor("#FFA726"))
            setDrawValues(false)
        }

        val burnDataSet = LineDataSet(burnEntries, "Calories Burned").apply {
            color = Color.parseColor("#4FC3F7")
            valueTextColor = Color.WHITE
            lineWidth = 2.5f
            circleRadius = 4f
            setCircleColor(Color.parseColor("#4FC3F7"))
            setDrawValues(false)
        }

        lineChartWeekly.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        lineChartWeekly.data = LineData(calorieDataSet, burnDataSet)
        lineChartWeekly.invalidate()
    }

    private fun updateStreak(activeDateStrings: Set<String>) {
        var streak = 0
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        while (true) {
            val dateKey = sdf.format(calendar.time)
            if (activeDateStrings.contains(dateKey)) {
                streak++
                calendar.add(Calendar.DAY_OF_MONTH, -1)
            } else {
                break
            }
        }

        tvStreak.text = "🔥 Current Streak: $streak day${if (streak == 1) "" else "s"}"
    }
}

class FireDecorator(private val dates: Set<CalendarDay>) : DayViewDecorator {
    override fun shouldDecorate(day: CalendarDay): Boolean {
        return dates.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(DotSpan(10f, Color.parseColor("#FFA726")))
        view.addSpan(ForegroundColorSpan(Color.WHITE))
    }
}