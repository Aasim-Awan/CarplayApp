package com.example.carplaytest.emergency

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

data class EmailRequest(
    val from: Email,
    val to: List<Email>,
    val subject: String,
    val text: String,
    val html: String
)

data class Email(
    val email: String,
    val name: String? = null
)

data class Content(val type: String, val value: String)

interface EmailApiService {
    @Headers("Content-Type: application/json")
    @POST("email")
    fun sendEmail(
        @Header("Authorization") authorizationHeader: String,
        @Body emailRequest: EmailRequest
    ): Call<Void>
}

