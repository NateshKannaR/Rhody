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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadpulse.app.ui.theme.CarbonDark
import com.roadpulse.app.ui.theme.CardBorder
import com.roadpulse.app.ui.theme.HyperCyan
import com.roadpulse.app.ui.theme.MidnightCard
import com.roadpulse.app.ui.theme.OptimalGreen
import com.roadpulse.app.ui.theme.RacingRed
import com.roadpulse.app.ui.theme.SportAmber
import com.roadpulse.app.ui.theme.TextMuted
import com.roadpulse.app.ui.theme.TextPrimary
import com.roadpulse.app.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.max

@Composable
fun FrictionCircleCard(
    lateralG: Float = 0f,
    longitudinalG: Float = 0f,
    externalLeanDegrees: Float? = null,
    onResetMemory: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val purpleAccent = Color(0xFFA855F7)
    val purpleLight = Color(0xFFD8B4FE)

    // Calculate real physics lean angle theta = atan(lateralG) in degrees if not provided
    val currentLean = remember(lateralG, externalLeanDegrees) {
        externalLeanDegrees ?: Math.toDegrees(atan(lateralG.toDouble())).toFloat().coerceIn(-60f, 60f)
    }

    // Apex memory for max left and max right
    var maxLeftLean by remember { mutableFloatStateOf(0f) }
    var maxRightLean by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentLean) {
        if (currentLean < 0) {
            val leftMag = abs(currentLean)
            if (leftMag > maxLeftLean) maxLeftLean = leftMag
        } else if (currentLean > 0) {
            if (currentLean > maxRightLean) maxRightLean = currentLean
        }
    }

    val animatedLatG by animateFloatAsState(
        targetValue = lateralG.coerceIn(-1.5f, 1.5f),
        animationSpec = tween(90),
        label = "LatGAnimation"
    )

    val animatedLongG by animateFloatAsState(
        targetValue = longitudinalG.coerceIn(-1.5f, 1.5f),
        animationSpec = tween(90),
        label = "LongGAnimation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MidnightCard)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
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
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(purpleAccent.copy(alpha = 0.15f))
                        .border(1.dp, purpleAccent.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TwoWheeler,
                        contentDescription = "Lean Angle",
                        tint = purpleAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "LEAN ANGLE & FRICTION CIRCLE",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Live IMU Lateral & Longitudinal G-Forces",
                        color = TextSecondary,
                        fontSize = 9.sp
                    )
                }
            }

            // Current Roll Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(purpleAccent.copy(alpha = 0.2f))
                    .border(1.dp, purpleAccent.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${String.format(Locale.US, "%.1f", abs(currentLean))}° LEAN",
                    color = purpleLight,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Friction Circle Target Visualizer (Canvas with concentric rings and real-time dot)
        Box(
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.CenterHorizontally)
                .clip(CircleShape)
                .background(CarbonDark)
                .border(1.dp, CardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f

                // Outer boundary 1.5G ring
                drawCircle(
                    color = Color.White.copy(alpha = 0.12f),
                    radius = radius * 0.95f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )

                // 1.0G Ring (Purple boundary)
                drawCircle(
                    color = purpleAccent.copy(alpha = 0.35f),
                    radius = radius * 0.65f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )

                // 0.5G Ring (Inner dashed circle)
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = radius * 0.35f,
                    center = center,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                )

                // Crosshairs
                drawLine(
                    color = Color.White.copy(alpha = 0.15f),
                    start = Offset(0f, center.y),
                    end = Offset(size.width, center.y),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.15f),
                    start = Offset(center.x, 0f),
                    end = Offset(center.x, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Real-Time Dynamic IMU Friction Dot
            // Map lateralG (-1.2 to +1.2) -> X offset (-50.dp to +50.dp)
            // Map longitudinalG (-1.2 to +1.2) -> Y offset (+50.dp to -50.dp)
            val maxOffsetDp = 48f
            val dotOffsetX = (animatedLatG / 1.2f).coerceIn(-1f, 1f) * maxOffsetDp
            val dotOffsetY = -(animatedLongG / 1.2f).coerceIn(-1f, 1f) * maxOffsetDp

            Box(
                modifier = Modifier
                    .offset(x = dotOffsetX.dp, y = dotOffsetY.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(HyperCyan)
                    .border(2.dp, Color.White, CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Apex Lean Memory Strip (Left vs Right Max)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CarbonDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LEFT APEX MAX",
                            color = TextMuted,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (maxLeftLean > 0f) "${String.format(Locale.US, "%.1f", maxLeftLean)}°" else "--°",
                            color = HyperCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "← APEX",
                        color = HyperCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CarbonDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "RIGHT APEX MAX",
                            color = TextMuted,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (maxRightLean > 0f) "${String.format(Locale.US, "%.1f", maxRightLean)}°" else "--°",
                            color = purpleLight,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "APEX →",
                        color = purpleAccent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Reset Apex Memory Button
        OutlinedButton(
            onClick = {
                maxLeftLean = 0f
                maxRightLean = 0f
                onResetMemory?.invoke()
            },
            modifier = Modifier.fillMaxWidth().height(36.dp),
            shape = RoundedCornerShape(10.dp),
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White.copy(alpha = 0.04f),
                contentColor = TextSecondary
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reset Apex",
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "Reset Apex Memory",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
