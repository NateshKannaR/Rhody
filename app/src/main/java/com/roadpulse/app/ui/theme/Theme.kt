package com.roadpulse.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = CarbonDark,
    primaryContainer = CardSurfaceLight,
    onPrimaryContainer = NeonCyan,
    secondary = ElectricBlue,
    onSecondary = CarbonDark,
    background = CarbonDark,
    onBackground = TextPrimary,
    surface = MidnightCard,
    onSurface = TextPrimary,
    surfaceVariant = CardBorder,
    onSurfaceVariant = TextSecondary,
    error = RacingRed,
    onError = TextPrimary
)

@Composable
fun RoadPulseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
