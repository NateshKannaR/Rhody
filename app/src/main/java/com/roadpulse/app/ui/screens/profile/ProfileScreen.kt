package com.roadpulse.app.ui.screens.profile

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadpulse.app.RoadPulseApp
import com.roadpulse.app.data.auth.AuthManager
import com.roadpulse.app.data.sync.CloudSyncManager
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
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = (context.applicationContext as RoadPulseApp).container.tripRepository
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val authState by AuthManager.authState.collectAsState()

    // Preferences & Settings State
    var isKmhSelected by remember { mutableStateOf(true) }
    var isHighFrequencyGps by remember { mutableStateOf(true) }
    var isLeanWarningEnabled by remember { mutableStateOf(true) }
    var isAutoPauseEnabled by remember { mutableStateOf(true) }

    // Rider Emergency Info State
    var bloodGroup by remember { mutableStateOf("O+") }
    var emergencyContactName by remember { mutableStateOf("Parents / ICE") }
    var emergencyContactPhone by remember { mutableStateOf("+91 98765 43210") }

    // Auth form state
    var isLoginMode by remember { mutableStateOf(true) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("Natesh Kanna") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Server & Cloud config
    var serverUrlInput by remember { mutableStateOf(CloudSyncManager.serverBaseUrl) }
    var showServerConfig by remember { mutableStateOf(false) }
    var isCheckingHealth by remember { mutableStateOf(false) }
    var healthStatusMessage by remember { mutableStateOf<String?>(null) }

    var isSyncingTrips by remember { mutableStateOf(false) }
    var syncStatusMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonDark)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ================= HEADER =================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SETTINGS & PROFILE",
                    color = HyperCyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = if (authState.isLoggedIn) "MONGODB ATLAS SYNCHRONIZED" else "OFFLINE / LOCAL STORAGE MODE",
                    color = if (authState.isLoggedIn) OptimalGreen else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }

            IconButton(
                onClick = { showServerConfig = !showServerConfig },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(ObsidianSurface)
                    .border(1.dp, CardBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Config",
                    tint = if (showServerConfig) HyperCyan else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ================= COLLAPSIBLE SERVER CONFIG =================
        AnimatedVisibility(visible = showServerConfig) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MidnightCard)
                    .border(1.dp, CardBorderGlow, RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        tint = HyperCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BACKEND GATEWAY & MONGODB",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = serverUrlInput,
                    onValueChange = { serverUrlInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Server Base URL", color = TextSecondary, fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HyperCyan,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = ObsidianSurface,
                        unfocusedContainerColor = ObsidianSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            CloudSyncManager.setServerUrl(context, serverUrlInput)
                            Toast.makeText(context, "Server URL updated!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HyperCyan, contentColor = CarbonDark),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save URL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            isCheckingHealth = true
                            healthStatusMessage = null
                            coroutineScope.launch {
                                CloudSyncManager.setServerUrl(context, serverUrlInput)
                                val res = CloudSyncManager.checkServerHealth()
                                isCheckingHealth = false
                                if (res.isSuccess) {
                                    val obj = res.getOrNull()
                                    val db = obj?.optString("database", "connected")
                                    healthStatusMessage = "Server Online! MongoDB: $db"
                                } else {
                                    healthStatusMessage = "Connection failed: ${res.exceptionOrNull()?.message}"
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = HyperCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        if (isCheckingHealth) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = HyperCyan, strokeWidth = 2.dp)
                        } else {
                            Text("Test Ping", fontSize = 11.sp)
                        }
                    }
                }

                healthStatusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = msg,
                        color = if (msg.contains("Online", ignoreCase = true)) OptimalGreen else RacingRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ================= 1. RIDER IDENTITY & HERO PROFILE CARD =================
        val displayName = if (authState.isLoggedIn && !authState.user?.name.isNullOrBlank()) {
            authState.user!!.name
        } else {
            "Natesh Kanna"
        }
        val displayEmail = if (authState.isLoggedIn && !authState.user?.email.isNullOrBlank()) {
            authState.user!!.email
        } else {
            "natesh.kanna@rhody.io"
        }

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
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(HyperCyan, CobaltBlue)
                        )
                    )
                    .border(2.dp, HyperCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "N",
                    color = CarbonDark,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = displayName,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )

            Text(
                text = displayEmail,
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Cloud Status Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(CarbonDark)
                    .border(1.dp, if (authState.isLoggedIn) OptimalGreen.copy(alpha = 0.5f) else CardBorder, RoundedCornerShape(30.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (authState.isLoggedIn) OptimalGreen else SportAmber)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (authState.isLoggedIn) "MongoDB Atlas Synced" else "Offline Local Storage Mode",
                    color = if (authState.isLoggedIn) OptimalGreen else SportAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ================= 2. EMERGENCY MEDICAL & SAFETY CARD =================
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
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = null,
                        tint = RacingRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RIDER EMERGENCY & SAFETY (ICE)",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(RacingRed.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "BLOOD: $bloodGroup",
                        color = RacingRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingInfoItem(
                    title = "EMERGENCY CONTACT",
                    value = emergencyContactPhone,
                    icon = Icons.Default.Phone,
                    accentColor = HyperCyan,
                    modifier = Modifier.weight(1f)
                )
                SettingInfoItem(
                    title = "BLOOD GROUP",
                    value = "$bloodGroup (Universal Donor)",
                    icon = Icons.Default.HealthAndSafety,
                    accentColor = RacingRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ================= 3. MOTORCYCLE GARAGE SPECS =================
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
                        imageVector = Icons.Default.TwoWheeler,
                        contentDescription = null,
                        tint = HyperCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MOTORCYCLE GARAGE",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                }

                Text(
                    text = "PRIMARY BIKE",
                    color = HyperCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingInfoItem(
                    title = "BIKE MODEL",
                    value = "Yamaha MT-15 V2",
                    icon = Icons.Default.TwoWheeler,
                    accentColor = HyperCyan,
                    modifier = Modifier.weight(1f)
                )
                SettingInfoItem(
                    title = "REGISTRATION",
                    value = "KA-01-EQ-1550",
                    icon = Icons.Default.Badge,
                    accentColor = OptimalGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingInfoItem(
                    title = "ENGINE DISPLACEMENT",
                    value = "155cc Liquid VVA",
                    icon = Icons.Default.Speed,
                    accentColor = SportAmber,
                    modifier = Modifier.weight(1f)
                )
                SettingInfoItem(
                    title = "TIRE TARGETS",
                    value = "F: 32 PSI | R: 34 PSI",
                    icon = Icons.Default.Tune,
                    accentColor = ElectricBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ================= 4. APP PREFERENCES & UNIT SWITCHES =================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MidnightCard)
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "APPLICATION PREFERENCES",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Speed Units Switch: KM/H vs MPH
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Speed & Distance Units",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isKmhSelected) "Metric (KM/H, Kilometers)" else "Imperial (MPH, Miles)",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianSurface)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isKmhSelected) HyperCyan else Color.Transparent)
                            .clickable { isKmhSelected = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "KM/H",
                            color = if (isKmhSelected) CarbonDark else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isKmhSelected) HyperCyan else Color.Transparent)
                            .clickable { isKmhSelected = false }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "MPH",
                            color = if (!isKmhSelected) CarbonDark else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // High Precision GPS Toggle
            PreferenceToggleRow(
                title = "High-Precision GPS (1 Hz)",
                subtitle = "Sample GPS fix every 1000ms for precision telemetry",
                checked = isHighFrequencyGps,
                onCheckedChange = { isHighFrequencyGps = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Motorcycle Lean Warning Toggle
            PreferenceToggleRow(
                title = "Apex Lean Warning (>45°)",
                subtitle = "Audible & visual warning on extreme motorcycle lean",
                checked = isLeanWarningEnabled,
                onCheckedChange = { isLeanWarningEnabled = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Auto-pause Toggle
            PreferenceToggleRow(
                title = "Auto-Pause at Red Lights & Stops",
                subtitle = "Exclude idle traffic stop duration from moving speed",
                checked = isAutoPauseEnabled,
                onCheckedChange = { isAutoPauseEnabled = it }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ================= 5. CLOUD TELEMETRY SYNC TO MONGODB =================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MidnightCard)
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = HyperCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MONGODB ATLAS CLOUD SYNC",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Pushes all locally saved rides to MongoDB Atlas cluster for instant companion viewing on the Rhody Web Dashboard.",
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    isSyncingTrips = true
                    syncStatusMessage = null
                    coroutineScope.launch {
                        try {
                            val trips = repository.getAllTrips()
                            var syncedCount = 0
                            var errorCount = 0
                            for (trip in trips) {
                                val points = repository.getPoints(trip.id)
                                val result = CloudSyncManager.syncTripToMongo(trip, points)
                                if (result.isSuccess) {
                                    syncedCount++
                                } else {
                                    errorCount++
                                }
                            }
                            syncStatusMessage = if (errorCount == 0) {
                                "Successfully synced $syncedCount trips to MongoDB Atlas!"
                            } else {
                                "Synced $syncedCount trips ($errorCount failed). Check server."
                            }
                        } catch (e: Exception) {
                            syncStatusMessage = "Sync error: ${e.message}"
                        } finally {
                            isSyncingTrips = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HyperCyan, contentColor = CarbonDark),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSyncingTrips
            ) {
                if (isSyncingTrips) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CarbonDark, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Syncing to Atlas...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sync All Local Trips to Cloud", fontWeight = FontWeight.Black)
                }
            }

            syncStatusMessage?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = msg,
                    color = if (msg.contains("Successfully", ignoreCase = true)) OptimalGreen else SportAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ================= 6. ACCOUNT SIGN IN / OUT =================
        if (authState.isLoggedIn) {
            OutlinedButton(
                onClick = {
                    AuthManager.logout()
                    Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RacingRed),
                border = androidx.compose.foundation.BorderStroke(1.dp, RacingRed.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Logout, contentDescription = null, tint = RacingRed, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Sign Out of Rhody Account", fontWeight = FontWeight.Bold, color = RacingRed)
            }
        } else {
            // Sign in / Create Account Form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MidnightCard)
                    .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                // Tab switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianSurface)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isLoginMode) HyperCyan else Color.Transparent)
                            .clickable { isLoginMode = true }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SIGN IN",
                            color = if (isLoginMode) CarbonDark else TextSecondary,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isLoginMode) HyperCyan else Color.Transparent)
                            .clickable { isLoginMode = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "REGISTER",
                            color = if (!isLoginMode) CarbonDark else TextSecondary,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!isLoginMode) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Rider Name", fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HyperCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HyperCyan,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password", fontSize = 11.sp) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HyperCyan,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        val email = emailInput.trim()
                        val password = passwordInput.trim()
                        if (email.isEmpty() || !email.contains("@")) {
                            Toast.makeText(context, "Enter a valid email", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isLoading = true
                        coroutineScope.launch {
                            if (isLoginMode) {
                                val result = AuthManager.login(email, password)
                                isLoading = false
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Welcome back, ${result.getOrNull()?.name}!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Login failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                val name = nameInput.trim().ifEmpty { "Natesh Kanna" }
                                val result = AuthManager.register(email, password, name)
                                isLoading = false
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Account created!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HyperCyan, contentColor = CarbonDark),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CarbonDark, strokeWidth = 2.dp)
                    } else {
                        Text(if (isLoginMode) "Sign In" else "Create Account", fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ================= 7. APP INFO FOOTER =================
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "RHODY MOTORCYCLE TELEMETRY",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = "Version 2.4.0 • Obsidian Cyber Build 2026.10",
                color = TextMuted,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Designed for high-performance telemetry & rider safety.",
                color = TextMuted,
                fontSize = 9.sp
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

// Subcomponents
@Composable
private fun SettingInfoItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = title,
                    color = TextMuted,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PreferenceToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CarbonDark,
                checkedTrackColor = HyperCyan,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = ObsidianSurface
            )
        )
    }
}
