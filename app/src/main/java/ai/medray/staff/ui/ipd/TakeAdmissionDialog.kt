package ai.medray.staff.ui.ipd

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ai.medray.staff.data.model.DoctorSummary
import ai.medray.staff.data.model.Patient
import ai.medray.staff.data.network.IpdBed
import ai.medray.staff.data.network.RegisterPatientRequest
import ai.medray.staff.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeAdmissionDialog(
    doctors: List<DoctorSummary>,
    availableBeds: List<IpdBed> = emptyList(),
    existingPatients: List<Patient> = emptyList(),
    isSubmitting: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onAdmit: (
        patientId: String?,
        newPatientReq: RegisterPatientRequest?,
        admittingDoctorId: String,
        attendingDoctorId: String,
        reason: String,
        diagnosis: String,
        source: String,
        type: String,
        bedId: String?,
        attendantName: String?,
        attendantPhone: String?,
        paymentType: String
    ) -> Unit
) {
    var isExistingPatientMode by remember { mutableStateOf(existingPatients.isNotEmpty()) }
    var selectedExistingPatient by remember { mutableStateOf<Patient?>(null) }
    var patientSearchQuery by remember { mutableStateOf("") }

    // New patient fields
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("MALE") }

    // Doctor selection
    var admittingDoctorId by remember { mutableStateOf(doctors.firstOrNull()?.id ?: "") }
    var attendingDoctorId by remember { mutableStateOf(doctors.firstOrNull()?.id ?: "") }
    var admittingDoctorDropdownExpanded by remember { mutableStateOf(false) }
    var attendingDoctorDropdownExpanded by remember { mutableStateOf(false) }

    // Clinical Details
    var reasonForAdmission by remember { mutableStateOf("") }
    var provisionalDiagnosis by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf("DIRECT") }
    var selectedType by remember { mutableStateOf("ELECTIVE") }

    // Bed selection
    var selectedBedId by remember { mutableStateOf<String?>(null) }
    var bedDropdownExpanded by remember { mutableStateOf(false) }

    // Attendant details
    var attendantName by remember { mutableStateOf("") }
    var attendantPhone by remember { mutableStateOf("") }
    var paymentType by remember { mutableStateOf("SELF_PAY") }

    var localError by remember { mutableStateOf<String?>(null) }

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

    val selectedBed = remember(selectedBedId, availableBeds) {
        availableBeds.firstOrNull { it.id == selectedBedId }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PureWhite,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 520.dp)
                .fillMaxHeight(0.90f)
                .imePadding()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                            text = "New Inpatient Admission",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Admit patient to IPD ward & allocate bed",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Error Banner
                (errorMessage ?: localError)?.let { err ->
                    Surface(
                        color = StatusErrorBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusErrorText,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Patient Selection Mode Tabs
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
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isExistingPatientMode) {
                    // Search & Select Existing Patient
                    if (selectedExistingPatient == null) {
                        OutlinedTextField(
                            value = patientSearchQuery,
                            onValueChange = { patientSearchQuery = it },
                            placeholder = { Text("Search by name, phone, or UHID...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (filteredPatients.isEmpty()) {
                            Text(
                                text = if (patientSearchQuery.isBlank()) "No recent patients" else "No matching patients found",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            filteredPatients.forEach { p ->
                                Surface(
                                    color = Slate50,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                                    onClick = { selectedExistingPatient = p },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(p.fullName, fontWeight = FontWeight.SemiBold, color = Slate900)
                                            Text("UHID: ${p.uhid} · ${p.phone ?: "No phone"}", fontSize = 11.sp, color = Slate500)
                                        }
                                        Icon(Icons.Default.CheckCircleOutline, contentDescription = "Select", tint = MedRayBluePrimary)
                                    }
                                }
                            }
                        }
                    } else {
                        // Selected Patient Card
                        Surface(
                            color = MedRayBluePrimary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MedRayBluePrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedExistingPatient!!.fullName,
                                        fontWeight = FontWeight.Bold,
                                        color = MedRayBluePrimary
                                    )
                                    Text(
                                        text = "UHID: ${selectedExistingPatient!!.uhid} · Phone: ${selectedExistingPatient!!.phone ?: "—"}",
                                        fontSize = 11.sp,
                                        color = Slate600
                                    )
                                }
                                IconButton(onClick = { selectedExistingPatient = null }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Change Patient", tint = MedRayBluePrimary)
                                }
                            }
                        }
                    }
                } else {
                    // New Patient Registration Fields
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = age,
                            onValueChange = { age = it },
                            label = { Text("Age") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Gender selection
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text("Gender", fontSize = 11.sp, color = Slate600)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("MALE" to "M", "FEMALE" to "F", "OTHER" to "O").forEach { (gVal, label) ->
                                    FilterChip(
                                        selected = gender == gVal,
                                        onClick = { gender = gVal },
                                        label = { Text(label, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 16.dp))

                // Section: Doctors
                Text("Doctor Assignment", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
                Spacer(modifier = Modifier.height(8.dp))

                // Admitting Doctor Dropdown
                ExposedDropdownMenuBox(
                    expanded = admittingDoctorDropdownExpanded,
                    onExpandedChange = { admittingDoctorDropdownExpanded = !admittingDoctorDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = doctors.firstOrNull { it.id == admittingDoctorId }?.fullName ?: "Select Admitting Doctor",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Admitting Doctor *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = admittingDoctorDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = admittingDoctorDropdownExpanded,
                        onDismissRequest = { admittingDoctorDropdownExpanded = false }
                    ) {
                        doctors.forEach { doc ->
                            DropdownMenuItem(
                                text = { Text("${doc.fullName} (${doc.specialization ?: "General"})") },
                                onClick = {
                                    admittingDoctorId = doc.id
                                    if (attendingDoctorId.isBlank()) attendingDoctorId = doc.id
                                    admittingDoctorDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Attending Doctor Dropdown
                ExposedDropdownMenuBox(
                    expanded = attendingDoctorDropdownExpanded,
                    onExpandedChange = { attendingDoctorDropdownExpanded = !attendingDoctorDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = doctors.firstOrNull { it.id == attendingDoctorId }?.fullName ?: "Select Attending Doctor",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Attending Doctor *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = attendingDoctorDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = attendingDoctorDropdownExpanded,
                        onDismissRequest = { attendingDoctorDropdownExpanded = false }
                    ) {
                        doctors.forEach { doc ->
                            DropdownMenuItem(
                                text = { Text("${doc.fullName} (${doc.specialization ?: "General"})") },
                                onClick = {
                                    attendingDoctorId = doc.id
                                    attendingDoctorDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 16.dp))

                // Section: Clinical Reason & Diagnosis
                Text("Clinical Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = reasonForAdmission,
                    onValueChange = { reasonForAdmission = it },
                    label = { Text("Reason for Admission *") },
                    placeholder = { Text("e.g. Acute severe abdominal pain") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = provisionalDiagnosis,
                    onValueChange = { provisionalDiagnosis = it },
                    label = { Text("Provisional Diagnosis *") },
                    placeholder = { Text("e.g. Suspected Acute Appendicitis") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Source & Type selectors
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Source", fontSize = 11.sp, color = Slate600)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("OPD", "DIRECT", "EMERGENCY").forEach { src ->
                                FilterChip(
                                    selected = selectedSource == src,
                                    onClick = { selectedSource = src },
                                    label = { Text(src, fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Type", fontSize = 11.sp, color = Slate600)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("ELECTIVE", "EMERGENCY").forEach { typ ->
                                FilterChip(
                                    selected = selectedType == typ,
                                    onClick = { selectedType = typ },
                                    label = { Text(typ, fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 16.dp))

                // Section: Bed Allocation (Optional for Fast-track)
                Text("Bed Allocation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Select an available bed to admit immediately into the ward", fontSize = 11.sp, color = Slate500)
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = bedDropdownExpanded,
                    onExpandedChange = { bedDropdownExpanded = !bedDropdownExpanded }
                ) {
                    val bedLabel = selectedBed?.let {
                        "${it.label} · ${it.room.ward.name} (${it.room.name})${it.dailyRate?.let { r -> " · ₹$r/day" } ?: ""}"
                    } ?: "Assign Bed Later (Request Only)"

                    OutlinedTextField(
                        value = bedLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Bed") },
                        leadingIcon = { Icon(Icons.Default.SingleBed, contentDescription = null, tint = MedRayBluePrimary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bedDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = bedDropdownExpanded,
                        onDismissRequest = { bedDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Assign Bed Later (Save Request Only)") },
                            onClick = {
                                selectedBedId = null
                                bedDropdownExpanded = false
                            }
                        )
                        availableBeds.forEach { b ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("${b.label} · ${b.room.name} (${b.room.ward.name})", fontWeight = FontWeight.SemiBold)
                                        b.dailyRate?.let { r -> Text("Daily Rate: ₹$r", fontSize = 11.sp, color = Slate500) }
                                    }
                                },
                                onClick = {
                                    selectedBedId = b.id
                                    bedDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 16.dp))

                // Attendant Details (Optional)
                Text("Attendant / Contact (Optional)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = attendantName,
                        onValueChange = { attendantName = it },
                        label = { Text("Attendant Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = attendantPhone,
                        onValueChange = { attendantPhone = it },
                        label = { Text("Attendant Phone") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            localError = null
                            // Validation
                            if (isExistingPatientMode && selectedExistingPatient == null) {
                                localError = "Please select an existing patient"
                                return@Button
                            }
                            if (!isExistingPatientMode && (fullName.isBlank() || phone.isBlank())) {
                                localError = "Please enter patient name and phone number"
                                return@Button
                            }
                            if (admittingDoctorId.isBlank()) {
                                localError = "Please select an admitting doctor"
                                return@Button
                            }
                            if (reasonForAdmission.isBlank() || provisionalDiagnosis.isBlank()) {
                                localError = "Please provide reason for admission and provisional diagnosis"
                                return@Button
                            }

                            val newPatReq = if (!isExistingPatientMode) {
                                val parsedAge = age.toIntOrNull()
                                RegisterPatientRequest(
                                    fullName = fullName.trim(),
                                    phone = phone.trim(),
                                    gender = gender,
                                    age = parsedAge
                                )
                            } else null

                            onAdmit(
                                selectedExistingPatient?.id,
                                newPatReq,
                                admittingDoctorId,
                                attendingDoctorId.ifBlank { admittingDoctorId },
                                reasonForAdmission.trim(),
                                provisionalDiagnosis.trim(),
                                selectedSource,
                                selectedType,
                                selectedBedId,
                                attendantName.trim().ifBlank { null },
                                attendantPhone.trim().ifBlank { null },
                                paymentType
                            )
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = PureWhite, strokeWidth = 2.dp)
                        } else {
                            Text(if (selectedBedId != null) "Confirm & Admit" else "Create Request")
                        }
                    }
                }
            }
        }
    }
}
