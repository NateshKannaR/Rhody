package com.roadpulse.app.domain.model

data class TripMetrics(
    val currentSpeedKmh: Float = 0f,
    val maxSpeedKmh: Float = 0f,
    val avgMovingSpeedKmh: Float = 0f,
    val avgOverallSpeedKmh: Float = 0f,
    val totalDistanceMeters: Double = 0.0,
    val elapsedTimeMs: Long = 0L,
    val movingDurationMs: Long = 0L,
    val pauseDurationMs: Long = 0L,
    val accuracyMeters: Float = 0f,
    val gpsStatus: GpsStatus = GpsStatus.SEARCHING,
    val activeTripId: Long? = null,
    val tripStatus: TripStatus = TripStatus.IDLE,
    val recordedPointsCount: Int = 0,
    val currentLatitude: Double? = null,
    val currentLongitude: Double? = null
) {
    val totalDistanceKm: Double
        get() = totalDistanceMeters / 1000.0

    val currentSpeedMps: Float
        get() = currentSpeedKmh / 3.6f

    val isRecording: Boolean
        get() = tripStatus == TripStatus.RECORDING

    val isPaused: Boolean
        get() = tripStatus == TripStatus.PAUSED
}
