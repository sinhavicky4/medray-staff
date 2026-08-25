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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ai.medray.staff.data.model.Patient
import ai.medray.staff.ui.common.QuickFilterPill
import ai.medray.staff.ui.common.StatCard
import ai.medray.staff.ui.theme.*

private enum class PatientGenderFilter { ALL, MALE, FEMALE, SENIORS }

@Composable
fun PatientsScreen(
    patients: List<Patient>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onPatientClick: (Patient) -> Unit,
    onRegisterPatientClick: () -> Unit,
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

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
    ) {
        // 1. Header & Register Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Patients Directory",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
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
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ New Patient", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                        title = "Total Registered",
                        value = "${patients.size}",
                        footer = "Clinic patient base",
                        icon = Icons.Filled.People,
                        iconBg = Color(0xFFEFF6FF),
                        iconTint = MedRayBluePrimary,
                        isSelected = selectedFilter == PatientGenderFilter.ALL,
                        onClick = { selectedFilter = PatientGenderFilter.ALL },
                        modifier = Modifier.weight(1f)
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
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
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
                        modifier = Modifier.weight(1f)
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
                        modifier = Modifier.weight(1f)
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
                    }
                )
            }
        }
    }

    // Patient Details Modal Dialog
    selectedPatientForDetails?.let { patient ->
        PatientDetailsDialog(
            patient = patient,
            onDismiss = { selectedPatientForDetails = null }
        )
    }
}

@Composable
fun PatientCard(
    patient: Patient,
    onClick: () -> Unit
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
                        color = Slate900
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

            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Slate400,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Detailed Patient Clinical Summary Modal.
 */
@Composable
fun PatientDetailsDialog(
    patient: Patient,
    onDismiss: () -> Unit
) {
    val initials = remember(patient.fullName) {
        val names = patient.fullName.trim().split(" ")
        if (names.size >= 2) "${names[0].take(1)}${names[1].take(1)}".uppercase()
        else patient.fullName.take(2).uppercase()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PureWhite,
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
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

                HorizontalDivider(color = Slate100, modifier = Modifier.padding(vertical = 12.dp))

                // Patient Info Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoRow("Age & Gender", "${patient.age ?: "Unknown"} yrs · ${patient.gender.lowercase().replaceFirstChar { it.uppercase() }}")
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

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
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
