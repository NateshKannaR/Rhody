package com.roadpulse.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.ui.graphics.vector.ImageVector

import com.roadpulse.app.ui.theme.RhodyIcons

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    // Primary 4 Navigation Bar Tabs
    object Dashboard : Screen("dashboard", "Dashboard", RhodyIcons.Cockpit)
    object Trips : Screen("trips", "Trips", RhodyIcons.RaceCircuit)
    object Analysis : Screen("analysis", "Analysis", RhodyIcons.TelemetryWave)
    object Settings : Screen("settings", "Settings", RhodyIcons.PilotHelmet)

    // Legacy and Sub-route references
    object LiveTrip : Screen("live_trip", "Trips", RhodyIcons.RaceCircuit)
    object Profile : Screen("profile", "Settings", RhodyIcons.PilotHelmet)
    object History : Screen("history", "History", Icons.Default.History)

    companion object {
        val bottomNavItems = listOf(Dashboard, Trips, Analysis, Settings)
        const val TRIP_DETAIL_ROUTE = "trip_detail/{tripId}"

        fun createTripDetailRoute(tripId: Long): String = "trip_detail/$tripId"
    }
}
