//package com.example.carplaytest.bluetooth
//
//import android.Manifest
//import android.bluetooth.BluetoothAdapter
//import android.content.Intent
//import android.os.Build
//import android.os.Bundle
//import android.widget.Toast
//import androidx.activity.result.ActivityResultLauncher
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.appcompat.app.AppCompatActivity
//import com.example.carplaytest.R
//import com.karumi.dexter.Dexter
//import com.karumi.dexter.MultiplePermissionsReport
//import com.karumi.dexter.PermissionToken
//import com.karumi.dexter.listener.PermissionRequest
//import com.karumi.dexter.listener.multi.MultiplePermissionsListener
//
//class BluetoothListActivity : AppCompatActivity() {
//
//    private lateinit var bluetoothAdapter: BluetoothAdapter
//    private lateinit var enableBtLauncher: ActivityResultLauncher<Intent>
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_device_bluetooth)
//
//        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
//        enableBtLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//            if (result.resultCode == RESULT_OK) {
//                openBluetoothSettings()
//            } else {
//                Toast.makeText(this, "Bluetooth was not enabled.", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        requestBluetoothPermissions()
//    }
//
//    private fun requestBluetoothPermissions() {
//        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            listOf(
//                Manifest.permission.BLUETOOTH,
//                Manifest.permission.BLUETOOTH_ADMIN,
//                Manifest.permission.BLUETOOTH_SCAN,
//                Manifest.permission.BLUETOOTH_CONNECT,
//                Manifest.permission.ACCESS_FINE_LOCATION
//            )
//        } else {
//            listOf(
//                Manifest.permission.BLUETOOTH,
//                Manifest.permission.BLUETOOTH_ADMIN,
//                Manifest.permission.BLUETOOTH_SCAN,
//                Manifest.permission.BLUETOOTH_CONNECT,
//                Manifest.permission.ACCESS_FINE_LOCATION
//            )
//        }
//
//        Dexter.withContext(this).withPermissions(permissions)
//            .withListener(object : MultiplePermissionsListener {
//                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
//                        enableBluetooth()
//                }
//
//                override fun onPermissionRationaleShouldBeShown(
//                    permissions: MutableList<PermissionRequest>?, token: PermissionToken?
//                ) {
//                    token?.continuePermissionRequest()
//                }
//            }).check()
//    }
//
//    private fun enableBluetooth() {
//        if (bluetoothAdapter.isEnabled) {
//            openBluetoothSettings()
//        } else {
//            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//            enableBtLauncher.launch(enableBtIntent)
//        }
//    }
//
//    private fun openBluetoothSettings() {
//        val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
//        startActivity(intent)
//        finish()
//    }
//}
