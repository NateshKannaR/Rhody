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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadpulse.app.ui.theme.BezelTrack
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
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun SpeedometerGauge(
    currentSpeedKmh: Float,
    maxSpeedKmh: Float,
    maxScaleKmh: Float = 180f,
    modifier: Modifier = Modifier
) {
    val animatedSpeed by animateFloatAsState(
        targetValue = currentSpeedKmh.coerceIn(0f, maxScaleKmh),
        animationSpec = tween(durationMillis = 300),
        label = "SpeedGaugeAnimation"
    )

    val animatedMaxSpeed by animateFloatAsState(
        targetValue = maxSpeedKmh.coerceIn(0f, maxScaleKmh),
        animationSpec = tween(durationMillis = 350),
        label = "MaxSpeedGaugeAnimation"
    )

    val speedFraction = (animatedSpeed / maxScaleKmh).coerceIn(0f, 1f)
    val maxFraction = (animatedMaxSpeed / maxScaleKmh).coerceIn(0f, 1f)

    // Dynamic color transition based on velocity
    val speedColor = when {
        animatedSpeed > 100f -> RacingRed
        animatedSpeed > 60f -> SportAmber
        animatedSpeed > 30f -> ElectricBlue
        else -> NeonCyan
    }

    Box(
        modifier = modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val glowWidth = 22.dp.toPx()
            val diameter = size.minDimension - strokeWidth * 2 - 20.dp.toPx()
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)

            val startAngle = 135f
            val totalSweep = 270f
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val radius = diameter / 2f

            // 1. Subtle Outer Segment Ticks (Every 10 and 20 km/h)
            val totalTicks = 18
            for (i in 0..totalTicks) {
                val tickFraction = i.toFloat() / totalTicks
                val angleDeg = startAngle + totalSweep * tickFraction
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val isMajor = i % 2 == 0
                val tickInnerR = radius + 10.dp.toPx()
                val tickOuterR = radius + (if (isMajor) 18.dp.toPx() else 14.dp.toPx())

                val startX = (centerX + tickInnerR * cos(angleRad)).toFloat()
                val startY = (centerY + tickInnerR * sin(angleRad)).toFloat()
                val endX = (centerX + tickOuterR * cos(angleRad)).toFloat()
                val endY = (centerY + tickOuterR * sin(angleRad)).toFloat()

                val tickColor = when {
                    tickFraction <= speedFraction -> speedColor.copy(alpha = if (isMajor) 0.9f else 0.5f)
                    isMajor -> CardBorder.copy(alpha = 0.9f)
                    else -> CardBorder.copy(alpha = 0.4f)
                }

                drawLine(
                    color = tickColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = (if (isMajor) 2.dp else 1.dp).toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 2. Background Track Arc
            drawArc(
                color = BezelTrack,
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 3. Ambient Glow Arc (Underneath Active Sweep)
            if (speedFraction > 0.01f) {
                val activeSweep = totalSweep * speedFraction
                drawArc(
                    brush = Brush.sweepGradient(
                        0.375f to NeonCyan.copy(alpha = 0.25f),
                        0.75f to SportAmber.copy(alpha = 0.25f),
                        1.0f to RacingRed.copy(alpha = 0.25f)
                    ),
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = Offset(topLeft.x - 4.dp.toPx(), topLeft.y - 4.dp.toPx()),
                    size = Size(diameter + 8.dp.toPx(), diameter + 8.dp.toPx()),
                    style = Stroke(width = glowWidth, cap = StrokeCap.Round)
                )

                // 4. Solid Foreground Active Sweep Arc
                drawArc(
                    brush = Brush.sweepGradient(
                        0.375f to NeonCyan,
                        0.70f to ElectricBlue,
                        0.85f to SportAmber,
                        1.0f to RacingRed
                    ),
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // 5. Active Leading Needle Head Dot
                val headAngleRad = Math.toRadians((startAngle + activeSweep).toDouble())
                val headX = (centerX + radius * cos(headAngleRad)).toFloat()
                val headY = (centerY + radius * sin(headAngleRad)).toFloat()

                drawCircle(
                    color = Color.White,
                    radius = (strokeWidth / 2f) - 2.dp.toPx(),
                    center = Offset(headX, headY)
                )
            }

            // 6. Max Peak Speed Marker
            if (maxFraction > 0.02f) {
                val maxAngle = Math.toRadians((startAngle + totalSweep * maxFraction).toDouble())
                val innerR = radius - strokeWidth / 2f - 3.dp.toPx()
                val outerR = radius + strokeWidth / 2f + 3.dp.toPx()

                val startX = (centerX + innerR * cos(maxAngle)).toFloat()
                val startY = (centerY + innerR * sin(maxAngle)).toFloat()
                val endX = (centerX + outerR * cos(maxAngle)).toFloat()
                val endY = (centerY + outerR * sin(maxAngle)).toFloat()

                drawLine(
                    color = RacingRed,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Center Precision Digital Readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${animatedSpeed.roundToInt()}",
                color = TextPrimary,
                fontSize = 62.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-2).sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(speedColor)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "KM/H",
                    color = speedColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Peak Speed Pill Callout
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MidnightCard)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PEAK",
                    color = RacingRed,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${maxSpeedKmh.roundToInt()} KM/H",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
