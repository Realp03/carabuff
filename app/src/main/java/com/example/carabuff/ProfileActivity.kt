package com.example.carabuff

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
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
    private lateinit var editProfileBtn: LinearLayout
    private lateinit var settingsBtn: LinearLayout
    private lateinit var aboutBtn: LinearLayout
    private lateinit var profileName: TextView

    private lateinit var navHomeContainer: LinearLayout
    private lateinit var navAnalyticsContainer: LinearLayout
    private lateinit var navNotifContainer: LinearLayout
    private lateinit var navProfileContainer: LinearLayout

    private lateinit var navHome: ImageView
    private lateinit var navAnalytics: ImageView
    private lateinit var navNotif: ImageView
    private lateinit var navProfile: ImageView

    private lateinit var navHomeLabel: TextView
    private lateinit var navAnalyticsLabel: TextView
    private lateinit var navNotifLabel: TextView
    private lateinit var navProfileLabel: TextView

    private lateinit var navNotifDot: View

    private var imageUri: Uri? = null
    private var hasAnimatedContent = false

    companion object {
        private const val ICON_ACTIVE_COLOR = "#111111"
        private const val ICON_INACTIVE_COLOR = "#B8C7D6"
        private const val LABEL_ACTIVE_COLOR = "#111111"
        private const val LABEL_INACTIVE_COLOR = "#8FA3B8"

        private const val NAV_ACTIVE_ALPHA_START = 0.60f
        private const val NAV_INACTIVE_ALPHA_START = 0.82f
        private const val NAV_ACTIVE_DURATION = 180L
        private const val NAV_INACTIVE_DURATION = 130L

        private const val CONTENT_FADE_DURATION = 220L
    }

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

        profileImage = findViewById(R.id.profileImage)
        logoutBtn = findViewById(R.id.logoutBtn)
        editProfileBtn = findViewById(R.id.editProfileBtn)
        settingsBtn = findViewById(R.id.settingsBtn)
        aboutBtn = findViewById(R.id.aboutBtn)
        profileName = findViewById(R.id.profileName)

        navHomeContainer = findViewById(R.id.navHomeContainer)
        navAnalyticsContainer = findViewById(R.id.navAnalyticsContainer)
        navNotifContainer = findViewById(R.id.navNotifContainer)
        navProfileContainer = findViewById(R.id.navProfileContainer)

        navHome = findViewById(R.id.navHome)
        navAnalytics = findViewById(R.id.navAnalytics)
        navNotif = findViewById(R.id.navNotif)
        navProfile = findViewById(R.id.navProfile)

        navHomeLabel = findViewById(R.id.navHomeLabel)
        navAnalyticsLabel = findViewById(R.id.navAnalyticsLabel)
        navNotifLabel = findViewById(R.id.navNotifLabel)
        navProfileLabel = findViewById(R.id.navProfileLabel)

        navNotifDot = findViewById(R.id.navNotifDot)

        loadProfileData()
        loadProfileImage()
        updateNotificationDot()

        profileImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        editProfileBtn.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
            overridePendingTransition(0, 0)
        }

        settingsBtn.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            overridePendingTransition(0, 0)
        }

        aboutBtn.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
            overridePendingTransition(0, 0)
        }

        logoutBtn.setOnClickListener {
            showLogoutDialog()
        }

        setupBottomNav()
        prepareContentForFade()
    }

    override fun onResume() {
        super.onResume()
        loadProfileData()
        loadProfileImage()
        updateNotificationDot()
        setActiveNav("profile", animate = false)
        enableAllNavButtons()

        if (!hasAnimatedContent) {
            animatePageContentOnly()
            hasAnimatedContent = true
        }
    }

    private fun setupBottomNav() {
        setActiveNav("profile", animate = false)

        navHomeContainer.setOnClickListener {
            openTab("home", HomeActivity::class.java)
        }

        navAnalyticsContainer.setOnClickListener {
            openTab("analytics", AnalyticsActivity::class.java)
        }

        navNotifContainer.setOnClickListener {
            openTab("notif", NotificationActivity::class.java)
        }

        navProfileContainer.setOnClickListener {
            setActiveNav("profile", animate = true)
        }
    }

    private fun openTab(tab: String, target: Class<*>) {
        if (this::class.java == target) {
            setActiveNav(tab, animate = true)
            return
        }

        setActiveNav(tab, animate = true)
        disableAllNavButtons()

        val intent = Intent(this, target).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    private fun setActiveNav(tab: String, animate: Boolean = true) {
        resetNavItem(navHomeContainer, navHome, navHomeLabel, animate)
        resetNavItem(navAnalyticsContainer, navAnalytics, navAnalyticsLabel, animate)
        resetNavItem(navNotifContainer, navNotif, navNotifLabel, animate)
        resetNavItem(navProfileContainer, navProfile, navProfileLabel, animate)

        when (tab) {
            "home" -> activateNavItem(navHomeContainer, navHome, navHomeLabel, animate)
            "analytics" -> activateNavItem(navAnalyticsContainer, navAnalytics, navAnalyticsLabel, animate)
            "notif" -> activateNavItem(navNotifContainer, navNotif, navNotifLabel, animate)
            "profile" -> activateNavItem(navProfileContainer, navProfile, navProfileLabel, animate)
        }
    }

    private fun activateNavItem(
        container: LinearLayout,
        icon: ImageView,
        label: TextView,
        animate: Boolean
    ) {
        container.animate().cancel()
        icon.animate().cancel()
        label.animate().cancel()

        container.setBackgroundResource(R.drawable.bg_nav_item_active)
        icon.setColorFilter(Color.parseColor(ICON_ACTIVE_COLOR))
        label.setTextColor(Color.parseColor(LABEL_ACTIVE_COLOR))

        if (animate) {
            container.alpha = NAV_ACTIVE_ALPHA_START
            icon.alpha = NAV_ACTIVE_ALPHA_START
            label.alpha = NAV_ACTIVE_ALPHA_START

            container.animate().alpha(1f).setDuration(NAV_ACTIVE_DURATION).start()
            icon.animate().alpha(1f).setDuration(NAV_ACTIVE_DURATION).start()
            label.animate().alpha(1f).setDuration(NAV_ACTIVE_DURATION).start()
        } else {
            container.alpha = 1f
            icon.alpha = 1f
            label.alpha = 1f
        }
    }

    private fun resetNavItem(
        container: LinearLayout,
        icon: ImageView,
        label: TextView,
        animate: Boolean
    ) {
        container.animate().cancel()
        icon.animate().cancel()
        label.animate().cancel()

        container.setBackgroundResource(R.drawable.bg_nav_item_inactive)
        icon.setColorFilter(Color.parseColor(ICON_INACTIVE_COLOR))
        label.setTextColor(Color.parseColor(LABEL_INACTIVE_COLOR))

        if (animate) {
            container.alpha = NAV_INACTIVE_ALPHA_START
            icon.alpha = NAV_INACTIVE_ALPHA_START
            label.alpha = NAV_INACTIVE_ALPHA_START

            container.animate().alpha(1f).setDuration(NAV_INACTIVE_DURATION).start()
            icon.animate().alpha(1f).setDuration(NAV_INACTIVE_DURATION).start()
            label.animate().alpha(1f).setDuration(NAV_INACTIVE_DURATION).start()
        } else {
            container.alpha = 1f
            icon.alpha = 1f
            label.alpha = 1f
        }
    }

    private fun prepareContentForFade() {
        val contentViews = listOf<View>(
            profileImage,
            profileName,
            editProfileBtn,
            settingsBtn,
            aboutBtn,
            logoutBtn
        )

        contentViews.forEach {
            it.alpha = 0f
        }
    }

    private fun animatePageContentOnly() {
        val contentViews = listOf<View>(
            profileImage,
            profileName,
            editProfileBtn,
            settingsBtn,
            aboutBtn,
            logoutBtn
        )

        contentViews.forEachIndexed { index, view ->
            view.animate().cancel()
            view.animate()
                .alpha(1f)
                .setStartDelay(index * 20L)
                .setDuration(CONTENT_FADE_DURATION)
                .start()
        }
    }

    private fun disableAllNavButtons() {
        navHomeContainer.isEnabled = false
        navAnalyticsContainer.isEnabled = false
        navNotifContainer.isEnabled = false
        navProfileContainer.isEnabled = false
    }

    private fun enableAllNavButtons() {
        navHomeContainer.isEnabled = true
        navAnalyticsContainer.isEnabled = true
        navNotifContainer.isEnabled = true
        navProfileContainer.isEnabled = true
    }

    private fun updateNotificationDot() {
        val userId = auth.currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("notifications")
            .whereEqualTo("read", false)
            .get()
            .addOnSuccessListener { result ->
                navNotifDot.visibility = if (result.isEmpty) View.GONE else View.VISIBLE
            }
            .addOnFailureListener {
                navNotifDot.visibility = View.GONE
            }
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
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(0, 0)

            dialog.dismiss()
        }

        btnNo.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}