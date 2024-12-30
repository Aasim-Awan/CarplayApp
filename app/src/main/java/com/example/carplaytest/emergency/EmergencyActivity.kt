package com.example.carplaytest.emergency

import SharedPreferencesHelper
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.carplaytest.databinding.ActivityEmergencyBinding
import com.example.carplaytest.sevice.CarPlayService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class EmergencyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmergencyBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var isTracingStarted = false

    companion object {
        private const val TAG = "EmergencyActivity"
        private const val PERMISSIONS_REQUEST_CODE = 1
        const val CRASH_THRESHOLD = 10.0
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmergencyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkPermissions()
        initializeComponents()
        manageInitialState()
    }

    private fun manageInitialState() {
        val emails = SharedPreferencesHelper.getEmails(this)
        Log.d(TAG, "onCreate: $emails")

        if (isUserDetailsComplete()) {
            binding.btnChangeEmail.visibility = View.VISIBLE
            binding.btnDelete.visibility = View.VISIBLE
            binding.userDetailsForms.visibility = View.VISIBLE
            binding.userDetailsForm.visibility = View.GONE
            showUserDetailForm()
        } else {
            binding.btnChangeEmail.visibility = View.GONE
            binding.btnDelete.visibility = View.GONE
            binding.userDetailsForms.visibility = View.GONE
            binding.userDetailsForm.visibility = View.VISIBLE
        }

        if (isServiceRunning(CarPlayService::class.java)) {
            binding.btnStartService.visibility = View.GONE
            binding.btnStopService.visibility = View.VISIBLE
            isTracingStarted = true
        } else {
            binding.btnStartService.visibility = View.VISIBLE
            binding.btnStopService.visibility = View.GONE
            isTracingStarted = false
        }
    }

    private fun initializeComponents() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Button listeners for various actions
        binding.btnChangeEmail.setOnClickListener { showChangeEmailDialog() }
        binding.btnStartService.setOnClickListener { startService() }
        binding.backArrow.setOnClickListener { finish() }
        binding.btnStopService.setOnClickListener { stopService() }
        binding.btnDelete.setOnClickListener {
            SharedPreferencesHelper.clearEmails(this)
            manageInitialState()
        }
        binding.btnSaveDetails.setOnClickListener { saveUserDetails() }
    }

    private fun isUserDetailsComplete(): Boolean {
        val emails = SharedPreferencesHelper.getEmails(this)
        return !emails.first.isNullOrEmpty() && !emails.second.isNullOrEmpty()
    }

    private fun showChangeEmailDialog() {
        AlertDialog.Builder(this).apply {
            setTitle("Change Emergency Email")
            val input = EditText(this@EmergencyActivity).apply {
                inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                hint = "Enter new email"
            }
            setView(input)

            setPositiveButton("Save") { _, _ ->
                val newEmail = input.text.toString().trim()
                if (newEmail.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail)
                        .matches()
                ) {
                    val emails = SharedPreferencesHelper.getEmails(this@EmergencyActivity)
                    SharedPreferencesHelper.saveEmails(
                        this@EmergencyActivity, emails.first!!, emails.second!!, newEmail
                    )
                    Toast.makeText(this@EmergencyActivity, "Email updated!", Toast.LENGTH_SHORT)
                        .show()
                    showUserDetailForm()
                } else {
                    Toast.makeText(
                        this@EmergencyActivity,
                        "Invalid email address!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            setNegativeButton("Cancel", null)
            show()
        }
    }

    private fun saveUserDetails() {
        val name = binding.etUserName.text.toString().trim()
        val senderEmail = binding.tvSenderEmail.text.toString().trim()
        val receiverEmail = binding.tvReceiverEmail.text.toString().trim()

        if (name.isEmpty() || senderEmail.isEmpty() || receiverEmail.isEmpty()) {
            Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show()
            return
        }

        SharedPreferencesHelper.saveEmails(this, name, senderEmail, receiverEmail)
        Toast.makeText(this, "Details saved successfully.", Toast.LENGTH_SHORT).show()
        manageInitialState()
    }

    private fun showUserDetailForm() {
        val emails = SharedPreferencesHelper.getEmails(this)
        binding.UserName.text = "Name: \n     ${emails.first}"
        binding.SenderEmail.text = "Email:\n     ${emails.second}"
        binding.ReceiverEmail.text = "Emergency Email: \n      ${emails.third}"
    }

    private fun checkPermissions() {
        if (arePermissionsGranted()) {
            checkLocationServicesEnabled()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
        }
    }

    private fun arePermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkLocationServicesEnabled() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            redirectToLocationSettings()
        }
    }

    private fun redirectToLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
        Toast.makeText(this, "Please enable GPS location services.", Toast.LENGTH_SHORT).show()
    }

    private fun startService() {
        if (isTracingStarted) {
            Toast.makeText(this, "Tracing is already active.", Toast.LENGTH_SHORT).show()
            return
        }

        if (isUserDetailsComplete()) {
            if (!isServiceRunning(CarPlayService::class.java)) {
                val intent = Intent(this, CarPlayService::class.java).apply {
                    putExtra("action", "CRASH_DETECTION")
                }
                startService(intent)
                Log.d(TAG, "Crash Detection Service Started")
                isTracingStarted = true
            }
            Toast.makeText(this, "Crash detection service started", Toast.LENGTH_SHORT).show()
            binding.btnStartService.visibility = View.GONE
            binding.btnStopService.visibility = View.VISIBLE
        } else {
            Toast.makeText(this, "Please enter your details first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopService() {
        if (!isTracingStarted) {
            Toast.makeText(this, "Tracing is not active.", Toast.LENGTH_SHORT).show()
            return
        }

        if (isServiceRunning(CarPlayService::class.java)) {
            val intent = Intent(this, CarPlayService::class.java)
            stopService(intent)
            Log.d(TAG, "Crash Detection Service Stopped")
            isTracingStarted = false
        }

        Toast.makeText(this, "Crash detection service stopped", Toast.LENGTH_SHORT).show()
        binding.btnStartService.visibility = View.VISIBLE
        binding.btnStopService.visibility = View.GONE
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val activityManager =
            getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

//    private fun startMonitoring() {
//        if (!isTracingStarted) {
//            accelerometer?.let {
//                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
//                Log.d(TAG, "Sensor monitoring started")
//            }
//        }
//    }
//    override fun onSensorChanged(event: SensorEvent?) {
//        event?.let {
//            val x = it.values[0]
//            val y = it.values[1]
//            val z = it.values[2]
//            val magnitude = sqrt((x * x + y * y + z * z).toDouble())
//
//            if (abs(magnitude - lastMagnitude) > CRASH_THRESHOLD) {
//                lastMagnitude = magnitude
//                Log.d(TAG, "Crash detected. Magnitude: $lastMagnitude")
//                onCrashDetected()
//            } else {
//                Log.d(TAG, "No crash detected. Magnitude: $magnitude")
//            }
//        }
//    }
//
//    private fun onCrashDetected() {
//        Toast.makeText(this, "Crash Detected", Toast.LENGTH_SHORT).show()
//        Log.d(TAG, "Crash Detected")
//
//        if (isServiceRunning(CrashDetectionService::class.java)) {
//            getLocationAndSendAlert()
//            val intent = Intent(this, CrashDetectionService::class.java).apply {
//                intent.putExtra("message", message)
//                Log.d(TAG, "Crash Detected and Email Sent$message")
//            }
//            startService(intent)
//            Log.d(TAG, "Crash Detected and Email Sent")
//        } else {
//            startService()
//            getLocationAndSendAlert()
//            Log.d(TAG, "Crash detected but service not running")
//            Toast.makeText(this, "Crash detected but service not running", Toast.LENGTH_SHORT)
//                .show()
//        }
//    }
//
//    private fun getLocationAndSendAlert() {
//        if (ActivityCompat.checkSelfPermission(
//                this, Manifest.permission.ACCESS_FINE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            Log.w(TAG, "Location permission not granted.")
//            return
//        }
//
//        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
//            message = if (location != null) {
//                val latitude = location.latitude
//                val longitude = location.longitude
//                "Emergency! A possible crash has been detected. Location: https://maps.google.com/?q=$latitude,$longitude"
//            } else {
//                "Emergency! A possible crash has been detected. Location unavailable."
//            }
//            sendEmailWithRetrofit(message)
//        }
//    }
//
//    private fun sendEmailWithRetrofit(message: String) {
//        val apiKey = "Bearer $apiKeyCode"
//        val (name, senderEmail, receiverEmail) = SharedPreferencesHelper.getEmails(this)
//
//        val emailRequest = EmailRequest(
//            from = Email(
//                email = "MS_Ei6Rpx@trial-yzkq3409k224d796.mlsender.net", name = appName
//            ),
//            to = listOf(
//                Email(
//                    email = receiverEmail ?: "default@example.com", name = name
//                )
//            ),
//            subject = "Crash Detected",
//            text = message,
//            html = "<p><b>${senderEmail ?: "Unknown Sender"}</b>: $message</p>"
//        )
//
//        val call = RetrofitClient.emailApiService.sendEmail(apiKey, emailRequest)
//        call.enqueue(object : Callback<Void> {
//            override fun onResponse(call: Call<Void>, response: Response<Void>) {
//                if (response.isSuccessful) {
//                    Log.d("CrashEmail", "Crash email sent successfully")
//                } else {
//                    Log.e("CrashEmail", "Failed to send email. Response code: ${response.code()}")
//                }
//            }
//
//            override fun onFailure(call: Call<Void>, t: Throwable) {
//                Log.e("CrashEmail", "Email sending failed", t)
//            }
//        })
//    }
    //override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    //  Log.i(TAG, "Sensor accuracy: $accuracy")
    //}
}
