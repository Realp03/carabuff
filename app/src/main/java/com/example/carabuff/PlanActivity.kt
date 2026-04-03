package com.example.carabuff

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class PlanActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var contentRoot: LinearLayout

    private lateinit var tvBMI: TextView
    private lateinit var tvGoal: TextView
    private lateinit var tvCalories: TextView
    private lateinit var tvProtein: TextView
    private lateinit var tvCarbs: TextView
    private lateinit var tvFats: TextView

    private lateinit var spinnerWorkout: Spinner
    private lateinit var btnStart: Button

    private var bmi: Double = 0.0
    private var goal: String = ""
    private var calories: Int = 0
    private var protein: Int = 0
    private var carbs: Int = 0
    private var fats: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan)
        overridePendingTransition(0, 0)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        contentRoot = findViewById(R.id.planContent)

        tvBMI = findViewById(R.id.tvPlanBMI)
        tvGoal = findViewById(R.id.tvPlanGoal)
        tvCalories = findViewById(R.id.tvPlanCalories)
        tvProtein = findViewById(R.id.tvPlanProtein)
        tvCarbs = findViewById(R.id.tvPlanCarbs)
        tvFats = findViewById(R.id.tvPlanFats)

        spinnerWorkout = findViewById(R.id.spinnerWorkout)
        btnStart = findViewById(R.id.btnStart)

        bmi = intent.getDoubleExtra("bmi", 0.0)
        goal = intent.getStringExtra("goal") ?: ""
        calories = intent.getIntExtra("calories", 0)
        protein = intent.getIntExtra("protein", 0)
        carbs = intent.getIntExtra("carbs", 0)
        fats = intent.getIntExtra("fats", 0)

        showContentEnterAnimation()
        bindPlanValues()
        setupWorkoutSpinner()

        btnStart.setOnClickListener {
            val workoutTime = spinnerWorkout.selectedItem?.toString()?.trim().orEmpty()
            val userId = auth.currentUser?.uid

            if (userId == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val workoutMinutes = when (workoutTime) {
                "30 mins" -> 30
                "1 hour" -> 60
                "2 hours" -> 120
                "3 hours" -> 180
                "4 hours" -> 240
                else -> 0
            }

            val planMap = hashMapOf(
                "plan" to hashMapOf(
                    "bmi" to bmi,
                    "goal" to goal,
                    "calories" to calories,
                    "protein" to protein,
                    "carbs" to carbs,
                    "fats" to fats,
                    "workoutMinutes" to workoutMinutes
                )
            )

            btnStart.isEnabled = false

            db.collection("users")
                .document(userId)
                .set(planMap, SetOptions.merge())
                .addOnSuccessListener {
                    sendWelcomeIfFirstTime(userId) {
                        Toast.makeText(this, "Plan Saved 🔥", Toast.LENGTH_SHORT).show()

                        animateContentExitLeft {
                            startActivity(Intent(this, HomeActivity::class.java).apply {
                                putExtra("from_plan", true)
                            })
                            overridePendingTransition(0, 0)
                            finish()
                        }
                    }
                }
                .addOnFailureListener {
                    btnStart.isEnabled = true
                    Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun bindPlanValues() {
        tvBMI.text = "BMI: %.1f".format(bmi)
        tvGoal.text = "Goal: $goal"
        tvCalories.text = "Calories: $calories"
        tvProtein.text = "Protein: ${protein}g"
        tvCarbs.text = "Carbs: ${carbs}g"
        tvFats.text = "Fats: ${fats}g"
    }

    private fun setupWorkoutSpinner() {
        val options = arrayOf("30 mins", "1 hour", "2 hours", "3 hours", "4 hours")

        val workoutAdapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            options
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.setTextColor(Color.parseColor("#111111"))
                textView.textSize = 15f
                textView.setPadding(12, 0, 12, 0)
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.setTextColor(Color.WHITE)
                textView.textSize = 15f
                textView.setBackgroundColor(Color.parseColor("#1C3557"))
                textView.setPadding(16, 16, 16, 16)
                return view
            }
        }

        workoutAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerWorkout.adapter = workoutAdapter
    }

    private fun sendWelcomeIfFirstTime(userId: String, onDone: () -> Unit) {
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->

                val alreadySent = document.getBoolean("welcomeNotifSent") ?: false

                if (alreadySent) {
                    onDone()
                    return@addOnSuccessListener
                }

                val rawName = document.getString("name")
                    ?: document.getString("fullName")
                    ?: document.getString("username")
                    ?: document.getString("displayName")
                    ?: "Carabuff Warrior"

                val firstName = rawName.trim()
                    .split(" ")
                    .firstOrNull()
                    ?.replaceFirstChar { it.uppercase() }
                    ?: "Carabuff Warrior"

                NotificationHelper.showNotification(
                    context = this,
                    title = "Welcome to Carabuff, $firstName! 🎉",
                    message = "Thanks for signing up! You’re officially in — let’s build your progress one meal, one workout, and one day at a time 💪",
                    type = "welcome",
                    target = "profile",
                    saveToDb = true
                )

                db.collection("users")
                    .document(userId)
                    .set(mapOf("welcomeNotifSent" to true), SetOptions.merge())
                    .addOnSuccessListener {
                        onDone()
                    }
                    .addOnFailureListener {
                        onDone()
                    }
            }
            .addOnFailureListener {
                onDone()
            }
    }

    private fun showContentEnterAnimation() {
        contentRoot.alpha = 0f
        contentRoot.translationX = 120f

        contentRoot.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(260)
            .start()
    }

    private fun animateContentExitLeft(onEnd: () -> Unit) {
        contentRoot.animate()
            .alpha(0f)
            .translationX(-120f)
            .setDuration(220)
            .withEndAction {
                onEnd()
            }
            .start()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        animateContentExitLeft {
            super.onBackPressed()
            overridePendingTransition(0, 0)
        }
    }
}