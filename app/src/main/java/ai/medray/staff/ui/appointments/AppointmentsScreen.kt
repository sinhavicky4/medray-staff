package ai.medray.staff.ui.appointments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.medray.staff.data.model.Appointment
import ai.medray.staff.data.model.AppointmentStatus
import ai.medray.staff.ui.common.MedRayPullRefreshBox
import ai.medray.staff.ui.common.QuickFilterPill
import ai.medray.staff.ui.common.StatCard
import ai.medray.staff.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AppointmentsScreen(
    appointments: List<Appointment>,
    onCheckInClick: (Appointment) -> Unit,
    onCancelClick: (Appointment) -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<AppointmentStatus?>(null) }
    val todayStr = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")) }

    val scheduledCount = appointments.count { it.status == AppointmentStatus.SCHEDULED }
    val checkedInCount = appointments.count { it.status == AppointmentStatus.CHECKED_IN }
    val completedCount = appointments.count { it.status == AppointmentStatus.COMPLETED }
    val cancelledCount = appointments.count { it.status == AppointmentStatus.CANCELLED }

    val filtered = remember(appointments, searchQuery, selectedStatusFilter) {
        appointments.filter { appt ->
            val matchesStatus = selectedStatusFilter == null || appt.status == selectedStatusFilter
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                appt.patient?.fullName?.lowercase()?.contains(q) == true ||
                        appt.patient?.phone?.contains(q) == true ||
                        appt.doctor?.fullName?.lowercase()?.contains(q) == true
            }
            matchesStatus && matchesSearch
        }
    }

    MedRayPullRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
        // 1. Header & Refresh
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Appointments Schedule",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            Icons.Filled.CalendarToday,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = todayStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(38.dp)
                        .background(PureWhite, RoundedCornerShape(10.dp))
                        .border(1.dp, Slate200, RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = MedRayBluePrimary, modifier = Modifier.size(18.dp))
                }
            }
        }

        // 2. Interactive KPI Stat Cards (2x2 Grid)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatCard(
                        title = "Scheduled",
                        value = "$scheduledCount",
                        footer = "Expected today",
                        icon = Icons.Filled.EventAvailable,
                        iconBg = Color(0xFFFEF3C7),
                        iconTint = Color(0xFFD97706),
                        isSelected = selectedStatusFilter == AppointmentStatus.SCHEDULED,
                        onClick = {
                            selectedStatusFilter = if (selectedStatusFilter == AppointmentStatus.SCHEDULED) null else AppointmentStatus.SCHEDULED
                        },
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Checked In",
                        value = "$checkedInCount",
                        footer = "In clinic queue",
                        icon = Icons.Filled.CheckCircleOutline,
                        iconBg = Color(0xFFDCFCE7),
                        iconTint = Color(0xFF16A34A),
                        isSelected = selectedStatusFilter == AppointmentStatus.CHECKED_IN,
                        onClick = {
                            selectedStatusFilter = if (selectedStatusFilter == AppointmentStatus.CHECKED_IN) null else AppointmentStatus.CHECKED_IN
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatCard(
                        title = "Completed",
                        value = "$completedCount",
                        footer = "Consultations finished",
                        icon = Icons.Filled.Check,
                        iconBg = Color(0xFFF1F5F9),
                        iconTint = Slate500,
                        isSelected = selectedStatusFilter == AppointmentStatus.COMPLETED,
                        onClick = {
                            selectedStatusFilter = if (selectedStatusFilter == AppointmentStatus.COMPLETED) null else AppointmentStatus.COMPLETED
                        },
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Cancelled",
                        value = "$cancelledCount",
                        footer = "Cancelled / No show",
                        icon = Icons.Filled.Cancel,
                        iconBg = Color(0xFFFEE2E2),
                        iconTint = Color(0xFFDC2626),
                        isSelected = selectedStatusFilter == AppointmentStatus.CANCELLED,
                        onClick = {
                            selectedStatusFilter = if (selectedStatusFilter == AppointmentStatus.CANCELLED) null else AppointmentStatus.CANCELLED
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search patient name, phone, or doctor…", fontSize = 13.sp, color = Slate400) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MedRayBluePrimary, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Slate400, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MedRayBluePrimary,
                    unfocusedBorderColor = Slate200,
                    focusedContainerColor = PureWhite,
                    unfocusedContainerColor = PureWhite
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 4. Quick Filter Pills
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                QuickFilterPill(
                    label = "All (${appointments.size})",
                    isSelected = selectedStatusFilter == null,
                    onClick = { selectedStatusFilter = null }
                )
                QuickFilterPill(
                    label = "Scheduled ($scheduledCount)",
                    isSelected = selectedStatusFilter == AppointmentStatus.SCHEDULED,
                    dotColor = Color(0xFFD97706),
                    onClick = { selectedStatusFilter = AppointmentStatus.SCHEDULED }
                )
                QuickFilterPill(
                    label = "Checked In ($checkedInCount)",
                    isSelected = selectedStatusFilter == AppointmentStatus.CHECKED_IN,
                    dotColor = Color(0xFF16A34A),
                    onClick = { selectedStatusFilter = AppointmentStatus.CHECKED_IN }
                )
                QuickFilterPill(
                    label = "Completed ($completedCount)",
                    isSelected = selectedStatusFilter == AppointmentStatus.COMPLETED,
                    dotColor = Slate400,
                    onClick = { selectedStatusFilter = AppointmentStatus.COMPLETED }
                )
            }
        }

        // 5. Appointments List
        if (filtered.isEmpty()) {
            item {
                Surface(
                    color = PureWhite,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.EventBusy,
                            contentDescription = null,
                            tint = Slate300,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedStatusFilter != null) "No matching appointments found" else "No appointments scheduled for today",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate700
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Walk-in patients can be registered instantly in the OPD Queue.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { appointment ->
                AppointmentCard(
                    appointment = appointment,
                    onCheckIn = { onCheckInClick(appointment) },
                    onCancel = { onCancelClick(appointment) }
                )
            }
        }
    }
}
}

@Composable
fun AppointmentCard(
    appointment: Appointment,
    onCheckIn: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val patient = appointment.patient
    val initials = remember(patient?.fullName) {
        val names = (patient?.fullName ?: "P").trim().split(" ")
        if (names.size >= 2) "${names[0].take(1)}${names[1].take(1)}".uppercase()
        else (patient?.fullName ?: "P").take(2).uppercase()
    }

    val timeStr = remember(appointment.scheduledAt) {
        try {
            if (appointment.scheduledAt.contains("T")) {
                val timePart = appointment.scheduledAt.substringAfter("T").substringBefore("Z").substringBefore("+").substringBefore(".")
                val parts = timePart.split(":")
                val hour = parts[0].toInt()
                val min = parts[1]
                val ampm = if (hour >= 12) "PM" else "AM"
                val h12 = if (hour % 12 == 0) 12 else hour % 12
                "$h12:$min $ampm"
            } else appointment.scheduledAt.takeLast(8).take(5)
        } catch (_: Exception) {
            "Today"
        }
    }

    val (statusBg, statusText, statusDot) = when (appointment.status) {
        AppointmentStatus.CHECKED_IN -> Triple(Color(0xFFDCFCE7), Color(0xFF16A34A), Color(0xFF22C55E))
        AppointmentStatus.COMPLETED -> Triple(Color(0xFFF1F5F9), Slate600, Slate400)
        AppointmentStatus.CANCELLED -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), Color(0xFFEF4444))
        else -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), Color(0xFFF59E0B))
    }

    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Time badge & Status badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MedRayBlueLight,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = MedRayBluePrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MedRayBluePrimary
                        )
                    }
                }

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(statusDot, CircleShape))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = appointment.status.name.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body: Patient Avatar + Name + Demographics + Doctor
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFEFF6FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MedRayBluePrimary
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = patient?.fullName ?: "Scheduled Patient",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        val demo = buildList {
                            if (patient?.age != null) add("${patient.age}y")
                            if (!patient?.gender.isNullOrBlank()) add(patient?.gender!!.lowercase().replaceFirstChar { it.uppercase() })
                            if (!patient?.phone.isNullOrBlank()) add("📞 +91 ${patient?.phone}")
                        }
                        Text(
                            text = demo.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }

                    if (appointment.doctor != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                Icons.Outlined.LocalHospital,
                                contentDescription = null,
                                tint = MedRayBluePrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Dr. ${appointment.doctor.fullName}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MedRayBlueDark
                            )
                        }
                    }
                }
            }

            // Action Buttons
            if (appointment.status == AppointmentStatus.SCHEDULED) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onCheckIn,
                        colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Filled.HowToReg, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Check In to Queue", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
