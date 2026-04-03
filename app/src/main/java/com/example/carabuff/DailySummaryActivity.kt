package com.example.carabuff

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DailySummaryActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout
    private lateinit var btnBack: ImageView

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private var foodListener: ListenerRegistration? = null
    private var workoutListener: ListenerRegistration? = null

    private var selectedDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_summary)

        container = findViewById(R.id.containerSummary)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }

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

        fun render(
            foodDocs: List<com.google.firebase.firestore.QueryDocumentSnapshot>?,
            workoutDocs: List<com.google.firebase.firestore.QueryDocumentSnapshot>?
        ) {
            container.removeAllViews()

            addHeaderCard("📅 Summary for $displayDate")

            addSectionTitle("🍔 Food Logs")

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

                addInfoCard(
                    title = name,
                    subtitle = time,
                    details = "🔥 $calories kcal   •   P:${protein.toInt()}  C:${carbs.toInt()}  F:${fats.toInt()}",
                    backgroundColor = "#24364A"
                )
            }

            if (!hasFood) {
                addEmptyState("No food logs for this day 🍔")
            } else {
                addSummaryCard(
                    title = "Food Total",
                    body = "🔥 $totalFoodCalories kcal\nP:${totalProtein.toInt()}  •  C:${totalCarbs.toInt()}  •  F:${totalFats.toInt()}",
                    backgroundColor = "#355C8C"
                )
            }

            addSectionTitle("💪 Workouts")

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

                addInfoCard(
                    title = name,
                    subtitle = time,
                    details = "⏱ $minutes mins   •   🔥 $calories kcal",
                    backgroundColor = "#24364A"
                )
            }

            if (!hasWorkout) {
                addEmptyState("No workouts for this day 💪")
            } else {
                addSummaryCard(
                    title = "Workout Total",
                    body = "⏱ $totalWorkoutMinutes mins\n🔥 $totalBurnedCalories kcal burned",
                    backgroundColor = "#355C8C"
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

    private fun addHeaderCard(text: String) {
        val tv = TextView(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedDrawable("#1E2E42", 16f)
            typeface = getIcelandTypeface() ?: Typeface.DEFAULT_BOLD
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.bottomMargin = dp(18)
        tv.layoutParams = params

        container.addView(tv)
    }

    private fun addSectionTitle(text: String) {
        val tv = TextView(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setPadding(0, dp(6), 0, dp(10))
            typeface = getIcelandTypeface() ?: Typeface.DEFAULT_BOLD
        }
        container.addView(tv)
    }

    private fun addEmptyState(text: String) {
        val tv = TextView(this).apply {
            this.text = text
            setTextColor(Color.parseColor("#D9E2EC"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(dp(4), dp(4), dp(4), dp(14))
            typeface = getIcelandTypeface() ?: Typeface.DEFAULT
            alpha = 0.9f
        }
        container.addView(tv)
    }

    private fun addInfoCard(
        title: String,
        subtitle: String,
        details: String,
        backgroundColor: String
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedDrawable(backgroundColor, 16f)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.bottomMargin = dp(12)
        card.layoutParams = params

        val tvTitle = TextView(this).apply {
            text = title
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = getIcelandTypeface() ?: Typeface.DEFAULT_BOLD
        }

        val tvSubtitle = TextView(this).apply {
            text = subtitle
            setTextColor(Color.parseColor("#C7D6E8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(3), 0, dp(8))
            typeface = getIcelandTypeface() ?: Typeface.DEFAULT
        }

        val tvDetails = TextView(this).apply {
            text = details
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = getIcelandTypeface() ?: Typeface.DEFAULT
        }

        card.addView(tvTitle)
        card.addView(tvSubtitle)
        card.addView(tvDetails)

        container.addView(card)
    }

    private fun addSummaryCard(
        title: String,
        body: String,
        backgroundColor: String
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedDrawable(backgroundColor, 18f)
            setPadding(dp(18), dp(16), dp(18), dp(16))
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.bottomMargin = dp(18)
        card.layoutParams = params

        val tvTitle = TextView(this).apply {
            text = title
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = getIcelandTypeface() ?: Typeface.DEFAULT_BOLD
        }

        val tvBody = TextView(this).apply {
            text = body
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(0, dp(8), 0, 0)
            typeface = getIcelandTypeface() ?: Typeface.DEFAULT
        }

        card.addView(tvTitle)
        card.addView(tvBody)

        container.addView(card)
    }

    private fun roundedDrawable(colorHex: String, radiusDp: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp.toInt()).toFloat()
            setColor(Color.parseColor(colorHex))
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun getIcelandTypeface(): Typeface? {
        return ResourcesCompat.getFont(this, R.font.iceland_regular)
    }

    override fun onDestroy() {
        super.onDestroy()
        foodListener?.remove()
        workoutListener?.remove()
    }
}