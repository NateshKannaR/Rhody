package com.roadpulse.app.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity): Long

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Delete
    suspend fun deleteTrip(trip: TripEntity)

    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun deleteTripById(tripId: Long)

    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTripById(tripId: Long): TripEntity?

    @Query("SELECT * FROM trips WHERE id = :tripId")
    fun getTripFlow(tripId: Long): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE status IN ('RECORDING', 'PAUSED') ORDER BY id DESC LIMIT 1")
    suspend fun getActiveOrPausedTrip(): TripEntity?

    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    fun getAllTripsFlow(): Flow<List<TripEntity>>

    @Query("UPDATE trips SET title = :title, notes = :notes WHERE id = :tripId")
    suspend fun updateTripMetadata(tripId: Long, title: String, notes: String?)
}
