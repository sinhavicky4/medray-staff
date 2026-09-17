package ai.medray.staff.ui.ipd

// IPD (Inpatient Department) — Phase 3 staff app, Nurse-scoped (spec §30:
// STAFF APP = Nurse). This app has no route-argument navigation anywhere
// (every detail view takes its target from a hoisted nullable state var set
// right before navigate() — see StaffNavGraph.kt's rxTargetEntry/
// uploadDocTargetPatient) — the Patient Chart follows that same idiom via
// ipdChartTargetAdmissionId rather than introducing nav-arguments as a
// first-of-its-kind mechanism in this codebase.

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.medray.staff.data.network.AdmissionStatus
import ai.medray.staff.data.network.IpdAdmission
import ai.medray.staff.data.network.IpdBed
import ai.medray.staff.data.repository.IpdOutboxSyncStatus
import ai.medray.staff.domain.IpdTaskListItem
import ai.medray.staff.domain.IpdTaskListDerivation
import ai.medray.staff.domain.IpdTaskType
import ai.medray.staff.ui.common.MedRayPullRefreshBox
import ai.medray.staff.ui.common.QuickFilterPill
import ai.medray.staff.ui.common.StatCard
import ai.medray.staff.ui.theme.*

private fun admissionStatusLabel(status: AdmissionStatus): String = when (status) {
    AdmissionStatus.ADMISSION_REQUESTED -> "Requested"
    AdmissionStatus.ADMISSION_APPROVED -> "Approved"
    AdmissionStatus.BED_RESERVED -> "Bed Reserved"
    AdmissionStatus.ADMITTED -> "Admitted"
    AdmissionStatus.INPATIENT -> "Inpatient"
    AdmissionStatus.DISCHARGE_INITIATED -> "Discharge Initiated"
    AdmissionStatus.DISCHARGED -> "Discharged"
    AdmissionStatus.CANCELLED -> "Cancelled"
    AdmissionStatus.TRANSFERRED_OUT -> "Transferred Out"
    AdmissionStatus.LAMA -> "LAMA"
    AdmissionStatus.ABSCONDED -> "Absconded"
    AdmissionStatus.DECEASED -> "Deceased"
}

/** "Bed (Room, Ward)" or "Not yet assigned" — same helper every IPD screen needs. */
fun currentBedLabel(admission: IpdAdmission): String {
    val bed = admission.currentBed ?: return "Not yet assigned"
    return "${bed.label} (${bed.room.name}, ${bed.room.ward.name})"
}

@Composable
private fun AdmissionRow(admission: IpdAdmission, onClick: () -> Unit) {
    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = admission.patient?.fullName ?: "Patient",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(color = StatusInfoBg, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = admissionStatusLabel(admission.status),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = StatusInfoText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "UHID ${admission.patient?.uhid ?: "—"} · ${currentBedLabel(admission)}",
                style = MaterialTheme.typography.bodySmall,
                color = Slate500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            admission.attendingDoctor?.fullName?.let {
                Text(text = "Attending: $it", style = MaterialTheme.typography.bodySmall, color = Slate400)
            }
        }
    }
}

/**
 * Ward Home's outbox sync-status banner — a nurse relying on the offline
 * fallback (vitals/nursing note/medication administration writes, see
 * OutboxManager) previously had no way to tell a write was still pending,
 * or had permanently failed (OutboxSyncWorker.MAX_ATTEMPTS), short of
 * checking the static "Outbox Active" line on the Profile screen. Only
 * rendered by the caller when outboxStatus.hasAnything is true.
 */
@Composable
private fun OutboxSyncBanner(status: IpdOutboxSyncStatus, onDismissFailed: (() -> Unit)? = null) {
    val hasFailed = status.failedCount > 0
    val bg = if (hasFailed) StatusErrorBg else StatusWarningBg
    val border = if (hasFailed) StatusErrorBorder else StatusWarningBorder
    val text = if (hasFailed) StatusErrorText else StatusWarningText
    val message = when {
        hasFailed && status.pendingCount > 0 ->
            "${status.pendingCount} change${if (status.pendingCount == 1) "" else "s"} waiting to sync · ${status.failedCount} failed to sync"
        hasFailed -> "${status.failedCount} change${if (status.failedCount == 1) "" else "s"} failed to sync"
        else -> "${status.pendingCount} change${if (status.pendingCount == 1) "" else "s"} waiting to sync"
    }
    Surface(color = bg, shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, border), modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                Icon(
                    if (hasFailed) Icons.Filled.ErrorOutline else Icons.Filled.CloudSync,
                    contentDescription = null,
                    tint = text,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(message, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = text)
            }
            if (hasFailed && onDismissFailed != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Dismiss",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = text,
                    modifier = Modifier.clickable { onDismissFailed() }
                )
            }
        }
    }
}

/**
 * Ward Home / Patient List / Task List's load-failure banner — refreshAdmissions()
 * previously masked every failure as a false "no inpatients" empty state; it
 * now surfaces a genuine failure (no cached data to fall back to) through
 * this. Structurally identical to OutboxSyncBanner's failed branch, just a
 * single message instead of a pending/failed distinction.
 */
@Composable
private fun ErrorBanner(message: String) {
    Surface(color = StatusErrorBg, shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, StatusErrorBorder), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = StatusErrorText, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = StatusErrorText)
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Outlined.LocalHospital, contentDescription = null, tint = Slate300, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate700)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Slate400)
        }
    }
}

/**
 * "My Ward" home (spec §17): Patients / Vitals Due / Medication Due /
 * Pending Tasks / Investigation Tasks tiles. Vitals Due has no backend
 * signal at all (see IpdTaskListDerivation's own doc comment) — it's a
 * heuristic, not a tracked deadline; shown the same as the others but
 * should read as a suggestion, not certainty.
 */
@Composable
fun IpdWardHomeScreen(
    userName: String?,
    admissions: List<IpdAdmission>,
    tasks: List<IpdTaskListItem>,
    outboxStatus: IpdOutboxSyncStatus = IpdOutboxSyncStatus(0, 0),
    errorMessage: String? = null,
    isLoading: Boolean,
    beds: List<IpdBed> = emptyList(),
    bedsLoading: Boolean = false,
    onRefresh: () -> Unit,
    onPatientsClick: () -> Unit,
    onTaskListClick: () -> Unit,
    onPatientClick: (IpdAdmission) -> Unit,
    onUpdateBedStatus: ((bedId: String, status: String) -> Unit)? = null,
    onNewAdmissionClick: (() -> Unit)? = null,
    onDismissFailedOutbox: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val counts = remember(admissions, tasks) { IpdTaskListDerivation.tileCounts(admissions.size, tasks) }

    MedRayPullRefreshBox(
        isRefreshing = isLoading,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize().background(Slate50)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "My Ward",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        userName?.let {
                            Text("Welcome back, ${it.trim().split(" ").firstOrNull() ?: it}", style = MaterialTheme.typography.bodySmall, color = Slate500)
                        }
                    }
                    if (onNewAdmissionClick != null) {
                        Button(
                            onClick = onNewAdmissionClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Admit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = PureWhite)
                        }
                    }
                }
            }

            if (errorMessage != null) {
                item { ErrorBanner(errorMessage) }
            }

            if (outboxStatus.hasAnything) {
                item { OutboxSyncBanner(outboxStatus, onDismissFailedOutbox) }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        StatCard(
                            title = "Patients",
                            value = "${counts.patients}",
                            footer = "Currently inpatient",
                            icon = Icons.Filled.People,
                            iconBg = MedRayBlueLight,
                            iconTint = MedRayBluePrimary,
                            onClick = onPatientsClick,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        StatCard(
                            title = "Vitals Due",
                            value = "${counts.vitalsDue}",
                            footer = "~4h since last reading",
                            icon = Icons.Filled.MonitorHeart,
                            iconBg = StatusWarningBg,
                            iconTint = StatusWarningText,
                            onClick = onTaskListClick,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        StatCard(
                            title = "Medication Due",
                            value = "${counts.medicationDue}",
                            footer = "Scheduled or due now",
                            icon = Icons.Filled.Medication,
                            iconBg = StatusErrorBg,
                            iconTint = StatusErrorText,
                            onClick = onTaskListClick,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        StatCard(
                            title = "Investigations",
                            value = "${counts.investigationTasks}",
                            footer = "Awaiting result",
                            icon = Icons.Filled.Science,
                            iconBg = StatusInfoBg,
                            iconTint = StatusInfoText,
                            onClick = onTaskListClick,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }

            item {
                Surface(
                    color = MedRayBluePrimary,
                    shape = RoundedCornerShape(14.dp),
                    onClick = onTaskListClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Column {
                            Text("Pending Tasks", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = PureWhite)
                            Text("${counts.pendingTasks} across the ward", style = MaterialTheme.typography.bodySmall, color = PureWhite.copy(alpha = 0.85f))
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = PureWhite)
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Patients", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
                    Text(
                        "View all →",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MedRayBluePrimary,
                        modifier = Modifier.clickable(onClick = onPatientsClick)
                    )
                }
            }

            if (admissions.isEmpty()) {
                item { EmptyState("No inpatients right now", "Patients admitted via the front desk will appear here automatically.") }
            } else {
                items(admissions.take(5), key = { it.id }) { admission ->
                    AdmissionRow(admission = admission, onClick = { onPatientClick(admission) })
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                BedBoardSection(
                    beds = beds,
                    bedsLoading = bedsLoading,
                    onUpdateBedStatus = onUpdateBedStatus
                )
            }
        }
    }
}

@Composable
fun BedBoardSection(
    beds: List<IpdBed>,
    bedsLoading: Boolean,
    onUpdateBedStatus: ((bedId: String, nextStatus: String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val availableCount = remember(beds) { beds.count { it.status.equals("AVAILABLE", ignoreCase = true) } }
    val occupiedCount = remember(beds) { beds.count { it.status.equals("OCCUPIED", ignoreCase = true) } }
    val cleaningCount = remember(beds) { beds.count { it.status.equals("CLEANING", ignoreCase = true) } }
    val maintenanceCount = remember(beds) { beds.count { it.status.equals("MAINTENANCE", ignoreCase = true) } }

    val filteredBeds = remember(beds, selectedFilter) {
        when (selectedFilter) {
            "AVAILABLE" -> beds.filter { it.status.equals("AVAILABLE", ignoreCase = true) }
            "OCCUPIED" -> beds.filter { it.status.equals("OCCUPIED", ignoreCase = true) }
            "CLEANING" -> beds.filter { it.status.equals("CLEANING", ignoreCase = true) }
            "MAINTENANCE" -> beds.filter { it.status.equals("MAINTENANCE", ignoreCase = true) }
            else -> beds
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Hotel, contentDescription = null, tint = Slate700, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bed Board & Housekeeping",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            }
            if (cleaningCount > 0) {
                Surface(color = StatusWarningBg, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = "$cleaningCount needs cleaning",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = StatusWarningText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Status Filter Chips Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            QuickFilterPill(
                label = "All (${beds.size})",
                isSelected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" }
            )
            QuickFilterPill(
                label = "Available ($availableCount)",
                isSelected = selectedFilter == "AVAILABLE",
                onClick = { selectedFilter = "AVAILABLE" }
            )
            QuickFilterPill(
                label = "Cleaning ($cleaningCount)",
                isSelected = selectedFilter == "CLEANING",
                onClick = { selectedFilter = "CLEANING" }
            )
            QuickFilterPill(
                label = "Occupied ($occupiedCount)",
                isSelected = selectedFilter == "OCCUPIED",
                onClick = { selectedFilter = "OCCUPIED" }
            )
            if (maintenanceCount > 0) {
                QuickFilterPill(
                    label = "Maint ($maintenanceCount)",
                    isSelected = selectedFilter == "MAINTENANCE",
                    onClick = { selectedFilter = "MAINTENANCE" }
                )
            }
        }

        if (bedsLoading && beds.isEmpty()) {
            Text("Loading bed board…", style = MaterialTheme.typography.bodySmall, color = Slate400)
        } else if (filteredBeds.isEmpty()) {
            Surface(
                color = PureWhite,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = if (beds.isEmpty()) "No beds configured for this clinic." else "No beds matching filter '$selectedFilter'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredBeds.forEach { bed ->
                    BedHousekeepingCard(
                        bed = bed,
                        onUpdateStatus = onUpdateBedStatus
                    )
                }
            }
        }
    }
}

@Composable
private fun BedHousekeepingCard(
    bed: IpdBed,
    onUpdateStatus: ((bedId: String, nextStatus: String) -> Unit)?
) {
    val isAvailable = bed.status.equals("AVAILABLE", ignoreCase = true)
    val isOccupied = bed.status.equals("OCCUPIED", ignoreCase = true)
    val isCleaning = bed.status.equals("CLEANING", ignoreCase = true)
    val isMaintenance = bed.status.equals("MAINTENANCE", ignoreCase = true)

    val (statusBg, statusText, statusBorder) = when {
        isAvailable -> Triple(StatusSuccessBg, StatusSuccessText, StatusSuccessBorder)
        isOccupied -> Triple(StatusInfoBg, StatusInfoText, StatusInfoBorder)
        isCleaning -> Triple(StatusWarningBg, StatusWarningText, StatusWarningBorder)
        isMaintenance -> Triple(StatusErrorBg, StatusErrorText, StatusErrorBorder)
        else -> Triple(Slate100, Slate700, Slate300)
    }

    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isCleaning) StatusWarningBorder else Slate200),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isOccupied) Icons.Filled.Hotel else if (isCleaning) Icons.Filled.CleaningServices else Icons.Outlined.Hotel,
                        contentDescription = null,
                        tint = statusText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = bed.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "${bed.room.name} · ${bed.room.ward.name}${bed.dailyRate?.let { " · ₹${it.toInt()}/day" } ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }
                }

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusBorder)
                ) {
                    Text(
                        text = bed.status.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Quick Lifecycle Action Buttons
            if (onUpdateStatus != null) {
                when {
                    isCleaning -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { onUpdateStatus(bed.id, "AVAILABLE") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp), tint = PureWhite)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sanitized & Ready", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                            }
                            OutlinedButton(
                                onClick = { onUpdateStatus(bed.id, "MAINTENANCE") },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate300)
                            ) {
                                Text("Maintenance", fontSize = 12.sp, color = Slate700)
                            }
                        }
                    }
                    isAvailable -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { onUpdateStatus(bed.id, "MAINTENANCE") },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate300),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(12.dp), tint = Slate600)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mark Maintenance", fontSize = 11.sp, color = Slate600)
                            }
                        }
                    }
                    isMaintenance -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { onUpdateStatus(bed.id, "AVAILABLE") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp), tint = PureWhite)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Fixed & Ready", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IpdPatientListScreen(
    admissions: List<IpdAdmission>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    errorMessage: String? = null,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onPatientClick: (IpdAdmission) -> Unit,
    onNewAdmissionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val filtered = remember(admissions, searchQuery) {
        if (searchQuery.isBlank()) admissions else {
            val q = searchQuery.trim().lowercase()
            admissions.filter {
                it.patient?.fullName?.lowercase()?.contains(q) == true ||
                    it.patient?.uhid?.lowercase()?.contains(q) == true ||
                    it.admissionNumber.lowercase().contains(q)
            }
        }
    }

    MedRayPullRefreshBox(isRefreshing = isLoading, onRefresh = onRefresh, modifier = modifier.fillMaxSize().background(Slate50)) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Inpatients", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Slate900)
                    if (onNewAdmissionClick != null) {
                        Button(
                            onClick = onNewAdmissionClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Admit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = PureWhite)
                        }
                    }
                }
            }
            if (errorMessage != null) {
                item { ErrorBanner(errorMessage) }
            }
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search patient, UHID, or admission #…", fontSize = 13.sp, color = Slate400) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MedRayBluePrimary, modifier = Modifier.size(20.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = PureWhite, unfocusedContainerColor = PureWhite),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (filtered.isEmpty()) {
                item { EmptyState(if (searchQuery.isBlank()) "No inpatients right now" else "No matching patients", "Try a different search.") }
            } else {
                items(filtered, key = { it.id }) { admission ->
                    AdmissionRow(admission = admission, onClick = { onPatientClick(admission) })
                }
            }
        }
    }
}

@Composable
private fun TaskRow(task: IpdTaskListItem, onClick: () -> Unit) {
    val (bg, text, icon) = when (task.taskType) {
        IpdTaskType.VITALS_DUE -> Triple(StatusWarningBg, StatusWarningText, Icons.Filled.MonitorHeart)
        IpdTaskType.MEDICATION_DUE -> Triple(StatusErrorBg, StatusErrorText, Icons.Filled.Medication)
        IpdTaskType.INVESTIGATION_PENDING -> Triple(StatusInfoBg, StatusInfoText, Icons.Filled.Science)
    }
    Surface(color = PureWhite, shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Slate200), onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Box(modifier = Modifier.size(36.dp).background(bg, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = text, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(task.patientName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Slate900)
                Text(task.label, style = MaterialTheme.typography.bodySmall, color = Slate500)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Slate300)
        }
    }
}

/** Derived, read-only — tapping a task navigates to that patient's chart to act on it (record vitals, administer, add a result). */
@Composable
fun IpdTaskListScreen(
    tasks: List<IpdTaskListItem>,
    errorMessage: String? = null,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onTaskClick: (IpdTaskListItem) -> Unit,
    modifier: Modifier = Modifier
) {
    MedRayPullRefreshBox(isRefreshing = isLoading, onRefresh = onRefresh, modifier = modifier.fillMaxSize().background(Slate50)) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Column {
                    Text("Pending Tasks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Slate900)
                    Text("${tasks.size} across the ward", style = MaterialTheme.typography.bodySmall, color = Slate500)
                }
            }
            if (errorMessage != null) {
                item { ErrorBanner(errorMessage) }
            }
            if (tasks.isEmpty()) {
                item { EmptyState("All caught up", "No pending vitals, medications, or investigations right now.") }
            } else {
                items(tasks, key = { "${it.admissionId}-${it.taskType}-${it.label}" }) { task ->
                    TaskRow(task = task, onClick = { onTaskClick(task) })
                }
            }
        }
    }
}
