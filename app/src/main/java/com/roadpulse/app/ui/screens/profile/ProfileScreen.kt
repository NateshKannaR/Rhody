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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
import com.roadpulse.app.ui.theme.ElectricBlue
import com.roadpulse.app.ui.theme.MidnightCard
import com.roadpulse.app.ui.theme.NeonCyan
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

    var isLoginMode by remember { mutableStateOf(true) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

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
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ACCOUNT & CLOUD",
                    color = NeonCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    text = if (authState.isLoggedIn) "MONGODB ATLAS SYNCHRONIZED" else "OFFLINE / LOCAL STORAGE",
                    color = if (authState.isLoggedIn) SuccessGreen else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            IconButton(
                onClick = { showServerConfig = !showServerConfig },
                modifier = Modifier
                    .size(40.dp)
                    .background(MidnightCard, shape = CircleShape)
                    .border(1.dp, CardBorder, shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Server Settings",
                    tint = if (showServerConfig) NeonCyan else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Collapsible Server Configuration Card
        AnimatedVisibility(visible = showServerConfig) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .background(MidnightCard, shape = RoundedCornerShape(16.dp))
                    .border(1.dp, ElectricBlue.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Backend Server & MongoDB Gateway",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Configure API endpoint (use http://10.0.2.2:3000 for Android Emulator, or local machine LAN IP on physical phones):",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = serverUrlInput,
                    onValueChange = { serverUrlInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Server URL", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            CloudSyncManager.setServerUrl(context, serverUrlInput)
                            Toast.makeText(context, "Server URL updated!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save URL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isCheckingHealth) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonCyan, strokeWidth = 2.dp)
                        } else {
                            Text("Test Ping", fontSize = 12.sp, color = NeonCyan)
                        }
                    }
                }

                healthStatusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = msg,
                        color = if (msg.contains("Online", ignoreCase = true)) SuccessGreen else RacingRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (authState.isLoggedIn && authState.user != null) {
            // ================= LOGGED IN STATE =================
            val user = authState.user!!

            // User Profile Hero Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MidnightCard, shape = RoundedCornerShape(20.dp))
                    .border(1.dp, CardBorder, shape = RoundedCornerShape(20.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar badge
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(CarbonDark)
                        .border(2.dp, NeonCyan, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = user.name.firstOrNull()?.uppercaseChar()?.toString()
                        ?: user.email.firstOrNull()?.uppercaseChar()?.toString()
                        ?: "D"
                    Text(
                        text = initial,
                        color = NeonCyan,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = user.name.ifEmpty { "RoadPulse Pilot" },
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = user.email,
                    color = TextSecondary,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Cloud Status Pill
                Row(
                    modifier = Modifier
                        .background(CarbonDark, shape = RoundedCornerShape(30.dp))
                        .border(1.dp, SuccessGreen.copy(alpha = 0.4f), shape = RoundedCornerShape(30.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MongoDB Atlas Connected",
                        color = SuccessGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cloud Synchronization Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MidnightCard, shape = RoundedCornerShape(20.dp))
                    .border(1.dp, CardBorder, shape = RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Cloud Telemetry Sync",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Cluster: cluster0.wwp3oig.mongodb.net",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Your trips and high-frequency GPS fixes are automatically pushed to your account and visualized on the web dashboard.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSyncingTrips
                ) {
                    if (isSyncingTrips) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = CarbonDark,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Syncing with Atlas...",
                            color = CarbonDark,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = CarbonDark
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sync All Local Trips Now",
                            color = CarbonDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                syncStatusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = msg,
                        color = if (msg.contains("Successfully", ignoreCase = true)) SuccessGreen else SportAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Logout Button
            OutlinedButton(
                onClick = {
                    AuthManager.logout()
                    Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = RacingRed
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, RacingRed.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    tint = RacingRed
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sign Out of RoadPulse",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = RacingRed
                )
            }

        } else {
            // ================= LOGGED OUT (SIGN IN / REGISTER) =================

            // Header Banner
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MidnightCard, shape = RoundedCornerShape(20.dp))
                    .border(1.dp, CardBorder, shape = RoundedCornerShape(20.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.1f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.3f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "CLOUD TELEMETRY",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Sign in or register to sync your trips, sprint timers, and live routes with MongoDB Atlas and access the companion Web Dashboard.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Toggle tabs: [ Sign In ] [ Create Account ]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CarbonDark, shape = RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isLoginMode) ElectricBlue else Color.Transparent)
                            .clickable {
                                isLoginMode = true
                                errorMessage = null
                                successMessage = null
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sign In",
                            color = if (isLoginMode) TextPrimary else TextSecondary,
                            fontWeight = if (isLoginMode) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isLoginMode) ElectricBlue else Color.Transparent)
                            .clickable {
                                isLoginMode = false
                                errorMessage = null
                                successMessage = null
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Create Account",
                            color = if (!isLoginMode) TextPrimary else TextSecondary,
                            fontWeight = if (!isLoginMode) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Form Inputs Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MidnightCard, shape = RoundedCornerShape(20.dp))
                    .border(1.dp, CardBorder, shape = RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                // If Register mode: Name Field
                if (!isLoginMode) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Driver / Rider Name") },
                        leadingIcon = {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = TextSecondary)
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Email Field
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email Address") },
                    placeholder = { Text("driver@roadpulse.io", color = TextMuted) },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = TextSecondary)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Password Field
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = TextSecondary
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    )
                )

                // Error / Success Message Banner
                errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RacingRed.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp))
                            .border(1.dp, RacingRed.copy(alpha = 0.4f), shape = RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = RacingRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = err, color = RacingRed, fontSize = 13.sp)
                    }
                }

                successMessage?.let { succ ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SuccessGreen.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp))
                            .border(1.dp, SuccessGreen.copy(alpha = 0.4f), shape = RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = succ, color = SuccessGreen, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Submit Button
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        val email = emailInput.trim()
                        val password = passwordInput.trim()

                        if (email.isEmpty() || !email.contains("@")) {
                            errorMessage = "Please enter a valid email address."
                            return@Button
                        }
                        if (password.length < 6) {
                            errorMessage = "Password must be at least 6 characters."
                            return@Button
                        }

                        isLoading = true
                        errorMessage = null
                        successMessage = null

                        coroutineScope.launch {
                            if (isLoginMode) {
                                val result = AuthManager.login(email, password)
                                isLoading = false
                                if (result.isSuccess) {
                                    val user = result.getOrNull()
                                    successMessage = "Welcome back, ${user?.name}!"
                                    Toast.makeText(context, "Welcome back, ${user?.name}!", Toast.LENGTH_SHORT).show()
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Login failed"
                                }
                            } else {
                                val name = nameInput.trim().ifEmpty { email.substringBefore("@") }
                                val result = AuthManager.register(email, password, name)
                                isLoading = false
                                if (result.isSuccess) {
                                    val user = result.getOrNull()
                                    successMessage = "Account created! Welcome, ${user?.name}."
                                    Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Registration failed"
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLoginMode) NeonCyan else ElectricBlue,
                        contentColor = if (isLoginMode) CarbonDark else TextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = if (isLoginMode) CarbonDark else TextPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isLoginMode) "Signing In..." else "Creating Account...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    } else {
                        Text(
                            text = if (isLoginMode) "Sign In to RoadPulse" else "Create RoadPulse Account",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Architecture Note
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MidnightCard.copy(alpha = 0.6f), shape = RoundedCornerShape(16.dp))
                    .border(1.dp, CardBorder, shape = RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "ROAD PULSE ARCHITECTURE",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• 100% Offline-First: Trip recording never relies on network connection.\n• Automatic MongoDB Sync: Synchronizes trip coordinates, distance, and speeds when connected.\n• Web Companion: Real-time map and speed vs distance graphs accessible anywhere.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
