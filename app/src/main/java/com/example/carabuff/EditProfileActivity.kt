package com.example.carabuff

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.*

class EditProfileActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etBirthday: EditText
    private lateinit var etAge: EditText
    private lateinit var etWeight: EditText
    private lateinit var etHeight: EditText
    private lateinit var btnSave: Button
    private lateinit var btnBack: ImageView // ✅ FIXED (ImageView na)

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // 🔥 BIND VIEWS
        etName = findViewById(R.id.etName)
        etBirthday = findViewById(R.id.etBirthday)
        etAge = findViewById(R.id.etAge)
        etWeight = findViewById(R.id.etWeight)
        etHeight = findViewById(R.id.etHeight)
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)

        loadProfile()

        // 🔥 BACK BUTTON
        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // 🔥 DATE PICKER
        etBirthday.setOnClickListener {
            showDatePicker()
        }

        // 🔥 SAVE BUTTON
        btnSave.setOnClickListener {
            saveProfile()
        }
    }

    // ✅ SAFE LOAD (NO CRASH)
    private fun loadProfile() {
        userId?.let {
            db.collection("users")
                .document(it)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        etName.setText(doc.getString("name") ?: "")
                        etBirthday.setText(doc.getString("birthday") ?: "")
                        etAge.setText(doc.get("age")?.toString() ?: "")
                        etWeight.setText(doc.get("weight")?.toString() ?: "")
                        etHeight.setText(doc.get("height")?.toString() ?: "")
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // 📅 DATE PICKER
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        val dialog = DatePickerDialog(
            this,
            { _, year, month, day ->

                val formatted = "${month + 1}/$day/$year"
                etBirthday.setText(formatted)

                val age = calculateAge(year, month, day)
                etAge.setText(age.toString())
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        dialog.show()
    }

    // 🎯 AGE CALCULATION
    private fun calculateAge(year: Int, month: Int, day: Int): Int {
        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - year

        if (today.get(Calendar.MONTH) < month ||
            (today.get(Calendar.MONTH) == month && today.get(Calendar.DAY_OF_MONTH) < day)
        ) {
            age--
        }

        return age
    }

    // 💾 SAVE PROFILE
    private fun saveProfile() {

        val name = etName.text.toString().trim()
        val birthday = etBirthday.text.toString().trim()

        val ageValue = etAge.text.toString().toIntOrNull()
        val weightValue = etWeight.text.toString().toDoubleOrNull()
        val heightValue = etHeight.text.toString().toIntOrNull()

        // 🔥 BASIC VALIDATION
        if (name.isEmpty()) {
            etName.error = "Required"
            return
        }

        if (birthday.isEmpty()) {
            etBirthday.error = "Required"
            return
        }

        val updates = hashMapOf(
            "name" to name,
            "birthday" to birthday,
            "age" to ageValue,
            "weight" to weightValue,
            "height" to heightValue
        )

        userId?.let {
            db.collection("users")
                .document(it)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Update Failed!", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }
}