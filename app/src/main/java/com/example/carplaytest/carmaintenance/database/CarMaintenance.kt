package com.example.carplaytest.carmaintenance.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "car_maintenance_table")
data class CarMaintenance(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val maintenanceName: String,
    val millage: Int,
    val date: String,
    val interval: Int,
    val cost: Double,
    val vendorCodes: String,
    val description: String,
    val iconRes: Int
)

