package com.example.carabuff

import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var carabuff: ImageView
    private lateinit var thoughtText: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var isThinking = false

    private val passwordHints = listOf(
        "Password must be at least 8 characters 😅",
        "Add at least 1 number 🔢",
        "Include an uppercase letter 🔠",
        "Make your password stronger 💪",
        "Follow the rules please 😎"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val confirmPasswordInput = findViewById<EditText>(R.id.confirmPasswordInput)
        val signUpBtn = findViewById<Button>(R.id.signUpButton)
        val signinText = findViewById<TextView>(R.id.signinText)

        carabuff = findViewById(R.id.carabuffSprite)
        thoughtText = findViewById(R.id.carabuffThought)

        // 🔥 DEFAULT PASSWORD STATE (HIDDEN)
        passwordInput.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        confirmPasswordInput.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        // 🔥 INTRO
        speak("Hey! I’d appreciate it if you sign up in the app 😄")

        // 🔒 PASSWORD TOGGLE
        passwordInput.setOnTouchListener { v, event ->
            val drawable = passwordInput.compoundDrawables[2]

            if (drawable != null && event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (passwordInput.right - drawable.bounds.width())) {

                    isPasswordVisible = !isPasswordVisible

                    passwordInput.inputType =
                        if (isPasswordVisible)
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        else
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

                    passwordInput.typeface =
                        ResourcesCompat.getFont(this, R.font.iceland_regular)

                    passwordInput.setSelection(passwordInput.text.length)

                    v.performClick()
                    return@setOnTouchListener true
                }
            }
            false
        }

        // 🔒 CONFIRM PASSWORD TOGGLE
        confirmPasswordInput.setOnTouchListener { v, event ->
            val drawable = confirmPasswordInput.compoundDrawables[2]

            if (drawable != null && event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (confirmPasswordInput.right - drawable.bounds.width())) {

                    isConfirmPasswordVisible = !isConfirmPasswordVisible

                    confirmPasswordInput.inputType =
                        if (isConfirmPasswordVisible)
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        else
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

                    confirmPasswordInput.typeface =
                        ResourcesCompat.getFont(this, R.font.iceland_regular)

                    confirmPasswordInput.setSelection(confirmPasswordInput.text.length)

                    v.performClick()
                    return@setOnTouchListener true
                }
            }
            false
        }

        // 🔥 SIGN UP
        signUpBtn.setOnClickListener {

            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirm = confirmPasswordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                speak("Please fill all fields 📝")
                return@setOnClickListener
            }

            if (!isValidPassword(password)) {
                speak(passwordHints.random())
                return@setOnClickListener
            }

            if (password != confirm) {
                speak("Passwords do not match ❌")
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->

                    if (task.isSuccessful) {

                        val user = auth.currentUser

                        user?.sendEmailVerification()

                        val userId = user!!.uid

                        val userMap = hashMapOf(
                            "email" to email
                        )

                        db.collection("users")
                            .document(userId)
                            .set(userMap)

                        speak("Nice! You're all set 🎉")

                        Toast.makeText(this, "Account Created! Verify your email.", Toast.LENGTH_LONG).show()

                        auth.signOut()

                        startActivity(Intent(this, MainActivity::class.java))
                        finish()

                    } else {
                        val errorCode = (task.exception as? FirebaseAuthException)?.errorCode

                        val message = when (errorCode) {
                            "ERROR_EMAIL_ALREADY_IN_USE" -> "Email already registered 📧"
                            "ERROR_INVALID_EMAIL" -> "Invalid email 😅"
                            "ERROR_WEAK_PASSWORD" -> "Weak password 💪"
                            else -> "Something went wrong 🤔"
                        }

                        speak(message)
                    }
                }
        }

        signinText.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 8 &&
                password.any { it.isUpperCase() } &&
                password.any { it.isDigit() }
    }

    private fun speak(message: String) {
        playTalkAnimation()
        typeText(message)
    }

    private fun playTalkAnimation() {
        carabuff.setImageResource(R.drawable.carabuff_talk_anim)

        val anim = carabuff.drawable as AnimationDrawable
        anim.start()

        val total = (0 until anim.numberOfFrames).sumOf {
            anim.getDuration(it)
        }

        carabuff.postDelayed({
            carabuff.setImageResource(R.drawable.carabuff1)
        }, total.toLong())
    }

    private fun typeText(text: String) {

        if (isThinking) return

        isThinking = true
        thoughtText.text = ""

        var index = 0

        handler.post(object : Runnable {
            override fun run() {
                if (index < text.length) {
                    thoughtText.text = thoughtText.text.toString() + text[index]
                    index++
                    handler.postDelayed(this, 35)
                } else {
                    isThinking = false
                }
            }
        })
    }
}