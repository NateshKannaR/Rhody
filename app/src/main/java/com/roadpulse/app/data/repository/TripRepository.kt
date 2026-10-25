package com.roadpulse.app.data.repository

import com.roadpulse.app.data.database.RoadPulseDatabase
import com.roadpulse.app.data.database.TrackPointEntity
import com.roadpulse.app.data.database.TripEntity
import com.roadpulse.app.domain.model.TripMetrics
import com.roadpulse.app.domain.model.TripStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TripRepository(
    private val database: RoadPulseDatabase
) {
    private val tripDao = database.tripDao()
    private val trackPointDao = database.trackPointDao()

    val allTrips: Flow<List<TripEntity>> = tripDao.getAllTripsFlow()

    suspend fun getAllTrips(): List<TripEntity> = withContext(Dispatchers.IO) {
        tripDao.getAllTrips()
    }

    fun getTripFlow(tripId: Long): Flow<TripEntity?> = tripDao.getTripFlow(tripId)

    fun getPointsFlow(tripId: Long): Flow<List<TrackPointEntity>> =
        trackPointDao.getPointsForTripFlow(tripId)

    suspend fun getPoints(tripId: Long): List<TrackPointEntity> = withContext(Dispatchers.IO) {
        trackPointDao.getPointsForTrip(tripId)
    }

    suspend fun getTrip(tripId: Long): TripEntity? = withContext(Dispatchers.IO) {
        tripDao.getTripById(tripId)
    }

    suspend fun getActiveOrPausedTrip(): TripEntity? = withContext(Dispatchers.IO) {
        tripDao.getActiveOrPausedTrip()
    }

    suspend fun createTrip(startTime: Long = System.currentTimeMillis()): Long = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("MMM d, yyyy - HH:mm", Locale.getDefault())
        val defaultTitle = "Trip on " + dateFormat.format(Date(startTime))

        val entity = TripEntity(
            title = defaultTitle,
            notes = null,
            startTime = startTime,
            endTime = null,
            status = TripStatus.RECORDING.name,
            totalDistanceMeters = 0.0,
            maxSpeedMps = 0f,
            avgMovingSpeedMps = 0f,
            avgOverallSpeedMps = 0f,
            movingDurationMs = 0L,
            totalDurationMs = 0L,
            pauseDurationMs = 0L
        )
        tripDao.insertTrip(entity)
    }

    suspend fun getRecentPoints(tripId: Long, limit: Int = 200): List<TrackPointEntity> = withContext(Dispatchers.IO) {
        trackPointDao.getRecentPointsForTrip(tripId, limit).reversed()
    }

    suspend fun recordTrackPoint(point: TrackPointEntity): Long = withContext(Dispatchers.IO) {
        trackPointDao.insertPoint(point)
    }

    suspend fun recordTrackPointsBatch(points: List<TrackPointEntity>) = withContext(Dispatchers.IO) {
        if (points.isNotEmpty()) {
            trackPointDao.insertPoints(points)
        }
    }

    suspend fun updateTripProgress(
        tripId: Long,
        metrics: TripMetrics,
        status: TripStatus,
        nowMs: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        val existing = tripDao.getTripById(tripId) ?: return@withContext
        val updated = existing.copy(
            status = status.name,
            totalDistanceMeters = metrics.totalDistanceMeters,
            maxSpeedMps = metrics.maxSpeedKmh / 3.6f,
            avgMovingSpeedMps = metrics.avgMovingSpeedKmh / 3.6f,
            avgOverallSpeedMps = metrics.avgOverallSpeedKmh / 3.6f,
            movingDurationMs = metrics.movingDurationMs,
            totalDurationMs = metrics.elapsedTimeMs,
            pauseDurationMs = metrics.pauseDurationMs
        )
        tripDao.updateTrip(updated)
    }

    suspend fun finishTrip(
        tripId: Long,
        metrics: TripMetrics,
        endTime: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        val existing = tripDao.getTripById(tripId) ?: return@withContext
        val updated = existing.copy(
            endTime = endTime,
            status = TripStatus.FINISHED.name,
            totalDistanceMeters = metrics.totalDistanceMeters,
            maxSpeedMps = metrics.maxSpeedKmh / 3.6f,
            avgMovingSpeedMps = metrics.avgMovingSpeedKmh / 3.6f,
            avgOverallSpeedMps = metrics.avgOverallSpeedKmh / 3.6f,
            movingDurationMs = metrics.movingDurationMs,
            totalDurationMs = metrics.elapsedTimeMs,
            pauseDurationMs = metrics.pauseDurationMs
        )
        tripDao.updateTrip(updated)
    }

    suspend fun updateTripMetadata(tripId: Long, title: String, notes: String?) = withContext(Dispatchers.IO) {
        tripDao.updateTripMetadata(tripId, title.trim(), notes?.trim())
    }

    suspend fun deleteTrip(tripId: Long) = withContext(Dispatchers.IO) {
        tripDao.deleteTripById(tripId)
    }

    suspend fun getLastPointForTrip(tripId: Long): TrackPointEntity? = withContext(Dispatchers.IO) {
        trackPointDao.getLastPointForTrip(tripId)
    }
}
