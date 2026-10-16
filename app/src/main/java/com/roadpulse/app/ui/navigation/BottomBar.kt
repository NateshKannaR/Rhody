package com.roadpulse.app.ui.navigation

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.roadpulse.app.ui.theme.CardBorder
import com.roadpulse.app.ui.theme.CardBorderGlow
import com.roadpulse.app.ui.theme.HyperCyan
import com.roadpulse.app.ui.theme.MidnightCard
import com.roadpulse.app.ui.theme.NeonCyan
import com.roadpulse.app.ui.theme.ObsidianGlass
import com.roadpulse.app.ui.theme.RacingRed
import com.roadpulse.app.ui.theme.TextMuted

@Composable
fun BottomBar(
    navController: NavController,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    val infiniteTransition = rememberInfiniteTransition(label = "pulseRec")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recDotPulse"
    )

    NavigationBar(
        modifier = modifier
            .background(ObsidianGlass)
            .border(width = 1.dp, color = CardBorder),
        containerColor = ObsidianGlass,
        tonalElevation = 8.dp
    ) {
        Screen.bottomNavItems.forEach { screen ->
            val isSelected = when (screen) {
                Screen.Dashboard -> currentRoute == Screen.Dashboard.route
                Screen.Trips -> currentRoute == Screen.Trips.route || currentRoute == Screen.LiveTrip.route
                Screen.Analysis -> currentRoute == Screen.Analysis.route || currentRoute == "analytics"
                Screen.Settings -> currentRoute == Screen.Settings.route || currentRoute == Screen.Profile.route
                else -> currentRoute == screen.route
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    val targetRoute = screen.route
                    if (currentRoute != targetRoute) {
                        navController.navigate(targetRoute) {
                            popUpTo(Screen.Dashboard.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Box {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.title,
                            modifier = Modifier.size(24.dp)
                        )
                        // Active recording glowing red dot badge on Trips tab
                        if ((screen == Screen.Trips || screen == Screen.LiveTrip) && isRecording) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 3.dp, y = (-3).dp)
                                    .clip(CircleShape)
                                    .background(RacingRed.copy(alpha = pulseAlpha))
                            )
                        }
                    }
                },
                label = {
                    Text(
                        text = screen.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NeonCyan,
                    selectedTextColor = NeonCyan,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted,
                    indicatorColor = NeonCyan.copy(alpha = 0.14f)
                )
            )
        }
    }
}
