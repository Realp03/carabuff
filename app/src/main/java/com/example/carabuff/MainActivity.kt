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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var carabuff: ImageView
    private lateinit var thoughtText: TextView

    private var isPasswordVisible = false
    private var isThinking = false
    private var hasPlayedIntro = false
    private var isRouting = false

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
        private const val RC_SIGN_IN = 1001
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
        val googleBtn = findViewById<SignInButton>(R.id.btnGoogleSignIn)

        carabuff = findViewById(R.id.carabuffSprite)
        thoughtText = findViewById(R.id.carabuffThought)

        googleBtn.setSize(SignInButton.SIZE_WIDE)

        googleBtn.post {
            for (i in 0 until googleBtn.childCount) {
                val child = googleBtn.getChildAt(i)
                if (child is TextView) {
                    child.text = "Sign in with Google"
                    child.isAllCaps = false
                }
            }
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

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
            if (isRouting) return@setOnClickListener

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
                                Toast.makeText(
                                    this,
                                    "Please verify your email first.",
                                    Toast.LENGTH_LONG
                                ).show()
                                forceResetCarabuff()
                                playRandomCarabuffAnimation("Please verify your email first 📩")
                                return@addOnCompleteListener
                            }

                            routeUserAfterLogin(user.uid, isGoogleUser = false)
                        }
                    } else {
                        forceResetCarabuff()
                        playRandomCarabuffAnimation(errorMessages.random())
                        Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        googleBtn.setOnClickListener {
            if (isRouting) return@setOnClickListener

            forceResetCarabuff()
            playRandomCarabuffAnimation("Opening Google sign in 😄")
            signInWithGoogle()
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
            overridePendingTransition(0, 0)
        }
    }

    override fun onStart() {
        super.onStart()

        if (isRouting) return

        val user = auth.currentUser ?: return
        user.reload().addOnCompleteListener {
            val refreshedUser = auth.currentUser ?: return@addOnCompleteListener

            val providerIds = refreshedUser.providerData.mapNotNull { it.providerId }
            val isGoogleUser = providerIds.contains("google.com")

            if (!isGoogleUser && !refreshedUser.isEmailVerified) {
                return@addOnCompleteListener
            }

            routeUserAfterLogin(
                userId = refreshedUser.uid,
                isGoogleUser = isGoogleUser,
                chosenGoogleEmail = refreshedUser.email?.trim()?.lowercase()
            )
        }
    }

    override fun onPostResume() {
        super.onPostResume()

        if (!hasPlayedIntro) {
            hasPlayedIntro = true
            carabuff.postDelayed({
                if (!isFinishing && !isDestroyed && !isRouting) {
                    playIntroCarabuff()
                }
            }, 400)
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
                val chosenGoogleEmail = account.email?.trim()?.lowercase()

                if (idToken.isNullOrEmpty() || chosenGoogleEmail.isNullOrEmpty()) {
                    forceResetCarabuff()
                    playRandomCarabuffAnimation("Google sign in failed ❌")
                    Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show()
                    return
                }

                firebaseAuthWithGoogle(idToken, chosenGoogleEmail)

            } catch (e: ApiException) {
                forceResetCarabuff()
                playRandomCarabuffAnimation("Google cancelled 😅")
                Toast.makeText(this, "Google sign in cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String, chosenGoogleEmail: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    if (user == null) {
                        forceResetCarabuff()
                        playRandomCarabuffAnimation("Google auth failed ❌")
                        Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }

                    handleGoogleSignInAfterAuth(user.uid, chosenGoogleEmail)
                } else {
                    forceResetCarabuff()
                    playRandomCarabuffAnimation("Google auth failed ❌")
                    Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun handleGoogleSignInAfterAuth(authUid: String, chosenGoogleEmail: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .whereEqualTo("email", chosenGoogleEmail)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val matchedDoc = snapshot.documents.first()
                    val matchedUid = matchedDoc.id
                    val storedGoogleLinkedEmail =
                        matchedDoc.getString("googleLinkedEmail")?.trim()?.lowercase()

                    if (matchedUid != authUid) {
                        forceFullGoogleLogout {
                            isRouting = false
                            forceResetCarabuff()

                            val message =
                                if (storedGoogleLinkedEmail.isNullOrEmpty()) {
                                    "This email already exists as a password account. Login manually first, then use Settings to relink Google."
                                } else {
                                    "That Google login does not match your existing Carabuff profile."
                                }

                            playRandomCarabuffAnimation("Use the correct linked account 🔐")
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        }
                        return@addOnSuccessListener
                    }
                }

                routeUserAfterLogin(
                    userId = authUid,
                    isGoogleUser = true,
                    chosenGoogleEmail = chosenGoogleEmail
                )
            }
            .addOnFailureListener {
                forceFullGoogleLogout {
                    isRouting = false
                    Toast.makeText(this, "Error validating Google account", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun forceFullGoogleLogout(onDone: (() -> Unit)? = null) {
        auth.signOut()

        googleSignInClient.signOut().addOnCompleteListener {
            googleSignInClient.revokeAccess().addOnCompleteListener {
                onDone?.invoke()
            }
        }
    }

    private fun routeUserAfterLogin(
        userId: String,
        isGoogleUser: Boolean,
        chosenGoogleEmail: String? = null
    ) {
        if (isRouting) return
        isRouting = true

        val db = FirebaseFirestore.getInstance()
        val currentFirebaseEmail = auth.currentUser?.email?.trim()?.lowercase()
        val actualChosenGoogleEmail = chosenGoogleEmail?.trim()?.lowercase()

        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    if (isGoogleUser) {
                        forceFullGoogleLogout {
                            isRouting = false
                            forceResetCarabuff()
                            playRandomCarabuffAnimation("No Carabuff account found. Please sign up first 📝")
                            Toast.makeText(
                                this,
                                "No Carabuff account found. Please sign up first.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        auth.signOut()
                        isRouting = false
                        forceResetCarabuff()
                        playRandomCarabuffAnimation("No Carabuff account found. Please sign up first 📝")
                        Toast.makeText(
                            this,
                            "No Carabuff account found. Please sign up first.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@addOnSuccessListener
                }

                val storedEmail = document.getString("email")?.trim()?.lowercase()
                val storedGoogleLinkedEmail = document.getString("googleLinkedEmail")?.trim()?.lowercase()

                if (isGoogleUser) {
                    val expectedGoogleEmail = when {
                        !storedGoogleLinkedEmail.isNullOrEmpty() -> storedGoogleLinkedEmail
                        !storedEmail.isNullOrEmpty() -> storedEmail
                        else -> currentFirebaseEmail
                    }

                    val actualGoogleEmail = when {
                        !actualChosenGoogleEmail.isNullOrEmpty() -> actualChosenGoogleEmail
                        !currentFirebaseEmail.isNullOrEmpty() -> currentFirebaseEmail
                        else -> ""
                    }

                    if (!expectedGoogleEmail.isNullOrEmpty() && actualGoogleEmail != expectedGoogleEmail) {
                        forceFullGoogleLogout {
                            isRouting = false
                            forceResetCarabuff()
                            playRandomCarabuffAnimation("This Google account is no longer linked to Carabuff 🚫")
                            Toast.makeText(
                                this,
                                "This Google account is no longer linked to this Carabuff account.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return@addOnSuccessListener
                    }
                }

                if (!currentFirebaseEmail.isNullOrEmpty()) {
                    val updates = mutableMapOf<String, Any>("email" to currentFirebaseEmail)

                    if (isGoogleUser && !actualChosenGoogleEmail.isNullOrEmpty()) {
                        updates["googleLinkedEmail"] = actualChosenGoogleEmail
                    }

                    db.collection("users")
                        .document(userId)
                        .set(updates, SetOptions.merge())
                }

                val name = document.getString("name")
                val age = document.get("age")?.toString()
                val gender = document.getString("gender")
                val weight = document.get("weight")?.toString()
                val height = document.get("height")?.toString()
                val lifestyle = document.getString("lifestyle")
                val timeAvailable = document.get("timeAvailable")?.toString()
                val activityLevel = document.get("activityLevel")?.toString()

                val profileIncomplete =
                    name.isNullOrEmpty() ||
                            age.isNullOrEmpty() ||
                            gender.isNullOrEmpty() ||
                            weight.isNullOrEmpty() ||
                            height.isNullOrEmpty() ||
                            lifestyle.isNullOrEmpty() ||
                            timeAvailable.isNullOrEmpty() ||
                            activityLevel.isNullOrEmpty()

                val bmi = document.getDouble("plan.bmi")
                val calories = document.getDouble("plan.calories")
                val protein = document.getDouble("plan.protein")
                val carbs = document.getDouble("plan.carbs")
                val fats = document.getDouble("plan.fats")

                val missingBmi = bmi == null || bmi <= 0.0
                val missingCalories = calories == null || calories <= 0.0
                val missingProtein = protein == null || protein <= 0.0
                val missingCarbs = carbs == null || carbs <= 0.0
                val missingFats = fats == null || fats <= 0.0

                if (profileIncomplete || missingBmi || missingCalories || missingProtein || missingCarbs || missingFats) {
                    openSetup()
                } else {
                    openHome()
                }
            }
            .addOnFailureListener {
                isRouting = false
                Toast.makeText(this, "Error loading user", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openSetup() {
        val intent = Intent(this, SetupProfileActivity::class.java).apply {
            putExtra("from_navbar", false)
            putExtra("from_login", true)
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    private fun openHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            putExtra("from_navbar", false)
            putExtra("from_login", true)
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
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

    override fun onPause() {
        super.onPause()
        stopAllCarabuffCallbacks()
        stopCurrentAnimationOnly()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAllCarabuffCallbacks()
        stopCurrentAnimationOnly()
        handler.removeCallbacksAndMessages(null)
    }
}