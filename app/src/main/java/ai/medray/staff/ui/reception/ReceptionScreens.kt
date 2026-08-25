package ai.medray.staff.ui.reception

import androidx.compose.foundation.background
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
import ai.medray.staff.data.model.*
import ai.medray.staff.ui.common.DynamicUpiQrDialog
import ai.medray.staff.ui.common.QueueStatusBadge
import ai.medray.staff.ui.common.VitalsSummaryBadge
import ai.medray.staff.ui.theme.*

@Composable
fun ReceptionHomeScreen(
    queue: List<QueueEntry>,
    doctors: List<DoctorSummary>,
    selectedDoctorId: String?,
    onDoctorFilterChange: (String?) -> Unit,
    onNewWalkInClick: () -> Unit,
    onStatusChange: (QueueEntry, QueueStatus) -> Unit,
    onCollectPaymentClick: (QueueEntry) -> Unit,
    onWhatsAppClick: (QueueEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
    ) {
        // Quick Action Hero Banner
        Surface(
            color = PureWhite,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "OPD Reception Desk",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "${queue.size} total registered today",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }

                    Button(
                        onClick = onNewWalkInClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Walk-In", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Doctor Filter Chips
                if (doctors.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = selectedDoctorId == null,
                            onClick = { onDoctorFilterChange(null) },
                            label = { Text("All Doctors (${queue.size})", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MedRayBluePrimary,
                                selectedLabelColor = PureWhite
                            )
                        )
                        doctors.take(3).forEach { doc ->
                            val docCount = queue.count { it.doctorId == doc.id }
                            FilterChip(
                                selected = selectedDoctorId == doc.id,
                                onClick = { onDoctorFilterChange(if (selectedDoctorId == doc.id) null else doc.id) },
                                label = { Text("Dr. ${doc.fullName.split(" ").last()} ($docCount)", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MedRayBluePrimary,
                                    selectedLabelColor = PureWhite
                                )
                            )
                        }
                    }
                }
            }
        }

        // Queue List
        val filtered = remember(queue, selectedDoctorId) {
            if (selectedDoctorId == null) queue else queue.filter { it.doctorId == selectedDoctorId }
        }

        if (filtered.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().padding(32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.People, contentDescription = null, tint = Slate300, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No patients in queue", style = MaterialTheme.typography.titleMedium, color = Slate600)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { entry ->
                    ReceptionPatientCard(
                        entry = entry,
                        onStatusChange = { newStatus -> onStatusChange(entry, newStatus) },
                        onCollectPaymentClick = { onCollectPaymentClick(entry) },
                        onWhatsAppClick = { onWhatsAppClick(entry) }
                    )
                }
            }
        }
    }
}

@Composable
fun ReceptionPatientCard(
    entry: QueueEntry,
    onStatusChange: (QueueStatus) -> Unit,
    onCollectPaymentClick: () -> Unit,
    onWhatsAppClick: () -> Unit
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

                QueueStatusBadge(status = entry.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Patient Name & Doctor
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
                if (entry.patient?.phone != null) demoParts.add("📱 ${entry.patient.phone}")
                if (entry.doctor != null) demoParts.add("👉 Dr. ${entry.doctor.fullName}")

                Text(
                    text = demoParts.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
            }

            // Vitals summary
            if (entry.vitals.hasAnyReading()) {
                Spacer(modifier = Modifier.height(8.dp))
                VitalsSummaryBadge(vitals = entry.vitals)
            }

            // Action Buttons
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (entry.status == QueueStatus.WAITING) {
                    Button(
                        onClick = { onStatusChange(QueueStatus.ARRIVED) },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusSuccessText),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mark Arrived", fontSize = 13.sp)
                    }
                }

                Button(
                    onClick = onCollectPaymentClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bill / UPI", fontSize = 13.sp)
                }

                if (!entry.patient?.phone.isNullOrBlank()) {
                    IconButton(
                        onClick = onWhatsAppClick,
                        modifier = Modifier
                            .background(Color(0xFF25D366).copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = Color(0xFF128C7E))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkInRegisterDialog(
    doctors: List<DoctorSummary>,
    onDismiss: () -> Unit,
    onRegister: (fullName: String, phone: String, doctorId: String, chiefComplaint: String, age: Int?, gender: String) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var ageStr by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("MALE") }
    var chiefComplaint by remember { mutableStateOf("Routine Consultation") }
    var selectedDoctorId by remember { mutableStateOf(doctors.firstOrNull()?.id ?: "") }

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
                Text(
                    text = "New Walk-In Registration",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    text = "Generates OPD token & sends WhatsApp alert",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Patient Full Name *") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (10 digits) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = ageStr,
                        onValueChange = { ageStr = it },
                        label = { Text("Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )

                    var genderExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = genderExpanded,
                        onExpandedChange = { genderExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = gender,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gender") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = genderExpanded,
                            onDismissRequest = { genderExpanded = false }
                        ) {
                            listOf("MALE", "FEMALE", "OTHER").forEach { g ->
                                DropdownMenuItem(
                                    text = { Text(g) },
                                    onClick = {
                                        gender = g
                                        genderExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Doctor Selection Dropdown
                var doctorExpanded by remember { mutableStateOf(false) }
                val selectedDoctor = doctors.find { it.id == selectedDoctorId }
                ExposedDropdownMenuBox(
                    expanded = doctorExpanded,
                    onExpandedChange = { doctorExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedDoctor?.fullName?.let { "Dr. $it" } ?: "Select Doctor *",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Consulting Doctor *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = doctorExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = doctorExpanded,
                        onDismissRequest = { doctorExpanded = false }
                    ) {
                        doctors.forEach { doc ->
                            DropdownMenuItem(
                                text = { Text("Dr. ${doc.fullName} (${doc.specialization ?: "General"})") },
                                onClick = {
                                    selectedDoctorId = doc.id
                                    doctorExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = chiefComplaint,
                    onValueChange = { chiefComplaint = it },
                    label = { Text("Chief Complaint / Reason") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

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
                            if (fullName.isNotBlank() && phone.isNotBlank() && selectedDoctorId.isNotBlank()) {
                                onRegister(fullName, phone, selectedDoctorId, chiefComplaint, ageStr.toIntOrNull(), gender)
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Register & Token")
                    }
                }
            }
        }
    }
}
