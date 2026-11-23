package com.roadpulse.app.ui.screens.parent

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.roadpulse.app.data.auth.AuthManager
import com.roadpulse.app.data.auth.CloudHttpException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun ParentPanelScreen(onLogout: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dashboard by remember { mutableStateOf<JSONObject?>(null) }
    var pairingEmail by remember { mutableStateOf("") }
    var updating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        AuthManager.cloudRequest("/api/parent/rider-status")
            .onSuccess {
                dashboard = it
                loadError = null
            }.onFailure {
                dashboard = null
                loadError = it.message
                if ((it as? CloudHttpException)?.status == 401) onLogout()
            }
    }

    LaunchedEffect(Unit) {
        while (true) {
            refresh()
            delay(5000)
        }
    }

    val state = dashboard
    val rider = state?.optJSONObject("rider")
    val authorized = state?.optBoolean("authorized") == true
    Box(modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 660.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Guardian dashboard", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = onLogout) { Text("Sign out") }
            }
            if (state == null) {
                if (loadError == null) CircularProgressIndicator()
                else Text(loadError.orEmpty(), color = MaterialTheme.colorScheme.error)
            }

            if (state != null) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            rider?.optString("name")?.ifBlank { null } ?: rider?.optString("email")?.ifBlank { null } ?: "Connect a rider",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(when (state.optString("status")) {
                            "approved" -> "Access approved"
                            "pending_approval" -> "Waiting for rider approval"
                            "rejected" -> "The rider declined the request"
                            "revoked" -> "The rider removed access"
                            else -> "Enter your rider’s registered email below"
                        })
                        if (authorized) {
                            rider?.optString("bikeModel")?.takeIf { it.isNotBlank() }?.let { Text(it) }
                        }
                    }
                }
            }

            if (authorized && state != null) {
                val beacon = state.optJSONObject("beacon")
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Live ride", style = MaterialTheme.typography.titleMedium)
                        if (beacon == null) {
                            Text("There is no current ride update.")
                        } else {
                            Text("${beacon.optDouble("currentSpeedKmh", 0.0).toInt()} km/h")
                            val latitude = beacon.optDouble("latitude")
                            val longitude = beacon.optDouble("longitude")
                            if (latitude.isFinite() && longitude.isFinite()) {
                                TextButton(onClick = {
                                    val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    } catch (_: ActivityNotFoundException) {
                                        error = "Install a maps app to open this location."
                                    }
                                }) { Text("Open location") }
                            }
                        }
                    }
                }
                state.optJSONArray("trips")?.takeIf { it.length() > 0 }?.let { trips ->
                    Text("Recent rides", style = MaterialTheme.typography.titleMedium)
                    repeat(trips.length()) { index ->
                        val trip = trips.optJSONObject(index)
                        if (trip != null) {
                            val distance = trip.optDouble("distance", trip.optDouble("totalDistanceMeters", 0.0) / 1000.0)
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp)) {
                                    Text(trip.optString("title", "Ride"))
                                    Text(String.format(java.util.Locale.getDefault(), "%.1f km", distance))
                                }
                            }
                        }
                    }
                }
                state.optJSONArray("notifications")?.takeIf { it.length() > 0 }?.let { alerts ->
                    Text("Safety alerts", style = MaterialTheme.typography.titleMedium)
                    repeat(alerts.length()) { index ->
                        val alert = alerts.optJSONObject(index)
                        if (alert != null) Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp)) {
                                Text(alert.optString("title"), style = MaterialTheme.typography.titleSmall)
                                Text(alert.optString("message"))
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = pairingEmail, onValueChange = { pairingEmail = it.take(254) },
                modifier = Modifier.fillMaxWidth(), label = { Text("Rider email") },
                singleLine = true, enabled = !updating,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Button(
                onClick = {
                    updating = true
                    error = null
                    scope.launch {
                        AuthManager.cloudRequest(
                            "/api/parent/connect-rider", "POST",
                            JSONObject().put("riderCodeOrEmail", pairingEmail.trim())
                        ).onSuccess {
                            pairingEmail = ""
                            refresh()
                        }.onFailure { error = it.message }
                        updating = false
                    }
                },
                enabled = !updating && pairingEmail.isNotBlank()
            ) { Text(if (updating) "Sending request…" else "Request access") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
