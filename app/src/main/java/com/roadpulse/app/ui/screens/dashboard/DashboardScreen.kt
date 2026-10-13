package com.roadpulse.app.ui.screens.dashboard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.roadpulse.app.data.auth.AuthManager
import com.roadpulse.app.domain.calculator.TripCalculator
import com.roadpulse.app.domain.model.GpsStatus
import com.roadpulse.app.domain.model.TripStatus
import com.roadpulse.app.service.TripRecordingController
import com.roadpulse.app.ui.components.GpsStatusBadge
import com.roadpulse.app.ui.components.SpeedometerGauge
import com.roadpulse.app.ui.components.StatCard
import com.roadpulse.app.ui.theme.CarbonDark
import com.roadpulse.app.ui.theme.CardBorder
import com.roadpulse.app.ui.theme.ElectricBlue
import com.roadpulse.app.ui.theme.MidnightCard
import com.roadpulse.app.ui.theme.NeonCyan
import com.roadpulse.app.ui.theme.RacingRed
import com.roadpulse.app.ui.theme.SportAmber
import com.roadpulse.app.ui.theme.SuccessGreen
import com.roadpulse.app.ui.theme.TextPrimary
import com.roadpulse.app.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun DashboardScreen(
    onNavigateToLiveTrip: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authState by AuthManager.authState.collectAsState()
    val metrics by TripRecordingController.metrics.collectAsState()
    val liveGForce by TripRecordingController.liveGForce.collectAsState()
    val livePoints by TripRecordingController.livePoints.collectAsState()
    val liveSprints = remember(livePoints) {
        com.roadpulse.app.domain.calculator.SprintTimerCalculator.calculateBestSprints(livePoints)
    }

    var hasPermissions by remember {
        mutableStateOf(checkPermissions(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.values.all { it }
        if (hasPermissions && metrics.tripStatus == TripStatus.IDLE) {
            TripRecordingController.startTrip(context)
            onNavigateToLiveTrip()
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonDark)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ROADPULSE",
                    color = NeonCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "DRIVING & RIDING TRACKER",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // User Auth Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MidnightCard)
                        .border(
                            1.dp,
                            if (authState.isLoggedIn) SuccessGreen.copy(alpha = 0.5f) else CardBorder,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { onNavigateToProfile() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (authState.isLoggedIn) Icons.Default.AccountCircle else Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = if (authState.isLoggedIn) SuccessGreen else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (authState.isLoggedIn) {
                            authState.user?.name?.ifEmpty { "Pilot" } ?: "Pilot"
                        } else {
                            "Sign In"
                        },
                        color = if (authState.isLoggedIn) TextPrimary else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                GpsStatusBadge(
                    gpsStatus = metrics.gpsStatus,
                    accuracyMeters = metrics.accuracyMeters
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Speedometer Gauge
        SpeedometerGauge(
            currentSpeedKmh = metrics.currentSpeedKmh,
            maxSpeedKmh = metrics.maxSpeedKmh
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons Row
        if (!hasPermissions) {
            Button(
                onClick = {
                    val permissionsToRequest = mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(permissionsToRequest.toTypedArray())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SportAmber,
                    contentColor = CarbonDark
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Grant Location Permissions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            when (metrics.tripStatus) {
                TripStatus.IDLE, TripStatus.FINISHED -> {
                    Button(
                        onClick = {
                            TripRecordingController.startTrip(context)
                            onNavigateToLiveTrip()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = CarbonDark
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "START TRIP",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
                TripStatus.RECORDING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { TripRecordingController.pauseTrip(context) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SportAmber
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Pause, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PAUSE", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { TripRecordingController.finishTrip(context) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RacingRed,
                                contentColor = TextPrimary
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("FINISH", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                TripStatus.PAUSED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { TripRecordingController.resumeTrip(context) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = CarbonDark
                            )
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RESUME", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { TripRecordingController.finishTrip(context) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RacingRed,
                                contentColor = TextPrimary
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("FINISH", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Primary Metric Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "Distance",
                value = String.format(Locale.US, "%.2f", metrics.totalDistanceKm),
                unit = "KM",
                icon = Icons.Default.Route,
                accentColor = NeonCyan,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Max Speed",
                value = String.format(Locale.US, "%.0f", metrics.maxSpeedKmh),
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
                label = "Moving Avg",
                value = String.format(Locale.US, "%.1f", metrics.avgMovingSpeedKmh),
                unit = "KM/H",
                icon = Icons.Default.Speed,
                accentColor = ElectricBlue,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Elapsed Time",
                value = TripCalculator.formatDuration(metrics.elapsedTimeMs),
                unit = "",
                icon = Icons.Default.Timer,
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
                label = "Moving Time",
                value = TripCalculator.formatDuration(metrics.movingDurationMs),
                unit = "",
                icon = Icons.Default.Timer,
                accentColor = NeonCyan,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Overall Avg",
                value = String.format(Locale.US, "%.1f", metrics.avgOverallSpeedKmh),
                unit = "KM/H",
                icon = Icons.Default.Speed,
                accentColor = TextSecondary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // G-Force Telemetry Crosshairs
        com.roadpulse.app.ui.components.GForceMeter(
            gForce = liveGForce,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Live Sprint Acceleration Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "0-60 KM/H Sprint",
                value = liveSprints.time0to60Seconds?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
                unit = "SEC",
                icon = Icons.Default.Flag,
                accentColor = NeonCyan,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "0-100 KM/H Sprint",
                value = liveSprints.time0to100Seconds?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
                unit = "SEC",
                icon = Icons.Default.Flag,
                accentColor = RacingRed,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

private fun checkPermissions(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val coarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    return fine && coarse
}
