package com.roadpulse.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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

private val LightColorScheme = lightColorScheme(
    primary = LightAccentBlue,
    onPrimary = LightSurface,
    primaryContainer = LightSurfaceElevated,
    onPrimaryContainer = LightAccentBlue,
    secondary = LightEmerald,
    onSecondary = LightSurface,
    background = LightCanvas,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightBorder,
    onSurfaceVariant = LightTextSecondary,
    error = RacingRed,
    onError = LightSurface
)

@Composable
fun RoadPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
