package com.roadpulse.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadpulse.app.ui.theme.OmniGreen
import com.roadpulse.app.ui.theme.GalvanCyan
import com.roadpulse.app.ui.theme.CyberDark
import com.roadpulse.app.ui.theme.CyberBorder

/**
 * Segmented Telemetry Bar (Inspired by ben10bynk.vercel.app stat__cells)
 * Displays 10 angled telemetry cells that illuminate dynamically with high-tech glow.
 */
@Composable
fun SegmentedTelemetryBar(
    activeSegments: Int,
    totalSegments: Int = 10,
    activeColor: Color = OmniGreen,
    inactiveColor: Color = activeColor.copy(alpha = 0.15f),
    modifier: Modifier = Modifier,
    height: Dp = 8.dp
) {
    val clampedActive = activeSegments.coerceIn(0, totalSegments)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalSegments) {
            val isActive = i < clampedActive
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(if (isActive) activeColor else inactiveColor)
            )
        }
    }
}

/**
 * High-Tech DNA & Telemetry Matrix Badge (Inspired by ben10bynk holo-dna-code)
 */
@Composable
fun HoloDnaBadge(
    code: String,
    modifier: Modifier = Modifier,
    accentColor: Color = OmniGreen
) {
    val infiniteTransition = rememberInfiniteTransition(label = "HoloPulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(CyberDark.copy(alpha = 0.85f))
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .alpha(alphaAnim)
                .background(accentColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = code,
            color = accentColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.2.sp
        )
    }
}

/**
 * High-Tech Cockpit Status Pill (Inspired by ben10bynk badge-pill)
 */
@Composable
fun HoloStatusPill(
    text: String,
    modifier: Modifier = Modifier,
    accentColor: Color = OmniGreen
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(accentColor.copy(alpha = 0.08f))
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(),
            color = accentColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.2.sp
        )
    }
}
