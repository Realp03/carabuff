package com.example.carabuff

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Step3Activity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var contentRoot: LinearLayout
    private lateinit var lifestyleGroup: RadioGroup
    private lateinit var timeSpinner: Spinner
    private lateinit var activitySpinner: Spinner
    private lateinit var nextBtn: Button
    private lateinit var prevBtn: Button

    private var name: String = ""
    private var age: String = ""
    private var gender: String = ""
    private var birthday: String = ""
    private var weight: String = ""
    private var height: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_step3)
        overridePendingTransition(0, 0)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        contentRoot = findViewById(R.id.step3Content)
        lifestyleGroup = findViewById(R.id.lifestyleGroup)
        timeSpinner = findViewById(R.id.timeSpinner)
        activitySpinner = findViewById(R.id.activitySpinner)
        nextBtn = findViewById(R.id.nextBtn)
        prevBtn = findViewById(R.id.prevBtn)

        name = intent.getStringExtra("name") ?: ""
        age = intent.getStringExtra("age") ?: ""
        gender = intent.getStringExtra("gender") ?: ""
        birthday = intent.getStringExtra("birthday") ?: ""
        weight = intent.getStringExtra("weight") ?: ""
        height = intent.getStringExtra("height") ?: ""

        setupSpinners()
        showContentEnterAnimation()

        prevBtn.setOnClickListener {
            animateContentExitRight {
                val intent = Intent(this, Step2Activity::class.java).apply {
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
            }
        }

        nextBtn.setOnClickListener {
            val selectedId = lifestyleGroup.checkedRadioButtonId

            if (selectedId == -1) {
                Toast.makeText(this, "Please answer the lifestyle question", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val lifestyle = findViewById<RadioButton>(selectedId).text.toString()
            val time = timeSpinner.selectedItem?.toString()?.trim().orEmpty()
            val activity = activitySpinner.selectedItem?.toString()?.trim().orEmpty()

            if (time.isEmpty()) {
                Toast.makeText(this, "Please select your available time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (activity.isEmpty()) {
                Toast.makeText(this, "Please select your activity level", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = auth.currentUser
            if (user == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val userId = user.uid
            val weightDouble = weight.toDoubleOrNull() ?: 0.0
            val heightDouble = height.toDoubleOrNull() ?: 0.0

            val userMap = hashMapOf(
                "name" to name,
                "age" to age,
                "gender" to gender,
                "birthday" to birthday,
                "weight" to weightDouble,
                "height" to heightDouble,
                "lifestyle" to lifestyle,
                "timeAvailable" to time,
                "activityLevel" to activity,
                "isProfileComplete" to true
            )

            animateContentExitLeft {
                db.collection("users")
                    .document(userId)
                    .set(userMap)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Profile Complete!", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this, ResultActivity::class.java).apply {
                            putExtra("weight", weightDouble)
                            putExtra("height", heightDouble)
                        }
                        startActivity(intent)
                        overridePendingTransition(0, 0)
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error saving profile", Toast.LENGTH_LONG).show()
                        showContentEnterAnimation()
                    }
            }
        }
    }

    private fun setupSpinners() {
        val timeOptions = arrayOf("30 mins", "1 hour", "2 hours", "3+ hours")
        val activityOptions = arrayOf("Sedentary", "Light", "Moderate", "Active")

        val timeAdapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            timeOptions
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
                textView.setTextColor(Color.parseColor("#FFFFFF"))
                textView.textSize = 15f
                textView.setBackgroundColor(Color.parseColor("#1C3557"))
                textView.setPadding(16, 16, 16, 16)
                return view
            }
        }
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timeSpinner.adapter = timeAdapter

        val activityAdapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            activityOptions
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
                textView.setTextColor(Color.parseColor("#FFFFFF"))
                textView.textSize = 15f
                textView.setBackgroundColor(Color.parseColor("#1C3557",))
                textView.setPadding(16, 16, 16, 16)
                return view
            }
        }
        activityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        activitySpinner.adapter = activityAdapter
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

            val intent = Intent(this, Step2Activity::class.java).apply {
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
        }
    }
}