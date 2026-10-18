package com.roadpulse.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadpulse.app.domain.model.GpsStatus
import com.roadpulse.app.ui.theme.CarbonDark
import com.roadpulse.app.ui.theme.CardBorder
import com.roadpulse.app.ui.theme.CobaltBlue
import com.roadpulse.app.ui.theme.HyperCyan
import com.roadpulse.app.ui.theme.MidnightCard
import com.roadpulse.app.ui.theme.NeonCyan
import com.roadpulse.app.ui.theme.OptimalGreen
import com.roadpulse.app.ui.theme.RacingRed
import com.roadpulse.app.ui.theme.TextMuted
import com.roadpulse.app.ui.theme.TextPrimary
import com.roadpulse.app.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun PilotTelemetryDossierCard(
    userName: String,
    userHandle: String = "@pilot",
    onNavigateToProfile: () -> Unit,
    onLogoutClick: () -> Unit,
    gpsStatus: GpsStatus,
    gpsAccuracyMeters: Float,
    careerDistanceKm: Double,
    careerPeakSpeedKmh: Int,
    careerMaxLeanDeg: Float,
    careerRunsLogged: Int,
    modifier: Modifier = Modifier
) {
    val purpleAccent = Color(0xFFA855F7)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MidnightCard)
            .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header: Rider Identity + Telemetry Status Chips + Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Rider Avatar & Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToProfile() }
            ) {
                // Micro-Precise Avatar with Status Ring
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(listOf(HyperCyan.copy(alpha = 0.2f), CobaltBlue.copy(alpha = 0.3f)))
                        )
                        .border(1.5.dp, HyperCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                        contentDescription = "Rider Profile",
                        tint = HyperCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "TELEMETRY PILOT DOSSIER",
                            color = HyperCyan,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(OptimalGreen.copy(alpha = 0.15f))
                                .border(1.dp, OptimalGreen.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "ONLINE • 60Hz",
                                color = OptimalGreen,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = userName,
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            text = userHandle,
                            color = HyperCyan.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Hardware Bus Status Indicators
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(OptimalGreen)
                        )
                        Text(
                            text = "ECU K-LINE: 60Hz",
                            color = OptimalGreen,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(text = "•", color = TextMuted, fontSize = 8.sp)
                        Text(
                            text = "GPS RTK: 10Hz",
                            color = HyperCyan,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(text = "•", color = TextMuted, fontSize = 8.sp)
                        Text(
                            text = "BLACK BOX: ARMED",
                            color = purpleAccent,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Top Status Badges: GPS Lock & Logout
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GpsStatusBadge(
                    gpsStatus = gpsStatus,
                    accuracyMeters = gpsAccuracyMeters
                )

                IconButton(
                    onClick = onLogoutClick,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(RacingRed.copy(alpha = 0.12f))
                        .border(1.dp, RacingRed.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Log Out",
                        tint = RacingRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Bio tagline
        Text(
            text = "Precision motorcycle telemetry & dynamic apex trajectory tracking.",
            color = TextMuted,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )

        // ================= 4-CARD CAREER MATRIX =================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CareerStatBox(
                title = "CAREER DIST",
                value = String.format(Locale.US, "%.1f", careerDistanceKm),
                unit = "KM",
                subtext = "Telemetry Ready",
                valueColor = TextPrimary,
                modifier = Modifier.weight(1f)
            )

            CareerStatBox(
                title = "PEAK VELOCITY",
                value = "$careerPeakSpeedKmh",
                unit = "KM/H",
                subtext = "GPS Clocked",
                valueColor = RacingRed,
                modifier = Modifier.weight(1f)
            )

            CareerStatBox(
                title = "MAX APEX LEAN",
                value = "${String.format(Locale.US, "%.1f", careerMaxLeanDeg)}°",
                unit = "MAX",
                subtext = "Gyro Calibrated",
                valueColor = HyperCyan,
                modifier = Modifier.weight(1f)
            )

            CareerStatBox(
                title = "RUNS LOGGED",
                value = "$careerRunsLogged",
                unit = "RUNS",
                subtext = if (careerRunsLogged > 0) "Logged Runs" else "Awaiting Ride",
                valueColor = OptimalGreen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CareerStatBox(
    title: String,
    value: String,
    unit: String,
    subtext: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CarbonDark)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = TextMuted,
                fontSize = 7.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = value,
                    color = valueColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    color = HyperCyan,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                color = TextSecondary,
                fontSize = 7.sp,
                maxLines = 1
            )
        }
    }
}
