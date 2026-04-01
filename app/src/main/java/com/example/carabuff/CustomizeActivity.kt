package com.example.carabuff

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class CustomizeActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customize)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val tvBMI = findViewById<TextView>(R.id.tvBMI)
        val spinnerGoal = findViewById<Spinner>(R.id.spinnerGoal)
        val etCalories = findViewById<EditText>(R.id.etCalories)
        val etProtein = findViewById<EditText>(R.id.etProtein)
        val etCarbs = findViewById<EditText>(R.id.etCarbs)
        val etFats = findViewById<EditText>(R.id.etFats)
        val spinnerWorkout = findViewById<Spinner>(R.id.spinnerWorkout)
        val btnSave = findViewById<Button>(R.id.btnSave)

        val bmi = intent.getDoubleExtra("bmi", 0.0)
        val goal = intent.getStringExtra("goal") ?: ""
        val calories = intent.getIntExtra("calories", 0)
        val protein = intent.getIntExtra("protein", 0)
        val carbs = intent.getIntExtra("carbs", 0)
        val fats = intent.getIntExtra("fats", 0)

        tvBMI.text = "BMI: %.1f".format(bmi)

        val goals = arrayOf("Cut", "Maintain", "Bulk")
        spinnerGoal.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, goals)

        spinnerGoal.setSelection(goals.indexOf(goal).coerceAtLeast(0))

        etCalories.setText(calories.toString())
        etProtein.setText(protein.toString())
        etCarbs.setText(carbs.toString())
        etFats.setText(fats.toString())

        val workouts = arrayOf("30 mins", "1 hour", "2 hours", "3 hours", "4 hours")
        spinnerWorkout.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, workouts)

        btnSave.setOnClickListener {

            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSave.isEnabled = false

            val selectedGoal = spinnerGoal.selectedItem.toString()
            val caloriesVal = etCalories.text.toString().toIntOrNull() ?: 0
            val proteinVal = etProtein.text.toString().toIntOrNull() ?: 0
            val carbsVal = etCarbs.text.toString().toIntOrNull() ?: 0
            val fatsVal = etFats.text.toString().toIntOrNull() ?: 0

            val workoutMinutes = when (spinnerWorkout.selectedItem.toString()) {
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
                    "goal" to selectedGoal,
                    "calories" to caloriesVal,
                    "protein" to proteinVal,
                    "carbs" to carbsVal,
                    "fats" to fatsVal,
                    "workoutMinutes" to workoutMinutes
                )
            )

            db.collection("users")
                .document(userId)
                .set(planMap, SetOptions.merge())
                .addOnSuccessListener {
                    sendWelcomeIfFirstTime(userId) {
                        Toast.makeText(this, "Custom Plan Saved 🔥", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    }
                }
                .addOnFailureListener {
                    btnSave.isEnabled = true
                    Toast.makeText(this, "Failed to save custom plan", Toast.LENGTH_SHORT).show()
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
                    message = "Thanks for signing up! Your fitness journey starts now — stay consistent and make every workout count 💪",
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