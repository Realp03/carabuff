package com.example.carabuff

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class DailySummaryActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_summary)

        container = findViewById(R.id.containerSummary)

        val selectedDate = intent.getStringExtra("date") ?: return

        loadDailyData(selectedDate)
    }

    private fun loadDailyData(date: String) {

        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // 🔥 SAFE DATE PARSE (NO BUG)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val startCal = Calendar.getInstance()
        startCal.time = sdf.parse(date)!!

        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)

        val endCal = startCal.clone() as Calendar
        endCal.add(Calendar.DAY_OF_MONTH, 1)

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        // =========================
        // 🍔 FOOD LOGS
        // =========================
        db.collection("users").document(userId)
            .collection("foodLogs")
            .get()
            .addOnSuccessListener { result ->

                addTitle("🍔 Food Logs")

                var hasFood = false

                for (doc in result) {

                    val timestamp = doc["timestamp"] as? Long ?: continue

                    // 🔥 RANGE CHECK (FINAL FIX)
                    if (timestamp < startCal.timeInMillis || timestamp >= endCal.timeInMillis) continue

                    hasFood = true

                    val name = doc["name"] as? String ?: "Food"

                    val calories = (doc["calories"] as? Long)?.toInt() ?: 0
                    val protein = doc["protein"] as? Double ?: 0.0
                    val carbs = doc["carbs"] as? Double ?: 0.0
                    val fats = doc["fats"] as? Double ?: 0.0

                    val time = timeFormat.format(Date(timestamp))

                    addText(
                        "$name\n$time\n🔥 $calories kcal | P:$protein C:$carbs F:$fats\n"
                    )
                }

                if (!hasFood) {
                    addText("No food logs for this day 🍔")
                }
            }

        // =========================
        // 💪 WORKOUTS
        // =========================
        db.collection("users").document(userId)
            .collection("workouts")
            .get()
            .addOnSuccessListener { result ->

                addTitle("💪 Workouts")

                var hasWorkout = false

                for (doc in result) {

                    val timestamp = doc["timestamp"] as? Long ?: continue

                    // 🔥 RANGE CHECK (FINAL FIX)
                    if (timestamp < startCal.timeInMillis || timestamp >= endCal.timeInMillis) continue

                    hasWorkout = true

                    val name = doc["name"] as? String ?: "Workout"

                    val minutes = (doc["minutes"] as? Long)?.toInt() ?: 0
                    val calories = (doc["caloriesBurned"] as? Long)?.toInt() ?: 0

                    val time = timeFormat.format(Date(timestamp))

                    addText(
                        "$name\n$time\n⏱ $minutes mins | 🔥 $calories kcal\n"
                    )
                }

                if (!hasWorkout) {
                    addText("No workouts for this day 💪")
                }
            }
    }

    private fun addTitle(text: String) {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 18f
        tv.setPadding(0, 20, 0, 10)
        container.addView(tv)
    }

    private fun addText(text: String) {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 14f
        tv.setPadding(0, 10, 0, 10)
        container.addView(tv)
    }
}