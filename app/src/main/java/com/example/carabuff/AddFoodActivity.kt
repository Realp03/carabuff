package com.example.carabuff

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.carabuff.api.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class AddFoodActivity : AppCompatActivity() {

    private lateinit var etFood: EditText
    private lateinit var btnSearchFood: Button
    private lateinit var rvFoods: RecyclerView
    private lateinit var tvResult: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnHelp: ImageView
    private lateinit var btnBack: TextView

    private var foodList = mutableListOf<FoodItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_food)

        etFood = findViewById(R.id.etFood)
        btnSearchFood = findViewById(R.id.btnSearchFood)
        rvFoods = findViewById(R.id.rvFoods)
        tvResult = findViewById(R.id.tvResult)
        progressBar = findViewById(R.id.progressBar)
        btnHelp = findViewById(R.id.btnHelp)
        btnBack = findViewById(R.id.btnBack)

        rvFoods.layoutManager = LinearLayoutManager(this)

        btnBack.setOnClickListener { finish() }

        btnSearchFood.setOnClickListener {
            val query = etFood.text.toString().trim()

            if (query.isEmpty()) {
                Toast.makeText(this, "Enter food", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (query.length < 2) {
                Toast.makeText(this, "Type at least 2 letters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSearchFood.isEnabled = false
            searchFoodFromApi(query)
        }

        btnHelp.setOnClickListener {
            showRulesDialog()
        }
    }

    private fun searchFoodFromApi(query: String) {

        progressBar.visibility = View.VISIBLE
        foodList.clear()

        val apiKey = "QVeIPMAStjyC65jSSKol0FCJ9zX6qQwjh0cefesb"

        RetrofitClient.instance.searchFood(apiKey, query)
            .enqueue(object : Callback<UsdaResponse> {

                override fun onResponse(call: Call<UsdaResponse>, response: Response<UsdaResponse>) {

                    progressBar.visibility = View.GONE
                    btnSearchFood.isEnabled = true

                    if (response.isSuccessful && response.body() != null) {

                        val foods = response.body()!!.foods

                        if (foods.isEmpty()) {
                            Toast.makeText(this@AddFoodActivity, "No food found", Toast.LENGTH_SHORT).show()
                            return
                        }

                        for (food in foods) {

                            var protein = 0.0
                            var carbs = 0.0
                            var fats = 0.0
                            var calories = 0

                            for (nutrient in food.foodNutrients) {
                                when (nutrient.nutrientName) {
                                    "Protein" -> protein = nutrient.value ?: 0.0
                                    "Carbohydrate, by difference" -> carbs = nutrient.value ?: 0.0
                                    "Total lipid (fat)" -> fats = nutrient.value ?: 0.0
                                    "Energy" -> calories = nutrient.value?.toInt() ?: 0
                                }
                            }

                            foodList.add(
                                FoodItem(
                                    name = food.description,
                                    calories = calories,
                                    protein = protein,
                                    carbs = carbs,
                                    fats = fats
                                )
                            )
                        }

                        updateRecycler()

                    } else {
                        Toast.makeText(this@AddFoodActivity, "Error Code: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<UsdaResponse>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    btnSearchFood.isEnabled = true
                    Toast.makeText(this@AddFoodActivity, "Connection failed", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateRecycler() {

        rvFoods.adapter = FoodAdapter(foodList) { selectedFood, quantity ->

            val mealType = getCurrentMealType() // 🔥 NEW

            saveFoodLog(
                selectedFood.name,
                selectedFood.calories,
                selectedFood.protein,
                selectedFood.carbs,
                selectedFood.fats,
                mealType // 🔥 NEW
            )
        }
    }

    // 🔥 UPDATED FUNCTION (ADD ONLY)
    private fun saveFoodLog(
        name: String,
        calories: Int,
        protein: Double,
        carbs: Double,
        fats: Double,
        mealType: String // 🔥 NEW
    ) {

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val data = mapOf(
            "name" to name,
            "calories" to calories,
            "protein" to protein,
            "carbs" to carbs,
            "fats" to fats,
            "mealType" to mealType, // 🔥 NEW
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(userId)
            .collection("foodLogs")
            .add(data)

        // 🔥 STOP REMINDER
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val prefs = getSharedPreferences("meal_reminder", MODE_PRIVATE)
        prefs.edit().putBoolean("${mealType}_$today", true).apply()

        Toast.makeText(this, "Food Added 🍗", Toast.LENGTH_SHORT).show()
    }

    // 🔥 NEW FUNCTION
    private fun getCurrentMealType(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        return when (hour) {
            in 6..10 -> "breakfast"
            in 11..16 -> "lunch"
            else -> "dinner"
        }
    }

    private fun showRulesDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_rules)

        val btnClose = dialog.findViewById<ImageView>(R.id.btnClose)
        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }
}