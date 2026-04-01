package com.example.carabuff

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class PlanActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val tvBMI = findViewById<TextView>(R.id.tvPlanBMI)
        val tvGoal = findViewById<TextView>(R.id.tvPlanGoal)
        val tvCalories = findViewById<TextView>(R.id.tvPlanCalories)
        val tvProtein = findViewById<TextView>(R.id.tvPlanProtein)
        val tvCarbs = findViewById<TextView>(R.id.tvPlanCarbs)
        val tvFats = findViewById<TextView>(R.id.tvPlanFats)

        val spinnerWorkout = findViewById<Spinner>(R.id.spinnerWorkout)
        val btnStart = findViewById<Button>(R.id.btnStart)

        val bmi = intent.getDoubleExtra("bmi", 0.0)
        val goal = intent.getStringExtra("goal") ?: ""

        val calories = intent.getIntExtra("calories", 0)
        val protein = intent.getIntExtra("protein", 0)
        val carbs = intent.getIntExtra("carbs", 0)
        val fats = intent.getIntExtra("fats", 0)

        tvBMI.text = "BMI: %.1f".format(bmi)
        tvGoal.text = "Goal: $goal"
        tvCalories.text = "Calories: $calories"
        tvProtein.text = "Protein: ${protein}g"
        tvCarbs.text = "Carbs: ${carbs}g"
        tvFats.text = "Fats: ${fats}g"

        val options = arrayOf("30 mins", "1 hour", "2 hours", "3 hours", "4 hours")
        spinnerWorkout.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)

        btnStart.setOnClickListener {

            val workoutTime = spinnerWorkout.selectedItem.toString()
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
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    }
                }
                .addOnFailureListener {
                    btnStart.isEnabled = true
                    Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show()
                }
        }
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
}