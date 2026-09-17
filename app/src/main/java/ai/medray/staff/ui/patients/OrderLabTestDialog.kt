package ai.medray.staff.ui.patients

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ai.medray.staff.data.model.LabOrderRequest
import ai.medray.staff.data.model.LabTestItemInput
import ai.medray.staff.data.model.Patient
import ai.medray.staff.data.model.TerminologyItem
import ai.medray.staff.ui.theme.*
import kotlinx.coroutines.delay

private val COMMON_INVESTIGATIONS = listOf(
    Pair("Complete Blood Count (CBC)", "58410-2"),
    Pair("Fasting Blood Sugar (FBS)", "1558-6"),
    Pair("HbA1c (Glycated Hemoglobin)", "4548-4"),
    Pair("Lipid Profile", "57698-3"),
    Pair("Liver Function Test (LFT)", "24325-3"),
    Pair("Kidney Function Test (KFT)", "24362-6"),
    Pair("Serum Creatinine", "2160-0"),
    Pair("Thyroid Profile (T3, T4, TSH)", "24348-5"),
    Pair("Urine Routine & Microscopic", "24356-8"),
    Pair("Serum Electrolytes", "24326-1")
)

private val FASTING_CHIPS = listOf(
    "Fasting 10-12 hrs",
    "Post-Prandial (2 hrs)",
    "Random / No Fasting",
    "Overnight Fasting Required"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderLabTestDialog(
    patient: Patient,
    isSubmitting: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSearchInvestigations: suspend (String) -> List<TerminologyItem>,
    onSubmitOrder: (LabOrderRequest) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<TerminologyItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val selectedTests = remember { mutableStateListOf<LabTestItemInput>() }
    var selectedUrgency by remember { mutableStateOf("ROUTINE") }
    var fastingInstructions by remember { mutableStateOf("") }
    var clinicalNotes by remember { mutableStateOf("") }
    var localValidationMsg by remember { mutableStateOf<String?>(null) }

    // Debounced search for LOINC investigations
    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.length >= 2) {
            isSearching = true
            delay(300)
            val res = onSearchInvestigations(q)
            searchResults = res
            isSearching = false
        } else {
            searchResults = emptyList()
            isSearching = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(20.dp),
            color = PureWhite,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MedRayTealLight
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Biotech,
                                    contentDescription = null,
                                    tint = MedRayTealDark,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Order Lab Investigations",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                "${patient.fullName} · UHID ${patient.uhid ?: "—"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Slate500)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Slate100)

                // Body Scrollable
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Error Banner
                    if (errorMessage != null || localValidationMsg != null) {
                        Surface(
                            color = StatusErrorBg,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StatusErrorBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Icon(
                                    Icons.Filled.ErrorOutline,
                                    contentDescription = null,
                                    tint = StatusErrorText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = localValidationMsg ?: errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StatusErrorText
                                )
                            }
                        }
                    }

                    // Section 1: Search & Add Tests
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Search Diagnostic Tests (LOINC 2.83)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                localValidationMsg = null
                            },
                            placeholder = { Text("Search by test name or LOINC code...", color = Slate400) },
                            leadingIcon = {
                                Icon(Icons.Filled.Search, contentDescription = null, tint = Slate400)
                            },
                            trailingIcon = {
                                if (isSearching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MedRayTealDark
                                    )
                                } else if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Slate400)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MedRayTealDark,
                                unfocusedBorderColor = Slate200,
                                focusedContainerColor = PureWhite,
                                unfocusedContainerColor = Slate50
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Search Results Dropdown List
                        if (searchResults.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PureWhite,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                                shadowElevation = 4.dp,
                                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
                            ) {
                                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                    items(searchResults) { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (selectedTests.none { it.name.equals(item.displayName, ignoreCase = true) }) {
                                                        selectedTests.add(
                                                            LabTestItemInput(
                                                                name = item.displayName,
                                                                code = item.code
                                                            )
                                                        )
                                                        searchQuery = ""
                                                        searchResults = emptyList()
                                                    }
                                                }
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    item.displayName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Slate900
                                                )
                                                if (item.category != null) {
                                                    Text(
                                                        item.category,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Slate400
                                                    )
                                                }
                                            }
                                            if (!item.code.isNullOrBlank()) {
                                                Surface(
                                                    color = MedRayTealLight,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        "LOINC ${item.code}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MedRayTealDark,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        HorizontalDivider(color = Slate100)
                                    }
                                }
                            }
                        }

                        // Common test shortcut chips
                        if (searchQuery.isBlank() && searchResults.isEmpty()) {
                            Text(
                                "Common Investigations",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate500,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(COMMON_INVESTIGATIONS) { (name, code) ->
                                    val isAdded = selectedTests.any { it.name == name }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isAdded) Slate100 else MedRayTealLight,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isAdded) Slate300 else MedRayTealBorder
                                        ),
                                        modifier = Modifier.clickable(enabled = !isAdded) {
                                            selectedTests.add(LabTestItemInput(name = name, code = code))
                                        }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                name,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isAdded) Slate400 else MedRayTealDark
                                            )
                                            if (!isAdded) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    Icons.Filled.Add,
                                                    contentDescription = null,
                                                    tint = MedRayTealDark,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section 2: Selected Tests
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Selected Tests (${selectedTests.size})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                            if (selectedTests.isNotEmpty()) {
                                TextButton(
                                    onClick = { selectedTests.clear() },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Clear All", color = StatusErrorText, fontSize = 12.sp)
                                }
                            }
                        }

                        if (selectedTests.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Slate50,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        "No tests added yet. Search above or tap common tests.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate400
                                    )
                                }
                            }
                        } else {
                            selectedTests.forEachIndexed { index, test ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Slate50,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(MedRayTealLight, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "${index + 1}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MedRayTealDark
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    test.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Slate900
                                                )
                                                if (!test.code.isNullOrBlank()) {
                                                    Text(
                                                        "LOINC: ${test.code}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MedRayTealDark,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                        IconButton(
                                            onClick = { selectedTests.removeAt(index) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Close,
                                                contentDescription = "Remove",
                                                tint = Slate400,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section 3: Urgency / Priority
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Priority / Urgency",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                Triple("ROUTINE", "Routine", Slate600),
                                Triple("URGENT", "Urgent", StatusWarningText),
                                Triple("STAT", "STAT (Emergency)", StatusErrorText)
                            ).forEach { (key, label, activeColor) ->
                                val isSelected = selectedUrgency == key
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) PureWhite else Slate50,
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) activeColor else Slate200
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedUrgency = key }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    ) {
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) activeColor else Slate600
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 4: Fasting Instructions
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Fasting Instructions",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(FASTING_CHIPS) { chip ->
                                val isSelected = fastingInstructions == chip
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MedRayBlueLight else Slate50,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) MedRayBluePrimary else Slate200
                                    ),
                                    modifier = Modifier.clickable {
                                        fastingInstructions = if (isSelected) "" else chip
                                    }
                                ) {
                                    Text(
                                        chip,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MedRayBlueDark else Slate600,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = fastingInstructions,
                            onValueChange = { fastingInstructions = it },
                            placeholder = { Text("E.g. Overnight fasting 12 hrs, water allowed", color = Slate400) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MedRayTealDark,
                                unfocusedBorderColor = Slate200
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Section 5: Clinical Notes
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Clinical Notes & Indication (Optional)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        OutlinedTextField(
                            value = clinicalNotes,
                            onValueChange = { clinicalNotes = it },
                            placeholder = { Text("Clinical reason, diagnosis, or instructions for lab technician", color = Slate400) },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MedRayTealDark,
                                unfocusedBorderColor = Slate200
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Slate100)

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate600),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (selectedTests.isEmpty()) {
                                localValidationMsg = "Please add at least one test to order"
                                return@Button
                            }
                            localValidationMsg = null
                            onSubmitOrder(
                                LabOrderRequest(
                                    tests = selectedTests.toList(),
                                    urgency = selectedUrgency,
                                    fastingInstructions = fastingInstructions.ifBlank { null },
                                    clinicalNotes = clinicalNotes.ifBlank { null }
                                )
                            )
                        },
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MedRayTealDark,
                            contentColor = PureWhite
                        )
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = PureWhite
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating Requisition...")
                        } else {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Issue Lab Order (${selectedTests.size})")
                        }
                    }
                }
            }
        }
    }
}
