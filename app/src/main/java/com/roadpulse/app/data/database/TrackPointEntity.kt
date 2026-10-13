package com.roadpulse.app.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "track_points",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tripId"]),
        Index(value = ["tripId", "timestamp"])
    ]
)
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tripId: Long,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val speedMps: Float,
    val accuracyMeters: Float,
    val altitudeMeters: Double? = null,
    val cumulativeDistanceMeters: Double = 0.0,
    val bearingDegrees: Float? = null
) {
    val speedKmh: Float get() = speedMps * 3.6f
    val cumulativeDistanceKm: Float get() = (cumulativeDistanceMeters / 1000.0).toFloat()
}
