package com.roadpulse.app

import com.roadpulse.app.data.database.TrackPointEntity
import com.roadpulse.app.data.database.TripEntity
import com.roadpulse.app.export.CsvExporter
import com.roadpulse.app.export.GpxExporter
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportFormattingTest {

    private val sampleTrip = TripEntity(
        id = 42L,
        title = "Track Day & Canyon Ride",
        notes = "Fast corners and sunny weather",
        startTime = 1715000000000L,
        endTime = 1715003600000L,
        status = "FINISHED",
        totalDistanceMeters = 25420.0,
        maxSpeedMps = 38.5f,
        avgMovingSpeedMps = 22.0f,
        avgOverallSpeedMps = 18.0f,
        movingDurationMs = 3000000L,
        totalDurationMs = 3600000L,
        pauseDurationMs = 600000L
    )

    private val samplePoints = listOf(
        TrackPointEntity(
            id = 1L,
            tripId = 42L,
            timestamp = 1715000000000L,
            latitude = 34.0522,
            longitude = -118.2437,
            speedMps = 0f,
            accuracyMeters = 4f,
            altitudeMeters = 85.0,
            cumulativeDistanceMeters = 0.0,
            bearingDegrees = 180f
        ),
        TrackPointEntity(
            id = 2L,
            tripId = 42L,
            timestamp = 1715000010000L,
            latitude = 34.0530,
            longitude = -118.2445,
            speedMps = 15.2f,
            accuracyMeters = 3.5f,
            altitudeMeters = 86.5,
            cumulativeDistanceMeters = 120.0,
            bearingDegrees = 185f
        ),
        TrackPointEntity(
            id = 3L,
            tripId = 42L,
            timestamp = 1715000020000L,
            latitude = 34.0545,
            longitude = -118.2458,
            speedMps = 24.8f,
            accuracyMeters = 3.0f,
            altitudeMeters = 88.0,
            cumulativeDistanceMeters = 340.0,
            bearingDegrees = 190f
        )
    )

    @Test
    fun testGpxExportFormat() {
        val gpx = GpxExporter.exportToGpx(sampleTrip, samplePoints)

        // Check XML Header and Schema
        assertTrue(gpx.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
        assertTrue(gpx.contains("<gpx version=\"1.1\""))
        assertTrue(gpx.contains("<name>Track Day &amp; Canyon Ride</name>"))
        assertTrue(gpx.contains("<desc>Fast corners and sunny weather</desc>"))
        assertTrue(gpx.contains("<trk>"))
        assertTrue(gpx.contains("<trkseg>"))

        // Check Point Coordinates and extensions
        assertTrue(gpx.contains("<trkpt lat=\"34.0522000\" lon=\"-118.2437000\">"))
        assertTrue(gpx.contains("<ele>85.00</ele>"))
        assertTrue(gpx.contains("<speed>15.20</speed>"))
        assertTrue(gpx.contains("<accuracy>3.5</accuracy>"))
        assertTrue(gpx.contains("<cumulativeDistance>340.00</cumulativeDistance>"))
        assertTrue(gpx.endsWith("</gpx>\n"))
    }

    @Test
    fun testCsvExportFormat() {
        val csv = CsvExporter.exportToCsv(sampleTrip, samplePoints)

        // Metadata Header
        assertTrue(csv.contains("# RoadPulse Trip Export"))
        assertTrue(csv.contains("# Title: Track Day & Canyon Ride"))
        assertTrue(csv.contains("# Total Distance (km): 25.420"))

        // Column Header
        assertTrue(csv.contains("point_index,timestamp_iso,timestamp_epoch_ms,latitude,longitude,speed_mps,speed_kmh,cumulative_distance_meters,cumulative_distance_km,accuracy_meters,altitude_meters,bearing_degrees"))

        // Rows
        val lines = csv.lines()
        val dataRows = lines.filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("point_index") }
        assertTrue(dataRows.size == 3)

        // Check row columns
        val firstRowCols = dataRows[0].split(",")
        assertTrue(firstRowCols[0] == "1") // point_index
        assertTrue(firstRowCols[2] == "1715000000000") // epoch
        assertTrue(firstRowCols[3] == "34.0522000") // lat
        assertTrue(firstRowCols[4] == "-118.2437000") // lon
    }

    @Test
    fun testEmptyTripExports() {
        val emptyGpx = GpxExporter.exportToGpx(sampleTrip, emptyList())
        assertTrue(emptyGpx.contains("<trkseg>\n    </trkseg>"))

        val emptyCsv = CsvExporter.exportToCsv(sampleTrip, emptyList())
        assertTrue(emptyCsv.contains("# Points Count: 0"))
    }
}
