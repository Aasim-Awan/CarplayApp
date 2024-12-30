package com.example.carplaytest.weather

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.example.carplaytest.R
import com.example.carplaytest.databinding.ActivityWeatherBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWeatherBinding
    private val apiKey = "2529d2449a484da08de90610241411"
    private val baseUrl = "http://api.weatherapi.com/v1/"
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sharedPreferences: SharedPreferences
    private val LOCATION_PERMISSION_REQUEST_CODE = 1000
    private val LOCATION_SETTINGS_REQUEST_CODE = 1001

    private val refreshHandler = android.os.Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            refreshWeatherData()
            refreshHandler.postDelayed(this, 60 * 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeatherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("WeatherPrefs", MODE_PRIVATE)

        val cities = resources.getStringArray(R.array.Cities)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, cities)
        binding.etCityName.setAdapter(adapter)

        binding.etCityName.setThreshold(1)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (checkLocationPermissions()) {
            checkLocationEnabled()
        } else {
            requestLocationPermissions()
        }

        val lastUpdateTime = sharedPreferences.getLong("LastUpdateTime", 0)
        val oneMintInMillis = 60 * 1000
        if (System.currentTimeMillis() - lastUpdateTime < oneMintInMillis) {
            val cachedData = sharedPreferences.getString("WeatherData", null)
            cachedData?.let {
                val cachedWeatherResponse = Gson().fromJson(it, WeatherResponse::class.java)
                updateUI(cachedWeatherResponse)
            }
        } else {
            getWeatherByCity("Default City")
        }

        binding.btnSearch.setOnClickListener {
            val cityName = binding.etCityName.text.toString()
            if (cityName.isNotEmpty()) {
                getWeatherByCity(cityName)
            } else {
                if (checkLocationPermissions()) {
                    checkLocationEnabled()
                } else {
                    requestLocationPermissions()
                }
            }
        }
        binding.tvUpdateTime.visibility = View.GONE
        binding.backArrow.setOnClickListener {
            finish()
        }
    }

    private fun getWeatherByCity(cityName: String) {
        binding.progressBar.visibility = View.VISIBLE

        if (!isNetworkAvailable()) {
            binding.tvUpdateTime.visibility = View.VISIBLE
            showCachedDataOrError()
            binding.progressBar.visibility = View.GONE
            return
        }

        val retrofit =
            Retrofit.Builder().baseUrl(baseUrl).addConverterFactory(GsonConverterFactory.create())
                .build()

        val weatherService = retrofit.create(WeatherService::class.java)
        val call = weatherService.getWeather(cityName, apiKey)

        call.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(
                call: Call<WeatherResponse>, response: Response<WeatherResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        updateUI(it)
                        binding.progressBar.visibility = View.GONE
                    }
                } else {
                    Log.e("WeatherError", "Response code: ${response.code()}")
                    Log.e("WeatherError", "Error body: ${response.errorBody()?.string()}")
                    Toast.makeText(
                        this@WeatherActivity, "Failed to retrieve weather data", Toast.LENGTH_SHORT
                    ).show()
                    binding.progressBar.visibility = View.GONE
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Log.e("WeatherError", "Network error: ${t.message}", t)
                Toast.makeText(
                    this@WeatherActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT
                ).show()
                binding.progressBar.visibility = View.GONE
            }
        })
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun showCachedDataOrError() {
        val cachedData = sharedPreferences.getString("WeatherData", null)
        val lastUpdated = sharedPreferences.getString("LastUpdated", "N/A")
        if (cachedData != null) {
            val cachedWeatherResponse = Gson().fromJson(cachedData, WeatherResponse::class.java)
            updateUI(cachedWeatherResponse)
            binding.tvUpdateTime.text = "Last Update: $lastUpdated"
        } else {
            Toast.makeText(
                this,
                "No cached data available. Please check your internet connection.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun cacheWeatherData(weatherResponse: WeatherResponse) {
        val editor = sharedPreferences.edit()
        editor.putString("WeatherData", Gson().toJson(weatherResponse))
        editor.putString(
            "LastUpdated",
            weatherResponse.current.last_updated
        ) // Save last updated time
        editor.putLong("LastUpdateTime", System.currentTimeMillis())
        editor.apply()
    }

    private fun checkLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
            ), LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun checkLocationEnabled() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            promptEnableLocation()
        } else {
            getCurrentLocation()
        }
    }

    private fun promptEnableLocation() {
        Toast.makeText(
            this, "Location is disabled. Please enable it in the settings.", Toast.LENGTH_LONG
        ).show()
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivityForResult(intent, LOCATION_SETTINGS_REQUEST_CODE)
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val latitude = location.latitude
                    val longitude = location.longitude
                    fetchByLocation(latitude, longitude)

                    fusedLocationClient.removeLocationUpdates(this)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper()
        )
    }

    private fun fetchByLocation(latitude: Double, longitude: Double) {
        binding.progressBar.visibility = View.VISIBLE

        if (!isNetworkAvailable()) {
            binding.tvUpdateTime.visibility = View.VISIBLE
            showCachedDataOrError()
            binding.progressBar.visibility = View.GONE
            return
        }

        val retrofit =
            Retrofit.Builder().baseUrl(baseUrl).addConverterFactory(GsonConverterFactory.create())
                .build()

        val weatherService = retrofit.create(WeatherService::class.java)
        val latLng = "$latitude,$longitude"
        val call = weatherService.getWeather(latLng, apiKey)

        call.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(
                call: Call<WeatherResponse>, response: Response<WeatherResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        updateUI(it)
                        cacheWeatherData(it)
                        binding.progressBar.visibility = View.GONE
                    }
                } else {
                    Log.e("WeatherError", "Response code: ${response.code()}")
                    Log.e("WeatherError", "Error body: ${response.errorBody()?.string()}")
                    Toast.makeText(
                        this@WeatherActivity, "Failed to retrieve weather data", Toast.LENGTH_SHORT
                    ).show()
                    binding.progressBar.visibility = View.GONE
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Log.e("WeatherError", "Network error: ${t.message}", t)
                Toast.makeText(
                    this@WeatherActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT
                ).show()
                binding.progressBar.visibility = View.GONE
            }
        })
    }

    private fun updateUI(weather: WeatherResponse) {
        val temperature = weather.current.temp_c
        val humidity = weather.current.humidity
        val condition = weather.current.condition.text
        val lastUpdated = weather.current.last_updated
        val iconUrl = "https:${weather.current.condition.icon}"

        binding.tvUpdateTime.text = lastUpdated
        binding.tvCity.text = weather.location.name ?: "Current Location"
        binding.tvTemperature.text = String.format("%.1f°C", temperature)
        binding.tvCondition.text = "$condition"
        binding.tvHumidity.text = "Humidity: $humidity%"

        Glide.with(this)
            .load(iconUrl)
            .into(binding.ivConditionIcon)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationEnabled()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshHandler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    private fun refreshWeatherData() {
        val cityName = binding.etCityName.text.toString()
        if (cityName.isNotEmpty()) {
            getWeatherByCity(cityName)
        } else {
            getCurrentLocation()
        }
    }
}
