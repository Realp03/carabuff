package com.example.carabuff

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var profileImage: ImageView

    private lateinit var logoutBtn: LinearLayout
    private lateinit var editProfileBtn: LinearLayout   // 🔥 FIXED (use layout not textview)
    private lateinit var settingsBtn: LinearLayout
    private lateinit var aboutBtn: LinearLayout
    private lateinit var profileName: TextView

    // 🔥 NAVBAR
    private lateinit var navHome: ImageView
    private lateinit var navAnalytics: ImageView
    private lateinit var navNotif: ImageView
    private lateinit var navProfile: ImageView

    private var imageUri: Uri? = null

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { showImagePreviewDialog(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()

        Log.d("PROFILE_DEBUG", "USER ID: ${auth.currentUser?.uid}")

        // 🔥 Bind views
        profileImage = findViewById(R.id.profileImage)
        logoutBtn = findViewById(R.id.logoutBtn)
        editProfileBtn = findViewById(R.id.editProfileBtn) // ✅ FIXED
        settingsBtn = findViewById(R.id.settingsBtn)
        aboutBtn = findViewById(R.id.aboutBtn)
        profileName = findViewById(R.id.profileName)

        // 🔥 NAVBAR
        navHome = findViewById(R.id.navHome)
        navAnalytics = findViewById(R.id.navAnalytics)
        navNotif = findViewById(R.id.navNotif)
        navProfile = findViewById(R.id.navProfile)

        loadProfileData()
        loadProfileImage()

        // 📷 pick image
        profileImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        // ✏️ EDIT PROFILE
        editProfileBtn.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        // ⚙️ SETTINGS
        settingsBtn.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // ℹ️ ABOUT
        aboutBtn.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        // 🚪 LOGOUT
        logoutBtn.setOnClickListener {
            showLogoutDialog()
        }

        setupBottomNav()
    }

    override fun onResume() {
        super.onResume()
        loadProfileData()
        loadProfileImage()
    }

    private fun setupBottomNav() {

        navHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        navAnalytics.setOnClickListener {
            startActivity(Intent(this, AnalyticsActivity::class.java))
            finish()
        }

        navNotif.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
            finish()
        }

        navProfile.alpha = 1f
    }

    private fun loadProfileData() {
        val userId = auth.currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name")

                profileName.text = if (!name.isNullOrEmpty()) {
                    name
                } else {
                    auth.currentUser?.email ?: "User"
                }
            }
    }

    private fun showImagePreviewDialog(uri: Uri) {

        val dialogView = layoutInflater.inflate(R.layout.image_confirm_dialog, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        val preview = dialogView.findViewById<ImageView>(R.id.previewImage)
        val btnConfirm = dialogView.findViewById<TextView>(R.id.btnConfirm)
        val btnChange = dialogView.findViewById<TextView>(R.id.btnChange)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancel)

        preview.setImageURI(uri)

        btnConfirm.setOnClickListener {
            imageUri = uri
            profileImage.setImageURI(uri)

            Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show()
            uploadImageToFirebase()

            dialog.dismiss()
        }

        btnChange.setOnClickListener {
            dialog.dismiss()
            pickImage.launch("image/*")
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun uploadImageToFirebase() {

        val userId = auth.currentUser?.uid ?: return
        profileImage.alpha = 0.5f

        val storageRef = FirebaseStorage.getInstance()
            .reference.child("profile_images/$userId.jpg")

        imageUri?.let { uri ->
            storageRef.putFile(uri)
                .addOnSuccessListener {

                    storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->

                        val url = downloadUrl.toString()

                        FirebaseFirestore.getInstance()
                            .collection("user_profiles")
                            .document(userId)
                            .set(mapOf("profileImage" to url), SetOptions.merge())

                        profileImage.alpha = 1f
                        Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Upload Failed", Toast.LENGTH_SHORT).show()
                    profileImage.alpha = 1f
                }
        }
    }

    private fun loadProfileImage() {

        val userId = auth.currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("user_profiles")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->

                val imageUrl = doc.getString("profileImage")

                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(imageUrl)
                        .circleCrop()
                        .into(profileImage)
                }
            }
    }

    private fun showLogoutDialog() {

        val dialogView = layoutInflater.inflate(R.layout.logout_dialog, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        val btnYes = dialogView.findViewById<TextView>(R.id.btnYes)
        val btnNo = dialogView.findViewById<TextView>(R.id.btnNo)

        btnYes.setOnClickListener {
            auth.signOut()

            val intent = Intent(this, MainActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            dialog.dismiss()
        }

        btnNo.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}