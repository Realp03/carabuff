package com.example.carabuff

import android.Manifest
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var carabuff: ImageView
    private lateinit var thoughtText: TextView

    private var isPasswordVisible = false
    private var isThinking = false
    private var hasPlayedIntro = false

    private val handler = Handler(Looper.getMainLooper())

    private var typingRunnable: Runnable? = null
    private var deletingRunnable: Runnable? = null
    private var animationEndRunnable: Runnable? = null
    private var currentAnim: AnimationDrawable? = null

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

    private val triviaList = listOf(
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

    companion object {
        private val STATIC_CARABUFF = R.drawable.carabuff1
        private val ANIM_TALK = R.drawable.carabuff_talk_anim
        private val ANIM_POINT = R.drawable.carabuff_point
        private val ANIM_VIBE = R.drawable.carabuff_vibe
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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

        showStaticCarabuff()
        thoughtText.text = "..."

        carabuff.setOnClickListener {
            forceResetCarabuff()
            playRandomCarabuffAnimation(triviaList.random())
        }

        passwordInput.setOnTouchListener { v, event ->
            val drawableEnd = 2

            if (event.action == MotionEvent.ACTION_UP) {
                val endDrawable = passwordInput.compoundDrawables[drawableEnd]
                if (endDrawable != null &&
                    event.rawX >= (passwordInput.right - endDrawable.bounds.width())
                ) {
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

        signInBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
                forceResetCarabuff()
                playRandomCarabuffAnimation("Enter your email and password first 📩")
                return@setOnClickListener
            }

            forceResetCarabuff()
            playRandomCarabuffAnimation("Signing you in...")

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        auth.currentUser?.reload()?.addOnCompleteListener {
                            val user = auth.currentUser

                            if (user == null) {
                                auth.signOut()
                                Toast.makeText(this, "Account error", Toast.LENGTH_LONG).show()
                                forceResetCarabuff()
                                playRandomCarabuffAnimation("Account error 😥")
                                return@addOnCompleteListener
                            }

                            if (!user.isEmailVerified) {
                                auth.signOut()
                                Toast.makeText(this, "Please verify your email first.", Toast.LENGTH_LONG).show()
                                forceResetCarabuff()
                                playRandomCarabuffAnimation("Please verify your email first 📩")
                                return@addOnCompleteListener
                            }

                            goToNextScreen(user.uid)
                        }
                    } else {
                        forceResetCarabuff()
                        playRandomCarabuffAnimation(errorMessages.random())
                        Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        forgotPassword.setOnClickListener {
            val email = emailInput.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Enter your email first", Toast.LENGTH_SHORT).show()
                forceResetCarabuff()
                playRandomCarabuffAnimation("Enter your email first 📩")
            } else {
                forceResetCarabuff()
                playRandomCarabuffAnimation("Sending reset email 📩")

                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            forceResetCarabuff()
                            playRandomCarabuffAnimation("Check your email to reset password 💪")
                            Toast.makeText(this, "Reset email sent!", Toast.LENGTH_LONG).show()
                        } else {
                            forceResetCarabuff()
                            playRandomCarabuffAnimation("Something went wrong 😅")
                            Toast.makeText(
                                this,
                                "Error: ${task.exception?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            }
        }

        signupText.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    override fun onPostResume() {
        super.onPostResume()

        if (!hasPlayedIntro) {
            hasPlayedIntro = true

            carabuff.postDelayed({
                if (!isFinishing && !isDestroyed) {
                    playIntroCarabuff()
                }
            }, 400)
        }
    }

    private fun playIntroCarabuff() {
        stopAllCarabuffCallbacks()
        isThinking = true

        val introMessage = "Hi! I'm Carabuff, your fitness AI companion 💪"
        thoughtText.text = introMessage

        playAnimationFromRes(ANIM_TALK)

        deletingRunnable = Runnable {
            deleteText(introMessage)
        }
        handler.postDelayed(deletingRunnable!!, 2200)

        animationEndRunnable = Runnable {
            if (!isFinishing && !isDestroyed) {
                showStaticCarabuff()
            }
        }
        handler.postDelayed(animationEndRunnable!!, getAnimationDuration(ANIM_TALK))
    }

    private fun playRandomCarabuffAnimation(message: String) {
        stopAllCarabuffCallbacks()
        isThinking = true
        thoughtText.text = ""

        val randomAnim = listOf(ANIM_TALK, ANIM_POINT, ANIM_VIBE).random()
        playAnimationFromRes(randomAnim)

        typingRunnable = Runnable {
            typeText(message)
        }
        handler.postDelayed(typingRunnable!!, 100)

        animationEndRunnable = Runnable {
            if (!isFinishing && !isDestroyed) {
                showStaticCarabuff()
            }
        }
        handler.postDelayed(animationEndRunnable!!, getAnimationDuration(randomAnim))
    }

    private fun playAnimationFromRes(resId: Int) {
        stopCurrentAnimationOnly()

        val drawable: Drawable? = ContextCompat.getDrawable(this, resId)
        if (drawable is AnimationDrawable) {
            val anim = drawable.constantState?.newDrawable()?.mutate()
            if (anim is AnimationDrawable) {
                anim.isOneShot = true
                currentAnim = anim
                carabuff.setImageDrawable(anim)

                carabuff.post {
                    currentAnim?.stop()
                    currentAnim?.start()
                }
                return
            }
        }

        carabuff.setImageResource(resId)
        currentAnim = null
    }

    private fun showStaticCarabuff() {
        stopCurrentAnimationOnly()
        carabuff.setImageResource(STATIC_CARABUFF)
    }

    private fun stopCurrentAnimationOnly() {
        animationEndRunnable?.let { handler.removeCallbacks(it) }
        currentAnim?.stop()
        currentAnim = null
        carabuff.clearAnimation()
    }

    private fun stopAllCarabuffCallbacks() {
        typingRunnable?.let { handler.removeCallbacks(it) }
        deletingRunnable?.let { handler.removeCallbacks(it) }
        animationEndRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun forceResetCarabuff() {
        stopAllCarabuffCallbacks()
        currentAnim?.stop()
        currentAnim = null
        carabuff.clearAnimation()
        thoughtText.text = "..."
        isThinking = false
        carabuff.setImageResource(STATIC_CARABUFF)
    }

    private fun typeText(text: String) {
        deletingRunnable?.let { handler.removeCallbacks(it) }

        thoughtText.text = ""
        var index = 0

        typingRunnable = object : Runnable {
            override fun run() {
                if (index < text.length) {
                    thoughtText.text = text.substring(0, index + 1)
                    index++
                    handler.postDelayed(this, 40)
                } else {
                    deletingRunnable = Runnable {
                        deleteText(text)
                    }
                    handler.postDelayed(deletingRunnable!!, 1800)
                }
            }
        }

        handler.post(typingRunnable!!)
    }

    private fun deleteText(text: String) {
        deletingRunnable?.let { handler.removeCallbacks(it) }

        var index = text.length

        deletingRunnable = object : Runnable {
            override fun run() {
                if (index > 0) {
                    index--
                    thoughtText.text = text.substring(0, index)
                    handler.postDelayed(this, 25)
                } else {
                    thoughtText.text = "..."
                    isThinking = false
                    showStaticCarabuff()
                }
            }
        }

        handler.post(deletingRunnable!!)
    }

    private fun getAnimationDuration(resId: Int): Long {
        val drawable = ContextCompat.getDrawable(this, resId)
        return if (drawable is AnimationDrawable) {
            var total = 0
            for (i in 0 until drawable.numberOfFrames) {
                total += drawable.getDuration(i)
            }
            total.toLong()
        } else {
            1000L
        }
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

    override fun onPause() {
        super.onPause()
        stopAllCarabuffCallbacks()
        stopCurrentAnimationOnly()
        if (!isThinking) {
            showStaticCarabuff()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        stopCurrentAnimationOnly()
    }
}