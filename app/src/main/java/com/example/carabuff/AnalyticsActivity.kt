package com.example.carabuff

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
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
import java.util.Calendar
import java.util.Date
import java.util.LinkedHashMap
import java.util.Locale

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var calendarView: MaterialCalendarView
    private lateinit var lineChartWeekly: LineChart
    private lateinit var pieChartMacros: PieChart

    private lateinit var imgAvatar: ImageView

    private lateinit var tvHeaderDate: TextView
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

    private lateinit var chipDay1: LinearLayout
    private lateinit var chipDay2: LinearLayout
    private lateinit var chipDay3: LinearLayout
    private lateinit var chipDay4: LinearLayout

    private lateinit var day1Text: TextView
    private lateinit var day2Text: TextView
    private lateinit var day3Text: TextView
    private lateinit var day4Text: TextView

    private lateinit var date1Text: TextView
    private lateinit var date2Text: TextView
    private lateinit var date3Text: TextView
    private lateinit var date4Text: TextView

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

    private var foodLoaded = false
    private var workoutLoaded = false
    private var dashboardDataAnimated = false

    // SHARED STATE FOR BOTH FIRESTORE REQUESTS
    private lateinit var todayKey: String
    private lateinit var weekLabels: MutableList<String>
    private lateinit var weeklyCaloriesMap: LinkedHashMap<String, Int>
    private lateinit var weeklyBurnMap: LinkedHashMap<String, Int>
    private var streakDates = mutableSetOf<String>()
    private var activeDates = mutableSetOf<CalendarDay>()

    private var todayCalories = 0
    private var todayBurn = 0

    private var maxCalories = 0
    private var minCalories = Int.MAX_VALUE

    private var maxBurn = 0
    private var minBurn = Int.MAX_VALUE

    private var maxTime = 0
    private var minTime = Int.MAX_VALUE

    private var totalCalories = 0
    private var totalProtein = 0.0
    private var totalCarbs = 0.0
    private var totalFats = 0.0
    private var daysWithFood = 0

    companion object {
        private const val ICON_ACTIVE_COLOR = "#111111"
        private const val ICON_INACTIVE_COLOR = "#B8C7D6"
        private const val LABEL_ACTIVE_COLOR = "#111111"
        private const val LABEL_INACTIVE_COLOR = "#8FA3B8"

        private const val NAV_ACTIVE_ALPHA_START = 0.60f
        private const val NAV_INACTIVE_ALPHA_START = 0.82f
        private const val NAV_ACTIVE_DURATION = 180L
        private const val NAV_INACTIVE_DURATION = 130L

        private const val CONTENT_FADE_DURATION = 220L
        private const val DASHBOARD_ITEM_DURATION = 170L
        private const val DASHBOARD_ITEM_DELAY_STEP = 8L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        bindViews()
        setupHeader()
        setupDayChips()
        setupNavbar()
        setupCalendarStyle()
        setupWeeklyChartStyle()
        setupPieChartStyle()
        loadUserProfileImage()
        updateNotificationDot()

        val fromNavbar = intent.getBooleanExtra("from_navbar", false)
        if (fromNavbar) {
            prepareContentForFade()
        } else {
            prepareLoadingState()
        }

        loadAnalytics()
    }

    override fun onResume() {
        super.onResume()

        setActiveNav("analytics", animate = false)
        updateNotificationDot()

        val fromNavbar = intent.getBooleanExtra("from_navbar", false)

        if (fromNavbar) {
            prepareContentForFade()
            animatePageContentOnly()
            intent.removeExtra("from_navbar")
        } else if (dashboardDataAnimated) {
            showContentImmediately()
        }
    }

    private fun bindViews() {
        calendarView = findViewById(R.id.calendarView)
        lineChartWeekly = findViewById(R.id.lineChartWeekly)
        pieChartMacros = findViewById(R.id.pieChartMacros)

        imgAvatar = findViewById(R.id.imgAvatar)

        tvHeaderDate = findViewById(R.id.tvHeaderDate)
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

        chipDay1 = findViewById(R.id.chipDay1)
        chipDay2 = findViewById(R.id.chipDay2)
        chipDay3 = findViewById(R.id.chipDay3)
        chipDay4 = findViewById(R.id.chipDay4)

        day1Text = findViewById(R.id.day1Text)
        day2Text = findViewById(R.id.day2Text)
        day3Text = findViewById(R.id.day3Text)
        day4Text = findViewById(R.id.day4Text)

        date1Text = findViewById(R.id.date1Text)
        date2Text = findViewById(R.id.date2Text)
        date3Text = findViewById(R.id.date3Text)
        date4Text = findViewById(R.id.date4Text)

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

    private fun setupHeader() {
        val headerFormat = SimpleDateFormat("EEE, MMM dd yyyy", Locale.getDefault())
        tvHeaderDate.text = headerFormat.format(Date())
    }

    private fun setupDayChips() {
        val today = Calendar.getInstance()

        val chipLayouts = listOf(chipDay1, chipDay2, chipDay3, chipDay4)
        val dayTexts = listOf(day1Text, day2Text, day3Text, day4Text)
        val dateTexts = listOf(date1Text, date2Text, date3Text, date4Text)

        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd", Locale.getDefault())

        for (i in 0..3) {
            val cal = today.clone() as Calendar
            cal.add(Calendar.DAY_OF_MONTH, i)

            dayTexts[i].text = dayFormat.format(cal.time)
            dateTexts[i].text = dateFormat.format(cal.time)

            if (i == 0) {
                chipLayouts[i].setBackgroundResource(R.drawable.bg_signup_button)
                dayTexts[i].setTextColor(Color.parseColor("#EAF8FF"))
                dateTexts[i].setTextColor(Color.WHITE)
                dateTexts[i].textSize = 24f
            } else {
                chipLayouts[i].setBackgroundResource(R.drawable.bg_signup_card)
                dayTexts[i].setTextColor(Color.parseColor("#AFC2DE"))
                dateTexts[i].setTextColor(Color.WHITE)
                dateTexts[i].textSize = 22f
            }
        }
    }

    private fun setupNavbar() {
        setActiveNav("analytics", animate = false)

        navHomeContainer.setOnClickListener {
            openTab("home", HomeActivity::class.java)
        }

        navAnalyticsContainer.setOnClickListener {
            setActiveNav("analytics", animate = true)
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

    private fun updateNotificationDot() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            navNotifDot.visibility = View.GONE
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("notifications")
            .whereEqualTo("read", false)
            .get()
            .addOnSuccessListener { result ->
                navNotifDot.visibility = if (result.isEmpty) View.GONE else View.VISIBLE
            }
            .addOnFailureListener {
                navNotifDot.visibility = View.GONE
            }
    }

    private fun loadUserProfileImage() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("user_profiles")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val imageUrl = document.getString("profileImage")
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.profile_icon)
                        .error(R.drawable.profile_icon)
                        .circleCrop()
                        .into(imgAvatar)
                } else {
                    imgAvatar.setImageResource(R.drawable.profile_icon)
                }
            }
            .addOnFailureListener {
                imgAvatar.setImageResource(R.drawable.profile_icon)
            }
    }

    private fun setupCalendarStyle() {
        calendarView.setSelectionColor(Color.parseColor("#26D9FF"))
        calendarView.topbarVisible = true
        calendarView.showOtherDates = MaterialCalendarView.SHOW_ALL

        calendarView.setTitleFormatter { day ->
            val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            sdf.format(day.date)
        }
    }

    private fun setupWeeklyChartStyle() {
        lineChartWeekly.setBackgroundColor(Color.TRANSPARENT)
        lineChartWeekly.description.isEnabled = false
        lineChartWeekly.setNoDataText("No weekly data yet")
        lineChartWeekly.setNoDataTextColor(Color.parseColor("#AFC2DE"))
        lineChartWeekly.setTouchEnabled(false)
        lineChartWeekly.setPinchZoom(false)
        lineChartWeekly.setScaleEnabled(false)
        lineChartWeekly.setDrawGridBackground(false)
        lineChartWeekly.axisRight.isEnabled = false
        lineChartWeekly.isDoubleTapToZoomEnabled = false
        lineChartWeekly.setExtraOffsets(8f, 12f, 8f, 8f)

        val legend = lineChartWeekly.legend
        legend.textColor = Color.WHITE
        legend.form = Legend.LegendForm.LINE
        legend.textSize = 11f
        legend.isWordWrapEnabled = true

        val xAxis = lineChartWeekly.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = Color.parseColor("#AFC2DE")
        xAxis.textSize = 11f
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(false)

        val leftAxis = lineChartWeekly.axisLeft
        leftAxis.textColor = Color.parseColor("#AFC2DE")
        leftAxis.textSize = 11f
        leftAxis.gridColor = Color.parseColor("#2E4260")
        leftAxis.setDrawAxisLine(false)
        leftAxis.axisMinimum = 0f
    }

    private fun setupPieChartStyle() {
        pieChartMacros.description.isEnabled = false
        pieChartMacros.legend.isEnabled = false
        pieChartMacros.setUsePercentValues(false)
        pieChartMacros.setDrawEntryLabels(false)
        pieChartMacros.setEntryLabelColor(Color.WHITE)
        pieChartMacros.setCenterTextColor(Color.WHITE)
        pieChartMacros.setCenterTextSize(14f)
        pieChartMacros.isDrawHoleEnabled = true
        pieChartMacros.holeRadius = 64f
        pieChartMacros.transparentCircleRadius = 68f
        pieChartMacros.setHoleColor(Color.parseColor("#22324B"))
        pieChartMacros.setTransparentCircleColor(Color.parseColor("#22324B"))
        pieChartMacros.setTransparentCircleAlpha(70)
        pieChartMacros.setNoDataText("No macro data yet")
        pieChartMacros.setNoDataTextColor(Color.parseColor("#AFC2DE"))
    }

    private fun prepareLoadingState() {
        tvTodayCalories.text = "Calories: --"
        tvTodayBurn.text = "Burned: --"
        tvTodayNet.text = "Net: --"
        tvHighestCalories.text = "Highest Intake: --"
        tvLowestCalories.text = "Lowest Intake: --"
        tvHighestBurn.text = "Highest Burn: --"
        tvLowestBurn.text = "Lowest Burn: --"
        tvMaxWorkout.text = "Longest Workout: --"
        tvMinWorkout.text = "Shortest Workout: --"
        tvStreak.text = "Loading..."

        getDashboardContentViews().forEach { view ->
            view.animate().cancel()
            view.alpha = 0f
            view.translationY = 8f
        }

        lineChartWeekly.clear()
        pieChartMacros.clear()
        calendarView.removeDecorators()
    }

    private fun prepareContentForFade() {
        getDashboardContentViews().forEach { view ->
            view.animate().cancel()
            view.alpha = 0f
        }
    }

    private fun animatePageContentOnly() {
        getDashboardContentViews().forEachIndexed { index, view ->
            view.animate().cancel()
            view.animate()
                .alpha(1f)
                .setStartDelay(index * 18L)
                .setDuration(CONTENT_FADE_DURATION)
                .start()
        }
    }

    private fun animateLoadedDashboard() {
        if (dashboardDataAnimated) return
        dashboardDataAnimated = true

        val views = getDashboardContentViews()
        val interpolator = DecelerateInterpolator()

        views.forEachIndexed { index, view ->
            view.animate().cancel()
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(DASHBOARD_ITEM_DURATION)
                .setStartDelay(index * DASHBOARD_ITEM_DELAY_STEP)
                .setInterpolator(interpolator)
                .start()
        }

        lineChartWeekly.post { lineChartWeekly.animateX(650) }
        pieChartMacros.post { pieChartMacros.animateY(650, Easing.EaseInOutQuad) }

        enableAllNavButtons()
    }

    private fun showContentImmediately() {
        getDashboardContentViews().forEach { view ->
            view.animate().cancel()
            view.alpha = 1f
            view.translationY = 0f
        }
        enableAllNavButtons()
    }

    private fun getDashboardContentViews(): List<View> {
        return listOf(
            chipDay1,
            chipDay2,
            chipDay3,
            chipDay4,
            tvTodayCalories,
            tvTodayBurn,
            tvTodayNet,
            tvHighestCalories,
            tvLowestCalories,
            tvHighestBurn,
            tvLowestBurn,
            tvMaxWorkout,
            tvMinWorkout,
            tvStreak,
            lineChartWeekly,
            pieChartMacros,
            calendarView
        )
    }

    private fun tryFinishDashboardLoading() {
        if (foodLoaded && workoutLoaded) {
            animateLoadedDashboard()
        }
    }

    private fun refreshDashboardUI() {
        updateFoodUI(
            todayCalories = todayCalories,
            maxCalories = maxCalories,
            minCalories = minCalories
        )

        updateWorkoutUI(
            todayCalories = todayCalories,
            todayBurn = todayBurn,
            maxBurn = maxBurn,
            minBurn = minBurn,
            maxTime = maxTime,
            minTime = minTime
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

        calendarView.removeDecorators()
        calendarView.addDecorator(FireDecorator(activeDates))
    }

    private fun loadAnalytics() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        todayKey = sdf.format(Date())

        todayCalories = 0
        todayBurn = 0

        maxCalories = 0
        minCalories = Int.MAX_VALUE

        maxBurn = 0
        minBurn = Int.MAX_VALUE

        maxTime = 0
        minTime = Int.MAX_VALUE

        totalCalories = 0
        totalProtein = 0.0
        totalCarbs = 0.0
        totalFats = 0.0
        daysWithFood = 0

        foodLoaded = false
        workoutLoaded = false
        dashboardDataAnimated = false

        activeDates = mutableSetOf()
        streakDates = mutableSetOf()

        weeklyCaloriesMap = LinkedHashMap()
        weeklyBurnMap = LinkedHashMap()
        weekLabels = mutableListOf()

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
                    val calories = (doc.get("calories") as? Number)?.toInt() ?: 0
                    val protein = (doc.get("protein") as? Number)?.toDouble() ?: 0.0
                    val carbs = (doc.get("carbs") as? Number)?.toDouble() ?: 0.0
                    val fats = (doc.get("fats") as? Number)?.toDouble() ?: 0.0
                    val timestamp = (doc.get("timestamp") as? Number)?.toLong() ?: continue

                    val date = sdf.format(Date(timestamp))
                    foodDates.add(date)
                    streakDates.add(date)

                    if (date == todayKey) {
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
                foodLoaded = true

                refreshDashboardUI()
                tryFinishDashboardLoading()
            }
            .addOnFailureListener {
                foodLoaded = true
                refreshDashboardUI()
                tryFinishDashboardLoading()
            }

        db.collection("users").document(userId)
            .collection("workouts")
            .get()
            .addOnSuccessListener { workoutResult ->
                for (doc in workoutResult) {
                    val burn = (doc.get("caloriesBurned") as? Number)?.toInt() ?: 0
                    val minutes = (doc.get("minutes") as? Number)?.toInt() ?: 0
                    val timestamp = (doc.get("timestamp") as? Number)?.toLong() ?: continue

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

                    if (date == todayKey) {
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

                workoutLoaded = true

                refreshDashboardUI()
                tryFinishDashboardLoading()
            }
            .addOnFailureListener {
                workoutLoaded = true
                refreshDashboardUI()
                tryFinishDashboardLoading()
            }

        calendarView.setOnDateChangedListener { _, date, _ ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(date.year, date.month, date.day)

            val selectedDate = sdf.format(selectedCalendar.time)

            val intent = Intent(this, DailySummaryActivity::class.java)
            intent.putExtra("date", selectedDate)
            startActivity(intent)
            overridePendingTransition(0, 0)
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
            pieChartMacros.centerText = "No Data Yet"
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
                Color.parseColor("#26D9FF"),
                Color.parseColor("#FF8A65"),
                Color.parseColor("#66E0A3"),
                Color.parseColor("#FFD166")
            )
            valueTextColor = Color.WHITE
            valueTextSize = 12f
            sliceSpace = 3f
            selectionShift = 6f
        }

        pieChartMacros.data = PieData(dataSet)
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
            color = Color.parseColor("#FF8A65")
            valueTextColor = Color.WHITE
            lineWidth = 3f
            circleRadius = 4.5f
            setCircleColor(Color.parseColor("#FF8A65"))
            setDrawValues(false)
            setDrawFilled(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val burnDataSet = LineDataSet(burnEntries, "Calories Burned").apply {
            color = Color.parseColor("#26D9FF")
            valueTextColor = Color.WHITE
            lineWidth = 3f
            circleRadius = 4.5f
            setCircleColor(Color.parseColor("#26D9FF"))
            setDrawValues(false)
            setDrawFilled(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
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

        tvStreak.text = "🔥 $streak day${if (streak == 1) "" else "s"}"
    }
}

class FireDecorator(private val dates: Set<CalendarDay>) : DayViewDecorator {
    override fun shouldDecorate(day: CalendarDay): Boolean {
        return dates.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(DotSpan(8f, Color.parseColor("#26D9FF")))
        view.addSpan(ForegroundColorSpan(Color.WHITE))
    }
}