package com.roadpulse.app.ui.components

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
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import com.roadpulse.app.ui.theme.RhodyIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadpulse.app.ui.theme.CardBorder
import com.roadpulse.app.ui.theme.HyperCyan
import com.roadpulse.app.ui.theme.LaserCrimson
import com.roadpulse.app.ui.theme.MidnightCard
import com.roadpulse.app.ui.theme.ObsidianSurface
import com.roadpulse.app.ui.theme.OptimalGreen
import com.roadpulse.app.ui.theme.SportAmber
import com.roadpulse.app.ui.theme.TextMuted
import com.roadpulse.app.ui.theme.TextPrimary
import com.roadpulse.app.ui.theme.TextSecondary

@Composable
fun MotoGpApexTelematicsCard(
    scoreTier: String,
    entrySpeed: String,
    rollAngle: String,
    lateralG: String,
    exitSpeed: String,
    coachTip: String,
    isAnalyzing: Boolean,
    onAnalyze: () -> Unit,
    modifier: Modifier = Modifier
) {
    val guardianPurple = Color(0xFFA855F7)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MidnightCard)
            .border(1.dp, guardianPurple.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ModernIconBadge(
                    icon = RhodyIcons.LeanApex,
                    accentColor = guardianPurple,
                    size = 36.dp,
                    iconSize = 20.dp,
                    shape = BadgeShape.Squircle
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "MOTOGP AI APEX TELEMATICS",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Corner sector split analysis & dynamic coaching",
                        color = TextSecondary,
                        fontSize = 9.sp
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(guardianPurple.copy(alpha = 0.25f))
                    .border(1.dp, guardianPurple.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = scoreTier,
                    color = Color(0xFFD8B4FE),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4 Sector Split Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ApexSectorCell("1. ENTRY", entrySpeed, "Trail-Brake Zone", HyperCyan, Modifier.weight(1f))
            ApexSectorCell("2. APEX ROLL", rollAngle, "Maximum Lean", guardianPurple, Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ApexSectorCell("3. LATERAL G", lateralG, "Tire Adhesion", OptimalGreen, Modifier.weight(1f))
            ApexSectorCell("4. EXIT DRIVE", exitSpeed, "Throttle Roll-On", SportAmber, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dynamic AI Coaching Advice Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1F1133))
                .border(1.dp, guardianPurple.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.Engineering, contentDescription = null, tint = guardianPurple, modifier = Modifier.size(14.dp))
                    Text(
                        text = "AI APEX CORNERING COACH",
                        color = Color(0xFFD8B4FE),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = coachTip,
                    color = TextPrimary,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onAnalyze,
            modifier = Modifier.fillMaxWidth().height(40.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = guardianPurple.copy(alpha = 0.25f),
                contentColor = Color(0xFFE9D5FF)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, guardianPurple.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("ANALYZE LATEST CORNER PULL", fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun ApexSectorCell(
    label: String,
    value: String,
    sub: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = sub, color = TextSecondary, fontSize = 8.sp)
    }
}

@Composable
fun GpsElevationCard(
    currentAltM: Float,
    peakAltM: Float,
    climbGainM: Float,
    gradeText: String,
    gradeColor: Color,
    onSyncElevation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MidnightCard)
            .border(1.dp, HyperCyan.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ModernIconBadge(
                    icon = RhodyIcons.ElevationMountain,
                    accentColor = HyperCyan,
                    size = 36.dp,
                    iconSize = 20.dp,
                    shape = BadgeShape.Squircle
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "GPS ELEVATION & CLIMB PROFILE",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Live GPS altimeter, climb gain & gradient",
                        color = TextSecondary,
                        fontSize = 9.sp
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(gradeColor.copy(alpha = 0.2f))
                    .border(1.dp, gradeColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = gradeText,
                    color = gradeColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(text = "CURRENT ALTITUDE (ASL)", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${currentAltM.toInt()}",
                        color = HyperCyan,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(text = "m ASL", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "Gain: +${climbGainM.toInt()} m", color = OptimalGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text(text = "Peak: ${peakAltM.toInt()} m", color = SportAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val progress = (currentAltM / 1200f).coerceIn(0.05f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ObsidianSurface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(HyperCyan, OptimalGreen)
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onSyncElevation,
            modifier = Modifier.fillMaxWidth().height(40.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = HyperCyan.copy(alpha = 0.2f),
                contentColor = HyperCyan
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, HyperCyan.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("CALIBRATE GPS ALTIMETER (3D SATELLITE SYNC)", fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun CrosswindRadarCard(
    windSpeed: String,
    leanComp: String,
    dragPenalty: String,
    riskText: String,
    riskColor: Color,
    onTestCrashBox: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MidnightCard)
            .border(1.dp, SportAmber.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ModernIconBadge(
                    icon = RhodyIcons.GForceReticle,
                    accentColor = SportAmber,
                    size = 36.dp,
                    iconSize = 20.dp,
                    shape = BadgeShape.Squircle
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "CROSSWIND VECTOR RADAR",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Highway shear & aerodynamic drag resistance",
                        color = TextSecondary,
                        fontSize = 9.sp
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(riskColor.copy(alpha = 0.2f))
                    .border(1.dp, riskColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = riskText,
                    color = riskColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Wind Compass Widget
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(ObsidianSurface)
                    .border(1.dp, CardBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    tint = SportAmber,
                    modifier = Modifier.size(36.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column {
                    Text(text = "RELATIVE WIND VELOCITY", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text(text = windSpeed, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                }
                Column {
                    Text(text = "COUNTER-STEER RECOMMENDATION", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text(text = leanComp, color = HyperCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ObsidianSurface)
                .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "AERODYNAMIC DRAG PENALTY", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(text = dragPenalty, color = LaserCrimson, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            }

            Button(
                onClick = onTestCrashBox,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LaserCrimson.copy(alpha = 0.2f),
                    contentColor = LaserCrimson
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, LaserCrimson.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("TEST CRASH BOX", fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
