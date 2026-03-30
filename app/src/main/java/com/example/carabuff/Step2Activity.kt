package com.example.carabuff

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class Step2Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_step2)

        val weightInput = findViewById<EditText>(R.id.weightInput)
        val heightInput = findViewById<EditText>(R.id.heightInput)

        val nextBtn = findViewById<Button>(R.id.nextBtn)
        val prevBtn = findViewById<Button>(R.id.prevBtn)

        // 🔥 ENABLE PREVIOUS BUTTON (IMPORTANT)
        prevBtn.isEnabled = true

        // 🔥 SAFE DATA FROM STEP 1 (no null crash)
        val name = intent.getStringExtra("name") ?: ""
        val age = intent.getStringExtra("age") ?: ""
        val gender = intent.getStringExtra("gender") ?: ""

        // 🔙 PREVIOUS BUTTON
        prevBtn.setOnClickListener {
            finish()
        }

        // 🔥 NEXT BUTTON
        nextBtn.setOnClickListener {

            val weight = weightInput.text.toString().trim()
            val height = heightInput.text.toString().trim()

            // ❌ VALIDATION
            if (weight.isEmpty() || height.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔥 DEBUG (optional)
            Toast.makeText(this, "Proceeding to Step 3", Toast.LENGTH_SHORT).show()

            try {
                val intent = Intent(this, Step3Activity::class.java).apply {
                    putExtra("name", name)
                    putExtra("age", age)
                    putExtra("gender", gender)
                    putExtra("weight", weight)
                    putExtra("height", height)
                }

                startActivity(intent)

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error opening Step 3", Toast.LENGTH_LONG).show()
            }
        }
    }
}

