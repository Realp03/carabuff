package com.example.carabuff

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class SettingsActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var changePasswordBtn: LinearLayout
    private lateinit var changeEmailBtn: LinearLayout
    private lateinit var deleteAccountBtn: LinearLayout
    private lateinit var switchWorkout: Switch
    private lateinit var switchMeal: Switch
    private lateinit var tvSecurityHint: TextView
    private lateinit var tvDeleteAccountHint: TextView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var isLoadingReminderState = false

    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    private lateinit var googleSignInClient: GoogleSignInClient

    private var pendingDeleteAfterGoogleReauth = false
    private var pendingExpectedGoogleEmail: String? = null

    companion object {
        private const val RC_LINK_GOOGLE_AFTER_EMAIL_CHANGE = 2001
    }

    private val googleReauthLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                Toast.makeText(this, "Google confirmation cancelled", Toast.LENGTH_SHORT).show()
                pendingDeleteAfterGoogleReauth = false
                return@registerForActivityResult
            }

            try {
                val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                val idToken = credential.googleIdToken

                if (idToken.isNullOrEmpty()) {
                    Toast.makeText(this, "Google token not found", Toast.LENGTH_SHORT).show()
                    pendingDeleteAfterGoogleReauth = false
                    return@registerForActivityResult
                }

                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val user = auth.currentUser

                if (user == null) {
                    Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                    pendingDeleteAfterGoogleReauth = false
                    return@registerForActivityResult
                }

                user.reauthenticate(firebaseCredential)
                    .addOnSuccessListener {
                        if (pendingDeleteAfterGoogleReauth) {
                            deleteCurrentUserAccount()
                        }
                    }
                    .addOnFailureListener { e ->
                        pendingDeleteAfterGoogleReauth = false
                        Toast.makeText(
                            this,
                            e.message ?: "Google confirmation failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } catch (e: Exception) {
                pendingDeleteAfterGoogleReauth = false
                Toast.makeText(
                    this,
                    e.message ?: "Google confirmation failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        btnBack = findViewById(R.id.btnBack)
        changePasswordBtn = findViewById(R.id.changePasswordBtn)
        changeEmailBtn = findViewById(R.id.changeEmailBtn)
        deleteAccountBtn = findViewById(R.id.deleteAccountBtn)
        switchWorkout = findViewById(R.id.switchWorkout)
        switchMeal = findViewById(R.id.switchMeal)
        tvSecurityHint = findViewById(R.id.tvSecurityHint)
        tvDeleteAccountHint = findViewById(R.id.tvDeleteAccountHint)

        setupGoogleOneTap()
        setupGoogleSignInClient()
        applyAccountSecurityUi()

        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }

        changePasswordBtn.setOnClickListener {
            if (isPasswordUser()) {
                showChangePasswordDialog()
            } else {
                Toast.makeText(this, "This account uses Google Sign-In", Toast.LENGTH_SHORT).show()
            }
        }

        changeEmailBtn.setOnClickListener {
            if (isPasswordUser()) {
                showChangeEmailDialog()
            } else {
                Toast.makeText(
                    this,
                    "Email changes for Google-only accounts are managed by Google.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        deleteAccountBtn.setOnClickListener {
            showDeleteDialog()
        }

        setupReminderSwitches()
        loadReminderSettings()
    }

    override fun onStart() {
        super.onStart()
        checkPendingEmailChange()
        applyAccountSecurityUi()
    }

    override fun onResume() {
        super.onResume()
        checkPendingEmailChange()
        applyAccountSecurityUi()
    }

    private fun setupGoogleOneTap() {
        oneTapClient = Identity.getSignInClient(this)

        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(true)
                    .build()
            )
            .setAutoSelectEnabled(false)
            .build()
    }

    private fun setupGoogleSignInClient() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun isPasswordUser(): Boolean {
        val user = auth.currentUser ?: return false
        return user.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }
    }

    private fun isGoogleUser(): Boolean {
        val user = auth.currentUser ?: return false
        return user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
    }

    private fun applyAccountSecurityUi() {
        val passwordUser = isPasswordUser()
        val googleUser = isGoogleUser()

        when {
            passwordUser && googleUser -> {
                changePasswordBtn.visibility = View.VISIBLE
                changeEmailBtn.visibility = View.VISIBLE
                tvSecurityHint.text = "This account uses both password and Google sign-in."
                tvDeleteAccountHint.text = "Password or Google confirmation may be required before deletion."
            }

            passwordUser -> {
                changePasswordBtn.visibility = View.VISIBLE
                changeEmailBtn.visibility = View.VISIBLE
                tvSecurityHint.text = "Manage your email, password, and account access."
                tvDeleteAccountHint.text = "Enter your password to confirm account deletion."
            }

            googleUser -> {
                changePasswordBtn.visibility = View.GONE
                changeEmailBtn.visibility = View.VISIBLE
                tvSecurityHint.text = "This account uses Google Sign-In."
                tvDeleteAccountHint.text = "You’ll need to continue with Google to confirm deletion."
            }

            else -> {
                changePasswordBtn.visibility = View.GONE
                changeEmailBtn.visibility = View.VISIBLE
                tvSecurityHint.text = "Manage your account settings."
                tvDeleteAccountHint.text = "You may need to confirm your identity before deletion."
            }
        }
    }

    private fun clearSettingsPrefs() {
        getSharedPreferences("settings", MODE_PRIVATE)
            .edit()
            .remove("pendingEmailChange")
            .remove("oldEmail")
            .remove("newEmail")
            .remove("relinkGoogleAfterEmailChange")
            .apply()
    }

    private fun returnToMainAfterSecurityChange(message: String) {
        googleSignInClient.signOut().addOnCompleteListener {
            oneTapClient.signOut().addOnCompleteListener {
                auth.signOut()

                Toast.makeText(this, message, Toast.LENGTH_LONG).show()

                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                overridePendingTransition(0, 0)
                finish()
            }
        }
    }

    private fun checkPendingEmailChange() {
        val user = auth.currentUser ?: return
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        val pendingEmailChange = prefs.getBoolean("pendingEmailChange", false)
        val oldEmail = prefs.getString("oldEmail", null)
        val newEmailFromPrefs = prefs.getString("newEmail", null)?.trim()?.lowercase()
        val relinkGoogle = prefs.getBoolean("relinkGoogleAfterEmailChange", false)

        if (!pendingEmailChange || oldEmail.isNullOrEmpty()) return

        user.reload().addOnSuccessListener {
            val updatedUser = auth.currentUser ?: return@addOnSuccessListener
            val newEmail = updatedUser.email?.trim()?.lowercase() ?: return@addOnSuccessListener
            val oldEmailClean = oldEmail.trim().lowercase()

            if (newEmail != oldEmailClean) {
                db.collection("users")
                    .document(updatedUser.uid)
                    .set(mapOf("email" to newEmail), SetOptions.merge())
                    .addOnCompleteListener {
                        if (relinkGoogle && !newEmailFromPrefs.isNullOrEmpty()) {
                            promptGoogleRelinkAfterEmailChange(newEmailFromPrefs)
                        } else {
                            clearSettingsPrefs()

                            NotificationHelper.showNotification(
                                context = this,
                                title = "Email Updated 📧",
                                message = "Your email has been successfully changed to $newEmail.",
                                type = "security",
                                target = "profile",
                                saveToDb = true
                            )

                            returnToMainAfterSecurityChange("Email changed successfully. Please login again.")
                        }
                    }
            }
        }
    }

    private fun promptGoogleRelinkAfterEmailChange(expectedEmail: String) {
        AlertDialog.Builder(this)
            .setTitle("Relink Google Sign-In")
            .setMessage("To keep Google sign-in working on the same account, continue with your new Google email:\n\n$expectedEmail")
            .setCancelable(false)
            .setPositiveButton("Continue") { _, _ ->
                startGoogleRelinkFlow(expectedEmail)
            }
            .setNegativeButton("Skip") { _, _ ->
                clearSettingsPrefs()
                returnToMainAfterSecurityChange("Email changed successfully. Please login again.")
            }
            .show()
    }

    private fun startGoogleRelinkFlow(expectedEmail: String) {
        pendingExpectedGoogleEmail = expectedEmail.trim().lowercase()

        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_LINK_GOOGLE_AFTER_EMAIL_CHANGE)
            overridePendingTransition(0, 0)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_LINK_GOOGLE_AFTER_EMAIL_CHANGE) {
            if (resultCode != Activity.RESULT_OK) {
                pendingExpectedGoogleEmail = null
                Toast.makeText(this, "Google relink cancelled", Toast.LENGTH_SHORT).show()
                return
            }

            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                    .getResult(ApiException::class.java)

                val chosenEmail = account.email?.trim()?.lowercase()
                val idToken = account.idToken
                val expectedEmail = pendingExpectedGoogleEmail?.trim()?.lowercase()
                val currentUser = auth.currentUser

                if (currentUser == null) {
                    pendingExpectedGoogleEmail = null
                    Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                    return
                }

                if (chosenEmail.isNullOrEmpty() || idToken.isNullOrEmpty()) {
                    pendingExpectedGoogleEmail = null
                    Toast.makeText(this, "Failed to get Google account", Toast.LENGTH_SHORT).show()
                    return
                }

                if (expectedEmail.isNullOrEmpty()) {
                    pendingExpectedGoogleEmail = null
                    Toast.makeText(this, "Missing expected email", Toast.LENGTH_SHORT).show()
                    return
                }

                if (chosenEmail != expectedEmail) {
                    googleSignInClient.signOut()
                    pendingExpectedGoogleEmail = null
                    Toast.makeText(
                        this,
                        "Please choose the exact new Google email: $expectedEmail",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                val credential = GoogleAuthProvider.getCredential(idToken, null)

                val hasOldGoogleProvider = currentUser.providerData.any {
                    it.providerId == GoogleAuthProvider.PROVIDER_ID
                }

                if (hasOldGoogleProvider) {
                    currentUser.unlink(GoogleAuthProvider.PROVIDER_ID)
                        .addOnSuccessListener {
                            linkNewGoogleCredentialToSameUser(currentUser.uid, credential, expectedEmail)
                        }
                        .addOnFailureListener { e ->
                            pendingExpectedGoogleEmail = null
                            Toast.makeText(
                                this,
                                e.message ?: "Failed to remove old Google link",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                } else {
                    linkNewGoogleCredentialToSameUser(currentUser.uid, credential, expectedEmail)
                }

            } catch (e: Exception) {
                pendingExpectedGoogleEmail = null
                Toast.makeText(
                    this,
                    e.message ?: "Google relink failed",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun linkNewGoogleCredentialToSameUser(
        uid: String,
        credential: com.google.firebase.auth.AuthCredential,
        expectedEmail: String
    ) {
        val currentUser = auth.currentUser ?: run {
            pendingExpectedGoogleEmail = null
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            return
        }

        currentUser.linkWithCredential(credential)
            .addOnSuccessListener {
                db.collection("users")
                    .document(uid)
                    .set(
                        mapOf(
                            "email" to expectedEmail,
                            "googleLinkedEmail" to expectedEmail
                        ),
                        SetOptions.merge()
                    )
                    .addOnSuccessListener {
                        clearSettingsPrefs()
                        pendingExpectedGoogleEmail = null

                        NotificationHelper.showNotification(
                            context = this,
                            title = "Email Updated 📧",
                            message = "Your email has been successfully changed to $expectedEmail.",
                            type = "security",
                            target = "profile",
                            saveToDb = true
                        )

                        returnToMainAfterSecurityChange(
                            "Email changed successfully and Google sign-in was relinked."
                        )
                    }
                    .addOnFailureListener { e ->
                        pendingExpectedGoogleEmail = null
                        Toast.makeText(
                            this,
                            e.message ?: "Failed to sync email data",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                pendingExpectedGoogleEmail = null
                Toast.makeText(
                    this,
                    e.message ?: "Failed to link new Google account",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun loadReminderSettings() {
        val userId = auth.currentUser?.uid ?: return

        isLoadingReminderState = true

        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                val workoutEnabled = doc.getBoolean("workoutReminderEnabled") ?: true
                val mealEnabled = doc.getBoolean("mealReminderEnabled") ?: true

                switchWorkout.isChecked = workoutEnabled
                switchMeal.isChecked = mealEnabled

                isLoadingReminderState = false
            }
            .addOnFailureListener {
                switchWorkout.isChecked = true
                switchMeal.isChecked = true
                isLoadingReminderState = false
            }
    }

    private fun setupReminderSwitches() {
        switchWorkout.setOnCheckedChangeListener { _, isChecked ->
            if (isLoadingReminderState) return@setOnCheckedChangeListener

            saveReminderSetting("workoutReminderEnabled", isChecked)

            Toast.makeText(
                this,
                if (isChecked) "Workout reminder ON" else "Workout reminder OFF",
                Toast.LENGTH_SHORT
            ).show()
        }

        switchMeal.setOnCheckedChangeListener { _, isChecked ->
            if (isLoadingReminderState) return@setOnCheckedChangeListener

            saveReminderSetting("mealReminderEnabled", isChecked)

            Toast.makeText(
                this,
                if (isChecked) "Meal reminder ON" else "Meal reminder OFF",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun saveReminderSetting(field: String, value: Boolean) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(userId)
            .update(field, value)
            .addOnFailureListener {
                db.collection("users")
                    .document(userId)
                    .set(mapOf(field to value), SetOptions.merge())
            }
    }

    private fun createPasswordField(hintText: String): EditText {
        return EditText(this).apply {
            hint = hintText
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            transformationMethod = PasswordTransformationMethod.getInstance()
        }
    }

    private fun setPasswordVisible(editText: EditText, visible: Boolean) {
        editText.transformationMethod =
            if (visible) HideReturnsTransformationMethod.getInstance()
            else PasswordTransformationMethod.getInstance()
        editText.setSelection(editText.text.length)
    }

    private fun showChangeEmailDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 10)
        }

        val newEmail = EditText(this).apply {
            hint = "New Email"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }

        val password = createPasswordField("Current Password")

        val showPassword = CheckBox(this).apply {
            text = "Show password"
            setOnCheckedChangeListener { _, isChecked ->
                setPasswordVisible(password, isChecked)
            }
        }

        layout.addView(newEmail)
        layout.addView(password)
        layout.addView(showPassword)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Change Email")
            .setMessage("A verification link will be sent to your new email.")
            .setView(layout)
            .setPositiveButton("Update", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val newEmailText = newEmail.text.toString().trim().lowercase()
            val passwordText = password.text.toString().trim()

            if (newEmailText.isEmpty() || passwordText.isEmpty()) {
                Toast.makeText(this, "Please complete all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = auth.currentUser
            val currentEmail = user?.email?.trim()?.lowercase()

            if (user == null || currentEmail.isNullOrEmpty()) {
                Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newEmailText == currentEmail) {
                Toast.makeText(this, "New email is the same as current email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val credential = EmailAuthProvider.getCredential(currentEmail, passwordText)

            user.reauthenticate(credential)
                .addOnSuccessListener {
                    user.verifyBeforeUpdateEmail(newEmailText)
                        .addOnSuccessListener {
                            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
                            prefs.edit()
                                .putBoolean("pendingEmailChange", true)
                                .putString("oldEmail", currentEmail)
                                .putString("newEmail", newEmailText)
                                .putBoolean("relinkGoogleAfterEmailChange", true)
                                .apply()

                            NotificationHelper.showNotification(
                                context = this,
                                title = "Email Change Requested 📩",
                                message = "We sent a verification link to $newEmailText. Verify it to finish updating your email.",
                                type = "security",
                                target = "profile",
                                saveToDb = true
                            )

                            Toast.makeText(
                                this,
                                "Verification sent. Check your new email, then come back to finish relinking Google.",
                                Toast.LENGTH_LONG
                            ).show()

                            dialog.dismiss()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                e.message ?: "Failed to send verification email",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Wrong password", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showChangePasswordDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 10)
        }

        val currentPass = createPasswordField("Current Password")
        val newPass = createPasswordField("New Password")

        val showPasswords = CheckBox(this).apply {
            text = "Show passwords"
            setOnCheckedChangeListener { _, isChecked ->
                setPasswordVisible(currentPass, isChecked)
                setPasswordVisible(newPass, isChecked)
            }
        }

        layout.addView(currentPass)
        layout.addView(newPass)
        layout.addView(showPasswords)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(layout)
            .setPositiveButton("Update", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val currentPassText = currentPass.text.toString().trim()
            val newPassText = newPass.text.toString().trim()

            if (currentPassText.isEmpty() || newPassText.isEmpty()) {
                Toast.makeText(this, "Please complete all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassText.length < 6) {
                Toast.makeText(this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = auth.currentUser
            val email = user?.email

            if (user == null || email.isNullOrEmpty()) {
                Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val credential = EmailAuthProvider.getCredential(email, currentPassText)

            user.reauthenticate(credential)
                .addOnSuccessListener {
                    user.updatePassword(newPassText)
                        .addOnSuccessListener {
                            NotificationHelper.showNotification(
                                context = this,
                                title = "Password Updated 🔐",
                                message = "Your password was successfully changed.",
                                type = "security",
                                target = "profile",
                                saveToDb = true
                            )

                            Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                e.message ?: "Failed to update password",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Wrong password", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showDeleteDialog() {
        when {
            isPasswordUser() -> showPasswordDeleteDialog()
            isGoogleUser() -> showGoogleDeleteDialog()
            else -> {
                Toast.makeText(
                    this,
                    "Unable to determine account sign-in method",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showPasswordDeleteDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 10)
        }

        val passwordInput = createPasswordField("Enter Password")

        val showPassword = CheckBox(this).apply {
            text = "Show password"
            setOnCheckedChangeListener { _, isChecked ->
                setPasswordVisible(passwordInput, isChecked)
            }
        }

        layout.addView(passwordInput)
        layout.addView(showPassword)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("This action cannot be undone.")
            .setView(layout)
            .setPositiveButton("Delete", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val user = auth.currentUser
            val email = user?.email
            val password = passwordInput.text.toString().trim()

            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (user == null || email.isNullOrEmpty()) {
                Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val credential = EmailAuthProvider.getCredential(email, password)

            user.reauthenticate(credential)
                .addOnSuccessListener {
                    dialog.dismiss()
                    deleteCurrentUserAccount()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Wrong password", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showGoogleDeleteDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("This account uses Google Sign-In. Continue with Google to confirm account deletion.")
            .setPositiveButton("Continue", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            pendingDeleteAfterGoogleReauth = true
            dialog.dismiss()
            beginGoogleReauthentication()
        }
    }

    private fun beginGoogleReauthentication() {
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener { result ->
                googleReauthLauncher.launch(
                    IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                )
            }
            .addOnFailureListener { e ->
                pendingDeleteAfterGoogleReauth = false
                Toast.makeText(
                    this,
                    e.message ?: "Unable to start Google confirmation",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun deleteCurrentUserAccount() {
        val user = auth.currentUser ?: run {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            pendingDeleteAfterGoogleReauth = false
            return
        }

        val userId = user.uid

        user.delete()
            .addOnSuccessListener {
                cleanupUserFirestoreData(userId)
                clearSettingsPrefs()

                NotificationHelper.showNotification(
                    context = this,
                    title = "Account Deleted",
                    message = "Your Carabuff account has been deleted successfully.",
                    type = "security",
                    target = "profile",
                    saveToDb = false
                )

                googleSignInClient.signOut().addOnCompleteListener {
                    oneTapClient.signOut().addOnCompleteListener {
                        auth.signOut()

                        Toast.makeText(
                            this,
                            "Account deleted successfully. You can use the same email to sign up again.",
                            Toast.LENGTH_LONG
                        ).show()

                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        overridePendingTransition(0, 0)
                        finish()
                    }
                }
            }
            .addOnFailureListener { e ->
                pendingDeleteAfterGoogleReauth = false
                Toast.makeText(
                    this,
                    e.message ?: "Failed to delete account",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun cleanupUserFirestoreData(userId: String) {
        db.collection("users").document(userId).delete()
        db.collection("user_profiles").document(userId).delete()
    }
}