package com.roadpulse.app

import com.roadpulse.app.domain.filter.FilterConfig
import com.roadpulse.app.domain.filter.FilterRejectionReason
import com.roadpulse.app.domain.filter.GpsQualityFilter
import com.roadpulse.app.domain.model.GpsLocationFix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GpsQualityFilterTest {

    private val filter = GpsQualityFilter(
        FilterConfig(
            maxAcceptableAccuracyMeters = 25.0f,
            maxRealisticSpeedMps = 70.0f,
            minMovingSpeedMps = 0.5f,
            minStationaryDistanceMeters = 1.5,
            maxLocationStalenessMs = 10_000L
        )
    )

    @Test
    fun testValidFixAccepted() {
        val fix = GpsLocationFix(
            timestamp = 10000L,
            latitude = 40.7128,
            longitude = -74.0060,
            speedMps = 15.0f, // 54 km/h
            accuracyMeters = 5.0f,
            hasValidSpeed = true
        )
        val result = filter.evaluate(fix, null, currentTimeMs = 10500L)
        assertTrue(result.accepted)
        assertEquals(FilterRejectionReason.NONE, result.reason)
    }

    @Test
    fun testInvalidCoordinatesRejected() {
        // Lat > 90
        val invalidLat = GpsLocationFix(
            timestamp = 10000L,
            latitude = 95.0,
            longitude = 10.0,
            speedMps = 10f,
            accuracyMeters = 5f
        )
        val res1 = filter.evaluate(invalidLat, null, currentTimeMs = 10000L)
        assertFalse(res1.accepted)
        assertEquals(FilterRejectionReason.INVALID_COORDINATES, res1.reason)

        // Lon > 180
        val invalidLon = GpsLocationFix(
            timestamp = 10000L,
            latitude = 10.0,
            longitude = 185.0,
            speedMps = 10f,
            accuracyMeters = 5f
        )
        val res2 = filter.evaluate(invalidLon, null, currentTimeMs = 10000L)
        assertFalse(res2.accepted)
        assertEquals(FilterRejectionReason.INVALID_COORDINATES, res2.reason)

        // NaN
        val nanFix = GpsLocationFix(
            timestamp = 10000L,
            latitude = Double.NaN,
            longitude = 10.0,
            speedMps = 10f,
            accuracyMeters = 5f
        )
        val res3 = filter.evaluate(nanFix, null, currentTimeMs = 10000L)
        assertFalse(res3.accepted)
        assertEquals(FilterRejectionReason.INVALID_COORDINATES, res3.reason)
    }

    @Test
    fun testPoorAccuracyRejected() {
        val poorFix = GpsLocationFix(
            timestamp = 10000L,
            latitude = 40.7128,
            longitude = -74.0060,
            speedMps = 10f,
            accuracyMeters = 45.0f // > 25m
        )
        val result = filter.evaluate(poorFix, null, currentTimeMs = 10000L)
        assertFalse(result.accepted)
        assertEquals(FilterRejectionReason.POOR_ACCURACY, result.reason)
    }

    @Test
    fun testStaleLocationRejected() {
        val staleFix = GpsLocationFix(
            timestamp = 10000L,
            latitude = 40.7128,
            longitude = -74.0060,
            speedMps = 10f,
            accuracyMeters = 5f
        )
        // Current time is 30 seconds later (> 10s max age)
        val result = filter.evaluate(staleFix, null, currentTimeMs = 40000L)
        assertFalse(result.accepted)
        assertEquals(FilterRejectionReason.STALE_LOCATION, result.reason)
    }

    @Test
    fun testDuplicateAndOutOfOrderTimestampsRejected() {
        val prevFix = GpsLocationFix(
            timestamp = 10000L,
            latitude = 40.7128,
            longitude = -74.0060,
            speedMps = 10f,
            accuracyMeters = 5f
        )

        // Duplicate timestamp
        val dupFix = GpsLocationFix(
            timestamp = 10000L,
            latitude = 40.7129,
            longitude = -74.0061,
            speedMps = 10f,
            accuracyMeters = 5f
        )
        val dupRes = filter.evaluate(dupFix, prevFix, currentTimeMs = 10000L)
        assertFalse(dupRes.accepted)
        assertEquals(FilterRejectionReason.DUPLICATE_OR_OUT_OF_ORDER_TIMESTAMP, dupRes.reason)

        // Out-of-order timestamp (backward time travel)
        val outOfOrderFix = GpsLocationFix(
            timestamp = 9000L,
            latitude = 40.7129,
            longitude = -74.0061,
            speedMps = 10f,
            accuracyMeters = 5f
        )
        val oooRes = filter.evaluate(outOfOrderFix, prevFix, currentTimeMs = 10000L)
        assertFalse(oooRes.accepted)
        assertEquals(FilterRejectionReason.DUPLICATE_OR_OUT_OF_ORDER_TIMESTAMP, oooRes.reason)
    }

    @Test
    fun testImplausibleSpeedAndImpossibleJumpRejected() {
        val prevFix = GpsLocationFix(
            timestamp = 10000L,
            latitude = 40.7128,
            longitude = -74.0060,
            speedMps = 10f,
            accuracyMeters = 5f
        )

        // Direct reported speed > 70 m/s (252 km/h)
        val supersonicFix = GpsLocationFix(
            timestamp = 11000L,
            latitude = 40.7129,
            longitude = -74.0060,
            speedMps = 95f, // ~342 km/h
            accuracyMeters = 5f,
            hasValidSpeed = true
        )
        val speedRes = filter.evaluate(supersonicFix, prevFix, currentTimeMs = 11000L)
        assertFalse(speedRes.accepted)
        assertEquals(FilterRejectionReason.IMPLAUSIBLE_SPEED, speedRes.reason)

        // Impossible jump: teleported 10 kilometers away in 1 second!
        val jumpFix = GpsLocationFix(
            timestamp = 11000L,
            latitude = 40.8000,
            longitude = -74.0060,
            speedMps = 10f,
            accuracyMeters = 5f,
            hasValidSpeed = false
        )
        val jumpRes = filter.evaluate(jumpFix, prevFix, currentTimeMs = 11000L)
        assertFalse(jumpRes.accepted)
        assertEquals(FilterRejectionReason.IMPOSSIBLE_GPS_JUMP, jumpRes.reason)
    }

    @Test
    fun testStationaryJitterSuppression() {
        val prevFix = GpsLocationFix(
            timestamp = 10000L,
            latitude = 40.7128000,
            longitude = -74.0060000,
            speedMps = 0f,
            accuracyMeters = 3f
        )

        // Move by 0.5 meters in 1 second with 0.1 m/s speed (stopped at red light)
        val jitterFix = GpsLocationFix(
            timestamp = 11000L,
            latitude = 40.7128030,
            longitude = -74.0060000,
            speedMps = 0.1f,
            accuracyMeters = 3f
        )

        val result = filter.evaluate(jitterFix, prevFix, currentTimeMs = 11000L)
        assertTrue(result.accepted)
        assertTrue(result.isStationaryNoise)
        assertEquals(0f, result.sanitizedFix?.speedMps ?: -1f, 0.001f)
    }
}
