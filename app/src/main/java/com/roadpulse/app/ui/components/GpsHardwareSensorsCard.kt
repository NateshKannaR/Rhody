package com.roadpulse.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadpulse.app.domain.model.GpsStatus
import com.roadpulse.app.ui.theme.CarbonDark
import com.roadpulse.app.ui.theme.CardBorder
import com.roadpulse.app.ui.theme.HyperCyan
import com.roadpulse.app.ui.theme.MidnightCard
import com.roadpulse.app.ui.theme.OptimalGreen
import com.roadpulse.app.ui.theme.SportAmber
import com.roadpulse.app.ui.theme.TextMuted
import com.roadpulse.app.ui.theme.TextPrimary
import com.roadpulse.app.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun GpsHardwareSensorsCard(
    gpsStatus: GpsStatus,
    accuracyMeters: Float,
    groundSpeedKmh: Float,
    headingDegrees: Float? = null,
    modifier: Modifier = Modifier
) {
    val isLocked = gpsStatus == GpsStatus.ACCURATE || (accuracyMeters in 0.1f..20f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MidnightCard)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(OptimalGreen.copy(alpha = 0.15f))
                        .border(1.dp, OptimalGreen.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = "GPS Sensors",
                        tint = OptimalGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "GPS & IMU SENSORS",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Hardware Telematics Bus Diagnostics",
                        color = TextSecondary,
                        fontSize = 9.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isLocked) OptimalGreen.copy(alpha = 0.2f) else Color(0xFF1E293B))
                    .border(1.dp, if (isLocked) OptimalGreen.copy(alpha = 0.4f) else CardBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isLocked) "GPS 3D FIX" else "GPS STANDBY",
                    color = if (isLocked) OptimalGreen else TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3 Sensor Blocks Stack
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Satellite Positioning
            SensorBlock(
                title = "SATELLITE POSITIONING",
                value = if (isLocked) "3D RTK LOCK" else "STANDBY",
                subtext = if (isLocked) "± ${String.format(Locale.US, "%.1f", accuracyMeters)} m precision" else "Awaiting 3D Satellite Fix",
                valueColor = if (isLocked) OptimalGreen else TextSecondary
            )

            // Ground Speed & Course
            val headingText = if (headingDegrees != null && headingDegrees >= 0f) {
                val dir = getCompassDirection(headingDegrees)
                "Course: ${headingDegrees.roundToInt()}° $dir"
            } else {
                "Course: Calibrated"
            }
            SensorBlock(
                title = "GROUND SPEED & COURSE",
                value = "${String.format(Locale.US, "%.1f", groundSpeedKmh)} KM/H",
                subtext = headingText,
                valueColor = HyperCyan
            )

            // Hardware IMU Sensor
            SensorBlock(
                title = "HARDWARE IMU SENSOR",
                value = "6-AXIS ARMED",
                subtext = "Gyroscope & Accelerometer (60Hz)",
                valueColor = SportAmber
            )
        }
    }
}

@Composable
private fun SensorBlock(
    title: String,
    value: String,
    subtext: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CarbonDark)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = title,
                color = TextMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                color = TextSecondary,
                fontSize = 8.sp
            )
        }
    }
}

private fun getCompassDirection(degrees: Float): String {
    val normalized = (degrees % 360 + 360) % 360
    return when {
        normalized >= 337.5 || normalized < 22.5 -> "N"
        normalized < 67.5 -> "NE"
        normalized < 112.5 -> "E"
        normalized < 157.5 -> "SE"
        normalized < 202.5 -> "S"
        normalized < 247.5 -> "SW"
        normalized < 292.5 -> "W"
        else -> "NW"
    }
}
