package com.example.carplaytest.fuel

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.carplaytest.databinding.ActivityFuelEfficiencyBinding
import com.google.android.gms.location.*

class FuelEfficiencyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFuelEfficiencyBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLocation: Location? = null
    private var totalDistanceKm = 0.0

    private var totalFuelLiters = 0.0
    private var fuelEfficiencyKmPerLiter = 0.0
    private var remainingFuelLiters = 0.0

    private var lowFuelWarnings = 0
    private var mediumFuelWarnings = 0
    private var isTracking = false
    private var isOutOfFuel = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFuelEfficiencyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.btnStartTracking.setOnClickListener {
            val totalFuelInput = binding.etTotalFuel.text.toString().toDoubleOrNull()
            val efficiencyInput = binding.etEfficiency.text.toString().toDoubleOrNull()

            if (totalFuelInput != null && efficiencyInput != null && totalFuelInput > 0 && efficiencyInput > 0) {
                totalFuelLiters = totalFuelInput
                fuelEfficiencyKmPerLiter = efficiencyInput
                remainingFuelLiters = totalFuelLiters
                isTracking = true
                isOutOfFuel = false
                startTracking()
                Toast.makeText(this, "Tracking started!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter valid fuel and efficiency values", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnStopTracking.setOnClickListener {
            stopTracking()
            Toast.makeText(this, "Tracking stopped!", Toast.LENGTH_SHORT).show()
        }

        binding.backArrow.setOnClickListener {
            finish()
        }

        checkLocationPermissions()
    }

    private fun startTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val locationRequest = LocationRequest.create().apply {
                interval = 2000
                fastestInterval = 1000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    if (!isTracking) return

                    val newLocation = result.lastLocation
                    lastLocation?.let {
                        val distanceKm = newLocation!!.distanceTo(it).toDouble() / 1000
                        totalDistanceKm += distanceKm

                        updateFuelConsumption(distanceKm)
                    }
                    lastLocation = newLocation
                }
            }

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
        }
    }

    private fun stopTracking() {
        isTracking = false
        isOutOfFuel = false
    }

    private fun updateFuelConsumption(distanceKm: Double) {
        if (isOutOfFuel) {
            Toast.makeText(this, "You're out of fuel! Please press Stop to stop notifications.", Toast.LENGTH_SHORT).show()
            return
        }

        val consumedFuel = distanceKm / fuelEfficiencyKmPerLiter
        remainingFuelLiters -= consumedFuel

        if (remainingFuelLiters <= 0) {
            remainingFuelLiters = 0.0
            isOutOfFuel = true
            Toast.makeText(this, "You're out of fuel! Please press Stop to stop notifications.", Toast.LENGTH_LONG).show()
        } else {
            checkFuelLevels()
        }

        updateFuelDisplay()
    }

    private fun checkFuelLevels() {
        val fuelThird = totalFuelLiters / 3

        when {
            remainingFuelLiters <= fuelThird && lowFuelWarnings < 3 -> {
                lowFuelWarnings++
                Toast.makeText(this, "Warning: Low Fuel! Less than 1/3 remaining.", Toast.LENGTH_SHORT).show()
            }

            remainingFuelLiters <= 2 * fuelThird && mediumFuelWarnings < 3 -> {
                mediumFuelWarnings++
                Toast.makeText(this, "Notice: Fuel less than 2/3 remaining.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateFuelDisplay() {
        binding.tvRemainingFuel.text = "Remaining Fuel: %.2f liters".format(remainingFuelLiters)
    }

    private fun checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (isLocationEnabled()) {
                startTracking()
            } else {
                promptUserToEnableLocationServices()
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationMode = Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF)
        return locationMode != Settings.Secure.LOCATION_MODE_OFF
    }

    private fun promptUserToEnableLocationServices() {
        Toast.makeText(this, "Please enable Location Services", Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isLocationEnabled()) {
                    startTracking()
                } else {
                    promptUserToEnableLocationServices()
                }
            } else {
                Toast.makeText(this, "Permission denied. Enable location access in Settings", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
    }
}
