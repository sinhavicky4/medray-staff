package ai.medray.staff.ui.selfcheckins

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ai.medray.staff.data.model.DoctorSummary
import ai.medray.staff.data.model.SelfCheckIn
import ai.medray.staff.ui.common.MedRayPullRefreshBox
import ai.medray.staff.ui.theme.*

@Composable
fun SelfCheckInsScreen(
    checkIns: List<SelfCheckIn>,
    onAssignClick: (SelfCheckIn) -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    MedRayPullRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize().background(Slate50)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
            ) {
                Column {
                    Text(
                        text = "Self Check-In Kiosk",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = "Patient QR Arrivals · ${checkIns.size} Pending",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )
                }

                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MedRayBluePrimary)
                }
            }

        if (checkIns.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircleOutline,
                        contentDescription = null,
                        tint = StatusSuccessText,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No pending self-check-ins",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate700
                    )
                    Text(
                        text = "When patients scan the clinic QR code at the entrance, they appear here instantly",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(checkIns, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = item.patient?.fullName ?: "Walk-in Patient",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Surface(
                                    color = StatusWarningBg,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = item.status.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusWarningText,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (!item.patient?.phone.isNullOrBlank()) {
                                Text(
                                    text = "+91 ${item.patient?.phone}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate500
                                )
                            }

                            if (!item.chiefComplaint.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Complaint: ${item.chiefComplaint}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate700
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = { onAssignClick(item) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Assign Doctor & Issue Token", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
}

/**
 * Assigns an already-kiosk-checked-in patient to a doctor's queue —
 * mirrors the web app's inline assign panel (SelfCheckinsClient.tsx). The
 * patient, chief complaint, and any vitals were already captured at the
 * kiosk, so unlike a fresh walk-in this only needs a doctor picked, not a
 * full register/search-existing-patient flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignSelfCheckInDialog(
    checkIn: SelfCheckIn,
    doctors: List<DoctorSummary>,
    busy: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (doctorId: String) -> Unit
) {
    var selectedDoctorId by remember { mutableStateOf(doctors.firstOrNull()?.id ?: "") }
    var doctorDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PureWhite,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Assign Doctor & Issue Token",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Route this kiosk check-in to a consulting doctor",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }
                    IconButton(onClick = onDismiss, enabled = !busy) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                    }
                }

                HorizontalDivider(color = Slate100, modifier = Modifier.padding(vertical = 12.dp))

                // Patient Info (read-only — already captured at the kiosk)
                Surface(
                    color = Slate50,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = checkIn.patient?.fullName ?: "Unknown Patient",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        if (!checkIn.patient?.uhid.isNullOrBlank() || !checkIn.patient?.phone.isNullOrBlank()) {
                            Text(
                                text = listOfNotNull(
                                    checkIn.patient?.uhid?.ifBlank { null }?.let { "UHID $it" },
                                    checkIn.patient?.phone?.ifBlank { null }
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                        }
                        if (!checkIn.chiefComplaint.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Complaint: ${checkIn.chiefComplaint}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate700
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                ExposedDropdownMenuBox(
                    expanded = doctorDropdownExpanded,
                    onExpandedChange = { doctorDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val selectedDocName = doctors.find { it.id == selectedDoctorId }?.fullName
                    OutlinedTextField(
                        value = selectedDocName?.let { "Dr. $it" } ?: "No doctors available",
                        onValueChange = {},
                        readOnly = true,
                        enabled = doctors.isNotEmpty(),
                        label = { Text("Consulting Doctor *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = doctorDropdownExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = doctorDropdownExpanded,
                        onDismissRequest = { doctorDropdownExpanded = false }
                    ) {
                        doctors.forEach { doc ->
                            DropdownMenuItem(
                                text = { Text("Dr. ${doc.fullName}${doc.specialization?.let { " — $it" } ?: ""}") },
                                onClick = {
                                    selectedDoctorId = doc.id
                                    doctorDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !busy,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { onConfirm(selectedDoctorId) },
                        enabled = !busy && selectedDoctorId.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Text(if (busy) "Adding…" else "Confirm & Add to Queue", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
