package com.roadpulse.app.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.roadpulse.app.service.TripRecordingController
import com.roadpulse.app.ui.components.InteractiveSpeedChart
import com.roadpulse.app.ui.components.StatCard
import com.roadpulse.app.ui.theme.CarbonDark
import com.roadpulse.app.ui.theme.ElectricBlue
import com.roadpulse.app.ui.theme.NeonCyan
import com.roadpulse.app.ui.theme.RacingRed
import com.roadpulse.app.ui.theme.SportAmber
import com.roadpulse.app.ui.theme.TextPrimary
import com.roadpulse.app.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = (context.applicationContext as RoadPulseApp).container.tripRepository

    val metrics by TripRecordingController.metrics.collectAsState()
    val livePoints by TripRecordingController.livePoints.collectAsState()

    val allTrips by repository.allTrips.collectAsState(initial = emptyList())
    var selectedHistoricalTrip by remember { mutableStateOf<TripEntity?>(null) }
    var historicalPoints by remember { mutableStateOf<List<TrackPointEntity>>(emptyList()) }

    val isLiveActive = metrics.tripStatus == TripStatus.RECORDING || metrics.tripStatus == TripStatus.PAUSED

    LaunchedEffect(allTrips, isLiveActive) {
        if (!isLiveActive && allTrips.isNotEmpty() && selectedHistoricalTrip == null) {
            val latest = allTrips.first()
            selectedHistoricalTrip = latest
            historicalPoints = repository.getPoints(latest.id)
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

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonDark)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "TRIP ANALYTICS",
            color = NeonCyan,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
        Text(
            text = if (isLiveActive) "ACTIVE RECORDING TELEMETRY" else "LATEST TRIP PERFORMANCE",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Interactive Speed vs Distance Chart
        InteractiveSpeedChart(
            points = chartPoints,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Summary Metric Cards
        val distanceKm = if (isLiveActive) metrics.totalDistanceKm else (selectedHistoricalTrip?.totalDistanceKm ?: 0.0)
        val maxSpeed = if (isLiveActive) metrics.maxSpeedKmh else (selectedHistoricalTrip?.maxSpeedKmh ?: 0f)
        val avgMoving = if (isLiveActive) metrics.avgMovingSpeedKmh else (selectedHistoricalTrip?.avgMovingSpeedKmh ?: 0f)
        val avgOverall = if (isLiveActive) metrics.avgOverallSpeedKmh else (selectedHistoricalTrip?.avgOverallSpeedKmh ?: 0f)
        val movingDuration = if (isLiveActive) metrics.movingDurationMs else (selectedHistoricalTrip?.movingDurationMs ?: 0L)
        val totalDuration = if (isLiveActive) metrics.elapsedTimeMs else (selectedHistoricalTrip?.totalDurationMs ?: 0L)
        val pauseDuration = if (isLiveActive) metrics.pauseDurationMs else (selectedHistoricalTrip?.pauseDurationMs ?: 0L)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "Total Distance",
                value = String.format(Locale.US, "%.2f", distanceKm),
                unit = "KM",
                icon = Icons.Default.Route,
                accentColor = NeonCyan,
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

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                label = "Overall Average",
                value = String.format(Locale.US, "%.1f", avgOverall),
                unit = "KM/H",
                icon = Icons.Default.Speed,
                accentColor = SportAmber,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "Moving Duration",
                value = TripCalculator.formatDuration(movingDuration),
                unit = "",
                icon = Icons.Default.Timer,
                accentColor = NeonCyan,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Total Duration",
                value = TripCalculator.formatDuration(totalDuration),
                unit = "",
                icon = Icons.Default.Timer,
                accentColor = TextSecondary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "Pause Time",
                value = TripCalculator.formatDuration(pauseDuration),
                unit = "",
                icon = Icons.Default.HourglassEmpty,
                accentColor = SportAmber,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Points Logged",
                value = "${displayPoints.size}",
                unit = "PTS",
                icon = Icons.Default.Route,
                accentColor = TextSecondary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "SPRINT PERFORMANCE (0-60 & 0-100)",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "0 - 60 KM/H",
                value = sprints.time0to60Seconds?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
                unit = "SEC",
                icon = Icons.Default.Flag,
                accentColor = NeonCyan,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "0 - 100 KM/H",
                value = sprints.time0to100Seconds?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
                unit = "SEC",
                icon = Icons.Default.Flag,
                accentColor = RacingRed,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
