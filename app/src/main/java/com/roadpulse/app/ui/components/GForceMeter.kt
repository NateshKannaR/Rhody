package com.roadpulse.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.roadpulse.app.ui.theme.SportAmber
import com.roadpulse.app.ui.theme.TextMuted
import com.roadpulse.app.ui.theme.TextPrimary
import com.roadpulse.app.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun GForceMeter(
    gForce: GForceData,
    modifier: Modifier = Modifier
) {
    val animLat by animateFloatAsState(
        targetValue = gForce.lateralG.coerceIn(-1.5f, 1.5f),
        animationSpec = tween(150),
        label = "LatG"
    )
    val animLong by animateFloatAsState(
        targetValue = gForce.longitudinalG.coerceIn(-1.5f, 1.5f),
        animationSpec = tween(150),
        label = "LongG"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightCard)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "G-FORCE TELEMETRY",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "LAT: ${String.format(Locale.US, "%.2f", animLat)}G • LONG: ${String.format(Locale.US, "%.2f", animLong)}G",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "PEAK ${String.format(Locale.US, "%.2f", gForce.maxG)}G",
                color = RacingRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(120.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f

                // Outer and inner concentric rings (0.5G, 1.0G, 1.5G)
                drawCircle(color = CardBorder, radius = radius * 0.33f, center = center, style = Stroke(1.dp.toPx()))
                drawCircle(color = CardBorder, radius = radius * 0.66f, center = center, style = Stroke(1.dp.toPx()))
                drawCircle(color = CardBorder, radius = radius, center = center, style = Stroke(1.5.dp.toPx()))

                // Crosshairs
                drawLine(color = CardBorder, start = Offset(center.x, 0f), end = Offset(center.x, size.height), strokeWidth = 1.dp.toPx())
                drawLine(color = CardBorder, start = Offset(0f, center.y), end = Offset(size.width, center.y), strokeWidth = 1.dp.toPx())

                // Dynamic G-Force Position Dot
                // Map [-1.5G, 1.5G] to [-radius, radius]
                val dotX = center.x + (animLat / 1.5f) * radius
                val dotY = center.y - (animLong / 1.5f) * radius

                drawCircle(color = NeonCyan.copy(alpha = 0.3f), radius = 10.dp.toPx(), center = Offset(dotX, dotY))
                drawCircle(color = NeonCyan, radius = 5.dp.toPx(), center = Offset(dotX, dotY))
                drawCircle(color = CarbonDark, radius = 2.dp.toPx(), center = Offset(dotX, dotY))
            }
        }
    }
}
