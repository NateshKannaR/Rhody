package com.roadpulse.app.domain.model

data class GpsLocationFix(
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val speedMps: Float,
    val accuracyMeters: Float,
    val altitudeMeters: Double? = null,
    val bearingDegrees: Float? = null,
    val hasValidSpeed: Boolean = false
) {
    val speedKmh: Float
        get() = speedMps * 3.6f
}
