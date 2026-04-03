package com.example.carabuff

import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ResultActivity : AppCompatActivity() {

    private lateinit var contentRoot: LinearLayout
    private lateinit var carabuffSprite: ImageView

    private lateinit var tvBMI: TextView
    private lateinit var tvGoal: TextView
    private lateinit var tvCalories: TextView
    private lateinit var tvProtein: TextView
    private lateinit var tvCarbs: TextView
    private lateinit var tvFats: TextView
    private lateinit var tvThought: TextView

    private lateinit var btnFollow: Button
    private lateinit var btnCustom: Button

    private var carabuffAnimation: AnimationDrawable? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        overridePendingTransition(0, 0)

        contentRoot = findViewById(R.id.resultContent)
        carabuffSprite = findViewById(R.id.carabuffSprite)

        tvBMI = findViewById(R.id.tvBMI)
        tvGoal = findViewById(R.id.tvGoal)
        tvCalories = findViewById(R.id.tvCalories)
        tvProtein = findViewById(R.id.tvProtein)
        tvCarbs = findViewById(R.id.tvCarbs)
        tvFats = findViewById(R.id.tvFats)
        tvThought = findViewById(R.id.carabuffThought)

        btnFollow = findViewById(R.id.btnFollow)
        btnCustom = findViewById(R.id.btnCustom)

        showContentEnterAnimation()
        startCarabuffAnimationOnce()

        val weight = intent.getDoubleExtra("weight", 0.0)
        val heightCm = intent.getDoubleExtra("height", 0.0)

        if (weight <= 0.0 || heightCm <= 0.0) {
            Toast.makeText(this, "Invalid result data received", Toast.LENGTH_SHORT).show()
            tvBMI.text = "BMI: --"
            tvGoal.text = "Goal: --"
            tvCalories.text = "Calories: --"
            tvProtein.text = "Protein: --"
            tvCarbs.text = "Carbs: --"
            tvFats.text = "Fats: --"
            tvThought.text = "I couldn't calculate your result properly. Please try again."
            return
        }

        val heightM = heightCm / 100.0
        val bmi = weight / (heightM * heightM)
        val bmiFormatted = String.format("%.1f", bmi)

        val category = getBMICategory(bmi)
        val goal = getGoal(category)
        val calories = calculateCalories(weight, goal)
        val macros = calculateMacros(weight, calories)

        tvBMI.text = "BMI: ..."
        tvGoal.text = "Goal: ..."
        tvCalories.text = "Calories: ..."
        tvProtein.text = "Protein: ..."
        tvCarbs.text = "Carbs: ..."
        tvFats.text = "Fats: ..."
        tvThought.text = "..."

        mainHandler.postDelayed({
            tvBMI.text = "BMI: $bmiFormatted"
            tvGoal.text = "Goal: $goal"
            tvCalories.text = "Calories: $calories"
            tvProtein.text = "Protein: ${macros.protein}g"
            tvCarbs.text = "Carbs: ${macros.carbs}g"
            tvFats.text = "Fats: ${macros.fats}g"

            typeText(generateThought(goal))
        }, 1000)

        btnFollow.setOnClickListener {
            animateContentExitLeft {
                val intent = Intent(this, PlanActivity::class.java).apply {
                    putExtra("bmi", bmi)
                    putExtra("goal", goal)
                    putExtra("calories", calories)
                    putExtra("protein", macros.protein)
                    putExtra("carbs", macros.carbs)
                    putExtra("fats", macros.fats)
                }
                startActivity(intent)
                overridePendingTransition(0, 0)
                finish()
            }
        }

        btnCustom.setOnClickListener {
            animateContentExitLeft {
                val intent = Intent(this, CustomizeActivity::class.java).apply {
                    putExtra("bmi", bmi)
                    putExtra("goal", goal)
                    putExtra("calories", calories)
                    putExtra("protein", macros.protein)
                    putExtra("carbs", macros.carbs)
                    putExtra("fats", macros.fats)
                }
                startActivity(intent)
                overridePendingTransition(0, 0)
                finish()
            }
        }
    }

    private fun startCarabuffAnimationOnce() {
        carabuffSprite.setImageResource(R.drawable.carabuff_talk_anim)

        carabuffSprite.post {
            carabuffAnimation = carabuffSprite.drawable as? AnimationDrawable
            carabuffAnimation?.start()

            val animation = carabuffAnimation
            if (animation != null) {
                var totalDuration = 0
                for (i in 0 until animation.numberOfFrames) {
                    totalDuration += animation.getDuration(i)
                }

                mainHandler.postDelayed({
                    carabuffAnimation?.stop()
                    carabuffSprite.setImageResource(R.drawable.carabuff1)
                }, totalDuration.toLong())
            } else {
                carabuffSprite.setImageResource(R.drawable.carabuff1)
            }
        }
    }

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

    private fun typeText(text: String) {
        tvThought.text = ""
        for (i in text.indices) {
            mainHandler.postDelayed({
                tvThought.text = text.substring(0, i + 1)
            }, (i * 28).toLong())
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

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacksAndMessages(null)
        carabuffAnimation?.stop()
    }
}