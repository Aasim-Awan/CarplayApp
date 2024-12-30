package com.example.carplaytest.cast

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.mediarouter.media.MediaRouter
import com.example.carplaytest.databinding.ActivityScreenCastBinding

class ScreenCastActivity : AppCompatActivity() {

    private var navigatingToSettings = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityScreenCastBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (isCastingConnected()) {
            showCastingConnectedDialog()
        } else if (isCastingSupported()) {
            showUseCaseDialog()
        } else {
            showCastingNotSupportedDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        if (navigatingToSettings) {
            finish() // Finish the activity when returning from settings
        }
    }

    private fun isCastingConnected(): Boolean {
        val mediaRouter = MediaRouter.getInstance(this)
        val routes = mediaRouter.routes
        return routes.any { route -> route.isEnabled && route.isSelected && route.deviceType != MediaRouter.RouteInfo.DEVICE_TYPE_UNKNOWN }
    }

    private fun isCastingSupported(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_CAST_SETTINGS)
            intent.resolveActivity(packageManager) != null
        } catch (e: Exception) {
            false
        }
    }

    private fun showUseCaseDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setTitle("Casting Feature Detected")
        builder.setMessage("Your device supports casting. Would you like to open the settings to enable casting?")
        builder.setPositiveButton("Go to Settings") { _, _ -> openCastSettings() }
        builder.setNegativeButton("Cancel") { _, _ -> finish() }
        builder.create().show()
    }

    private fun showCastingNotSupportedDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setTitle("Casting Not Supported")
        builder.setMessage("This device does not support casting.")
        builder.setPositiveButton("OK") { _, _ -> finish() }
        builder.create().show()
    }

    private fun showCastingConnectedDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setTitle("Casting Connected")
        builder.setMessage("Your device is currently casting to another device. Would you like to manage the casting settings?")
        builder.setPositiveButton("Go to Settings") { _, _ -> openCastSettings() }
        builder.setNegativeButton("Cancel") { _, _ -> finish() }
        builder.create().show()
    }

    private fun openCastSettings() {
        try {
            val intent = Intent(Settings.ACTION_CAST_SETTINGS)
            if (intent.resolveActivity(packageManager) != null) {
                navigatingToSettings = true
                startActivity(intent)
            } else {
                showCastingNotSupportedDialog()
            }
        } catch (e: Exception) {
            Log.e("ScreenCastActivity", "Error opening cast settings: ${e.message}")
            showCastingNotSupportedDialog()
        }
    }
}
