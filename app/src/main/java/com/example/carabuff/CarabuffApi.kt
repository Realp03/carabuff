package com.example.carabuff

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

object CarabuffApi {

    private val client = OkHttpClient()

    private const val FUNCTION_URL =
        "https://asia-southeast1-carabuff-52cc5.cloudfunctions.net/askCarabuff"

    fun askCarabuff(
        message: String,
        userId: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val json = JSONObject().apply {
                put("message", message)
                put("userId", userId)
            }

            val body = json.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(FUNCTION_URL)
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    onError(e.message ?: "Network error")
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val resBody = it.body?.string().orEmpty()

                        try {
                            val jsonRes = JSONObject(resBody)

                            if (!it.isSuccessful) {
                                val errorMessage = jsonRes.optString(
                                    "error",
                                    "Server error: ${it.code}"
                                )
                                onError(errorMessage)
                                return
                            }

                            val reply = jsonRes.optString(
                                "reply",
                                "No reply from Carabuff"
                            )
                            onSuccess(reply)

                        } catch (e: Exception) {
                            if (!it.isSuccessful) {
                                onError("Server error: ${it.code}")
                            } else {
                                onError("Invalid response")
                            }
                        }
                    }
                }
            })

        } catch (e: Exception) {
            onError(e.message ?: "Unexpected error")
        }
    }
}