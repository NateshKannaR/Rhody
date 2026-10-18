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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.min
import kotlin.math.pow

enum class DynoState {
    IDLE,
    STAGING_STAGE,
    STAGING_Y1,
    STAGING_Y2,
    RUNNING,
    FINISHED
}

@Composable
fun SprintDynoCard(
    currentGpsSpeedKmh: Float = 0f,
    best0to60Sec: Float? = null,
    best0to100Sec: Float? = null,
    onDynoSprintPoint: ((lateralG: Float, longitudinalG: Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    var dynoState by remember { mutableStateOf(DynoState.IDLE) }
    var dynoSpeed by remember { mutableFloatStateOf(0f) }
    var dynoElapsedSec by remember { mutableFloatStateOf(0f) }
    var dyno0to60 by remember { mutableStateOf<Float?>(best0to60Sec) }
    var dyno0to100 by remember { mutableStateOf<Float?>(best0to100Sec) }
    var dynoTrapSpeed by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(best0to60Sec, best0to100Sec) {
        if (dynoState == DynoState.IDLE) {
            if (best0to60Sec != null) dyno0to60 = best0to60Sec
            if (best0to100Sec != null) dyno0to100 = best0to100Sec
        }
    }

    val runDynoTest: () -> Unit = {
        if (dynoState == DynoState.IDLE || dynoState == DynoState.FINISHED) {
            coroutineScope.launch {
                dynoSpeed = 0f
                dynoElapsedSec = 0f
                dynoTrapSpeed = 0f
                dyno0to60 = null
                dyno0to100 = null

                // F1 Drag Tree Sequence
                dynoState = DynoState.STAGING_STAGE
                delay(400)
                dynoState = DynoState.STAGING_Y1
                delay(400)
                dynoState = DynoState.STAGING_Y2
                delay(400)
                dynoState = DynoState.RUNNING

                val startTime = System.currentTimeMillis()
                var recorded60 = false
                var recorded100 = false

                while (dynoState == DynoState.RUNNING) {
                    val now = System.currentTimeMillis()
                    val elapsed = (now - startTime) / 1000f
                    dynoElapsedSec = elapsed

                    // Non-linear realistic motorcycle power curve matching web engine:
                    // speed = min(124, (elapsedSec / 6.2)^0.72 * 100)
                    val speed = min(124f, ((elapsed / 6.2f).coerceAtLeast(0f).pow(0.72f)) * 100f)
                    dynoSpeed = speed

                    val simLat = 0.12f
                    val simLong = min(1.0f, 0.35f + (speed / 120f) * 0.55f)
                    onDynoSprintPoint?.invoke(simLat, simLong)

                    if (speed >= 60f && !recorded60) {
                        recorded60 = true
                        dyno0to60 = elapsed
                    }

                    if (speed >= 100f && !recorded100) {
                        recorded100 = true
                        dyno0to100 = elapsed
                        dynoTrapSpeed = speed * 1.08f
                        dynoState = DynoState.FINISHED
                        break
                    }

                    delay(40)
                }
            }
        }
    }

    val resetDyno: () -> Unit = {
        dynoState = DynoState.IDLE
        dynoSpeed = 0f
        dynoElapsedSec = 0f
        dyno0to60 = null
        dyno0to100 = null
        dynoTrapSpeed = 0f
        onDynoSprintPoint?.invoke(0f, 0f)
    }

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
                        text = "0–100 KM/H SPRINT DYNO",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Precision IMU & GPS Acceleration Splits",
                        color = TextSecondary,
                        fontSize = 9.sp
                    )
                }
            }

            // Status Badge
            val (badgeText, badgeBg, badgeBorder, badgeFg) = when (dynoState) {
                DynoState.IDLE -> Quad("STAGE READY", Color(0xFF1E293B), CardBorder, TextSecondary)
                DynoState.STAGING_STAGE, DynoState.STAGING_Y1, DynoState.STAGING_Y2 ->
                    Quad("STAGING", SportAmber.copy(alpha = 0.2f), SportAmber.copy(alpha = 0.5f), SportAmber)
                DynoState.RUNNING ->
                    Quad("RUNNING", OptimalGreen.copy(alpha = 0.2f), OptimalGreen.copy(alpha = 0.5f), OptimalGreen)
                DynoState.FINISHED -> {
                    val passSec = dyno0to100?.let { String.format(Locale.US, "%.2fs", it) } ?: "PASS"
                    Quad("PASS: $passSec", HyperCyan.copy(alpha = 0.2f), HyperCyan.copy(alpha = 0.5f), HyperCyan)
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeBg)
                    .border(1.dp, badgeBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badgeText,
                    color = badgeFg,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // F1 Drag Tree Launch Lights
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CarbonDark)
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                .padding(vertical = 10.dp, horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "LAUNCH TREE:",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )

                val isStageOn = dynoState in listOf(DynoState.STAGING_STAGE, DynoState.STAGING_Y1, DynoState.STAGING_Y2, DynoState.RUNNING, DynoState.FINISHED)
                TreeLight(
                    active = isStageOn,
                    activeColor = SportAmber,
                    glowColor = SportAmber.copy(alpha = 0.5f)
                )

                val isY1On = dynoState in listOf(DynoState.STAGING_Y1, DynoState.STAGING_Y2, DynoState.RUNNING, DynoState.FINISHED)
                TreeLight(
                    active = isY1On,
                    activeColor = SportAmber,
                    glowColor = SportAmber.copy(alpha = 0.5f)
                )

                val isY2On = dynoState in listOf(DynoState.STAGING_Y2, DynoState.RUNNING, DynoState.FINISHED)
                TreeLight(
                    active = isY2On,
                    activeColor = SportAmber,
                    glowColor = SportAmber.copy(alpha = 0.5f)
                )

                val isGreenOn = dynoState in listOf(DynoState.RUNNING, DynoState.FINISHED)
                TreeLight(
                    active = isGreenOn,
                    activeColor = OptimalGreen,
                    glowColor = OptimalGreen.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3 Digital Readout Cards (0-60, 0-100, Trap Speed)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DynoReadoutBox(
                title = "0–60 KM/H",
                value = dyno0to60?.let { String.format(Locale.US, "%.2f s", it) } ?: "-- s",
                subtext = "Quick Launch",
                valueColor = HyperCyan,
                modifier = Modifier.weight(1f)
            )
            DynoReadoutBox(
                title = "0–100 KM/H",
                value = dyno0to100?.let { String.format(Locale.US, "%.2f s", it) } ?: "-- s",
                subtext = "Peak Sprint",
                valueColor = SportAmber,
                modifier = Modifier.weight(1f)
            )
            DynoReadoutBox(
                title = "TRAP SPEED",
                value = if (dynoTrapSpeed > 0f) String.format(Locale.US, "%.1f", dynoTrapSpeed) else "--",
                subtext = "KM/H @ 400M",
                valueColor = RacingRed,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dynamic Dyno Live Run Speed Bar
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val displaySpeed = if (dynoState == DynoState.RUNNING || dynoState == DynoState.FINISHED) {
                dynoSpeed
            } else {
                currentGpsSpeedKmh
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Current Sprint: ${String.format(Locale.US, "%.1f", displaySpeed)} km/h",
                    color = TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Elapsed: ${String.format(Locale.US, "%.2f s", dynoElapsedSec)}",
                    color = HyperCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            val speedProgress = (displaySpeed / 120f).coerceIn(0f, 1f)
            val animatedProgress by animateFloatAsState(
                targetValue = speedProgress,
                animationSpec = tween(80),
                label = "DynoSpeedProgressBar"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(CarbonDark)
                    .border(1.dp, CardBorder.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(HyperCyan, SportAmber, RacingRed)
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = runDynoTest,
                enabled = dynoState == DynoState.IDLE || dynoState == DynoState.FINISHED,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HyperCyan,
                    contentColor = CarbonDark,
                    disabledContainerColor = HyperCyan.copy(alpha = 0.3f),
                    disabledContentColor = CarbonDark.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (dynoState == DynoState.RUNNING) "SPRINT ACTIVE" else "Launch Sprint Dyno",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }

            OutlinedButton(
                onClick = resetDyno,
                modifier = Modifier
                    .height(42.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White.copy(alpha = 0.05f),
                    contentColor = TextSecondary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Reset",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TreeLight(
    active: Boolean,
    activeColor: Color,
    glowColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(if (active) activeColor else Color(0xFF1E293B))
            .border(
                1.5.dp,
                if (active) glowColor else Color(0xFF334155),
                CircleShape
            )
    )
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
            .clip(RoundedCornerShape(12.dp))
            .background(CarbonDark)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = TextMuted,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtext,
            color = TextSecondary,
            fontSize = 8.sp
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
