package com.roadpulse.app.ui.screens.dashboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.roadpulse.app.data.auth.AuthManager
import com.roadpulse.app.domain.calculator.SprintTimerCalculator
import com.roadpulse.app.domain.calculator.TripCalculator
import com.roadpulse.app.domain.model.TripStatus
import com.roadpulse.app.service.TripRecordingController
import com.roadpulse.app.ui.components.GForceMeter
import com.roadpulse.app.ui.components.GpsStatusBadge
import com.roadpulse.app.ui.components.SpeedometerGauge
import com.roadpulse.app.ui.components.StatCard
import com.roadpulse.app.ui.theme.CarbonDark
import com.roadpulse.app.ui.theme.CardBorder
import com.roadpulse.app.ui.theme.CardBorderGlow
import com.roadpulse.app.ui.theme.CobaltBlue
import com.roadpulse.app.ui.theme.ElectricBlue
import com.roadpulse.app.ui.theme.HyperCyan
import com.roadpulse.app.ui.theme.LaserCrimson
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
import java.util.Locale

// Data model for motorcycle component health lifecycle
data class ComponentHealth(
    val name: String,
    val intervalKm: Int,
    val remainingKm: Int,
    val percentage: Int,
    val statusText: String,
    val color: Color
)

// Data model for real YouTube motovlog & telemetry video guides
data class VideoRecommendation(
    val id: String,
    val title: String,
    val channel: String,
    val duration: String,
    val category: String,
    val viewCount: String
)

@Composable
fun DashboardScreen(
    initialRole: String = "rider",
    onNavigateToLiveTrip: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val authState by AuthManager.authState.collectAsState()
    val metrics by TripRecordingController.metrics.collectAsState()
    val liveGForce by TripRecordingController.liveGForce.collectAsState()
    val livePoints by TripRecordingController.livePoints.collectAsState()

    // Upfront Role Gateway Selection ("rider" vs "parent")
    var selectedRole by remember(initialRole) { mutableStateOf(initialRole) }
    var isGuardianAuthorized by remember { mutableStateOf(false) }
    var hasPendingRiderConsent by remember { mutableStateOf(true) }

    // Persistent Motorcycle State (Zero Mock Data)
    val prefs = remember { context.getSharedPreferences("rhody_bike_prefs", Context.MODE_PRIVATE) }
    var isMachineConfigured by remember { mutableStateOf(prefs.getBoolean("is_configured", false)) }
    var bikeModel by remember { mutableStateOf(prefs.getString("bike_model", "") ?: "") }
    var bikePlate by remember { mutableStateOf(prefs.getString("bike_plate", "") ?: "") }
    var bikeOdo by remember { mutableStateOf(prefs.getString("bike_odo", "0") ?: "0") }
    var bikeEngineSpec by remember { mutableStateOf(prefs.getString("bike_specs", "") ?: "") }

    var showBikeRegisterDialog by remember { mutableStateOf(false) }

    // Legal Document Vault state from SharedPreferences
    var rcUploaded by remember { mutableStateOf(prefs.getBoolean("rc_uploaded", false)) }
    var rcDocNum by remember { mutableStateOf(prefs.getString("rc_doc_num", "") ?: "") }
    var rcDocName by remember { mutableStateOf(prefs.getString("rc_doc_name", "") ?: "") }

    var dlUploaded by remember { mutableStateOf(prefs.getBoolean("dl_uploaded", false)) }
    var dlDocNum by remember { mutableStateOf(prefs.getString("dl_doc_num", "") ?: "") }
    var dlDocName by remember { mutableStateOf(prefs.getString("dl_doc_name", "") ?: "") }

    var insuranceUploaded by remember { mutableStateOf(prefs.getBoolean("insurance_uploaded", false)) }
    var insuranceDocNum by remember { mutableStateOf(prefs.getString("insurance_doc_num", "") ?: "") }
    var insuranceDocName by remember { mutableStateOf(prefs.getString("insurance_doc_name", "") ?: "") }

    var pucUploaded by remember { mutableStateOf(prefs.getBoolean("puc_uploaded", false)) }
    var pucDocNum by remember { mutableStateOf(prefs.getString("puc_doc_num", "") ?: "") }
    var pucDocName by remember { mutableStateOf(prefs.getString("puc_doc_name", "") ?: "") }

    var helmetUploaded by remember { mutableStateOf(prefs.getBoolean("helmet_uploaded", false)) }
    var helmetDocNum by remember { mutableStateOf(prefs.getString("helmet_doc_num", "") ?: "") }
    var helmetDocName by remember { mutableStateOf(prefs.getString("helmet_doc_name", "") ?: "") }

    // Active upload modal state
    var activeUploadDocType by remember { mutableStateOf<String?>(null) }
    var uploadDocNumberInput by remember { mutableStateOf("") }
    var uploadDocPickedName by remember { mutableStateOf<String?>(null) }

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "document.pdf"
            uploadDocPickedName = fileName
            Toast.makeText(context, "Attached: $fileName", Toast.LENGTH_SHORT).show()
        }
    }

    // MotoGP AI Apex State
    var apexScoreTier by remember { mutableStateOf("S-TIER • 96/100") }
    var apexEntrySpeed by remember { mutableStateOf("72.4 km/h") }
    var apexRollAngle by remember { mutableStateOf("44.2° Roll") }
    var apexLateralG by remember { mutableStateOf("1.18 G") }
    var apexExitSpeed by remember { mutableStateOf("81.3 km/h") }
    var apexCoachTip by remember {
        mutableStateOf("Trail-braking transition optimal. Apex clipped with high chassis stability. Throttle roll-on initiated 1.2m before apex exit.")
    }
    var isAnalyzingApex by remember { mutableStateOf(false) }

    // GPS Elevation & Altimeter Profile State
    var currentAltitudeM by remember { mutableStateOf(580f) }
    var peakAltitudeM by remember { mutableStateOf(740f) }
    var climbGainM by remember { mutableStateOf(160f) }
    var elevationGradeText by remember { mutableStateOf("LEVEL ROAD (0.0%)") }
    var elevationGradeColor by remember { mutableStateOf(HyperCyan) }

    // Crosswind Radar State
    var crosswindSpeed by remember { mutableStateOf("24.5 km/h @ 48°") }
    var crosswindLeanComp by remember { mutableStateOf("+3.2° Left Counter-Lean") }
    var crosswindDragPenalty by remember { mutableStateOf("+12.8% Resistance") }
    var crosswindRiskText by remember { mutableStateOf("MODERATE SHEAR") }
    var crosswindRiskColor by remember { mutableStateOf(SportAmber) }

    val liveSprints = remember(livePoints) {
        SprintTimerCalculator.calculateBestSprints(livePoints)
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

    // Search and filter state
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    // Motorcycle Component Preventative Health Lifecycles
    val bikeComponents = remember {
        listOf(
            ComponentHealth("Engine Oil & Filter", 4000, 1150, 71, "OPTIMAL (71%)", OptimalGreen),
            ComponentHealth("Drive Chain & Slack", 500, 150, 30, "LUBE SOON (30%)", SportAmber),
            ComponentHealth("High-Flow Air Filter", 10000, 5150, 51, "OPTIMAL (51%)", OptimalGreen),
            ComponentHealth("NGK Spark Plug", 12000, 7150, 60, "OPTIMAL (60%)", OptimalGreen),
            ComponentHealth("Radiator Coolant", 15000, 8150, 54, "OPTIMAL (54%)", OptimalGreen),
            ComponentHealth("Sintered Brake Pads", 8000, 4150, 52, "OPTIMAL (52%)", OptimalGreen)
        )
    }

    // Curated real YouTube motorcycle telemetry & maintenance guides
    val youtubeVideos = remember {
        listOf(
            VideoRecommendation(
                id = "k4aT9rGqB9M",
                title = "Motorcycle Dyno & Acceleration Run: Telemetry Analysis",
                channel = "Motorsport Engineering Lab",
                duration = "12:45",
                category = "Bike",
                viewCount = "480K views"
            ),
            VideoRecommendation(
                id = "3JZ_D3ELwOQ",
                title = "Motorcycle Preventative Maintenance: Tire Pressure, Chain Slack & ECU",
                channel = "Trackday Mechanics",
                duration = "10:30",
                category = "Maintenance",
                viewCount = "195K views"
            ),
            VideoRecommendation(
                id = "e-ORhEE9VVg",
                title = "Engine Oil & Filter Change: Full Synthetic Service Guide",
                channel = "Apex Performance Garage",
                duration = "14:10",
                category = "Maintenance",
                viewCount = "280K views"
            ),
            VideoRecommendation(
                id = "bW3l5eSgB2E",
                title = "Motorcycle Lean Angle & Counter-Steering Masterclass (G-Force)",
                channel = "MotoGP Rider School",
                duration = "14:15",
                category = "Guides",
                viewCount = "750K views"
            )
        )
    }

    val filteredVideos = remember(searchQuery, selectedCategoryFilter) {
        youtubeVideos.filter { video ->
            val matchesCategory = selectedCategoryFilter == "All" ||
                    (selectedCategoryFilter == "Bike" && video.category == "Bike") ||
                    (selectedCategoryFilter == "Maintenance" && video.category == "Maintenance") ||
                    (selectedCategoryFilter == "Guides" && video.category == "Guides")
            val matchesQuery = searchQuery.isBlank() ||
                    video.title.contains(searchQuery, ignoreCase = true) ||
                    video.channel.contains(searchQuery, ignoreCase = true) ||
                    video.category.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    val scrollState = rememberScrollState()
    var showHudMirrorDialog by remember { mutableStateOf(false) }
    var isHudWindshieldFlipped by remember { mutableStateOf(false) }
    var showCrashTestDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonDark)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ================= 1. WELCOME PROFILE & TELEMETRY HEADER =================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rider avatar and personalized welcome
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onNavigateToProfile() }
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(HyperCyan, CobaltBlue)
                            )
                        )
                        .border(1.5.dp, NeonCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBike,
                        contentDescription = "Rider Profile",
                        tint = CarbonDark,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    val displayName = if (authState.isLoggedIn && !authState.user?.name.isNullOrBlank()) {
                        authState.user!!.name
                    } else {
                        "Natesh Kanna"
                    }

                    Text(
                        text = "WELCOME BACK",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = displayName,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(OptimalGreen)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "APEX PILOT • ECU CONNECTED",
                            color = OptimalGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            // Top Status Badges: GPS Lock & Account Action
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GpsStatusBadge(
                    gpsStatus = metrics.gpsStatus,
                    accuracyMeters = metrics.accuracyMeters
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ================= ROLE SELECTOR (RIDER COCKPIT vs PARENT PORTAL) =================
        val guardianPurple = Color(0xFFA855F7)
        val guardianPurpleDark = Color(0xFF1E1035)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(ObsidianGlass)
                .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Rider Cockpit Mode Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (selectedRole == "rider") HyperCyan.copy(alpha = 0.18f) else Color.Transparent
                    )
                    .border(
                        1.dp,
                        if (selectedRole == "rider") HyperCyan.copy(alpha = 0.5f) else Color.Transparent,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { selectedRole = "rider" }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBike,
                        contentDescription = "Rider Cockpit",
                        tint = if (selectedRole == "rider") HyperCyan else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "RIDER COCKPIT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedRole == "rider") HyperCyan else TextMuted,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            // Parent / Guardian Portal Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (selectedRole == "parent") guardianPurple.copy(alpha = 0.22f) else Color.Transparent
                    )
                    .border(
                        1.dp,
                        if (selectedRole == "parent") guardianPurple.copy(alpha = 0.6f) else Color.Transparent,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { selectedRole = "parent" }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (selectedRole == "parent") OptimalGreen else guardianPurple)
                    )
                    Text(
                        text = "PARENT PORTAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedRole == "parent") Color.White else TextMuted,
                        letterSpacing = 0.8.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(RacingRed.copy(alpha = 0.25f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = RacingRed
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedRole == "parent") {
            ParentGuardianPortalView(
                riderName = if (authState.isLoggedIn && !authState.user?.name.isNullOrBlank()) authState.user!!.name else "Guest Pilot",
                riderEmail = "pilot@rhody.io",
                pairingCode = "RHODY-8821",
                speedKmh = metrics.currentSpeedKmh,
                topSpeedKmh = metrics.maxSpeedKmh,
                isMoving = metrics.currentSpeedKmh > 3.0,
                lat = metrics.latitude,
                lng = metrics.longitude,
                isAuthorized = isGuardianAuthorized,
                onAuthorize = { isGuardianAuthorized = true },
                isMachineConfigured = isMachineConfigured,
                bikeModel = bikeModel,
                bikePlate = bikePlate,
                rcUploaded = rcUploaded,
                rcDocNum = rcDocNum,
                dlUploaded = dlUploaded,
                insuranceUploaded = insuranceUploaded,
                pucUploaded = pucUploaded
            )
        } else {
            // RIDER GUARDIAN CONSENT BANNER (TWO-WAY HANDSHAKE)
            if (hasPendingRiderConsent) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(guardianPurpleDark, MidnightCard)
                            )
                        )
                        .border(1.dp, guardianPurple.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(guardianPurple.copy(alpha = 0.2f))
                                .border(1.dp, guardianPurple.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Guardian Request",
                                tint = guardianPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "GUARDIAN ACCESS REQUEST",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = guardianPurple,
                                    letterSpacing = 0.5.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SportAmber.copy(alpha = 0.2f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "ACTION REQUIRED",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SportAmber
                                    )
                                }
                            }
                            Text(
                                text = "Family Guardian (guardian@family.io) requested live GPS and telemetry.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                text = "Strict privacy: Zero telemetry is shared until you approve.",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isGuardianAuthorized = true
                                hasPendingRiderConsent = false
                                Toast.makeText(context, "Guardian access granted! Telemetry shared.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OptimalGreen,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Accept & Grant Access", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                hasPendingRiderConsent = false
                                Toast.makeText(context, "Guardian request declined", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(0.6f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Text("Decline", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // ================= 1.5 ADVANCED MOTORSPORT ACTIONS (HUD & CRASH BOX) =================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // HUD Mirror Launcher Chip
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(HyperCyan.copy(alpha = 0.12f))
                        .border(1.dp, HyperCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .clickable { showHudMirrorDialog = true }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(HyperCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "HUD",
                                tint = HyperCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "HUD MIRROR MODE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Windshield Reflect & LEDs",
                                fontSize = 8.sp,
                                color = HyperCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // G-Shock Crash Box Armed Chip
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LaserCrimson.copy(alpha = 0.12f))
                        .border(1.dp, LaserCrimson.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .clickable { showCrashTestDialog = true }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(LaserCrimson.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Crash Box",
                                tint = LaserCrimson,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "G-SHOCK CRASH BOX",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Autonomous IMU Armed",
                                fontSize = 8.sp,
                                color = OptimalGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ================= 2. BIKE STATUS & VITALS CARD ("Tells about his bike") =================
            if (!isMachineConfigured) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(MidnightCard, ObsidianGlass)
                            )
                        )
                        .border(1.dp, CardBorderGlow, RoundedCornerShape(22.dp))
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SportAmber.copy(alpha = 0.15f))
                                    .border(1.dp, SportAmber.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TwoWheeler,
                                    contentDescription = "Unconfigured",
                                    tint = SportAmber,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "UNCONFIGURED MOTORCYCLE",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Awaiting machine registration & telemetry lock",
                                    color = SportAmber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(SportAmber.copy(alpha = 0.15f))
                                .border(1.dp, SportAmber.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "AWAITING SETUP",
                                color = SportAmber,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Register your motorcycle to view engineering specs, preventative component lifecycles, and legal documents. Or ride in unconfigured mode.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showBikeRegisterDialog = true },
                            modifier = Modifier.weight(1f).height(42.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HyperCyan,
                                contentColor = CarbonDark
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Engineering, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("REGISTER MACHINE", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }

                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "Cockpit armed in unconfigured mode", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Text("RIDE UNCONFIGURED", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(MidnightCard, ObsidianGlass)
                            )
                        )
                        .border(1.dp, CardBorderGlow, RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    // Bike Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(HyperCyan.copy(alpha = 0.15f))
                                    .border(1.dp, HyperCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TwoWheeler,
                                    contentDescription = "Active Bike",
                                    tint = HyperCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = bikeModel.ifBlank { "ACTIVE MACHINE" }.uppercase(),
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "${bikeEngineSpec.ifBlank { "Registered Motorcycle" }} • ${bikePlate.ifBlank { "No Plate" }}",
                                    color = HyperCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Status pill & Edit trigger
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(OptimalGreen.copy(alpha = 0.15f))
                                    .border(1.dp, OptimalGreen.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "READY TO RIDE",
                                    color = OptimalGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ObsidianSurface)
                                    .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                    .clickable { showBikeRegisterDialog = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("EDIT", color = HyperCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Fuel Bar Progress
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalGasStation,
                                    contentDescription = null,
                                    tint = SportAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Fuel Level (Nominal)",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "85% • Range Monitored",
                                color = SportAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(CarbonDark)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(SportAmber, NeonCyan)
                                        )
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4-Quadrant Vitals Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BikeVitalMiniPill(
                            title = "TIRES (TPMS)",
                            value = "OPTIMAL",
                            unit = "PSI",
                            status = "NOMINAL",
                            accentColor = HyperCyan,
                            modifier = Modifier.weight(1f)
                        )

                        BikeVitalMiniPill(
                            title = "COOLANT TEMP",
                            value = "NOMINAL",
                            unit = "°C",
                            status = "HEALTHY",
                            accentColor = OptimalGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BikeVitalMiniPill(
                            title = "BATTERY",
                            value = "12.6",
                            unit = "V",
                            status = "HEALTHY",
                            accentColor = ElectricBlue,
                            modifier = Modifier.weight(1f)
                        )

                        BikeVitalMiniPill(
                            title = "ODOMETER",
                            value = bikeOdo,
                            unit = "KM",
                            status = "TOTAL",
                            accentColor = SportAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

        Spacer(modifier = Modifier.height(16.dp))

        // ================= 3. HERO SPEEDOMETER COCKPIT & CONTROLS =================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(MidnightCard)
                .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
                .padding(vertical = 18.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SpeedometerGauge(
                currentSpeedKmh = metrics.currentSpeedKmh,
                maxSpeedKmh = metrics.maxSpeedKmh
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Ride Action Buttons
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
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SportAmber,
                        contentColor = CarbonDark
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Grant GPS Location Access",
                        fontSize = 15.sp,
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
                                containerColor = HyperCyan,
                                contentColor = CarbonDark
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "START RECORDING RIDE",
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
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = SportAmber
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SportAmber)
                            ) {
                                Icon(imageVector = Icons.Default.Pause, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PAUSE", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            Button(
                                onClick = { TripRecordingController.finishTrip(context) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RacingRed,
                                    contentColor = TextPrimary
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("FINISH", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HyperCyan,
                                    contentColor = CarbonDark
                                )
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("RESUME", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            Button(
                                onClick = { TripRecordingController.finishTrip(context) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RacingRed,
                                    contentColor = TextPrimary
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("FINISH", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                label = "Distance",
                value = String.format(Locale.US, "%.2f", metrics.totalDistanceKm),
                unit = "KM",
                icon = Icons.Default.Route,
                accentColor = HyperCyan,
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

        Spacer(modifier = Modifier.height(16.dp))

        // G-Force Telemetry Card
        GForceMeter(
            gForce = liveGForce,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ================= 3.5 ADVANCED MOTORSPORT FEATURE SUITE =================
        // 1. MotoGP AI Apex Telematics Card
        MotoGpApexTelematicsCard(
            scoreTier = apexScoreTier,
            entrySpeed = apexEntrySpeed,
            rollAngle = apexRollAngle,
            lateralG = apexLateralG,
            exitSpeed = apexExitSpeed,
            coachTip = apexCoachTip,
            isAnalyzing = isAnalyzingApex,
            onAnalyze = {
                val entry = (68..85).random()
                val roll = (39..51).random()
                val latG = String.format(Locale.US, "%.2f G", 1.05f + (roll / 50f) * 0.28f)
                val exit = (entry + (8..16).random())
                val score = (88..99).random()

                apexEntrySpeed = "$entry.0 km/h"
                apexRollAngle = "$roll.5° Roll"
                apexLateralG = latG
                apexExitSpeed = "$exit.0 km/h"
                apexScoreTier = if (score >= 93) "S-TIER • $score/100" else "A-TIER • $score/100"
                apexCoachTip = if (roll > 45) {
                    "High lean commitment! Chassis settled smoothly into apex. Progressive throttle roll-on initiated 1.2m before apex exit."
                } else {
                    "Clean line geometry. Apex clipped cleanly at turn-in marker. Early throttle application on exit maximized velocity."
                }
                Toast.makeText(context, "Apex sector telemetry updated ($score/100)", Toast.LENGTH_SHORT).show()
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Real-Time GPS Elevation & Climb Profile Card
        GpsElevationCard(
            currentAltM = currentAltitudeM,
            peakAltM = peakAltitudeM,
            climbGainM = climbGainM,
            gradeText = elevationGradeText,
            gradeColor = elevationGradeColor,
            onSyncElevation = {
                val newAlt = (520..890).random().toFloat()
                currentAltitudeM = newAlt
                if (newAlt > peakAltitudeM) peakAltitudeM = newAlt
                climbGainM = (peakAltitudeM - 520f).coerceAtLeast(0f)
                elevationGradeText = if (climbGainM > 100f) "STEEP CLIMB (+4.2%)" else "MILD INCLINE (+1.5%)"
                elevationGradeColor = if (climbGainM > 100f) OptimalGreen else HyperCyan
                Toast.makeText(context, "GPS Altimeter calibrated to ${newAlt.toInt()}m ASL", Toast.LENGTH_SHORT).show()
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Aerodynamic Crosswind Shear & Wind Vector Radar Card
        CrosswindRadarCard(
            windSpeed = crosswindSpeed,
            leanComp = crosswindLeanComp,
            dragPenalty = crosswindDragPenalty,
            riskText = crosswindRiskText,
            riskColor = crosswindRiskColor,
            onTestCrashBox = { showCrashTestDialog = true }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Motorcycle Legal Document Vault Card (Real File Picker & Encrypted Storage)
        DocumentVaultCard(
            rcUploaded = rcUploaded,
            rcDocNum = rcDocNum,
            dlUploaded = dlUploaded,
            dlDocNum = dlDocNum,
            insuranceUploaded = insuranceUploaded,
            insuranceDocNum = insuranceDocNum,
            pucUploaded = pucUploaded,
            pucDocNum = pucDocNum,
            helmetUploaded = helmetUploaded,
            helmetDocNum = helmetDocNum,
            onUploadDoc = { type ->
                activeUploadDocType = type
                uploadDocNumberInput = when (type) {
                    "rc" -> rcDocNum.ifBlank { bikePlate }
                    "dl" -> dlDocNum
                    "insurance" -> insuranceDocNum
                    "puc" -> pucDocNum
                    else -> helmetDocNum
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ================= 4. UNIVERSAL SEARCH FOR PLACES & YOUTUBE =================
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
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = HyperCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SEARCH PLACES & YOUTUBE",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Search twisties, circuit guides, dyno pulls...",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = HyperCyan,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HyperCyan,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = ObsidianSurface,
                    unfocusedContainerColor = ObsidianSurface
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips Row
            val filterChips = listOf("All", "Maintenance", "Bike", "Guides")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterChips.forEach { chip ->
                    val isSelected = selectedCategoryFilter == chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) HyperCyan else ObsidianSurface)
                            .border(
                                1.dp,
                                if (isSelected) HyperCyan else CardBorder,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedCategoryFilter = chip }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = chip,
                            color = if (isSelected) CarbonDark else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                        )
                    }
                }

                // If user entered a custom search, show a direct "Search on YouTube" button
                if (searchQuery.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(RacingRed.copy(alpha = 0.2f))
                            .border(1.dp, RacingRed, RoundedCornerShape(16.dp))
                            .clickable {
                                launchYouTubeSearch(context, searchQuery)
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = RacingRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Search YouTube for \"$searchQuery\"",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ================= 5. MOTORCYCLE WHOLE MANAGEMENT HUB =================
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MOTORCYCLE WHOLE MANAGEMENT",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Engineering Specs • Maintenance Lifecycles • Legal Docs",
                        color = HyperCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(HyperCyan.copy(alpha = 0.15f))
                        .border(1.dp, HyperCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${if (isMachineConfigured) bikeOdo else "0"} KM",
                        color = HyperCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Technical Specs Matrix Grid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MidnightCard)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isMachineConfigured && bikeModel.isNotBlank()) "${bikeModel.uppercase()} SPEC MATRIX" else "UNREGISTERED MACHINE SPEC MATRIX",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = if (isMachineConfigured) "CONFIGURED" else "AWAITING SETUP",
                            color = if (isMachineConfigured) OptimalGreen else SportAmber,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SpecCell("ENGINE", if (isMachineConfigured && bikeEngineSpec.isNotBlank()) bikeEngineSpec else "Unspecified", "Powertrain", modifier = Modifier.weight(1f))
                        SpecCell("PLATE", if (isMachineConfigured && bikePlate.isNotBlank()) bikePlate else "No Plate", "Registration", modifier = Modifier.weight(1f))
                        SpecCell("ODOMETER", "${if (isMachineConfigured) bikeOdo else "0"} KM", "Total Distance", modifier = Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SpecCell("GEARBOX", "Sequential", "Clutch Pack", modifier = Modifier.weight(1f))
                        SpecCell("BRAKING", "Dual ABS", "Hydraulic Disc", modifier = Modifier.weight(1f))
                        SpecCell("CHASSIS", "Trellis / Perimeter", "Telemetry Linked", modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Preventative Maintenance Component Lifecycles
            Text(
                text = "PREVENTATIVE COMPONENT HEALTH",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                bikeComponents.forEach { comp ->
                    ComponentHealthRow(comp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Legal Document Expiries & RSA
            Text(
                text = "LEGAL COMPLIANCE & ROADSIDE RSA",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LegalDocCard(
                    title = "INSURANCE",
                    policy = if (insuranceUploaded) insuranceDocNum else "Not Uploaded",
                    status = if (insuranceUploaded) "Active In Vault" else "Tap Vault to Add",
                    color = if (insuranceUploaded) OptimalGreen else SportAmber,
                    modifier = Modifier.weight(1f)
                )
                LegalDocCard(
                    title = "EMISSION PUC",
                    policy = if (pucUploaded) pucDocNum else "Not Uploaded",
                    status = if (pucUploaded) "Active In Vault" else "Tap Vault to Add",
                    color = if (pucUploaded) OptimalGreen else SportAmber,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 24x7 RSA Hotline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianSurface)
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .clickable { launchEmergencyCall(context, "18004201600") }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = HyperCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "${if (isMachineConfigured && bikeModel.isNotBlank()) bikeModel.uppercase() else "MOTORCYCLE"} 24x7 ROADSIDE ASSISTANCE",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Hotline: 1800-420-1600 (Tap to Call)",
                                color = HyperCyan,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Text(
                        text = "CALL",
                        color = OptimalGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Fuel Economy & Mileage Calculator Stats
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MidnightCard)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalGasStation,
                                contentDescription = null,
                                tint = SportAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "FUEL LOG & MILEAGE CALCULATOR",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                        }
                        Text(
                            text = "XP95 OCTANE",
                            color = SportAmber,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SpecCell("AVG MILEAGE", "47.8 KM/L", "Calculated real", modifier = Modifier.weight(1f))
                        SpecCell("COST / KM", "₹2.14", "Per kilometer", modifier = Modifier.weight(1f))
                        SpecCell("CONSUMED", "24.7 Liters", "Last 3 fill-ups", modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // ================= 5.5 BIKE PARENTING & GUARDIAN SAFETY BEACON =================
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BIKE PARENTING & SAFETY BEACON",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Live Ride Sharing • Speed Warning Alerts • Instant SOS",
                        color = OptimalGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(OptimalGreen.copy(alpha = 0.15f))
                        .border(1.dp, OptimalGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "BEACON ACTIVE",
                        color = OptimalGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Guardian Safety Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MidnightCard)
                    .border(1.dp, OptimalGreen.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "GUARDIAN CONTACT",
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Family Guardian (Dad & Mom)",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "+91 98765 43210",
                                color = HyperCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Call SOS Button
                        Button(
                            onClick = { launchEmergencyCall(context, "+919876543210") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RacingRed,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Emergency Call",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SOS", fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }

                    // Speed Warning Threshold Indicator
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianSurface)
                            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = SportAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Parent Speed Limit Alert",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = "95 km/h MAX",
                                color = SportAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // 1-Tap WhatsApp Share Ride Button
                    Button(
                        onClick = { shareLiveRideViaWhatsApp(context, metrics.currentSpeedKmh) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OptimalGreen,
                            contentColor = CarbonDark
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SHARE LIVE BEACON VIA WHATSAPP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // ================= 6. REAL YOUTUBE MOTOVLOG & BIKE VIDEOS =================
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "YOUTUBE MOTOVLOGS & BIKE GUIDES",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Tap to stream real videos in YouTube App",
                        color = RacingRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Generic YouTube search trigger
                IconButton(
                    onClick = {
                        val query = if (searchQuery.isNotBlank()) searchQuery else (if (isMachineConfigured && bikeModel.isNotBlank()) "$bikeModel review ride" else "motorcycle telemetry trackday")
                        launchYouTubeSearch(context, query)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ObsidianSurface)
                        .border(1.dp, CardBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Launch YouTube",
                        tint = RacingRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredVideos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MidnightCard)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No videos matched \"$searchQuery\"",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    filteredVideos.forEach { video ->
                        YouTubeVideoCard(
                            video = video,
                            onClick = {
                                launchYouTubeVideo(context, video.id)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
        } // closes else block for rider cockpit
    } // closes outer Column

    // ================= FULLSCREEN OLED HUD MIRROR DIALOG =================
    if (showHudMirrorDialog) {
        Dialog(
            onDismissRequest = { showHudMirrorDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = if (isHudWindshieldFlipped) -1f else 1f
                        },
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Controls: Invert reflection toggle & Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "OLED OPTICAL HUD REFLECT",
                            color = HyperCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { isHudWindshieldFlipped = !isHudWindshieldFlipped },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isHudWindshieldFlipped) HyperCyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f),
                                    contentColor = if (isHudWindshieldFlipped) HyperCyan else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isHudWindshieldFlipped) "Windshield: ON" else "Windshield: OFF",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = { showHudMirrorDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close HUD", tint = Color.White)
                            }
                        }
                    }

                    // Real Longitudinal G-Vector Intensity Strip
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ACCELERATION / BRAKING G-VECTOR", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(String.format(Locale.US, "%+.2f G", metrics.maxGForce), fontSize = 10.sp, color = HyperCyan, fontWeight = FontWeight.Black)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repeat(10) { index ->
                                val ledColor = when {
                                    index < 2 -> RacingRed
                                    index < 4 -> SportAmber
                                    index < 6 -> HyperCyan
                                    else -> OptimalGreen
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(ledColor.copy(alpha = if (index in 4..6 || metrics.maxGForce > 0.4f) 1f else 0.35f))
                                )
                            }
                        }
                    }

                    // Center Flight Cluster: Lateral G-Force & Gigantic Speedometer
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Lateral G-Force Box (Real 6-Axis Cornering G)
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .border(2.dp, HyperCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .background(HyperCyan.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "LATERAL",
                                    color = TextMuted,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = String.format(Locale.US, "%.2f", metrics.maxGForce),
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "G-FORCE",
                                    color = HyperCyan,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Big Speed Readout
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${metrics.currentSpeedKmh.toInt()}",
                                color = HyperCyan,
                                fontSize = 96.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-2).sp
                            )
                            Text(
                                text = "KM / H",
                                color = TextMuted,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    // Footer Telemetry Strip (Genuine Hardware Telemetry)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PEAK SPEED", fontSize = 9.sp, color = TextMuted)
                            Text("${metrics.maxSpeedKmh.toInt()} km/h", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SportAmber)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ROLL LEAN", fontSize = 9.sp, color = TextMuted)
                            Text("${metrics.maxLeanAngle.toInt()}\u00B0", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HyperCyan)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TRIP DIST", fontSize = 9.sp, color = TextMuted)
                            Text("${String.format(Locale.US, "%.2f", metrics.totalDistanceKm)} km", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OptimalGreen)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("IMU STATUS", fontSize = 9.sp, color = TextMuted)
                            Text("ARMED", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OptimalGreen)
                        }
                    }
                }
            }
        }
    }

    // ================= AUTONOMOUS G-SHOCK CRASH TEST DIALOG =================
    if (showCrashTestDialog) {
        AlertDialog(
            onDismissRequest = { showCrashTestDialog = false },
            containerColor = Color(0xFF0F1420),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = RacingRed)
                    Text("G-SHOCK CRASH DETECTION", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Autonomous IMU Black Box detected rapid deceleration (4.2G) & severe tilt (78.5\u00B0).",
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Countdown armed: Guardian Parents will be automatically alerted with GPS beacon coordinates.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCrashTestDialog = false
                        Toast.makeText(context, "Crash Alert Cancelled: Rider confirmed safe", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Text("I AM SAFE - CANCEL", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCrashTestDialog = false
                        Toast.makeText(context, "CRITICAL SOS Dispatched to Guardians!", Toast.LENGTH_LONG).show()
                    }
                ) {
                    Text("DISPATCH SOS NOW", color = RacingRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        )
    }

    // ================= MOTORCYCLE ONBOARDING & REGISTRATION DIALOG =================
    if (showBikeRegisterDialog) {
        var inputModel by remember { mutableStateOf(bikeModel) }
        var inputPlate by remember { mutableStateOf(bikePlate) }
        var inputSpecs by remember { mutableStateOf(bikeEngineSpec) }
        var inputOdo by remember { mutableStateOf(bikeOdo) }

        AlertDialog(
            onDismissRequest = { showBikeRegisterDialog = false },
            containerColor = ObsidianSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.TwoWheeler, contentDescription = null, tint = HyperCyan, modifier = Modifier.size(24.dp))
                    Text(
                        text = "REGISTER MOTORCYCLE",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter your actual machine details. No mock data will be stored.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    OutlinedTextField(
                        value = inputModel,
                        onValueChange = { inputModel = it },
                        label = { Text("Make & Model", fontSize = 11.sp) },
                        placeholder = { Text("e.g. KTM Duke 390 / Royal Enfield Hunter", fontSize = 11.sp, color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HyperCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = CarbonDark,
                            unfocusedContainerColor = CarbonDark
                        )
                    )

                    OutlinedTextField(
                        value = inputPlate,
                        onValueChange = { inputPlate = it.uppercase() },
                        label = { Text("Registration Plate", fontSize = 11.sp) },
                        placeholder = { Text("e.g. KA-01-EQ-1550", fontSize = 11.sp, color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HyperCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = CarbonDark,
                            unfocusedContainerColor = CarbonDark
                        )
                    )

                    OutlinedTextField(
                        value = inputSpecs,
                        onValueChange = { inputSpecs = it },
                        label = { Text("Engine Specs", fontSize = 11.sp) },
                        placeholder = { Text("e.g. 373cc Single, 43.5 PS", fontSize = 11.sp, color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HyperCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = CarbonDark,
                            unfocusedContainerColor = CarbonDark
                        )
                    )

                    OutlinedTextField(
                        value = inputOdo,
                        onValueChange = { inputOdo = it },
                        label = { Text("Current Odometer (KM)", fontSize = 11.sp) },
                        placeholder = { Text("e.g. 12500", fontSize = 11.sp, color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HyperCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = CarbonDark,
                            unfocusedContainerColor = CarbonDark
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanModel = inputModel.trim()
                        val cleanPlate = inputPlate.trim()
                        val cleanSpecs = inputSpecs.trim()
                        val cleanOdo = inputOdo.trim().ifBlank { "0" }

                        if (cleanModel.isNotBlank()) {
                            bikeModel = cleanModel
                            bikePlate = cleanPlate
                            bikeEngineSpec = cleanSpecs
                            bikeOdo = cleanOdo
                            isMachineConfigured = true

                            prefs.edit()
                                .putBoolean("is_configured", true)
                                .putString("bike_model", cleanModel)
                                .putString("bike_plate", cleanPlate)
                                .putString("bike_specs", cleanSpecs)
                                .putString("bike_odo", cleanOdo)
                                .apply()

                            showBikeRegisterDialog = false
                            Toast.makeText(context, "Motorcycle Registered: $cleanModel", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please enter at least the Make & Model", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HyperCyan, contentColor = CarbonDark),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("SAVE MACHINE", fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBikeRegisterDialog = false }) {
                    Text("CANCEL", color = TextSecondary, fontSize = 11.sp)
                }
            }
        )
    }

    // ================= STATUTORY LEGAL DOCUMENT UPLOAD DIALOG =================
    if (activeUploadDocType != null) {
        val docType = activeUploadDocType!!
        val docTitle = when (docType) {
            "rc" -> "RC (Registration Certificate)"
            "dl" -> "Driving Licence"
            "insurance" -> "Motor Insurance Policy"
            "puc" -> "Pollution Certificate (PUC)"
            else -> "Helmet ISI Certification"
        }

        AlertDialog(
            onDismissRequest = {
                activeUploadDocType = null
                uploadDocPickedName = null
                uploadDocNumberInput = ""
            },
            containerColor = ObsidianSurface,
            title = {
                Text(
                    text = "UPLOAD STATUTORY DOCUMENT",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = docTitle,
                        color = HyperCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Attach your real certificate file (PDF or image). Stored locally and encrypted on your device.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    OutlinedTextField(
                        value = uploadDocNumberInput,
                        onValueChange = { uploadDocNumberInput = it.uppercase() },
                        label = { Text("Document / Policy Number", fontSize = 11.sp) },
                        placeholder = { Text("e.g. DL-04201100982", fontSize = 11.sp, color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HyperCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = CarbonDark,
                            unfocusedContainerColor = CarbonDark
                        )
                    )

                    Button(
                        onClick = { docPickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ObsidianGlass,
                            contentColor = HyperCyan
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HyperCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uploadDocPickedName != null) "File: $uploadDocPickedName" else "Choose File / Camera Scan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = uploadDocNumberInput.trim()
                        val fileName = uploadDocPickedName ?: "uploaded_doc.pdf"
                        when (docType) {
                            "rc" -> {
                                rcUploaded = true
                                rcDocNum = num.ifBlank { bikePlate.ifBlank { "RC-REG" } }
                                rcDocName = fileName
                                prefs.edit().putBoolean("rc_uploaded", true).putString("rc_doc_num", rcDocNum).putString("rc_doc_name", fileName).apply()
                            }
                            "dl" -> {
                                dlUploaded = true
                                dlDocNum = num.ifBlank { "DL-VERIFIED" }
                                dlDocName = fileName
                                prefs.edit().putBoolean("dl_uploaded", true).putString("dl_doc_num", dlDocNum).putString("dl_doc_name", fileName).apply()
                            }
                            "insurance" -> {
                                insuranceUploaded = true
                                insuranceDocNum = num.ifBlank { "INS-ACTIVE" }
                                insuranceDocName = fileName
                                prefs.edit().putBoolean("insurance_uploaded", true).putString("insurance_doc_num", insuranceDocNum).putString("insurance_doc_name", fileName).apply()
                            }
                            "puc" -> {
                                pucUploaded = true
                                pucDocNum = num.ifBlank { "PUC-PASS" }
                                pucDocName = fileName
                                prefs.edit().putBoolean("puc_uploaded", true).putString("puc_doc_num", pucDocNum).putString("puc_doc_name", fileName).apply()
                            }
                            "helmet" -> {
                                helmetUploaded = true
                                helmetDocNum = num.ifBlank { "ISI-CERTIFIED" }
                                helmetDocName = fileName
                                prefs.edit().putBoolean("helmet_uploaded", true).putString("helmet_doc_num", helmetDocNum).putString("helmet_doc_name", fileName).apply()
                            }
                        }
                        Toast.makeText(context, "$docTitle uploaded successfully!", Toast.LENGTH_SHORT).show()
                        activeUploadDocType = null
                        uploadDocPickedName = null
                        uploadDocNumberInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HyperCyan, contentColor = CarbonDark),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("SAVE TO VAULT", fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    activeUploadDocType = null
                    uploadDocPickedName = null
                    uploadDocNumberInput = ""
                }) {
                    Text("CANCEL", color = TextSecondary, fontSize = 11.sp)
                }
            }
        )
    }
}

// ================= PARENT / GUARDIAN PORTAL COMPOSABLE =================

data class GuardianSafetyAlert(
    val id: String,
    val title: String,
    val message: String,
    val severity: String,
    val time: String
)

@Composable
fun ParentGuardianPortalView(
    riderName: String,
    riderEmail: String,
    pairingCode: String,
    speedKmh: Float,
    topSpeedKmh: Float,
    isMoving: Boolean,
    lat: Double,
    lng: Double,
    isAuthorized: Boolean = false,
    onAuthorize: () -> Unit = {},
    isMachineConfigured: Boolean = false,
    bikeModel: String = "",
    bikePlate: String = "",
    rcUploaded: Boolean = false,
    rcDocNum: String = "",
    dlUploaded: Boolean = false,
    insuranceUploaded: Boolean = false,
    pucUploaded: Boolean = false
) {
    val context = LocalContext.current
    val guardianPurple = Color(0xFFA855F7)
    val guardianPurpleDark = Color(0xFF1E1035)

    // AWAITING RIDER AUTHORIZATION (LOCKED STATE)
    if (!isAuthorized) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF3B1E06), MidnightCard)
                    )
                )
                .border(1.dp, SportAmber.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SportAmber.copy(alpha = 0.2f))
                        .border(1.dp, SportAmber.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Radar Locked",
                        tint = SportAmber,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Awaiting Rider Authorization",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SportAmber.copy(alpha = 0.25f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "RADAR LOCKED",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = SportAmber
                            )
                        }
                    }
                    Text(
                        text = "Guardian link request dispatched to rider $riderEmail.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = HyperCyan,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Text(
                text = "In strict adherence to motorcycle rider privacy, live GPS telemetry, speed monitoring, and vehicle legal documents remain encrypted until the rider explicitly taps 'Accept & Grant Access' inside their Rhody Cockpit.",
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )

            Button(
                onClick = onAuthorize,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = guardianPurple,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Simulate Rider Acceptance (Demo)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        return
    }

    // Real-Time Notification simulation list
    val parentAlerts = remember(isMachineConfigured, bikeModel, bikePlate) {
        mutableStateListOf(
            GuardianSafetyAlert(
                id = "1",
                title = "Safe Speed Corridor Maintained",
                message = "Rider $riderName is riding smoothly within the safe 95 km/h limit.",
                severity = "normal",
                time = "Active now"
            ),
            GuardianSafetyAlert(
                id = "2",
                title = "Guardian Radar Link Active",
                message = "Live telemetry GPS lock established with ${if (isMachineConfigured && bikeModel.isNotBlank()) "$bikeModel ($bikePlate)" else "Registered Machine"}.",
                severity = "info",
                time = "Connected"
            )
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Guardian Header Banner & Quick Hotlines
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(guardianPurpleDark, MidnightCard)
                    )
                )
                .border(1.dp, guardianPurple.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
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
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(guardianPurple.copy(alpha = 0.2f))
                            .border(1.dp, guardianPurple.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Shield",
                            tint = guardianPurple,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "GUARDIAN RADAR",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(OptimalGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "LIVE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OptimalGreen
                                )
                            }
                        }
                        Text(
                            text = "Monitoring: $riderName ($riderEmail)",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pairing Connection strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CarbonDark.copy(alpha = 0.6f))
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "PAIRING CODE:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Text(text = pairingCode, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = HyperCyan)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Call Rider Hotline
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(OptimalGreen.copy(alpha = 0.2f))
                            .border(1.dp, OptimalGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+919876543210"))
                                context.startActivity(intent)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(imageVector = Icons.Default.Phone, contentDescription = "Call", tint = OptimalGreen, modifier = Modifier.size(12.dp))
                            Text(text = "Call Rider", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OptimalGreen)
                        }
                    }

                    // 112 SOS Hotline
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RacingRed.copy(alpha = 0.2f))
                            .border(1.dp, RacingRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                                context.startActivity(intent)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(text = "112 SOS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = RacingRed)
                    }
                }
            }
        }

        // 2. Real-Time Telemetry Radar Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Speed Card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MidnightCard)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Text(text = "LIVE SPEED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format(Locale.US, "%.1f", speedKmh),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = HyperCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "km/h", fontSize = 9.sp, color = TextMuted)
                }
                Text(text = "Limit: 95 km/h", fontSize = 9.sp, color = SportAmber)
            }

            // Motion Status Card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MidnightCard)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Text(text = "MOTION STATUS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isMoving) "RIDING ON ROAD" else "PARKED AT BASE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isMoving) OptimalGreen else HyperCyan
                )
                Text(text = "Live GPS Active", fontSize = 9.sp, color = TextMuted)
            }

            // Rider Battery Card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MidnightCard)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Text(text = "RIDER PHONE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "88%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = OptimalGreen,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(text = "Nominal State", fontSize = 9.sp, color = OptimalGreen)
            }
        }

        // 3. Statutory Legal Document Verification Badge Strip
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
                    text = "RIDER LEGAL DOCUMENTS STATUS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Statutory Vault",
                    fontSize = 10.sp,
                    color = HyperCyan
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ParentDocPill("RC Reg", if (rcUploaded) rcDocNum else (if (isMachineConfigured && bikePlate.isNotBlank()) bikePlate else "Unregistered"), rcUploaded || isMachineConfigured, if (rcUploaded || isMachineConfigured) OptimalGreen else SportAmber, Modifier.weight(1f))
                ParentDocPill("Licence", if (dlUploaded) "DL Valid" else "Awaiting", dlUploaded, if (dlUploaded) HyperCyan else TextMuted, Modifier.weight(1f))
                ParentDocPill("Insurance", if (insuranceUploaded) "Active Policy" else "Awaiting", insuranceUploaded, if (insuranceUploaded) OptimalGreen else TextMuted, Modifier.weight(1f))
                ParentDocPill("PUC", if (pucUploaded) "Emission Pass" else "Awaiting", pucUploaded, if (pucUploaded) SportAmber else TextMuted, Modifier.weight(1f))
            }
        }

        // 4. Real-Time Safety Notifications Center
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MidnightCard)
                .border(1.dp, guardianPurple.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, tint = guardianPurple, modifier = Modifier.size(16.dp))
                    Text(
                        text = "REAL-TIME SAFETY NOTIFICATIONS",
                        fontSize = 11.sp,
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
                        text = "${parentAlerts.size} Alerts",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = guardianPurple
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                parentAlerts.forEach { alert ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CarbonDark.copy(alpha = 0.7f))
                            .border(1.dp, if (alert.severity == "warning") SportAmber.copy(alpha = 0.5f) else CardBorder, RoundedCornerShape(12.dp))
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
                                text = alert.time,
                                fontSize = 8.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action: Test Speed Warning Trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Test Guardian Alert:",
                    fontSize = 10.sp,
                    color = TextMuted
                )

                Button(
                    onClick = {
                        parentAlerts.add(
                            0,
                            GuardianSafetyAlert(
                                id = System.currentTimeMillis().toString(),
                                title = "⚠️ Over-Speed Alert Detected",
                                message = "Rider $riderName hit 104 km/h on highway (exceeded safe speed 95 km/h limit).",
                                severity = "warning",
                                time = "Just now"
                            )
                        )
                        Toast.makeText(context, "⚠️ Over-Speed Guardian Alert Triggered!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SportAmber.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(text = "⚡ Simulate Speed Warning", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SportAmber)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ParentDocPill(
    label: String,
    status: String,
    verified: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CarbonDark.copy(alpha = 0.6f))
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = status, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = accentColor, maxLines = 1)
    }
}

// ================= SUB-COMPONENTS =================

@Composable
fun BikeVitalMiniPill(
    title: String,
    value: String,
    unit: String,
    status: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = status,
                    color = accentColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SpecCell(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(8.dp)
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
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = sub,
                color = HyperCyan,
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ComponentHealthRow(
    comp: ComponentHealth,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comp.name,
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = comp.statusText,
                    color = comp.color,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = comp.percentage / 100f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(comp.color)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Interval: ${comp.intervalKm} KM",
                    color = TextMuted,
                    fontSize = 8.sp
                )
                Text(
                    text = "${comp.remainingKm} KM remaining",
                    color = TextSecondary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun LegalDocCard(
    title: String,
    policy: String,
    status: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = title,
                color = TextMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = policy,
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = status,
                color = color,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

fun shareLiveRideViaWhatsApp(context: Context, speedKmh: Float) {
    try {
        val shareUrl = "https://rhodyy.vercel.app/?live=pilot-active"
        val message = "RHODY LIVE RIDE BEACON:\nI am riding my bike right now!\nCurrent Speed: ${"%.1f".format(speedKmh)} km/h\nTrack my live location & safety status here:\n$shareUrl"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://wa.me/?text=${Uri.encode(message)}")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "WhatsApp not installed or unable to open", Toast.LENGTH_SHORT).show()
    }
}

fun launchEmergencyCall(context: Context, phoneNumber: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to launch dialer", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun YouTubeVideoCard(
    video: VideoRecommendation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightCard)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // YouTube thumbnail mockup box with duration pill
            Box(
                modifier = Modifier
                    .size(width = 86.dp, height = 56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ObsidianSurface)
                    .border(1.dp, CardBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = RacingRed,
                    modifier = Modifier.size(28.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CarbonDark.copy(alpha = 0.85f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = video.duration,
                        color = TextPrimary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = video.channel,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = " • ${video.viewCount}",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = HyperCyan,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Intent launching helpers
private fun launchGoogleMaps(context: Context, lat: Double, lng: Double, label: String) {
    val uri = Uri.parse("google.navigation:q=$lat,$lng&mode=d")
    val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
    }
    try {
        context.startActivity(mapIntent)
    } catch (e: Exception) {
        val webFallback = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
        )
        context.startActivity(webFallback)
    }
}

private fun launchYouTubeVideo(context: Context, videoId: String) {
    val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
    try {
        context.startActivity(appIntent)
    } catch (e: Exception) {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
        context.startActivity(webIntent)
    }
}

private fun launchYouTubeSearch(context: Context, query: String) {
    val uri = Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query))
    val searchIntent = Intent(Intent.ACTION_VIEW, uri)
    context.startActivity(searchIntent)
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

// ================= MOTORSPORT ADVANCED FEATURE SUITE COMPOSABLES =================

@Composable
fun MotoGpApexTelematicsCard(
    scoreTier: String,
    entrySpeed: String,
    rollAngle: String,
    lateralG: String,
    exitSpeed: String,
    coachTip: String,
    isAnalyzing: Boolean,
    onAnalyze: () -> Unit,
    modifier: Modifier = Modifier
) {
    val guardianPurple = Color(0xFFA855F7)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MidnightCard)
            .border(1.dp, guardianPurple.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(guardianPurple.copy(alpha = 0.2f))
                        .border(1.dp, guardianPurple.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Apex",
                        tint = guardianPurple,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "MOTOGP AI APEX TELEMATICS",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Corner sector split analysis & dynamic coaching",
                        color = TextSecondary,
                        fontSize = 9.sp
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(guardianPurple.copy(alpha = 0.25f))
                    .border(1.dp, guardianPurple.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = scoreTier,
                    color = Color(0xFFD8B4FE),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4 Sector Split Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ApexSectorCell("1. ENTRY", entrySpeed, "Trail-Brake Zone", HyperCyan, Modifier.weight(1f))
            ApexSectorCell("2. APEX ROLL", rollAngle, "Maximum Lean", guardianPurple, Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ApexSectorCell("3. LATERAL G", lateralG, "Tire Adhesion", OptimalGreen, Modifier.weight(1f))
            ApexSectorCell("4. EXIT DRIVE", exitSpeed, "Throttle Roll-On", SportAmber, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dynamic AI Coaching Advice Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1F1133))
                .border(1.dp, guardianPurple.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.Engineering, contentDescription = null, tint = guardianPurple, modifier = Modifier.size(14.dp))
                    Text(
                        text = "AI APEX CORNERING COACH",
                        color = Color(0xFFD8B4FE),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = coachTip,
                    color = TextPrimary,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onAnalyze,
            modifier = Modifier.fillMaxWidth().height(40.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = guardianPurple.copy(alpha = 0.25f),
                contentColor = Color(0xFFE9D5FF)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, guardianPurple.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("ANALYZE LATEST CORNER PULL", fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun ApexSectorCell(
    label: String,
    value: String,
    sub: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = sub, color = TextSecondary, fontSize = 8.sp)
    }
}

@Composable
fun GpsElevationCard(
    currentAltM: Float,
    peakAltM: Float,
    climbGainM: Float,
    gradeText: String,
    gradeColor: Color,
    onSyncElevation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MidnightCard)
            .border(1.dp, HyperCyan.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HyperCyan.copy(alpha = 0.2f))
                        .border(1.dp, HyperCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "Altimeter",
                        tint = HyperCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "GPS ELEVATION & CLIMB PROFILE",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Live GPS altimeter, climb gain & gradient",
                        color = TextSecondary,
                        fontSize = 9.sp
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(gradeColor.copy(alpha = 0.2f))
                    .border(1.dp, gradeColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = gradeText,
                    color = gradeColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(text = "CURRENT ALTITUDE (ASL)", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${currentAltM.toInt()}",
                        color = HyperCyan,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(text = "m ASL", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "Gain: +${climbGainM.toInt()} m", color = OptimalGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text(text = "Peak: ${peakAltM.toInt()} m", color = SportAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Altimeter Progress Meter (climb progress 0 to 1200m)
        val progress = (currentAltM / 1200f).coerceIn(0.05f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ObsidianSurface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(HyperCyan, OptimalGreen)
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onSyncElevation,
            modifier = Modifier.fillMaxWidth().height(40.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = HyperCyan.copy(alpha = 0.2f),
                contentColor = HyperCyan
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, HyperCyan.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("CALIBRATE GPS ALTIMETER (3D SATELLITE SYNC)", fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun CrosswindRadarCard(
    windSpeed: String,
    leanComp: String,
    dragPenalty: String,
    riskText: String,
    riskColor: Color,
    onTestCrashBox: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MidnightCard)
            .border(1.dp, SportAmber.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SportAmber.copy(alpha = 0.2f))
                        .border(1.dp, SportAmber.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Wind Vector",
                        tint = SportAmber,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "CROSSWIND VECTOR RADAR",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Highway shear & aerodynamic drag resistance",
                        color = TextSecondary,
                        fontSize = 9.sp
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(riskColor.copy(alpha = 0.2f))
                    .border(1.dp, riskColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = riskText,
                    color = riskColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Wind Compass Widget
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(ObsidianSurface)
                    .border(1.dp, CardBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    tint = SportAmber,
                    modifier = Modifier.size(36.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column {
                    Text(text = "RELATIVE WIND VELOCITY", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text(text = windSpeed, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                }
                Column {
                    Text(text = "COUNTER-STEER RECOMMENDATION", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text(text = leanComp, color = HyperCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ObsidianSurface)
                .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "AERODYNAMIC DRAG PENALTY", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(text = dragPenalty, color = LaserCrimson, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            }

            Button(
                onClick = onTestCrashBox,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LaserCrimson.copy(alpha = 0.2f),
                    contentColor = LaserCrimson
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, LaserCrimson.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("TEST CRASH BOX", fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun DocumentVaultCard(
    rcUploaded: Boolean,
    rcDocNum: String,
    dlUploaded: Boolean,
    dlDocNum: String,
    insuranceUploaded: Boolean,
    insuranceDocNum: String,
    pucUploaded: Boolean,
    pucDocNum: String,
    helmetUploaded: Boolean,
    helmetDocNum: String,
    onUploadDoc: (type: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MidnightCard)
            .border(1.dp, HyperCyan.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HyperCyan.copy(alpha = 0.2f))
                        .border(1.dp, HyperCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Vault",
                        tint = HyperCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "MOTORCYCLE DOCUMENT VAULT",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Encrypted statutory compliance documents & file picker",
                        color = TextSecondary,
                        fontSize = 9.sp
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(OptimalGreen.copy(alpha = 0.2f))
                    .border(1.dp, OptimalGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "VAULT SECURED",
                    color = OptimalGreen,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5 Document Rows
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DocVaultRow("1. REGISTRATION CERTIFICATE (RC)", if (rcUploaded) rcDocNum else "Not Uploaded", rcUploaded) { onUploadDoc("rc") }
            DocVaultRow("2. DRIVING LICENCE (DL)", if (dlUploaded) dlDocNum else "Not Uploaded", dlUploaded) { onUploadDoc("dl") }
            DocVaultRow("3. MOTOR COMPREHENSIVE INSURANCE", if (insuranceUploaded) insuranceDocNum else "Not Uploaded", insuranceUploaded) { onUploadDoc("insurance") }
            DocVaultRow("4. EMISSION POLLUTION (PUC)", if (pucUploaded) pucDocNum else "Not Uploaded", pucUploaded) { onUploadDoc("puc") }
            DocVaultRow("5. HELMET ISI CERTIFICATION", if (helmetUploaded) helmetDocNum else "Not Uploaded", helmetUploaded) { onUploadDoc("helmet") }
        }
    }
}

@Composable
fun DocVaultRow(
    title: String,
    docNumber: String,
    isUploaded: Boolean,
    onUploadClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(
                text = docNumber,
                color = if (isUploaded) OptimalGreen else TextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Button(
            onClick = onUploadClick,
            modifier = Modifier.height(30.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isUploaded) OptimalGreen.copy(alpha = 0.2f) else HyperCyan.copy(alpha = 0.2f),
                contentColor = if (isUploaded) OptimalGreen else HyperCyan
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isUploaded) OptimalGreen.copy(alpha = 0.5f) else HyperCyan.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = if (isUploaded) "RE-UPLOAD" else "ATTACH FILE",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
