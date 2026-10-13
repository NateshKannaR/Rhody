package com.roadpulse.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val notes: String? = null,
    val startTime: Long,
    val endTime: Long? = null,
    val status: String, // RECORDING, PAUSED, FINISHED
    val totalDistanceMeters: Double = 0.0,
    val maxSpeedMps: Float = 0f,
    val avgMovingSpeedMps: Float = 0f,
    val avgOverallSpeedMps: Float = 0f,
    val movingDurationMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val pauseDurationMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalDistanceKm: Double get() = totalDistanceMeters / 1000.0
    val maxSpeedKmh: Float get() = maxSpeedMps * 3.6f
    val avgMovingSpeedKmh: Float get() = avgMovingSpeedMps * 3.6f
    val avgOverallSpeedKmh: Float get() = avgOverallSpeedMps * 3.6f
}
