package com.roadpulse.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadpulse.app.domain.calculator.ChartPoint
import com.roadpulse.app.domain.calculator.TripCalculator
import com.roadpulse.app.ui.theme.CarbonDark
import com.roadpulse.app.ui.theme.CardBorder
import com.roadpulse.app.ui.theme.CardSurfaceLight
import com.roadpulse.app.ui.theme.ElectricBlue
import com.roadpulse.app.ui.theme.MidnightCard
import com.roadpulse.app.ui.theme.NeonCyan
import com.roadpulse.app.ui.theme.RacingRed
import com.roadpulse.app.ui.theme.TextMuted
import com.roadpulse.app.ui.theme.TextPrimary
import com.roadpulse.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun InteractiveSpeedChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) {
        EmptyChartState(modifier = modifier)
        return
    }

    val sampledPoints = remember(points) {
        TripCalculator.buildChartPoints(points, maxSamples = 250)
    }

    val maxDistKm = remember(sampledPoints) {
        (sampledPoints.maxOfOrNull { it.distanceKm } ?: 1f).coerceAtLeast(0.1f)
    }
    val maxSpeedKmh = remember(sampledPoints) {
        (sampledPoints.maxOfOrNull { it.speedKmh } ?: 30f).coerceAtLeast(30f)
    }
    val maxSpeedPoint = remember(sampledPoints) {
        sampledPoints.maxByOrNull { it.speedKmh } ?: sampledPoints.first()
    }

    var selectedPoint by remember { mutableStateOf<ChartPoint?>(null) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightCard)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Chart Header with Inspection Tooltip or Max Speed info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SPEED VS DISTANCE",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                if (selectedPoint != null) {
                    val pt = selectedPoint!!
                    Text(
                        text = "${String.format(Locale.US, "%.1f", pt.speedKmh)} km/h at ${String.format(Locale.US, "%.2f", pt.distanceKm)} km",
                        color = NeonCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Peak: ${maxSpeedPoint.speedKmh.roundToInt()} km/h",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (selectedPoint != null) {
                Text(
                    text = timeFormatter.format(Date(selectedPoint!!.timestamp)),
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(RacingRed)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Max Point",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Canvas Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(sampledPoints) {
                        detectTapGestures(
                            onPress = { offset ->
                                val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                val targetDist = fraction * maxDistKm
                                selectedPoint = sampledPoints.minByOrNull { kotlin.math.abs(it.distanceKm - targetDist) }
                            }
                        )
                    }
                    .pointerInput(sampledPoints) {
                        detectDragGestures(
                            onDrag = { change, _ ->
                                change.consume()
                                val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                val targetDist = fraction * maxDistKm
                                selectedPoint = sampledPoints.minByOrNull { kotlin.math.abs(it.distanceKm - targetDist) }
                            },
                            onDragEnd = {
                                // keep selected for reading or clear on double tap
                            }
                        )
                    }
            ) {
                val chartWidth = size.width
                val chartHeight = size.height

                // Draw horizontal grid lines (4 lines)
                val gridSteps = 4
                for (i in 1..gridSteps) {
                    val y = chartHeight - (chartHeight / gridSteps) * i
                    drawLine(
                        color = CardBorder,
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Construct Area Path and Line Path
                val linePath = Path()
                val areaPath = Path()

                var maxPointOffset = Offset.Zero

                sampledPoints.forEachIndexed { idx, pt ->
                    val x = (pt.distanceKm / maxDistKm) * chartWidth
                    val y = chartHeight - ((pt.speedKmh / maxSpeedKmh) * chartHeight).coerceIn(0f, chartHeight)

                    if (idx == 0) {
                        linePath.moveTo(x, y)
                        areaPath.moveTo(x, chartHeight)
                        areaPath.lineTo(x, y)
                    } else {
                        linePath.lineTo(x, y)
                        areaPath.lineTo(x, y)
                    }

                    if (pt == maxSpeedPoint) {
                        maxPointOffset = Offset(x, y)
                    }
                }

                areaPath.lineTo(chartWidth, chartHeight)
                areaPath.close()

                // Draw Gradient Fill
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = 0.28f),
                            ElectricBlue.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = chartHeight
                    )
                )

                // Draw Speed Polyline
                drawPath(
                    path = linePath,
                    color = NeonCyan,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw Peak Max Speed Dot
                if (maxPointOffset != Offset.Zero) {
                    drawCircle(
                        color = RacingRed,
                        radius = 5.dp.toPx(),
                        center = maxPointOffset
                    )
                    drawCircle(
                        color = CarbonDark,
                        radius = 2.dp.toPx(),
                        center = maxPointOffset
                    )
                }

                // Draw Touch Scrubber Cursor
                selectedPoint?.let { pt ->
                    val cursorX = (pt.distanceKm / maxDistKm) * chartWidth
                    val cursorY = chartHeight - ((pt.speedKmh / maxSpeedKmh) * chartHeight).coerceIn(0f, chartHeight)

                    // Vertical guide line
                    drawLine(
                        color = ElectricBlue.copy(alpha = 0.7f),
                        start = Offset(cursorX, 0f),
                        end = Offset(cursorX, chartHeight),
                        strokeWidth = 1.5.dp.toPx()
                    )

                    // Dot on cursor
                    drawCircle(
                        color = ElectricBlue,
                        radius = 6.dp.toPx(),
                        center = Offset(cursorX, cursorY)
                    )
                    drawCircle(
                        color = TextPrimary,
                        radius = 3.dp.toPx(),
                        center = Offset(cursorX, cursorY)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Axis Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "0.0 km", color = TextMuted, fontSize = 11.sp)
            Text(text = "${String.format(Locale.US, "%.1f", maxDistKm / 2f)} km", color = TextMuted, fontSize = 11.sp)
            Text(text = "${String.format(Locale.US, "%.2f", maxDistKm)} km", color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun EmptyChartState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightCard)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(24.dp)
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No Chart Data Yet",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Record at least 2 GPS points to generate the speed curve.",
                color = TextMuted,
                fontSize = 12.sp
            )
        }
    }
}
