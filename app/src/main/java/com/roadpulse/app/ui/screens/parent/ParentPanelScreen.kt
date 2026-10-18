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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.roadpulse.app.data.auth.AuthManager
import com.roadpulse.app.data.database.TripEntity
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
    val authState by AuthManager.authState.collectAsState()

    // Guardian Parent Preferences
    val parentPrefs = remember { context.getSharedPreferences("rhody_parent_prefs", Context.MODE_PRIVATE) }
    val parentName = remember { parentPrefs.getString("parent_name", "Family Guardian") ?: "Family Guardian" }
    val pairedCode = remember { parentPrefs.getString("paired_rider_code", "RHODY-8821") ?: "RHODY-8821" }

    // Vehicle Preferences
    val vehiclePrefs = remember { context.getSharedPreferences("rhody_vehicle_prefs", Context.MODE_PRIVATE) }
    val bikeModel = remember { vehiclePrefs.getString("bike_model", "Bajaj Pulsar 150") ?: "Bajaj Pulsar 150" }
    val bikePlate = remember { vehiclePrefs.getString("bike_plate", "MH-12-RP-8821") ?: "MH-12-RP-8821" }

    // Document Vault Preferences
    val docPrefs = remember { context.getSharedPreferences("rhody_doc_vault", Context.MODE_PRIVATE) }
    val rcUploaded = remember { docPrefs.getBoolean("rc_uploaded", true) }
    val dlUploaded = remember { docPrefs.getBoolean("dl_uploaded", true) }
    val insUploaded = remember { docPrefs.getBoolean("insurance_uploaded", true) }
    val pucUploaded = remember { docPrefs.getBoolean("puc_uploaded", true) }

    val riderName = if (authState.isLoggedIn && !authState.user?.name.isNullOrBlank()) {
        authState.user!!.name
    } else {
        "Natesh Kanna"
    }
    val riderEmail = if (authState.isLoggedIn && !authState.user?.email.isNullOrBlank()) {
        authState.user!!.email
    } else {
        "pilot@rhody.io"
    }

    var showLogoutConfirm by remember { mutableStateOf(false) }

    // Dynamic guardian alerts
    val alerts = remember {
        mutableStateListOf(
            GuardianAlertItem(
                id = "1",
                title = "Safe Speed Corridor Maintained",
                message = "Rider $riderName is riding smoothly within the safe 95 km/h limit.",
                severity = "normal",
                timestamp = "Active now"
            ),
            GuardianAlertItem(
                id = "2",
                title = "Guardian Radar Link Active",
                message = "Live telemetry GPS lock established with $bikeModel ($bikePlate).",
                severity = "info",
                timestamp = "Connected"
            )
        )
    }

    val isMoving = metrics.currentSpeedKmh > 3.0f || metrics.isRecording
    val scrollState = rememberScrollState()
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()) }

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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "You are currently logged in as $parentName. Exiting will sign you out of the guardian telemetry radar and return to the login gateway.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RacingRed)
                ) {
                    Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonDark)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ================= 1. GUARDIAN RADAR HEADER & HOTLINES =================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(guardianPurpleDark, MidnightCard)
                    )
                )
                .border(1.dp, guardianPurple.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(guardianPurple.copy(alpha = 0.2f))
                            .border(1.dp, guardianPurple.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Guardian Shield",
                            tint = guardianPurple,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "GUARDIAN SAFETY RADAR",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = TextPrimary
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(OptimalGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "LIVE RADAR",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = OptimalGreen
                                )
                            }
                        }
                        Text(
                            text = "Monitoring Rider: $riderName ($riderEmail)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Text(
                            text = "Machine: $bikeModel • $bikePlate",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = HyperCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pairing Connection & Hotlines Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CarbonDark.copy(alpha = 0.7f))
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "PAIRING CODE:",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Text(
                        text = pairedCode,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = HyperCyan
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Call Rider Hotline
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(OptimalGreen.copy(alpha = 0.15f))
                            .border(1.dp, OptimalGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+919876543210"))
                                context.startActivity(intent)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Call",
                                tint = OptimalGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Call Rider",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OptimalGreen
                            )
                        }
                    }

                    // 112 SOS Hotline
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RacingRed.copy(alpha = 0.15f))
                            .border(1.dp, RacingRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                                context.startActivity(intent)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "112 SOS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = RacingRed
                        )
                    }

                    // Logout Button (Exits Guardian Session)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CarbonDark)
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                            .clickable { showLogoutConfirm = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Logout",
                                tint = TextMuted,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Exit",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // ================= 2. LIVE TELEMETRY RADAR STRIP =================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Motion Status Card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MidnightCard)
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "MOTION STATUS",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isMoving) "RIDING ON ROAD" else "PARKED AT BASE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isMoving) OptimalGreen else HyperCyan
                )
                Text(
                    text = if (isMoving) "Telemetry live" else "Beacon parked",
                    fontSize = 8.5.sp,
                    color = TextMuted
                )
            }

            // Live Speed Card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MidnightCard)
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "CURRENT SPEED",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format(Locale.US, "%.1f", metrics.currentSpeedKmh),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (metrics.currentSpeedKmh > 95f) RacingRed else NeonCyan
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "km/h", fontSize = 8.5.sp, color = TextMuted)
                }
                Text(text = "Limit: 95 km/h", fontSize = 8.5.sp, color = SportAmber, fontWeight = FontWeight.Bold)
            }

            // Peak Speed Logged Today
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MidnightCard)
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "PEAK SPEED",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    val peakToday = maxOf(metrics.maxSpeedKmh, allTrips.firstOrNull()?.maxSpeedKmh ?: 0f)
                    Text(
                        text = String.format(Locale.US, "%.1f", peakToday),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = SportAmber
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "km/h", fontSize = 8.5.sp, color = TextMuted)
                }
                Text(text = "Today's max", fontSize = 8.5.sp, color = TextMuted)
            }

            // Rider Phone Battery
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MidnightCard)
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "RIDER PHONE",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "88%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = OptimalGreen
                )
                Text(text = "Good Health", fontSize = 8.5.sp, color = OptimalGreen, fontWeight = FontWeight.Bold)
            }
        }

        // ================= 3. LIVE RADAR GPS MAP =================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MidnightCard)
                .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(OptimalGreen)
                    )
                    Text(
                        text = "LIVE RIDER GPS BREADCRUMB RADAR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = TextPrimary
                    )
                }

                Text(
                    text = if (metrics.currentLatitude != null) "GPS Lock Active" else "Awaiting Satellite Lock",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (metrics.currentLatitude != null) OptimalGreen else SportAmber
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
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

        // ================= 4. RIDER LEGAL DOCUMENTS VERIFICATION =================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MidnightCard)
                .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RIDER STATUTORY COMPLIANCE & LEGAL VAULT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Statutory Pass",
                    fontSize = 10.sp,
                    color = OptimalGreen,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ParentDocBadge("RC Reg", bikePlate, rcUploaded, if (rcUploaded) OptimalGreen else SportAmber, Modifier.weight(1f))
                ParentDocBadge("Driving Licence", if (dlUploaded) "Valid DL" else "Pending", dlUploaded, if (dlUploaded) HyperCyan else TextMuted, Modifier.weight(1f))
                ParentDocBadge("Insurance", if (insUploaded) "Active Policy" else "Pending", insUploaded, if (insUploaded) OptimalGreen else TextMuted, Modifier.weight(1f))
                ParentDocBadge("PUC Certificate", if (pucUploaded) "Emission Pass" else "Pending", pucUploaded, if (pucUploaded) SportAmber else TextMuted, Modifier.weight(1f))
            }
        }

        // ================= 5. WARD / RIDER TRIP HISTORY & DETAILED TRIP VIEWER =================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MidnightCard)
                .border(1.dp, CardBorderGlow, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Route,
                        contentDescription = "Trips",
                        tint = HyperCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "WARD / RIDER TRIP HISTORY & DETAILS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        color = HyperCyan
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(HyperCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${allTrips.size} RIDES LOGGED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = HyperCyan
                    )
                }
            }

            Text(
                text = "Inspect completed rides, GPS paths, acceleration profiles, and road safety metrics recorded by your rider.",
                fontSize = 10.5.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            if (allTrips.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CarbonDark)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "NO COMPLETED TRIPS YET",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "As soon as your rider records a motorcycle ride, full GPS telemetry, speed charts, and safety compliance will appear here.",
                            color = TextMuted,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                .clip(RoundedCornerShape(14.dp))
                                .background(CarbonDark)
                                .border(
                                    1.dp,
                                    if (isSpeedExceeded) SportAmber.copy(alpha = 0.6f) else CardBorder,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { onNavigateToTripDetail(trip.id) }
                                .padding(14.dp)
                        ) {
                            // Trip Card Top Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Ride #${trip.id}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "•  $dateFormatted",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isSpeedExceeded) SportAmber.copy(alpha = 0.2f) else OptimalGreen.copy(alpha = 0.15f)
                                        )
                                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                                ) {
                                    Text(
                                        text = if (isSpeedExceeded) "⚠️ SPEED EXCEEDED" else "✓ SAFE CORRIDOR",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isSpeedExceeded) SportAmber else OptimalGreen
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Key Telemetry Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "DISTANCE", fontSize = 8.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", distanceKm)} km",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextPrimary
                                    )
                                }

                                Column {
                                    Text(text = "DURATION", fontSize = 8.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = durationFormatted,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextPrimary
                                    )
                                }

                                Column {
                                    Text(text = "TOP SPEED", fontSize = 8.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${trip.maxSpeedKmh.roundToInt()} km/h",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isSpeedExceeded) RacingRed else HyperCyan
                                    )
                                }

                                Column {
                                    Text(text = "AVG SPEED", fontSize = 8.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${trip.avgMovingSpeedKmh.roundToInt()} km/h",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action Button to View Full Details
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ObsidianGlass)
                                    .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "INSPECT GPS ROUTE, ELEVATION & ACCELERATION",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HyperCyan,
                                    letterSpacing = 0.5.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Inspect",
                                    tint = HyperCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ================= 6. REAL-TIME SAFETY NOTIFICATIONS & ALERTS CENTER =================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MidnightCard)
                .border(1.dp, guardianPurple.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Alerts",
                        tint = guardianPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "SAFETY NOTIFICATIONS & ALERTS",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(guardianPurple.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${alerts.size} ALERTS",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = guardianPurple
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                alerts.forEach { alert ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CarbonDark)
                            .border(
                                1.dp,
                                if (alert.severity == "warning") SportAmber.copy(alpha = 0.5f) else CardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = alert.title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (alert.severity == "warning") SportAmber else TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = alert.message,
                                fontSize = 10.sp,
                                color = TextSecondary,
                                lineHeight = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = alert.timestamp,
                                fontSize = 8.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Simulate Alert Test Trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Test Guardian Alarm:",
                    fontSize = 10.sp,
                    color = TextMuted
                )

                Button(
                    onClick = {
                        alerts.add(
                            0,
                            GuardianAlertItem(
                                id = System.currentTimeMillis().toString(),
                                title = "⚠️ Over-Speed Alert Triggered",
                                message = "Rider $riderName hit 104 km/h (exceeded safe speed limit of 95 km/h).",
                                severity = "warning",
                                timestamp = "Just now"
                            )
                        )
                        Toast.makeText(context, "⚠️ Over-Speed Guardian Alert Triggered!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SportAmber.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = "⚡ Simulate Speed Warning",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = SportAmber
                    )
                }
            }
        }

        // ================= 7. ENFORCED SAFETY GUARDRAILS INFO =================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(guardianPurpleDark.copy(alpha = 0.6f), CarbonDark)
                    )
                )
                .border(1.dp, guardianPurple.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = guardianPurple,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "PARENTAL SAFETY GUARDRAILS ENFORCED",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Text(
                text = "• Speed Alert Limit: Triggers instant notification if rider exceeds 95 km/h.",
                fontSize = 9.5.sp,
                color = TextSecondary
            )
            Text(
                text = "• Crash & Tilt Beacon: Automated SOS broadcast to guardian phone upon high-G impact.",
                fontSize = 9.5.sp,
                color = TextSecondary
            )
            Text(
                text = "• Real-Time Beacon: Live GPS telemetry refreshes every 3 seconds during rides.",
                fontSize = 9.5.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ParentDocBadge(
    label: String,
    status: String,
    verified: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CarbonDark.copy(alpha = 0.7f))
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = status,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            maxLines = 1
        )
    }
}
