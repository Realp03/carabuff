package com.example.carabuff

import android.animation.ValueAnimator
import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {

    private var notifId: Int = -1

    private lateinit var logo: ImageView
    private lateinit var tvAppName: TextView
    private lateinit var tvTagline: TextView
    private lateinit var tvLoadingPercent: TextView
    private lateinit var loadingTrack: View
    private lateinit var loadingFill: View
    private lateinit var topGlow: View
    private lateinit var bottomGlow: View
    private lateinit var splashContainer: View
    private lateinit var logoCard: View

    private val handler = Handler(Looper.getMainLooper())

    private var progressAnimator: ValueAnimator? = null

    private var isLeavingScreen = false
    private var isRouteReady = false
    private var currentProgress = 0
    private var nextIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        notifId = intent.getIntExtra("notifId", -1)

        if (notifId != -1) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(notifId)
        }

        logo = findViewById(R.id.logo)
        tvAppName = findViewById(R.id.tvAppName)
        tvTagline = findViewById(R.id.tvTagline)
        tvLoadingPercent = findViewById(R.id.tvLoadingPercent)
        loadingTrack = findViewById(R.id.loadingTrack)
        loadingFill = findViewById(R.id.loadingFill)
        topGlow = findViewById(R.id.topGlow)
        bottomGlow = findViewById(R.id.bottomGlow)
        splashContainer = findViewById(R.id.splashContainer)
        logoCard = findViewById(R.id.logoCard)

        prepareIntroState()
        playSplashAnimation()

        loadingTrack.post {
            updateProgressUI(0)
            startFakeLoading()
        }

        handler.postDelayed({
            checkUserFlow()
        }, 300)
    }

    private fun prepareIntroState() {
        logo.alpha = 0f
        logo.scaleX = 0.72f
        logo.scaleY = 0.72f
        logo.translationY = 40f

        logoCard.alpha = 0f
        logoCard.scaleX = 0.90f
        logoCard.scaleY = 0.90f
        logoCard.translationY = 20f

        tvAppName.alpha = 0f
        tvAppName.translationY = 28f

        tvTagline.alpha = 0f
        tvTagline.translationY = 20f

        loadingTrack.alpha = 0f
        loadingTrack.scaleX = 0.85f

        tvLoadingPercent.alpha = 0f
        tvLoadingPercent.translationY = 10f

        topGlow.alpha = 0f
        topGlow.scaleX = 0.8f
        topGlow.scaleY = 0.8f

        bottomGlow.alpha = 0f
        bottomGlow.scaleX = 0.8f
        bottomGlow.scaleY = 0.8f
    }

    private fun playSplashAnimation() {
        topGlow.animate()
            .alpha(0.18f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(1000)
            .setInterpolator(DecelerateInterpolator())
            .start()

        bottomGlow.animate()
            .alpha(0.10f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(1200)
            .setInterpolator(DecelerateInterpolator())
            .start()

        logoCard.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(650)
            .setInterpolator(OvershootInterpolator(1.15f))
            .start()

        logo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setStartDelay(120)
            .setDuration(750)
            .setInterpolator(OvershootInterpolator(1.2f))
            .withEndAction {
                startFloatingLogo()
            }
            .start()

        tvAppName.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(380)
            .setDuration(500)
            .setInterpolator(DecelerateInterpolator())
            .start()

        tvTagline.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(560)
            .setDuration(450)
            .setInterpolator(DecelerateInterpolator())
            .start()

        loadingTrack.animate()
            .alpha(1f)
            .scaleX(1f)
            .setStartDelay(760)
            .setDuration(450)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        tvLoadingPercent.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(860)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun startFloatingLogo() {
        if (isLeavingScreen) return

        logo.animate()
            .translationY(-10f)
            .setDuration(1200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                if (isLeavingScreen) return@withEndAction

                logo.animate()
                    .translationY(0f)
                    .setDuration(1200)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        if (!isLeavingScreen) {
                            startFloatingLogo()
                        }
                    }
                    .start()
            }
            .start()
    }

    private fun startFakeLoading() {
        progressAnimator?.cancel()

        progressAnimator = ValueAnimator.ofInt(0, 92).apply {
            duration = 2600L
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                currentProgress = value
                updateProgressUI(currentProgress)
            }
            start()
        }
    }

    private fun finishLoadingToHundred() {
        progressAnimator?.cancel()

        progressAnimator = ValueAnimator.ofInt(currentProgress, 100).apply {
            duration = 500L
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                currentProgress = value
                updateProgressUI(currentProgress)
            }
            start()
        }

        handler.postDelayed({
            if (isRouteReady && nextIntent != null && !isLeavingScreen) {
                animateExitAndNavigate(nextIntent!!)
            }
        }, 520)
    }

    private fun updateProgressUI(progress: Int) {
        val safeProgress = progress.coerceIn(0, 100)
        tvLoadingPercent.text = "$safeProgress%"

        val trackWidth = loadingTrack.width
        if (trackWidth <= 0) return

        val targetWidth = ((trackWidth - dpToPx(2)) * (safeProgress / 100f)).toInt().coerceAtLeast(0)
        val params = loadingFill.layoutParams
        params.width = targetWidth
        loadingFill.layoutParams = params
    }

    private fun checkUserFlow() {
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            prepareRouteTo(Intent(this, MainActivity::class.java).apply {
                putExtra("from_splash", true)
            })
            return
        }

        user.reload().addOnCompleteListener {
            val refreshedUser = FirebaseAuth.getInstance().currentUser

            if (refreshedUser == null) {
                prepareRouteTo(Intent(this, MainActivity::class.java).apply {
                    putExtra("from_splash", true)
                })
                return@addOnCompleteListener
            }

            val userId = refreshedUser.uid
            val db = FirebaseFirestore.getInstance()

            db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (!document.exists()) {
                        openSetupFromSplash()
                        return@addOnSuccessListener
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
                        openSetupFromSplash()
                    } else {
                        prepareRouteTo(Intent(this, HomeActivity::class.java).apply {
                            if (notifId != -1) putExtra("notifId", notifId)
                            putExtra("from_splash", true)
                        })
                    }
                }
                .addOnFailureListener {
                    prepareRouteTo(Intent(this, MainActivity::class.java).apply {
                        putExtra("from_splash", true)
                    })
                }
        }
    }

    private fun openSetupFromSplash() {
        prepareRouteTo(Intent(this, SetupProfileActivity::class.java).apply {
            if (notifId != -1) putExtra("notifId", notifId)
            putExtra("from_splash", true)
        })
    }

    private fun prepareRouteTo(intent: Intent) {
        nextIntent = intent
        isRouteReady = true

        if (currentProgress >= 100) {
            if (!isLeavingScreen) animateExitAndNavigate(intent)
        } else {
            finishLoadingToHundred()
        }
    }

    private fun animateExitAndNavigate(intent: Intent) {
        if (isLeavingScreen) return

        isLeavingScreen = true
        progressAnimator?.cancel()

        splashContainer.animate()
            .alpha(0f)
            .translationX(-120f)
            .setDuration(280)
            .withEndAction {
                startActivity(intent)
                overridePendingTransition(0, 0)
                finish()
            }
            .start()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        isLeavingScreen = true
        progressAnimator?.cancel()
        handler.removeCallbacksAndMessages(null)
    }
}