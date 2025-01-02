package com.example.carplaytest.carparking

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.example.carplaytest.R
import com.example.carplaytest.databinding.ActivityFindCarBinding
import com.example.carplaytest.utils.SessionManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

class FindCarActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityFindCarBinding
    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFindCarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        sessionManager = SessionManager(this)

        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        binding.findCarButton.setOnClickListener {
            showCarParkingLocation()
        }
        binding.backArrow.setOnClickListener {
            finish()
        }

        checkLocationEnabled()
        checkPermissions()
    }

    private fun checkPermissions() {
        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report?.areAllPermissionsGranted() == true) {
                        checkLocationEnabled()
                    } else {
                        Toast.makeText(
                            this@FindCarActivity,
                            "Location permissions are required to use this feature.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    Toast.makeText(
                        this@FindCarActivity,
                        "Please grant location permissions to continue.",
                        Toast.LENGTH_SHORT
                    ).show()
                    token?.continuePermissionRequest()
                }
            }).check()
    }

    private fun checkLocationEnabled() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Location is disabled. Please enable it.", Toast.LENGTH_SHORT)
                .show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            getCurrentLocation()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
            getCurrentLocation()
        } else {
            Toast.makeText(
                this,
                "Location permissions are required to display the map.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun getCurrentLocation() {
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                googleMap.addMarker(
                    MarkerOptions()
                        .position(currentLatLng)
                        .title("Your Current Location")
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

            } else {
                Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to retrieve location.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCarParkingLocation() {
        val latitude = sessionManager.getString("latitude", null)?.toDoubleOrNull()
        val longitude = sessionManager.getString("longitude", null)?.toDoubleOrNull()

        if (latitude != null && longitude != null) {
            val carLatLng = LatLng(latitude, longitude)

            if (::googleMap.isInitialized) {
                val customIcon = try {
                    val vectorDrawable = AppCompatResources.getDrawable(this, R.drawable.ic_car)
                    if (vectorDrawable != null) {
                        val bitmap = Bitmap.createBitmap(
                            vectorDrawable.intrinsicWidth,
                            vectorDrawable.intrinsicHeight,
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = Canvas(bitmap)
                        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
                        vectorDrawable.draw(canvas)
                        BitmapDescriptorFactory.fromBitmap(bitmap)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e("IconError", "Error loading car icon", e)
                    null
                }

                googleMap.addMarker(
                    MarkerOptions()
                        .position(carLatLng)
                        .title("Your Car Parking Location")
                        .icon(customIcon)
                )
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(carLatLng, 15f))
                Toast.makeText(this, "Car parking location displayed.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Google Map is not initialized yet.", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            Toast.makeText(this, "No car parking location saved.", Toast.LENGTH_SHORT).show()
        }
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
