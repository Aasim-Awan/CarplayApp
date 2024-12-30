package com.example.carplaytest.locationhistory.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface LocationDao {

    @Insert
    suspend fun insertLocation(location: LocationEntity): Long

    @Update
    suspend fun updateLocation(location: LocationEntity)

    @Delete
    suspend fun deleteLocation(location: LocationEntity)

    @Query("SELECT * FROM `track location` ORDER BY createdDate DESC")
    suspend fun getAllLocations(): List<LocationEntity>

    @Query("SELECT * FROM `track location` WHERE id = :id LIMIT 1")
    suspend fun getLocationById(id: Long): LocationEntity?

    @Query("DELETE FROM `track location`")
    suspend fun clearAll()
}



