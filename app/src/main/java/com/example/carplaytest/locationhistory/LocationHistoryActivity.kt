package com.example.carplaytest.locationhistory

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.room.Room
import com.example.carplaytest.R
import com.example.carplaytest.carmaintenance.database.CarPlayDB
import com.example.carplaytest.databinding.ActivityLocationHistoryBinding
import com.example.carplaytest.locationhistory.database.LocationDao
import com.example.carplaytest.locationhistory.database.LocationEntity
import com.example.carplaytest.locationhistory.database.LocationLog
import com.example.carplaytest.locationhistory.database.LocationLogDao
import com.example.carplaytest.sevice.CarPlayService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocationHistoryActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityLocationHistoryBinding
    private lateinit var locationDao: LocationDao
    private lateinit var locationLogDao: LocationLogDao
    private lateinit var googleMap: GoogleMap
    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private val locationManager by lazy { getSystemService(LOCATION_SERVICE) as LocationManager }

    private val permissionRequestLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                checkLocationEnabled()
            } else {
                Toast.makeText(
                    this, "Permission denied. Location tracking unavailable.", Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val appDatabase = Room.databaseBuilder(
            applicationContext, CarPlayDB::class.java, "app-database"
        ).build()
        locationDao = appDatabase.locationDao()
        locationLogDao = appDatabase.locationLogDao()
        mapView = binding.mapView

        binding.stopLocationButton.isEnabled = false
        binding.stopLocationButton.setBackgroundColor(ContextCompat.getColor(this, R.color.gray))

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        binding.mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        binding.locationRecyclerView.layoutManager = GridLayoutManager(this, 2)

        binding.trackLocationButton.setOnClickListener {
            startTracking()
            Log.d("LocationHistoryActivity", "Start button clicked")
        }

        binding.stopLocationButton.setOnClickListener {
            stopTracking()
            Log.d("LocationHistoryActivity", "Stop button clicked")
        }

        binding.deleteHistoryButton.setOnClickListener {
            clearAllLocations()
            binding.locationRecyclerView.visibility = View.GONE
            binding.noHistoryTextView.visibility = View.VISIBLE
        }

        binding.showAllLocationsButton.setOnClickListener {
            showAllLocationsOnMap()
        }


        binding.backArrow.setOnClickListener { finish() }

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionRequestLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            checkLocationEnabled()
        }

        updateDisplayedLocations()

        setupLocationRequest()
    }

    private fun setupLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = 10000  // Request location updates every 10 seconds
            fastestInterval = 5000 // Allow updates every 5 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    updateMapWithLocation(location)
                }
            }
        }


    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
            moveToCurrentLocation()
        } else {
            Toast.makeText(this, "Location permission is not granted.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveToCurrentLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                googleMap.addMarker(
                    MarkerOptions().position(currentLatLng).title("You are here")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                )
            } else {
                Toast.makeText(this, "Unable to fetch current location.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to retrieve location: ${it.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun updateMapWithLocation(location: Location) {
        val currentLatLng = LatLng(location.latitude, location.longitude)
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

        googleMap.addMarker(
            MarkerOptions().position(currentLatLng).title("You are here")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        )
    }

    private fun startTracking() {
        if (isServiceRunning()) {
            Toast.makeText(this, "Tracking is already running.", Toast.LENGTH_SHORT).show()
            return
        }

        val serviceIntent = Intent(this, CarPlayService::class.java).apply {
            putExtra("action", "LOCATION_TRACKING")
        }
        ContextCompat.startForegroundService(this, serviceIntent)
        Log.d("LocationHistoryActivity", "Service started")

        startLocationUpdates()
        updateUIForTracking(true)
    }

    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun stopTracking() {
        if (!isServiceRunning()) {
            Toast.makeText(this, "Tracking is not currently running.", Toast.LENGTH_SHORT).show()
            return
        }

        val serviceIntent = Intent(this, CarPlayService::class.java)
        stopService(serviceIntent)
        Log.d("LocationHistoryActivity", "Service stopped")

        stopLocationUpdates()
        updateUIForTracking(false)
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun updateUIForTracking(isTracking: Boolean) {
        if (isTracking) {
            binding.stopLocationButton.isEnabled = true
            binding.stopLocationButton.setBackgroundColor(
                ContextCompat.getColor(this, R.color.primary)
            )
            binding.trackLocationButton.isEnabled = false
            binding.trackLocationButton.setBackgroundColor(
                ContextCompat.getColor(this, R.color.gray)
            )
        } else {
            binding.stopLocationButton.isEnabled = false
            binding.stopLocationButton.setBackgroundColor(
                ContextCompat.getColor(this, R.color.gray)
            )
            binding.trackLocationButton.isEnabled = true
            binding.trackLocationButton.setBackgroundColor(
                ContextCompat.getColor(this, R.color.primary)
            )
        }
    }

    private fun clearAllLocations() {
        lifecycleScope.launch(Dispatchers.IO) {
            locationDao.clearAll()
            locationLogDao.clearLocationLogs()
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@LocationHistoryActivity,
                    "All locations cleared.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun checkLocationEnabled() {
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder(this)
                .setMessage("Location is disabled. Enable it to start tracking.")
                .setPositiveButton("Enable") { _, _ ->
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }.setNegativeButton("Cancel") { _, _ ->
                    Toast.makeText(
                        this,
                        "Location tracking requires enabled location services.",
                        Toast.LENGTH_SHORT
                    ).show()
                }.show()
        } else {
            Toast.makeText(this, "Location is enabled. You can start tracking.", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun updateDisplayedLocations() {
        lifecycleScope.launch {
            val locations = withContext(Dispatchers.IO) { locationLogDao.getAllLocationsLogs() }
            displayLocations(locations)
        }
    }

    private fun showAllLocationsOnMap() {
        lifecycleScope.launch {
            // Get all location logs from the database
            val locations = withContext(Dispatchers.IO) { locationLogDao.getAllLocationsLogs() }

            if (locations.isNotEmpty()) {
                googleMap.clear()  // Clear existing markers

                // Add a marker for each location
                locations.forEach { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    googleMap.addMarker(
                        MarkerOptions().position(latLng)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                            .title("Saved Location")
                    )
                }

                // Move the camera to show all the markers
                val boundsBuilder = LatLngBounds.Builder()
                locations.forEach { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    boundsBuilder.include(latLng)
                }
                val bounds = boundsBuilder.build()
                val padding = 100 // padding around the map
                val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
                googleMap.animateCamera(cameraUpdate)
            } else {
                Toast.makeText(
                    this@LocationHistoryActivity,
                    "No saved locations found.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun displayLocations(locations: List<LocationLog>) {
        if (locations.isNotEmpty()) {
            googleMap.clear()

            val markers = locations.map { LatLng(it.latitude, it.longitude) }
            markers.forEach { latLng ->
                googleMap.addMarker(
                    MarkerOptions().position(latLng)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                )
            }

            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(markers.last(), 15f))

            binding.locationRecyclerView.adapter = LocationHistoryAdapter(
                this@LocationHistoryActivity,
                locations
            )
            binding.noHistoryTextView.visibility = View.GONE
        } else {
            binding.locationRecyclerView.visibility = View.GONE
            binding.noHistoryTextView.visibility = View.VISIBLE
        }
    }

    private fun isServiceRunning(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningServices = activityManager.getRunningServices(Int.MAX_VALUE)
        for (service in runningServices) {
            if (CarPlayService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        if (isServiceRunning()) {
            updateUIForTracking(true)
        } else {
            updateUIForTracking(false)
        }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}
