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

        // UI
        val tvBMI = findViewById<TextView>(R.id.tvPlanBMI)
        val tvGoal = findViewById<TextView>(R.id.tvPlanGoal)
        val tvCalories = findViewById<TextView>(R.id.tvPlanCalories)
        val tvProtein = findViewById<TextView>(R.id.tvPlanProtein)
        val tvCarbs = findViewById<TextView>(R.id.tvPlanCarbs)
        val tvFats = findViewById<TextView>(R.id.tvPlanFats)

        val spinnerWorkout = findViewById<Spinner>(R.id.spinnerWorkout)
        val btnStart = findViewById<Button>(R.id.btnStart)

        // 🔥 GET DATA FROM RESULT
        val bmi = intent.getDoubleExtra("bmi", 0.0)
        val goal = intent.getStringExtra("goal") ?: ""

        val calories = intent.getIntExtra("calories", 0)
        val protein = intent.getIntExtra("protein", 0)
        val carbs = intent.getIntExtra("carbs", 0)
        val fats = intent.getIntExtra("fats", 0)

        // DISPLAY
        tvBMI.text = "BMI: %.1f".format(bmi)
        tvGoal.text = "Goal: $goal"

        tvCalories.text = "Calories: $calories"
        tvProtein.text = "Protein: ${protein}g"
        tvCarbs.text = "Carbs: ${carbs}g"
        tvFats.text = "Fats: ${fats}g"

        // WORKOUT OPTIONS
        val options = arrayOf("30 mins", "1 hour", "2 hours", "3 hours", "4 hours")

        spinnerWorkout.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)

        // 🚀 SAVE PLAN
        btnStart.setOnClickListener {

            val workoutTime = spinnerWorkout.selectedItem.toString()
            val userId = auth.currentUser?.uid

            if (userId == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔥 CONVERT TO MINUTES (FIX)
            val workoutMinutes = when (workoutTime) {
                "30 mins" -> 30
                "1 hour" -> 60
                "2 hours" -> 120
                "3 hours" -> 180
                "4 hours" -> 240
                else -> 0
            }

            // 🔥 FIREBASE DATA STRUCTURE
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
                .set(planMap, SetOptions.merge()) // 🔥 SAFE SAVE
                .addOnSuccessListener {

                    Toast.makeText(this, "Plan Saved 🔥", Toast.LENGTH_SHORT).show()

                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }
                .addOnFailureListener {
                    btnStart.isEnabled = true
                    Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show()
                }
        }
    }
}