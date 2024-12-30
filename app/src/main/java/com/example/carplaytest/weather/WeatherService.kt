package com.example.carplaytest.weather

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {
    @GET("current.json")
    fun getWeather(
        @Query("q") coordinates: String, @Query("key") apiKey: String
    ): Call<WeatherResponse>

}