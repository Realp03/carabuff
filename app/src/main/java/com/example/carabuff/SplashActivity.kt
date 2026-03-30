package com.example.carabuff

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {

    private var notifId: Int = -1 // 🔥 store notifId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 🔥 GET NOTIFICATION ID (FROM CLICK)
        notifId = intent.getIntExtra("notifId", -1)

        // 🔥 CANCEL NOTIFICATION IMMEDIATELY
        if (notifId != -1) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.cancel(notifId)
        }

        val logo = findViewById<ImageView>(R.id.logo)

        // 🔥 Animation
        logo.alpha = 0f
        logo.animate().alpha(1f).setDuration(1000).start()

        Handler(Looper.getMainLooper()).postDelayed({

            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                goToLogin()
                return@postDelayed
            }

            user.reload().addOnCompleteListener {

                val refreshedUser = FirebaseAuth.getInstance().currentUser

                if (refreshedUser == null) {
                    goToLogin()
                    return@addOnCompleteListener
                }

                val userId = refreshedUser.uid

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener { document ->

                        if (!document.exists()) {
                            goToSetup()
                            return@addOnSuccessListener
                        }

                        val name = document.get("name")?.toString()
                        val age = document.get("age")?.toString()
                        val gender = document.get("gender")?.toString()
                        val weight = document.get("weight")?.toString()
                        val height = document.get("height")?.toString()
                        val lifestyle = document.get("lifestyle")?.toString()
                        val time = document.get("timeAvailable")?.toString()
                        val activity = document.get("activityLevel")?.toString()

                        if (
                            name.isNullOrEmpty() ||
                            age.isNullOrEmpty() ||
                            gender.isNullOrEmpty() ||
                            weight.isNullOrEmpty() ||
                            height.isNullOrEmpty() ||
                            lifestyle.isNullOrEmpty() ||
                            time.isNullOrEmpty() ||
                            activity.isNullOrEmpty()
                        ) {
                            goToSetup()
                        } else {
                            goToHome()
                        }
                    }
                    .addOnFailureListener {
                        goToLogin()
                    }
            }

        }, 1500)
    }

    private fun goToHome() {
        val intent = Intent(this, HomeActivity::class.java)

        // 🔥 PASS notifId forward
        if (notifId != -1) {
            intent.putExtra("notifId", notifId)
        }

        startActivity(intent)
        finish()
    }

    private fun goToSetup() {
        startActivity(Intent(this, SetupProfileActivity::class.java))
        finish()
    }

    private fun goToLogin() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}