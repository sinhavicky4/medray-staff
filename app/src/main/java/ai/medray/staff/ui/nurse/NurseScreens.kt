package ai.medray.staff.ui.nurse

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ai.medray.staff.data.model.DocumentKind
import ai.medray.staff.data.model.QueueEntry
import ai.medray.staff.data.model.QueueStatus
import ai.medray.staff.data.model.Vitals
import ai.medray.staff.domain.VitalsSeverity
import ai.medray.staff.domain.VitalsValidator
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
fun NurseHomeScreen(
    queue: List<QueueEntry>,
    searchQuery: String,
    userName: String? = null,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onSearchChange: (String) -> Unit,
    onRecordVitalsClick: (QueueEntry) -> Unit,
    onViewPrescriptionClick: (QueueEntry) -> Unit = {},
    onScanDocumentClick: (QueueEntry) -> Unit,
    onStatusChange: (QueueEntry, QueueStatus) -> Unit,
    modifier: Modifier = Modifier
) {
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

    val filteredQueue = remember(queue, searchQuery, selectedStatusFilter) {
        queue.filter { entry ->
            val matchesFilter = selectedStatusFilter == null || entry.status == selectedStatusFilter
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                entry.patient?.fullName?.lowercase()?.contains(q) == true ||
                        entry.patient?.phone?.contains(q) == true ||
                        entry.patient?.uhid?.lowercase()?.contains(q) == true ||
                        entry.opdNumber.lowercase().contains(q) ||
                        entry.doctor?.fullName?.lowercase()?.contains(q) == true
            }
            matchesFilter && matchesSearch
        }.sortedWith(
            compareBy<QueueEntry> { entry ->
                // Priority 1: Completed / Cancelled records move down to the bottom
                when (entry.status) {
                    QueueStatus.COMPLETED -> 2
                    QueueStatus.CANCELLED -> 3
                    else -> 1 // Active triage/queue items at the top
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
        // 1. Clinical Header & Shift Status
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    val displayName = userName?.trim()?.split(" ")?.firstOrNull()?.ifBlank { "Nurse" } ?: "Nurse"
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

                Surface(
                    color = Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(modifier = Modifier.size(7.dp).background(Color(0xFF16A34A), CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Triage Active",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                    }
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
                        footer = "Ready for vitals",
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
                        footer = "Vitals in progress",
                        icon = Icons.Filled.Favorite,
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
                        footer = "Vitals synced",
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
                onValueChange = onSearchChange,
                placeholder = { Text("Search patient name, phone, or UHID…", fontSize = 13.sp, color = Slate400) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MedRayBluePrimary, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
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

        // 4. Quick Status Filter Pills (Horizontal Scroll)
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

        // 5. Patient Queue Cards
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
                            text = if (searchQuery.isNotBlank() || selectedStatusFilter != null) "No matching patients found" else "No patients in triage queue today",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate700
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "New arrivals checked-in at reception will appear automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }
                }
            }
        } else {
            items(filteredQueue, key = { it.id }) { entry ->
                NursePatientCard(
                    entry = entry,
                    onRecordVitalsClick = { onRecordVitalsClick(entry) },
                    onViewPrescriptionClick = { onViewPrescriptionClick(entry) },
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
    onViewPrescriptionClick: () -> Unit = {},
    onScanDocumentClick: () -> Unit,
    onMarkArrived: () -> Unit
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

            // Body: Patient Avatar + Name + Demographics + Doctor Assignment
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
                            if (!patient?.uhid.isNullOrBlank()) add("UHID: ${patient?.uhid}")
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
                if (entry.status == QueueStatus.COMPLETED) {
                    Button(
                        onClick = onViewPrescriptionClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("View Rx", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                } else {
                    Button(
                        onClick = onRecordVitalsClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (entry.vitals.hasAnyReading()) "Edit Vitals" else "Record Vitals",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                OutlinedButton(
                    onClick = onScanDocumentClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MedRayTealDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF99F6E4)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                    modifier = Modifier.weight(0.9f).fillMaxHeight()
                ) {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Scan Lab", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                if (entry.status == QueueStatus.WAITING) {
                    IconButton(
                        onClick = onMarkArrived,
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
 * Fast Vitals Entry Dialog modal with numeric numpads and instant BMI / clinical severity detection.
 */
@Composable
fun FastVitalsEntryDialog(
    initialVitals: Vitals,
    patientName: String,
    onDismiss: () -> Unit,
    onSave: (Vitals) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var bpSystolic by remember { mutableStateOf(initialVitals.vitalsBp?.split("/")?.getOrNull(0) ?: "") }
    var bpDiastolic by remember { mutableStateOf(initialVitals.vitalsBp?.split("/")?.getOrNull(1) ?: "") }
    var pulse by remember { mutableStateOf(initialVitals.vitalsPulseBpm?.toString() ?: "") }
    var spo2 by remember { mutableStateOf(initialVitals.vitalsSpo2?.toString() ?: "") }
    var temp by remember { mutableStateOf(initialVitals.vitalsTemperatureF?.toString() ?: "") }
    var weight by remember { mutableStateOf(initialVitals.vitalsWeightKg?.toString() ?: "") }
    var height by remember { mutableStateOf(initialVitals.vitalsHeightCm?.toString() ?: "") }

    val currentVitals = remember(bpSystolic, bpDiastolic, pulse, spo2, temp, weight, height) {
        val bp = if (bpSystolic.isNotBlank() && bpDiastolic.isNotBlank()) "$bpSystolic/$bpDiastolic" else null
        Vitals(
            vitalsBp = bp,
            vitalsPulseBpm = pulse.toIntOrNull(),
            vitalsSpo2 = spo2.toIntOrNull(),
            vitalsTemperatureF = temp.toDoubleOrNull(),
            vitalsWeightKg = weight.toDoubleOrNull(),
            vitalsHeightCm = height.toDoubleOrNull()
        )
    }

    val validation = remember(currentVitals) { VitalsValidator.evaluate(currentVitals) }

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
                // Modal Header
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Triage Vitals Entry",
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
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                    }
                }

                HorizontalDivider(color = Slate100, modifier = Modifier.padding(vertical = 12.dp))

                // Clinical Severity Banner
                if (validation.overallSeverity != VitalsSeverity.NORMAL && currentVitals.hasAnyReading()) {
                    val alertBg = if (validation.overallSeverity == VitalsSeverity.CRITICAL) Color(0xFFFEF2F2) else Color(0xFFFFFBEB)
                    val alertBorder = if (validation.overallSeverity == VitalsSeverity.CRITICAL) Color(0xFFFCA5A5) else Color(0xFFFDE68A)
                    val alertText = if (validation.overallSeverity == VitalsSeverity.CRITICAL) Color(0xFFDC2626) else Color(0xFFD97706)
                    val alertMsg = validation.bpMessage ?: validation.tempMessage ?: validation.pulseMessage ?: validation.spo2Message ?: "Abnormal reading detected"

                    Surface(
                        color = alertBg,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, alertBorder),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = alertText, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = alertMsg,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = alertText
                            )
                        }
                    }
                }

                // Input Grid
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Blood Pressure Row
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = bpSystolic,
                            onValueChange = { if (it.length <= 3) bpSystolic = it },
                            label = { Text("Sys (mmHg)", fontSize = 11.sp) },
                            placeholder = { Text("120") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = bpDiastolic,
                            onValueChange = { if (it.length <= 3) bpDiastolic = it },
                            label = { Text("Dia (mmHg)", fontSize = 11.sp) },
                            placeholder = { Text("80") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Pulse & SpO2 Row
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = pulse,
                            onValueChange = { if (it.length <= 3) pulse = it },
                            label = { Text("Pulse (bpm)", fontSize = 11.sp) },
                            placeholder = { Text("72") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = spo2,
                            onValueChange = { if (it.length <= 3) spo2 = it },
                            label = { Text("SpO2 (%)", fontSize = 11.sp) },
                            placeholder = { Text("98") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Temperature & Weight Row
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = temp,
                            onValueChange = { if (it.length <= 5) temp = it },
                            label = { Text("Temp (°F)", fontSize = 11.sp) },
                            placeholder = { Text("98.6") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { if (it.length <= 5) weight = it },
                            label = { Text("Weight (kg)", fontSize = 11.sp) },
                            placeholder = { Text("65") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
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
                            keyboardController?.hide()
                            onSave(currentVitals)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Vitals", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
