package com.roadpulse.app.ui.screens.livetrip

import android.content.Intent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadpulse.app.domain.calculator.TripCalculator
import com.roadpulse.app.domain.model.TripStatus
import com.roadpulse.app.service.TripRecordingController
import com.roadpulse.app.ui.components.GpsStatusBadge
import com.roadpulse.app.ui.components.RouteMapView
import com.roadpulse.app.ui.theme.CarbonDark
import com.roadpulse.app.ui.theme.CardBorder
import com.roadpulse.app.ui.theme.CardBorderGlow
import com.roadpulse.app.ui.theme.ElectricBlue
import com.roadpulse.app.ui.theme.HyperCyan
import com.roadpulse.app.ui.theme.MidnightCard
import com.roadpulse.app.ui.theme.NeonCyan
import com.roadpulse.app.ui.theme.ObsidianGlass
import com.roadpulse.app.ui.theme.ObsidianSurface
import com.roadpulse.app.ui.theme.OptimalGreen
import com.roadpulse.app.ui.theme.RacingRed
import com.roadpulse.app.ui.theme.SportAmber
import com.roadpulse.app.ui.theme.SuccessGreen
import com.roadpulse.app.ui.theme.TextMuted
import com.roadpulse.app.ui.theme.TextPrimary
import com.roadpulse.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.roundToInt

@Composable
fun LiveTripScreen(
    onFinishTrip: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val metrics by TripRecordingController.metrics.collectAsState()
    val livePoints by TripRecordingController.livePoints.collectAsState()
    val liveGForce by TripRecordingController.liveGForce.collectAsState()

    // Pulse animation for recording badge
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Calculate start timestamp when ride began
    val startedAtString = remember(metrics.elapsedTimeMs, metrics.tripStatus) {
        if (metrics.tripStatus != TripStatus.IDLE && metrics.elapsedTimeMs > 0) {
            val startTimeMs = System.currentTimeMillis() - metrics.elapsedTimeMs
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            "Started at " + sdf.format(Date(startTimeMs))
        } else {
            "Ready to Record"
        }
    }

    // Physics-based lean angle estimation from lateral G (theta = atan(G_lat))
    val leanAngleDegrees = remember(liveGForce.lateralG) {
        val rawDegrees = Math.toDegrees(atan(liveGForce.lateralG.toDouble())).toFloat()
        rawDegrees.coerceIn(-60f, 60f)
    }

    val animatedLean by animateFloatAsState(
        targetValue = leanAngleDegrees,
        animationSpec = tween(120),
        label = "LeanAngleAnimation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonDark)
    ) {
        // Map View (base layer)
        RouteMapView(
            points = livePoints,
            currentLat = metrics.currentLatitude,
            currentLon = metrics.currentLongitude,
            isLiveTracking = true,
            modifier = Modifier.fillMaxSize()
        )

        // Top HUD Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(ObsidianGlass.copy(alpha = 0.95f))
                    .border(1.dp, CardBorderGlow, RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                // Header Status Row: REC LIVE + Started At + GPS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .background(CarbonDark, shape = RoundedCornerShape(30.dp))
                            .border(1.dp, CardBorder, shape = RoundedCornerShape(30.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val (dotColor, statusText) = when (metrics.tripStatus) {
                            TripStatus.RECORDING -> RacingRed.copy(alpha = pulseAlpha) to "REC LIVE"
                            TripStatus.PAUSED -> SportAmber to "PAUSED"
                            else -> TextMuted to "STANDBY"
                        }
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }

                    // Started At timestamp callout
                    Text(
                        text = startedAtString,
                        color = HyperCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    GpsStatusBadge(
                        gpsStatus = metrics.gpsStatus,
                        accuracyMeters = metrics.accuracyMeters
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Primary Speed Reading Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${metrics.currentSpeedKmh.roundToInt()}",
                            color = HyperCyan,
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = (-2).sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.padding(bottom = 6.dp)) {
                            Text(
                                text = "KM/H",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "DIGITAL SPEED",
                                color = TextMuted,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Peak speed pill on right
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "PEAK SPEED",
                            color = TextMuted,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${metrics.maxSpeedKmh.roundToInt()} km/h",
                            color = RacingRed,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Divider line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(CardBorder.copy(alpha = 0.5f))
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 4-Column Secondary Telemetry Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HudStatPill(
                        label = "DIST",
                        value = String.format(Locale.US, "%.2f", metrics.totalDistanceKm),
                        unit = "KM",
                        color = HyperCyan,
                        modifier = Modifier.weight(1f)
                    )
                    HudStatPill(
                        label = "TIME",
                        value = TripCalculator.formatDuration(metrics.elapsedTimeMs),
                        unit = "",
                        color = SportAmber,
                        modifier = Modifier.weight(1.1f)
                    )
                    HudStatPill(
                        label = "AVG SPEED",
                        value = "${metrics.avgMovingSpeedKmh.roundToInt()}",
                        unit = "KM/H",
                        color = ElectricBlue,
                        modifier = Modifier.weight(1f)
                    )
                    HudStatPill(
                        label = "LAT G",
                        value = String.format(Locale.US, "%.2f", liveGForce.lateralG),
                        unit = "G",
                        color = OptimalGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ================= LIVE MOTORCYCLE LEAN ANGLE METER =================
                LiveLeanAngleBar(
                    leanDegrees = animatedLean,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Parent Safety Live Tracking Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CarbonDark.copy(alpha = 0.92f))
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            val activeId = metrics.activeTripId ?: System.currentTimeMillis()
                            val shareUrl = "https://rhodyy.vercel.app/?live=$activeId"
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Track my live motorcycle ride on Rhody: $shareUrl")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Live Ride with Parents"))
                        }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(OptimalGreen)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Column {
                            Text(
                                text = "PARENT LIVE SAFETY MONITOR",
                                color = TextPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (metrics.tripStatus == TripStatus.RECORDING) "Broadcasting live GPS & speed to parents" else "Tap to share live tracking link",
                                color = TextSecondary,
                                fontSize = 8.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = HyperCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Bottom Controls Floating Pill Dock
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp)
                .align(Alignment.BottomCenter)
        ) {
            when (metrics.tripStatus) {
                TripStatus.IDLE, TripStatus.FINISHED -> {
                    Button(
                        onClick = { TripRecordingController.startTrip(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HyperCyan,
                            contentColor = CarbonDark
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "START RECORDING TRIP",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
                TripStatus.RECORDING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { TripRecordingController.pauseTrip(context) },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = ObsidianGlass.copy(alpha = 0.95f),
                                contentColor = SportAmber
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, SportAmber.copy(alpha = 0.8f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PAUSE",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }

                        Button(
                            onClick = {
                                val tripId = metrics.activeTripId
                                TripRecordingController.finishTrip(context)
                                if (tripId != null && tripId > 0L) {
                                    onFinishTrip?.invoke(tripId)
                                }
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RacingRed,
                                contentColor = TextPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "FINISH TRIP",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
                TripStatus.PAUSED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { TripRecordingController.resumeTrip(context) },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HyperCyan,
                                contentColor = CarbonDark
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "RESUME",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }

                        Button(
                            onClick = {
                                val tripId = metrics.activeTripId
                                TripRecordingController.finishTrip(context)
                                if (tripId != null && tripId > 0L) {
                                    onFinishTrip?.invoke(tripId)
                                }
                            },
                            modifier = Modifier
                                .weight(1.1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RacingRed,
                                contentColor = TextPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "FINISH",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// Live Motorcycle Lean Angle Meter Component
@Composable
fun LiveLeanAngleBar(
    leanDegrees: Float,
    modifier: Modifier = Modifier
) {
    val absLean = abs(leanDegrees).roundToInt()
    val leanSide = when {
        absLean < 2 -> "UPRIGHT"
        leanDegrees < 0 -> "${absLean}° LEFT"
        else -> "${absLean}° RIGHT"
    }

    val leanStatusColor = when {
        absLean >= 45 -> RacingRed
        absLean >= 30 -> SportAmber
        absLean >= 15 -> HyperCyan
        else -> OptimalGreen
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CarbonDark.copy(alpha = 0.9f))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TwoWheeler,
                        contentDescription = null,
                        tint = leanStatusColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "LEAN ANGLE",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp
                    )
                }

                Text(
                    text = leanSide,
                    color = leanStatusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Canvas Tilt Indicator Bar (-50° Left to +50° Right)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
            ) {
                val width = size.width
                val height = size.height
                val midX = width / 2f

                // Background slot track
                drawLine(
                    color = Color(0xFF1E2538),
                    start = Offset(0f, height / 2f),
                    end = Offset(width, height / 2f),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Center Notch (0° Upright marker)
                drawLine(
                    color = Color.White.copy(alpha = 0.7f),
                    start = Offset(midX, 0f),
                    end = Offset(midX, height),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Square
                )

                // Lean offset line from center
                val maxScaleDeg = 50f
                val clampedLean = leanDegrees.coerceIn(-maxScaleDeg, maxScaleDeg)
                val indicatorX = midX + (clampedLean / maxScaleDeg) * (midX - 10.dp.toPx())

                drawLine(
                    color = leanStatusColor,
                    start = Offset(midX, height / 2f),
                    end = Offset(indicatorX, height / 2f),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Apex cursor dot
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(indicatorX, height / 2f)
                )
            }
        }
    }
}

@Composable
private fun HudStatPill(
    label: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CarbonDark.copy(alpha = 0.85f))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {
        Column {
            Text(
                text = label,
                color = TextMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    color = color,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = unit,
                        color = TextSecondary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                }
            }
        }
    }
}
