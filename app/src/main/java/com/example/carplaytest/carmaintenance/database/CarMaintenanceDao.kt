package com.example.carplaytest.carmaintenance.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CarMaintenanceDao {

    @Query("SELECT * FROM car_maintenance_table")
    fun getAllMaintenances(): LiveData<List<CarMaintenance>>

    @Insert
    fun insert(maintenance: CarMaintenance)

    @Update
    fun update(maintenance: CarMaintenance)

    @Delete
    fun delete(maintenance: CarMaintenance)
}


