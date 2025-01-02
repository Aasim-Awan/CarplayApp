package com.example.carplaytest.locationhistory

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.carplaytest.carmaintenance.database.CarPlayDB
import com.example.carplaytest.databinding.ActivityLocationHistoryBinding
import com.example.carplaytest.locationhistory.database.LocationDao
import com.example.carplaytest.locationhistory.database.LocationEntity
import com.example.carplaytest.locationhistory.database.LocationLog
import com.example.carplaytest.locationhistory.database.LocationLogDao
import com.example.carplaytest.service.CarPlayService
import com.example.carplaytest.utils.SessionManager
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
import com.google.android.gms.maps.model.PolylineOptions
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
    private lateinit var appDatabase: CarPlayDB
    private lateinit var sessionManager: SessionManager

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

        sessionManager = SessionManager(this)
        appDatabase = Room.databaseBuilder(
            applicationContext, CarPlayDB::class.java, "app-database"
        ).build()

        locationDao = appDatabase.locationDao()
        locationLogDao = appDatabase.locationLogDao()
        mapView = binding.mapView

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        binding.mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        binding.locationRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)


        lifecycleScope.launch {
            if (locationDao.getAllLocations().isNotEmpty()) {
                binding.showAllLocationsButton.visibility = View.VISIBLE
            } else {
                binding.noHistoryTextView.visibility = View.VISIBLE
            }

            if (locationDao.getAllLocations().isNotEmpty() && locationDao.getAllLocations()
                    .lastOrNull()?.endTime == null
            ) {
                Log.d(
                    "LocationHistoryActivity",
                    "Tracking in progress ${locationDao.getAllLocations().lastOrNull()?.endTime}"
                )
                binding.stopLocationButton.visibility = View.VISIBLE
                binding.trackLocationButton.visibility = View.GONE
            } else {
                binding.stopLocationButton.visibility = View.GONE
                binding.trackLocationButton.visibility = View.VISIBLE
            }
        }

        binding.trackLocationButton.setOnClickListener {
            startTracking()
            binding.stopLocationButton.visibility = View.VISIBLE
            binding.trackLocationButton.visibility = View.GONE
            binding.locationRecyclerView.visibility = View.VISIBLE
            binding.noHistoryTextView.visibility = View.GONE
            binding.showAllLocationsButton.visibility = View.VISIBLE
            binding.deleteHistoryButton.visibility = View.VISIBLE
            Log.d("LocationHistoryActivity", "Start button clicked")
        }

        binding.stopLocationButton.setOnClickListener {
            stopTracking()
            updateDisplayedLocations()
            binding.stopLocationButton.visibility = View.GONE
            binding.trackLocationButton.visibility = View.VISIBLE
            Log.d("LocationHistoryActivity", "Stop button clicked")
        }

        binding.deleteHistoryButton.setOnClickListener {
            clearAllLocations()
            binding.locationRecyclerView.visibility = View.GONE
            binding.noHistoryTextView.visibility = View.VISIBLE
        }

        binding.showAllLocationsButton.setOnClickListener {
            showAllLocationsOnMap()
            updateDisplayedLocations()
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
            googleMap.setOnMyLocationButtonClickListener {
                moveToCurrentLocation()
                true
            }
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
                    MarkerOptions().position(currentLatLng)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                )

                googleMap.setOnMarkerClickListener { clickedMarker ->
                    googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            clickedMarker.position,
                            15f
                        )
                    )

                    clickedMarker.title?.let { title ->
                        Toast.makeText(this, "Marker clicked: $title", Toast.LENGTH_SHORT).show()
                    }
                    true
                }


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
            MarkerOptions().position(currentLatLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        )
    }

    private fun startTracking() {
        Log.d("CarPlayService2", "Starting tracking")

        lifecycleScope.launch(Dispatchers.IO) {
            val locationEntity = LocationEntity(
                id = 0,
                startTime = System.currentTimeMillis(),
                endTime = null,
                createdDate = System.currentTimeMillis()
            )

            val sessionId = locationDao.insertLocation(locationEntity)
            sessionManager.saveLong("activeLocationEntityId", sessionId)
            Log.d("CarPlayService2", "Started tracking with sessionId: $sessionId")
        }

        val serviceIntent = Intent(this, CarPlayService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        Log.d("LocationHistoryActivity", "Service started")
        sessionManager.saveBoolean(SessionManager.SessionKeys.IS_LOCATION_TRACKING_ENABLED, true)
        sessionManager.saveBoolean(SessionManager.SessionKeys.IS_CRASH_MONITORING_ENABLED, true)
        sessionManager.saveBoolean(SessionManager.SessionKeys.IS_SPEED_TRACKING_ENABLED, true)
    }

    private fun stopTracking() {
        val sessionId = sessionManager.getLong("activeLocationEntityId", -1)
        if (sessionId != -1L) {
            lifecycleScope.launch(Dispatchers.IO) {
                val locationEntity = locationDao.getLocationById(sessionId)

                if (locationEntity != null) {
                    locationEntity.endTime = System.currentTimeMillis()
                    locationDao.updateLocation(locationEntity)
                    sessionManager.removeKey("activeLocationEntityId")
                    sessionManager.clear()
                }
            }
        } else {
            lifecycleScope.launch(Dispatchers.Main) {
                Log.d("LocationHistoryActivity", "No active tracking session found")
            }
        }

        val serviceIntent = Intent(this, CarPlayService::class.java)
        stopService(serviceIntent)
        Log.d("LocationHistoryActivity", "Service stopped")

        sessionManager.saveBoolean(SessionManager.SessionKeys.IS_LOCATION_TRACKING_ENABLED, false)
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun clearAllLocations() {
        lifecycleScope.launch(Dispatchers.IO) {
            locationDao.clearAll()
            locationLogDao.clearLocationLogs()
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@LocationHistoryActivity, "All locations cleared.", Toast.LENGTH_SHORT
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
            Log.d("LocationHistoryActivity", "Location is enabled.")
        }
    }

    private fun updateDisplayedLocations() {
        lifecycleScope.launch {
            val locations = withContext(Dispatchers.IO) { locationDao.getAllLocations() }
            displayLocations(locations)
        }
    }

    private fun showAllLocationsOnMap() {
        lifecycleScope.launch {
            val locations = withContext(Dispatchers.IO) { locationLogDao.getAllLocationsLogs() }

            if (locations.isNotEmpty()) {
                googleMap.clear()

                locations.forEach { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    googleMap.addMarker(
                        MarkerOptions().position(latLng)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))

                    )
                }

                val boundsBuilder = LatLngBounds.Builder()
                locations.forEach { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    boundsBuilder.include(latLng)
                }
                val bounds = boundsBuilder.build()
                val padding = 100
                val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
                googleMap.animateCamera(cameraUpdate)
            } else {
                Toast.makeText(
                    this@LocationHistoryActivity, "No saved locations found.", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun displayLocations(locations: List<LocationEntity>) {
        if (locations.isNotEmpty()) {
            googleMap.clear()

//            locations.forEach { location ->
//                val latLng = LatLng(location.latitude, location.longitude)
//                googleMap.addMarker(
//                    MarkerOptions().position(latLng)
//                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
//                )
//            }
//
//            val lastLocation = LatLng(locations.last().latitude, locations.last().longitude)
//            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastLocation, 15f))

            binding.locationRecyclerView.apply {
                adapter = LocationHistoryAdapter(this@LocationHistoryActivity,
                    locations.toMutableList(),
                    onEntitySelected = { selectedEntity ->
                        handleLocationClick(selectedEntity)
                    },
                    onEntityDeleted = { deletedEntity ->
                        deleteLocation(deletedEntity)
                    })
            }

            binding.noHistoryTextView.visibility = View.GONE
        } else {
            binding.locationRecyclerView.visibility = View.GONE
            binding.noHistoryTextView.visibility = View.VISIBLE
        }
    }

    private fun handleLocationClick(selectedEntity: LocationEntity) {
        lifecycleScope.launch {
            try {
                val relatedLocationLogs = withContext(Dispatchers.IO) {
                    appDatabase.locationLogDao().getLocationLogsByLocationId(selectedEntity.id)
                }

                if (relatedLocationLogs.isNotEmpty()) {
                    drawPathOnMap(relatedLocationLogs)
                    //    displayLocationLogs(relatedLocationLogs) =
                } else {
                    Toast.makeText(
                        this@LocationHistoryActivity,
                        "No logs found for this location",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@LocationHistoryActivity, "Error fetching location logs", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun drawPathOnMap(locations: List<LocationLog>) {
        googleMap.clear()

        if (locations.isEmpty()) return

        val firstLocation = LatLng(locations.first().latitude, locations.first().longitude)
        googleMap.addMarker(
            MarkerOptions().position(firstLocation)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
             .title("Start")
        )

        val lastLocation = LatLng(locations.last().latitude, locations.last().longitude)
        googleMap.addMarker(
            MarkerOptions().position(lastLocation)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
             .title("End")
        )

        val polylineOptions = PolylineOptions().color(Color.BLUE).width(5f)

        locations.forEach { location ->
            polylineOptions.add(LatLng(location.latitude, location.longitude))
        }

        googleMap.addPolyline(polylineOptions)

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 15f))
    }

    private fun deleteLocation(locationEntity: LocationEntity) {
        lifecycleScope.launch {
            locationDao.deleteLocation(locationEntity)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}
