package com.roadpulse.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadpulse.app.domain.model.GForceData
import com.roadpulse.app.ui.theme.CarbonDark
import com.roadpulse.app.ui.theme.CardBorder
import com.roadpulse.app.ui.theme.ElectricBlue
import com.roadpulse.app.ui.theme.MidnightCard
import com.roadpulse.app.ui.theme.NeonCyan
import com.roadpulse.app.ui.theme.RacingRed
import com.roadpulse.app.ui.theme.TextMuted
import com.roadpulse.app.ui.theme.TextPrimary
import com.roadpulse.app.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.abs

@Composable
fun GForceMeter(
    gForce: GForceData,
    modifier: Modifier = Modifier
) {
    val animLat by animateFloatAsState(
        targetValue = gForce.lateralG.coerceIn(-1.5f, 1.5f),
        animationSpec = tween(120),
        label = "LatG"
    )
    val animLong by animateFloatAsState(
        targetValue = gForce.longitudinalG.coerceIn(-1.5f, 1.5f),
        animationSpec = tween(120),
        label = "LongG"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MidnightCard)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(NeonCyan)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "G-FORCE RADAR",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(RacingRed.copy(alpha = 0.12f))
                    .border(1.dp, RacingRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "PEAK ${String.format(Locale.US, "%.2f", gForce.maxG)}G",
                    color = RacingRed,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Center Radar and Value Pill Columns
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // Lateral Indicator Pill
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "CORNERING",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${if (animLat >= 0) "▶" else "◀"} ${String.format(Locale.US, "%.2f", abs(animLat))}G",
                    color = if (abs(animLat) > 0.8f) RacingRed else NeonCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Radar Circle Canvas
            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(110.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension / 2f

                    // Concentric Laser Rings (0.5G, 1.0G, 1.5G)
                    drawCircle(color = CardBorder.copy(alpha = 0.5f), radius = radius * 0.33f, center = center, style = Stroke(1.dp.toPx()))
                    drawCircle(color = CardBorder.copy(alpha = 0.8f), radius = radius * 0.66f, center = center, style = Stroke(1.dp.toPx()))
                    drawCircle(color = CardBorder, radius = radius, center = center, style = Stroke(1.5.dp.toPx()))

                    // Crosshair Axes
                    drawLine(color = CardBorder.copy(alpha = 0.6f), start = Offset(center.x, 0f), end = Offset(center.x, size.height), strokeWidth = 1.dp.toPx())
                    drawLine(color = CardBorder.copy(alpha = 0.6f), start = Offset(0f, center.y), end = Offset(size.width, center.y), strokeWidth = 1.dp.toPx())

                    // Dynamic G-Force Position Dot
                    val dotX = center.x + (animLat / 1.5f) * radius
                    val dotY = center.y - (animLong / 1.5f) * radius

                    // Soft ambient glow
                    drawCircle(color = NeonCyan.copy(alpha = 0.25f), radius = 12.dp.toPx(), center = Offset(dotX, dotY))
                    // Sharp core
                    drawCircle(color = NeonCyan, radius = 5.dp.toPx(), center = Offset(dotX, dotY))
                    drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(dotX, dotY))
                }
            }

            // Longitudinal Indicator Pill
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (animLong >= 0) "ACCEL" else "BRAKE",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${if (animLong >= 0) "▲" else "▼"} ${String.format(Locale.US, "%.2f", abs(animLong))}G",
                    color = if (animLong < -0.6f) RacingRed else ElectricBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
