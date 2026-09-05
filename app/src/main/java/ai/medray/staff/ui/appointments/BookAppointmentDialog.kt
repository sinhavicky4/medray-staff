package ai.medray.staff.ui.appointments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ai.medray.staff.data.model.DoctorSummary
import ai.medray.staff.data.model.Patient
import ai.medray.staff.data.model.formatToIsoUtc
import ai.medray.staff.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TIME_SLOT_OPTIONS = listOf(
    Pair(9, 0), Pair(9, 30),
    Pair(10, 0), Pair(10, 30),
    Pair(11, 0), Pair(11, 30),
    Pair(14, 0), Pair(14, 30),
    Pair(15, 0), Pair(15, 30),
    Pair(16, 0), Pair(16, 30),
    Pair(17, 0), Pair(17, 30),
    Pair(18, 0), Pair(18, 30)
)

private val COMMON_COMPLAINTS = listOf(
    "General Checkup",
    "Routine Follow-up",
    "Fever & Body Ache",
    "Cough & Cold",
    "BP Review",
    "Diabetes Management",
    "Skin Rash",
    "Abdominal Pain"
)

private val VISIT_TYPES = listOf(
    "FIRST_VISIT" to "First Visit",
    "FOLLOW_UP" to "Follow-up",
    "ROUTINE" to "Routine Checkup"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookAppointmentDialog(
    doctors: List<DoctorSummary>,
    existingPatients: List<Patient> = emptyList(),
    initialPatient: Patient? = null,
    isBusy: Boolean = false,
    onDismiss: () -> Unit,
    onBookExisting: (
        patient: Patient,
        doctorId: String,
        scheduledAtIso: String,
        durationMinutes: Int,
        chiefComplaint: String,
        visitType: String
    ) -> Unit,
    onBookNew: (
        patientName: String,
        phone: String,
        age: Int?,
        gender: String,
        doctorId: String,
        scheduledAtIso: String,
        durationMinutes: Int,
        chiefComplaint: String,
        visitType: String
    ) -> Unit
) {
    // Mode: existing vs new
    var isNewPatientMode by remember { mutableStateOf(initialPatient == null && existingPatients.isEmpty()) }
    var selectedPatient by remember { mutableStateOf(initialPatient) }
    var patientSearchQuery by remember { mutableStateOf("") }

    // New patient fields
    var newPatientName by remember { mutableStateOf("") }
    var newPatientPhone by remember { mutableStateOf("") }
    var newPatientAge by remember { mutableStateOf("") }
    var newPatientGender by remember { mutableStateOf("MALE") }

    // Doctor selection
    var selectedDoctorId by remember {
        mutableStateOf(doctors.firstOrNull()?.id ?: "")
    }
    var doctorDropdownExpanded by remember { mutableStateOf(false) }

    // Date selection
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Time selection
    var selectedHour by remember { mutableStateOf(10) }
    var selectedMinute by remember { mutableStateOf(0) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Visit metadata
    var selectedVisitType by remember { mutableStateOf("FIRST_VISIT") }
    var selectedDurationMinutes by remember { mutableStateOf(15) }
    var chiefComplaint by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val filteredPatients = remember(patientSearchQuery, existingPatients) {
        if (patientSearchQuery.isBlank()) {
            existingPatients.take(5)
        } else {
            val q = patientSearchQuery.trim().lowercase()
            existingPatients.filter {
                it.fullName.lowercase().contains(q) ||
                    (it.phone?.contains(q) == true) ||
                    it.uhid.lowercase().contains(q)
            }.take(6)
        }
    }

    Dialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PureWhite,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 520.dp)
                .imePadding()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Header
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Schedule Appointment",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Book a clinic consultation slot for patient",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }
                    IconButton(
                        onClick = { if (!isBusy) onDismiss() },
                        enabled = !isBusy
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Patient Selection Section
                Text(
                    text = "Patient",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Slate700
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (initialPatient != null) {
                    // Pre-selected Patient display
                    SelectedPatientBadge(
                        patient = initialPatient,
                        allowChange = false,
                        onChange = {}
                    )
                } else if (selectedPatient != null) {
                    // Chosen existing patient
                    SelectedPatientBadge(
                        patient = selectedPatient!!,
                        allowChange = true,
                        onChange = { selectedPatient = null }
                    )
                } else {
                    // Mode Switcher: Existing vs New
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate100, RoundedCornerShape(10.dp))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isNewPatientMode) PureWhite else Color.Transparent)
                                .clickable { isNewPatientMode = false }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Existing Patient",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (!isNewPatientMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (!isNewPatientMode) MedRayBluePrimary else Slate600
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isNewPatientMode) PureWhite else Color.Transparent)
                                .clickable { isNewPatientMode = true }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+ New Patient",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isNewPatientMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (isNewPatientMode) MedRayBluePrimary else Slate600
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (!isNewPatientMode) {
                        // Search existing patients
                        OutlinedTextField(
                            value = patientSearchQuery,
                            onValueChange = { patientSearchQuery = it },
                            placeholder = { Text("Search by name, phone or UHID...", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) },
                            trailingIcon = {
                                if (patientSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { patientSearchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Slate400)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (filteredPatients.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Slate50, RoundedCornerShape(10.dp))
                                    .border(1.dp, Slate200, RoundedCornerShape(10.dp))
                                    .padding(14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "No matching patient found",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate500
                                    )
                                    TextButton(onClick = { isNewPatientMode = true }) {
                                        Text("+ Register as New Patient", color = MedRayBluePrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                filteredPatients.forEach { patient ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Slate50, RoundedCornerShape(10.dp))
                                            .border(1.dp, Slate200, RoundedCornerShape(10.dp))
                                            .clickable {
                                                selectedPatient = patient
                                                validationError = null
                                            }
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = patient.fullName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Slate900
                                            )
                                            Text(
                                                text = "${patient.phone ?: "No phone"} • UHID: ${patient.uhid}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Slate500
                                            )
                                        }
                                        Icon(
                                            Icons.Outlined.ChevronRight,
                                            contentDescription = "Select",
                                            tint = MedRayBluePrimary
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // New patient inline fields
                        OutlinedTextField(
                            value = newPatientName,
                            onValueChange = { newPatientName = it; validationError = null },
                            label = { Text("Full Name *") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Slate400) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newPatientPhone,
                            onValueChange = {
                                if (it.length <= 10 && it.all { c -> c.isDigit() }) {
                                    newPatientPhone = it
                                    validationError = null
                                }
                            },
                            label = { Text("Phone Number *") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Slate400) },
                            prefix = { Text("+91 ", color = Slate600, fontWeight = FontWeight.Medium) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newPatientAge,
                                onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) newPatientAge = it },
                                label = { Text("Age") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )

                            // Gender Chips
                            Column(modifier = Modifier.weight(2f)) {
                                Text(
                                    text = "Gender",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate600
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("MALE" to "M", "FEMALE" to "F", "OTHER" to "O").forEach { (code, label) ->
                                        val isSelected = newPatientGender == code
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) MedRayBluePrimary else Slate100)
                                                .clickable { newPatientGender = code }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) PureWhite else Slate700,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Consulting Doctor Selection
                Text(
                    text = "Consulting Doctor *",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Slate700
                )
                Spacer(modifier = Modifier.height(6.dp))

                val selectedDoctor = doctors.find { it.id == selectedDoctorId } ?: doctors.firstOrNull()
                ExposedDropdownMenuBox(
                    expanded = doctorDropdownExpanded,
                    onExpandedChange = { doctorDropdownExpanded = !doctorDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedDoctor?.let { "Dr. ${it.fullName.removePrefix("Dr. ").trim()} (${it.specialization ?: "General"})" } ?: "Select Doctor",
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = { Icon(Icons.Default.MedicalServices, contentDescription = null, tint = MedRayBluePrimary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = doctorDropdownExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = doctorDropdownExpanded,
                        onDismissRequest = { doctorDropdownExpanded = false }
                    ) {
                        doctors.forEach { doc ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Dr. ${doc.fullName.removePrefix("Dr. ").trim()}", fontWeight = FontWeight.Bold)
                                        Text(doc.specialization ?: "General Physician", style = MaterialTheme.typography.bodySmall, color = Slate500)
                                    }
                                },
                                onClick = {
                                    selectedDoctorId = doc.id
                                    doctorDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Date Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Appointment Date *",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate700
                    )
                    TextButton(
                        onClick = { showDatePicker = true },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp), tint = MedRayBluePrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Calendar", color = MedRayBluePrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                // Quick date pills: Today, Tomorrow, In 2 Days
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        0L to "Today",
                        1L to "Tomorrow",
                        2L to today.plusDays(2).format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH))
                    ).forEach { (offset, label) ->
                        val targetDate = today.plusDays(offset)
                        val isSelected = selectedDate == targetDate
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MedRayBluePrimary else Slate100)
                                .border(1.dp, if (isSelected) MedRayBluePrimary else Slate200, RoundedCornerShape(10.dp))
                                .clickable { selectedDate = targetDate }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PureWhite else Slate700
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Selected: ${selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MedRayBlueDark,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 5. Time Slot Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Consultation Time *",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate700
                    )
                    TextButton(
                        onClick = { showTimePicker = true },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = MedRayBluePrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Custom Time", color = MedRayBluePrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                // Common Slot Pills (Horizontal Scroll)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(TIME_SLOT_OPTIONS) { (h, m) ->
                        val isSelected = selectedHour == h && selectedMinute == m
                        val timeString = LocalTime.of(h, m).format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MedRayBluePrimary else Slate100)
                                .border(1.dp, if (isSelected) MedRayBluePrimary else Slate200, RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedHour = h
                                    selectedMinute = m
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = timeString,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PureWhite else Slate700
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                val formattedTime = LocalTime.of(selectedHour, selectedMinute).format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
                Text(
                    text = "Scheduled at $formattedTime (${selectedDurationMinutes} mins)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MedRayBlueDark,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 6. Visit Type & Duration
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Visit Type
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(
                            text = "Visit Type",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate700
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            VISIT_TYPES.forEach { (typeKey, typeLabel) ->
                                val isSelected = selectedVisitType == typeKey
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MedRayBlueContainer else Slate100)
                                        .clickable { selectedVisitType = typeKey }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = typeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MedRayBluePrimary else Slate600
                                    )
                                }
                            }
                        }
                    }

                    // Duration
                    Column(modifier = Modifier.weight(0.8f)) {
                        Text(
                            text = "Duration",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate700
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(15, 30, 45).forEach { mins ->
                                val isSelected = selectedDurationMinutes == mins
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MedRayBlueContainer else Slate100)
                                        .clickable { selectedDurationMinutes = mins }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${mins}m",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MedRayBluePrimary else Slate600
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 7. Chief Complaint
                Text(
                    text = "Chief Complaint / Reason *",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Slate700
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Quick Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(COMMON_COMPLAINTS) { comp ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Slate100)
                                .clickable {
                                    chiefComplaint = if (chiefComplaint.isBlank()) comp else "$chiefComplaint, $comp"
                                    validationError = null
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "+ $comp",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate700
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = chiefComplaint,
                    onValueChange = { chiefComplaint = it; validationError = null },
                    placeholder = { Text("e.g. Fever for 2 days, mild cough, routine review...", fontSize = 14.sp) },
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Error Banner
                if (validationError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = validationError!!,
                        color = StatusErrorText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 8. Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { if (!isBusy) onDismiss() },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Cancel", color = Slate700)
                    }

                    Button(
                        onClick = {
                            val activeDoctorId = selectedDoctorId.ifBlank { doctors.firstOrNull()?.id ?: "" }
                            if (activeDoctorId.isBlank()) {
                                validationError = "Please select a consulting doctor"
                                return@Button
                            }
                            if (chiefComplaint.trim().isBlank()) {
                                validationError = "Please specify a chief complaint / reason for visit"
                                return@Button
                            }

                            val scheduledAtIso = formatToIsoUtc(selectedDate, selectedHour, selectedMinute)

                            if (selectedPatient != null) {
                                onBookExisting(
                                    selectedPatient!!,
                                    activeDoctorId,
                                    scheduledAtIso,
                                    selectedDurationMinutes,
                                    chiefComplaint.trim(),
                                    selectedVisitType
                                )
                            } else {
                                if (newPatientName.trim().isBlank()) {
                                    validationError = "Please enter patient name"
                                    return@Button
                                }
                                if (newPatientPhone.trim().length < 10) {
                                    validationError = "Please enter a valid 10-digit phone number"
                                    return@Button
                                }
                                onBookNew(
                                    newPatientName.trim(),
                                    newPatientPhone.trim(),
                                    newPatientAge.toIntOrNull(),
                                    newPatientGender,
                                    activeDoctorId,
                                    scheduledAtIso,
                                    selectedDurationMinutes,
                                    chiefComplaint.trim(),
                                    selectedVisitType
                                )
                            }
                        },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = PureWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.EventAvailable, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirm Booking", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Material 3 Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    // Only allow today or future dates
                    val itemDate = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                    return !itemDate.isBefore(today)
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = MedRayBluePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = Slate500)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Material 3 Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = {
                Text(
                    text = "Select Consultation Time",
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedHour = timePickerState.hour
                    selectedMinute = timePickerState.minute
                    showTimePicker = false
                }) {
                    Text("Confirm", color = MedRayBluePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel", color = Slate500)
                }
            }
        )
    }
}

@Composable
private fun SelectedPatientBadge(
    patient: Patient,
    allowChange: Boolean,
    onChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MedRayBlueContainer.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .border(1.dp, MedRayBluePrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MedRayBluePrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = patient.fullName.take(1).uppercase(),
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = patient.fullName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${patient.phone ?: "No phone"} • UHID: ${patient.uhid}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600
                )
            }
        }
        if (allowChange) {
            TextButton(
                onClick = onChange,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text("Change", color = MedRayBluePrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}
