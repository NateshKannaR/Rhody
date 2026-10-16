package com.roadpulse.app.ui.screens.analytics

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadpulse.app.RoadPulseApp
import com.roadpulse.app.data.database.TrackPointEntity
import com.roadpulse.app.data.database.TripEntity
import com.roadpulse.app.domain.calculator.ChartPoint
import com.roadpulse.app.domain.calculator.SprintTimerCalculator
import com.roadpulse.app.domain.calculator.TripCalculator
import com.roadpulse.app.domain.model.TripStatus
import com.roadpulse.app.export.ExportManager
import com.roadpulse.app.service.TripRecordingController
import com.roadpulse.app.ui.components.InteractiveSpeedChart
import com.roadpulse.app.ui.components.StatCard
import com.roadpulse.app.ui.theme.CarbonDark
import com.roadpulse.app.ui.theme.CardBorder
import com.roadpulse.app.ui.theme.CardBorderGlow
import com.roadpulse.app.ui.theme.CobaltBlue
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = (context.applicationContext as RoadPulseApp).container.tripRepository

    val metrics by TripRecordingController.metrics.collectAsState()
    val livePoints by TripRecordingController.livePoints.collectAsState()

    val allTrips by repository.allTrips.collectAsState(initial = emptyList())
    var selectedTripId by remember { mutableStateOf<Long?>(null) }
    var historicalPoints by remember { mutableStateOf<List<TrackPointEntity>>(emptyList()) }
    var selectedHistoricalTrip by remember { mutableStateOf<TripEntity?>(null) }

    val isLiveActive = metrics.tripStatus == TripStatus.RECORDING || metrics.tripStatus == TripStatus.PAUSED

    LaunchedEffect(allTrips, isLiveActive, selectedTripId) {
        if (!isLiveActive) {
            val tripToLoad = if (selectedTripId != null) {
                allTrips.find { it.id == selectedTripId }
            } else {
                allTrips.firstOrNull()
            }
            if (tripToLoad != null) {
                selectedHistoricalTrip = tripToLoad
                selectedTripId = tripToLoad.id
                withContext(Dispatchers.IO) {
                    historicalPoints = repository.getPoints(tripToLoad.id)
                }
            }
        }
    }

    val displayPoints = if (isLiveActive) livePoints else historicalPoints
    val chartPoints = remember(displayPoints) {
        displayPoints.map { pt ->
            ChartPoint(
                distanceKm = (pt.cumulativeDistanceMeters / 1000.0).toFloat(),
                speedKmh = pt.speedMps * 3.6f,
                timestamp = pt.timestamp,
                altitudeMeters = pt.altitudeMeters
            )
        }
    }

    val sprints = remember(displayPoints) {
        SprintTimerCalculator.calculateBestSprints(displayPoints)
    }

    // Speed Zones Distribution calculation:
    // Low (<40 km/h), Mid Cruising (40-80 km/h), High Apex (>80 km/h)
    val totalPts = displayPoints.size.coerceAtLeast(1)
    val lowSpeedPts = displayPoints.count { it.speedMps * 3.6f < 40f }
    val midSpeedPts = displayPoints.count { val s = it.speedMps * 3.6f; s >= 40f && s <= 80f }
    val highSpeedPts = displayPoints.count { it.speedMps * 3.6f > 80f }

    val lowPct = (lowSpeedPts.toFloat() / totalPts) * 100f
    val midPct = (midSpeedPts.toFloat() / totalPts) * 100f
    val highPct = (highSpeedPts.toFloat() / totalPts) * 100f

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonDark)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        // ================= HEADER =================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TRIP ANALYSIS",
                    color = HyperCyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = if (isLiveActive) "LIVE TELEMETRY SPEED ZONES" else "HISTORICAL TRIP COMPLETION LOG",
                    color = if (isLiveActive) RacingRed else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(ObsidianSurface)
                    .border(
                        1.dp,
                        if (isLiveActive) RacingRed.copy(alpha = 0.6f) else CardBorder,
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (isLiveActive) "● LIVE SESSION" else "LOGGED RIDE",
                    color = if (isLiveActive) RacingRed else OptimalGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ================= TRIP SELECTOR PILL CAROUSEL =================
        if (!isLiveActive && allTrips.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allTrips.forEachIndexed { index, trip ->
                    val isSelected = selectedTripId == trip.id
                    val dateFormatted = remember(trip.startTime) {
                        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(trip.startTime))
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) HyperCyan else ObsidianSurface)
                            .border(
                                1.dp,
                                if (isSelected) HyperCyan else CardBorder,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                selectedTripId = trip.id
                                selectedHistoricalTrip = trip
                                coroutineScope.launch(Dispatchers.IO) {
                                    historicalPoints = repository.getPoints(trip.id)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text(
                                text = "Ride #${trip.id}",
                                color = if (isSelected) CarbonDark else HyperCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = dateFormatted,
                                color = if (isSelected) CarbonDark.copy(alpha = 0.8f) else TextSecondary,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // ================= 1. SPEED VS DISTANCE TELEMETRY CURVE =================
        InteractiveSpeedChart(
            points = chartPoints,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ================= 2. SPEED ZONES BREAKDOWN ("in which speed in each place") =================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MidnightCard)
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SPEED & PACE DISTRIBUTION",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${displayPoints.size} GPS FIXES",
                    color = HyperCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Multi-segment progress bar showing zones
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(CarbonDark)
            ) {
                if (lowPct > 0) {
                    Box(
                        modifier = Modifier
                            .weight(lowPct.coerceAtLeast(1f))
                            .height(8.dp)
                            .background(HyperCyan)
                    )
                }
                if (midPct > 0) {
                    Box(
                        modifier = Modifier
                            .weight(midPct.coerceAtLeast(1f))
                            .height(8.dp)
                            .background(OptimalGreen)
                    )
                }
                if (highPct > 0) {
                    Box(
                        modifier = Modifier
                            .weight(highPct.coerceAtLeast(1f))
                            .height(8.dp)
                            .background(RacingRed)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Zone Breakdown Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ZoneMetricPill(
                    name = "HAIRPINS & URBAN",
                    range = "< 40 KM/H",
                    pct = lowPct.roundToInt(),
                    color = HyperCyan,
                    modifier = Modifier.weight(1f)
                )

                ZoneMetricPill(
                    name = "CRUISING PACE",
                    range = "40 - 80 KM/H",
                    pct = midPct.roundToInt(),
                    color = OptimalGreen,
                    modifier = Modifier.weight(1f)
                )

                ZoneMetricPill(
                    name = "APEX & STRAIGHTS",
                    range = "> 80 KM/H",
                    pct = highPct.roundToInt(),
                    color = RacingRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ================= 3. TRIP COMPLETION SUMMARY ("how he completed the trip") =================
        val distanceKm = if (isLiveActive) metrics.totalDistanceKm else (selectedHistoricalTrip?.totalDistanceKm ?: 0.0)
        val maxSpeed = if (isLiveActive) metrics.maxSpeedKmh else (selectedHistoricalTrip?.maxSpeedKmh ?: 0f)
        val avgMoving = if (isLiveActive) metrics.avgMovingSpeedKmh else (selectedHistoricalTrip?.avgMovingSpeedKmh ?: 0f)
        val movingDuration = if (isLiveActive) metrics.movingDurationMs else (selectedHistoricalTrip?.movingDurationMs ?: 0L)
        val totalDuration = if (isLiveActive) metrics.elapsedTimeMs else (selectedHistoricalTrip?.totalDurationMs ?: 0L)
        val pauseDuration = if (isLiveActive) metrics.pauseDurationMs else (selectedHistoricalTrip?.pauseDurationMs ?: 0L)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MidnightCard)
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = OptimalGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "COMPLETION OVERVIEW",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = if (isLiveActive) "IN PROGRESS" else "TRIP COMPLETED",
                    color = if (isLiveActive) SportAmber else OptimalGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    label = "Total Distance",
                    value = String.format(Locale.US, "%.2f", distanceKm),
                    unit = "KM",
                    icon = Icons.Default.Route,
                    accentColor = HyperCyan,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Peak Speed",
                    value = String.format(Locale.US, "%.0f", maxSpeed),
                    unit = "KM/H",
                    icon = Icons.Default.Speed,
                    accentColor = RacingRed,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    label = "Moving Average",
                    value = String.format(Locale.US, "%.1f", avgMoving),
                    unit = "KM/H",
                    icon = Icons.Default.Speed,
                    accentColor = ElectricBlue,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Moving Time",
                    value = TripCalculator.formatDuration(movingDuration),
                    unit = "",
                    icon = Icons.Default.Timer,
                    accentColor = SportAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ================= 4. SPRINT ACCELERATION BENCHMARKS =================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MidnightCard)
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = HyperCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SPRINT BENCHMARKS",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "GPS TELEMETRY CLOCK",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    label = "0 - 60 KM/H Sprint",
                    value = sprints.time0to60Seconds?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
                    unit = "SEC",
                    icon = Icons.Default.Flag,
                    accentColor = HyperCyan,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "0 - 100 KM/H Sprint",
                    value = sprints.time0to100Seconds?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
                    unit = "SEC",
                    icon = Icons.Default.Flag,
                    accentColor = RacingRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ================= 5. TELEMETRY EXPORT DOCK =================
        if (selectedHistoricalTrip != null && !isLiveActive) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val trip = selectedHistoricalTrip
                        if (trip != null) {
                            coroutineScope.launch {
                                try {
                                    val uri = ExportManager.exportTripToGpx(context, trip, historicalPoints)
                                    if (uri != null) {
                                        ExportManager.shareExportedFile(context, uri, "application/gpx+xml")
                                    } else {
                                        Toast.makeText(context, "Export error", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = HyperCyan
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SHARE GPX", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        val trip = selectedHistoricalTrip
                        if (trip != null) {
                            coroutineScope.launch {
                                try {
                                    val uri = ExportManager.exportTripToCsv(context, trip, historicalPoints)
                                    if (uri != null) {
                                        ExportManager.shareExportedFile(context, uri, "text/csv")
                                    } else {
                                        Toast.makeText(context, "Export error", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HyperCyan,
                        contentColor = CarbonDark
                    )
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("EXPORT CSV", fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))
    }
}

@Composable
private fun ZoneMetricPill(
    name: String,
    range: String,
    pct: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Column {
            Text(
                text = name,
                color = TextMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
            Text(
                text = range,
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$pct%",
                color = color,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
