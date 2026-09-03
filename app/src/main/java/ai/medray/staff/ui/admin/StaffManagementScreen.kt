package ai.medray.staff.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.medray.staff.data.model.User
import ai.medray.staff.data.model.UserRole
import ai.medray.staff.ui.theme.*

private val CardBorderColor = Color(0xFFE2E8F0)
private val Slate500 = Color(0xFF64748B)
private val Slate50 = Color(0xFFF8FAFC)

/**
 * Clinic Admin's staff directory — mobile counterpart to web's /staff page,
 * backed by the same `GET/POST/PATCH/DELETE /api/users` endpoints
 * (StaffManagementRepository). Roles a Clinic Admin can assign here are
 * restricted server-side to Receptionist/Nurse/Doctor.
 */
@Composable
fun StaffManagementScreen(
    staff: List<User>,
    isLoading: Boolean,
    error: String?,
    showDeactivated: Boolean,
    onShowDeactivatedChange: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onAddStaffClick: () -> Unit,
    onEditStaffClick: (User) -> Unit,
    onResendInvite: (User) -> Unit,
    onDeactivateStaff: (User) -> Unit,
    onRestoreStaff: (User) -> Unit,
    actionBusy: Boolean,
    modifier: Modifier = Modifier
) {
    var confirmDeactivateTarget by remember { mutableStateOf<User?>(null) }
    var menuTargetId by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxSize().background(Slate50)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${staff.count { !it.isDeactivated }} active staff",
                        fontFamily = HeadingFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Show deactivated",
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            color = Slate500
                        )
                        Switch(
                            checked = showDeactivated,
                            onCheckedChange = onShowDeactivatedChange,
                            modifier = Modifier.scale(0.75f)
                        )
                    }
                }
                IconButton(onClick = onRefresh, enabled = !isLoading) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Slate500)
                }
                Button(
                    onClick = onAddStaffClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary)
                ) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Staff", fontFamily = HeadingFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (!error.isNullOrBlank()) {
                Text(
                    error,
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = Color(0xFFDC2626),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (isLoading && staff.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MedRayBluePrimary)
                }
            } else if (staff.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Groups, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No staff yet. Add your first receptionist, nurse, or doctor.",
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp,
                            color = Slate500,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(staff, key = { it.id }) { member ->
                        StaffRow(
                            member = member,
                            menuOpen = menuTargetId == member.id,
                            onMenuOpenChange = { open -> menuTargetId = if (open) member.id else null },
                            onEdit = { menuTargetId = null; onEditStaffClick(member) },
                            onResendInvite = { menuTargetId = null; onResendInvite(member) },
                            onDeactivate = { menuTargetId = null; confirmDeactivateTarget = member },
                            onRestore = { menuTargetId = null; onRestoreStaff(member) },
                            actionBusy = actionBusy
                        )
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }

    confirmDeactivateTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmDeactivateTarget = null },
            title = { Text("Deactivate ${target.fullName}?", fontFamily = HeadingFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "They'll immediately lose access to the app and web portal. You can restore this account later from \"Show deactivated\".",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = Slate500
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeactivateStaff(target)
                    confirmDeactivateTarget = null
                }) {
                    Text("Deactivate", fontFamily = HeadingFontFamily, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeactivateTarget = null }) {
                    Text("Cancel", fontFamily = HeadingFontFamily)
                }
            }
        )
    }
}

@Composable
private fun StaffRow(
    member: User,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onResendInvite: () -> Unit,
    onDeactivate: () -> Unit,
    onRestore: () -> Unit,
    actionBusy: Boolean
) {
    val roleLabel = when {
        member.roles.contains(UserRole.DOCTOR) -> "Doctor"
        member.roles.contains(UserRole.NURSE) -> "Nurse"
        member.roles.contains(UserRole.RECEPTIONIST) -> "Receptionist"
        else -> "Staff"
    }
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, CardBorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (member.isDeactivated) Color(0xFFF1F5F9) else Color(0xFFDBEAFE), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    member.fullName.trim().firstOrNull()?.uppercase() ?: "?",
                    fontFamily = HeadingFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (member.isDeactivated) Slate500 else MedRayBlueDark
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        member.fullName,
                        fontFamily = HeadingFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (member.isDeactivated) {
                        Spacer(Modifier.width(6.dp))
                        Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(5.dp)) {
                            Text(
                                "DEACTIVATED",
                                fontFamily = HeadingFontFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB91C1C),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    "$roleLabel · ${member.email}",
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = Slate500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box {
                IconButton(onClick = { onMenuOpenChange(true) }, enabled = !actionBusy) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More actions", tint = Slate500)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { onMenuOpenChange(false) }) {
                    if (!member.isDeactivated) {
                        DropdownMenuItem(text = { Text("Edit") }, onClick = onEdit)
                        DropdownMenuItem(text = { Text("Resend Invite") }, onClick = onResendInvite)
                        DropdownMenuItem(text = { Text("Deactivate", color = Color(0xFFDC2626)) }, onClick = onDeactivate)
                    } else {
                        DropdownMenuItem(text = { Text("Restore") }, onClick = onRestore)
                    }
                }
            }
        }
    }
}
