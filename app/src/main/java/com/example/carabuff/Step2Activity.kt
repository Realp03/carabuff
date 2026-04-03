package com.example.carabuff

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Step2Activity : AppCompatActivity() {

    private lateinit var contentRoot: LinearLayout
    private lateinit var weightInput: EditText
    private lateinit var heightInput: EditText
    private lateinit var prevBtn: Button
    private lateinit var nextBtn: Button

    private var name: String = ""
    private var age: String = ""
    private var gender: String = ""
    private var birthday: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_step2)
        overridePendingTransition(0, 0)

        contentRoot = findViewById(R.id.step2Content)
        weightInput = findViewById(R.id.weightInput)
        heightInput = findViewById(R.id.heightInput)
        prevBtn = findViewById(R.id.prevBtn)
        nextBtn = findViewById(R.id.nextBtn)

        name = intent.getStringExtra("name") ?: ""
        age = intent.getStringExtra("age") ?: ""
        gender = intent.getStringExtra("gender") ?: ""
        birthday = intent.getStringExtra("birthday") ?: ""

        showContentEnterAnimation()

        prevBtn.setOnClickListener {
            animateContentExitRight {
                val intent = Intent(this, SetupProfileActivity::class.java).apply {
                    putExtra("name", name)
                    putExtra("age", age)
                    putExtra("gender", gender)
                    putExtra("birthday", birthday)
                }
                startActivity(intent)
                overridePendingTransition(0, 0)
                finish()
            }
        }

        nextBtn.setOnClickListener {
            val weight = weightInput.text.toString().trim()
            val height = heightInput.text.toString().trim()

            if (weight.isEmpty()) {
                weightInput.error = "Please enter your weight"
                weightInput.requestFocus()
                return@setOnClickListener
            }

            if (height.isEmpty()) {
                heightInput.error = "Please enter your height"
                heightInput.requestFocus()
                return@setOnClickListener
            }

            val weightValue = weight.toDoubleOrNull()
            val heightValue = height.toDoubleOrNull()

            if (weightValue == null || weightValue <= 0) {
                weightInput.error = "Enter a valid weight"
                weightInput.requestFocus()
                return@setOnClickListener
            }

            if (heightValue == null || heightValue <= 0) {
                heightInput.error = "Enter a valid height"
                heightInput.requestFocus()
                return@setOnClickListener
            }

            if (weightValue < 20 || weightValue > 300) {
                weightInput.error = "Weight must be between 20 and 300 kg"
                weightInput.requestFocus()
                return@setOnClickListener
            }

            if (heightValue < 100 || heightValue > 250) {
                heightInput.error = "Height must be between 100 and 250 cm"
                heightInput.requestFocus()
                return@setOnClickListener
            }

            animateContentExitLeft {
                try {
                    val intent = Intent(this, Step3Activity::class.java).apply {
                        putExtra("name", name)
                        putExtra("age", age)
                        putExtra("gender", gender)
                        putExtra("birthday", birthday)
                        putExtra("weight", weight)
                        putExtra("height", height)
                    }
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error opening Step 3", Toast.LENGTH_LONG).show()
                }
            }
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

    private fun animateContentExitRight(onEnd: () -> Unit) {
        contentRoot.animate()
            .alpha(0f)
            .translationX(120f)
            .setDuration(220)
            .withEndAction {
                onEnd()
            }
            .start()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        animateContentExitRight {
            val intent = Intent(this, SetupProfileActivity::class.java).apply {
                putExtra("name", name)
                putExtra("age", age)
                putExtra("gender", gender)
                putExtra("birthday", birthday)
            }
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }
    }
}