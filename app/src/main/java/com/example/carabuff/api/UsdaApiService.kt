package com.example.carabuff.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface UsdaApiService {

    @GET("fdc/v1/foods/search")
    fun searchFood(
        @Query("api_key") apiKey: String,
        @Query("query") query: String
    ): Call<UsdaResponse>
}