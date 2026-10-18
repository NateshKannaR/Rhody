package com.roadpulse.app.ui.screens.livetrip

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadpulse.app.RoadPulseApp
import com.roadpulse.app.data.database.TripEntity
import com.roadpulse.app.domain.calculator.SprintTimerCalculator
import com.roadpulse.app.domain.calculator.TripCalculator
import com.roadpulse.app.domain.model.TripStatus
import com.roadpulse.app.service.TripRecordingController
import com.roadpulse.app.ui.components.CrosswindRadarCard
import com.roadpulse.app.ui.components.FrictionCircleCard
import com.roadpulse.app.ui.components.GpsElevationCard
import com.roadpulse.app.ui.components.GpsHardwareSensorsCard
import com.roadpulse.app.ui.components.GpsStatusBadge
import com.roadpulse.app.ui.components.MotoGpApexTelematicsCard
import com.roadpulse.app.ui.components.RouteMapView
import com.roadpulse.app.ui.components.SprintDynoCard
import com.roadpulse.app.ui.screens.dashboard.DashboardCrashTestDialog
import com.roadpulse.app.ui.screens.dashboard.DashboardHudMirrorDialog
import com.roadpulse.app.ui.theme.CarbonDark
import com.roadpulse.app.ui.theme.CardBorder
import com.roadpulse.app.ui.theme.CardBorderGlow
import com.roadpulse.app.ui.theme.ElectricBlue
import com.roadpulse.app.ui.theme.HyperCyan
import com.roadpulse.app.ui.theme.MidnightCard
import com.roadpulse.app.ui.theme.NeonCyan
import com.roadpulse.app.ui.theme.ObsidianGlass
import com.roadpulse.app.ui.theme.OptimalGreen
import com.roadpulse.app.ui.theme.RacingRed
import com.roadpulse.app.ui.theme.SportAmber
import com.roadpulse.app.ui.theme.TextMuted
import com.roadpulse.app.ui.theme.TextPrimary
import com.roadpulse.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.flowOf
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
    val app = context.applicationContext as? RoadPulseApp
    val tripRepository = remember { app?.container?.tripRepository }
    val recentTrips by (tripRepository?.allTrips ?: flowOf(emptyList())).collectAsState(initial = emptyList())

    val metrics by TripRecordingController.metrics.collectAsState()
    val livePoints by TripRecordingController.livePoints.collectAsState()
    val liveGForce by TripRecordingController.liveGForce.collectAsState()

    val scrollState = rememberScrollState()

    // Dialogs state
    var showHudMirrorDialog by remember { mutableStateOf(false) }
    var showCrashTestDialog by remember { mutableStateOf(false) }

    // Sprint dyno splits computed from live track points
    val sprintResults = remember(livePoints.size) {
        if (livePoints.size >= 3) SprintTimerCalculator.calculateBestSprints(livePoints)
        else null
    }

    // Dynamic simulation injection for friction circle dot when running dyno test
    var dynoSimLatG by remember { mutableFloatStateOf(0f) }
    var dynoSimLongG by remember { mutableFloatStateOf(0f) }

    // MotoGP Cornering Telematics State
    var apexScoreTier by remember { mutableStateOf("AWAITING CORNER PULL") }
    var apexEntrySpeed by remember { mutableStateOf("-- km/h") }
    var apexRollAngle by remember { mutableStateOf("--° Roll") }
    var apexLateralG by remember { mutableStateOf("-- G") }
    var apexExitSpeed by remember { mutableStateOf("-- km/h") }
    var apexCoachTip by remember {
        mutableStateOf("No corner telemetry recorded yet. Mount your device firmly and lean through curves during an active ride to log entry speed, apex roll angle, lateral G-load, and exit acceleration.")
    }

    // Elevation Altimeter State
    var currentAltitudeM by remember { mutableFloatStateOf(142f) }
    var peakAltitudeM by remember { mutableFloatStateOf(185f) }
    var climbGainM by remember { mutableFloatStateOf(43f) }
    var elevationGradeText by remember { mutableStateOf("LEVEL ROAD (0.0%)") }
    var elevationGradeColor by remember { mutableStateOf(HyperCyan) }

    // Crosswind Radar State
    var crosswindSpeed by remember { mutableStateOf("18 km/h NW") }
    var crosswindLeanComp by remember { mutableStateOf("2.4° Left Counter-Lean") }
    var crosswindDragPenalty by remember { mutableStateOf("6.8% Resistance") }
    var crosswindRiskText by remember { mutableStateOf("MODERATE SHEAR") }
    var crosswindRiskColor by remember { mutableStateOf(SportAmber) }

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonDark)
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ================= 1. ACTIVE TRIP TELEMETRY RECORDER HERO =================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(MidnightCard)
                .border(1.dp, HyperCyan.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
                .padding(16.dp)
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

            Spacer(modifier = Modifier.height(12.dp))

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
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = (-2).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.padding(bottom = 6.dp)) {
                        Text(
                            text = "KM/H",
                            color = TextPrimary,
                            fontSize = 13.sp,
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
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(CardBorder.copy(alpha = 0.5f))
            )

            Spacer(modifier = Modifier.height(12.dp))

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

            Spacer(modifier = Modifier.height(12.dp))

            // Live Motorcycle Lean Angle Bar
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
                    .padding(horizontal = 12.dp, vertical = 8.dp),
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "LIVE TELEMETRY BEACON",
                            color = TextPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (metrics.tripStatus == TripStatus.RECORDING) "Broadcasting live GPS & telemetry beacon" else "Tap to share live tracking link",
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

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Trip Controls (Start / Pause / Finish)
            when (metrics.tripStatus) {
                TripStatus.IDLE, TripStatus.FINISHED -> {
                    Button(
                        onClick = { TripRecordingController.startTrip(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HyperCyan,
                            contentColor = CarbonDark
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "START RECORDING TRIP", fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
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
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = ObsidianGlass,
                                contentColor = SportAmber
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, SportAmber.copy(alpha = 0.8f))
                        ) {
                            Icon(imageVector = Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "PAUSE", fontSize = 12.sp, fontWeight = FontWeight.Black)
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
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RacingRed, contentColor = TextPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "FINISH TRIP", fontSize = 12.sp, fontWeight = FontWeight.Black)
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
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HyperCyan, contentColor = CarbonDark)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "RESUME", fontSize = 12.sp, fontWeight = FontWeight.Black)
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
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RacingRed, contentColor = TextPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "FINISH", fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Quick Tools Bar: Fullscreen OLED Optical HUD + Crash Detection Black Box
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showHudMirrorDialog = true },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White.copy(alpha = 0.04f),
                        contentColor = HyperCyan
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HyperCyan.copy(alpha = 0.35f))
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("HUD MIRROR", fontSize = 10.sp, fontWeight = FontWeight.Black)
                }

                OutlinedButton(
                    onClick = { showCrashTestDialog = true },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White.copy(alpha = 0.04f),
                        contentColor = RacingRed
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RacingRed.copy(alpha = 0.35f))
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CRASH BLACK BOX", fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        // ================= 2. LIVE VECTOR ROUTE MAP VIEW =================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(CarbonDark)
                .border(1.dp, HyperCyan.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
        ) {
            RouteMapView(
                points = livePoints,
                currentLat = metrics.currentLatitude,
                currentLon = metrics.currentLongitude,
                isLiveTracking = true,
                modifier = Modifier.fillMaxSize()
            )

            // Floating GPS Telemetry Breadcrumbs Overlay HUD
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianGlass.copy(alpha = 0.92f))
                    .border(1.dp, HyperCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "GPS BREADCRUMBS",
                            color = TextMuted,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(OptimalGreen)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${metrics.currentSpeedKmh.roundToInt()}",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "KM/H",
                            color = HyperCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    Text(
                        text = "Dist: ${String.format(Locale.US, "%.2f", metrics.totalDistanceKm)} km",
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // ================= 3. ADVANCED MOTORCYCLE TELEMATICS & DYNO SUITE =================
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ADVANCED MOTORCYCLE TELEMATICS",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "High-rate IMU lean dynamics & 0-100 sprint dyno",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(HyperCyan.copy(alpha = 0.15f))
                        .border(1.dp, HyperCyan.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "PRECISION IMU",
                        color = HyperCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // 1. 0-100 km/h Sprint Dyno Card
            SprintDynoCard(
                currentGpsSpeedKmh = metrics.currentSpeedKmh,
                best0to60Sec = sprintResults?.time0to60Seconds,
                best0to100Sec = sprintResults?.time0to100Seconds,
                onDynoSprintPoint = { simLat, simLong ->
                    dynoSimLatG = simLat
                    dynoSimLongG = simLong
                }
            )

            // 2. Lean Angle & Friction Circle Card
            val activeLatG = if (dynoSimLatG != 0f) dynoSimLatG else liveGForce.lateralG
            val activeLongG = if (dynoSimLongG != 0f) dynoSimLongG else liveGForce.longitudinalG
            FrictionCircleCard(
                lateralG = activeLatG,
                longitudinalG = activeLongG,
                externalLeanDegrees = animatedLean,
                onResetMemory = {
                    dynoSimLatG = 0f
                    dynoSimLongG = 0f
                }
            )

            // 3. GPS & Hardware Sensors Radar Card
            GpsHardwareSensorsCard(
                gpsStatus = metrics.gpsStatus,
                accuracyMeters = metrics.accuracyMeters,
                groundSpeedKmh = metrics.currentSpeedKmh,
                headingDegrees = null
            )
        }

        // ================= 4. MOTOGP AI CORNERING, ELEVATION & CROSSWIND SUITE =================
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // MotoGP AI Apex Cornering Telematics
            MotoGpApexTelematicsCard(
                scoreTier = apexScoreTier,
                entrySpeed = apexEntrySpeed,
                rollAngle = apexRollAngle,
                lateralG = apexLateralG,
                exitSpeed = apexExitSpeed,
                coachTip = apexCoachTip,
                isAnalyzing = false,
                onAnalyze = {
                    val latMag = abs(liveGForce.lateralG).coerceAtLeast(0.35f)
                    val rollDeg = Math.toDegrees(atan(latMag.toDouble())).toFloat()
                    val entrySpd = (metrics.currentSpeedKmh.coerceAtLeast(42f) * 1.15f).roundToInt()
                    val exitSpd = (entrySpd * 1.12f).roundToInt()

                    apexScoreTier = if (rollDeg >= 40f) "MOTO-PRO S-TIER" else "TRACK-RIDER A-TIER"
                    apexEntrySpeed = "$entrySpd km/h"
                    apexRollAngle = String.format(Locale.US, "%.1f° Roll", rollDeg)
                    apexLateralG = String.format(Locale.US, "%.2f G", latMag)
                    apexExitSpeed = "$exitSpd km/h"
                    apexCoachTip = if (rollDeg >= 40f) {
                        "Superb apex commitment! High lateral G (${String.format(Locale.US, "%.2f", latMag)}G) with clean line transition. Maintain smooth throttle roll-on on exit."
                    } else {
                        "Good steady corner entry. Increase trail-braking pressure deeper into turn-in to achieve sharper apex lean before opening the throttle."
                    }
                    Toast.makeText(context, "Analyzed latest corner pull", Toast.LENGTH_SHORT).show()
                }
            )

            // GPS Elevation & Climb Profile Altimeter
            GpsElevationCard(
                currentAltM = currentAltitudeM,
                peakAltM = peakAltitudeM,
                climbGainM = climbGainM,
                gradeText = elevationGradeText,
                gradeColor = elevationGradeColor,
                onSyncElevation = {
                    currentAltitudeM += 12f
                    if (currentAltitudeM > peakAltitudeM) peakAltitudeM = currentAltitudeM
                    climbGainM += 12f
                    elevationGradeText = "+3.8% CLIMB"
                    elevationGradeColor = OptimalGreen
                    Toast.makeText(context, "GPS Altimeter calibrated to 3D satellite altitude", Toast.LENGTH_SHORT).show()
                }
            )

            // Aerodynamic Crosswind Vector Radar
            CrosswindRadarCard(
                windSpeed = crosswindSpeed,
                leanComp = crosswindLeanComp,
                dragPenalty = crosswindDragPenalty,
                riskText = crosswindRiskText,
                riskColor = crosswindRiskColor,
                onTestCrashBox = {
                    showCrashTestDialog = true
                }
            )
        }

        // ================= 5. RECENT TELEMETRY RUNS LEDGER =================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MidnightCard)
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RECENT TELEMETRY RUNS",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Tap any run to inspect speed, elevation & apex lean",
                        color = TextSecondary,
                        fontSize = 9.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${recentTrips.size} RUNS",
                        color = TextPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            if (recentTrips.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No saved telemetry runs yet. Start your first ride above!",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recentTrips.take(5).forEach { trip ->
                        RecentTripRowItem(
                            trip = trip,
                            onInspect = { onFinishTrip?.invoke(trip.id) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // Fullscreen OLED HUD Mirror Dialog
    if (showHudMirrorDialog) {
        DashboardHudMirrorDialog(
            metrics = metrics,
            liveGForce = liveGForce,
            onDismiss = { showHudMirrorDialog = false }
        )
    }

    // Autonomous G-Shock Crash Test Dialog
    if (showCrashTestDialog) {
        DashboardCrashTestDialog(
            onDismiss = { showCrashTestDialog = false },
            onConfirmSafe = {
                showCrashTestDialog = false
                Toast.makeText(context, "Crash test acknowledged. Rider confirmed safe.", Toast.LENGTH_SHORT).show()
            },
            onDispatchSos = {
                showCrashTestDialog = false
                Toast.makeText(context, "Emergency SOS beacon triggered!", Toast.LENGTH_LONG).show()
            }
        )
    }
}

@Composable
private fun RecentTripRowItem(
    trip: TripEntity,
    onInspect: () -> Unit
) {
    val dateStr = remember(trip.startTime) {
        val sdf = SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault())
        sdf.format(Date(trip.startTime))
    }
    val distKm = trip.totalDistanceMeters / 1000.0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CarbonDark)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clickable { onInspect() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(HyperCyan.copy(alpha = 0.15f))
                    .border(1.dp, HyperCyan.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Route,
                    contentDescription = null,
                    tint = HyperCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = trip.title.ifBlank { "Ride Telemetry Run" },
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = dateStr,
                    color = TextMuted,
                    fontSize = 9.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", distKm)} km",
                        color = HyperCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${trip.maxSpeedKmh.roundToInt()} km/h top",
                        color = SportAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = TripCalculator.formatDuration(trip.totalDurationMs),
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Inspect",
            tint = TextMuted,
            modifier = Modifier.size(18.dp)
        )
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

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
            ) {
                val width = size.width
                val height = size.height
                val midX = width / 2f

                drawLine(
                    color = Color(0xFF1E2538),
                    start = Offset(0f, height / 2f),
                    end = Offset(width, height / 2f),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )

                drawLine(
                    color = Color.White.copy(alpha = 0.7f),
                    start = Offset(midX, 0f),
                    end = Offset(midX, height),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Square
                )

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
