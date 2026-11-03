package com.roadpulse.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import kotlin.math.roundToInt

@Composable
fun SprintDynoCard(
    currentGpsSpeedKmh: Float = 0f,
    best0to60Sec: Float? = null,
    best0to100Sec: Float? = null,
    modifier: Modifier = Modifier
) {
    val isStationary = currentGpsSpeedKmh < 2.0f
    val hasSplit100 = best0to100Sec != null
    val hasSplit60 = best0to60Sec != null

    val (badgeText, badgeColor) = when {
        hasSplit100 -> "BEST: ${String.format(Locale.US, "%.2fs", best0to100Sec)}" to HyperCyan
        hasSplit60 -> "0-60: ${String.format(Locale.US, "%.2fs", best0to60Sec)}" to HyperCyan
        isStationary -> "ARMED AT 0 KM/H • READY" to OptimalGreen
        else -> "ACCELERATING • LOGGING" to SportAmber
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightCard)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
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
                        .background(HyperCyan.copy(alpha = 0.15f))
                        .border(1.dp, HyperCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Dyno",
                        tint = HyperCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "0–100 KM/H SPRINT TELEMETRY",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Live GPS Standing Launch Detection",
                        color = TextSecondary,
                        fontSize = 9.sp
                    )
                }
            }

            // Real-time Status Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badgeText,
                    color = badgeColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3 Digital Readout Cards (0-60, 0-100, Current GPS Speed)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DynoReadoutBox(
                title = "0–60 KM/H",
                value = best0to60Sec?.let { String.format(Locale.US, "%.2f s", it) } ?: "-- s",
                subtext = if (hasSplit60) "Standing Split" else "Awaiting Launch",
                valueColor = if (hasSplit60) HyperCyan else TextMuted,
                modifier = Modifier.weight(1f)
            )
            DynoReadoutBox(
                title = "0–100 KM/H",
                value = best0to100Sec?.let { String.format(Locale.US, "%.2f s", it) } ?: "-- s",
                subtext = if (hasSplit100) "Full Sprint" else "Awaiting Launch",
                valueColor = if (hasSplit100) OptimalGreen else TextMuted,
                modifier = Modifier.weight(1f)
            )
            DynoReadoutBox(
                title = "LIVE SPEED",
                value = "${currentGpsSpeedKmh.roundToInt()}",
                subtext = "KM/H GPS",
                valueColor = TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dynamic Sprint Progress Bar (0 to 100 km/h)
        val speedProgress = (currentGpsSpeedKmh / 100f).coerceIn(0f, 1f)
        val animatedProgress by animateFloatAsState(
            targetValue = speedProgress,
            animationSpec = tween(80),
            label = "SprintProgressBar"
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Launch Target: 0 → 100 km/h",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${(speedProgress * 100).toInt()}% Target Speed",
                    color = HyperCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(CarbonDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(HyperCyan, OptimalGreen)
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Explanatory HUD Note (Zero Mock Guarantee)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CarbonDark)
                .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Text(
                text = "Autonomous GPS standing sprint benchmark. Come to a complete stop (0 km/h) to arm, then accelerate to full throttle to record precision 0–60 and 0–100 km/h times.",
                color = TextSecondary,
                fontSize = 8.5.sp,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
private fun DynoReadoutBox(
    title: String,
    value: String,
    subtext: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CarbonDark)
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = TextMuted,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtext,
            color = TextSecondary,
            fontSize = 7.5.sp
        )
    }
}
