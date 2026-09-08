package ai.medray.staff.ui.ipd

import androidx.compose.animation.*
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
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.text.style.TextAlign
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
    // Current wizard step: 1 (Patient), 2 (Care & Bed), 3 (Clinical)
    var currentStep by remember { mutableStateOf(1) }

    // Step 1: Patient Selection / Creation
    var isExistingPatientMode by remember { mutableStateOf(existingPatients.isNotEmpty()) }
    var selectedExistingPatient by remember { mutableStateOf<Patient?>(null) }
    var patientSearchQuery by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("MALE") }

    // Step 2: Doctor & Bed
    var admittingDoctorId by remember { mutableStateOf(doctors.firstOrNull()?.id ?: "") }
    var attendingDoctorId by remember { mutableStateOf(doctors.firstOrNull()?.id ?: "") }
    var admittingDoctorDropdownExpanded by remember { mutableStateOf(false) }
    var attendingDoctorDropdownExpanded by remember { mutableStateOf(false) }
    var selectedBedId by remember { mutableStateOf<String?>(null) }
    var bedDropdownExpanded by remember { mutableStateOf(false) }

    // Step 3: Clinical & Contact Details
    var reasonForAdmission by remember { mutableStateOf("") }
    var provisionalDiagnosis by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf("DIRECT") }
    var selectedType by remember { mutableStateOf("ELECTIVE") }
    var attendantName by remember { mutableStateOf("") }
    var attendantPhone by remember { mutableStateOf("") }
    var paymentType by remember { mutableStateOf("SELF_PAY") }

    var localError by remember { mutableStateOf<String?>(null) }

    val step1ScrollState = rememberScrollState()
    val step2ScrollState = rememberScrollState()
    val step3ScrollState = rememberScrollState()

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
                .fillMaxHeight(0.88f)
                .imePadding()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "New Inpatient Admission",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = when (currentStep) {
                                1 -> "Step 1 of 3: Patient Information"
                                2 -> "Step 2 of 3: Care Team & Bed"
                                else -> "Step 3 of 3: Clinical & Contact"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stepper Indicator
                StepperIndicator(
                    currentStep = currentStep,
                    onStepClick = { targetStep ->
                        if (targetStep < currentStep) {
                            localError = null
                            currentStep = targetStep
                        }
                    }
                )

                HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 12.dp))

                // Error Banner
                (errorMessage ?: localError)?.let { err ->
                    Surface(
                        color = StatusErrorBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = StatusErrorText,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusErrorText,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Scrollable Step Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (currentStep) {
                        1 -> {
                            Step1PatientContent(
                                isExistingPatientMode = isExistingPatientMode,
                                onModeChange = {
                                    isExistingPatientMode = it
                                    localError = null
                                },
                                patientSearchQuery = patientSearchQuery,
                                onSearchQueryChange = { patientSearchQuery = it },
                                filteredPatients = filteredPatients,
                                selectedExistingPatient = selectedExistingPatient,
                                onSelectExistingPatient = {
                                    selectedExistingPatient = it
                                    localError = null
                                },
                                onClearExistingPatient = { selectedExistingPatient = null },
                                fullName = fullName,
                                onFullNameChange = {
                                    fullName = it
                                    localError = null
                                },
                                phone = phone,
                                onPhoneChange = {
                                    phone = it
                                    localError = null
                                },
                                age = age,
                                onAgeChange = { age = it },
                                gender = gender,
                                onGenderChange = { gender = it },
                                scrollState = step1ScrollState
                            )
                        }
                        2 -> {
                            Step2CareAndBedContent(
                                isExistingPatientMode = isExistingPatientMode,
                                selectedExistingPatient = selectedExistingPatient,
                                fullName = fullName,
                                phone = phone,
                                doctors = doctors,
                                admittingDoctorId = admittingDoctorId,
                                onAdmittingDoctorChange = {
                                    admittingDoctorId = it
                                    if (attendingDoctorId.isBlank()) attendingDoctorId = it
                                    localError = null
                                },
                                attendingDoctorId = attendingDoctorId,
                                onAttendingDoctorChange = { attendingDoctorId = it },
                                admittingDoctorDropdownExpanded = admittingDoctorDropdownExpanded,
                                onAdmittingDropdownExpandChange = { admittingDoctorDropdownExpanded = it },
                                attendingDoctorDropdownExpanded = attendingDoctorDropdownExpanded,
                                onAttendingDropdownExpandChange = { attendingDoctorDropdownExpanded = it },
                                availableBeds = availableBeds,
                                selectedBedId = selectedBedId,
                                onSelectBedId = { selectedBedId = it },
                                bedDropdownExpanded = bedDropdownExpanded,
                                onBedDropdownExpandChange = { bedDropdownExpanded = it },
                                scrollState = step2ScrollState
                            )
                        }
                        3 -> {
                            Step3ClinicalContent(
                                isExistingPatientMode = isExistingPatientMode,
                                selectedExistingPatient = selectedExistingPatient,
                                fullName = fullName,
                                phone = phone,
                                doctors = doctors,
                                admittingDoctorId = admittingDoctorId,
                                selectedBed = selectedBed,
                                reasonForAdmission = reasonForAdmission,
                                onReasonChange = {
                                    reasonForAdmission = it
                                    localError = null
                                },
                                provisionalDiagnosis = provisionalDiagnosis,
                                onDiagnosisChange = {
                                    provisionalDiagnosis = it
                                    localError = null
                                },
                                selectedSource = selectedSource,
                                onSourceChange = { selectedSource = it },
                                selectedType = selectedType,
                                onTypeChange = { selectedType = it },
                                attendantName = attendantName,
                                onAttendantNameChange = { attendantName = it },
                                attendantPhone = attendantPhone,
                                onAttendantPhoneChange = { attendantPhone = it },
                                scrollState = step3ScrollState
                            )
                        }
                    }
                }

                HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 12.dp))

                // Bottom Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (currentStep) {
                        1 -> {
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
                                    if (isExistingPatientMode) {
                                        if (selectedExistingPatient == null) {
                                            localError = "Please select an existing patient"
                                            return@Button
                                        }
                                    } else {
                                        if (fullName.isBlank()) {
                                            localError = "Please enter patient's full name"
                                            return@Button
                                        }
                                        if (phone.isBlank() || phone.trim().length < 10) {
                                            localError = "Please enter a valid phone number (at least 10 digits)"
                                            return@Button
                                        }
                                    }
                                    currentStep = 2
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Next: Care & Bed")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                        2 -> {
                            OutlinedButton(
                                onClick = {
                                    localError = null
                                    currentStep = 1
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Back")
                            }

                            Button(
                                onClick = {
                                    localError = null
                                    if (admittingDoctorId.isBlank()) {
                                        localError = "Please select an admitting doctor"
                                        return@Button
                                    }
                                    currentStep = 3
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Next: Clinical")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                        3 -> {
                            OutlinedButton(
                                onClick = {
                                    localError = null
                                    currentStep = 2
                                },
                                enabled = !isSubmitting,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Back")
                            }

                            Button(
                                onClick = {
                                    localError = null
                                    if (reasonForAdmission.isBlank() || provisionalDiagnosis.isBlank()) {
                                        localError = "Please enter reason for admission and provisional diagnosis"
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
    }
}

@Composable
private fun StepperIndicator(
    currentStep: Int,
    onStepClick: (Int) -> Unit
) {
    val steps = listOf("Patient", "Care & Bed", "Clinical")
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, title ->
            val stepNumber = index + 1
            val isCompleted = currentStep > stepNumber
            val isCurrent = currentStep == stepNumber

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .then(if (index < steps.size - 1) Modifier.weight(1f) else Modifier)
            ) {
                // Step badge
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCompleted -> StatusSuccessBg
                                isCurrent -> MedRayBluePrimary
                                else -> Slate100
                            }
                        )
                        .border(
                            width = if (isCurrent) 1.5.dp else 1.dp,
                            color = when {
                                isCompleted -> StatusSuccessBorder
                                isCurrent -> MedRayBluePrimary
                                else -> Slate300
                            },
                            shape = CircleShape
                        )
                        .clickable(enabled = isCompleted) {
                            onStepClick(stepNumber)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = StatusSuccessText,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Text(
                            text = "$stepNumber",
                            fontSize = 11.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCurrent) PureWhite else Slate500
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color = when {
                        isCurrent -> MedRayBluePrimary
                        isCompleted -> Slate800
                        else -> Slate400
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (index < steps.size - 1) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(if (currentStep > stepNumber) MedRayBluePrimary.copy(alpha = 0.6f) else Slate200)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }
        }
    }
}

@Composable
private fun Step1PatientContent(
    isExistingPatientMode: Boolean,
    onModeChange: (Boolean) -> Unit,
    patientSearchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filteredPatients: List<Patient>,
    selectedExistingPatient: Patient?,
    onSelectExistingPatient: (Patient) -> Unit,
    onClearExistingPatient: () -> Unit,
    fullName: String,
    onFullNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    age: String,
    onAgeChange: (String) -> Unit,
    gender: String,
    onGenderChange: (String) -> Unit,
    scrollState: androidx.compose.foundation.ScrollState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
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
                    .clickable { onModeChange(true) }
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
                    .clickable { onModeChange(false) }
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
            if (selectedExistingPatient == null) {
                OutlinedTextField(
                    value = patientSearchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search by name, phone, or UHID...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredPatients.isEmpty()) {
                    Text(
                        text = if (patientSearchQuery.isBlank()) "No recent patients found" else "No matching patients found",
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
                            onClick = { onSelectExistingPatient(p) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
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
                                text = selectedExistingPatient.fullName,
                                fontWeight = FontWeight.Bold,
                                color = MedRayBluePrimary
                            )
                            Text(
                                text = "UHID: ${selectedExistingPatient.uhid} · Phone: ${selectedExistingPatient.phone ?: "—"}",
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        }
                        IconButton(onClick = onClearExistingPatient) {
                            Icon(Icons.Default.Edit, contentDescription = "Change Patient", tint = MedRayBluePrimary)
                        }
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = fullName,
                onValueChange = onFullNameChange,
                label = { Text("Full Name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
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
                    onValueChange = onAgeChange,
                    label = { Text("Age") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )

                Column(modifier = Modifier.weight(1.5f)) {
                    Text("Gender", fontSize = 11.sp, color = Slate600)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("MALE" to "M", "FEMALE" to "F", "OTHER" to "O").forEach { (gVal, label) ->
                            FilterChip(
                                selected = gender == gVal,
                                onClick = { onGenderChange(gVal) },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Step2CareAndBedContent(
    isExistingPatientMode: Boolean,
    selectedExistingPatient: Patient?,
    fullName: String,
    phone: String,
    doctors: List<DoctorSummary>,
    admittingDoctorId: String,
    onAdmittingDoctorChange: (String) -> Unit,
    attendingDoctorId: String,
    onAttendingDoctorChange: (String) -> Unit,
    admittingDoctorDropdownExpanded: Boolean,
    onAdmittingDropdownExpandChange: (Boolean) -> Unit,
    attendingDoctorDropdownExpanded: Boolean,
    onAttendingDropdownExpandChange: (Boolean) -> Unit,
    availableBeds: List<IpdBed>,
    selectedBedId: String?,
    onSelectBedId: (String?) -> Unit,
    bedDropdownExpanded: Boolean,
    onBedDropdownExpandChange: (Boolean) -> Unit,
    scrollState: androidx.compose.foundation.ScrollState
) {
    val selectedBed = remember(selectedBedId, availableBeds) {
        availableBeds.firstOrNull { it.id == selectedBedId }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Patient Summary Pill
        Surface(
            color = MedRayBlueLight,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MedRayBluePrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isExistingPatientMode) {
                        "Patient: ${selectedExistingPatient?.fullName ?: "Unknown"} (UHID: ${selectedExistingPatient?.uhid ?: "—"})"
                    } else {
                        "Patient: $fullName ($phone)"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MedRayBlueDarker
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Doctor Assignment Section
        Text("Doctor Assignment", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
        Spacer(modifier = Modifier.height(8.dp))

        // Admitting Doctor Dropdown
        ExposedDropdownMenuBox(
            expanded = admittingDoctorDropdownExpanded,
            onExpandedChange = onAdmittingDropdownExpandChange
        ) {
            OutlinedTextField(
                value = doctors.firstOrNull { it.id == admittingDoctorId }?.fullName ?: "Select Admitting Doctor",
                onValueChange = {},
                readOnly = true,
                label = { Text("Admitting Doctor *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = admittingDoctorDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
            ExposedDropdownMenu(
                expanded = admittingDoctorDropdownExpanded,
                onDismissRequest = { onAdmittingDropdownExpandChange(false) }
            ) {
                doctors.forEach { doc ->
                    DropdownMenuItem(
                        text = { Text("${doc.fullName} (${doc.specialization ?: "General"})") },
                        onClick = {
                            onAdmittingDoctorChange(doc.id)
                            onAdmittingDropdownExpandChange(false)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Attending Doctor Dropdown
        ExposedDropdownMenuBox(
            expanded = attendingDoctorDropdownExpanded,
            onExpandedChange = onAttendingDropdownExpandChange
        ) {
            OutlinedTextField(
                value = doctors.firstOrNull { it.id == attendingDoctorId }?.fullName ?: "Select Attending Doctor",
                onValueChange = {},
                readOnly = true,
                label = { Text("Attending Doctor") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = attendingDoctorDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
            ExposedDropdownMenu(
                expanded = attendingDoctorDropdownExpanded,
                onDismissRequest = { onAttendingDropdownExpandChange(false) }
            ) {
                doctors.forEach { doc ->
                    DropdownMenuItem(
                        text = { Text("${doc.fullName} (${doc.specialization ?: "General"})") },
                        onClick = {
                            onAttendingDoctorChange(doc.id)
                            onAttendingDropdownExpandChange(false)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bed Allocation Section
        Text("Bed Allocation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Select an available bed to admit immediately or assign later", fontSize = 11.sp, color = Slate500)
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = bedDropdownExpanded,
            onExpandedChange = onBedDropdownExpandChange
        ) {
            val bedLabel = selectedBed?.let {
                "${it.label} · ${it.room.ward.name} (${it.room.name})${it.dailyRate?.let { r -> " · ₹$r/day" } ?: ""}"
            } ?: "Assign Bed Later (Save Request Only)"

            OutlinedTextField(
                value = bedLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Bed") },
                leadingIcon = { Icon(Icons.Default.SingleBed, contentDescription = null, tint = MedRayBluePrimary) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bedDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
            ExposedDropdownMenu(
                expanded = bedDropdownExpanded,
                onDismissRequest = { onBedDropdownExpandChange(false) }
            ) {
                DropdownMenuItem(
                    text = { Text("Assign Bed Later (Save Request Only)") },
                    onClick = {
                        onSelectBedId(null)
                        onBedDropdownExpandChange(false)
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
                            onSelectBedId(b.id)
                            onBedDropdownExpandChange(false)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Step3ClinicalContent(
    isExistingPatientMode: Boolean,
    selectedExistingPatient: Patient?,
    fullName: String,
    phone: String,
    doctors: List<DoctorSummary>,
    admittingDoctorId: String,
    selectedBed: IpdBed?,
    reasonForAdmission: String,
    onReasonChange: (String) -> Unit,
    provisionalDiagnosis: String,
    onDiagnosisChange: (String) -> Unit,
    selectedSource: String,
    onSourceChange: (String) -> Unit,
    selectedType: String,
    onTypeChange: (String) -> Unit,
    attendantName: String,
    onAttendantNameChange: (String) -> Unit,
    attendantPhone: String,
    onAttendantPhoneChange: (String) -> Unit,
    scrollState: androidx.compose.foundation.ScrollState
) {
    val patName = if (isExistingPatientMode) selectedExistingPatient?.fullName ?: "Patient" else fullName
    val patSub = if (isExistingPatientMode) "UHID: ${selectedExistingPatient?.uhid ?: "—"}" else phone
    val docName = doctors.firstOrNull { it.id == admittingDoctorId }?.fullName ?: "Admitting Doctor"
    val bedText = selectedBed?.let { "${it.label} (${it.room.name})" } ?: "Bed: Assign Later"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Summary Card
        Surface(
            color = Slate50,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$patName · $patSub",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = "Dr. $docName · $bedText",
                        fontSize = 11.sp,
                        color = Slate600
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Clinical Details Section
        Text("Clinical Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = reasonForAdmission,
            onValueChange = onReasonChange,
            label = { Text("Reason for Admission *") },
            placeholder = { Text("e.g. Acute severe abdominal pain") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = provisionalDiagnosis,
            onValueChange = onDiagnosisChange,
            label = { Text("Provisional Diagnosis *") },
            placeholder = { Text("e.g. Suspected Acute Appendicitis") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Source & Type selectors
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Admission Source",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Slate700
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("OPD", "DIRECT", "EMERGENCY").forEach { src ->
                    FilterChip(
                        selected = selectedSource == src,
                        onClick = { onSourceChange(src) },
                        label = {
                            Text(
                                text = src,
                                fontSize = 11.sp,
                                fontWeight = if (selectedSource == src) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MedRayBlueLight,
                            selectedLabelColor = MedRayBluePrimary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Admission Type",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Slate700
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("ELECTIVE", "EMERGENCY").forEach { typ ->
                    FilterChip(
                        selected = selectedType == typ,
                        onClick = { onTypeChange(typ) },
                        label = {
                            Text(
                                text = typ,
                                fontSize = 11.sp,
                                fontWeight = if (selectedType == typ) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MedRayBlueLight,
                            selectedLabelColor = MedRayBluePrimary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Attendant Details Section (Optional)
        Text("Attendant / Contact (Optional)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = attendantName,
            onValueChange = onAttendantNameChange,
            label = { Text("Attendant Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = attendantPhone,
            onValueChange = onAttendantPhoneChange,
            label = { Text("Attendant Phone") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )
    }
}
