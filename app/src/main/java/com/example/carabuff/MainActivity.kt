package com.example.carabuff

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AnimationDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private var isPasswordVisible = false
    private lateinit var auth: FirebaseAuth

    private lateinit var carabuff: ImageView
    private lateinit var thoughtText: TextView

    private var isThinking = false
    private val handler = Handler(Looper.getMainLooper())

    private val errorMessages = listOf(
        "Oops, try again 😅",
        "Hmm… that didn’t work 🤔",
        "Wrong credentials! You got this 💪",
        "Try to remember your password 🧠",
        "Almost there! Check again 🔍",
        "Carabuff believes in you 🐃🔥",
        "Oops! Something’s not right 🚫",
        "Take your time, no rush ⏳"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 🔔 FORCE NOTIFICATION PERMISSION (ANDROID 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            Toast.makeText(this, "Requesting notification permission...", Toast.LENGTH_SHORT).show()

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1
            )
        }

        auth = FirebaseAuth.getInstance()

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val signInBtn = findViewById<Button>(R.id.signInButton)
        val signupText = findViewById<TextView>(R.id.signupText)
        val forgotPassword = findViewById<TextView>(R.id.forgotPasswordText)

        carabuff = findViewById(R.id.carabuffSprite)
        thoughtText = findViewById(R.id.carabuffThought)

        carabuff.setImageResource(R.drawable.carabuff1)

        val triviaList = listOf(
            "Eggs are one of the best protein sources 🥚",
            "Walking burns fat more consistently 🚶",
            "Muscle burns more calories than fat 💪",
            "Drink water to boost metabolism 💧",
            "Rest days help muscles grow 😴",
            "Protein helps recovery 🍗",
            "Consistency beats intensity 🔥",
            "No gym? Bodyweight works 🏃",
            "Sleep affects your progress 🛌",
            "Small progress is still progress 👊"
        )

        // DEFAULT TEXT
        typeText("...", true)

        // INTRO
        playTalkAnimation()
        typeText("Hi! I'm Carabuff Your fitness AI companion 💪")

        // CLICK CARABUFF
        carabuff.setOnClickListener {
            if (isThinking) return@setOnClickListener
            val randomMessage = triviaList.random()
            playTalkAnimation()
            typeText(randomMessage)
        }

        // PASSWORD TOGGLE
        passwordInput.setOnTouchListener { v, event ->
            val drawableEnd = 2

            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (passwordInput.right - passwordInput.compoundDrawables[drawableEnd].bounds.width())) {

                    val selection = passwordInput.selectionEnd

                    if (isPasswordVisible) {
                        passwordInput.inputType =
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        isPasswordVisible = false
                    } else {
                        passwordInput.inputType =
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        isPasswordVisible = true
                    }

                    passwordInput.setSelection(selection)
                    v.performClick()
                    return@setOnTouchListener true
                }
            }
            false
        }

        // LOGIN BUTTON
        signInBtn.setOnClickListener {

            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            playTalkAnimation()

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->

                    if (task.isSuccessful) {

                        auth.currentUser?.reload()?.addOnCompleteListener {

                            val user = auth.currentUser

                            if (user == null) {
                                auth.signOut()
                                Toast.makeText(this, "Account error", Toast.LENGTH_LONG).show()
                                return@addOnCompleteListener
                            }

                            if (!user.isEmailVerified) {
                                auth.signOut()
                                Toast.makeText(this, "Please verify your email first.", Toast.LENGTH_LONG).show()
                                return@addOnCompleteListener
                            }

                            goToNextScreen(user.uid)
                        }

                    } else {
                        val randomMessage = errorMessages.random()
                        playTalkAnimation()
                        typeText(randomMessage)

                        Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()

                        carabuff.alpha = 0.7f
                        handler.postDelayed({
                            carabuff.alpha = 1f
                        }, 500)
                    }
                }
        }

        // FORGOT PASSWORD
        forgotPassword.setOnClickListener {

            val email = emailInput.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Enter your email first", Toast.LENGTH_SHORT).show()
            } else {

                playTalkAnimation()
                typeText("Sending reset email 📩")

                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->

                        if (task.isSuccessful) {
                            typeText("Check your email to reset password 💪")
                            Toast.makeText(this, "Reset email sent!", Toast.LENGTH_LONG).show()
                        } else {
                            typeText("Something went wrong 😅")
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

        // SIGNUP NAVIGATION
        signupText.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun playTalkAnimation() {
        carabuff.setImageResource(R.drawable.carabuff_talk_anim)

        val anim = carabuff.drawable as AnimationDrawable
        anim.start()

        val totalDuration = (0 until anim.numberOfFrames).sumOf {
            anim.getDuration(it)
        }

        carabuff.postDelayed({
            carabuff.setImageResource(R.drawable.carabuff1)
        }, totalDuration.toLong())
    }

    private fun typeText(text: String, isDefault: Boolean = false) {

        isThinking = true
        thoughtText.text = ""

        var index = 0

        handler.post(object : Runnable {
            override fun run() {
                if (index < text.length) {
                    thoughtText.text = thoughtText.text.toString() + text[index]
                    index++
                    handler.postDelayed(this, 40)
                } else {

                    if (!isDefault) {
                        handler.postDelayed({
                            deleteText(text)
                        }, 5000)
                    } else {
                        isThinking = false
                    }
                }
            }
        })
    }

    private fun deleteText(text: String) {

        var index = text.length

        handler.post(object : Runnable {
            override fun run() {
                if (index > 0) {
                    index--
                    thoughtText.text = text.substring(0, index)
                    handler.postDelayed(this, 25)
                } else {
                    typeText("...", true)
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()

        val user = auth.currentUser

        if (user != null) {
            user.reload().addOnCompleteListener {

                val refreshedUser = auth.currentUser

                if (refreshedUser != null && refreshedUser.isEmailVerified) {
                    goToNextScreen(refreshedUser.uid)
                }
            }
        }
    }

    private fun goToNextScreen(userId: String) {

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    val isComplete = document.getBoolean("isProfileComplete") ?: false

                    if (isComplete) {
                        startActivity(Intent(this, HomeActivity::class.java))
                    } else {
                        startActivity(Intent(this, SetupProfileActivity::class.java))
                    }

                } else {
                    startActivity(Intent(this, SetupProfileActivity::class.java))
                }

                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading user", Toast.LENGTH_SHORT).show()
            }
    }
}