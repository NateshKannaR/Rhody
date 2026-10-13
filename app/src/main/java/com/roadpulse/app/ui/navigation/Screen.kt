package com.roadpulse.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Speed)
    object LiveTrip : Screen("live_trip", "Live Trip", Icons.Default.Map)
    object Analytics : Screen("analytics", "Analytics", Icons.Default.Analytics)
    object History : Screen("history", "History", Icons.Default.History)
    object Profile : Screen("profile", "Account", Icons.Default.Person)

    companion object {
        val bottomNavItems = listOf(Dashboard, LiveTrip, Analytics, History, Profile)
        const val TRIP_DETAIL_ROUTE = "trip_detail/{tripId}"

        fun createTripDetailRoute(tripId: Long): String = "trip_detail/$tripId"
    }
}
