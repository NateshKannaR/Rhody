package com.roadpulse.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackPointDao {
    @Insert
    suspend fun insertPoint(point: TrackPointEntity): Long

    @Insert
    suspend fun insertPoints(points: List<TrackPointEntity>)

    @Query("SELECT * FROM track_points WHERE tripId = :tripId ORDER BY timestamp ASC")
    suspend fun getPointsForTrip(tripId: Long): List<TrackPointEntity>

    @Query("SELECT * FROM track_points WHERE tripId = :tripId ORDER BY timestamp ASC")
    fun getPointsForTripFlow(tripId: Long): Flow<List<TrackPointEntity>>

    @Query("SELECT * FROM track_points WHERE tripId = :tripId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastPointForTrip(tripId: Long): TrackPointEntity?

    @Query("SELECT COUNT(*) FROM track_points WHERE tripId = :tripId")
    suspend fun getPointCountForTrip(tripId: Long): Int

    @Query("SELECT COUNT(*) FROM track_points WHERE tripId = :tripId")
    fun getPointCountForTripFlow(tripId: Long): Flow<Int>
}
