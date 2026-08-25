package ai.medray.staff.ui.patients

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.medray.staff.data.model.Patient
import ai.medray.staff.ui.theme.*

@Composable
fun PatientsScreen(
    patients: List<Patient>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onPatientClick: (Patient) -> Unit,
    onRegisterPatientClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().background(Slate50).padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search patient name, phone, UHID...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = PureWhite,
                    unfocusedContainerColor = PureWhite,
                    focusedBorderColor = MedRayBluePrimary,
                    unfocusedBorderColor = Slate200
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(10.dp))

            FloatingActionButton(
                onClick = onRegisterPatientClick,
                containerColor = MedRayBluePrimary,
                contentColor = PureWhite,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(52.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Patient")
            }
        }

        val filtered = patients.filter {
            if (searchQuery.isBlank()) true
            else it.fullName.contains(searchQuery, ignoreCase = true) ||
                 it.phone?.contains(searchQuery) == true ||
                 it.uhid.contains(searchQuery, ignoreCase = true)
        }

        if (filtered.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.PeopleOutline,
                        contentDescription = null,
                        tint = Slate300,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "No patients registered yet" else "No matching patients",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate600
                    )
                    Text(
                        text = "Tap the + button to register a new walk-in patient",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { patient ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPatientClick(patient) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Surface(
                                color = MedRayBlueLight,
                                shape = CircleShape,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = patient.fullName.take(2).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MedRayBluePrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = patient.fullName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = Slate100,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = patient.uhid,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Slate600,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    val ageGender = buildString {
                                        if (patient.age != null) append("${patient.age}y")
                                        if (patient.gender.isNotBlank()) {
                                            if (isNotEmpty()) append(", ")
                                            append(patient.gender.lowercase().replaceFirstChar { it.uppercase() })
                                        }
                                    }
                                    if (ageGender.isNotEmpty()) {
                                        Text(
                                            text = ageGender,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate500
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    if (!patient.phone.isNullOrBlank()) {
                                        Text(
                                            text = "+91 ${patient.phone}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate600,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
