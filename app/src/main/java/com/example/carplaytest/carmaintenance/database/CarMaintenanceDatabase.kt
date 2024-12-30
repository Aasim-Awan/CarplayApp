package com.example.carplaytest.carmaintenance.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.carplaytest.locationhistory.database.LocationDao
import com.example.carplaytest.locationhistory.database.LocationEntity
import com.example.carplaytest.locationhistory.database.LocationLog
import com.example.carplaytest.locationhistory.database.LocationLogDao

@Database(
    entities = [CarMaintenance::class, LocationEntity::class, LocationLog::class],
    version = 1,
    exportSchema = false
)
abstract class CarPlayDB : RoomDatabase() {

    abstract fun carMaintenanceDao(): CarMaintenanceDao
    abstract fun locationDao(): LocationDao
    abstract fun locationLogDao(): LocationLogDao

    companion object {
        @Volatile
        private var INSTANCE: CarPlayDB? = null

        fun getInstance(context: Context): CarPlayDB {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext, CarPlayDB::class.java, "car_maintenance_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

