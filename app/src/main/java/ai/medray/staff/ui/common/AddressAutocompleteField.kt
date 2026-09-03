package ai.medray.staff.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.medray.staff.data.repository.AddressSuggestion
import ai.medray.staff.data.repository.PlacesAutocompleteRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Address field with Google Places suggestions — Android counterpart to
 * web's AddressAutocompleteInput.tsx. Debounces the query, shows results in
 * a standard Material3 exposed dropdown, and on selection replaces the
 * field with the place's full formatted address (same as web's
 * `place.fetchFields({fields: ["formattedAddress"]})`). Stays a perfectly
 * normal free-typed text field when [repository] has no Places client
 * configured (no API key set locally) — predict() just returns no
 * suggestions rather than erroring.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressAutocompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    repository: PlacesAutocompleteRepository,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    var suggestions by remember { mutableStateOf<List<AddressSuggestion>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    var isResolving by remember { mutableStateOf(false) }
    // Selecting a suggestion sets `value` to its full formatted address,
    // which would otherwise re-trigger this same LaunchedEffect and pop a
    // second, irrelevant dropdown right on top of the field the user just
    // finished picking from — skip exactly that one resulting query.
    var suppressNextPredict by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(value) {
        if (suppressNextPredict) {
            suppressNextPredict = false
            return@LaunchedEffect
        }
        if (value.length < 3) {
            suggestions = emptyList()
            expanded = false
            return@LaunchedEffect
        }
        delay(300) // debounce so we're not firing a request per keystroke
        val result = repository.predict(value)
        suggestions = result.getOrDefault(emptyList())
        expanded = suggestions.isNotEmpty()
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it && suggestions.isNotEmpty() },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 14.sp) },
            singleLine = true,
            trailingIcon = { if (isResolving) CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2563EB), unfocusedBorderColor = Color(0xFFCBD5E1)),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(suggestion.primaryText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(suggestion.secondaryText, fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                    },
                    onClick = {
                        expanded = false
                        suggestions = emptyList()
                        coroutineScope.launch {
                            isResolving = true
                            val result = repository.fetchFormattedAddress(suggestion.placeId)
                            isResolving = false
                            suppressNextPredict = true
                            onValueChange(result.getOrDefault("${suggestion.primaryText}, ${suggestion.secondaryText}"))
                        }
                    }
                )
            }
        }
    }
}
