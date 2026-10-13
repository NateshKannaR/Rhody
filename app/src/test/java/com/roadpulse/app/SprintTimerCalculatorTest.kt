package com.roadpulse.app

import com.roadpulse.app.data.database.TrackPointEntity
import com.roadpulse.app.domain.calculator.SprintTimerCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SprintTimerCalculatorTest {

    @Test
    fun testEmptyOrInsufficientPoints() {
        val emptyResult = SprintTimerCalculator.calculateBestSprints(emptyList())
        assertNull(emptyResult.time0to60Seconds)
        assertNull(emptyResult.time0to100Seconds)

        val onePoint = listOf(createPoint(timestamp = 1000L, speedKmh = 0f))
        val oneResult = SprintTimerCalculator.calculateBestSprints(onePoint)
        assertNull(oneResult.time0to60Seconds)
        assertNull(oneResult.time0to100Seconds)
    }

    @Test
    fun testCruisingWithoutStandingStart() {
        // Cruising at 50-80 km/h without any standing start (< 3 km/h)
        val points = (0..10).map { i ->
            createPoint(timestamp = 1000L * i, speedKmh = 50f + (i * 3f))
        }
        val result = SprintTimerCalculator.calculateBestSprints(points)
        assertNull("Should not detect sprint without launch start", result.time0to60Seconds)
        assertNull("Should not detect sprint without launch start", result.time0to100Seconds)
    }

    @Test
    fun testAbortedSprintUnder60() {
        // Accelerates from 0 up to 35 km/h, then brakes back to 0 km/h
        val points = listOf(
            createPoint(timestamp = 0L, speedKmh = 0f),
            createPoint(timestamp = 1000L, speedKmh = 18f),
            createPoint(timestamp = 2000L, speedKmh = 35f),
            createPoint(timestamp = 3000L, speedKmh = 15f),
            createPoint(timestamp = 4000L, speedKmh = 0f)
        )
        val result = SprintTimerCalculator.calculateBestSprints(points)
        assertNull(result.time0to60Seconds)
        assertNull(result.time0to100Seconds)
    }

    @Test
    fun testSuccessful0to60Sprint() {
        // Standing start: 0 -> 25 -> 50 -> 75 km/h over 3 seconds
        val points = listOf(
            createPoint(timestamp = 0L, speedKmh = 0f),
            createPoint(timestamp = 1000L, speedKmh = 25f),
            createPoint(timestamp = 2000L, speedKmh = 50f),
            createPoint(timestamp = 3000L, speedKmh = 75f)
        )
        val result = SprintTimerCalculator.calculateBestSprints(points)
        assertNotNull("Should detect 0-60 sprint", result.time0to60Seconds)
        // 50 km/h at 2s, 75 km/h at 3s -> 60 km/h crossed at 2.4s
        assertEquals(2.4f, result.time0to60Seconds!!, 0.1f)
        assertNull("Should not have 0-100 sprint", result.time0to100Seconds)
    }

    @Test
    fun testFull0to100Sprint() {
        // Motorcycle launch:
        // 0s: 0 km/h
        // 1s: 25 km/h
        // 2s: 50 km/h
        // 3s: 75 km/h (crosses 60 at 2.4s)
        // 4s: 95 km/h
        // 5s: 110 km/h (crosses 100 at ~4.3s)
        val points = listOf(
            createPoint(timestamp = 0L, speedKmh = 0f),
            createPoint(timestamp = 1000L, speedKmh = 25f),
            createPoint(timestamp = 2000L, speedKmh = 50f),
            createPoint(timestamp = 3000L, speedKmh = 75f),
            createPoint(timestamp = 4000L, speedKmh = 95f),
            createPoint(timestamp = 5000L, speedKmh = 110f)
        )
        val result = SprintTimerCalculator.calculateBestSprints(points)
        assertNotNull(result.time0to60Seconds)
        assertNotNull(result.time0to100Seconds)
        assertEquals(2.4f, result.time0to60Seconds!!, 0.1f)
        assertTrue("0-100 time should be ~4.3s", result.time0to100Seconds!! in 4.0f..4.6f)
    }

    private fun createPoint(timestamp: Long, speedKmh: Float): TrackPointEntity {
        return TrackPointEntity(
            id = 0L,
            tripId = 1L,
            timestamp = timestamp,
            latitude = 37.7749,
            longitude = -122.4194,
            speedMps = speedKmh / 3.6f,
            accuracyMeters = 5f,
            cumulativeDistanceMeters = 0.0
        )
    }
}
