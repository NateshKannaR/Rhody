package com.roadpulse.app.export

import com.roadpulse.app.data.database.TrackPointEntity
import com.roadpulse.app.data.database.TripEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object GpxExporter {
    private val iso8601Format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun exportToGpx(trip: TripEntity, points: List<TrackPointEntity>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
        sb.append("""<gpx version="1.1" creator="Rhody Android GPS Tracker"""")
        sb.append(""" xmlns="http://www.topografix.com/GPX/1/1"""")
        sb.append(""" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"""")
        sb.append(""" xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd">""").append("\n")

        sb.append("  <metadata>\n")
        sb.append("    <name>").append(escapeXml(trip.title)).append("</name>\n")
        if (!trip.notes.isNullOrBlank()) {
            sb.append("    <desc>").append(escapeXml(trip.notes)).append("</desc>\n")
        }
        sb.append("    <time>").append(iso8601Format.format(Date(trip.startTime))).append("</time>\n")
        sb.append("  </metadata>\n")

        sb.append("  <trk>\n")
        sb.append("    <name>").append(escapeXml(trip.title)).append("</name>\n")
        sb.append("    <trkseg>\n")

        for (pt in points) {
            sb.append(String.format(Locale.US, "      <trkpt lat=\"%.7f\" lon=\"%.7f\">\n", pt.latitude, pt.longitude))
            pt.altitudeMeters?.let { alt ->
                sb.append(String.format(Locale.US, "        <ele>%.2f</ele>\n", alt))
            }
            sb.append("        <time>").append(iso8601Format.format(Date(pt.timestamp))).append("</time>\n")
            sb.append("        <extensions>\n")
            sb.append(String.format(Locale.US, "          <speed>%.2f</speed>\n", pt.speedMps))
            sb.append(String.format(Locale.US, "          <accuracy>%.1f</accuracy>\n", pt.accuracyMeters))
            pt.bearingDegrees?.let { brg ->
                sb.append(String.format(Locale.US, "          <bearing>%.1f</bearing>\n", brg))
            }
            sb.append(String.format(Locale.US, "          <cumulativeDistance>%.2f</cumulativeDistance>\n", pt.cumulativeDistanceMeters))
            sb.append("        </extensions>\n")
            sb.append("      </trkpt>\n")
        }

        sb.append("    </trkseg>\n")
        sb.append("  </trk>\n")
        sb.append("</gpx>\n")

        return sb.toString()
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
