package com.example.carplaytest.speedtracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.carplaytest.R
import com.example.carplaytest.databinding.ActivitySpeedTrackerBinding
import com.example.carplaytest.sevice.CarPlayService
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*

class SpeedTrackerActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySpeedTrackerBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var speedLimit: Float = 0f
    private var alarmPlayer: MediaPlayer? = null
    private lateinit var settingsClient: SettingsClient

    companion object {
        private const val REQUEST_CHECK_SETTINGS = 1001
        private const val BACKGROUND_LOCATION_REQUEST_CODE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySpeedTrackerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        settingsClient = LocationServices.getSettingsClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                location?.let {
                    Log.d("SpeedTrackerActivity", "Location update: Latitude: ${it.latitude}, Longitude: ${it.longitude}, Speed: ${it.speed}")
                    updateSpeed(it)
                }
            }
        }

        loadSavedSpeedLimit()
        startLocationUpdates()

        binding.btnSetLimit.setOnClickListener {
            val limit = binding.etSpeedLimit.text.toString()
            if (limit.isNotEmpty()) {
                speedLimit = limit.toFloat()
                saveSpeedLimit(speedLimit)
                binding.etSpeedLimit.clearFocus()
                binding.tvSpeedLimit.text = getString(R.string.speed_limit_set, speedLimit)
                Log.d("SpeedTrackerActivity", "Speed limit set to: $speedLimit")
                startTracking()
            } else {
                Toast.makeText(this, "Please enter a valid speed limit", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnStopAlarm.setOnClickListener {
            stopTracking()
        }

        binding.backArrow.setOnClickListener {
            finish()
        }

        checkLocationPermission()
    }

    private fun saveSpeedLimit(speedLimit: Float) {
        val sharedPreferences = getSharedPreferences("SpeedPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putFloat("SAVED_SPEED_LIMIT", speedLimit)
        editor.apply()
        Log.d("SpeedTrackerActivity", "Speed limit saved: $speedLimit")
    }

    private fun getSavedSpeedLimit(): Float {
        val sharedPreferences = getSharedPreferences("SpeedPrefs", Context.MODE_PRIVATE)
        val savedLimit = sharedPreferences.getFloat("SAVED_SPEED_LIMIT", 0f)
        Log.d("SpeedTrackerActivity", "Loaded saved speed limit: $savedLimit")
        return savedLimit
    }

    private fun loadSavedSpeedLimit() {
        speedLimit = getSavedSpeedLimit()
        binding.tvSpeedLimit.text = getString(R.string.speed_limit_set, speedLimit)
        Log.d("SpeedTrackerActivity", "Speed limit loaded: $speedLimit")
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    BACKGROUND_LOCATION_REQUEST_CODE
                )
            }
        }
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            requestBackgroundLocationPermission()
            checkLocationSettings()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                requestBackgroundLocationPermission()
                checkLocationSettings()
            } else {
                Toast.makeText(
                    this, "Location permission is required to track speed.", Toast.LENGTH_SHORT
                ).show()
            }
        }

    private fun checkLocationSettings() {
        val locationRequest = LocationRequest.create().apply {
            interval = 2000
            fastestInterval = 1000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val task = settingsClient.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            Log.d("SpeedTrackerActivity", "Location settings are satisfied.")
        }.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                exception.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
            } else {
                Toast.makeText(this, "Please enable location services.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 2000
            fastestInterval = 1000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback, mainLooper
            )
            Log.d("SpeedTrackerActivity", "Started location updates.")
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d("SpeedTrackerActivity", "Stopped location updates.")
    }

    private fun updateSpeed(location: Location) {
        val speed = location.speed * 3.6f // Convert from m/s to km/h
        Log.d("SpeedTrackerActivity", "Current speed: $speed km/h")
        binding.tvSpeed.text = getString(R.string.speed_format, speed)

        if (speed > speedLimit) {
            Log.d("SpeedTrackerActivity", "Speed exceeded limit: $speed km/h > $speedLimit km/h")
            startAlarm()
        } else {
            stopAlarm()
        }
    }

    private fun startAlarm() {
        if (alarmPlayer == null) {
            alarmPlayer = MediaPlayer.create(this, R.raw.alert)
            alarmPlayer?.isLooping = true
            alarmPlayer?.start()
            Log.d("SpeedTrackerActivity", "Alarm started")
        }
    }

    private fun stopAlarm() {
        alarmPlayer?.let {
            if (it.isPlaying) {
                it.stop()
                it.release()
                alarmPlayer = null
                Log.d("SpeedTrackerActivity", "Alarm stopped")
            }
        }
    }

    private fun startTracking() {
        val intent = Intent(this, CarPlayService::class.java).apply {
            putExtra("action", "SPEED_TRACKING")
            putExtra("SPEED_LIMIT_EXTRA", speedLimit)
        }
        ContextCompat.startForegroundService(this, intent)
        startLocationUpdates()
        Toast.makeText(this, "Tracking with speed limit $speedLimit", Toast.LENGTH_SHORT).show()
    }

    private fun stopTracking() {
        stopService(Intent(this, CarPlayService::class.java))
        stopLocationUpdates()
        stopAlarm()
        Log.d("SpeedTrackerActivity", "Speed Tracking Stopped")
        Toast.makeText(this, "Speed Tracking Stopped", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        stopAlarm()
    }
}
