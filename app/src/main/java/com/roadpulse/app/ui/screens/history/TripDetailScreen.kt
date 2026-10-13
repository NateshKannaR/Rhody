package com.roadpulse.app.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadpulse.app.RoadPulseApp
import com.roadpulse.app.data.database.TrackPointEntity
import com.roadpulse.app.data.database.TripEntity
import com.roadpulse.app.domain.calculator.ChartPoint
import com.roadpulse.app.domain.calculator.TripCalculator
import com.roadpulse.app.export.ExportManager
import com.roadpulse.app.ui.components.InteractiveSpeedChart
import com.roadpulse.app.ui.components.RouteMapView
import com.roadpulse.app.ui.components.StatCard
import com.roadpulse.app.ui.theme.CarbonDark
import com.roadpulse.app.ui.theme.CardBorder
import com.roadpulse.app.ui.theme.ElectricBlue
import com.roadpulse.app.ui.theme.MidnightCard
import com.roadpulse.app.ui.theme.NeonCyan
import com.roadpulse.app.ui.theme.RacingRed
import com.roadpulse.app.ui.theme.SportAmber
import com.roadpulse.app.ui.theme.TextMuted
import com.roadpulse.app.ui.theme.TextPrimary
import com.roadpulse.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TripDetailScreen(
    tripId: Long,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = (context.applicationContext as RoadPulseApp).container.tripRepository
    val coroutineScope = rememberCoroutineScope()

    val tripFlow = remember(tripId) { repository.getTripFlow(tripId) }
    val trip by tripFlow.collectAsState(initial = null)

    var points by remember { mutableStateOf<List<TrackPointEntity>>(emptyList()) }
    var isRenaming by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var isDeleting by remember { mutableStateOf(false) }

    LaunchedEffect(tripId) {
        points = repository.getPoints(tripId)
    }

    val dateFormatter = remember { SimpleDateFormat("EEEE, MMMM d, yyyy • HH:mm:ss", Locale.getDefault()) }

    if (trip == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(CarbonDark),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Loading trip...", color = TextSecondary)
        }
        return
    }

    val currentTrip = trip!!
    val chartPoints = remember(points) {
        points.map { pt ->
            ChartPoint(
                distanceKm = (pt.cumulativeDistanceMeters / 1000.0).toFloat(),
                speedKmh = pt.speedMps * 3.6f,
                timestamp = pt.timestamp,
                altitudeMeters = pt.altitudeMeters
            )
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonDark)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentTrip.title,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1
                    )
                    IconButton(
                        onClick = {
                            newTitle = currentTrip.title
                            isRenaming = true
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename",
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = dateFormatter.format(Date(currentTrip.startTime)),
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Route Map
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
        ) {
            RouteMapView(
                points = points,
                currentLat = points.lastOrNull()?.latitude,
                currentLon = points.lastOrNull()?.longitude,
                isLiveTracking = false,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Speed vs Distance Chart
        InteractiveSpeedChart(
            points = chartPoints,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Metrics Summary Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "Total Distance",
                value = String.format(Locale.US, "%.2f", currentTrip.totalDistanceKm),
                unit = "KM",
                icon = Icons.Default.Route,
                accentColor = NeonCyan,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Max Speed",
                value = String.format(Locale.US, "%.0f", currentTrip.maxSpeedKmh),
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
                value = String.format(Locale.US, "%.1f", currentTrip.avgMovingSpeedKmh),
                unit = "KM/H",
                icon = Icons.Default.Speed,
                accentColor = ElectricBlue,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Overall Average",
                value = String.format(Locale.US, "%.1f", currentTrip.avgOverallSpeedKmh),
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
                value = TripCalculator.formatDuration(currentTrip.movingDurationMs),
                unit = "",
                icon = Icons.Default.Timer,
                accentColor = NeonCyan,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Total Duration",
                value = TripCalculator.formatDuration(currentTrip.totalDurationMs),
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
                value = TripCalculator.formatDuration(currentTrip.pauseDurationMs),
                unit = "",
                icon = Icons.Default.HourglassEmpty,
                accentColor = SportAmber,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "GPS Points",
                value = "${points.size}",
                unit = "PTS",
                icon = Icons.Default.Route,
                accentColor = TextMuted,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Export Actions Section
        Text(
            text = "EXPORT OPTIONS",
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
            Button(
                onClick = { ExportManager.shareGpx(context, currentTrip, points) },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MidnightCard,
                    contentColor = NeonCyan
                )
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("GPX Route", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { ExportManager.shareCsv(context, currentTrip, points) },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MidnightCard,
                    contentColor = ElectricBlue
                )
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("CSV Data", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Delete Trip Button
        OutlinedButton(
            onClick = { isDeleting = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = RacingRed
            )
        ) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Delete Trip", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // Rename Dialog
    if (isRenaming) {
        AlertDialog(
            onDismissRequest = { isRenaming = false },
            title = { Text(text = "Rename Trip", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Trip Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = newTitle.trim()
                        if (trimmed.isNotEmpty()) {
                            coroutineScope.launch {
                                repository.updateTripMetadata(tripId, trimmed, currentTrip.notes)
                            }
                        }
                        isRenaming = false
                    }
                ) {
                    Text("Save", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isRenaming = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = MidnightCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Delete Dialog
    if (isDeleting) {
        AlertDialog(
            onDismissRequest = { isDeleting = false },
            title = { Text(text = "Delete Trip", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you sure you want to delete this trip and its recorded GPS points?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            repository.deleteTrip(tripId)
                            onNavigateBack()
                        }
                    }
                ) {
                    Text("Delete", color = RacingRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isDeleting = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = MidnightCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
