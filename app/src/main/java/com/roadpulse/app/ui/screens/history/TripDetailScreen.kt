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
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.roadpulse.app.data.sync.CloudSyncManager
import com.roadpulse.app.domain.calculator.SprintTimerCalculator
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
import com.roadpulse.app.ui.theme.RhodyIcons
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
    var isSyncing by remember { mutableStateOf(false) }
    var syncStatusMessage by remember { mutableStateOf<String?>(null) }

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
    val sprints = remember(points) {
        SprintTimerCalculator.calculateBestSprints(points)
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
                modifier = Modifier
                    .size(40.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MidnightCard)
                    .border(1.dp, CardBorder, androidx.compose.foundation.shape.CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
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
                    Spacer(modifier = Modifier.width(4.dp))
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
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
        ) {
            RouteMapView(
                points = points,
                currentLat = points.lastOrNull()?.latitude,
                currentLon = points.lastOrNull()?.longitude,
                isLiveTracking = false,
                modifier = Modifier.fillMaxSize()
            )

            // Route Badge Overlay
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MidnightCard.copy(alpha = 0.92f))
                    .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "GPS ROUTE TRACE",
                    color = NeonCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
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
                icon = RhodyIcons.RaceCircuit,
                accentColor = NeonCyan,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Max Speed",
                value = String.format(Locale.US, "%.0f", currentTrip.maxSpeedKmh),
                unit = "KM/H",
                icon = RhodyIcons.Cockpit,
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
                icon = RhodyIcons.Cockpit,
                accentColor = ElectricBlue,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Overall Average",
                value = String.format(Locale.US, "%.1f", currentTrip.avgOverallSpeedKmh),
                unit = "KM/H",
                icon = RhodyIcons.Cockpit,
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
                icon = RhodyIcons.RaceCircuit,
                accentColor = TextMuted,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Sprint Acceleration Section
        Text(
            text = "SPRINT ACCELERATION",
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
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MidnightCard,
                    contentColor = NeonCyan
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("GPX Route", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Button(
                onClick = { ExportManager.shareCsv(context, currentTrip, points) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MidnightCard,
                    contentColor = ElectricBlue
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("CSV Data", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Cloud Telemetry (MongoDB Atlas)
        Text(
            text = "CLOUD TELEMETRY (MONGODB ATLAS)",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    isSyncing = true
                    syncStatusMessage = "Syncing with MongoDB Atlas..."
                    val result = CloudSyncManager.syncTripToMongo(currentTrip, points)
                    isSyncing = false
                    syncStatusMessage = result.getOrElse { it.localizedMessage ?: "Sync error" }
                }
            },
            enabled = !isSyncing,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonCyan,
                contentColor = CarbonDark
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = CarbonDark,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Syncing to Cloud...", fontWeight = FontWeight.Bold)
            } else {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SYNC TO MONGODB ATLAS",
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }

        if (syncStatusMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MidnightCard)
                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = syncStatusMessage!!,
                    color = if (syncStatusMessage!!.contains("Synced", ignoreCase = true)) NeonCyan else SportAmber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Delete Trip Button
        OutlinedButton(
            onClick = { isDeleting = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = RacingRed
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, RacingRed.copy(alpha = 0.6f))
        ) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Delete Trip", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
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
