package com.example.carabuff

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : AppCompatActivity() {

    private lateinit var btnBack: TextView
    private lateinit var changePasswordBtn: LinearLayout
    private lateinit var changeEmailBtn: LinearLayout
    private lateinit var deleteAccountBtn: LinearLayout
    private lateinit var switchWorkout: Switch
    private lateinit var switchMeal: Switch

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        btnBack = findViewById(R.id.btnBack)
        changePasswordBtn = findViewById(R.id.changePasswordBtn)
        changeEmailBtn = findViewById(R.id.changeEmailBtn)
        deleteAccountBtn = findViewById(R.id.deleteAccountBtn)
        switchWorkout = findViewById(R.id.switchWorkout)
        switchMeal = findViewById(R.id.switchMeal)

        btnBack.setOnClickListener { finish() }

        changePasswordBtn.setOnClickListener { showChangePasswordDialog() }
        changeEmailBtn.setOnClickListener { showChangeEmailDialog() }
        deleteAccountBtn.setOnClickListener { showDeleteDialog() }

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        switchWorkout.isChecked = prefs.getBoolean("workoutReminder", false)
        switchMeal.isChecked = prefs.getBoolean("mealReminder", false)

        switchWorkout.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("workoutReminder", isChecked).apply()
        }

        switchMeal.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("mealReminder", isChecked).apply()
        }
    }

    // 🔥 AUTO LOGOUT AFTER EMAIL VERIFIED
    override fun onResume() {
        super.onResume()

        val user = auth.currentUser ?: return
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        val pendingEmailChange = prefs.getBoolean("pendingEmailChange", false)
        val oldEmail = prefs.getString("oldEmail", null)

        if (!pendingEmailChange) return

        user.reload().addOnSuccessListener {

            // 🔥 CHECK IF EMAIL CHANGED
            if (user.email != oldEmail) {

                // 🔥 SEND NOTIFICATION
                val helper = NotificationHelper(this)
                helper.sendNotification(
                    "Email Updated 📧",
                    "Your email has been successfully changed.",
                    "security"
                )

                // 🔥 CLEAR FLAGS
                prefs.edit()
                    .remove("pendingEmailChange")
                    .remove("oldEmail")
                    .apply()

                // 🔥 FORCE LOGOUT
                FirebaseAuth.getInstance().signOut()

                Toast.makeText(
                    this,
                    "Email changed. Please login again.",
                    Toast.LENGTH_LONG
                ).show()

                val intent = Intent(this, MainActivity::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }

    // 📧 CHANGE EMAIL
    private fun showChangeEmailDialog() {

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 20, 40, 10)

        val newEmail = EditText(this)
        newEmail.hint = "New Email"

        val password = EditText(this)
        password.hint = "Current Password"

        layout.addView(newEmail)
        layout.addView(password)

        AlertDialog.Builder(this)
            .setTitle("Change Email")
            .setMessage("A verification link will be sent. Check your email.")
            .setView(layout)
            .setPositiveButton("Update") { _, _ ->

                val user = auth.currentUser
                val currentEmail = user?.email

                if (user != null && currentEmail != null) {

                    val credential = EmailAuthProvider.getCredential(
                        currentEmail,
                        password.text.toString()
                    )

                    user.reauthenticate(credential)
                        .addOnSuccessListener {

                            user.verifyBeforeUpdateEmail(newEmail.text.toString())
                                .addOnSuccessListener {

                                    Toast.makeText(
                                        this,
                                        "Verification sent! Check your email.",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    // 🔥 SAVE STATE
                                    val prefs = getSharedPreferences("settings", MODE_PRIVATE)
                                    prefs.edit()
                                        .putBoolean("pendingEmailChange", true)
                                        .putString("oldEmail", currentEmail)
                                        .apply()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Failed to send email", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Wrong Password", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // 🔐 CHANGE PASSWORD
    private fun showChangePasswordDialog() {

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 20, 40, 10)

        val currentPass = EditText(this)
        val newPass = EditText(this)

        currentPass.hint = "Current Password"
        newPass.hint = "New Password"

        layout.addView(currentPass)
        layout.addView(newPass)

        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(layout)
            .setPositiveButton("Update") { _, _ ->

                val user = auth.currentUser
                val email = user?.email

                if (user != null && email != null) {

                    val credential = EmailAuthProvider.getCredential(
                        email,
                        currentPass.text.toString()
                    )

                    user.reauthenticate(credential)
                        .addOnSuccessListener {

                            user.updatePassword(newPass.text.toString())
                                .addOnSuccessListener {

                                    Toast.makeText(this, "Password Updated", Toast.LENGTH_SHORT).show()

                                    // 🔥 NOTIFICATION
                                    val helper = NotificationHelper(this)
                                    helper.sendNotification(
                                        "Password Updated 🔐",
                                        "Your password was successfully changed.",
                                        "security"
                                    )
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Failed to update password", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Wrong Password", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ❌ DELETE ACCOUNT
    private fun showDeleteDialog() {

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 20, 40, 10)

        val passwordInput = EditText(this)
        passwordInput.hint = "Enter Password"

        layout.addView(passwordInput)

        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("This action cannot be undone")
            .setView(layout)
            .setPositiveButton("Delete") { _, _ ->

                val user = auth.currentUser
                val email = user?.email
                val password = passwordInput.text.toString()

                if (user != null && email != null) {

                    val credential = EmailAuthProvider.getCredential(email, password)

                    user.reauthenticate(credential)
                        .addOnSuccessListener {

                            val userId = user.uid

                            db.collection("users").document(userId).delete()
                            db.collection("user_profiles").document(userId).delete()

                            user.delete()
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Account Deleted", Toast.LENGTH_SHORT).show()

                                    val intent = Intent(this, MainActivity::class.java)
                                    intent.flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Wrong Password", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}