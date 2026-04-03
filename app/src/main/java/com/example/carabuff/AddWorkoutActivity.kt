package com.example.carabuff

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AddWorkoutActivity : AppCompatActivity() {

    private lateinit var favoritesContainer: LinearLayout
    private lateinit var spinnerFilter: Spinner

    private val allFavorites = mutableListOf<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_workout)

        val spinnerType = findViewById<Spinner>(R.id.spinnerType)
        val spinnerIntensity = findViewById<Spinner>(R.id.spinnerIntensity)
        val etExercise = findViewById<EditText>(R.id.etExercise)
        val etMinutes = findViewById<EditText>(R.id.etMinutes)
        val etSets = findViewById<EditText>(R.id.etSets)
        val etReps = findViewById<EditText>(R.id.etReps)
        val btnSave = findViewById<AppCompatButton>(R.id.btnSaveWorkout)
        val btnAddFavorite = findViewById<AppCompatButton>(R.id.btnAddFavorite)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        favoritesContainer = findViewById(R.id.favoritesContainer)
        spinnerFilter = findViewById(R.id.spinnerFilter)

        val db = FirebaseFirestore.getInstance()

        btnBack.setOnClickListener { finish() }

        val types = arrayOf("Cardio", "Weight Lifting", "Bodyweight")
        val intensities = arrayOf("Light", "Moderate", "Intense")
        val filters = arrayOf("All", "Cardio", "Weight Lifting", "Bodyweight")

        val typeAdapter = ArrayAdapter(this, R.layout.spinner_item_dark, types)
        typeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark)
        spinnerType.adapter = typeAdapter

        val intensityAdapter = ArrayAdapter(this, R.layout.spinner_item_dark, intensities)
        intensityAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark)
        spinnerIntensity.adapter = intensityAdapter

        val filterAdapter = ArrayAdapter(this, R.layout.spinner_item_dark, filters)
        filterAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark)
        spinnerFilter.adapter = filterAdapter

        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                displayFavorites(filters[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val isCardio = types[position] == "Cardio"
                etSets.visibility = if (isCardio) View.GONE else View.VISIBLE
                etReps.visibility = if (isCardio) View.GONE else View.VISIBLE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        loadFavorites()

        btnSave.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val type = spinnerType.selectedItem?.toString() ?: "Cardio"
            val intensity = spinnerIntensity.selectedItem?.toString() ?: "Moderate"
            val name = etExercise.text.toString().trim()
            val minutes = etMinutes.text.toString().trim().toIntOrNull()
            val sets = etSets.text.toString().trim().toIntOrNull()
            val reps = etReps.text.toString().trim().toIntOrNull()

            if (name.isEmpty()) {
                Toast.makeText(this, "Enter exercise name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (minutes == null || minutes <= 0) {
                Toast.makeText(this, "Enter valid minutes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (userId == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveWorkout(type, name, minutes, sets, reps, intensity)
        }

        btnAddFavorite.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val type = spinnerType.selectedItem?.toString() ?: "Cardio"
            val intensity = spinnerIntensity.selectedItem?.toString() ?: "Moderate"
            val name = etExercise.text.toString().trim()
            val minutes = etMinutes.text.toString().trim().toIntOrNull() ?: 15
            val sets = etSets.text.toString().trim().toIntOrNull()
            val reps = etReps.text.toString().trim().toIntOrNull()

            if (name.isEmpty()) {
                Toast.makeText(this, "Enter exercise name first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (userId == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val data = hashMapOf<String, Any>(
                "type" to type,
                "name" to name,
                "minutes" to minutes,
                "intensity" to intensity
            )

            if (type != "Cardio") {
                if (sets != null) data["sets"] = sets
                if (reps != null) data["reps"] = reps
            }

            db.collection("users")
                .document(userId)
                .collection("favorites")
                .add(data)
                .addOnSuccessListener {
                    Toast.makeText(this, "Added to favorites ⭐", Toast.LENGTH_SHORT).show()
                    loadFavorites()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to add favorite", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun calculateCalories(
        name: String,
        minutes: Int,
        weight: Double,
        intensity: String
    ): Int {
        val baseMet = when {
            name.contains("jump rope", true) -> 11.0
            name.contains("running", true) -> 10.0
            name.contains("jogging", true) -> 7.0
            name.contains("walking", true) -> 3.5
            name.contains("cycling", true) -> 6.0
            name.contains("push", true) -> 4.0
            name.contains("squat", true) -> 5.0
            else -> 5.0
        }

        val multiplier = when (intensity) {
            "Light" -> 0.8
            "Moderate" -> 1.0
            "Intense" -> 1.2
            else -> 1.0
        }

        val met = baseMet * multiplier
        val hours = minutes / 60.0

        return (met * weight * hours).toInt()
    }

    private fun loadFavorites() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        allFavorites.clear()

        db.collection("users")
            .document(userId)
            .collection("favorites")
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val data = doc.data.toMutableMap()
                    data["id"] = doc.id
                    allFavorites.add(data)
                }
                displayFavorites(spinnerFilter.selectedItem?.toString() ?: "All")
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load favorites", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayFavorites(filter: String) {
        favoritesContainer.removeAllViews()

        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        for (item in allFavorites) {
            val type = item["type"] as? String ?: continue
            if (filter != "All" && type != filter) continue

            val name = item["name"] as? String ?: continue
            val minutes = (item["minutes"] as? Long)?.toInt() ?: 15
            val sets = (item["sets"] as? Long)?.toInt()
            val reps = (item["reps"] as? Long)?.toInt()
            val intensity = item["intensity"] as? String ?: "Moderate"
            val id = item["id"] as? String ?: continue

            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 20, 20, 20)
                background = ContextCompat.getDrawable(this@AddWorkoutActivity, R.drawable.bg_signup_card)

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.bottomMargin = 12
                layoutParams = params
            }

            val title = TextView(this).apply {
                text = "$name ($type)"
                textSize = 16f
                setTextColor(ContextCompat.getColor(this@AddWorkoutActivity, android.R.color.white))
                typeface = resources.getFont(R.font.iceland_regular)
            }

            val details = TextView(this).apply {
                text = buildString {
                    append("Time: $minutes mins")
                    append(" | Intensity: $intensity")
                    if (type != "Cardio") {
                        if (sets != null) append(" | Sets: $sets")
                        if (reps != null) append(" | Reps: $reps")
                    }
                }
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@AddWorkoutActivity, android.R.color.white))
                alpha = 0.75f
                visibility = View.GONE
                setPadding(0, 12, 0, 12)
                typeface = resources.getFont(R.font.iceland_regular)
            }

            val btnLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                visibility = View.GONE
            }

            val btnSaveFav = AppCompatButton(this).apply {
                text = "Save"
                background = ContextCompat.getDrawable(this@AddWorkoutActivity, R.drawable.bg_signup_button)
                setTextColor(ContextCompat.getColor(this@AddWorkoutActivity, android.R.color.white))
                typeface = resources.getFont(R.font.iceland_regular)
                textSize = 15f
                setOnClickListener {
                    saveWorkout(type, name, minutes, sets, reps, intensity)
                }
            }

            val saveParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            saveParams.marginEnd = 8
            btnSaveFav.layoutParams = saveParams

            val btnDelete = AppCompatButton(this).apply {
                text = "Delete"
                background = ContextCompat.getDrawable(this@AddWorkoutActivity, R.drawable.bg_signup_button)
                setTextColor(ContextCompat.getColor(this@AddWorkoutActivity, android.R.color.white))
                typeface = resources.getFont(R.font.iceland_regular)
                textSize = 15f
                setOnClickListener {
                    db.collection("users")
                        .document(userId)
                        .collection("favorites")
                        .document(id)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(this@AddWorkoutActivity, "Deleted", Toast.LENGTH_SHORT).show()
                            loadFavorites()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this@AddWorkoutActivity, "Failed to delete", Toast.LENGTH_SHORT).show()
                        }
                }
            }

            val deleteParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            btnDelete.layoutParams = deleteParams

            btnLayout.addView(btnSaveFav)
            btnLayout.addView(btnDelete)

            title.setOnClickListener {
                val expanded = details.visibility == View.VISIBLE
                details.visibility = if (expanded) View.GONE else View.VISIBLE
                btnLayout.visibility = if (expanded) View.GONE else View.VISIBLE
            }

            container.addView(title)
            container.addView(details)
            container.addView(btnLayout)

            favoritesContainer.addView(container)
        }
    }

    private fun saveWorkout(
        type: String,
        name: String,
        minutes: Int,
        sets: Int?,
        reps: Int?,
        intensity: String
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val userWeight = 60.0
        val caloriesBurned = calculateCalories(name, minutes, userWeight, intensity)

        val userRef = db.collection("users").document(userId)

        val data = hashMapOf<String, Any>(
            "type" to type,
            "name" to name,
            "minutes" to minutes,
            "intensity" to intensity,
            "caloriesBurned" to caloriesBurned,
            "timestamp" to System.currentTimeMillis()
        )

        if (type != "Cardio") {
            if (sets != null) data["sets"] = sets
            if (reps != null) data["reps"] = reps
        }

        userRef.collection("workouts")
            .add(data)
            .addOnSuccessListener {
                userRef.update("plan.workoutDone", FieldValue.increment(minutes.toLong()))
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "🔥 $caloriesBurned kcal burned!",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this,
                            "Workout saved, but failed to update plan",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save workout", Toast.LENGTH_SHORT).show()
            }
    }
}