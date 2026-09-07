package ai.medray.staff.ui.ipd

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ai.medray.staff.data.network.*
import ai.medray.staff.domain.VitalsSeverity
import ai.medray.staff.domain.VitalsValidator
import ai.medray.staff.ui.theme.*
import java.time.Instant

@Composable
private fun DialogShell(title: String, subtitle: String? = null, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PureWhite,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 480.dp).imePadding().padding(vertical = 16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Slate900)
                        subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Slate500) }
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate500) }
                }
                HorizontalDivider(color = Slate100, modifier = Modifier.padding(vertical = 12.dp))
                content()
            }
        }
    }
}

/**
 * Mirrors ui/nurse/NurseScreens.kt's FastVitalsEntryDialog pattern (reactive
 * state, live severity validation) but for IpdVitalsReading's own field set
 * — bloodGlucose/painScore/intakeMl/outputMl instead of OPD's height, via
 * VitalsValidator's new evaluate(bp, temp, pulse, spo2) overload.
 */
@Composable
fun IpdVitalsEntryDialog(patientName: String, onDismiss: () -> Unit, onSave: (CreateIpdVitalsRequest) -> Unit) {
    var systolic by remember { mutableStateOf("") }
    var diastolic by remember { mutableStateOf("") }
    var pulse by remember { mutableStateOf("") }
    var spo2 by remember { mutableStateOf("") }
    var temp by remember { mutableStateOf("") }
    var respRate by remember { mutableStateOf("") }
    var bloodGlucose by remember { mutableStateOf("") }
    var painScore by remember { mutableStateOf("") }

    val bp = remember(systolic, diastolic) { if (systolic.isNotBlank() && diastolic.isNotBlank()) "$systolic/$diastolic" else null }
    val evaluation = remember(bp, temp, pulse, spo2) {
        VitalsValidator.evaluate(bloodPressure = bp, temperatureF = temp.toDoubleOrNull(), pulseBpm = pulse.toIntOrNull(), spo2Percent = spo2.toIntOrNull())
    }

    DialogShell(title = "Record Vitals", subtitle = patientName, onDismiss = onDismiss) {
        if (evaluation.overallSeverity != VitalsSeverity.NORMAL) {
            val alertText = if (evaluation.overallSeverity == VitalsSeverity.CRITICAL) StatusErrorText else StatusWarningText
            val alertBg = if (evaluation.overallSeverity == VitalsSeverity.CRITICAL) StatusErrorBg else StatusWarningBg
            Surface(color = alertBg, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = alertText, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        evaluation.bpMessage ?: evaluation.tempMessage ?: evaluation.pulseMessage ?: evaluation.spo2Message ?: "Abnormal reading detected",
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = alertText
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = systolic, onValueChange = { if (it.length <= 3) systolic = it }, label = { Text("Sys", fontSize = 11.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = diastolic, onValueChange = { if (it.length <= 3) diastolic = it }, label = { Text("Dia", fontSize = 11.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = pulse, onValueChange = { if (it.length <= 3) pulse = it }, label = { Text("Pulse (bpm)", fontSize = 11.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = spo2, onValueChange = { if (it.length <= 3) spo2 = it }, label = { Text("SpO2 (%)", fontSize = 11.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = temp, onValueChange = { if (it.length <= 5) temp = it }, label = { Text("Temp (°F)", fontSize = 11.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = respRate, onValueChange = { if (it.length <= 3) respRate = it }, label = { Text("Resp Rate", fontSize = 11.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = bloodGlucose, onValueChange = { if (it.length <= 5) bloodGlucose = it }, label = { Text("Glucose", fontSize = 11.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = painScore, onValueChange = { if (it.length <= 2) painScore = it }, label = { Text("Pain (0-10)", fontSize = 11.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = {
                    onSave(
                        CreateIpdVitalsRequest(
                            admissionId = "", // caller (IpdPatientChartScreen) fills this in
                            temperatureF = temp.toDoubleOrNull(),
                            bloodPressure = bp,
                            pulseBpm = pulse.toIntOrNull(),
                            respRatePerMin = respRate.toIntOrNull(),
                            spo2Percent = spo2.toIntOrNull(),
                            bloodGlucose = bloodGlucose.toDoubleOrNull(),
                            painScore = painScore.toIntOrNull()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) { Text("Save Vitals", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun NursingNoteEntryDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var note by remember { mutableStateOf("") }
    DialogShell(title = "Add Nursing Note", onDismiss = onDismiss) {
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            placeholder = { Text("Patient resting comfortably…") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = { if (note.isNotBlank()) onSave(note.trim()) },
                enabled = note.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) { Text("Save Note", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun ScheduleMedicationAdministrationDialog(order: MedicationOrder, onDismiss: () -> Unit, onSchedule: (CreateMedicationAdministrationRequest) -> Unit) {
    var dose by remember { mutableStateOf(order.dose) }
    var route by remember { mutableStateOf(order.route) }
    DialogShell(title = "Schedule Administration", subtitle = order.medicationName, onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(value = dose, onValueChange = { dose = it }, label = { Text("Dose") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = route, onValueChange = { route = it }, label = { Text("Route") }, modifier = Modifier.fillMaxWidth())
            Text("Scheduled for now — update the dose/route above if this administration differs from the standing order.", style = MaterialTheme.typography.labelSmall, color = Slate400)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = {
                    onSchedule(
                        CreateMedicationAdministrationRequest(
                            medicationOrderId = order.id,
                            scheduledAt = Instant.now().toString(),
                            dose = dose,
                            route = route
                        )
                    )
                },
                enabled = dose.isNotBlank() && route.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) { Text("Schedule", fontWeight = FontWeight.Bold) }
        }
    }
}

private val NEXT_STATUSES: Map<MedicationAdministrationStatus, List<MedicationAdministrationStatus>> = mapOf(
    MedicationAdministrationStatus.SCHEDULED to listOf(MedicationAdministrationStatus.DUE, MedicationAdministrationStatus.CANCELLED),
    MedicationAdministrationStatus.DUE to listOf(
        MedicationAdministrationStatus.ADMINISTERED, MedicationAdministrationStatus.HELD,
        MedicationAdministrationStatus.REFUSED, MedicationAdministrationStatus.MISSED, MedicationAdministrationStatus.CANCELLED
    ),
    MedicationAdministrationStatus.HELD to listOf(MedicationAdministrationStatus.DUE, MedicationAdministrationStatus.CANCELLED)
)
private val REASON_REQUIRED = setOf(MedicationAdministrationStatus.HELD, MedicationAdministrationStatus.REFUSED, MedicationAdministrationStatus.MISSED)

/** Online-only, no offline queueing — see IpdRepository's own doc comment for why. */
@Composable
fun MedicationAdministrationActionSheet(administration: MedicationAdministration, onDismiss: () -> Unit, onConfirm: (MedicationAdministrationStatus, String?) -> Unit) {
    var pendingStatus by remember { mutableStateOf<MedicationAdministrationStatus?>(null) }
    var reason by remember { mutableStateOf("") }
    val options = NEXT_STATUSES[administration.status].orEmpty()

    DialogShell(title = "Update Administration", subtitle = "${administration.dose}, ${administration.route}", onDismiss = onDismiss) {
        if (pendingStatus == null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { status ->
                    OutlinedButton(onClick = { pendingStatus = status }, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(status.name)
                    }
                }
            }
        } else {
            val status = pendingStatus!!
            if (status in REASON_REQUIRED) {
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { pendingStatus = null }, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) { Text("Back") }
                Button(
                    onClick = { onConfirm(status, reason.ifBlank { null }) },
                    enabled = status !in REASON_REQUIRED || reason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("Confirm ${status.name}") }
            }
        }
    }
}

@Composable
fun InvestigationResultEntryDialog(investigation: InvestigationOrder, onDismiss: () -> Unit, onSave: (AddInvestigationResultRequest) -> Unit) {
    var resultText by remember { mutableStateOf("") }
    DialogShell(title = "Add Result", subtitle = investigation.testName, onDismiss = onDismiss) {
        Text(
            "Structured text only here — to attach a scanned PDF or photo instead, use the patient's Documents panel (reuses the same upload flow already used for reports), then attach it to this investigation.",
            style = MaterialTheme.typography.labelSmall, color = Slate400
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(value = resultText, onValueChange = { resultText = it }, minLines = 4, placeholder = { Text("WBC 7.2, Hb 13.1 — normal") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = { onSave(AddInvestigationResultRequest(resultType = InvestigationResultType.STRUCTURED, resultText = resultText.trim())) },
                enabled = resultText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) { Text("Save Result", fontWeight = FontWeight.Bold) }
        }
    }
}
