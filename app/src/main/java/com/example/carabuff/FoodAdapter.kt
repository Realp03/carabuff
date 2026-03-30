package com.example.carabuff

import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView

class FoodAdapter(
    private val foodList: List<FoodItem>,
    private val onAddClick: (FoodItem, Double) -> Unit
) : RecyclerView.Adapter<FoodAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvFoodName)
        val calories: TextView = view.findViewById(R.id.tvFoodCalories)
        val btnAdd: Button = view.findViewById(R.id.btnAdd)
        val etQuantity: EditText = view.findViewById(R.id.etQuantity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = foodList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val food = foodList[position]

        holder.name.text = food.name
        holder.calories.text = "${food.calories} kcal"

        // 🔥 numeric input
        holder.etQuantity.inputType =
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        // 🔥 default = 1 (IMPORTANT)
        if (holder.etQuantity.text.isNullOrEmpty()) {
            holder.etQuantity.setText("1")
        }

        holder.btnAdd.setOnClickListener {

            val quantityText = holder.etQuantity.text.toString()

            val quantity = quantityText.toDoubleOrNull() ?: 1.0

            val safeQuantity = if (quantity <= 0) 1.0 else quantity

            // 🔥 DIRECT MULTIPLIER
            val finalCalories = (food.calories * safeQuantity).toInt()
            val finalProtein = food.protein * safeQuantity
            val finalCarbs = food.carbs * safeQuantity
            val finalFats = food.fats * safeQuantity

            // 🔥 send computed values
            val updatedFood = FoodItem(
                name = food.name,
                calories = finalCalories,
                protein = finalProtein,
                carbs = finalCarbs,
                fats = finalFats
            )

            onAddClick(updatedFood, safeQuantity)
        }
    }
}