package com.example.carabuff

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Step3Activity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_step3)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val lifestyleGroup = findViewById<RadioGroup>(R.id.lifestyleGroup)
        val timeSpinner = findViewById<Spinner>(R.id.timeSpinner)
        val activitySpinner = findViewById<Spinner>(R.id.activitySpinner)

        val nextBtn = findViewById<Button>(R.id.nextBtn)
        val prevBtn = findViewById<Button>(R.id.prevBtn)

        // 🔥 GET DATA FROM STEP 1 & 2
        val name = intent.getStringExtra("name") ?: ""
        val age = intent.getStringExtra("age") ?: ""
        val gender = intent.getStringExtra("gender") ?: ""
        val weight = intent.getStringExtra("weight") ?: ""
        val height = intent.getStringExtra("height") ?: ""

        // 🔥 SPINNER OPTIONS
        val timeOptions = arrayOf("30 mins", "1 hour", "2 hours", "3+ hours")
        val activityOptions = arrayOf("Sedentary", "Light", "Moderate", "Active")

        timeSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, timeOptions)

        activitySpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, activityOptions)

        // 🔙 PREVIOUS BUTTON
        prevBtn.setOnClickListener {
            finish()
        }

        // 🔥 FINISH BUTTON
        nextBtn.setOnClickListener {

            val selectedId = lifestyleGroup.checkedRadioButtonId

            if (selectedId == -1) {
                Toast.makeText(this, "Answer all questions", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val lifestyle = findViewById<RadioButton>(selectedId).text.toString()
            val time = timeSpinner.selectedItem.toString()
            val activity = activitySpinner.selectedItem.toString()

            val user = auth.currentUser

            if (user == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val userId = user.uid

            // 🔥 SAFE CONVERSION (NO CRASH)
            val weightDouble = weight.toDoubleOrNull() ?: 0.0
            val heightDouble = height.toDoubleOrNull() ?: 0.0

            // 🔥 FINAL USER DATA
            val userMap = hashMapOf(
                "name" to name,
                "age" to age,
                "gender" to gender,
                "weight" to weightDouble,
                "height" to heightDouble,
                "lifestyle" to lifestyle,
                "timeAvailable" to time,
                "activityLevel" to activity,
                "isProfileComplete" to true
            )

            // 🔥 SAVE TO FIRESTORE
            db.collection("users")
                .document(userId)
                .set(userMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile Complete!", Toast.LENGTH_SHORT).show()

                    // 🚀 GO TO RESULT SCREEN (AI)
                    val intent = Intent(this, ResultActivity::class.java)
                    intent.putExtra("weight", weightDouble)
                    intent.putExtra("height", heightDouble)

                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error saving profile", Toast.LENGTH_LONG).show()
                }
        }
    }
}