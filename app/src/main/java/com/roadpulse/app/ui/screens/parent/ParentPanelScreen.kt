package com.roadpulse.app.ui.screens.parent

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.roadpulse.app.domain.calculator.TripCalculator
import com.roadpulse.app.service.TripRecordingController
import com.roadpulse.app.ui.components.RouteMapView
import com.roadpulse.app.ui.theme.CarbonDark
import com.roadpulse.app.ui.theme.CardBorder
import com.roadpulse.app.ui.theme.CardBorderGlow
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class GuardianAlertItem(
    val id: String,
    val title: String,
    val message: String,
    val severity: String,
    val timestamp: String
)

@Composable
fun ParentPanelScreen(
    onNavigateToTripDetail: (Long) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = (context.applicationContext as RoadPulseApp).container.tripRepository
    val allTrips by repository.allTrips.collectAsState(initial = emptyList())
    val metrics by TripRecordingController.metrics.collectAsState()
    val livePoints by TripRecordingController.livePoints.collectAsState()

    // Guardian Parent Preferences (Separated from Rider)
    val parentPrefs = remember { context.getSharedPreferences("rhody_parent_prefs", Context.MODE_PRIVATE) }
    val parentName = remember { parentPrefs.getString("parent_name", "Family Guardian") ?: "Family Guardian" }
    val parentEmail = remember { parentPrefs.getString("parent_email", "guardian@family.io") ?: "guardian@family.io" }
    val pairedCode = remember { parentPrefs.getString("paired_rider_code", "RHODY-8821") ?: "RHODY-8821" }
    val monitoredRiderEmail = remember { parentPrefs.getString("monitored_rider_email", "pilot@rhody.io") ?: "pilot@rhody.io" }

    // Vehicle Preferences
    val vehiclePrefs = remember { context.getSharedPreferences("rhody_bike_prefs", Context.MODE_PRIVATE) }
    val bikeModel = remember { vehiclePrefs.getString("bike_model", "Bajaj Pulsar 150") ?: "Bajaj Pulsar 150" }
    val bikePlate = remember { vehiclePrefs.getString("bike_plate", "MH-12-RP-8821") ?: "MH-12-RP-8821" }

    var showLogoutConfirm by remember { mutableStateOf(false) }

    // Dynamic guardian alerts
    val alerts = remember {
        mutableStateListOf(
            GuardianAlertItem(
                id = "1",
                title = "Safe Speed Corridor Maintained",
                message = "Ward ($monitoredRiderEmail) is riding safely within the 95 km/h limit.",
                severity = "normal",
                timestamp = "Active now"
            ),
            GuardianAlertItem(
                id = "2",
                title = "Guardian Radar Link Active",
                message = "Telemetry beacon locked on $bikeModel ($bikePlate).",
                severity = "info",
                timestamp = "Connected"
            )
        )
    }

    val isMoving = metrics.currentSpeedKmh > 3.0f || metrics.isRecording
    val scrollState = rememberScrollState()
    val dateFormatter = remember { SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault()) }

    val guardianPurple = Color(0xFFA855F7)
    val guardianPurpleDark = Color(0xFF1E1035)

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            containerColor = MidnightCard,
            title = {
                Text(
                    text = "Exit Guardian Session?",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Logged in as Guardian: $parentName ($parentEmail).\n\nExiting will end the monitoring session and return to the role gateway.",
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RacingRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Exit Session", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel", color = TextSecondary, fontSize = 12.sp)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonDark)
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ================= 1. GUARDIAN HEADER BAR (CONCISE & FOCUSED) =================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(guardianPurpleDark, MidnightCard)
                    )
                )
                .border(1.dp, guardianPurple.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(guardianPurple.copy(alpha = 0.2f))
                            .border(1.dp, guardianPurple.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Guardian",
                            tint = guardianPurple,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "GUARDIAN SAFETY RADAR",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                color = TextPrimary
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(OptimalGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
                            ) {
                                Text(
                                    text = "LIVE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = OptimalGreen
                                )
                            }
                        }
                        Text(
                            text = "Monitoring Ward: $monitoredRiderEmail",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = HyperCyan
                        )
                    }
                }

                // Quick Action Bar: Call, SOS, Exit
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Call Ward
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(OptimalGreen.copy(alpha = 0.15f))
                            .border(1.dp, OptimalGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+919876543210"))
                                context.startActivity(intent)
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Default.Phone, contentDescription = "Call", tint = OptimalGreen, modifier = Modifier.size(11.dp))
                            Text("Call", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = OptimalGreen)
                        }
                    }

                    // 112 SOS
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RacingRed.copy(alpha = 0.15f))
                            .border(1.dp, RacingRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                                context.startActivity(intent)
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text("112", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = RacingRed)
                    }

                    // Logout / Exit
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CarbonDark)
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                            .clickable { showLogoutConfirm = true }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Exit", tint = TextMuted, modifier = Modifier.size(11.dp))
                            Text("Exit", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle Strip: Machine & Guardian Account
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CarbonDark.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Machine: $bikeModel • $bikePlate",
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )
                Text(
                    text = "Guardian: $parentEmail | Code: $pairedCode",
                    fontSize = 9.sp,
                    color = TextMuted
                )
            }
        }

        // ================= 2. LIVE TELEMETRY STRIP (COMPACT 4-GRID) =================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Motion Status Card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MidnightCard)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Text("STATUS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isMoving) "RIDING" else "PARKED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isMoving) OptimalGreen else HyperCyan
                )
                Text(if (isMoving) "Live GPS" else "Basecamp", fontSize = 8.sp, color = TextMuted)
            }

            // Current Speed
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MidnightCard)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Text("SPEED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format(Locale.US, "%.1f", metrics.currentSpeedKmh),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (metrics.currentSpeedKmh > 95f) RacingRed else NeonCyan
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("km/h", fontSize = 7.5.sp, color = TextMuted)
                }
                Text("Limit 95", fontSize = 8.sp, color = SportAmber, fontWeight = FontWeight.Bold)
            }

            // Peak Speed
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MidnightCard)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Text("PEAK", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Spacer(modifier = Modifier.height(2.dp))
                val peakToday = maxOf(metrics.maxSpeedKmh, allTrips.firstOrNull()?.maxSpeedKmh ?: 0f)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format(Locale.US, "%.1f", peakToday),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = SportAmber
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("km/h", fontSize = 7.5.sp, color = TextMuted)
                }
                Text("Max today", fontSize = 8.sp, color = TextMuted)
            }

            // Ward Phone Battery
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MidnightCard)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Text("BATTERY", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "88%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = OptimalGreen
                )
                Text("Good state", fontSize = 8.sp, color = OptimalGreen)
            }
        }

        // ================= 3. COMPACT LIVE GPS RADAR MAP =================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MidnightCard)
                .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(OptimalGreen))
                    Text(
                        text = "LIVE WARD GPS RADAR MAP",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Text(
                    text = if (metrics.currentLatitude != null) "RTK Lock Active" else "Awaiting Satellite Lock",
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (metrics.currentLatitude != null) OptimalGreen else SportAmber
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            ) {
                RouteMapView(
                    points = livePoints,
                    currentLat = metrics.currentLatitude,
                    currentLon = metrics.currentLongitude,
                    isLiveTracking = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // ================= 4. WARD COMPLETED RIDES & TELEMETRY DETAILS (CORE SECTION) =================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MidnightCard)
                .border(1.dp, CardBorderGlow, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.Route,
                        contentDescription = "Rides",
                        tint = HyperCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "WARD'S COMPLETED RIDES",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        color = HyperCyan
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(HyperCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${allTrips.size} RIDES RECORDED",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        color = HyperCyan
                    )
                }
            }

            Text(
                text = "Tap any ride to inspect full GPS route on map, speed graphs & safety statistics.",
                fontSize = 9.5.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 3.dp, bottom = 10.dp)
            )

            if (allTrips.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CarbonDark)
                        .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                        Text(
                            text = "NO COMPLETED TRIPS YET",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "When your ward completes a motorcycle ride, full GPS telemetry will appear here.",
                            color = TextMuted,
                            fontSize = 9.5.sp
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    allTrips.forEach { trip ->
                        val isSpeedExceeded = trip.maxSpeedKmh > 95f
                        val distanceKm = trip.totalDistanceMeters / 1000.0
                        val durationFormatted = TripCalculator.formatDuration(trip.totalDurationMs)
                        val dateFormatted = remember(trip.startTime) {
                            dateFormatter.format(Date(trip.startTime))
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CarbonDark)
                                .border(
                                    1.dp,
                                    if (isSpeedExceeded) SportAmber.copy(alpha = 0.5f) else CardBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onNavigateToTripDetail(trip.id) }
                                .padding(12.dp)
                        ) {
                            // Trip Card Top Line
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Ride #${trip.id}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "• $dateFormatted",
                                        fontSize = 9.5.sp,
                                        color = TextMuted
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(
                                            if (isSpeedExceeded) SportAmber.copy(alpha = 0.2f) else OptimalGreen.copy(alpha = 0.15f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isSpeedExceeded) "⚠️ EXCEEDED 95 KM/H" else "✓ SAFE CORRIDOR",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isSpeedExceeded) SportAmber else OptimalGreen
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Key Telemetry Line
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("DISTANCE", fontSize = 7.5.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${String.format(Locale.US, "%.1f", distanceKm)} km",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextPrimary
                                    )
                                }

                                Column {
                                    Text("DURATION", fontSize = 7.5.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                    Text(
                                        durationFormatted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextPrimary
                                    )
                                }

                                Column {
                                    Text("TOP SPEED", fontSize = 7.5.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${trip.maxSpeedKmh.roundToInt()} km/h",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isSpeedExceeded) RacingRed else HyperCyan
                                    )
                                }

                                Column {
                                    Text("AVG SPEED", fontSize = 7.5.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${trip.avgMovingSpeedKmh.roundToInt()} km/h",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // One-tap inspect button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ObsidianGlass)
                                    .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "INSPECT FULL GPS ROUTE & TELEMETRY",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HyperCyan,
                                    letterSpacing = 0.5.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = HyperCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ================= 5. SAFETY ALERTS & NOTIFICATIONS FEED =================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MidnightCard)
                .border(1.dp, guardianPurple.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Alerts",
                        tint = guardianPurple,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "SAFETY NOTIFICATIONS & ALERTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(guardianPurple.copy(alpha = 0.2f))
                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = "${alerts.size} ALERTS",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = guardianPurple
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                alerts.forEach { alert ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CarbonDark)
                            .border(
                                1.dp,
                                if (alert.severity == "warning") SportAmber.copy(alpha = 0.5f) else CardBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = alert.title,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (alert.severity == "warning") SportAmber else TextPrimary
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = alert.message,
                                fontSize = 9.5.sp,
                                color = TextSecondary,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Simulate Alert Test Trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Test Guardian Alarm:",
                    fontSize = 9.5.sp,
                    color = TextMuted
                )

                Button(
                    onClick = {
                        alerts.add(
                            0,
                            GuardianAlertItem(
                                id = System.currentTimeMillis().toString(),
                                title = "⚠️ Over-Speed Alert Triggered",
                                message = "Ward ($monitoredRiderEmail) exceeded safe limit of 95 km/h.",
                                severity = "warning",
                                timestamp = "Just now"
                            )
                        )
                        Toast.makeText(context, "⚠️ Over-Speed Guardian Alert Triggered!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SportAmber.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = "⚡ Simulate Speed Warning",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SportAmber
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}
