package ai.medray.staff.ui.patients

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.verticalScroll
import ai.medray.staff.data.model.Patient
import ai.medray.staff.data.model.Prescription
import ai.medray.staff.data.model.Visit
import ai.medray.staff.data.model.formatDateDisplay
import ai.medray.staff.data.model.formatIsoDateTimeLocal
import ai.medray.staff.ui.common.MedRayPullRefreshBox
import ai.medray.staff.ui.common.QuickFilterPill
import ai.medray.staff.ui.common.StatCard
import ai.medray.staff.ui.theme.*

private enum class PatientDetailTab { OVERVIEW, VISITS, PRESCRIPTIONS }

private enum class PatientGenderFilter { ALL, MALE, FEMALE, SENIORS }

@Composable
fun PatientsScreen(
    patients: List<Patient>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onPatientClick: (Patient) -> Unit,
    onRegisterPatientClick: () -> Unit,
    onAddToQueueClick: (Patient) -> Unit = {},
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(PatientGenderFilter.ALL) }
    var selectedPatientForDetails by remember { mutableStateOf<Patient?>(null) }

    val maleCount = patients.count { it.gender.equals("MALE", ignoreCase = true) }
    val femaleCount = patients.count { it.gender.equals("FEMALE", ignoreCase = true) }
    val seniorCount = patients.count { (it.age ?: 0) >= 60 }

    val filtered = remember(patients, searchQuery, selectedFilter) {
        patients.filter { patient ->
            val matchesFilter = when (selectedFilter) {
                PatientGenderFilter.ALL -> true
                PatientGenderFilter.MALE -> patient.gender.equals("MALE", ignoreCase = true)
                PatientGenderFilter.FEMALE -> patient.gender.equals("FEMALE", ignoreCase = true)
                PatientGenderFilter.SENIORS -> (patient.age ?: 0) >= 60
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                patient.fullName.lowercase().contains(q) ||
                        patient.phone?.contains(q) == true ||
                        patient.uhid.lowercase().contains(q)
            }
            matchesFilter && matchesSearch
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
        // 1. Header & Register Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Patients Directory",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${patients.size} registered patient records",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )
                }

                Button(
                    onClick = onRegisterPatientClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("+ New Patient", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                        title = "Total Registered",
                        value = "${patients.size}",
                        footer = "Clinic patient base",
                        icon = Icons.Filled.People,
                        iconBg = Color(0xFFEFF6FF),
                        iconTint = MedRayBluePrimary,
                        isSelected = selectedFilter == PatientGenderFilter.ALL,
                        onClick = { selectedFilter = PatientGenderFilter.ALL },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    StatCard(
                        title = "Male Patients",
                        value = "$maleCount",
                        footer = "Active records",
                        icon = Icons.Filled.Man,
                        iconBg = Color(0xFFE0F2FE),
                        iconTint = Color(0xFF0284C7),
                        isSelected = selectedFilter == PatientGenderFilter.MALE,
                        onClick = {
                            selectedFilter = if (selectedFilter == PatientGenderFilter.MALE) PatientGenderFilter.ALL else PatientGenderFilter.MALE
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                ) {
                    StatCard(
                        title = "Female Patients",
                        value = "$femaleCount",
                        footer = "Active records",
                        icon = Icons.Filled.Woman,
                        iconBg = Color(0xFFFDF2F8),
                        iconTint = Color(0xFFDB2777),
                        isSelected = selectedFilter == PatientGenderFilter.FEMALE,
                        onClick = {
                            selectedFilter = if (selectedFilter == PatientGenderFilter.FEMALE) PatientGenderFilter.ALL else PatientGenderFilter.FEMALE
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    StatCard(
                        title = "Senior Citizens",
                        value = "$seniorCount",
                        footer = "Age 60+ priority",
                        icon = Icons.Filled.Elderly,
                        iconBg = Color(0xFFFEF3C7),
                        iconTint = Color(0xFFD97706),
                        isSelected = selectedFilter == PatientGenderFilter.SENIORS,
                        onClick = {
                            selectedFilter = if (selectedFilter == PatientGenderFilter.SENIORS) PatientGenderFilter.ALL else PatientGenderFilter.SENIORS
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

        // 4. Quick Filter Pills
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                QuickFilterPill(
                    label = "All (${patients.size})",
                    isSelected = selectedFilter == PatientGenderFilter.ALL,
                    onClick = { selectedFilter = PatientGenderFilter.ALL }
                )
                QuickFilterPill(
                    label = "Male ($maleCount)",
                    isSelected = selectedFilter == PatientGenderFilter.MALE,
                    dotColor = Color(0xFF0284C7),
                    onClick = { selectedFilter = PatientGenderFilter.MALE }
                )
                QuickFilterPill(
                    label = "Female ($femaleCount)",
                    isSelected = selectedFilter == PatientGenderFilter.FEMALE,
                    dotColor = Color(0xFFDB2777),
                    onClick = { selectedFilter = PatientGenderFilter.FEMALE }
                )
                QuickFilterPill(
                    label = "Seniors 60+ ($seniorCount)",
                    isSelected = selectedFilter == PatientGenderFilter.SENIORS,
                    dotColor = Color(0xFFD97706),
                    onClick = { selectedFilter = PatientGenderFilter.SENIORS }
                )
            }
        }

        // 5. Patient Records List
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
                            Icons.Outlined.People,
                            contentDescription = null,
                            tint = Slate300,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching patients found" else "No patients registered yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate700
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap + New Patient above to register a new clinical patient record.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { patient ->
                PatientCard(
                    patient = patient,
                    onClick = {
                        selectedPatientForDetails = patient
                        onPatientClick(patient)
                    },
                    onAddToQueueClick = {
                        onAddToQueueClick(patient)
                    }
                )
            }
        }
    }
}

    // Patient Details Modal Dialog
    selectedPatientForDetails?.let { patient ->
        PatientDetailsDialog(
            patient = patient,
            onDismiss = { selectedPatientForDetails = null },
            onAddToQueueClick = {
                selectedPatientForDetails = null
                onAddToQueueClick(patient)
            }
        )
    }
}

@Composable
fun PatientCard(
    patient: Patient,
    onClick: () -> Unit,
    onAddToQueueClick: () -> Unit = {}
) {
    val initials = remember(patient.fullName) {
        val names = patient.fullName.trim().split(" ")
        if (names.size >= 2) "${names[0].take(1)}${names[1].take(1)}".uppercase()
        else patient.fullName.take(2).uppercase()
    }

    val avatarBg = when (patient.gender.uppercase()) {
        "FEMALE" -> Color(0xFFFDF2F8)
        else -> Color(0xFFEFF6FF)
    }
    val avatarTint = when (patient.gender.uppercase()) {
        "FEMALE" -> Color(0xFFDB2777)
        else -> MedRayBluePrimary
    }

    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            // Patient Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(avatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = avatarTint
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = patient.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                    )

                    Surface(
                        color = MedRayBlueLight,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = patient.uhid,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MedRayBluePrimary,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    val demo = buildList {
                        if (patient.age != null) add("${patient.age} yrs")
                        if (patient.gender.isNotBlank()) add(patient.gender.lowercase().replaceFirstChar { it.uppercase() })
                        if (!patient.bloodGroup.isNullOrBlank()) add("🩸 ${patient.bloodGroup}")
                    }
                    Text(
                        text = demo.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )
                }

                if (!patient.phone.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Phone,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+91 ${patient.phone}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Slate700
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Quick Add to Queue action button
            IconButton(
                onClick = onAddToQueueClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color(0xFFEFF6FF),
                    contentColor = MedRayBluePrimary
                ),
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                Icon(
                    Icons.Filled.PersonAdd,
                    contentDescription = "Add to Queue",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Full patient profile — Overview / Visits / Prescriptions, matching the
 * web app's PatientDetailClient.tsx tabs. [visits] (each carrying its own
 * nested prescriptions) is fetched once when the dialog opens; Prescriptions
 * is just those visits' prescriptions flattened, since there's no separate
 * by-patient prescriptions endpoint — same data the web tab is built from.
 */
@Composable
fun PatientDetailsDialog(
    patient: Patient,
    visits: List<Visit> = emptyList(),
    visitsLoading: Boolean = false,
    onDismiss: () -> Unit,
    onAddToQueueClick: () -> Unit = {}
) {
    val initials = remember(patient.fullName) {
        val names = patient.fullName.trim().split(" ")
        if (names.size >= 2) "${names[0].take(1)}${names[1].take(1)}".uppercase()
        else patient.fullName.take(2).uppercase()
    }
    var tab by remember { mutableStateOf(PatientDetailTab.OVERVIEW) }
    val prescriptions = remember(visits) { visits.flatMap { v -> v.prescriptions.map { it to v } } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .padding(vertical = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                // Header
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
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
                        Column {
                            Text(
                                text = patient.fullName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Surface(
                                color = MedRayBlueLight,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "UHID: ${patient.uhid}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MedRayBluePrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickFilterPill(label = "Overview", isSelected = tab == PatientDetailTab.OVERVIEW, onClick = { tab = PatientDetailTab.OVERVIEW })
                    QuickFilterPill(label = "Visits (${visits.size})", isSelected = tab == PatientDetailTab.VISITS, onClick = { tab = PatientDetailTab.VISITS })
                    QuickFilterPill(label = "Prescriptions (${prescriptions.size})", isSelected = tab == PatientDetailTab.PRESCRIPTIONS, onClick = { tab = PatientDetailTab.PRESCRIPTIONS })
                }

                HorizontalDivider(color = Slate100, modifier = Modifier.padding(vertical = 12.dp))

                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    when (tab) {
                        PatientDetailTab.OVERVIEW -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                InfoRow("Age & Gender", "${patient.age ?: "Unknown"} yrs · ${patient.gender.lowercase().replaceFirstChar { it.uppercase() }}")
                                if (!patient.dob.isNullOrBlank()) {
                                    InfoRow("Date of Birth", formatDateDisplay(patient.dob))
                                }
                                if (!patient.phone.isNullOrBlank()) {
                                    InfoRow("Phone Number", "+91 ${patient.phone}")
                                }
                                if (!patient.email.isNullOrBlank()) {
                                    InfoRow("Email", patient.email)
                                }
                                if (!patient.bloodGroup.isNullOrBlank()) {
                                    InfoRow("Blood Group", patient.bloodGroup)
                                }
                                if (!patient.address.isNullOrBlank()) {
                                    InfoRow("Address", patient.address)
                                }
                                if (!patient.emergencyContact.isNullOrBlank()) {
                                    InfoRow("Emergency Contact", patient.emergencyContact)
                                }
                            }
                        }

                        PatientDetailTab.VISITS -> {
                            if (visitsLoading) {
                                Text("Loading visits…", style = MaterialTheme.typography.bodySmall, color = Slate400)
                            } else if (visits.isEmpty()) {
                                Text("No past visits recorded yet.", style = MaterialTheme.typography.bodySmall, color = Slate400)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    visits.forEach { visit -> VisitSummaryCard(visit) }
                                }
                            }
                        }

                        PatientDetailTab.PRESCRIPTIONS -> {
                            if (visitsLoading) {
                                Text("Loading prescriptions…", style = MaterialTheme.typography.bodySmall, color = Slate400)
                            } else if (prescriptions.isEmpty()) {
                                Text("No prescriptions on file yet.", style = MaterialTheme.typography.bodySmall, color = Slate400)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    prescriptions.forEach { (rx, visit) -> PrescriptionSummaryCard(rx, visit) }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close")
                    }

                    Button(
                        onClick = onAddToQueueClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.4f)
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

@Composable
private fun VisitSummaryCard(visit: Visit) {
    Surface(
        color = Slate50,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = formatIsoDateTimeLocal(visit.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = visit.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            if (!visit.doctor?.fullName.isNullOrBlank()) {
                Text("Dr. ${visit.doctor?.fullName}", style = MaterialTheme.typography.bodySmall, color = Slate600)
            }
            if (!visit.chiefComplaint.isNullOrBlank()) {
                Text("Complaint: ${visit.chiefComplaint}", style = MaterialTheme.typography.bodySmall, color = Slate700, modifier = Modifier.padding(top = 4.dp))
            }
            if (!visit.diagnosis.isNullOrBlank()) {
                Text("Diagnosis: ${visit.diagnosis}", style = MaterialTheme.typography.bodySmall, color = Slate700, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun PrescriptionSummaryCard(rx: Prescription, visit: Visit) {
    Surface(
        color = Slate50,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = formatIsoDateTimeLocal(rx.createdAt ?: visit.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                if (!visit.doctor?.fullName.isNullOrBlank()) {
                    Text("Dr. ${visit.doctor?.fullName}", style = MaterialTheme.typography.labelSmall, color = Slate500)
                }
            }
            if (rx.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                rx.items.forEach { item ->
                    Text(
                        text = "• ${item.medicineName}${item.dosage.let { d -> if (d.isNotBlank()) " ($d)" else "" }} — ${item.frequencyCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )
                }
            }
            if (!rx.adviceNotes.isNullOrBlank()) {
                Text("Advice: ${rx.adviceNotes}", style = MaterialTheme.typography.bodySmall, color = Slate600, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate50, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Slate500)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Slate900)
    }
}
