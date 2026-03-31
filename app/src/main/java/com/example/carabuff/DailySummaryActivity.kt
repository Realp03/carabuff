package com.example.carabuff

import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

class DailySummaryActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private var foodListener: ListenerRegistration? = null
    private var workoutListener: ListenerRegistration? = null

    private var selectedDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_summary)

        container = findViewById(R.id.containerSummary)

        selectedDate = intent.getStringExtra("date")
            ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        listenDailyData(selectedDate)
    }

    private fun listenDailyData(date: String) {
        val uid = userId ?: return

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val startCal = Calendar.getInstance().apply {
            time = sdf.parse(date) ?: Date()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCal = startCal.clone() as Calendar
        endCal.add(Calendar.DAY_OF_MONTH, 1)

        val startMillis = startCal.timeInMillis
        val endMillis = endCal.timeInMillis

        val displayDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            .format(startCal.time)

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        fun render(foodDocs: List<com.google.firebase.firestore.QueryDocumentSnapshot>?,
                   workoutDocs: List<com.google.firebase.firestore.QueryDocumentSnapshot>?) {

            container.removeAllViews()

            addHeader("📅 Daily Summary for $displayDate")

            // =========================
            // 🍔 FOOD LOGS
            // =========================
            addTitle("🍔 Food Logs")

            var totalFoodCalories = 0
            var totalProtein = 0.0
            var totalCarbs = 0.0
            var totalFats = 0.0
            var hasFood = false

            foodDocs?.forEach { doc ->
                val timestamp = doc.getLong("timestamp") ?: return@forEach
                if (timestamp < startMillis || timestamp >= endMillis) return@forEach

                hasFood = true

                val name = doc.getString("name") ?: "Food"
                val calories = (doc.getLong("calories") ?: 0).toInt()
                val protein = doc.getDouble("protein") ?: 0.0
                val carbs = doc.getDouble("carbs") ?: 0.0
                val fats = doc.getDouble("fats") ?: 0.0
                val time = timeFormat.format(Date(timestamp))

                totalFoodCalories += calories
                totalProtein += protein
                totalCarbs += carbs
                totalFats += fats

                addCard(
                    "$name\n$time\n🔥 $calories kcal | P:${protein.toInt()} C:${carbs.toInt()} F:${fats.toInt()}"
                )
            }

            if (!hasFood) {
                addText("No food logs for this day 🍔")
            } else {
                addSummaryBox(
                    "Food Total",
                    "🔥 $totalFoodCalories kcal\nP:${totalProtein.toInt()}  C:${totalCarbs.toInt()}  F:${totalFats.toInt()}"
                )
            }

            // =========================
            // 💪 WORKOUTS
            // =========================
            addTitle("💪 Workouts")

            var totalWorkoutMinutes = 0
            var totalBurnedCalories = 0
            var hasWorkout = false

            workoutDocs?.forEach { doc ->
                val timestamp = doc.getLong("timestamp") ?: return@forEach
                if (timestamp < startMillis || timestamp >= endMillis) return@forEach

                hasWorkout = true

                val name = doc.getString("name") ?: "Workout"
                val minutes = (doc.getLong("minutes") ?: 0).toInt()
                val calories = (doc.getLong("caloriesBurned") ?: 0).toInt()
                val time = timeFormat.format(Date(timestamp))

                totalWorkoutMinutes += minutes
                totalBurnedCalories += calories

                addCard(
                    "$name\n$time\n⏱ $minutes mins | 🔥 $calories kcal"
                )
            }

            if (!hasWorkout) {
                addText("No workouts for this day 💪")
            } else {
                addSummaryBox(
                    "Workout Total",
                    "⏱ $totalWorkoutMinutes mins\n🔥 $totalBurnedCalories kcal burned"
                )
            }
        }

        var latestFoodDocs: List<com.google.firebase.firestore.QueryDocumentSnapshot>? = null
        var latestWorkoutDocs: List<com.google.firebase.firestore.QueryDocumentSnapshot>? = null

        foodListener?.remove()
        workoutListener?.remove()

        foodListener = db.collection("users").document(uid)
            .collection("foodLogs")
            .addSnapshotListener { snapshot, _ ->
                latestFoodDocs = snapshot?.documents
                    ?.filterIsInstance<com.google.firebase.firestore.QueryDocumentSnapshot>()
                render(latestFoodDocs, latestWorkoutDocs)
            }

        workoutListener = db.collection("users").document(uid)
            .collection("workouts")
            .addSnapshotListener { snapshot, _ ->
                latestWorkoutDocs = snapshot?.documents
                    ?.filterIsInstance<com.google.firebase.firestore.QueryDocumentSnapshot>()
                render(latestFoodDocs, latestWorkoutDocs)
            }
    }

    private fun addHeader(text: String) {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 20f
        tv.setTextColor(Color.WHITE)
        tv.setPadding(0, 0, 0, 20)
        tv.setTypeface(null, android.graphics.Typeface.BOLD)
        container.addView(tv)
    }

    private fun addTitle(text: String) {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 18f
        tv.setTextColor(Color.WHITE)
        tv.setPadding(0, 20, 0, 10)
        tv.setTypeface(null, android.graphics.Typeface.BOLD)
        container.addView(tv)
    }

    private fun addText(text: String) {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 14f
        tv.setTextColor(Color.parseColor("#D9E2EC"))
        tv.setPadding(0, 10, 0, 10)
        container.addView(tv)
    }

    private fun addCard(text: String) {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 14f
        tv.setTextColor(Color.WHITE)
        tv.setBackgroundColor(Color.parseColor("#263544"))
        tv.setPadding(24, 20, 24, 20)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, 16)
        tv.layoutParams = params

        container.addView(tv)
    }

    private fun addSummaryBox(title: String, body: String) {
        val tv = TextView(this)
        tv.text = "$title\n$body"
        tv.textSize = 15f
        tv.setTextColor(Color.WHITE)
        tv.setBackgroundColor(Color.parseColor("#355070"))
        tv.setPadding(24, 20, 24, 20)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, 20)
        tv.layoutParams = params

        container.addView(tv)
    }

    override fun onDestroy() {
        super.onDestroy()
        foodListener?.remove()
        workoutListener?.remove()
    }
}