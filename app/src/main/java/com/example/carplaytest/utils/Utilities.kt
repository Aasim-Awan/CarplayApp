package com.example.carplaytest.utils

import android.content.Context
import com.google.gson.Gson
import java.io.IOException

object Utilities {

    private fun getJsonDataFromAsset(context: Context, fileName: String): String? {
        val jsonString: String
        try {
            jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            return null
        }
        return jsonString
    }
    fun <T> parseJsonToExercisesModel(context: Context, fileName: String, clazz: Class<T>): T? {
        val jsonString = getJsonDataFromAsset(context, fileName)
        return if (jsonString != null) {
            try {
                val gson = Gson()
                gson.fromJson(jsonString, clazz)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }

}