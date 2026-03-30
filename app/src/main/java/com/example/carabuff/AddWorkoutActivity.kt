package com.example.carabuff

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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
        val btnSave = findViewById<Button>(R.id.btnSaveWorkout)
        val btnAddFavorite = findViewById<Button>(R.id.btnAddFavorite)
        val btnBack = findViewById<TextView>(R.id.btnBack)

        favoritesContainer = findViewById(R.id.favoritesContainer)
        spinnerFilter = findViewById(R.id.spinnerFilter)

        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        btnBack.setOnClickListener { finish() }

        val types = arrayOf("Cardio", "Weight Lifting", "Bodyweight")
        spinnerType.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)

        val intensities = arrayOf("Light", "Moderate", "Intense")
        spinnerIntensity.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, intensities)

        val filters = arrayOf("All", "Cardio", "Weight Lifting", "Bodyweight")
        spinnerFilter.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, filters)

        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                displayFavorites(filters[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (types[position] == "Cardio") {
                    etSets.visibility = View.GONE
                    etReps.visibility = View.GONE
                } else {
                    etSets.visibility = View.VISIBLE
                    etReps.visibility = View.VISIBLE
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        loadFavorites()

        // SAVE WORKOUT
        btnSave.setOnClickListener {

            val type = spinnerType.selectedItem.toString()
            val intensity = spinnerIntensity.selectedItem.toString()
            val name = etExercise.text.toString().trim()
            val minutes = etMinutes.text.toString().toIntOrNull()
            val sets = etSets.text.toString().toIntOrNull()
            val reps = etReps.text.toString().toIntOrNull()

            if (name.isEmpty()) {
                Toast.makeText(this, "Enter exercise name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (minutes == null || minutes <= 0) {
                Toast.makeText(this, "Enter valid minutes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveWorkout(type, name, minutes, sets, reps, intensity)
        }

        // ADD FAVORITE (🔥 FIXED)
        btnAddFavorite.setOnClickListener {

            val type = spinnerType.selectedItem.toString()
            val intensity = spinnerIntensity.selectedItem.toString() // 🔥 NEW
            val name = etExercise.text.toString().trim()
            val minutes = etMinutes.text.toString().toIntOrNull() ?: 15
            val sets = etSets.text.toString().toIntOrNull()
            val reps = etReps.text.toString().toIntOrNull()

            if (name.isEmpty()) {
                Toast.makeText(this, "Enter exercise name first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (userId == null) return@setOnClickListener

            val data = hashMapOf<String, Any>(
                "type" to type,
                "name" to name,
                "minutes" to minutes,
                "intensity" to intensity // 🔥 NEW
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
        }
    }

    // MET CALCULATION
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
                displayFavorites("All")
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
            val intensity = item["intensity"] as? String ?: "Moderate" // 🔥 FIX
            val id = item["id"] as String

            val container = LinearLayout(this)
            container.orientation = LinearLayout.VERTICAL
            container.setPadding(16, 16, 16, 16)

            val title = TextView(this)
            title.text = "$name ($type)"
            title.setTextColor(resources.getColor(android.R.color.white))

            val details = TextView(this)
            details.text = "Time: $minutes mins | Intensity: $intensity"
            details.visibility = View.GONE

            val btnLayout = LinearLayout(this)
            btnLayout.orientation = LinearLayout.HORIZONTAL
            btnLayout.visibility = View.GONE

            val btnSaveFav = Button(this)
            btnSaveFav.text = "Save"
            btnSaveFav.setOnClickListener {
                saveWorkout(type, name, minutes, sets, reps, intensity)
            }

            val btnDelete = Button(this)
            btnDelete.text = "Delete"
            btnDelete.setOnClickListener {
                db.collection("users")
                    .document(userId)
                    .collection("favorites")
                    .document(id)
                    .delete()
                    .addOnSuccessListener { loadFavorites() }
            }

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
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val userWeight = 60.0

        val caloriesBurned = calculateCalories(name, minutes, userWeight, intensity)

        val userRef = db.collection("users").document(userId)

        userRef.update("plan.workoutDone", FieldValue.increment(minutes.toLong()))

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
                Toast.makeText(this, "🔥 $caloriesBurned kcal burned!", Toast.LENGTH_SHORT).show()
                finish()
            }
    }
}