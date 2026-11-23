package com.roadpulse.app.ui.screens.auth

import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadpulse.app.R
import com.roadpulse.app.data.auth.AuthManager
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val colors = MaterialTheme.colorScheme
    var registering by rememberSaveable { mutableStateOf(false) }
    var guardian by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var riderEmail by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (loading) return
        error = when {
            !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Enter a valid email address."
            registering && name.isBlank() -> "Enter your full name."
            registering && password.length < 6 -> "Use at least 6 characters for your password."
            password.isEmpty() -> "Enter your password."
            registering && guardian && !Patterns.EMAIL_ADDRESS.matcher(riderEmail.trim()).matches() ->
                "Enter your rider’s registered email address."
            registering && guardian && email.trim().equals(riderEmail.trim(), ignoreCase = true) ->
                "Use a different email address for your guardian account."
            else -> null
        }
        if (error != null) return
        focusManager.clearFocus()
        loading = true
        scope.launch {
            try {
                val result = if (registering) {
                    AuthManager.register(email, password, name, if (guardian) "parent" else "rider", riderEmail)
                } else {
                    AuthManager.login(email, password)
                }
                error = result.exceptionOrNull()?.message
            } finally {
                loading = false
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize().background(colors.background).imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(painterResource(R.drawable.rhody_brand_logo), null, Modifier.size(44.dp))
                Text("Rhody", style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
            }
            val cardShape = RoundedCornerShape(24.dp)
            Column(
                modifier = Modifier.fillMaxWidth().background(colors.surface, cardShape)
                    .border(1.dp, colors.surfaceVariant, cardShape).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    if (registering) "Create your account" else "Sign in",
                    color = colors.onSurface,
                    fontSize = 30.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (registering) "Choose how you’ll use Rhody." else "Use your Rhody email and password.",
                    color = colors.onSurfaceVariant
                )
                AuthChoiceRow(
                    first = "Sign in", second = "Create account", secondSelected = registering,
                    enabled = !loading
                ) {
                    registering = it
                    error = null
                }
                if (registering) {
                    Text("Account type", color = colors.onSurface, fontWeight = FontWeight.SemiBold)
                    AuthChoiceRow("Rider", "Guardian", guardian, !loading) {
                        guardian = it
                        error = null
                    }
                    OutlinedTextField(
                        value = name, onValueChange = { name = it.take(120) },
                        label = { Text("Full name") }, enabled = !loading,
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                }
                OutlinedTextField(
                    value = email, onValueChange = { email = it.take(254) },
                    label = { Text("Email") }, enabled = !loading,
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it.take(256) },
                    label = { Text("Password") }, enabled = !loading,
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (registering && guardian) ImeAction.Next else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    supportingText = if (registering) ({ Text("Use at least 6 characters.") }) else null,
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }, enabled = !loading) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                if (showPassword) "Hide password" else "Show password"
                            )
                        }
                    }
                )
                if (registering && guardian) {
                    OutlinedTextField(
                        value = riderEmail, onValueChange = { riderEmail = it.take(254) },
                        label = { Text("Rider email") }, enabled = !loading,
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        supportingText = { Text("Your rider must approve the request before you can see their telemetry.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() })
                    )
                }
                error?.let {
                    Text(
                        it, color = colors.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
                Button(
                    onClick = { submit() },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = colors.onSurfaceVariant
                        )
                        Text(
                            if (registering) "Creating account…" else "Signing in…",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    } else {
                        Text(if (registering) "Create account" else "Sign in")
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthChoiceRow(
    first: String,
    second: String,
    secondSelected: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().background(colors.background, shape).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(first, second).forEachIndexed { index, label ->
            val active = (index == 1) == secondSelected
            TextButton(
                onClick = { onChange(index == 1) },
                enabled = enabled,
                shape = shape,
                modifier = Modifier.weight(1f).heightIn(min = 44.dp).semantics { selected = active },
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (active) colors.primaryContainer else colors.background,
                    contentColor = if (active) colors.onPrimaryContainer else colors.onSurfaceVariant
                )
            ) { Text(label, fontWeight = FontWeight.SemiBold) }
        }
    }
}
