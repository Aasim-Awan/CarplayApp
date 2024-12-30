package com.example.carplaytest.carmaintenance

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.carplaytest.R
import com.example.carplaytest.carmaintenance.database.CarMaintenanceViewModel
import com.example.carplaytest.databinding.ActivityCarMaintenanceBinding
import com.example.carplaytest.databinding.ServiceDetailsSheetBinding
import com.example.carplaytest.notifications.TaskNotificationWorker
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.example.carplaytest.carmaintenance.database.CarMaintenance as CarMaintenance1

class CarMaintenanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCarMaintenanceBinding
    private val viewModel: CarMaintenanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarMaintenanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val maintenanceItems = resources.getStringArray(R.array.Car_Maintenances).toList()

        val icons = listOf(
            R.drawable.tyre,
            R.drawable.oilfilter,
            R.drawable.ic_engineblock,
            R.drawable.ic_belt_chain,
            R.drawable.ic_sparkplug,
            R.drawable.ic_radiator,
            R.drawable.ic_transmission,
            R.drawable.ic_clutch,
            R.drawable.ic_driveshatf,
            R.drawable.ic_breakshoo,
            R.drawable.ic_breakspad,
            R.drawable.ic_breakclipper,
            R.drawable.ic_shockabsorber,
            R.drawable.ic_sturts,
            R.drawable.ic_controlarm,
            R.drawable.ic_startermotor,
            R.drawable.ic_powerstreeringpump,
            R.drawable.ic_coolingsystem,
            R.drawable.ic_waterpump,
            R.drawable.ic_fuelpump,
            R.drawable.ic_fuelfilter,
            R.drawable.ic_alternator,
            R.drawable.ic_startermotor,
            R.drawable.ic_catalyticconverter,
            R.drawable.ic_wheelbearing,
            R.drawable.ic_car_wash
        )

        val adapter = MaintenanceAdapter(maintenanceItems, icons) { name, iconRes ->
            openMaintenanceForm(name, iconRes)
        }

        binding.servicesRecycler.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.servicesRecycler.adapter = adapter

        val adapter2 = ListRecyclerAdapter(onViewClicked = { maintenance ->
            val dialog = BottomSheetDialog(this)
            val sheetBinding = ServiceDetailsSheetBinding.inflate(layoutInflater)

            sheetBinding.signImage.setImageResource(maintenance.iconRes)
            sheetBinding.signTitle.text = maintenance.maintenanceName
            sheetBinding.serviceDescription.text = "Description: ${maintenance.description}"
            sheetBinding.serviceMillage.text = "Millage: ${maintenance.millage}"
            sheetBinding.signRules.text = "Interval: ${maintenance.interval}"
            sheetBinding.serviceCost.text = "Cost: ${maintenance.cost}"
            sheetBinding.serviceVendorCodes.text = "Vendor Codes: ${maintenance.vendorCodes}"
            sheetBinding.serviceDate.text = "Date: ${maintenance.date}"

            dialog.setContentView(sheetBinding.root)
            dialog.show()
            Toast.makeText(this, "View Clicked", Toast.LENGTH_SHORT).show()
        }, onUpdateClicked = { maintenance ->
            val intent = Intent(this, MaintenanceFormActivity::class.java).apply {
                putExtra("MAINTENANCE_ID", maintenance.id)
                Log.d("MaintenanceFormActivity", "Maintenance ID: ${maintenance.id}")
                putExtra("NAME", maintenance.maintenanceName)
                putExtra("MILLAGE", maintenance.millage.toString())
                Log.d("MaintenanceFormActivity", "Maintenance Millage: ${maintenance.millage}")
                putExtra("DATE", maintenance.date)
                putExtra("INTERVAL", maintenance.interval)
                putExtra("COST", maintenance.cost)
                putExtra("VENDOR_CODES", maintenance.vendorCodes)
                putExtra("DESCRIPTION", maintenance.description)
                putExtra("IMAGE_URI", maintenance.iconRes)
            }
            startActivity(intent)
        }, onDeleteClicked = { maintenance ->
            deleteMaintenance(maintenance)
        })

        binding.listRecycler.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.listRecycler.adapter = adapter2

        viewModel.allMaintenances.observe(this) { maintenance ->
            val sortedMaintenances = maintenance.sortedByDescending {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(it.date)
            }
            adapter2.submitList(sortedMaintenances)
            checkForUpcomingMaintenance(maintenance)
        }

        binding.backArrow.setOnClickListener {
            finish()
        }
    }

    private fun checkForUpcomingMaintenance(maintenances: List<CarMaintenance1>) {
        val currentDate = Calendar.getInstance()
        val upcomingThreshold = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }

        Log.d("MaintenanceCheck", "Current Date: ${currentDate.time}")
        Log.d("MaintenanceCheck", "Upcoming Threshold Date: ${upcomingThreshold.time}")

        val upcomingMaintenances = maintenances.filter { maintenance ->
            val maintenanceDate = parseDate(maintenance.date)
            maintenanceDate?.let {
                Log.d(
                    "MaintenanceCheck",
                    "Checking Maintenance: ${maintenance.maintenanceName}, Date: $it"
                )
                it.after(currentDate.time) && it.before(upcomingThreshold.time)
            } ?: false
        }

        if (upcomingMaintenances.isNotEmpty()) {
            Log.d("MaintenanceCheck", "Upcoming Maintenance Found: ${upcomingMaintenances.size}")
            upcomingMaintenances.forEach {
                Log.d(
                    "MaintenanceCheck",
                    "Upcoming Maintenance: ${it.maintenanceName}, Date: ${it.date}"
                )
            }
            scheduleNotificationsForUpcomingMaintenance(upcomingMaintenances)
        } else {
            Log.d("MaintenanceCheck", "No Upcoming Maintenance Found")
        }
    }

    private fun scheduleNotificationsForUpcomingMaintenance(maintenances: List<CarMaintenance1>) {
        for (maintenance in maintenances) {
            val maintenanceDate = parseDate(maintenance.date)
            val maintenanceTime = maintenanceDate?.time ?: continue
            val currentTime = System.currentTimeMillis()

            Log.d(
                "NotificationScheduler",
                "Scheduling Notification for: ${maintenance.maintenanceName}"
            )
            Log.d(
                "NotificationScheduler",
                "Current Time: $currentTime, Maintenance Time: $maintenanceTime"
            )

            if (maintenanceTime > currentTime) {
                val delay = maintenanceTime - currentTime
                Log.d("NotificationScheduler", "Notification Delay: $delay ms")

                val workData = workDataOf(
                    "MAINTENANCE_NAME" to maintenance.maintenanceName,
                    "TASK_DATE" to maintenance.date
                )

                val workRequest =
                    OneTimeWorkRequestBuilder<TaskNotificationWorker>().setInputData(workData)
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS).build()

                WorkManager.getInstance(this).enqueue(workRequest)
                Log.d(
                    "NotificationScheduler",
                    "Work Request Enqueued for: ${maintenance.maintenanceName}"
                )
            }
        }
    }

    private fun parseDate(dateString: String): Date? {
        return try {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateString).also {
                Log.d("DateParser", "Parsed Date: $it from $dateString")
            }
        } catch (e: Exception) {
            Log.e("DateParser", "Error Parsing Date: $dateString", e)
            null
        }
    }

    private fun openMaintenanceForm(name: String, iconRes: Int) {
        val intent = Intent(this, MaintenanceFormActivity::class.java).apply {
            putExtra("NAME", name)
            putExtra("IMAGE_URI", iconRes)
        }
        startActivity(intent)
    }

    private fun deleteMaintenance(maintenance: CarMaintenance1) {
        val dialog = AlertDialog.Builder(this).setTitle("Delete Maintenance")
            .setMessage("Are you sure you want to delete ${maintenance.maintenanceName}?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.delete(maintenance)
                Toast.makeText(
                    this, "Maintenance deleted: ${maintenance.maintenanceName}", Toast.LENGTH_SHORT
                ).show()
            }.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }.create()

        dialog.show()
    }

}