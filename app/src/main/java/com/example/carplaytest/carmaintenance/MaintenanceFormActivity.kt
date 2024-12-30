package com.example.carplaytest.carmaintenance

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.carplaytest.R
import com.example.carplaytest.carmaintenance.database.CarMaintenance
import com.example.carplaytest.carmaintenance.database.CarMaintenanceViewModel
import com.example.carplaytest.databinding.ActivityMaintenanceFormBinding
import java.util.Calendar

class MaintenanceFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMaintenanceFormBinding
    private val viewModel: CarMaintenanceViewModel by viewModels()
    private var maintenanceId: Long? = null
    private var maintenanceName: String? = null
    private var iconRes: Int = R.drawable.car

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMaintenanceFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        maintenanceName = intent.getStringExtra("NAME")
        iconRes = intent.getIntExtra("IMAGE_URI", iconRes)
        maintenanceId = intent.getLongExtra("MAINTENANCE_ID", -1)
        if (maintenanceId == -1L) {
            maintenanceId = null
        }

        Log.d("MaintenanceFormActivity", "Maintenance Name: $maintenanceName")
        Log.d("MaintenanceFormActivity", "Maintenance ID: $maintenanceId")

        if (maintenanceId != null) {
            val millage = intent.getStringExtra("MILLAGE")
            Log.d("MaintenanceFormActivity", "Maintenance Millage: $millage")
            val date = intent.getStringExtra("DATE")
            val interval = intent.getIntExtra("INTERVAL", 0)
            val cost = intent.getDoubleExtra("COST", 0.0)
            val vendorCodes = intent.getStringExtra("VENDOR_CODES")
            val description = intent.getStringExtra("DESCRIPTION")

            binding.etMaintenanceName.setText(maintenanceName)
            binding.etMillage.setText(millage)
            binding.etDate.setText(date)
            binding.etInterval.setText(interval.toString())
            binding.etCost.setText(cost.toString())
            binding.etVenderCodes.setText(vendorCodes)
            binding.etDescription.setText(description)

            binding.btnSave.text = "Update"
        }

        binding.ivMaintenanceIcon.setImageResource(iconRes)
        binding.etMaintenanceName.setText(maintenanceName)

        setUpClickListener()
    }

    private fun setUpClickListener() {
        binding.etDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnSave.setOnClickListener {
            saveMaintenanceDetails()
        }

        binding.backArrow.setOnClickListener {
            finish()

        }

        binding.calender.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this, { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                binding.etDate.setText(selectedDate)
            }, year, month, day
        )
        datePickerDialog.show()
    }

    private fun saveMaintenanceDetails() {
        val iconRes = intent.getIntExtra("IMAGE_URI", iconRes)
        val maintenanceName = binding.etMaintenanceName.text.toString().trim()
        val millage = binding.etMillage.text.toString().toIntOrNull() ?: 0
        val date = binding.etDate.text.toString().trim()
        val interval = binding.etInterval.text.toString().toIntOrNull() ?: 0
        val cost = binding.etCost.text.toString().toDoubleOrNull() ?: 0.0
        val vendorCodes = binding.etVenderCodes.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        if (maintenanceName.isBlank() || date.isBlank() || interval == 0) {
            Toast.makeText(this, "Please fill all mandatory fields", Toast.LENGTH_SHORT).show()
            return
        }

        val maintenance = CarMaintenance(
            id = maintenanceId ?: 0,
            maintenanceName = maintenanceName,
            millage = millage,
            date = date,
            interval = interval,
            cost = cost,
            vendorCodes = vendorCodes,
            description = description,
            iconRes = iconRes
        )

        if (maintenanceId != null) {
            viewModel.update(maintenance)
            Toast.makeText(this, "Maintenance updated successfully!", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.insert(maintenance)
            Toast.makeText(this, "Maintenance saved successfully!", Toast.LENGTH_SHORT).show()
        }

        finish()
    }
}
