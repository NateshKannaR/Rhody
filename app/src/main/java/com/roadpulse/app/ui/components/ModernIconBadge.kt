package com.roadpulse.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.roadpulse.app.ui.theme.ElectricBlue
import com.roadpulse.app.ui.theme.NeonCyan
import com.roadpulse.app.ui.theme.RacingRed

enum class BadgeShape {
    Squircle,
    Circle
}

/**
 * Modern High-Performance Glassmorphic Icon Badge for Rhody Motorsport Telemetry.
 * Features gradient hairline border, subtle translucent tinted glass, and optional pulse aura.
 */
@Composable
fun ModernIconBadge(
    icon: ImageVector,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    accentColor: Color = NeonCyan,
    secondaryColor: Color = ElectricBlue,
    shape: BadgeShape = BadgeShape.Squircle,
    hasGlowBorder: Boolean = true,
    isPulsing: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val cornerRadius = when (shape) {
        BadgeShape.Squircle -> (size * 0.32f)
        BadgeShape.Circle -> (size / 2f)
    }
    val badgeShape = RoundedCornerShape(cornerRadius)

    val infiniteTransition = rememberInfiniteTransition(label = "pulseBadge")
    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val backgroundBrush = Brush.linearGradient(
        listOf(
            accentColor.copy(alpha = if (isPulsing) pulseGlowAlpha * 0.25f else 0.16f),
            secondaryColor.copy(alpha = 0.05f)
        )
    )

    val borderBrush = if (hasGlowBorder) {
        Brush.linearGradient(
            listOf(
                accentColor.copy(alpha = if (isPulsing) pulseGlowAlpha else 0.55f),
                secondaryColor.copy(alpha = 0.20f),
                Color.Transparent
            )
        )
    } else {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent))
    }

    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .then(clickModifier)
            .clip(badgeShape)
            .background(backgroundBrush)
            .border(width = 1.dp, brush = borderBrush, shape = badgeShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = accentColor,
            modifier = Modifier.size(iconSize)
        )

        if (isPulsing) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-3).dp, y = 3.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
        }
    }
}
