package com.example.carabuff

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var carabuff: ImageView
    private lateinit var thoughtText: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var isThinking = false

    companion object {
        private const val RC_SIGN_IN = 1001
    }

    private val passwordHints = listOf(
        "Password must be at least 8 characters 😅",
        "Add at least 1 number 🔢",
        "Include an uppercase letter 🔠",
        "Include a lowercase letter 🔡",
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

        val signUpBtn = findViewById<Button>(R.id.signupBtn)
        val googleSignUpBtn = findViewById<SignInButton>(R.id.btnGoogleSignIn)
        val signinText = findViewById<TextView>(R.id.loginRedirect)

        carabuff = findViewById(R.id.carabuffImage)
        thoughtText = findViewById(R.id.thoughtText)

        val ruleLength = findViewById<TextView>(R.id.ruleLength)
        val ruleNumber = findViewById<TextView>(R.id.ruleNumber)
        val ruleUpper = findViewById<TextView>(R.id.ruleUpper)
        val ruleLower = findViewById<TextView>(R.id.ruleLower)

        googleSignUpBtn.setSize(SignInButton.SIZE_WIDE)

        googleSignUpBtn.post {
            for (i in 0 until googleSignUpBtn.childCount) {
                val child = googleSignUpBtn.getChildAt(i)
                if (child is TextView) {
                    child.text = "Sign up with Google"
                    child.isAllCaps = false
                    child.textSize = 16f
                }
            }
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        passwordInput.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        confirmPasswordInput.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        speak("Hey! I’d appreciate it if you sign up in the app 😄")

        passwordInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()

                val hasLength = password.length >= 8
                val hasNumber = password.any { it.isDigit() }
                val hasUpper = password.any { it.isUpperCase() }
                val hasLower = password.any { it.isLowerCase() }

                updateRule(ruleLength, hasLength, "At least 8 characters")
                updateRule(ruleNumber, hasNumber, "At least 1 number")
                updateRule(ruleUpper, hasUpper, "At least 1 uppercase letter")
                updateRule(ruleLower, hasLower, "At least 1 lowercase letter")
            }
        })

        passwordInput.setOnTouchListener { v, event ->
            val drawable = passwordInput.compoundDrawables[2]

            if (drawable != null && event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (passwordInput.right - drawable.bounds.width())) {
                    isPasswordVisible = !isPasswordVisible

                    passwordInput.inputType =
                        if (isPasswordVisible) {
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        } else {
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        }

                    passwordInput.typeface =
                        ResourcesCompat.getFont(this, R.font.iceland_regular)

                    passwordInput.setSelection(passwordInput.text.length)

                    v.performClick()
                    return@setOnTouchListener true
                }
            }
            false
        }

        confirmPasswordInput.setOnTouchListener { v, event ->
            val drawable = confirmPasswordInput.compoundDrawables[2]

            if (drawable != null && event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (confirmPasswordInput.right - drawable.bounds.width())) {
                    isConfirmPasswordVisible = !isConfirmPasswordVisible

                    confirmPasswordInput.inputType =
                        if (isConfirmPasswordVisible) {
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        } else {
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        }

                    confirmPasswordInput.typeface =
                        ResourcesCompat.getFont(this, R.font.iceland_regular)

                    confirmPasswordInput.setSelection(confirmPasswordInput.text.length)

                    v.performClick()
                    return@setOnTouchListener true
                }
            }
            false
        }

        signUpBtn.setOnClickListener {
            val email = emailInput.text.toString().trim().lowercase()
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

                        val userId = user?.uid ?: return@addOnCompleteListener

                        val userMap = hashMapOf(
                            "email" to email,
                            "name" to "",
                            "gender" to "",
                            "birthday" to "",
                            "age" to 0,
                            "isProfileComplete" to false,
                            "authProvider" to "email"
                        )

                        db.collection("users")
                            .document(userId)
                            .set(userMap)
                            .addOnSuccessListener {
                                speak("Nice! You're all set 🎉")
                                Toast.makeText(
                                    this,
                                    "Account Created! Verify your email.",
                                    Toast.LENGTH_LONG
                                ).show()

                                auth.signOut()
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener {
                                speak("Account created but failed to save profile data 🤔")
                            }

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

        googleSignUpBtn.setOnClickListener {
            speak("Opening Google sign up 😄")
            signInWithGoogle()
        }

        signinText.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun updateRule(textView: TextView, isValid: Boolean, text: String) {
        if (isValid) {
            textView.text = "✔ $text"
            textView.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            textView.text = "✖ $text"
            textView.setTextColor(Color.parseColor("#FF6B6B"))
        }
    }

    private fun signInWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
            overridePendingTransition(0, 0)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken

                if (idToken != null) {
                    firebaseAuthWithGoogle(idToken)
                } else {
                    speak("Google sign in failed ❌")
                    Toast.makeText(this, "Google ID token is null", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                speak("Google sign in cancelled or failed ❌")
                Toast.makeText(
                    this,
                    "Google sign in failed: ${e.statusCode}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    if (user == null) {
                        speak("Google sign in failed ❌")
                        return@addOnCompleteListener
                    }

                    val userId = user.uid
                    val userEmail = user.email?.trim()?.lowercase() ?: ""

                    db.collection("users")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                val isProfileComplete =
                                    document.getBoolean("isProfileComplete") ?: false

                                speak("Welcome ${user.displayName ?: "back"} 🎉")

                                if (isProfileComplete) {
                                    startActivity(Intent(this, HomeActivity::class.java))
                                } else {
                                    startActivity(Intent(this, SetupProfileActivity::class.java))
                                }
                                finish()
                            } else {
                                val userMap = hashMapOf(
                                    "email" to userEmail,
                                    "name" to (user.displayName ?: ""),
                                    "gender" to "",
                                    "birthday" to "",
                                    "age" to 0,
                                    "photoUrl" to (user.photoUrl?.toString() ?: ""),
                                    "isProfileComplete" to false,
                                    "authProvider" to "google",
                                    "googleLinkedEmail" to userEmail
                                )

                                db.collection("users")
                                    .document(userId)
                                    .set(userMap)
                                    .addOnSuccessListener {
                                        speak("Welcome ${user.displayName ?: "to Carabuff"} 🎉")
                                        startActivity(Intent(this, SetupProfileActivity::class.java))
                                        finish()
                                    }
                                    .addOnFailureListener {
                                        speak("Google account created but failed to save user data 🤔")
                                    }
                            }
                        }
                        .addOnFailureListener {
                            speak("Failed to check your account data 🤔")
                        }

                } else {
                    speak("Firebase Google auth failed ❌")
                    Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 8 &&
                password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() } &&
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