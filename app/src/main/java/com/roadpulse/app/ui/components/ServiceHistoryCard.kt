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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.roadpulse.app.ui.theme.TextMuted
import com.roadpulse.app.ui.theme.TextPrimary
import com.roadpulse.app.ui.theme.TextSecondary

data class ServiceRecord(
    val id: Long,
    val date: String,
    val center: String,
    val odo: String,
    val workDone: String,
    val cost: String
)

@Composable
fun ServiceHistoryCard(
    records: List<ServiceRecord>,
    onAddRecordClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MidnightCard)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        imageVector = Icons.Default.Receipt,
                        contentDescription = "Service Ledger",
                        tint = HyperCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "SERVICE HISTORY & WORKSHOP INVOICES",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Complete historical maintenance ledger recorded for your machine",
                        color = TextSecondary,
                        fontSize = 9.sp
                    )
                }
            }

            Button(
                onClick = onAddRecordClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = HyperCyan.copy(alpha = 0.18f),
                    contentColor = HyperCyan
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, HyperCyan.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text("+ Add Record", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No service records logged yet. Tap '+ Add Record' to track maintenance invoices.",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                records.forEach { record ->
                    ServiceRecordRow(record = record)
                }
            }
        }
    }
}

@Composable
private fun ServiceRecordRow(
    record: ServiceRecord
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CarbonDark)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = record.date,
                        color = HyperCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "•", color = TextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = record.odo,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "₹${record.cost}",
                    color = OptimalGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = record.center,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = record.workDone,
                color = TextMuted,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun AddServiceRecordDialog(
    onDismiss: () -> Unit,
    onSaveRecord: (ServiceRecord) -> Unit
) {
    var date by remember { mutableStateOf("18 Oct 2026") }
    var center by remember { mutableStateOf("") }
    var odo by remember { mutableStateOf("") }
    var workDone by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F1420),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Build, contentDescription = null, tint = HyperCyan, modifier = Modifier.size(20.dp))
                Text("ADD WORKSHOP INVOICE", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Service Date") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HyperCyan,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = center,
                    onValueChange = { center = it },
                    label = { Text("Service Center Name") },
                    placeholder = { Text("e.g. Authorized Service Center") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HyperCyan,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = odo,
                    onValueChange = { odo = it },
                    label = { Text("Odometer Reading") },
                    placeholder = { Text("e.g. 4,500 km") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HyperCyan,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = workDone,
                    onValueChange = { workDone = it },
                    label = { Text("Work Done / Parts Replaced") },
                    placeholder = { Text("e.g. Engine oil, filter & chain service") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HyperCyan,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it },
                    label = { Text("Invoice Amount (₹)") },
                    placeholder = { Text("e.g. 2,450") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HyperCyan,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (center.isNotBlank() && cost.isNotBlank()) {
                        val record = ServiceRecord(
                            id = System.currentTimeMillis(),
                            date = date.ifBlank { "Recent" },
                            center = center,
                            odo = if (odo.endsWith("km")) odo else "$odo km",
                            workDone = workDone.ifBlank { "Periodic Service" },
                            cost = cost.replace("₹", "").trim()
                        )
                        onSaveRecord(record)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HyperCyan, contentColor = CarbonDark)
            ) {
                Text("SAVE RECORD", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    )
}
