package com.example.carabuff

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class ResultActivity : AppCompatActivity() {

    private lateinit var tvBMI: TextView
    private lateinit var tvGoal: TextView
    private lateinit var tvCalories: TextView
    private lateinit var tvProtein: TextView
    private lateinit var tvCarbs: TextView
    private lateinit var tvFats: TextView
    private lateinit var tvThought: TextView

    private lateinit var btnFollow: Button
    private lateinit var btnCustom: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        tvBMI = findViewById(R.id.tvBMI)
        tvGoal = findViewById(R.id.tvGoal)
        tvCalories = findViewById(R.id.tvCalories)
        tvProtein = findViewById(R.id.tvProtein)
        tvCarbs = findViewById(R.id.tvCarbs)
        tvFats = findViewById(R.id.tvFats)
        tvThought = findViewById(R.id.carabuffThought)

        btnFollow = findViewById(R.id.btnFollow)
        btnCustom = findViewById(R.id.btnCustom)

        // GET DATA FROM PREVIOUS SCREEN
        val weight = intent.getDoubleExtra("weight", 0.0)
        val heightCm = intent.getDoubleExtra("height", 0.0)

        val heightM = heightCm / 100

        // CALCULATE BMI
        val bmi = weight / (heightM * heightM)
        val bmiFormatted = String.format("%.1f", bmi)

        val category = getBMICategory(bmi)
        val goal = getGoal(category)

        val calories = calculateCalories(weight, goal)
        val macros = calculateMacros(weight, calories)

        // DELAY (AI EFFECT)
        Handler(Looper.getMainLooper()).postDelayed({
            tvBMI.text = "BMI: $bmiFormatted"
            tvGoal.text = "Goal: $goal"

            tvCalories.text = "Calories: $calories"
            tvProtein.text = "Protein: ${macros.protein}g"
            tvCarbs.text = "Carbs: ${macros.carbs}g"
            tvFats.text = "Fats: ${macros.fats}g"

            typeText(generateThought(goal))

        }, 1200)

        // 🔥 UPDATED BUTTON ACTION
        btnFollow.setOnClickListener {

            val intent = Intent(this, PlanActivity::class.java)

            // PASS DATA
            intent.putExtra("bmi", bmi)
            intent.putExtra("goal", goal)

            intent.putExtra("calories", calories)
            intent.putExtra("protein", macros.protein)
            intent.putExtra("carbs", macros.carbs)
            intent.putExtra("fats", macros.fats)

            startActivity(intent)
        }

        btnCustom.setOnClickListener {

            val intent = Intent(this, CustomizeActivity::class.java)

            // 🔥 PASS SAME DATA AS PLAN
            intent.putExtra("bmi", bmi)
            intent.putExtra("goal", goal)

            intent.putExtra("calories", calories)
            intent.putExtra("protein", macros.protein)
            intent.putExtra("carbs", macros.carbs)
            intent.putExtra("fats", macros.fats)

            startActivity(intent)
        }
    }

    // ================= LOGIC =================

    private fun getBMICategory(bmi: Double): String {
        return when {
            bmi < 18.5 -> "Underweight"
            bmi < 25 -> "Normal"
            bmi < 30 -> "Overweight"
            else -> "Obese"
        }
    }

    private fun getGoal(category: String): String {
        return when (category) {
            "Underweight" -> "Bulk"
            "Normal" -> "Maintain"
            else -> "Cut"
        }
    }

    private fun calculateCalories(weight: Double, goal: String): Int {
        val base = weight * 33
        return when (goal) {
            "Bulk" -> (base + 300).toInt()
            "Cut" -> (base - 300).toInt()
            else -> base.toInt()
        }
    }

    data class Macros(val protein: Int, val carbs: Int, val fats: Int)

    private fun calculateMacros(weight: Double, calories: Int): Macros {
        val protein = (weight * 2).toInt()
        val fats = (calories * 0.25 / 9).toInt()
        val carbs = ((calories - (protein * 4 + fats * 9)) / 4).toInt()
        return Macros(protein, carbs, fats)
    }

    private fun generateThought(goal: String): String {
        return when (goal) {
            "Maintain" -> "Ohh your body mass is good 😄 I recommend maintaining it. But if you want muscles, we can do a clean bulk 💪"
            "Bulk" -> "You're a bit underweight 👀 Let's build muscle and strength 🔥"
            "Cut" -> "We need to trim some fat 🔥 A calorie deficit will help!"
            else -> "Let's improve your fitness step by step 💪"
        }
    }

    // TYPEWRITER EFFECT
    private fun typeText(text: String) {
        tvThought.text = ""
        val handler = Handler(Looper.getMainLooper())

        for (i in text.indices) {
            handler.postDelayed({
                tvThought.text = text.substring(0, i + 1)
            }, (i * 30).toLong())
        }
    }
}