package ai.medray.staff.ui.nurse

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ai.medray.staff.data.model.DocumentKind
import ai.medray.staff.data.model.QueueEntry
import ai.medray.staff.data.model.QueueStatus
import ai.medray.staff.data.model.Vitals
import ai.medray.staff.domain.VitalsSeverity
import ai.medray.staff.domain.VitalsValidator
import ai.medray.staff.ui.common.QueueStatusBadge
import ai.medray.staff.ui.common.VitalsSummaryBadge
import ai.medray.staff.ui.theme.*

@Composable
fun NurseHomeScreen(
    queue: List<QueueEntry>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onRecordVitalsClick: (QueueEntry) -> Unit,
    onScanDocumentClick: (QueueEntry) -> Unit,
    onStatusChange: (QueueEntry, QueueStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
    ) {
        // Search & Filter Bar
        Surface(
            color = PureWhite,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search patient, UHID, or phone...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Slate400)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MedRayBluePrimary,
                        unfocusedBorderColor = Slate200,
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val waitingCount = queue.count { it.status == QueueStatus.WAITING }
                    val arrivedCount = queue.count { it.status == QueueStatus.ARRIVED }

                    Text(
                        text = "Active Triage Queue (${queue.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(color = StatusWarningBg, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "$waitingCount Waiting",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusWarningText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(color = StatusSuccessBg, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "$arrivedCount Arrived",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusSuccessText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Patient Queue List
        val filtered = remember(queue, searchQuery) {
            if (searchQuery.isBlank()) queue else {
                val q = searchQuery.trim().lowercase()
                queue.filter {
                    it.patient?.fullName?.lowercase()?.contains(q) == true ||
                            it.patient?.phone?.contains(q) == true ||
                            it.patient?.uhid?.lowercase()?.contains(q) == true ||
                            it.opdNumber.lowercase().contains(q)
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().padding(32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.People,
                        contentDescription = null,
                        tint = Slate300,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isEmpty()) "No patients in queue today" else "No matching patients found",
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate600,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { entry ->
                    NursePatientCard(
                        entry = entry,
                        onRecordVitalsClick = { onRecordVitalsClick(entry) },
                        onScanDocumentClick = { onScanDocumentClick(entry) },
                        onMarkArrived = { onStatusChange(entry, QueueStatus.ARRIVED) }
                    )
                }
            }
        }
    }
}

@Composable
fun NursePatientCard(
    entry: QueueEntry,
    onRecordVitalsClick: () -> Unit,
    onScanDocumentClick: () -> Unit,
    onMarkArrived: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: OPD Token + Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MedRayBlueLight,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = entry.opdNumber,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MedRayBluePrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (entry.doctor != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "👉 Dr. ${entry.doctor.fullName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                QueueStatusBadge(status = entry.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Patient Name & Demographics
            Text(
                text = entry.patient?.fullName ?: "Walk-in Patient",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                val demoParts = mutableListOf<String>()
                if (entry.patient?.age != null) demoParts.add("${entry.patient.age}y")
                if (entry.patient?.gender != null) demoParts.add(entry.patient.gender.lowercase().replaceFirstChar { it.uppercase() })
                if (entry.patient?.uhid != null) demoParts.add("UHID: ${entry.patient.uhid}")

                Text(
                    text = demoParts.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
            }

            if (entry.chiefComplaint.isNotBlank()) {
                Text(
                    text = "Chief Complaint: ${entry.chiefComplaint}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate700,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            // Vitals Pill Banner
            Spacer(modifier = Modifier.height(10.dp))
            VitalsSummaryBadge(vitals = entry.vitals)

            // Action Buttons Row
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onRecordVitalsClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (entry.vitals.hasAnyReading()) "Edit Vitals" else "Record Vitals", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onScanDocumentClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MedRayTealDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MedRayTealLight),
                    modifier = Modifier.weight(0.9f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan Lab", fontSize = 13.sp)
                }

                if (entry.status == QueueStatus.WAITING) {
                    IconButton(
                        onClick = onMarkArrived,
                        modifier = Modifier
                            .background(StatusSuccessBg, RoundedCornerShape(10.dp))
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Mark Arrived", tint = StatusSuccessText)
                    }
                }
            }
        }
    }
}

@Composable
fun FastVitalsEntryDialog(
    initialVitals: Vitals,
    patientName: String,
    onDismiss: () -> Unit,
    onSave: (Vitals) -> Unit
) {
    var bp by remember { mutableStateOf(initialVitals.vitalsBp ?: "") }
    var pulse by remember { mutableStateOf(initialVitals.vitalsPulseBpm?.toString() ?: "") }
    var temp by remember { mutableStateOf(initialVitals.vitalsTemperatureF?.toString() ?: "") }
    var spo2 by remember { mutableStateOf(initialVitals.vitalsSpo2?.toString() ?: "") }
    var respRate by remember { mutableStateOf(initialVitals.vitalsRespRate?.toString() ?: "") }
    var weight by remember { mutableStateOf(initialVitals.vitalsWeightKg?.toString() ?: "") }
    var height by remember { mutableStateOf(initialVitals.vitalsHeightCm?.toString() ?: "") }

    val currentVitals = remember(bp, pulse, temp, spo2, respRate, weight, height) {
        Vitals(
            vitalsBp = bp.ifBlank { null },
            vitalsPulseBpm = pulse.toIntOrNull(),
            vitalsTemperatureF = temp.toDoubleOrNull(),
            vitalsSpo2 = spo2.toIntOrNull(),
            vitalsRespRate = respRate.toIntOrNull(),
            vitalsWeightKg = weight.toDoubleOrNull(),
            vitalsHeightCm = height.toDoubleOrNull()
        )
    }

    val eval = remember(currentVitals) { VitalsValidator.evaluate(currentVitals) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Record Patient Vitals",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = patientName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                    }
                }

                // Abnormal Warnings Banner
                AnimatedVisibility(visible = eval.overallSeverity != VitalsSeverity.NORMAL) {
                    val alertColor = if (eval.overallSeverity == VitalsSeverity.CRITICAL) StatusErrorText else StatusWarningText
                    val alertBg = if (eval.overallSeverity == VitalsSeverity.CRITICAL) StatusErrorBg else StatusWarningBg

                    Surface(
                        color = alertBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = alertColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            val msgs = listOfNotNull(eval.bpMessage, eval.spo2Message, eval.tempMessage, eval.pulseMessage)
                            Text(
                                text = msgs.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = alertColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Input Grid
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = bp,
                        onValueChange = { bp = it },
                        label = { Text("BP (120/80)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pulse,
                        onValueChange = { pulse = it },
                        label = { Text("Pulse (bpm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = temp,
                        onValueChange = { temp = it },
                        label = { Text("Temp (°F)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = spo2,
                        onValueChange = { spo2 = it },
                        label = { Text("SpO2 (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it },
                        label = { Text("Height (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (currentVitals.bmi != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Calculated BMI: ${currentVitals.bmi} kg/m²",
                        style = MaterialTheme.typography.labelMedium,
                        color = MedRayBluePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save & Cancel Buttons
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            onSave(currentVitals)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Vitals")
                    }
                }
            }
        }
    }
}
