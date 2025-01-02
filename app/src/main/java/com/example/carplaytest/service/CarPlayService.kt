package com.example.carplaytest.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.example.carplaytest.R
import com.example.carplaytest.carmaintenance.database.CarPlayDB
import com.example.carplaytest.emergency.Email
import com.example.carplaytest.emergency.EmailRequest
import com.example.carplaytest.emergency.EmergencyActivity.Companion.CRASH_THRESHOLD
import com.example.carplaytest.emergency.RetrofitClient
import com.example.carplaytest.locationhistory.database.LocationDao
import com.example.carplaytest.locationhistory.database.LocationLog
import com.example.carplaytest.locationhistory.database.LocationLogDao
import com.example.carplaytest.main.MainActivity
import com.example.carplaytest.utils.SessionManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.abs
import kotlin.math.sqrt

class CarPlayService : Service(), SensorEventListener {

    private val locationChannelId = "location_tracking_channel"

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationDao: LocationDao
    private lateinit var locationLogDao: LocationLogDao
    private lateinit var sensorManager: SensorManager
    private lateinit var sessionManager: SessionManager
    private var sessionId: Long = 0
    private var accelerometer: Sensor? = null
    private var lastMagnitude = 0.0
    private val apiKeyCode = "mlsn.c026a8fd0429e201de04dc01406590239042cfa57c69387b35ca4686bc5d9f8e"
    private val appName = String.format("%s", R.string.app_name)
    private var speedLimit: Float = 0f
    private var alarmPlayer: MediaPlayer? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("CarPlayService", "Service created")

        initializeDatabase()
        setupLocationTracking()

        sessionManager = SessionManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                location?.let { handleSpeed(it) }
                Log.d("CarPlayService", "Location updated: $location")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(locationChannelId, "Location Tracking")
            startForeground(
                1,
                buildNotification("Tracking Location", "Tracking your location in the background")
            )
        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val isLocationTrackingEnabled =
            sessionManager.getBoolean(SessionManager.SessionKeys.IS_LOCATION_TRACKING_ENABLED)
        Log.d("CarPlayService5", "Received isLocationTrackingEnabled: $isLocationTrackingEnabled")
        val isCrashMonitoringEnabled =
            sessionManager.getBoolean(SessionManager.SessionKeys.IS_CRASH_MONITORING_ENABLED)
        Log.d("CarPlayService5", "Received isCrashMonitoringEnabled: $isCrashMonitoringEnabled")
        val isSpeedTrackingEnabled =
            sessionManager.getBoolean(SessionManager.SessionKeys.IS_SPEED_TRACKING_ENABLED)
        Log.d("CarPlayService5", "Received isSpeedTrackingEnabled: $isSpeedTrackingEnabled")


        sessionId = sessionManager.getLong("activeLocationEntityId", 0)
        Log.d("CarPlayService2", "Received sessionId: $sessionId")
        if (sessionId != 0L) {
            if (isLocationTrackingEnabled) {
                startLocationTracking()
                Log.d("CarPlayService", "Starting location tracking")
            } else if (!isLocationTrackingEnabled) {
                stopLocationTracking()
                Log.d("CarPlayService", "Stopping location tracking")
            }
        }

        if (isCrashMonitoringEnabled) {
            startMonitoring()
            Log.d("CarPlayService", "Sensor monitoring started")
        } else if (!isCrashMonitoringEnabled) {
            stopMonitoring()
            Log.d("CarPlayService", "Sensor monitoring stopped")
        }


        speedLimit = sessionManager.getFloat("SAVED_SPEED_LIMIT", 0f)
        Log.d("CarPlayService", "Received speed limit: $speedLimit")

        if (isSpeedTrackingEnabled) {
            startLocationUpdates()
            Log.d("CarPlayService", "Starting speed tracking")
        } else if (!isSpeedTrackingEnabled) {
            Log.d("CarPlayService", "Stopping speed tracking")
            stopSpeedTracking()
        }

        return START_STICKY
    }


// ------------------------------- Initialization -------------------------------

    private fun initializeDatabase() {
        val appDatabase = Room.databaseBuilder(
            applicationContext, CarPlayDB::class.java, "app-database"
        ).build()
        locationDao = appDatabase.locationDao()
        locationLogDao = appDatabase.locationLogDao()
    }

    private fun createNotificationChannel(channelId: String, channelName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, message: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, locationChannelId).setContentTitle(title)
            .setContentText(message).setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent).build()
    }

    private fun updateNotification(title: String, content: String) {
        val notification = buildNotification(title, content)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }


// ------------------------------- Location Tracking -------------------------------

    private fun setupLocationTracking() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 120_000L)
            .setMinUpdateDistanceMeters(100f).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    GlobalScope.launch(Dispatchers.IO) {
                        saveLocation(location.latitude, location.longitude)
                    }
                }
            }
        }
    }

    private suspend fun saveLocation(latitude: Double, longitude: Double) {
        val geocoder = Geocoder(this, java.util.Locale.getDefault())
        val addressList = geocoder.getFromLocation(latitude, longitude, 1)
        val address = addressList?.get(0)?.getAddressLine(0)

        val locationLog = LocationLog(
            latitude = latitude,
            longitude = longitude,
            timestamp = System.currentTimeMillis(),
            address = address,
            trackLocationId = sessionId
        )
        Log.d("CarPlayService2", "Saving location: $locationLog")
        locationLogDao.insertLocationLog(locationLog)

    }

    private fun startLocationTracking() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback, mainLooper
        )
        setupLocationTracking()
    }


    private fun stopLocationTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }


//------------------------------ Crash Detection -----------------------

    private fun startMonitoring() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            Log.d("CarPlayService", "Sensor monitoring started")
        } ?: Log.e("CarPlayService", "Accelerometer sensor not available")
    }

    private fun stopMonitoring() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]
            val magnitude = sqrt((x * x + y * y + z * z).toDouble())

            if (abs(magnitude - lastMagnitude) > CRASH_THRESHOLD) {
                lastMagnitude = magnitude
                Log.d("CarPlayService", "Crash detected. Magnitude: $lastMagnitude")
                Toast.makeText(this, "Crash Detected", Toast.LENGTH_SHORT).show()
                Log.d("CarPlayService", "Crash Detected")
                getLocationAndSendAlert()
            } else {
                Log.d("CarPlayService", "No crash detected. Magnitude: $magnitude")
            }
        }
    }

    private fun getLocationAndSendAlert() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("CarPlayService", "Location permission not granted.")
            // Optionally, you can request permission here
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            val message = if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                "Emergency! A possible crash has been detected. Location: https://maps.google.com/?q=$latitude,$longitude"
            } else {
                "Emergency! A possible crash has been detected. Location unavailable."
            }
            sendEmailWithRetrofit(message)
        }.addOnFailureListener { exception ->
            Log.e("CarPlayService", "Error getting location", exception)
        }
    }

    private fun sendEmailWithRetrofit(message: String) {
        val apiKey = "Bearer $apiKeyCode"
        val (name, senderEmail, receiverEmail) = sessionManager.getEmails()

        val emailRequest = EmailRequest(
            from = Email(
                email = "MS_Ei6Rpx@trial-yzkq3409k224d796.mlsender.net", name = appName
            ),
            to = listOf(
                Email(
                    email = receiverEmail ?: "default@example.com", name = name
                )
            ),
            subject = "Crash Detected",
            text = message,
            html = "<p><b>${senderEmail ?: "Unknown Sender"}</b>: $message</p>"
        )

        val call = RetrofitClient.emailApiService.sendEmail(apiKey, emailRequest)

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("CrashEmail", "Crash email sent successfully")
                } else {
                    Log.e("CrashEmail", "Failed to send email. Response code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("CrashEmail", "Email sending failed", t)
                Toast.makeText(this@CarPlayService, "Email sending failed", Toast.LENGTH_SHORT)
            }
        })
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.i("CarPlayService", "Sensor accuracy: $accuracy")
    }

// ----------------------------- Speed Tracking -----------------------

    private fun startLocationUpdates() {
        Log.d("CarPlayService", "Starting location updates")
        val locationRequest = LocationRequest.create().apply {
            interval = 2000
            fastestInterval = 1000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback, mainLooper
            )
        } else {
            Log.e("CarPlayService", "Location permission not granted")
            stopSelf()
        }
    }

    private fun stopSpeedTracking() {
        Log.d("CarPlayService", "Stopping Tracking updates")
        fusedLocationClient.removeLocationUpdates(locationCallback)
        sessionManager.saveBoolean(SessionManager.SessionKeys.IS_SPEED_TRACKING_ENABLED, false)
        Log.d(
            "CarPlayService",
            "Speed Tracking Stopped${sessionManager.getBoolean(SessionManager.SessionKeys.IS_SPEED_TRACKING_ENABLED)}"
        )
        stopAlarm()
    }

    private fun handleSpeed(location: Location) {
        val speed = location.speed * 3.6f
        Log.d("CarPlayService", "Speed: $speed km/h")

        if (speed > speedLimit && speedLimit > 0) {
            Log.d("CarPlayService", "Speed exceeded limit: $speed km/h > $speedLimit km/h")
            if (alarmPlayer == null) {
                playAlarm()
            }
        } else {
            stopAlarm()
        }
    }

    private fun playAlarm() {
        Log.d("CarPlayService", "Playing alarm")
        val alarmUri = Uri.parse("android.resource://${packageName}/${R.raw.alert}")
        alarmPlayer = MediaPlayer.create(this, alarmUri)
        alarmPlayer?.isLooping = true
        alarmPlayer?.start()
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


// ------------------------------- Cleanup -------------------------------

    override fun onDestroy() {
        super.onDestroy()
        stopLocationTracking()
        stopMonitoring()
        stopSpeedTracking()
        Log.d("CarPlayService", "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder {
        return LocationBinder()
    }

    inner class LocationBinder : Binder() {
        fun getService(): CarPlayService = this@CarPlayService
    }
}