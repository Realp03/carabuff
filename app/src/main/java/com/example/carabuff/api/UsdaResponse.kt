package com.example.carabuff.api

data class UsdaResponse(
    val foods: List<UsdaFood>
)

data class UsdaFood(
    val description: String,
    val foodNutrients: List<Nutrient>
)

data class Nutrient(
    val nutrientName: String,
    val value: Double?
)