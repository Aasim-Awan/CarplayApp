//package com.example.carplaytest.wifi
//
//import android.Manifest
//import android.content.Context
//import android.content.Intent
//import android.net.wifi.ScanResult
//import android.net.wifi.WifiManager
//import android.os.Build
//import android.os.Bundle
//import android.widget.ArrayAdapter
//import android.widget.ListView
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import com.example.carplaytest.R
//import com.karumi.dexter.Dexter
//import com.karumi.dexter.MultiplePermissionsReport
//import com.karumi.dexter.PermissionToken
//import com.karumi.dexter.listener.PermissionRequest
//import com.karumi.dexter.listener.multi.MultiplePermissionsListener
//
//class WifiListActivity : AppCompatActivity() {
//
//    private lateinit var wifiManager: WifiManager
//    private lateinit var wifiListView: ListView
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_wifi_list)
//
//        wifiListView = findViewById(R.id.wifiListView)
//        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
//
//        requestWifiPermissions()
//    }
//
//
//
//    private fun scanForWifiNetworks() {
//        // Check if Wi-Fi is enabled, and enable it if not
//        if (!wifiManager.isWifiEnabled) {
//            wifiManager.isWifiEnabled = true
//            Toast.makeText(this, "Enabling Wi-Fi...", Toast.LENGTH_SHORT).show()
//        }
//
//        // Start scanning for available networks
//        wifiManager.startScan()
//
//        // Get the list of available Wi-Fi networks
//        val scanResults: List<ScanResult> = wifiManager.scanResults
//        val availableNetworks = scanResults.map { it.SSID }.filter { it.isNotEmpty() }
//
//        // Display the available networks in a ListView
//        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, availableNetworks)
//        wifiListView.adapter = adapter
//
//        // Intent to open Wi-Fi settings after displaying available networks
//        openWifiSettings()
//    }
//
//    private fun openWifiSettings() {
//        val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
//        startActivity(intent)
//    }
//}
