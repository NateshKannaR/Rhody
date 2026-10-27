package com.roadpulse.app.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Curated Rhody Motorsport Modern Iconography Suite.
 * Provides custom high-precision telemetry vectors and stylized navigation icons.
 */
object RhodyIcons {

    /** Modern Aerodynamic Cockpit & Velocity Gauge */
    val Cockpit: ImageVector by lazy {
        ImageVector.Builder(
            name = "Cockpit",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Gauge Arc
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(4.5f, 17f)
                curveTo(2.8f, 14.5f, 2.5f, 11f, 3.8f, 8.2f)
                curveTo(5.5f, 4.5f, 9.5f, 2.5f, 13.5f, 2.8f)
                curveTo(17.5f, 3.2f, 20.8f, 6.2f, 21.5f, 10.2f)
                curveTo(22f, 12.8f, 21.2f, 15.5f, 19.5f, 17.5f)
            }
            // Dynamic Velocity Needle
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 14f)
                lineTo(17.5f, 7.5f)
            }
            // Pivot Ring
            path(
                fill = SolidColor(Color.White)
            ) {
                moveTo(12f, 12f)
                arcTo(2f, 2f, 0f, true, true, 12.01f, 12f)
                close()
            }
        }.build()
    }

    /** Racing Apex Circuit Route Track */
    val RaceCircuit: ImageVector by lazy {
        ImageVector.Builder(
            name = "RaceCircuit",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 16f)
                curveTo(3f, 10f, 6f, 5f, 11f, 4f)
                curveTo(16f, 3f, 20f, 6f, 20.5f, 10.5f)
                curveTo(21f, 14f, 18.5f, 17f, 15f, 17.5f)
                curveTo(12.5f, 17.8f, 10f, 16.5f, 9.5f, 14f)
                curveTo(9f, 11f, 11f, 9f, 13.5f, 9.5f)
                curveTo(15f, 9.8f, 16f, 11f, 15.5f, 12.5f)
            }
            // Origin Apex Pin
            path(
                fill = SolidColor(Color.White)
            ) {
                moveTo(4f, 16f)
                arcTo(2.2f, 2.2f, 0f, true, true, 4.01f, 16f)
                close()
            }
        }.build()
    }

    /** High-Resolution Dyno & Telemetry Waveform */
    val TelemetryWave: ImageVector by lazy {
        ImageVector.Builder(
            name = "TelemetryWave",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2.5f, 14f)
                lineTo(6f, 14f)
                lineTo(8.5f, 6.5f)
                lineTo(12f, 19f)
                lineTo(15.5f, 10.5f)
                lineTo(18f, 15f)
                lineTo(21.5f, 15f)
            }
            // Peak Marker
            path(fill = SolidColor(Color.White)) {
                moveTo(8.5f, 5f)
                arcTo(1.5f, 1.5f, 0f, true, true, 8.51f, 5f)
                close()
            }
        }.build()
    }

    /** Motorsport Aerodynamic Pilot Helmet */
    val PilotHelmet: ImageVector by lazy {
        ImageVector.Builder(
            name = "PilotHelmet",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Helmet Outer Shell
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 3.5f)
                curveTo(6.5f, 3.5f, 3f, 7.5f, 3f, 12.5f)
                curveTo(3f, 17f, 6.5f, 20.5f, 11.5f, 20.5f)
                lineTo(17.5f, 20.5f)
                curveTo(19.5f, 20.5f, 21f, 19f, 21f, 17f)
                lineTo(21f, 12f)
                curveTo(21f, 7f, 17f, 3.5f, 12f, 3.5f)
                close()
            }
            // Visor Glass Aperture
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6.5f, 12f)
                lineTo(17f, 11f)
                curveTo(19f, 11f, 19.5f, 13f, 18.5f, 15f)
                lineTo(8f, 15.5f)
                close()
            }
        }.build()
    }

    /** Lean Angle Telemetry Apex Inclinometer */
    val LeanApex: ImageVector by lazy {
        ImageVector.Builder(
            name = "LeanApex",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Horizon Datum
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(3f, 19f)
                lineTo(21f, 19f)
            }
            // Lean Angle Incline Axis
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 19f)
                lineTo(18.5f, 5.5f)
            }
            // Curved Angle Measure Arc
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(16.5f, 19f)
                curveTo(16.5f, 16.5f, 15.5f, 14.5f, 14.2f, 13.5f)
            }
        }.build()
    }

    /** G-Force Acceleration Crosshair Reticle */
    val GForceReticle: ImageVector by lazy {
        ImageVector.Builder(
            name = "GForceReticle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Concentric Precision Target Ring
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f
            ) {
                moveTo(12f, 4.5f)
                arcTo(7.5f, 7.5f, 0f, true, true, 11.99f, 4.5f)
                close()
            }
            // Crosshairs
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 2f); lineTo(12f, 6.5f)
                moveTo(12f, 17.5f); lineTo(12f, 22f)
                moveTo(2f, 12f); lineTo(6.5f, 12f)
                moveTo(17.5f, 12f); lineTo(22f, 12f)
            }
            // Center G-Force Point
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 10.8f)
                arcTo(1.2f, 1.2f, 0f, true, true, 12.01f, 10.8f)
                close()
            }
        }.build()
    }

    /** High-Security Biometric Document Vault Shield */
    val BiometricVault: ImageVector by lazy {
        ImageVector.Builder(
            name = "BiometricVault",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 2.5f)
                lineTo(4f, 6f)
                lineTo(4f, 12f)
                curveTo(4f, 17.5f, 7.5f, 20.8f, 12f, 22f)
                curveTo(16.5f, 20.8f, 20f, 17.5f, 20f, 12f)
                lineTo(20f, 6f)
                close()
            }
            // Internal Biometric Circuit Node
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 8f); lineTo(12f, 14f)
                moveTo(9.5f, 11.5f); lineTo(14.5f, 11.5f)
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 15.5f)
                arcTo(1.3f, 1.3f, 0f, true, true, 12.01f, 15.5f)
                close()
            }
        }.build()
    }

    /** High-Voltage Lithium Ion Battery Cell */
    val IonBattery: ImageVector by lazy {
        ImageVector.Builder(
            name = "IonBattery",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Battery Terminal
            path(
                fill = SolidColor(Color.White)
            ) {
                moveTo(10f, 2f)
                lineTo(14f, 2f)
                lineTo(14f, 4f)
                lineTo(10f, 4f)
                close()
            }
            // Battery Body
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6.5f, 4f)
                lineTo(17.5f, 4f)
                lineTo(17.5f, 21f)
                lineTo(6.5f, 21f)
                close()
            }
            // Electric Flash Bolt
            path(
                fill = SolidColor(Color.White)
            ) {
                moveTo(12.5f, 7f)
                lineTo(9f, 13f)
                lineTo(12f, 13f)
                lineTo(11.5f, 18f)
                lineTo(15f, 12f)
                lineTo(12.5f, 12f)
                close()
            }
        }.build()
    }

    /** Topographic GPS Altitude Contour Mountain */
    val ElevationMountain: ImageVector by lazy {
        ImageVector.Builder(
            name = "ElevationMountain",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2.5f, 20f)
                lineTo(10f, 5.5f)
                lineTo(14.5f, 13.5f)
                lineTo(17.5f, 9.5f)
                lineTo(22f, 20f)
                close()
            }
            // Contour Peak Line
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(7.5f, 15f)
                lineTo(12.5f, 15f)
            }
        }.build()
    }

    /** Precision Superbike Silhouette */
    val Superbike: ImageVector by lazy {
        ImageVector.Builder(
            name = "Superbike",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Wheels
            path(stroke = SolidColor(Color.White), strokeLineWidth = 2.0f) {
                moveTo(5f, 16f); arcTo(3f, 3f, 0f, true, true, 4.99f, 16f); close()
                moveTo(19f, 16f); arcTo(3f, 3f, 0f, true, true, 18.99f, 16f); close()
            }
            // Aero Chassis
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 16f)
                lineTo(8f, 12f)
                lineTo(11f, 11f)
                lineTo(14f, 8f)
                lineTo(17f, 9f)
                lineTo(19f, 16f)
            }
            // Clip-on handlebar & tank
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(10f, 11f)
                lineTo(13f, 11f)
                moveTo(15.5f, 7.5f)
                lineTo(14f, 8.5f)
            }
        }.build()
    }
}
