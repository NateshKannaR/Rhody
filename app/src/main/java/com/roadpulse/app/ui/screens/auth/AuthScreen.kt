package com.roadpulse.app.ui.screens.auth

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadpulse.app.data.auth.AuthManager
import com.roadpulse.app.ui.theme.CarbonDark
import com.roadpulse.app.ui.theme.CardBorder
import com.roadpulse.app.ui.theme.MidnightCard
import com.roadpulse.app.ui.theme.NeonCyan
import com.roadpulse.app.ui.theme.RacingRed
import com.roadpulse.app.ui.theme.TextMuted
import com.roadpulse.app.ui.theme.TextPrimary
import com.roadpulse.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onAuthenticated: (role: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // Upfront Role Gateway: "rider" vs "parent"
    var selectedRole by remember { mutableStateOf("rider") }

    // Rider Form State
    var isLoginMode by remember { mutableStateOf(true) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Parent Form State
    var parentNameInput by remember { mutableStateOf("") }
    var parentEmailInput by remember { mutableStateOf("") }
    var riderPairingCodeInput by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    val guardianPurple = Color(0xFFA855F7)

    fun performRiderAuth() {
        focusManager.clearFocus()
        errorMessage = null

        val email = emailInput.trim()
        val password = passwordInput.trim()

        if (email.isEmpty() || !email.contains("@")) {
            errorMessage = "Please enter a valid email address"
            return
        }

        if (password.length < 6) {
            errorMessage = "Password must be at least 6 characters"
            return
        }

        if (!isLoginMode && nameInput.trim().isEmpty()) {
            errorMessage = "Please enter your name"
            return
        }

        isLoading = true
        coroutineScope.launch {
            val result = if (isLoginMode) {
                AuthManager.login(email, password)
            } else {
                AuthManager.register(email, password, nameInput.trim())
            }

            isLoading = false
            result.onSuccess {
                onAuthenticated("rider")
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Authentication failed. Check credentials or network."
            }
        }
    }

    fun performParentConnect() {
        focusManager.clearFocus()
        errorMessage = null

        val pName = parentNameInput.trim().ifEmpty { "Family Guardian" }
        val pEmail = parentEmailInput.trim().lowercase()
        val rCodeOrEmail = riderPairingCodeInput.trim().lowercase()

        // Validate parent email format
        if (pEmail.isEmpty()) {
            errorMessage = "Please enter the parent / guardian email address"
            return
        }
        if (!pEmail.contains("@") || !pEmail.contains(".")) {
            errorMessage = "Please enter a valid guardian email address"
            return
        }

        // Validate rider pairing input
        if (rCodeOrEmail.isEmpty()) {
            errorMessage = "Please enter rider's pairing code or email (e.g. RHODY-8821)"
            return
        }

        // Prevent parent from using the rider's email
        if (pEmail == rCodeOrEmail) {
            errorMessage = "Parent email cannot be the same as rider email! A guardian and rider must be separate individuals."
            return
        }

        val riderPrefs = context.getSharedPreferences("roadpulse_auth_prefs", Context.MODE_PRIVATE)
        val savedRiderEmail = riderPrefs.getString("user_email", "")?.trim()?.lowercase() ?: ""
        if (savedRiderEmail.isNotEmpty() && pEmail == savedRiderEmail) {
            errorMessage = "This email ($pEmail) is registered as a Rider. A parent must use a different, separate email address."
            return
        }

        val wardEmail = if (rCodeOrEmail.contains("@")) {
            rCodeOrEmail
        } else if (savedRiderEmail.isNotEmpty()) {
            savedRiderEmail
        } else {
            rCodeOrEmail
        }

        val wardCode = if (!rCodeOrEmail.contains("@")) rCodeOrEmail.uppercase() else (savedRiderEmail.ifEmpty { "PAIRED" })

        // Persist parent credentials to preferences
        val prefs = context.getSharedPreferences("rhody_parent_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("parent_name", pName)
            .putString("parent_email", pEmail)
            .putString("paired_rider_code", wardCode)
            .putString("monitored_rider_email", wardEmail)
            .putBoolean("is_parent_logged_in", true)
            .apply()

        onAuthenticated("parent")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Monogram
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MidnightCard)
                    .border(1.dp, CardBorder, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Rhody Logo",
                    tint = NeonCyan,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "RHODY TELEMETRY",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = TextPrimary
            )

            Text(
                text = "Precision Motorcycle GPS & Guardian Safety Radar",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Gateway Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MidnightCard)
                    .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                // UPFRONT ROLE GATEWAY SWITCHER: RIDER COCKPIT vs GUARDIAN PORTAL
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CarbonDark)
                        .padding(4.dp)
                ) {
                    // Rider Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedRole == "rider") NeonCyan.copy(alpha = 0.15f) else Color.Transparent)
                            .border(
                                width = if (selectedRole == "rider") 1.dp else 0.dp,
                                color = if (selectedRole == "rider") NeonCyan.copy(alpha = 0.4f) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                selectedRole = "rider"
                                errorMessage = null
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TwoWheeler,
                                contentDescription = null,
                                tint = if (selectedRole == "rider") NeonCyan else TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "RIDER COCKPIT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                color = if (selectedRole == "rider") NeonCyan else TextMuted
                            )
                        }
                    }

                    // Guardian Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedRole == "parent") guardianPurple.copy(alpha = 0.15f) else Color.Transparent)
                            .border(
                                width = if (selectedRole == "parent") 1.dp else 0.dp,
                                color = if (selectedRole == "parent") guardianPurple.copy(alpha = 0.4f) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                selectedRole = "parent"
                                errorMessage = null
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (selectedRole == "parent") guardianPurple else TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "GUARDIAN RADAR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                color = if (selectedRole == "parent") guardianPurple else TextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ================= TAB 1: RIDER COCKPIT LOGIN / REGISTER / BYPASS =================
                if (selectedRole == "rider") {
                    // Sign In vs Register sub-tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CarbonDark)
                            .padding(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isLoginMode) MidnightCard else Color.Transparent)
                                .clickable {
                                    isLoginMode = true
                                    errorMessage = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "SIGN IN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLoginMode) NeonCyan else TextMuted
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isLoginMode) MidnightCard else Color.Transparent)
                                .clickable {
                                    isLoginMode = false
                                    errorMessage = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "REGISTER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isLoginMode) NeonCyan else TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Name Field (Register Mode Only)
                    AnimatedVisibility(visible = !isLoginMode) {
                        Column {
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text("Full Name", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = CardBorder,
                                    focusedLabelColor = NeonCyan,
                                    unfocusedLabelColor = TextMuted,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    cursorColor = NeonCyan
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // Email Field
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Rider Email", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedLabelColor = NeonCyan,
                            unfocusedLabelColor = TextMuted,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = NeonCyan
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Field
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { performRiderAuth() }),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedLabelColor = NeonCyan,
                            unfocusedLabelColor = TextMuted,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = NeonCyan
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Error Message
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = RacingRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Rider Submit Button
                    Button(
                        onClick = { performRiderAuth() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = CarbonDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = CarbonDark,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isLoginMode) "SIGN IN TO COCKPIT" else "CREATE ACCOUNT",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // PROMINENT BYPASS BUTTON: ENTER WITHOUT DETAILS
                    OutlinedButton(
                        onClick = { onAuthenticated("rider") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextSecondary
                        )
                    ) {
                        Text(
                            text = "Enter Without Details (Set up later)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // ================= TAB 2: PARENT / GUARDIAN RADAR PORTAL =================
                if (selectedRole == "parent") {
                    Text(
                        text = "Connect to Rider Safety Radar",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Real-time GPS beacon, high-G crash detection alerts, and motorcycle statutory document status monitoring.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                    )

                    // Parent Name
                    OutlinedTextField(
                        value = parentNameInput,
                        onValueChange = { parentNameInput = it },
                        label = { Text("Parent / Guardian Name", fontSize = 12.sp) },
                        placeholder = { Text("e.g. Dad & Mom", fontSize = 12.sp, color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = guardianPurple,
                            unfocusedBorderColor = CardBorder,
                            focusedLabelColor = guardianPurple,
                            unfocusedLabelColor = TextMuted,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = guardianPurple
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Parent Email
                    OutlinedTextField(
                        value = parentEmailInput,
                        onValueChange = { parentEmailInput = it },
                        label = { Text("Guardian Email", fontSize = 12.sp) },
                        placeholder = { Text("guardian@family.io", fontSize = 12.sp, color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = guardianPurple,
                            unfocusedBorderColor = CardBorder,
                            focusedLabelColor = guardianPurple,
                            unfocusedLabelColor = TextMuted,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = guardianPurple
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Rider Pairing Code / Email
                    OutlinedTextField(
                        value = riderPairingCodeInput,
                        onValueChange = { riderPairingCodeInput = it },
                        label = { Text("Rider Pairing Code or Email", fontSize = 12.sp) },
                        placeholder = { Text("e.g. rider@rhody.io or pairing code", fontSize = 12.sp, color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = guardianPurple, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { performParentConnect() }),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = guardianPurple,
                            unfocusedBorderColor = CardBorder,
                            focusedLabelColor = guardianPurple,
                            unfocusedLabelColor = TextMuted,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = guardianPurple
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Connect Button
                    Button(
                        onClick = { performParentConnect() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = guardianPurple,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "CONNECT TO GUARDIAN RADAR",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                    }

                    val localRiderPrefs = remember { context.getSharedPreferences("roadpulse_auth_prefs", Context.MODE_PRIVATE) }
                    val localRiderEmail = remember { localRiderPrefs.getString("user_email", "") ?: "" }
                    if (localRiderEmail.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                riderPairingCodeInput = localRiderEmail
                                if (parentNameInput.isBlank()) parentNameInput = "Guardian"
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, guardianPurple.copy(alpha = 0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = guardianPurple
                            )
                        ) {
                            Text(
                                text = "Link Registered Rider ($localRiderEmail)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

