package com.example.carabuff

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

        // GET DATA
        val bmi = intent.getDoubleExtra("bmi", 0.0)
        val goal = intent.getStringExtra("goal") ?: ""
        val calories = intent.getIntExtra("calories", 0)
        val protein = intent.getIntExtra("protein", 0)
        val carbs = intent.getIntExtra("carbs", 0)
        val fats = intent.getIntExtra("fats", 0)

        tvBMI.text = "BMI: %.1f".format(bmi)

        // GOAL OPTIONS
        val goals = arrayOf("Cut", "Maintain", "Bulk")
        spinnerGoal.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, goals)

        spinnerGoal.setSelection(goals.indexOf(goal))

        // PREFILL
        etCalories.setText(calories.toString())
        etProtein.setText(protein.toString())
        etCarbs.setText(carbs.toString())
        etFats.setText(fats.toString())

        // WORKOUT OPTIONS
        val workouts = arrayOf("30 mins", "1 hour", "2 hours", "3 hours", "4 hours")
        spinnerWorkout.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, workouts)

        btnSave.setOnClickListener {

            val userId = auth.currentUser?.uid ?: return@setOnClickListener

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
                .set(planMap, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(this, "Custom Plan Saved 🔥", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }
        }
    }
}