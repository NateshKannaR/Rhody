package com.roadpulse.app

import com.roadpulse.app.domain.calculator.TripCalculator
import com.roadpulse.app.domain.model.TripMetrics
import com.roadpulse.app.domain.model.TripStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripStateMachineTest {

    @Test
    fun testInitialIdleState() {
        val initial = TripMetrics()
        assertEquals(TripStatus.IDLE, initial.tripStatus)
        assertEquals(0f, initial.currentSpeedKmh, 0.001f)
        assertEquals(0.0, initial.totalDistanceMeters, 0.001)
        assertFalse(initial.isRecording)
        assertFalse(initial.isPaused)
    }

    @Test
    fun testStartToPauseToResumeToFinishTransitions() {
        var metrics = TripMetrics()

        // 1. Start Trip
        metrics = metrics.copy(
            tripStatus = TripStatus.RECORDING,
            activeTripId = 1L
        )
        assertTrue(metrics.isRecording)
        assertFalse(metrics.isPaused)
        assertEquals(TripStatus.RECORDING, metrics.tripStatus)

        // 2. Accumulate movement
        metrics = metrics.copy(
            currentSpeedKmh = 65.0f,
            maxSpeedKmh = 82.0f,
            totalDistanceMeters = 5400.0,
            movingDurationMs = 300_000L,
            elapsedTimeMs = 320_000L
        )
        assertEquals(5.4, metrics.totalDistanceKm, 0.001)

        // 3. Pause Trip
        metrics = metrics.copy(
            tripStatus = TripStatus.PAUSED,
            currentSpeedKmh = 0f
        )
        assertFalse(metrics.isRecording)
        assertTrue(metrics.isPaused)
        assertEquals(0f, metrics.currentSpeedKmh, 0.001f)

        // 4. Resume Trip
        metrics = metrics.copy(
            tripStatus = TripStatus.RECORDING,
            pauseDurationMs = 60_000L
        )
        assertTrue(metrics.isRecording)
        assertFalse(metrics.isPaused)
        assertEquals(60_000L, metrics.pauseDurationMs)

        // 5. Finish Trip
        metrics = metrics.copy(
            tripStatus = TripStatus.FINISHED,
            currentSpeedKmh = 0f
        )
        assertFalse(metrics.isRecording)
        assertFalse(metrics.isPaused)
        assertEquals(TripStatus.FINISHED, metrics.tripStatus)
    }

    @Test
    fun testMultiPointTrackAccumulation() {
        // Synthetic track with 5 points along a road
        val coords = listOf(
            Pair(37.7749, -122.4194),
            Pair(37.7755, -122.4180),
            Pair(37.7762, -122.4165),
            Pair(37.7770, -122.4150),
            Pair(37.7780, -122.4135)
        )

        var totalDist = 0.0
        for (i in 1 until coords.size) {
            val d = TripCalculator.calculateDistanceMeters(
                coords[i - 1].first, coords[i - 1].second,
                coords[i].first, coords[i].second
            )
            totalDist += d
        }

        assertTrue("Distance should be accumulated (~600-800m), got $totalDist", totalDist in 600.0..800.0)
    }
}
