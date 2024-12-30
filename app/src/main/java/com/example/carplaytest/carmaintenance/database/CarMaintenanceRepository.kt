package com.example.carplaytest.carmaintenance.database

import android.content.Context
import androidx.lifecycle.LiveData

class CarMaintenanceRepository(context: Context) {

    private val maintenanceDao: CarMaintenanceDao = CarPlayDB.getInstance(context).carMaintenanceDao()

    fun getAllMaintenances(): LiveData<List<CarMaintenance>> {
        return maintenanceDao.getAllMaintenances()
    }

    fun insert(maintenance: CarMaintenance) {
        maintenanceDao.insert(maintenance)
    }

    fun update(maintenance: CarMaintenance) {
        maintenanceDao.update(maintenance)
    }

    fun delete(maintenance: CarMaintenance) {
        maintenanceDao.delete(maintenance)
    }
}
