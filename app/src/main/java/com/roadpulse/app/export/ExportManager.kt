package com.roadpulse.app.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.roadpulse.app.data.database.TrackPointEntity
import com.roadpulse.app.data.database.TripEntity
import java.io.File
import java.io.FileOutputStream

object ExportManager {
    fun shareGpx(context: Context, trip: TripEntity, points: List<TrackPointEntity>) {
        val gpxContent = GpxExporter.exportToGpx(trip, points)
        val fileName = sanitizeFileName("roadpulse_trip_${trip.id}_${trip.startTime}.gpx")
        val file = writeToFile(context, fileName, gpxContent)

        shareFile(context, file, "application/gpx+xml", "Share GPX Route: ${trip.title}")
    }

    fun shareCsv(context: Context, trip: TripEntity, points: List<TrackPointEntity>) {
        val csvContent = CsvExporter.exportToCsv(trip, points)
        val fileName = sanitizeFileName("roadpulse_trip_${trip.id}_${trip.startTime}.csv")
        val file = writeToFile(context, fileName, csvContent)

        shareFile(context, file, "text/csv", "Share CSV Metrics: ${trip.title}")
    }

    private fun writeToFile(context: Context, fileName: String, content: String): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, fileName)
        FileOutputStream(file).use { out ->
            out.write(content.toByteArray(Charsets.UTF_8))
        }
        return file
    }

    private fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
    }
}
