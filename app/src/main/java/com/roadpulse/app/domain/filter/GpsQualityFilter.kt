package com.roadpulse.app.domain.filter

import com.roadpulse.app.domain.calculator.TripCalculator
import com.roadpulse.app.domain.model.GpsLocationFix
import kotlin.math.abs

/**
 * Filter configuration with configurable thresholds.
 *
 * Trade-offs:
 * - maxAcceptableAccuracyMeters: 25m balances rejecting indoor/reflected multipath spikes
 *   without dropping fixes in urban canyons or forested roads.
 * - maxRealisticSpeedMps: 70 m/s (252 km/h) allows high-performance highway/track riding
 *   while eliminating cellular tower jump teleportation.
 * - minMovingSpeedMps: 0.5 m/s (1.8 km/h) prevents GPS jitter when stopped at traffic lights
 *   from falsely inflating cumulative trip distance.
 * - maxLocationStalenessMs: 10,000ms rejects cached/stale fixes from prior locations.
 */
data class FilterConfig(
    val maxAcceptableAccuracyMeters: Float = 25.0f,
    val maxRealisticSpeedMps: Float = 70.0f,
    val minMovingSpeedMps: Float = 0.5f,
    val minStationaryDistanceMeters: Double = 1.5,
    val maxLocationStalenessMs: Long = 10_000L
)

enum class FilterRejectionReason {
    NONE,
    INVALID_COORDINATES,
    POOR_ACCURACY,
    STALE_LOCATION,
    DUPLICATE_OR_OUT_OF_ORDER_TIMESTAMP,
    IMPLAUSIBLE_SPEED,
    IMPOSSIBLE_GPS_JUMP
}

data class FilterResult(
    val accepted: Boolean,
    val reason: FilterRejectionReason = FilterRejectionReason.NONE,
    val sanitizedFix: GpsLocationFix? = null,
    val isStationaryNoise: Boolean = false
)

class GpsQualityFilter(
    private val config: FilterConfig = FilterConfig()
) {
    /**
     * Evaluates an incoming GPS fix against quality criteria and previous accepted fix.
     */
    fun evaluate(
        incomingFix: GpsLocationFix,
        previousAcceptedFix: GpsLocationFix?,
        currentTimeMs: Long = System.currentTimeMillis()
    ): FilterResult {
        // 1. Coordinate range validation
        if (incomingFix.latitude.isNaN() || incomingFix.longitude.isNaN() ||
            abs(incomingFix.latitude) > 90.0 || abs(incomingFix.longitude) > 180.0
        ) {
            return FilterResult(accepted = false, reason = FilterRejectionReason.INVALID_COORDINATES)
        }

        // 2. Horizontal accuracy validation
        if (incomingFix.accuracyMeters <= 0f || incomingFix.accuracyMeters > config.maxAcceptableAccuracyMeters) {
            return FilterResult(accepted = false, reason = FilterRejectionReason.POOR_ACCURACY)
        }

        // 3. Staleness validation
        val ageMs = currentTimeMs - incomingFix.timestamp
        if (ageMs > config.maxLocationStalenessMs) {
            return FilterResult(accepted = false, reason = FilterRejectionReason.STALE_LOCATION)
        }

        // 4. Consecutive fix checks (if we have a prior fix)
        if (previousAcceptedFix != null) {
            // Timestamp progression
            val timeDeltaMs = incomingFix.timestamp - previousAcceptedFix.timestamp
            if (timeDeltaMs <= 0L) {
                return FilterResult(
                    accepted = false,
                    reason = FilterRejectionReason.DUPLICATE_OR_OUT_OF_ORDER_TIMESTAMP
                )
            }

            // Geodesic distance calculation
            val distanceMeters = TripCalculator.calculateDistanceMeters(
                previousAcceptedFix.latitude,
                previousAcceptedFix.longitude,
                incomingFix.latitude,
                incomingFix.longitude
            )

            val timeDeltaSeconds = timeDeltaMs / 1000.0
            val derivedSpeedMps = (distanceMeters / timeDeltaSeconds).toFloat()

            // 5. Implausible speed & Impossible GPS jump
            if (incomingFix.hasValidSpeed && incomingFix.speedMps > config.maxRealisticSpeedMps) {
                return FilterResult(accepted = false, reason = FilterRejectionReason.IMPLAUSIBLE_SPEED)
            }
            if (derivedSpeedMps > config.maxRealisticSpeedMps) {
                return FilterResult(accepted = false, reason = FilterRejectionReason.IMPOSSIBLE_GPS_JUMP)
            }

            // 6. Stationary noise check (at red light or parked)
            val isSpeedStationary = incomingFix.speedMps < config.minMovingSpeedMps
            val isDistanceMicro = distanceMeters < config.minStationaryDistanceMeters
            if (isSpeedStationary && isDistanceMicro) {
                // Accept point to update current position/timestamp, but flag as stationary noise
                return FilterResult(
                    accepted = true,
                    reason = FilterRejectionReason.NONE,
                    sanitizedFix = incomingFix.copy(speedMps = 0f),
                    isStationaryNoise = true
                )
            }
        } else {
            // First fix: check standalone speed validity
            if (incomingFix.hasValidSpeed && incomingFix.speedMps > config.maxRealisticSpeedMps) {
                return FilterResult(accepted = false, reason = FilterRejectionReason.IMPLAUSIBLE_SPEED)
            }
        }

        return FilterResult(
            accepted = true,
            reason = FilterRejectionReason.NONE,
            sanitizedFix = incomingFix,
            isStationaryNoise = false
        )
    }
}
