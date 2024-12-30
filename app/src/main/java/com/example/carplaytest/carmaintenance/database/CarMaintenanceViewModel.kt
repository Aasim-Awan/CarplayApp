package com.example.carplaytest.carmaintenance.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.carplaytest.carmaintenance.database.CarMaintenanceRepository

class CarMaintenanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CarMaintenanceRepository = CarMaintenanceRepository(application)
    val allMaintenances: LiveData<List<CarMaintenance>> = repository.getAllMaintenances()

    fun insert(maintenance: CarMaintenance) {
        Thread {
            repository.insert(maintenance)
        }.start()
    }

    fun update(maintenance: CarMaintenance) {
        Thread {
            repository.update(maintenance)
        }.start()
    }

    fun delete(maintenance: CarMaintenance) {
        Thread {
            repository.delete(maintenance)
        }.start()
    }
}
