package ai.medray.staff.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.medray.staff.data.model.User
import ai.medray.staff.data.model.UserRole
import ai.medray.staff.ui.theme.*

private val Slate500 = Color(0xFF64748B)

// Roles a Clinic Admin may assign from this dialog — mirrors the server's
// STAFF_ROLES restriction in users.ts (canManageStaff blocks CLINIC_ADMIN
// and SUPER_ADMIN through this endpoint). The backend's STAFF_ROLES also
// includes a bare "GENERAL" role, which has no UserRole enum entry on
// mobile — omitted here, matching the enum's actual coverage.
private val ASSIGNABLE_ROLES = listOf(
    UserRole.RECEPTIONIST to "Receptionist",
    UserRole.NURSE to "Nurse",
    UserRole.DOCTOR to "Doctor"
)

/**
 * Add or edit a staff member. In edit mode, email/phone stay editable (same
 * as web) but there's no password field — passwords are always
 * server-generated (create) or admin-triggered via "Resend Invite".
 */
@Composable
fun AddEditStaffDialog(
    existing: User?,
    isBusy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (fullName: String, email: String, phone: String, roles: List<UserRole>) -> Unit
) {
    var fullName by remember(existing) { mutableStateOf(existing?.fullName ?: "") }
    var email by remember(existing) { mutableStateOf(existing?.email ?: "") }
    var phone by remember(existing) { mutableStateOf(existing?.phone ?: "") }
    var selectedRoles by remember(existing) {
        mutableStateOf(existing?.roles?.filter { it in ASSIGNABLE_ROLES.map { r -> r.first } }?.toSet() ?: setOf(UserRole.RECEPTIONIST))
    }

    val isValid = fullName.isNotBlank() && email.isNotBlank() && phone.isNotBlank() && selectedRoles.isNotEmpty()

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        title = {
            Text(
                if (existing != null) "Edit Staff Member" else "Add Staff Member",
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Role", fontFamily = HeadingFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate500)
                Column {
                    ASSIGNABLE_ROLES.forEach { (role, label) ->
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedRoles.contains(role),
                                onCheckedChange = { checked ->
                                    selectedRoles = if (checked) selectedRoles + role else selectedRoles - role
                                }
                            )
                            Text(
                                label,
                                fontFamily = InterFontFamily,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                if (existing == null) {
                    Text(
                        "A temporary password will be generated — you'll be shown it after creating this account.",
                        fontFamily = InterFontFamily,
                        fontSize = 11.sp,
                        color = Slate500
                    )
                }

                if (!error.isNullOrBlank()) {
                    Text(error, fontFamily = InterFontFamily, fontSize = 12.sp, color = Color(0xFFDC2626))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(fullName.trim(), email.trim(), phone.trim(), selectedRoles.toList()) },
                enabled = isValid && !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary)
            ) {
                if (isBusy) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (existing != null) "Save Changes" else "Add Staff", fontFamily = HeadingFontFamily, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text("Cancel", fontFamily = HeadingFontFamily)
            }
        }
    )
}
