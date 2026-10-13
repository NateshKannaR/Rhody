package com.roadpulse.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TripEntity::class, TrackPointEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RoadPulseDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun trackPointDao(): TrackPointDao

    companion object {
        @Volatile
        private var INSTANCE: RoadPulseDatabase? = null

        fun getInstance(context: Context): RoadPulseDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RoadPulseDatabase::class.java,
                    "roadpulse.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
