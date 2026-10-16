package com.roadpulse.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.roadpulse.app.RoadPulseApp
import com.roadpulse.app.domain.calculator.TripCalculator
import com.roadpulse.app.domain.model.TripMetrics
import com.roadpulse.app.domain.model.TripStatus
import com.roadpulse.app.service.TripRecordingController
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.roadpulse.app.data.auth.AuthManager
import com.roadpulse.app.ui.screens.auth.AuthScreen
import com.roadpulse.app.ui.navigation.BottomBar
import com.roadpulse.app.ui.navigation.Screen
import com.roadpulse.app.ui.screens.analytics.AnalyticsScreen
import com.roadpulse.app.ui.screens.dashboard.DashboardScreen
import com.roadpulse.app.ui.screens.history.HistoryScreen
import com.roadpulse.app.ui.screens.history.TripDetailScreen
import com.roadpulse.app.ui.screens.livetrip.LiveTripScreen
import com.roadpulse.app.ui.screens.profile.ProfileScreen
import com.roadpulse.app.ui.theme.CarbonDark
import com.roadpulse.app.ui.theme.RoadPulseTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check for unfinished/paused trip on launch to recover safely
        val repository = (application as RoadPulseApp).container.tripRepository
        lifecycleScope.launch {
            val activeTrip = repository.getActiveOrPausedTrip()
            if (activeTrip != null) {
                val status = try {
                    TripStatus.valueOf(activeTrip.status)
                } catch (e: Exception) {
                    TripStatus.PAUSED
                }

                val points = repository.getPoints(activeTrip.id)
                TripRecordingController.setLivePoints(points)

                val recoveredMetrics = TripMetrics(
                    currentSpeedKmh = 0f,
                    maxSpeedKmh = activeTrip.maxSpeedKmh,
                    avgMovingSpeedKmh = activeTrip.avgMovingSpeedKmh,
                    avgOverallSpeedKmh = activeTrip.avgOverallSpeedKmh,
                    totalDistanceMeters = activeTrip.totalDistanceMeters,
                    elapsedTimeMs = activeTrip.totalDurationMs,
                    movingDurationMs = activeTrip.movingDurationMs,
                    pauseDurationMs = activeTrip.pauseDurationMs,
                    activeTripId = activeTrip.id,
                    tripStatus = status,
                    recordedPointsCount = points.size,
                    currentLatitude = points.lastOrNull()?.latitude,
                    currentLongitude = points.lastOrNull()?.longitude
                )
                TripRecordingController.updateMetrics(recoveredMetrics)
            }
        }

        setContent {
            RoadPulseTheme {
                val authState by AuthManager.authState.collectAsState()
                var guestModeActive by remember { mutableStateOf(false) }

                if (!authState.isLoggedIn && !guestModeActive) {
                    AuthScreen(
                        onAuthenticated = {
                            guestModeActive = true
                        }
                    )
                } else {
                    val navController = rememberNavController()
                    val metrics by TripRecordingController.metrics.collectAsState()

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = CarbonDark,
                        bottomBar = {
                            BottomBar(
                                navController = navController,
                                isRecording = metrics.isRecording
                            )
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Dashboard.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.Dashboard.route) {
                                DashboardScreen(
                                    onNavigateToLiveTrip = {
                                        navController.navigate(Screen.Trips.route)
                                    },
                                    onNavigateToProfile = {
                                        navController.navigate(Screen.Settings.route)
                                    }
                                )
                            }

                            composable(Screen.Trips.route) {
                                LiveTripScreen(
                                    onFinishTrip = { tripId ->
                                        navController.navigate(Screen.createTripDetailRoute(tripId))
                                    }
                                )
                            }

                            composable(Screen.LiveTrip.route) {
                                LiveTripScreen(
                                    onFinishTrip = { tripId ->
                                        navController.navigate(Screen.createTripDetailRoute(tripId))
                                    }
                                )
                            }

                            composable(Screen.Analysis.route) {
                                AnalyticsScreen()
                            }

                            composable("analytics") {
                                AnalyticsScreen()
                            }

                            composable(Screen.History.route) {
                                HistoryScreen(
                                    onNavigateToTripDetail = { tripId ->
                                        navController.navigate(Screen.createTripDetailRoute(tripId))
                                    }
                                )
                            }

                            composable(Screen.Settings.route) {
                                ProfileScreen()
                            }

                            composable(Screen.Profile.route) {
                                ProfileScreen()
                            }

                            composable(
                                route = Screen.TRIP_DETAIL_ROUTE,
                                arguments = listOf(navArgument("tripId") { type = NavType.LongType })
                            ) { backStackEntry ->
                                val tripId = backStackEntry.arguments?.getLong("tripId") ?: 0L
                                TripDetailScreen(
                                    tripId = tripId,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
