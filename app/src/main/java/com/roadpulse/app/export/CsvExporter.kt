package com.roadpulse.app.export

import com.roadpulse.app.data.database.TrackPointEntity
import com.roadpulse.app.data.database.TripEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object CsvExporter {
    private val iso8601Format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun exportToCsv(trip: TripEntity, points: List<TrackPointEntity>): String {
        val sb = StringBuilder()

        // Metadata Header
        sb.append("# RoadPulse Trip Export\n")
        sb.append("# Title: ").append(sanitizeCsv(trip.title)).append("\n")
        sb.append("# Notes: ").append(sanitizeCsv(trip.notes ?: "")).append("\n")
        sb.append("# Start Time: ").append(iso8601Format.format(Date(trip.startTime))).append("\n")
        trip.endTime?.let {
            sb.append("# End Time: ").append(iso8601Format.format(Date(it))).append("\n")
        }
        sb.append(String.format(Locale.US, "# Total Distance (km): %.3f\n", trip.totalDistanceKm))
        sb.append(String.format(Locale.US, "# Max Speed (km/h): %.2f\n", trip.maxSpeedKmh))
        sb.append(String.format(Locale.US, "# Avg Moving Speed (km/h): %.2f\n", trip.avgMovingSpeedKmh))
        sb.append(String.format(Locale.US, "# Avg Overall Speed (km/h): %.2f\n", trip.avgOverallSpeedKmh))
        sb.append("# Moving Duration (s): ").append(trip.movingDurationMs / 1000).append("\n")
        sb.append("# Total Duration (s): ").append(trip.totalDurationMs / 1000).append("\n")
        sb.append("# Points Count: ").append(points.size).append("\n")
        sb.append("#\n")

        // Columns
        sb.append("point_index,timestamp_iso,timestamp_epoch_ms,latitude,longitude,speed_mps,speed_kmh,cumulative_distance_meters,cumulative_distance_km,accuracy_meters,altitude_meters,bearing_degrees\n")

        // Rows
        for ((idx, pt) in points.withIndex()) {
            val iso = iso8601Format.format(Date(pt.timestamp))
            val speedKmh = pt.speedMps * 3.6f
            val distKm = pt.cumulativeDistanceMeters / 1000.0
            val alt = pt.altitudeMeters?.let { String.format(Locale.US, "%.2f", it) } ?: ""
            val brg = pt.bearingDegrees?.let { String.format(Locale.US, "%.1f", it) } ?: ""

            sb.append(idx + 1).append(",")
                .append(iso).append(",")
                .append(pt.timestamp).append(",")
                .append(String.format(Locale.US, "%.7f", pt.latitude)).append(",")
                .append(String.format(Locale.US, "%.7f", pt.longitude)).append(",")
                .append(String.format(Locale.US, "%.2f", pt.speedMps)).append(",")
                .append(String.format(Locale.US, "%.2f", speedKmh)).append(",")
                .append(String.format(Locale.US, "%.2f", pt.cumulativeDistanceMeters)).append(",")
                .append(String.format(Locale.US, "%.3f", distKm)).append(",")
                .append(String.format(Locale.US, "%.1f", pt.accuracyMeters)).append(",")
                .append(alt).append(",")
                .append(brg).append("\n")
        }

        return sb.toString()
    }

    private fun sanitizeCsv(value: String): String {
        return value.replace("\n", " ").replace("\r", " ").replace(",", ";")
    }
}
