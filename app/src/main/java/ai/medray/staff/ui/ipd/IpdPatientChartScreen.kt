package ai.medray.staff.ui.ipd

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ai.medray.staff.data.model.formatIsoDateTimeLocal
import ai.medray.staff.data.network.*
import ai.medray.staff.ui.common.QuickFilterPill
import ai.medray.staff.ui.theme.*
import java.time.Duration
import java.time.Instant

/**
 * All of Screen.IpdPatientChart's tab data, hoisted into StaffAppNavHost
 * like every other screen's data in this app (see PatientsScreen/
 * BillingScreen — no screen fetches its own data). One bundle rather than
 * six separate hoisted vars.
 */
data class IpdChartData(
    val admission: IpdAdmission? = null,
    val vitals: List<IpdVitalsReading> = emptyList(),
    val notes: List<NursingNote> = emptyList(),
    val medicationOrders: List<MedicationOrder> = emptyList(),
    val investigations: List<InvestigationOrder> = emptyList(),
    val timeline: List<IpdTimelineEvent> = emptyList(),
    val isLoading: Boolean = false
)

private enum class ChartSection(val label: String) {
    OVERVIEW("Overview"), VITALS("Vitals"), NURSING("Nursing"), MEDICATIONS("Medications"),
    INVESTIGATIONS("Investigations"), TIMELINE("Timeline")
}

private fun lengthOfStayDays(admission: IpdAdmission): Long {
    val start = try { Instant.parse(admission.admissionDateTime) } catch (_: Exception) { return 0 }
    val end = admission.dischargedAt?.let { try { Instant.parse(it) } catch (_: Exception) { null } } ?: Instant.now()
    return Duration.between(start, end).toDays().coerceAtLeast(0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IpdPatientChartScreen(
    chart: IpdChartData,
    error: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRecordVitals: (CreateIpdVitalsRequest) -> Unit,
    onAddNursingNote: (CreateNursingNoteRequest) -> Unit,
    onCreateMedicationAdministration: (CreateMedicationAdministrationRequest) -> Unit,
    onUpdateMedicationAdministrationStatus: (String, UpdateMedicationAdministrationStatusRequest) -> Unit,
    onUpdateInvestigationStatus: (String, InvestigationOrderStatus) -> Unit,
    onAddInvestigationResult: (String, AddInvestigationResultRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    var section by remember { mutableStateOf(ChartSection.OVERVIEW) }
    var showVitalsDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var administeringOrderId by remember { mutableStateOf<String?>(null) }
    var actionSheetTarget by remember { mutableStateOf<MedicationAdministration?>(null) }
    var resultTarget by remember { mutableStateOf<InvestigationOrder?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chart.admission?.patient?.fullName ?: "Patient Chart") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh") }
                }
            )
        },
        containerColor = Slate50,
        modifier = modifier
    ) { padding ->
        if (chart.isLoading && chart.admission == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MedRayBluePrimary)
            }
            return@Scaffold
        }
        if (error != null && chart.admission == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(error, color = StatusErrorText, style = MaterialTheme.typography.bodyMedium)
            }
            return@Scaffold
        }
        val admission = chart.admission ?: return@Scaffold

        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Header
            Surface(color = PureWhite, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(admission.admissionNumber, style = MaterialTheme.typography.bodySmall, color = Slate500)
                        Surface(color = MedRayBlueLight, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                admission.status.name.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MedRayBluePrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("UHID ${admission.patient?.uhid ?: "—"} · ${currentBedLabel(admission)}", style = MaterialTheme.typography.bodySmall, color = Slate600)
                    admission.attendingDoctor?.fullName?.let {
                        Text("Attending: $it", style = MaterialTheme.typography.bodySmall, color = Slate600)
                    }
                    Text("LOS ${lengthOfStayDays(admission)}d", style = MaterialTheme.typography.bodySmall, color = Slate500)

                    // Discharge checklist gap (§47 Phase 3 bullet with zero
                    // spec elaboration, no backend model) — read-only status
                    // badge only, no interactive checklist. See the plan's
                    // own resolution note.
                    if (admission.status == AdmissionStatus.DISCHARGE_INITIATED) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(color = StatusWarningBg, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                "Discharge in progress",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = StatusWarningText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    } else if (admission.dischargedAt != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(color = Slate100, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                "Discharged",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Slate600,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Section switcher — no TabRow/tabbed-detail-screen exists
            // anywhere in this app today; this is a genuinely new pattern
            // here, built from the existing QuickFilterPill building block
            // rather than a bespoke one.
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).background(PureWhite).padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                ChartSection.entries.forEach { s ->
                    QuickFilterPill(label = s.label, isSelected = section == s, onClick = { section = s })
                }
            }

            Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                when (section) {
                    ChartSection.OVERVIEW -> OverviewPanel(admission)
                    ChartSection.VITALS -> VitalsPanel(chart.vitals, onRecordClick = { showVitalsDialog = true })
                    ChartSection.NURSING -> NursingPanel(chart.notes, onAddClick = { showNoteDialog = true })
                    ChartSection.MEDICATIONS -> MedicationsPanel(
                        orders = chart.medicationOrders,
                        onAdministerClick = { order -> administeringOrderId = order.id },
                        onActionClick = { admin -> actionSheetTarget = admin }
                    )
                    ChartSection.INVESTIGATIONS -> InvestigationsPanel(
                        investigations = chart.investigations,
                        onAdvanceStatus = { inv, next -> onUpdateInvestigationStatus(inv.id, next) },
                        onAddResultClick = { inv -> resultTarget = inv }
                    )
                    ChartSection.TIMELINE -> TimelinePanel(chart.timeline)
                }
            }
        }
    }

    if (showVitalsDialog) {
        IpdVitalsEntryDialog(
            patientName = chart.admission?.patient?.fullName ?: "Patient",
            onDismiss = { showVitalsDialog = false },
            onSave = { req ->
                onRecordVitals(req.copy(admissionId = chart.admission!!.id))
                showVitalsDialog = false
            }
        )
    }

    if (showNoteDialog) {
        NursingNoteEntryDialog(
            onDismiss = { showNoteDialog = false },
            onSave = { note ->
                onAddNursingNote(CreateNursingNoteRequest(admissionId = chart.admission!!.id, note = note))
                showNoteDialog = false
            }
        )
    }

    administeringOrderId?.let { orderId ->
        val order = chart.medicationOrders.firstOrNull { it.id == orderId }
        if (order != null) {
            ScheduleMedicationAdministrationDialog(
                order = order,
                onDismiss = { administeringOrderId = null },
                onSchedule = { req ->
                    onCreateMedicationAdministration(req)
                    administeringOrderId = null
                }
            )
        }
    }

    actionSheetTarget?.let { admin ->
        MedicationAdministrationActionSheet(
            administration = admin,
            onDismiss = { actionSheetTarget = null },
            onConfirm = { status, reason ->
                onUpdateMedicationAdministrationStatus(admin.id!!, UpdateMedicationAdministrationStatusRequest(status = status, reason = reason))
                actionSheetTarget = null
            }
        )
    }

    resultTarget?.let { inv ->
        InvestigationResultEntryDialog(
            investigation = inv,
            onDismiss = { resultTarget = null },
            onSave = { req ->
                onAddInvestigationResult(inv.id, req)
                resultTarget = null
            }
        )
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = PureWhite, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun EmptyRow(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = Slate400, modifier = Modifier.padding(vertical = 12.dp))
}

@Composable
private fun OverviewPanel(admission: IpdAdmission) {
    SectionCard {
        Text("Admission Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
        Spacer(modifier = Modifier.height(10.dp))
        Text("Reason for Admission", style = MaterialTheme.typography.labelSmall, color = Slate500)
        Text(admission.reasonForAdmission, style = MaterialTheme.typography.bodyMedium, color = Slate800)
        Spacer(modifier = Modifier.height(10.dp))
        Text("Provisional Diagnosis", style = MaterialTheme.typography.labelSmall, color = Slate500)
        Text(admission.provisionalDiagnosis, style = MaterialTheme.typography.bodyMedium, color = Slate800)
        // Doctor progress notes (Phase 4, Doctor App) — no such model exists
        // in the backend yet, so this stays an empty-state placeholder
        // rather than pretending to read something that isn't there.
        Spacer(modifier = Modifier.height(10.dp))
        Text("Doctor Progress Notes", style = MaterialTheme.typography.labelSmall, color = Slate500)
        Text("Will appear here once available.", style = MaterialTheme.typography.bodySmall, color = Slate400)
    }
}

@Composable
private fun VitalsPanel(readings: List<IpdVitalsReading>, onRecordClick: () -> Unit) {
    SectionCard {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Vitals", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
            Button(onClick = onRecordClick, colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary), shape = RoundedCornerShape(10.dp)) {
                Text("+ Record")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (readings.isEmpty()) {
            EmptyRow("No vitals recorded yet.")
        } else {
            readings.forEach { r ->
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Text(formatIsoDateTimeLocal(r.recordedAt), style = MaterialTheme.typography.labelSmall, color = Slate500)
                    Text(
                        listOfNotNull(
                            r.temperatureF?.let { "Temp $it°F" },
                            r.bloodPressure?.let { "BP $it" },
                            r.pulseBpm?.let { "Pulse $it" },
                            r.spo2Percent?.let { "SpO2 $it%" }
                        ).joinToString(" · ").ifBlank { "No readings" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate800
                    )
                }
                HorizontalDivider(color = Slate100)
            }
        }
    }
}

@Composable
private fun NursingPanel(notes: List<NursingNote>, onAddClick: () -> Unit) {
    SectionCard {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Nursing Notes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
            Button(onClick = onAddClick, colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary), shape = RoundedCornerShape(10.dp)) {
                Text("+ Add Note")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (notes.isEmpty()) {
            EmptyRow("No nursing notes yet.")
        } else {
            notes.forEach { n ->
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Text(n.note, style = MaterialTheme.typography.bodyMedium, color = Slate800)
                    Text(formatIsoDateTimeLocal(n.createdAt), style = MaterialTheme.typography.labelSmall, color = Slate400)
                }
                HorizontalDivider(color = Slate100)
            }
        }
        // Vitals/notes are append-only server-side (no PATCH/DELETE route)
        // — explained here rather than leaving a nurse looking for a
        // missing edit button.
        Text("Cannot be edited after saving — add a new note to correct an error.", style = MaterialTheme.typography.labelSmall, color = Slate400, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun MedicationsPanel(
    orders: List<MedicationOrder>,
    onAdministerClick: (MedicationOrder) -> Unit,
    onActionClick: (MedicationAdministration) -> Unit
) {
    SectionCard {
        Text("Medications", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
        Spacer(modifier = Modifier.height(10.dp))
        if (orders.isEmpty()) {
            EmptyRow("No medication orders yet.")
        } else {
            orders.forEach { order ->
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("${order.medicationName} — ${order.dose}, ${order.route}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Slate900)
                        TextButton(onClick = { onAdministerClick(order) }) { Text("+ Administer") }
                    }
                    Text(order.frequency, style = MaterialTheme.typography.bodySmall, color = Slate500)
                    order.administrations.forEach { admin ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                        ) {
                            Text(formatIsoDateTimeLocal(admin.scheduledAt), style = MaterialTheme.typography.bodySmall, color = Slate600)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(color = medicationStatusBg(admin.status), shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        admin.status.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = medicationStatusText(admin.status),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                if (admin.status != MedicationAdministrationStatus.ADMINISTERED &&
                                    admin.status != MedicationAdministrationStatus.REFUSED &&
                                    admin.status != MedicationAdministrationStatus.MISSED &&
                                    admin.status != MedicationAdministrationStatus.CANCELLED
                                ) {
                                    TextButton(onClick = { onActionClick(admin) }) { Text("Update") }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = Slate100)
            }
        }
    }
}

private fun medicationStatusBg(status: MedicationAdministrationStatus) = when (status) {
    MedicationAdministrationStatus.SCHEDULED -> Slate100
    MedicationAdministrationStatus.DUE -> StatusWarningBg
    MedicationAdministrationStatus.ADMINISTERED -> StatusSuccessBg
    MedicationAdministrationStatus.HELD, MedicationAdministrationStatus.REFUSED, MedicationAdministrationStatus.MISSED -> StatusErrorBg
    MedicationAdministrationStatus.CANCELLED -> Slate100
}

private fun medicationStatusText(status: MedicationAdministrationStatus) = when (status) {
    MedicationAdministrationStatus.SCHEDULED -> Slate600
    MedicationAdministrationStatus.DUE -> StatusWarningText
    MedicationAdministrationStatus.ADMINISTERED -> StatusSuccessText
    MedicationAdministrationStatus.HELD, MedicationAdministrationStatus.REFUSED, MedicationAdministrationStatus.MISSED -> StatusErrorText
    MedicationAdministrationStatus.CANCELLED -> Slate500
}

@Composable
private fun InvestigationsPanel(
    investigations: List<InvestigationOrder>,
    onAdvanceStatus: (InvestigationOrder, InvestigationOrderStatus) -> Unit,
    onAddResultClick: (InvestigationOrder) -> Unit
) {
    SectionCard {
        Text("Investigations", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
        Spacer(modifier = Modifier.height(10.dp))
        if (investigations.isEmpty()) {
            EmptyRow("No investigations yet.")
        } else {
            investigations.forEach { inv ->
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("${inv.testName} (${inv.investigationType})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Slate900)
                        Text(inv.status.name.replace("_", " "), style = MaterialTheme.typography.labelSmall, color = Slate500)
                    }
                    inv.resultText?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Slate700, modifier = Modifier.padding(top = 4.dp)) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                        when (inv.status) {
                            InvestigationOrderStatus.ORDERED -> TextButton(onClick = { onAdvanceStatus(inv, InvestigationOrderStatus.ACCEPTED) }) { Text("Accept") }
                            InvestigationOrderStatus.ACCEPTED -> TextButton(onClick = { onAdvanceStatus(inv, InvestigationOrderStatus.IN_PROGRESS) }) { Text("Start") }
                            else -> {}
                        }
                        if (inv.resultAt == null && inv.status != InvestigationOrderStatus.CANCELLED) {
                            TextButton(onClick = { onAddResultClick(inv) }) { Text("Add Result") }
                        }
                    }
                }
                HorizontalDivider(color = Slate100)
            }
        }
    }
}

@Composable
private fun TimelinePanel(events: List<IpdTimelineEvent>) {
    SectionCard {
        Text("Timeline", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
        Spacer(modifier = Modifier.height(10.dp))
        if (events.isEmpty()) {
            EmptyRow("Nothing recorded yet.")
        } else {
            events.forEach { e ->
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Text(e.summary, style = MaterialTheme.typography.bodyMedium, color = Slate800)
                    Text(formatIsoDateTimeLocal(e.occurredAt) + (e.actorEmail?.let { " · $it" } ?: ""), style = MaterialTheme.typography.labelSmall, color = Slate400)
                }
                HorizontalDivider(color = Slate100)
            }
        }
    }
}
