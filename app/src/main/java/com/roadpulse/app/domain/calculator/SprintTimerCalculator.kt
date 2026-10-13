package com.roadpulse.app.domain.calculator

import com.roadpulse.app.data.database.TrackPointEntity
import kotlin.math.roundToInt

data class SprintResults(
    val time0to60Seconds: Float? = null,
    val time0to100Seconds: Float? = null
)

object SprintTimerCalculator {
    /**
     * Scans recorded track points for standing-start acceleration runs (0-60 km/h and 0-100 km/h).
     * Employs linear interpolation for high-precision split timing.
     */
    fun calculateBestSprints(points: List<TrackPointEntity>): SprintResults {
        if (points.size < 3) return SprintResults()

        var best0to60: Float? = null
        var best0to100: Float? = null

        for (i in 0 until points.size - 1) {
            val startSpeedKmh = points[i].speedMps * 3.6f
            // Valid launch: nearly stationary (< 3.0 km/h)
            if (startSpeedKmh <= 3.0f) {
                val startTime = points[i].timestamp
                var run0to60Found = false
                var run0to100Found = false

                var prevSpeedKmh = startSpeedKmh
                var prevTime = startTime

                for (j in (i + 1) until minOf(i + 50, points.size)) {
                    val currentSpeedKmh = points[j].speedMps * 3.6f
                    val currentTime = points[j].timestamp

                    // If speed dropped significantly before 60, abort this run
                    if (currentSpeedKmh < prevSpeedKmh - 6.0f && currentSpeedKmh < 45.0f) {
                        break
                    }

                    // Check 0-60 km/h crossing
                    if (!run0to60Found && currentSpeedKmh >= 60.0f) {
                        val fraction = if (currentSpeedKmh > prevSpeedKmh) {
                            (60.0f - prevSpeedKmh) / (currentSpeedKmh - prevSpeedKmh)
                        } else 0f
                        val crossTime = prevTime + (fraction * (currentTime - prevTime)).toLong()
                        val durationSec = (crossTime - startTime) / 1000.0f
                        if (durationSec > 0.5f) {
                            if (best0to60 == null || durationSec < best0to60) {
                                best0to60 = durationSec
                            }
                            run0to60Found = true
                        }
                    }

                    // Check 0-100 km/h crossing
                    if (!run0to100Found && currentSpeedKmh >= 100.0f) {
                        val fraction = if (currentSpeedKmh > prevSpeedKmh) {
                            (100.0f - prevSpeedKmh) / (currentSpeedKmh - prevSpeedKmh)
                        } else 0f
                        val crossTime = prevTime + (fraction * (currentTime - prevTime)).toLong()
                        val durationSec = (crossTime - startTime) / 1000.0f
                        if (durationSec > 1.0f) {
                            if (best0to100 == null || durationSec < best0to100) {
                                best0to100 = durationSec
                            }
                            run0to100Found = true
                            break
                        }
                    }

                    prevSpeedKmh = currentSpeedKmh
                    prevTime = currentTime
                }
            }
        }

        return SprintResults(
            time0to60Seconds = best0to60?.let { (it * 10f).roundToInt() / 10.0f },
            time0to100Seconds = best0to100?.let { (it * 10f).roundToInt() / 10.0f }
        )
    }
}
