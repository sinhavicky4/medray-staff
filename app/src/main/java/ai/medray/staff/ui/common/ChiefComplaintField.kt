package ai.medray.staff.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.medray.staff.ui.theme.*
import kotlinx.coroutines.delay

val DEFAULT_COMMON_COMPLAINTS = listOf(
    "Fever",
    "Cough & Cold",
    "Headache",
    "Body Ache",
    "BP Review",
    "Diabetes Review",
    "Gastric / Stomach",
    "Routine Checkup"
)

/**
 * Reusable Chief Complaint input component providing:
 * 1. Quick chips for one-tap selection of frequent complaints.
 * 2. Token-aware backend clinical terminology search (debounced 300ms).
 * 3. Clean auto-suggested chips from terminology service (e.g. SNOMED/symptoms).
 * 4. Smart appending/token-replacement so multiple symptoms can be added via commas.
 */
@Composable
fun ChiefComplaintField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = "Chief Complaint",
    placeholder: String = "Search symptom or type complaint (e.g. Fever, Cough)…",
    quickComplaints: List<String> = DEFAULT_COMMON_COMPLAINTS,
    onSearchSymptoms: (suspend (String) -> List<String>)? = null,
    singleLine: Boolean = false,
    maxLines: Int = 3,
    showQuickTags: Boolean = true
) {
    var symptomSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSearchingSymptoms by remember { mutableStateOf(false) }
    var suppressNextSearch by remember { mutableStateOf(false) }

    val activeToken = remember(value) {
        value.substringAfterLast(',').trim()
    }

    LaunchedEffect(value) {
        if (suppressNextSearch) {
            suppressNextSearch = false
            symptomSuggestions = emptyList()
            isSearchingSymptoms = false
            return@LaunchedEffect
        }

        if (activeToken.length >= 2 && onSearchSymptoms != null) {
            delay(300)
            isSearchingSymptoms = true
            try {
                val results = onSearchSymptoms(activeToken)
                val currentTokens = value.split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }
                symptomSuggestions = results.filter { it.isNotBlank() && it.trim().lowercase() !in currentTokens }
            } catch (_: Exception) {
                symptomSuggestions = emptyList()
            } finally {
                isSearchingSymptoms = false
            }
        } else {
            symptomSuggestions = emptyList()
            isSearchingSymptoms = false
        }
    }

    Column(modifier = modifier) {
        if (showQuickTags && quickComplaints.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickComplaints) { comp ->
                    val isIncluded = value.split(',').map { it.trim() }.any { it.equals(comp, ignoreCase = true) }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isIncluded) Color(0xFFEFF6FF) else Slate100)
                            .border(1.dp, if (isIncluded) Color(0xFF93C5FD) else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable {
                                val tokens = value.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                                val newValue = if (isIncluded) {
                                    tokens.filter { !it.equals(comp, ignoreCase = true) }.joinToString(", ")
                                } else {
                                    if (value.isBlank()) comp else "$value, $comp"
                                }
                                suppressNextSearch = true
                                symptomSuggestions = emptyList()
                                onValueChange(newValue)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isIncluded) "✓ $comp" else "+ $comp",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isIncluded) FontWeight.Bold else FontWeight.Medium,
                            color = if (isIncluded) MedRayBlueDark else Slate700
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it) },
            label = if (label != null) { { Text(label) } } else null,
            placeholder = { Text(placeholder, fontSize = 13.sp) },
            singleLine = singleLine,
            maxLines = maxLines,
            trailingIcon = {
                if (isSearchingSymptoms) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MedRayBluePrimary
                    )
                } else if (value.isNotEmpty()) {
                    IconButton(onClick = {
                        suppressNextSearch = true
                        symptomSuggestions = emptyList()
                        onValueChange("")
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Slate400, modifier = Modifier.size(16.dp))
                    }
                }
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // Backend Clinical Terminology Suggestions
        if (symptomSuggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = PureWhite,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Checklist, contentDescription = null, tint = MedRayBluePrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Clinical Terminology Suggestions",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MedRayBlueDark
                            )
                        }
                        Text(
                            text = "${symptomSuggestions.size} found",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400
                        )
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(symptomSuggestions) { suggestion ->
                            Surface(
                                color = Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                                modifier = Modifier.clickable {
                                    val lastCommaIndex = value.lastIndexOf(',')
                                    val newValue = if (lastCommaIndex >= 0) {
                                        val prefix = value.substring(0, lastCommaIndex + 1).trim()
                                        "$prefix $suggestion"
                                    } else {
                                        suggestion
                                    }
                                    suppressNextSearch = true
                                    symptomSuggestions = emptyList()
                                    onValueChange(newValue)
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = MedRayBluePrimary, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = suggestion,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MedRayBlueDark
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
