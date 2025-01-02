package com.example.carplaytest.main

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.mediarouter.media.MediaRouter
import androidx.recyclerview.widget.GridLayoutManager
import com.example.carplaytest.R
import com.example.carplaytest.carmaintenance.CarMaintenanceActivity
import com.example.carplaytest.carparking.CarParkingActivity
import com.example.carplaytest.cast.ScreenCastActivity
import com.example.carplaytest.databinding.ActivityMainBinding
import com.example.carplaytest.emergency.EmergencyActivity
import com.example.carplaytest.fuel.FuelEfficiencyActivity
import com.example.carplaytest.locationhistory.LocationHistoryActivity
import com.example.carplaytest.speedtracker.SpeedTrackerActivity
import com.example.carplaytest.trafficsign.TrafficSignActivity
import com.example.carplaytest.utils.ExitDialogBuilder
import com.example.carplaytest.weather.WeatherActivity
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var wifiManager: WifiManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var enableBtLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        enableBtLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    openBluetoothSettings()
                } else {
                    Toast.makeText(this, "Bluetooth was not enabled.", Toast.LENGTH_SHORT).show()
                }
            }


        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            handleNavigationItemSelected(menuItem.itemId)
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            true
        }

        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            R.string.Done,
            R.string.Cancel
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.Hamburg.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }

        val featureCards = listOf(
            FeatureCard(
                iconRes = R.drawable.ic_wifi,
                title = "Scan Wifi",
                description = "Scan for wifi devices",
                onClickAction = { openWifiDeviceList() }
            ),
            FeatureCard(
                iconRes = R.drawable.ic_bluetooth,
                title = "Bluetooth",
                description = "Search Bluetooth devices",
                onClickAction = { openBluetoothDeviceList() }
            ),
            FeatureCard(
                iconRes = R.drawable.ic_cast,
                title = "Cast",
                description = "Open Cast devices",
                onClickAction = { openCastDeviceList() }
            ),
            FeatureCard(
                iconRes = R.drawable.ic_speedtracker,
                title = "Speed Tracker",
                description = "Track your speed",
                onClickAction = { openSpeedTracker() }
            ),
            FeatureCard(
                iconRes = R.drawable.ic_weather,
                title = "Weather",
                description = "Get current weather info",
                onClickAction = { openWeather() }
            ),
            FeatureCard(
                iconRes = R.drawable.ic_parking,
                title = "Car Parking",
                description = "Save parking spots",
                onClickAction = { openCarParking() }
            ),
            FeatureCard(
                iconRes = R.drawable.ic_car,
                title = "Car Maintenance",
                description = "Track maintenance",
                onClickAction = { openCarMaintenance() }
            ),
            FeatureCard(
                iconRes = R.drawable.ic_traffic,
                title = "Traffic Signs",
                description = "Learn traffic signs",
                onClickAction = { openTrafficSign() }
            ),
            FeatureCard(
                iconRes = R.drawable.ic_emergency,
                title = "Emergency",
                description = "Emergency contacts",
                onClickAction = { openEmergency() }
            ),
//            FeatureCard(
//                iconRes = R.drawable.ic_wifi,
//                title = "Fuel Efficiency",
//                description = "Track fuel efficiency",
//                onClickAction = { openFuelEfficiency() }
//            ),
            FeatureCard(
                iconRes = R.drawable.ic_location,
                title = "Tracking History",
                description = "Track your location",
                onClickAction = { openLocationHistory() }
            )
        )

        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = FeatureCardAdapter(featureCards)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitDialog()
            }
        })
    }

    private fun handleNavigationItemSelected(itemId: Int) {
        when (itemId) {
            R.id.nav_privacy_policy -> {
                onClickPrivacyPolicy()
            }

            R.id.nav_rate_us -> {
                onClickRateUs()
            }

            R.id.nav_share_app -> {
                onClickShareApp()
            }

            R.id.nav_more_apps -> {
                moreApps()
            }

            R.id.nav_close -> {
                binding.drawerLayout.closeDrawer(GravityCompat.END)
            }

            R.id.nav_Exit -> {
                showExitDialog()
            }
        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }

    private fun moreApps() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("More Apps")
        builder.setMessage("You will be redirect to our Play store account.\nAre you sure?")
        builder.setPositiveButton("Redirect") { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.cancel()
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://search?q=pub:" + getString(R.string.developer_account_link))
                    )
                )
            } catch (ex: ActivityNotFoundException) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/search?q=3DevTech&c=apps")
                    )
                )
            }
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialogInterface: DialogInterface, i: Int -> dialogInterface.cancel() }
        builder.show()
    }

    private fun onClickRateUs() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Rate App")
        builder.setMessage("Please give your feedback about the application. We will consider your point of view at serious note.")
        builder.setPositiveButton("Rate us") { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.cancel()
            val uri =
                Uri.parse("market://details?id=com.bg.remover.android.background.eraser.editor")
            val goToMarket = Intent(Intent.ACTION_VIEW, uri)
            try {
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                startActivity(goToMarket)
            } catch (e: ActivityNotFoundException) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.bg.remover.android.background.eraser.editor")
                    )
                )
            }
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialogInterface: DialogInterface, i: Int -> dialogInterface.cancel() }
        builder.show()
    }

    private fun onClickShareApp() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Share with Friend")
        builder.setMessage("App invite link will be share with your friend. Thanks for like our app.\nAre you sure?")
        builder.setPositiveButton("Share") { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.cancel()
            val intent =
                Intent(Intent.ACTION_SEND)
            val shareBody =
                "https://play.google.com/store/apps/details?id=com.bg.remover.android.background.eraser.editor"
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_SUBJECT, "")
            intent.putExtra(Intent.EXTRA_TEXT, shareBody)
            startActivity(Intent.createChooser(intent, "Share Using"))
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialogInterface: DialogInterface, i: Int -> dialogInterface.cancel() }
        builder.show()
    }

    private fun onClickPrivacyPolicy() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Privacy Policy")
        builder.setMessage("You will be redirect to Browser to open privacy policy.\nAre you sure?")
        builder.setPositiveButton("Redirect") { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.cancel()
            startActivity(
                Intent(
                    "android.intent.action.VIEW",
                    Uri.parse(getString(R.string.policy_url))
                )
            )
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialogInterface: DialogInterface, i: Int -> dialogInterface.cancel() }
        builder.show()
    }

    private fun showExitDialog() {
        val exitDialog = ExitDialogBuilder.Builder(this).withTitle("Exit App")
            .withMessage("Are you sure you want to exit?")
            .withButtonListener("Yes", object : ExitDialogBuilder.OnOkClick {
                override fun onClick(dialogs: AlertDialog) {
                    dialogs.dismiss()
                    finish()
                }
            }).withCancelButtonListener("No", object : ExitDialogBuilder.OnCancelClick {
                override fun onCancel(dialogs: AlertDialog) {
                    dialogs.dismiss()
                }
            }).build()
        exitDialog.show()
    }

    private fun openCastDeviceList() {
        checkCastingCompatibilityAndStartCapture()
    }

    private fun checkCastingCompatibilityAndStartCapture() {
        Log.d("ScreenCastActivity", "Checking casting compatibility...")

        if (isCastingSupported()) {
            Log.d("ScreenCastActivity", "Casting is supported on this device.")

            if (isCastingOn()) {
                Log.d(
                    "ScreenCastActivity",
                    "Casting is currently active. Starting screen capture..."
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val intent = Intent(this, ScreenCastActivity::class.java)
                    startActivity(intent)
                } else {
                    Log.e("ScreenCastActivity", "Android version too low for screen capture.")
                }
            } else {
                Log.d(
                    "ScreenCastActivity",
                    "Casting is not currently active. Showing casting dialog."
                )
                showCastingDialog()
            }
        } else {
            Log.e("ScreenCastActivity", "Casting is not supported or enabled on this device.")
            showCastingNotSupportedDialog()
        }
    }

    private fun isCastingOn(): Boolean {
        Log.d("ScreenCastActivity", "Checking if casting is on...")
        val mediaRouter = MediaRouter.getInstance(this)
        val routes = mediaRouter.routes
        val isCastingActive = routes.any { route -> route.isEnabled && route.isSelected }
        Log.d("ScreenCastActivity", "Casting active: $isCastingActive")
        return isCastingActive
    }

    private fun isCastingSupported(): Boolean {
        Log.d("ScreenCastActivity", "Checking if casting is supported on this device...")
        return try {
            val intent = Intent(Settings.ACTION_CAST_SETTINGS)
            val isSupported = intent.resolveActivity(packageManager) != null
            Log.d("ScreenCastActivity", "Casting supported: $isSupported")
            isSupported
        } catch (e: Exception) {
            Log.e("ScreenCastActivity", "Error checking if casting is supported: ${e.message}")
            false
        }
    }

    private fun showCastingNotSupportedDialog() {
        Log.d("ScreenCastActivity", "Showing 'Casting Not Supported' dialog...")
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setTitle("Casting Not Supported")
        builder.setMessage("This device does not support casting or casting is not enabled.")
        builder.setPositiveButton("Go to Settings") { _, _ -> openCastSettings() }
        builder.setNegativeButton("OK") { _, _ -> finish() }
        builder.create().show()
    }

    private fun showCastingDialog() {
        Log.d("ScreenCastActivity", "Showing 'Enable Casting' dialog...")
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setTitle("Casting")
        builder.setMessage("Casting is available but not yet enabled. Would you like to enable it?")
        builder.setPositiveButton("Enable") { _, _ -> openCastSettings() }
        builder.setNegativeButton("Cancel") { _, _ -> finish() }
        builder.create().show()
    }

    private fun openCastSettings() {
        Log.d("ScreenCastActivity", "Opening cast settings...")
        try {
            val intent = Intent(Settings.ACTION_CAST_SETTINGS)
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Log.e("ScreenCastActivity", "Casting settings activity not found.")
                showCastingNotSupportedDialog()
            }
        } catch (e: Exception) {
            Log.e("ScreenCastActivity", "Error opening cast settings: ${e.message}")
            showCastingNotSupportedDialog()
        }
    }

    private fun openBluetoothDeviceList() {
        requestBluetoothPermissions()
    }

    private fun openWifiDeviceList() {
        wifiChecker()
    }

    private fun openSpeedTracker() {
        val intent = Intent(this, SpeedTrackerActivity::class.java)
        startActivity(intent)
    }

    private fun openWeather() {
        val intent = Intent(this, WeatherActivity::class.java)
        startActivity(intent)
    }

    private fun openCarParking() {
        val intent = Intent(this, CarParkingActivity::class.java)
        startActivity(intent)
    }

    private fun openCarMaintenance() {
        val intent = Intent(this, CarMaintenanceActivity::class.java)
        startActivity(intent)
    }

    private fun openTrafficSign() {
        val intent = Intent(this, TrafficSignActivity::class.java)
        startActivity(intent)
    }

    private fun openEmergency() {
        val intent = Intent(this, EmergencyActivity::class.java)
        startActivity(intent)
    }

    private fun openFuelEfficiency() {
        val intent = Intent(this, FuelEfficiencyActivity::class.java)
        startActivity(intent)
    }

    private fun openLocationHistory() {
        val intent = Intent(this, LocationHistoryActivity::class.java)
        startActivity(intent)
    }

    private fun wifiChecker() {
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        requestWifiPermissionsForWifi()
    }

    private fun requestWifiPermissionsForWifi() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE
            )
        } else {
            listOf(
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE
            )
        }

        Dexter.withContext(this).withPermissions(permissions)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        openWifiSettings()
                    } else {
                        showPermissionDeniedDialog(
                            title = "Permission Required",
                            message = "Wi-Fi functionality requires these permissions. Please enable them in Settings."
                        )

                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?, token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            }).check()
    }

    private fun openWifiSettings() {
        val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
        startActivity(intent)
    }

    private fun requestBluetoothPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        Dexter.withContext(this).withPermissions(permissions)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        enableBluetooth()
                    } else {
                        showPermissionDeniedDialog(
                            title = "Permission Required",
                            message = "Bluetooth functionality requires these permissions. Please enable them in Settings."
                        )

                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?, token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            }).check()
    }

    private fun showPermissionDeniedDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun enableBluetooth() {
        if (bluetoothAdapter.isEnabled) {
            openBluetoothSettings()
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBtLauncher.launch(enableBtIntent)
        }
    }

    private fun openBluetoothSettings() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        startActivity(intent)
    }

}
