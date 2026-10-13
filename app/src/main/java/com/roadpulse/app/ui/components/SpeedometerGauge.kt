package com.roadpulse.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadpulse.app.ui.theme.CarbonDark
import com.roadpulse.app.ui.theme.CardBorder
import com.roadpulse.app.ui.theme.ElectricBlue
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
        animationSpec = tween(durationMillis = 350),
        label = "SpeedGaugeAnimation"
    )

    val animatedMaxSpeed by animateFloatAsState(
        targetValue = maxSpeedKmh.coerceIn(0f, maxScaleKmh),
        animationSpec = tween(durationMillis = 400),
        label = "MaxSpeedGaugeAnimation"
    )

    val speedFraction = (animatedSpeed / maxScaleKmh).coerceIn(0f, 1f)
    val maxFraction = (animatedMaxSpeed / maxScaleKmh).coerceIn(0f, 1f)

    // Dynamic color based on current speed
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
            val strokeWidth = 18.dp.toPx()
            val diameter = size.minDimension - strokeWidth * 2
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)

            val startAngle = 135f
            val totalSweep = 270f

            // 1. Background Arc Track
            drawArc(
                color = CardBorder,
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 2. Active Speed Sweep
            if (speedFraction > 0.005f) {
                val activeSweep = totalSweep * speedFraction
                drawArc(
                    brush = Brush.sweepGradient(
                        0.375f to NeonCyan,
                        0.75f to SportAmber,
                        1.0f to RacingRed
                    ),
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // 3. Max Speed Marker Tick
            if (maxFraction > 0.01f) {
                val maxAngle = Math.toRadians((startAngle + totalSweep * maxFraction).toDouble())
                val radius = diameter / 2f
                val centerX = size.width / 2f
                val centerY = size.height / 2f

                val innerR = radius - strokeWidth / 2f
                val outerR = radius + strokeWidth / 2f

                val startX = (centerX + innerR * cos(maxAngle)).toFloat()
                val startY = (centerY + innerR * sin(maxAngle)).toFloat()
                val endX = (centerX + outerR * cos(maxAngle)).toFloat()
                val endY = (centerY + outerR * sin(maxAngle)).toFloat()

                drawLine(
                    color = RacingRed,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Center Digital Readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${animatedSpeed.roundToInt()}",
                color = speedColor,
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.5).sp
            )
            Text(
                text = "KM / H",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "MAX: ${maxSpeedKmh.roundToInt()} KM/H",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
