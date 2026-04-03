package com.example.carabuff

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class CustomizeActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var contentRoot: LinearLayout

    private lateinit var tvBMI: TextView
    private lateinit var spinnerGoal: Spinner
    private lateinit var etCalories: EditText
    private lateinit var etProtein: EditText
    private lateinit var etCarbs: EditText
    private lateinit var etFats: EditText
    private lateinit var spinnerWorkout: Spinner
    private lateinit var btnSave: Button

    private var bmi: Double = 0.0
    private var goal: String = ""
    private var calories: Int = 0
    private var protein: Int = 0
    private var carbs: Int = 0
    private var fats: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customize)
        overridePendingTransition(0, 0)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        contentRoot = findViewById(R.id.customizeContent)

        tvBMI = findViewById(R.id.tvBMI)
        spinnerGoal = findViewById(R.id.spinnerGoal)
        etCalories = findViewById(R.id.etCalories)
        etProtein = findViewById(R.id.etProtein)
        etCarbs = findViewById(R.id.etCarbs)
        etFats = findViewById(R.id.etFats)
        spinnerWorkout = findViewById(R.id.spinnerWorkout)
        btnSave = findViewById(R.id.btnSave)

        bmi = intent.getDoubleExtra("bmi", 0.0)
        goal = intent.getStringExtra("goal") ?: ""
        calories = intent.getIntExtra("calories", 0)
        protein = intent.getIntExtra("protein", 0)
        carbs = intent.getIntExtra("carbs", 0)
        fats = intent.getIntExtra("fats", 0)

        showContentEnterAnimation()
        bindInitialValues()
        setupGoalSpinner()
        setupWorkoutSpinner()

        btnSave.setOnClickListener {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedGoal = spinnerGoal.selectedItem?.toString()?.trim().orEmpty()
            val caloriesVal = etCalories.text.toString().trim().toIntOrNull()
            val proteinVal = etProtein.text.toString().trim().toIntOrNull()
            val carbsVal = etCarbs.text.toString().trim().toIntOrNull()
            val fatsVal = etFats.text.toString().trim().toIntOrNull()

            if (selectedGoal.isEmpty()) {
                Toast.makeText(this, "Please select a goal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (caloriesVal == null || caloriesVal <= 0) {
                etCalories.error = "Enter valid calories"
                etCalories.requestFocus()
                return@setOnClickListener
            }

            if (proteinVal == null || proteinVal < 0) {
                etProtein.error = "Enter valid protein"
                etProtein.requestFocus()
                return@setOnClickListener
            }

            if (carbsVal == null || carbsVal < 0) {
                etCarbs.error = "Enter valid carbs"
                etCarbs.requestFocus()
                return@setOnClickListener
            }

            if (fatsVal == null || fatsVal < 0) {
                etFats.error = "Enter valid fats"
                etFats.requestFocus()
                return@setOnClickListener
            }

            val workoutMinutes = when (spinnerWorkout.selectedItem?.toString().orEmpty()) {
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

            btnSave.isEnabled = false

            db.collection("users")
                .document(userId)
                .set(planMap, SetOptions.merge())
                .addOnSuccessListener {
                    sendWelcomeIfFirstTime(userId) {
                        Toast.makeText(this, "Custom Plan Saved 🔥", Toast.LENGTH_SHORT).show()

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
                    btnSave.isEnabled = true
                    Toast.makeText(this, "Failed to save custom plan", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun bindInitialValues() {
        tvBMI.text = "BMI: %.1f".format(bmi)
        etCalories.setText(calories.toString())
        etProtein.setText(protein.toString())
        etCarbs.setText(carbs.toString())
        etFats.setText(fats.toString())
    }

    private fun setupGoalSpinner() {
        val goals = arrayOf("Cut", "Maintain", "Bulk")

        val goalAdapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            goals
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

        goalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGoal.adapter = goalAdapter
        spinnerGoal.setSelection(goals.indexOf(goal).coerceAtLeast(0))
    }

    private fun setupWorkoutSpinner() {
        val workouts = arrayOf("30 mins", "1 hour", "2 hours", "3 hours", "4 hours")

        val workoutAdapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            workouts
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