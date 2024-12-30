package com.example.carplaytest.locationhistory.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import okhttp3.Address

@Entity(
    tableName = "Location Log",
    foreignKeys = [ForeignKey(
        entity = LocationEntity::class,
        parentColumns = ["id"],
        childColumns = ["trackLocationId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class LocationLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val address: String? = null,
    val trackLocationId: Long
)

