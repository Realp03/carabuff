package com.example.carabuff

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.carabuff.api.RetrofitClient
import com.example.carabuff.api.UsdaResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddFoodActivity : AppCompatActivity() {

    private lateinit var etFood: EditText
    private lateinit var btnSearchFood: Button
    private lateinit var rvFoods: RecyclerView
    private lateinit var tvResult: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnHelp: ImageView
    private lateinit var btnBack: ImageView

    private val foodList = mutableListOf<FoodItem>()

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

        btnBack.setOnClickListener {
            finish()
        }

        btnHelp.setOnClickListener {
            showRulesDialog()
        }

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
            progressBar.visibility = View.VISIBLE
            tvResult.text = ""
            foodList.clear()
            rvFoods.adapter = null

            searchFoodFromApi(query)
        }
    }

    private fun searchFoodFromApi(query: String) {
        val apiKey = "QVeIPMAStjyC65jSSKol0FCJ9zX6qQwjh0cefesb"

        RetrofitClient.instance.searchFood(apiKey, query)
            .enqueue(object : Callback<UsdaResponse> {

                override fun onResponse(
                    call: Call<UsdaResponse>,
                    response: Response<UsdaResponse>
                ) {
                    progressBar.visibility = View.GONE
                    btnSearchFood.isEnabled = true

                    if (!response.isSuccessful) {
                        tvResult.text = "Error Code: ${response.code()}"
                        Toast.makeText(
                            this@AddFoodActivity,
                            "Error Code: ${response.code()}",
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }

                    val body = response.body()
                    val foods = body?.foods ?: emptyList()

                    if (foods.isEmpty()) {
                        tvResult.text = "No food found"
                        Toast.makeText(
                            this@AddFoodActivity,
                            "No food found",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }

                    foodList.clear()

                    for (food in foods) {
                        var protein = 0.0
                        var carbs = 0.0
                        var fats = 0.0
                        var calories = 0

                        val nutrients = food.foodNutrients ?: emptyList()

                        for (nutrient in nutrients) {
                            when (nutrient.nutrientName) {
                                "Protein" -> protein = nutrient.value ?: 0.0
                                "Carbohydrate, by difference" -> carbs = nutrient.value ?: 0.0
                                "Total lipid (fat)" -> fats = nutrient.value ?: 0.0
                                "Energy" -> calories = nutrient.value?.toInt() ?: 0
                            }
                        }

                        foodList.add(
                            FoodItem(
                                name = food.description ?: "Unknown Food",
                                calories = calories,
                                protein = protein,
                                carbs = carbs,
                                fats = fats
                            )
                        )
                    }

                    tvResult.text = "${foodList.size} food result(s)"
                    updateRecycler()
                }

                override fun onFailure(call: Call<UsdaResponse>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    btnSearchFood.isEnabled = true
                    tvResult.text = "Connection failed"

                    Toast.makeText(
                        this@AddFoodActivity,
                        "Connection failed: ${t.localizedMessage ?: "Unknown error"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun updateRecycler() {
        rvFoods.adapter = FoodAdapter(foodList) { selectedFood, quantity ->

            val finalCalories = (selectedFood.calories * quantity).toInt()
            val finalProtein = selectedFood.protein * quantity
            val finalCarbs = selectedFood.carbs * quantity
            val finalFats = selectedFood.fats * quantity

            val mealType = getCurrentMealType()

            saveFoodLog(
                selectedFood.name,
                finalCalories,
                finalProtein,
                finalCarbs,
                finalFats,
                mealType
            )
        }
    }

    private fun saveFoodLog(
        name: String,
        calories: Int,
        protein: Double,
        carbs: Double,
        fats: Double,
        mealType: String
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()

        val data = hashMapOf(
            "name" to name,
            "calories" to calories,
            "protein" to protein,
            "carbs" to carbs,
            "fats" to fats,
            "mealType" to mealType,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(userId)
            .collection("foodLogs")
            .add(data)
            .addOnSuccessListener {
                val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                val prefs = getSharedPreferences("meal_reminder", MODE_PRIVATE)
                prefs.edit().putBoolean("${mealType}_$today", true).apply()

                Toast.makeText(this, "Food Added 🍗", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save food", Toast.LENGTH_SHORT).show()
            }
    }

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
        btnClose?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}