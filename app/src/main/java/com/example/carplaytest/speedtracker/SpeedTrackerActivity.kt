package com.example.carplaytest.speedtracker

import android.Manifest
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
import com.example.carplaytest.service.CarPlayService
import com.example.carplaytest.utils.SessionManager
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.SettingsClient

class SpeedTrackerActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySpeedTrackerBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var sessionManager: SessionManager
    private var speedLimit: Float = 0f
    private var alarmPlayer: MediaPlayer? = null
    private lateinit var settingsClient: SettingsClient
    private var isServiceRunning = false

    companion object {
        private const val REQUEST_CHECK_SETTINGS = 1001
        private const val BACKGROUND_LOCATION_REQUEST_CODE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySpeedTrackerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        settingsClient = LocationServices.getSettingsClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                location?.let {
                    updateSpeed(it)
                }
            }
        }

        loadSavedSpeedLimit()
        startLocationUpdates()

        binding.btnSetLimit.setOnClickListener {
            val limit = binding.etSpeedLimit.text.toString()
            if (limit.isNotEmpty() && limit.toFloatOrNull() != null) {
                speedLimit = limit.toFloat()
                Log.d("SpeedTrackerActivity", "Speed limit set to: $speedLimit")
                sessionManager.saveFloat("SAVED_SPEED_LIMIT", speedLimit)
                Log.d("SpeedTrackerActivity", "Speed limit saved: $speedLimit")
                binding.etSpeedLimit.clearFocus()
                binding.tvSpeedLimit.text = getString(R.string.speed_limit_set, speedLimit)
                Log.d("SpeedTrackerActivity", "Speed limit set to: $speedLimit")
                startTracking()
            } else {
                Toast.makeText(this, "Please enter a valid speed limit", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnStopAlarm.setOnClickListener {
            Log.d("SpeedTrackerActivity", "Stop Alarm button clicked")
            sessionManager.saveBoolean(SessionManager.SessionKeys.IS_SPEED_TRACKING_ENABLED, false)
            Log.d("CarPlayService", "Stopping location updates")
            stopTracking()
        }

        binding.backArrow.setOnClickListener {
            finish()
        }

        checkLocationPermission()
    }

    private fun loadSavedSpeedLimit() {
        speedLimit = sessionManager.getFloat("SAVED_SPEED_LIMIT", 0f)
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

    private fun updateSpeed(location: Location) {
        val speed = location.speed * 3.6f
        Log.d("SpeedTrackerActivity", "Current speed: $speed km/h")
        binding.tvSpeed.text = getString(R.string.speed_format, speed)

        Log.d("SpeedTrackerActivity", "Speed limit: $speedLimit")
    }

    private fun startTracking() {
        if (!isServiceRunning) {
            val intent = Intent(this, CarPlayService::class.java)
            ContextCompat.startForegroundService(this, intent)
            sessionManager.saveBoolean(
                SessionManager.SessionKeys.IS_LOCATION_TRACKING_ENABLED, false
            )
            sessionManager.saveBoolean(SessionManager.SessionKeys.IS_CRASH_MONITORING_ENABLED, true)
            sessionManager.saveBoolean(SessionManager.SessionKeys.IS_SPEED_TRACKING_ENABLED, true)
            isServiceRunning = true
            startLocationUpdates()
            Toast.makeText(this, "Tracking with speed limit $speedLimit", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopTracking() {
        val intent = Intent(this, CarPlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
