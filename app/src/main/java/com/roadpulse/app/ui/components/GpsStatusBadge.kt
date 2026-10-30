package com.roadpulse.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadpulse.app.domain.model.GpsStatus
import com.roadpulse.app.ui.theme.CardBorder
import com.roadpulse.app.ui.theme.MidnightCard
import com.roadpulse.app.ui.theme.RacingRed
import com.roadpulse.app.ui.theme.SportAmber
import com.roadpulse.app.ui.theme.SuccessGreen
import com.roadpulse.app.ui.theme.TextMuted
import com.roadpulse.app.ui.theme.TextPrimary

@Composable
fun GpsStatusBadge(
    gpsStatus: GpsStatus,
    accuracyMeters: Float,
    modifier: Modifier = Modifier
) {
    val (dotColor, text) = when (gpsStatus) {
        GpsStatus.ACCURATE -> SuccessGreen to "3D FIX ±${accuracyMeters.toInt()}m"
        GpsStatus.POOR_ACCURACY -> SportAmber to "2D FIX ±${accuracyMeters.toInt()}m"
        GpsStatus.SEARCHING -> SportAmber to "SYNCING GPS"
        GpsStatus.GPS_DISABLED -> RacingRed to "GPS OFF"
        GpsStatus.NO_PERMISSION -> RacingRed to "NO PERMISSION"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MidnightCard)
            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}
