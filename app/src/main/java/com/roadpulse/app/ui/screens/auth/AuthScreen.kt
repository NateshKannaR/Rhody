package com.roadpulse.app.ui.screens.auth

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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadpulse.app.data.auth.AuthManager
import com.roadpulse.app.data.auth.AuthUser
import com.roadpulse.app.ui.theme.CarbonDark
import com.roadpulse.app.ui.theme.CardBorder
import com.roadpulse.app.ui.theme.ElectricBlue
import com.roadpulse.app.ui.theme.MidnightCard
import com.roadpulse.app.ui.theme.NeonCyan
import com.roadpulse.app.ui.theme.RacingRed
import com.roadpulse.app.ui.theme.SuccessGreen
import com.roadpulse.app.ui.theme.TextMuted
import com.roadpulse.app.ui.theme.TextPrimary
import com.roadpulse.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var isLoginMode by remember { mutableStateOf(true) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    fun performAuth() {
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
                onAuthenticated()
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Authentication failed. Please check credentials or network."
            }
        }
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
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Telemetry Logo Monogram
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MidnightCard)
                    .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Rhody Logo",
                    tint = NeonCyan,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "RHODY TELEMETRY",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = TextPrimary
            )

            Text(
                text = "Precision Motorcycle GPS & Guardian Live Safety",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Card Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MidnightCard)
                    .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                // Sign In / Sign Up Segmented Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CarbonDark)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isLoginMode) MidnightCard else Color.Transparent)
                            .clickable {
                                isLoginMode = true
                                errorMessage = null
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SIGN IN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = if (isLoginMode) NeonCyan else TextMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isLoginMode) MidnightCard else Color.Transparent)
                            .clickable {
                                isLoginMode = false
                                errorMessage = null
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "REGISTER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = if (!isLoginMode) NeonCyan else TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Name Field (Register Mode Only)
                AnimatedVisibility(visible = !isLoginMode) {
                    Column {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Full Name") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted)
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
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                // Email Field
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Email Address") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = TextMuted)
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

                Spacer(modifier = Modifier.height(14.dp))

                // Password Field
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = TextMuted
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { performAuth() }),
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
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = RacingRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Submit Button
                Button(
                    onClick = { performAuth() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = TextPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isLoginMode) "SIGN IN TO RHODY" else "CREATE ACCOUNT",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Guest / Offline fallback option
            Text(
                text = "Continue as Guest (Offline Mode)",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        // Enter app as guest session
                        onAuthenticated()
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}
