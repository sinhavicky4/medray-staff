package ai.medray.staff.ui.reception

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ai.medray.staff.data.model.*
import ai.medray.staff.ui.common.DynamicUpiQrDialog
import ai.medray.staff.ui.common.MedRayPullRefreshBox
import ai.medray.staff.ui.common.QueueStatusBadge
import ai.medray.staff.ui.common.QuickFilterPill
import ai.medray.staff.ui.common.StatCard
import ai.medray.staff.ui.common.VitalsSummaryBadge
import ai.medray.staff.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun ReceptionHomeScreen(
    queue: List<QueueEntry>,
    doctors: List<DoctorSummary>,
    selectedDoctorId: String?,
    userName: String? = null,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onDoctorFilterChange: (String?) -> Unit,
    onNewWalkInClick: () -> Unit,
    onRecordVitalsClick: (QueueEntry) -> Unit,
    onViewPrescriptionClick: (QueueEntry) -> Unit = {},
    onStatusChange: (QueueEntry, QueueStatus) -> Unit,
    onCollectPaymentClick: (QueueEntry) -> Unit,
    onWhatsAppClick: (QueueEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<QueueStatus?>(null) }

    val todayStr = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")) }
    val greeting = remember {
        val hour = LocalTime.now().hour
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    val arrivedCount = queue.count { it.status == QueueStatus.ARRIVED }
    val waitingCount = queue.count { it.status == QueueStatus.WAITING }
    val inProgressCount = queue.count { it.status == QueueStatus.IN_PROGRESS }
    val completedCount = queue.count { it.status == QueueStatus.COMPLETED }

    val filteredQueue = remember(queue, searchQuery, selectedDoctorId, selectedStatusFilter) {
        queue.filter { entry ->
            val matchesDoctor = selectedDoctorId == null || entry.doctorId == selectedDoctorId
            val matchesStatus = selectedStatusFilter == null || entry.status == selectedStatusFilter
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                entry.patient?.fullName?.lowercase()?.contains(q) == true ||
                        entry.patient?.phone?.contains(q) == true ||
                        entry.patient?.uhid?.lowercase()?.contains(q) == true ||
                        entry.opdNumber.lowercase().contains(q) ||
                        entry.doctor?.fullName?.lowercase()?.contains(q) == true
            }
            matchesDoctor && matchesStatus && matchesSearch
        }.sortedWith(
            compareBy<QueueEntry> { entry ->
                // Priority 1: Completed / Cancelled records move down to the bottom
                when (entry.status) {
                    QueueStatus.COMPLETED -> 2
                    QueueStatus.CANCELLED -> 3
                    else -> 1 // Active queue items at the top
                }
            }.thenBy { entry ->
                // Priority 2: FIFO (First In, First Out) by check-in / creation timestamp
                entry.createdAt ?: entry.scheduledAt
            }.thenBy { entry ->
                // Priority 3: Fallback token number
                entry.opdNumber
            }
        )
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
        // 1. Hero Header & Quick Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    val displayName = userName?.trim()?.split(" ")?.firstOrNull()?.ifBlank { "Front Desk" } ?: "Front Desk"
                    Text(
                        text = "$greeting, $displayName 👋",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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

                Button(
                    onClick = onNewWalkInClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("+ Walk-In", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. Interactive KPI Stat Cards (2x2 Grid)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                ) {
                    StatCard(
                        title = "Arrived",
                        value = "$arrivedCount",
                        footer = "Checked-in at clinic",
                        icon = Icons.Filled.Check,
                        iconBg = Color(0xFFDCFCE7),
                        iconTint = Color(0xFF16A34A),
                        isSelected = selectedStatusFilter == QueueStatus.ARRIVED,
                        onClick = {
                            selectedStatusFilter = if (selectedStatusFilter == QueueStatus.ARRIVED) null else QueueStatus.ARRIVED
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    StatCard(
                        title = "Waiting",
                        value = "$waitingCount",
                        footer = "In waiting lounge",
                        icon = Icons.Filled.Schedule,
                        iconBg = Color(0xFFFEF3C7),
                        iconTint = Color(0xFFD97706),
                        isSelected = selectedStatusFilter == QueueStatus.WAITING,
                        onClick = {
                            selectedStatusFilter = if (selectedStatusFilter == QueueStatus.WAITING) null else QueueStatus.WAITING
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                ) {
                    StatCard(
                        title = "In Triage",
                        value = "$inProgressCount",
                        footer = "With nurse / doctor",
                        icon = Icons.Filled.LocalHospital,
                        iconBg = Color(0xFFEFF6FF),
                        iconTint = MedRayBluePrimary,
                        isSelected = selectedStatusFilter == QueueStatus.IN_PROGRESS,
                        onClick = {
                            selectedStatusFilter = if (selectedStatusFilter == QueueStatus.IN_PROGRESS) null else QueueStatus.IN_PROGRESS
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    StatCard(
                        title = "Completed",
                        value = "$completedCount",
                        footer = "Consultations done",
                        icon = Icons.Filled.CheckCircle,
                        iconBg = Color(0xFFF1F5F9),
                        iconTint = Slate500,
                        isSelected = selectedStatusFilter == QueueStatus.COMPLETED,
                        onClick = {
                            selectedStatusFilter = if (selectedStatusFilter == QueueStatus.COMPLETED) null else QueueStatus.COMPLETED
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }

        // 3. Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search patient name, phone, or UHID…", fontSize = 13.sp, color = Slate400) },
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

        // 4. Doctor Filter Chips (Horizontal Scroll)
        if (doctors.isNotEmpty()) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    FilterChip(
                        selected = selectedDoctorId == null,
                        onClick = { onDoctorFilterChange(null) },
                        label = { Text("All Doctors (${queue.size})", fontSize = 12.sp, fontWeight = if (selectedDoctorId == null) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MedRayBluePrimary,
                            selectedLabelColor = PureWhite,
                            containerColor = PureWhite,
                            labelColor = Slate700
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (selectedDoctorId == null) MedRayBluePrimary else Slate200,
                            selectedBorderColor = MedRayBluePrimary,
                            enabled = true,
                            selected = selectedDoctorId == null
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    doctors.forEach { doc ->
                        val docCount = queue.count { it.doctorId == doc.id }
                        val isDocSelected = selectedDoctorId == doc.id
                        FilterChip(
                            selected = isDocSelected,
                            onClick = { onDoctorFilterChange(if (isDocSelected) null else doc.id) },
                            label = { Text("Dr. ${doc.fullName.split(" ").last()} ($docCount)", fontSize = 12.sp, fontWeight = if (isDocSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MedRayBluePrimary,
                                selectedLabelColor = PureWhite,
                                containerColor = PureWhite,
                                labelColor = Slate700
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (isDocSelected) MedRayBluePrimary else Slate200,
                                selectedBorderColor = MedRayBluePrimary,
                                enabled = true,
                                selected = isDocSelected
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }

        // 5. Quick Status Filter Pills
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                QuickFilterPill(
                    label = "All (${queue.size})",
                    isSelected = selectedStatusFilter == null,
                    onClick = { selectedStatusFilter = null }
                )
                QuickFilterPill(
                    label = "Arrived ($arrivedCount)",
                    isSelected = selectedStatusFilter == QueueStatus.ARRIVED,
                    dotColor = Color(0xFF16A34A),
                    onClick = { selectedStatusFilter = QueueStatus.ARRIVED }
                )
                QuickFilterPill(
                    label = "Waiting ($waitingCount)",
                    isSelected = selectedStatusFilter == QueueStatus.WAITING,
                    dotColor = Color(0xFFD97706),
                    onClick = { selectedStatusFilter = QueueStatus.WAITING }
                )
                QuickFilterPill(
                    label = "In Triage ($inProgressCount)",
                    isSelected = selectedStatusFilter == QueueStatus.IN_PROGRESS,
                    dotColor = MedRayBluePrimary,
                    onClick = { selectedStatusFilter = QueueStatus.IN_PROGRESS }
                )
                QuickFilterPill(
                    label = "Completed ($completedCount)",
                    isSelected = selectedStatusFilter == QueueStatus.COMPLETED,
                    dotColor = Slate400,
                    onClick = { selectedStatusFilter = QueueStatus.COMPLETED }
                )
            }
        }

        // 6. Patient Queue Cards
        if (filteredQueue.isEmpty()) {
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
                            Icons.Outlined.People,
                            contentDescription = null,
                            tint = Slate300,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedStatusFilter != null) "No matching patients found" else "No patients in queue today",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate700
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap + Walk-In above to register a new arriving patient.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }
                }
            }
        } else {
            items(filteredQueue, key = { it.id }) { entry ->
                ReceptionPatientCard(
                    entry = entry,
                    onRecordVitalsClick = { onRecordVitalsClick(entry) },
                    onViewPrescriptionClick = { onViewPrescriptionClick(entry) },
                    onStatusChange = { onStatusChange(entry, it) },
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
    onRecordVitalsClick: () -> Unit,
    onViewPrescriptionClick: () -> Unit = {},
    onStatusChange: (QueueStatus) -> Unit,
    onCollectPaymentClick: () -> Unit,
    onWhatsAppClick: () -> Unit
) {
    val patient = entry.patient
    val initials = remember(patient?.fullName) {
        val names = (patient?.fullName ?: "P").trim().split(" ")
        if (names.size >= 2) "${names[0].take(1)}${names[1].take(1)}".uppercase()
        else (patient?.fullName ?: "P").take(2).uppercase()
    }

    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Token Badge, Scheduled Time, and Status Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false).padding(end = 6.dp)
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
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        color = Slate100,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = Slate500,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${entry.formattedTime} · by ${entry.addedByDisplay}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate600,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                QueueStatusBadge(status = entry.status)
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
                        text = patient?.fullName ?: "Walk-in Patient",
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

                    if (entry.doctor != null) {
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
                                text = "Dr. ${entry.doctor.fullName}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MedRayBlueDark
                            )
                        }
                    }
                }
            }

            if (entry.chiefComplaint.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Slate50,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = entry.chiefComplaint,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate700,
                            maxLines = 2
                        )
                    }
                }
            }

            // Vitals Banner Strip
            Spacer(modifier = Modifier.height(10.dp))
            VitalsSummaryBadge(vitals = entry.vitals, modifier = Modifier.fillMaxWidth())

            // Action Buttons Bar
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
            ) {
                OutlinedButton(
                    onClick = onRecordVitalsClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MedRayBluePrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(14.dp), tint = MedRayBluePrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (entry.vitals.hasAnyReading()) "Vitals" else "+ Vitals",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (entry.status == QueueStatus.COMPLETED) {
                    Button(
                        onClick = onViewPrescriptionClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Rx", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                } else if (entry.advancePaidTotal > 0) {
                    Surface(
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = StatusSuccessText, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "₹${entry.advancePaidTotal.toInt()} Paid",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusSuccessText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onCollectPaymentClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Icon(Icons.Filled.QrCode, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Collect Fee", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                OutlinedButton(
                    onClick = onWhatsAppClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF15803D)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFF16A34A))
                }

                if (entry.status == QueueStatus.WAITING) {
                    IconButton(
                        onClick = { onStatusChange(QueueStatus.ARRIVED) },
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(Color(0xFFDCFCE7), RoundedCornerShape(10.dp))
                            .width(36.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Mark Arrived", tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

/**
 * OPD Walk-in Registration Dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToQueueForPatientDialog(
    patient: Patient,
    doctors: List<DoctorSummary>,
    onDismiss: () -> Unit,
    onAddToQueue: (doctorId: String, complaint: String, vitals: Vitals?) -> Unit
) {
    var selectedDoctorId by remember { mutableStateOf(doctors.firstOrNull()?.id ?: "") }
    var chiefComplaint by remember { mutableStateOf("") }
    var doctorDropdownExpanded by remember { mutableStateOf(false) }
    var showVitalsFields by remember { mutableStateOf(false) }

    var bp by remember { mutableStateOf("") }
    var temp by remember { mutableStateOf("") }
    var pulse by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }

    val initials = remember(patient.fullName) {
        val names = patient.fullName.trim().split(" ")
        if (names.size >= 2) "${names[0].take(1)}${names[1].take(1)}".uppercase()
        else patient.fullName.take(2).uppercase()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PureWhite,
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 480.dp)
                .imePadding()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Add to Doctor's Queue",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Assign consulting doctor & issue token",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                    }
                }

                HorizontalDivider(color = Slate100, modifier = Modifier.padding(vertical = 12.dp))

                // Patient Info Card
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
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
                                text = patient.fullName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = "UHID: ${patient.uhid} · 📞 +91 ${patient.phone ?: "N/A"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Consulting Doctor Dropdown
                    ExposedDropdownMenuBox(
                        expanded = doctorDropdownExpanded,
                        onExpandedChange = { doctorDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val selectedDocName = doctors.find { it.id == selectedDoctorId }?.fullName ?: "Select Doctor"
                        OutlinedTextField(
                            value = "Dr. $selectedDocName",
                            onValueChange = {},
                            readOnly = true,
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
                                    text = { Text("Dr. ${doc.fullName}") },
                                    onClick = {
                                        selectedDoctorId = doc.id
                                        doctorDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Chief Complaint
                    OutlinedTextField(
                        value = chiefComplaint,
                        onValueChange = { chiefComplaint = it },
                        label = { Text("Chief Complaint") },
                        placeholder = { Text("e.g. Routine Consultation, Fever, Follow-up") },
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Optional Vitals Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showVitalsFields = !showVitalsFields }
                            .padding(vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Favorite, contentDescription = null, tint = MedRayBluePrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pre-Visit Vitals (Optional)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
                        }
                        Text(
                            text = if (showVitalsFields) "Hide ▲" else "Add ▼",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedRayBluePrimary
                        )
                    }

                    if (showVitalsFields) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = bp,
                                    onValueChange = { bp = it },
                                    label = { Text("BP (mmHg)") },
                                    placeholder = { Text("120/80") },
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = temp,
                                    onValueChange = { temp = it },
                                    label = { Text("Temp (°F)") },
                                    placeholder = { Text("98.6") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = pulse,
                                    onValueChange = { pulse = it },
                                    label = { Text("Pulse (bpm)") },
                                    placeholder = { Text("72") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = weight,
                                    onValueChange = { weight = it },
                                    label = { Text("Weight (kg)") },
                                    placeholder = { Text("65.0") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
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
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (selectedDoctorId.isNotBlank()) {
                                val vitals = if (bp.isNotBlank() || temp.isNotBlank() || pulse.isNotBlank() || weight.isNotBlank()) {
                                    Vitals(
                                        vitalsBp = bp.ifBlank { null },
                                        vitalsTemperatureF = temp.toDoubleOrNull(),
                                        vitalsPulseBpm = pulse.toIntOrNull(),
                                        vitalsWeightKg = weight.toDoubleOrNull()
                                    )
                                } else null
                                val finalComplaint = chiefComplaint.ifBlank { "Routine Consultation" }
                                onAddToQueue(selectedDoctorId, finalComplaint, vitals)
                                onDismiss()
                            }
                        },
                        enabled = selectedDoctorId.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add to Queue", fontWeight = FontWeight.Bold)
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
    existingPatients: List<Patient> = emptyList(),
    onDismiss: () -> Unit,
    onRegister: (patientName: String, phone: String, doctorId: String, complaint: String, age: Int?, gender: String) -> Unit,
    onAddExisting: (patient: Patient, doctorId: String, complaint: String) -> Unit = { _, _, _ -> }
) {
    var isExistingPatientMode by remember { mutableStateOf(false) }
    var selectedExistingPatient by remember { mutableStateOf<Patient?>(null) }
    var patientSearchQuery by remember { mutableStateOf("") }

    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("MALE") }
    var selectedDoctorId by remember { mutableStateOf(doctors.firstOrNull()?.id ?: "") }
    var chiefComplaint by remember { mutableStateOf("") }
    var doctorDropdownExpanded by remember { mutableStateOf(false) }

    val filteredPatients = remember(patientSearchQuery, existingPatients) {
        if (patientSearchQuery.isBlank()) existingPatients.take(5)
        else {
            val q = patientSearchQuery.trim().lowercase()
            existingPatients.filter {
                it.fullName.lowercase().contains(q) ||
                        it.phone?.contains(q) == true ||
                        it.uhid.lowercase().contains(q)
            }.take(6)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PureWhite,
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 480.dp)
                .imePadding()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = if (isExistingPatientMode) "Add Existing Patient" else "New Walk-In Patient",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Instant OPD token generation & registration",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode Selector Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate100, RoundedCornerShape(10.dp))
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isExistingPatientMode) PureWhite else Color.Transparent)
                            .clickable { isExistingPatientMode = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "New Patient",
                            fontSize = 12.sp,
                            fontWeight = if (!isExistingPatientMode) FontWeight.Bold else FontWeight.Normal,
                            color = if (!isExistingPatientMode) MedRayBluePrimary else Slate600
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isExistingPatientMode) PureWhite else Color.Transparent)
                            .clickable { isExistingPatientMode = true }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Existing Patient",
                            fontSize = 12.sp,
                            fontWeight = if (isExistingPatientMode) FontWeight.Bold else FontWeight.Normal,
                            color = if (isExistingPatientMode) MedRayBluePrimary else Slate600
                        )
                    }
                }

                HorizontalDivider(color = Slate100, modifier = Modifier.padding(vertical = 12.dp))

                if (isExistingPatientMode) {
                    // Existing Patient Lookup Flow
                    if (selectedExistingPatient == null) {
                        OutlinedTextField(
                            value = patientSearchQuery,
                            onValueChange = { patientSearchQuery = it },
                            placeholder = { Text("Search by name, phone, or UHID…", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MedRayBluePrimary, modifier = Modifier.size(18.dp)) },
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (filteredPatients.isEmpty()) {
                            Text(
                                text = "No registered patients match search.",
                                fontSize = 12.sp,
                                color = Slate400,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                filteredPatients.forEach { p ->
                                    Surface(
                                        color = Slate50,
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedExistingPatient = p }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.padding(10.dp)
                                        ) {
                                            Column {
                                                Text(text = p.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
                                                Text(text = "UHID: ${p.uhid} · 📞 +91 ${p.phone ?: "N/A"}", fontSize = 11.sp, color = Slate500)
                                            }
                                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Selected Patient Banner
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Column {
                                    Text(text = selectedExistingPatient!!.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MedRayBluePrimary)
                                    Text(text = "UHID: ${selectedExistingPatient!!.uhid} · 📞 +91 ${selectedExistingPatient!!.phone ?: "N/A"}", fontSize = 11.sp, color = Slate600)
                                }
                                TextButton(onClick = { selectedExistingPatient = null }) {
                                    Text("Change", fontSize = 12.sp, color = MedRayBluePrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Doctor Selector Dropdown
                        ExposedDropdownMenuBox(
                            expanded = doctorDropdownExpanded,
                            onExpandedChange = { doctorDropdownExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val selectedDocName = doctors.find { it.id == selectedDoctorId }?.fullName ?: "Select Doctor"
                            OutlinedTextField(
                                value = "Dr. $selectedDocName",
                                onValueChange = {},
                                readOnly = true,
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
                                        text = { Text("Dr. ${doc.fullName}") },
                                        onClick = {
                                            selectedDoctorId = doc.id
                                            doctorDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = chiefComplaint,
                            onValueChange = { chiefComplaint = it },
                            label = { Text("Chief Complaint") },
                            placeholder = { Text("e.g. Follow-up, Fever, Routine Consultation") },
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    // New Patient Registration Form
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Patient Full Name *") },
                            placeholder = { Text("e.g. Rahul Sharma") },
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { if (it.length <= 10) phone = it },
                            label = { Text("Mobile Number (WhatsApp) *") },
                            placeholder = { Text("10-digit mobile number") },
                            leadingIcon = { Text("🇮🇳 +91 ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate600, modifier = Modifier.padding(start = 12.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = age,
                                onValueChange = { if (it.length <= 3) age = it },
                                label = { Text("Age") },
                                placeholder = { Text("e.g. 35") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            // Gender Selector
                            Column(modifier = Modifier.weight(1.5f)) {
                                Text("Gender", fontSize = 11.sp, color = Slate600)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("MALE" to "M", "FEMALE" to "F", "OTHER" to "O").forEach { (g, label) ->
                                        FilterChip(
                                            selected = gender == g,
                                            onClick = { gender = g },
                                            label = { Text(label, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MedRayBluePrimary,
                                                selectedLabelColor = PureWhite
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Doctor Selector Dropdown
                        ExposedDropdownMenuBox(
                            expanded = doctorDropdownExpanded,
                            onExpandedChange = { doctorDropdownExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val selectedDocName = doctors.find { it.id == selectedDoctorId }?.fullName ?: "Select Doctor"
                            OutlinedTextField(
                                value = "Dr. $selectedDocName",
                                onValueChange = {},
                                readOnly = true,
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
                                        text = { Text("Dr. ${doc.fullName}") },
                                        onClick = {
                                            selectedDoctorId = doc.id
                                            doctorDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = chiefComplaint,
                            onValueChange = { chiefComplaint = it },
                            label = { Text("Chief Complaint") },
                            placeholder = { Text("e.g. Fever, headache for 2 days") },
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (isExistingPatientMode) {
                                selectedExistingPatient?.let { p ->
                                    val complaint = chiefComplaint.ifBlank { "Routine Consultation" }
                                    onAddExisting(p, selectedDoctorId, complaint)
                                    onDismiss()
                                }
                            } else {
                                if (fullName.isNotBlank() && phone.isNotBlank()) {
                                    val complaint = chiefComplaint.ifBlank { "Routine Consultation" }
                                    onRegister(fullName, phone, selectedDoctorId, complaint, age.toIntOrNull(), gender)
                                    onDismiss()
                                }
                            }
                        },
                        enabled = if (isExistingPatientMode) selectedExistingPatient != null && selectedDoctorId.isNotBlank()
                                  else fullName.isNotBlank() && phone.length >= 10 && selectedDoctorId.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isExistingPatientMode) "Add to Queue" else "Generate Token", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

