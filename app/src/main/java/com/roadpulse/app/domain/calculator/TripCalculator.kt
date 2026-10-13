package com.roadpulse.app.domain.calculator

import com.roadpulse.app.domain.model.GpsLocationFix
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class ChartPoint(
    val distanceKm: Float,
    val speedKmh: Float,
    val timestamp: Long,
    val altitudeMeters: Double? = null
)

data class AggregatedTripSummary(
    val totalDistanceMeters: Double,
    val maxSpeedMps: Float,
    val maxSpeedKmh: Float,
    val avgMovingSpeedKmh: Float,
    val avgOverallSpeedKmh: Float,
    val movingDurationMs: Long,
    val totalDurationMs: Long,
    val pauseDurationMs: Long
)

object TripCalculator {
    private const val EARTH_RADIUS_METERS = 6371000.0 // WGS84 mean radius
    const val MOVING_SPEED_THRESHOLD_KMH = 1.8f // 0.5 m/s

    /**
     * Calculates great-circle distance between two coordinates using the Haversine formula.
     * Pure Kotlin implementation that works identically on JVM and Android runtimes.
     */
    fun calculateDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        if (lat1 == lat2 && lon1 == lon2) return 0.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    fun mpsToKmh(speedMps: Float): Float {
        if (speedMps <= 0f) return 0f
        return speedMps * 3.6f
    }

    fun kmhToMps(speedKmh: Float): Float {
        if (speedKmh <= 0f) return 0f
        return speedKmh / 3.6f
    }

    fun calculateMovingAverageSpeedKmh(
        totalDistanceMeters: Double,
        movingDurationMs: Long
    ): Float {
        if (movingDurationMs <= 0L || totalDistanceMeters <= 0.0) return 0f
        val seconds = movingDurationMs / 1000.0
        val mps = totalDistanceMeters / seconds
        return (mps * 3.6).toFloat()
    }

    fun calculateOverallAverageSpeedKmh(
        totalDistanceMeters: Double,
        totalDurationMs: Long
    ): Float {
        if (totalDurationMs <= 0L || totalDistanceMeters <= 0.0) return 0f
        val seconds = totalDurationMs / 1000.0
        val mps = totalDistanceMeters / seconds
        return (mps * 3.6).toFloat()
    }

    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0L) return "00:00"
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatDistance(distanceMeters: Double): String {
        val km = distanceMeters / 1000.0
        return if (km < 1.0) {
            String.format(Locale.US, "%.0f m", distanceMeters)
        } else {
            String.format(Locale.US, "%.2f km", km)
        }
    }

    fun formatSpeed(speedKmh: Float): String {
        return String.format(Locale.US, "%.1f km/h", speedKmh)
    }

    /**
     * Builds clean chart data points (Speed vs Cumulative Distance) from raw fixes.
     * Guaranteed safe against empty or single-point datasets.
     */
    fun buildChartPoints(
        points: List<ChartPoint>,
        maxSamples: Int = 300
    ): List<ChartPoint> {
        if (points.isEmpty()) return emptyList()
        if (points.size <= maxSamples) return points

        // Downsample evenly while preserving the global maximum speed peak
        val maxSpeedIndex = points.indices.maxByOrNull { points[it].speedKmh } ?: 0
        val step = points.size.toDouble() / (maxSamples - 1)
        val sampled = ArrayList<ChartPoint>(maxSamples)

        for (i in 0 until maxSamples - 1) {
            val idx = (i * step).toInt().coerceIn(0, points.size - 1)
            sampled.add(points[idx])
        }
        sampled.add(points.last())

        // Ensure maximum point is present
        if (sampled.none { it == points[maxSpeedIndex] }) {
            sampled.add(points[maxSpeedIndex])
            sampled.sortBy { it.distanceKm }
        }

        return sampled
    }
}
