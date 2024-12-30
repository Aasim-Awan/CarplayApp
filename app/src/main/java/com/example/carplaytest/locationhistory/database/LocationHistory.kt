package com.example.carplaytest.locationhistory.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Track Location")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val startTime: Long,
    var endTime: Long? = null,
    val createdDate: Long
)
