package com.example.carplaytest.locationhistory.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LocationLogDao {

    @Insert
    suspend fun insertLocationLog(locationLog: LocationLog)

    @Query("SELECT * FROM `Location Log` WHERE trackLocationId = :locationId ORDER BY timestamp DESC")
    suspend fun getLocationLogsByLocationId(locationId: Int): List<LocationLog>

    @Query("SELECT * FROM `Location Log` ORDER BY timestamp DESC")
    suspend fun getAllLocationsLogs(): List<LocationLog>

    @Query("DELETE FROM `Location Log`")
    suspend fun clearLocationLogs()
}

