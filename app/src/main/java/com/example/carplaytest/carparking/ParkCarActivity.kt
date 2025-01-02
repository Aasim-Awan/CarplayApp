package com.example.carplaytest.carparking

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.carplaytest.databinding.ActivityParkCarBinding
import com.example.carplaytest.utils.SessionManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

class ParkCarActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityParkCarBinding
    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParkCarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initMapView(savedInstanceState)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        sessionManager = SessionManager(this)

        setupListeners()
        requestLocationPermissions()
        checkLocationServicesEnabled()
    }

    private fun initMapView(savedInstanceState: Bundle?) {
        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    private fun setupListeners() {
        binding.parkCarButton.setOnClickListener {
            fetchAndSaveCurrentLocation()
        }
        binding.backArrow.setOnClickListener {
            finish()
        }
    }

    private fun requestLocationPermissions() {
        Dexter.withContext(this).withPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
        ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                if (report?.areAllPermissionsGranted() == true) {
                    checkLocationServicesEnabled()
                } else {
                    showToast("Location permissions are required to use this feature.")
                }
            }

            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<PermissionRequest>?, token: PermissionToken?
            ) {
                token?.continuePermissionRequest()
            }
        }).check()
    }

    private fun checkLocationServicesEnabled() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showToast("Location is disabled. Please enable it.")
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        } else {
            getCurrentLocation()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true

        if (hasLocationPermission()) {
            googleMap.isMyLocationEnabled = true
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun fetchAndSaveCurrentLocation() {
        if (!hasLocationPermission()) {
            showToast("Please grant location permission to fetch your current location.")
            return
        }

        if (!isLocationEnabled()) {
            showToast("Please enable location services.")
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                handleLocationRetrieved(location)
            } else {
                showToast("Unable to fetch location. Please try again.")
            }
        }
    }

    private fun handleLocationRetrieved(location: Location) {
        val currentLatLng = LatLng(location.latitude, location.longitude)
        googleMap.addMarker(
            MarkerOptions().position(currentLatLng).title("Your Current Location")
        )
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
        saveCarLocation(location.latitude, location.longitude)
    }

    private fun saveCarLocation(latitude: Double, longitude: Double) {
        sessionManager.saveString("latitude", latitude.toString())
        sessionManager.saveString("longitude", longitude.toString())

        showToast("Car parking location saved!")
    }

    private fun getCurrentLocation() {
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                googleMap.addMarker(
                    MarkerOptions().position(currentLatLng).title("Your Current Location")
                )
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

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
            } ?: showToast("Unable to fetch location")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }
}
