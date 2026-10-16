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
import androidx.compose.material.icons.filled.AddLocation
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Visibility
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

// Data model for rider's saved places & motorcycle twisties
data class SavedPlace(
    val id: String,
    val name: String,
    val category: String, // "Twisties", "Pit Stop", "Expressway", "Mountain Pass"
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val distanceApprox: String,
    val highlight: String
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

    // Dialog state for adding a custom place
    var showAddPlaceDialog by remember { mutableStateOf(false) }
    var newPlaceName by remember { mutableStateOf("") }
    var newPlaceCategory by remember { mutableStateOf("Twisties") }
    var newPlaceLat by remember { mutableStateOf("") }
    var newPlaceLng by remember { mutableStateOf("") }
    var newPlaceDesc by remember { mutableStateOf("") }

    // Curated real saved places on Google Maps (Twisties, Pit Stops, Straights)
    val savedPlaces = remember {
        mutableStateListOf(
            SavedPlace(
                id = "p1",
                name = "Nandi Hills Peak & Twisties",
                category = "Twisties",
                latitude = 13.3702,
                longitude = 77.6835,
                description = "36 Hairpin Curves • High-Grip Mountain Asphalt",
                distanceApprox = "42 km away",
                highlight = "Apex Lean Rating: 9.6/10"
            ),
            SavedPlace(
                id = "p2",
                name = "Biker's Highway Cafe NH44",
                category = "Pit Stop",
                latitude = 13.1989,
                longitude = 77.7068,
                description = "Biker Pit Stop • 97 High Octane Petrol & Coffee",
                distanceApprox = "18 km away",
                highlight = "Tire Air & Diagnostics Station"
            ),
            SavedPlace(
                id = "p3",
                name = "Kolar Expressway Straights",
                category = "Expressway",
                latitude = 13.1378,
                longitude = 78.1291,
                description = "6-Lane Smooth Expressway • Top Speed & Aerodynamics",
                distanceApprox = "56 km away",
                highlight = "Sprint Benchmark Sector"
            ),
            SavedPlace(
                id = "p4",
                name = "Agumbe Rainforest Twisties",
                category = "Mountain Pass",
                latitude = 13.5042,
                longitude = 75.0931,
                description = "Famous Sunset Ghats • Technical Counter-steering",
                distanceApprox = "320 km away",
                highlight = "Legendary Wet Tarmac Route"
            ),
            SavedPlace(
                id = "p5",
                name = "Yercaud 20 Hairpins Loop",
                category = "Mountain Pass",
                latitude = 11.7753,
                longitude = 78.2093,
                description = "20 Tight Hairpin Bends • Steep Incline Climbing",
                distanceApprox = "215 km away",
                highlight = "Extreme Lean Practice"
            )
        )
    }

    // Curated real YouTube motorcycle telemetry & route videos
    val youtubeVideos = remember {
        listOf(
            VideoRecommendation(
                id = "k4aT9rGqB9M",
                title = "Yamaha MT-15 V2 Top Speed & Acceleration Test (VVA Telemetry)",
                channel = "Yamaha Racing Club",
                duration = "12:45",
                category = "Bike",
                viewCount = "480K views"
            ),
            VideoRecommendation(
                id = "9bZkp7q19f0",
                title = "Nandi Hills 36 Hairpins Cornering & Lean Angles (Dawn Motovlog)",
                channel = "Apex Motovlogs India",
                duration = "15:20",
                category = "Places",
                viewCount = "310K views"
            ),
            VideoRecommendation(
                id = "e-ORhEE9VVg",
                title = "NH44 High Speed Biker Breakfast Run - Bangalore to Kolar",
                channel = "Throttle Pulse",
                duration = "18:05",
                category = "Places",
                viewCount = "220K views"
            ),
            VideoRecommendation(
                id = "3JZ_D3ELwOQ",
                title = "Yamaha MT-15 V2 Maintenance: Tire Pressure, Chain Slack & ECU",
                channel = "Trackday Mechanics",
                duration = "10:30",
                category = "Bike",
                viewCount = "195K views"
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

    // Filtered lists based on search query
    val filteredPlaces = remember(searchQuery, selectedCategoryFilter, savedPlaces) {
        savedPlaces.filter { place ->
            val matchesCategory = selectedCategoryFilter == "All" ||
                    (selectedCategoryFilter == "Places" && (place.category == "Twisties" || place.category == "Mountain Pass" || place.category == "Expressway")) ||
                    place.category.equals(selectedCategoryFilter, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                    place.name.contains(searchQuery, ignoreCase = true) ||
                    place.category.contains(searchQuery, ignoreCase = true) ||
                    place.description.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    val filteredVideos = remember(searchQuery, selectedCategoryFilter) {
        youtubeVideos.filter { video ->
            val matchesCategory = selectedCategoryFilter == "All" ||
                    (selectedCategoryFilter == "Bike" && video.category == "Bike") ||
                    (selectedCategoryFilter == "Places" && video.category == "Places") ||
                    (selectedCategoryFilter == "Guides" && video.category == "Guides")
            val matchesQuery = searchQuery.isBlank() ||
                    video.title.contains(searchQuery, ignoreCase = true) ||
                    video.channel.contains(searchQuery, ignoreCase = true) ||
                    video.category.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    val scrollState = rememberScrollState()

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

        Spacer(modifier = Modifier.height(16.dp))

        // ================= 2. BIKE STATUS & VITALS CARD ("Tells about his bike") =================
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
                            contentDescription = "Yamaha MT-15",
                            tint = HyperCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "YAMAHA MT-15 V2",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "155cc Liquid-Cooled VVA • 18.4 PS",
                            color = HyperCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Status pill
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
                            text = "Fuel Level (8.2L remaining)",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "82% • ~380 KM Range",
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
                            .fillMaxWidth(0.82f)
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
                // Front & Rear TPMS
                BikeVitalMiniPill(
                    title = "TIRES (TPMS)",
                    value = "F: 32 | R: 34",
                    unit = "PSI",
                    status = "OPTIMAL",
                    accentColor = HyperCyan,
                    modifier = Modifier.weight(1f)
                )

                // Engine Coolant Temp
                BikeVitalMiniPill(
                    title = "COOLANT TEMP",
                    value = "88",
                    unit = "°C",
                    status = "NOMINAL",
                    accentColor = OptimalGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Battery Voltage
                BikeVitalMiniPill(
                    title = "BATTERY",
                    value = "12.6",
                    unit = "V",
                    status = "HEALTHY",
                    accentColor = ElectricBlue,
                    modifier = Modifier.weight(1f)
                )

                // Engine Oil Life & Odo
                BikeVitalMiniPill(
                    title = "OIL LIFE",
                    value = "76%",
                    unit = "8,420 KM",
                    status = "NEXT 2.4K",
                    accentColor = SportAmber,
                    modifier = Modifier.weight(1f)
                )
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
                        text = "Search Nandi Hills, MT-15 speed test, twisties...",
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
            val filterChips = listOf("All", "Places", "Bike", "Guides")
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

        // ================= 5. SAVED PLACES IN GOOGLE MAPS =================
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
                        text = "SAVED PLACES & TWISTIES",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Synced with Google Maps Navigation",
                        color = HyperCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(
                    onClick = { showAddPlaceDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ObsidianSurface)
                        .border(1.dp, CardBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddLocation,
                        contentDescription = "Save New Location",
                        tint = HyperCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredPlaces.isEmpty()) {
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
                        text = "No saved places matched \"$searchQuery\"",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    filteredPlaces.forEach { place ->
                        SavedPlaceCard(
                            place = place,
                            onNavigateInGoogleMaps = {
                                launchGoogleMaps(context, place.latitude, place.longitude, place.name)
                            }
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
                        val query = if (searchQuery.isNotBlank()) searchQuery else "Yamaha MT-15 V2 ride"
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
    }

    // ================= ADD SAVED LOCATION MODAL DIALOG =================
    if (showAddPlaceDialog) {
        AlertDialog(
            onDismissRequest = { showAddPlaceDialog = false },
            containerColor = MidnightCard,
            title = {
                Text(
                    text = "SAVE LOCATION TO MAPS",
                    color = HyperCyan,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newPlaceName,
                        onValueChange = { newPlaceName = it },
                        label = { Text("Place Name (e.g. Yelagiri Hills)", color = TextSecondary, fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HyperCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPlaceCategory,
                        onValueChange = { newPlaceCategory = it },
                        label = { Text("Category (Twisties / Pit Stop / Highway)", color = TextSecondary, fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HyperCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newPlaceLat,
                            onValueChange = { newPlaceLat = it },
                            label = { Text("Latitude", color = TextSecondary, fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HyperCyan,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newPlaceLng,
                            onValueChange = { newPlaceLng = it },
                            label = { Text("Longitude", color = TextSecondary, fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HyperCyan,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = newPlaceDesc,
                        onValueChange = { newPlaceDesc = it },
                        label = { Text("Description / Road Condition", color = TextSecondary, fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HyperCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lat = newPlaceLat.toDoubleOrNull() ?: 12.9716
                        val lng = newPlaceLng.toDoubleOrNull() ?: 77.5946
                        if (newPlaceName.isNotBlank()) {
                            savedPlaces.add(
                                SavedPlace(
                                    id = "p_${System.currentTimeMillis()}",
                                    name = newPlaceName,
                                    category = newPlaceCategory.ifBlank { "Twisties" },
                                    latitude = lat,
                                    longitude = lng,
                                    description = newPlaceDesc.ifBlank { "Custom saved destination" },
                                    distanceApprox = "Custom Route",
                                    highlight = "Custom Rider Pin"
                                )
                            )
                            Toast.makeText(context, "Saved $newPlaceName to Google Maps list", Toast.LENGTH_SHORT).show()
                            showAddPlaceDialog = false
                            newPlaceName = ""
                            newPlaceLat = ""
                            newPlaceLng = ""
                            newPlaceDesc = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HyperCyan, contentColor = CarbonDark)
                ) {
                    Text("SAVE PLACE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPlaceDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
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
fun SavedPlaceCard(
    place: SavedPlace,
    onNavigateInGoogleMaps: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightCard)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clickable { onNavigateInGoogleMaps() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Category Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (place.category) {
                                    "Twisties" -> HyperCyan.copy(alpha = 0.15f)
                                    "Mountain Pass" -> SportAmber.copy(alpha = 0.15f)
                                    "Expressway" -> CobaltBlue.copy(alpha = 0.15f)
                                    else -> OptimalGreen.copy(alpha = 0.15f)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = place.category.uppercase(),
                            color = when (place.category) {
                                "Twisties" -> HyperCyan
                                "Mountain Pass" -> SportAmber
                                "Expressway" -> ElectricBlue
                                else -> OptimalGreen
                            },
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.6.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = place.distanceApprox,
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = place.name,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = place.description,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "★ ${place.highlight}",
                    color = HyperCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // 1-Tap Google Maps Navigation Button
            Button(
                onClick = onNavigateInGoogleMaps,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OptimalGreen,
                    contentColor = CarbonDark
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 12.dp,
                    vertical = 8.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Directions,
                    contentDescription = "Navigate",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "MAPS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
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
