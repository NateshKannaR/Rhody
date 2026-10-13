package com.roadpulse.app

import com.roadpulse.app.domain.calculator.ChartPoint
import com.roadpulse.app.domain.calculator.TripCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripCalculatorTest {

    @Test
    fun testZeroDistanceMovement() {
        val distance = TripCalculator.calculateDistanceMeters(
            lat1 = 37.7749, lon1 = -122.4194,
            lat2 = 37.7749, lon2 = -122.4194
        )
        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun testKnownCoordinateDistancePairs() {
        // Paris Eiffel Tower (48.8584, 2.2945) to Louvre Museum (48.8606, 2.3376)
        // Expected geodesic distance is ~3.16 km (3160 meters)
        val distance = TripCalculator.calculateDistanceMeters(
            lat1 = 48.8584, lon1 = 2.2945,
            lat2 = 48.8606, lon2 = 2.3376
        )
        assertTrue("Distance should be ~3160 meters, got $distance", distance in 3140.0..3190.0)

        // New York: Empire State Building (40.7484, -73.9857) to Times Square (40.7580, -73.9855)
        // Expected distance is ~1.07 km (1067 meters)
        val nyDistance = TripCalculator.calculateDistanceMeters(
            lat1 = 40.7484, lon1 = -73.9857,
            lat2 = 40.7580, lon2 = -73.9855
        )
        assertTrue("NY distance should be ~1067m, got $nyDistance", nyDistance in 1050.0..1090.0)
    }

    @Test
    fun testSpeedConversion() {
        // 0 m/s
        assertEquals(0f, TripCalculator.mpsToKmh(0f), 0.001f)
        assertEquals(0f, TripCalculator.kmhToMps(0f), 0.001f)

        // 10 m/s = 36 km/h
        assertEquals(36f, TripCalculator.mpsToKmh(10f), 0.001f)
        assertEquals(10f, TripCalculator.kmhToMps(36f), 0.001f)

        // Highway speed: 27.7778 m/s = 100 km/h
        val kmh = TripCalculator.mpsToKmh(27.777778f)
        assertEquals(100f, kmh, 0.01f)

        // Negative values clamped to 0
        assertEquals(0f, TripCalculator.mpsToKmh(-5f), 0.001f)
    }

    @Test
    fun testMovingVsOverallAverageSpeed() {
        // Traveled 10,000 meters (10 km)
        // Moving time: 10 minutes = 600,000 ms (60 km/h moving avg)
        // Total time: 20 minutes = 1,200,000 ms (including 10 min red light / coffee pause -> 30 km/h overall avg)
        val movingAvg = TripCalculator.calculateMovingAverageSpeedKmh(
            totalDistanceMeters = 10000.0,
            movingDurationMs = 600000L
        )
        assertEquals(60.0f, movingAvg, 0.1f)

        val overallAvg = TripCalculator.calculateOverallAverageSpeedKmh(
            totalDistanceMeters = 10000.0,
            totalDurationMs = 1200000L
        )
        assertEquals(30.0f, overallAvg, 0.1f)

        // Zero duration safety
        assertEquals(0f, TripCalculator.calculateMovingAverageSpeedKmh(1000.0, 0L), 0.001f)
        assertEquals(0f, TripCalculator.calculateOverallAverageSpeedKmh(1000.0, 0L), 0.001f)
    }

    @Test
    fun testDurationFormatting() {
        assertEquals("00:00", TripCalculator.formatDuration(0L))
        assertEquals("00:45", TripCalculator.formatDuration(45_000L))
        assertEquals("05:30", TripCalculator.formatDuration(330_000L))
        assertEquals("01:15:30", TripCalculator.formatDuration(4_530_000L))
    }

    @Test
    fun testChartPointsEdgeCases() {
        // Empty dataset
        val empty = TripCalculator.buildChartPoints(emptyList())
        assertTrue(empty.isEmpty())

        // Single point dataset
        val single = listOf(ChartPoint(distanceKm = 0f, speedKmh = 0f, timestamp = 1000L))
        val singleResult = TripCalculator.buildChartPoints(single)
        assertEquals(1, singleResult.size)

        // Multiple points downsampling preserves peak speed point
        val points = mutableListOf<ChartPoint>()
        for (i in 0..500) {
            val speed = if (i == 250) 140f else (i % 60).toFloat()
            points.add(ChartPoint(distanceKm = i * 0.1f, speedKmh = speed, timestamp = 1000L + i * 1000L))
        }

        val downsampled = TripCalculator.buildChartPoints(points, maxSamples = 100)
        assertTrue(downsampled.size <= 101)
        val maxPoint = downsampled.maxByOrNull { it.speedKmh }
        assertEquals(140f, maxPoint?.speedKmh ?: 0f, 0.001f)
    }
}
