package ai.medray.staff.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.Logout
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
import ai.medray.staff.BuildConfig
import ai.medray.staff.data.model.User
import ai.medray.staff.ui.theme.*

@Composable
fun ProfileScreen(
    user: User?,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    val initials = remember(user?.fullName) {
        val names = (user?.fullName ?: "ST").trim().split(" ")
        if (names.size >= 2) "${names[0].take(1)}${names[1].take(1)}".uppercase()
        else (user?.fullName ?: "ST").take(2).uppercase()
    }

    val roleName = if (user?.isNurse == true) "Registered Clinical Nurse" else "Front Desk & Receptionist"
    val roleStation = if (user?.isNurse == true) "Triage Station" else "Front Desk Workspace"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Staff Profile Hero Lockup
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PureWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFEFF6FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MedRayBluePrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = user?.fullName ?: "Staff Member",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )

                Text(
                    text = user?.email ?: "staff@medray.ai",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    color = MedRayBlueLight,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Filled.Verified,
                            contentDescription = null,
                            tint = MedRayBluePrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "$roleName · $roleStation",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MedRayBluePrimary
                        )
                    }
                }
            }
        }

        // 2. Assigned Clinic Information
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = PureWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Assigned Clinic",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )

                Spacer(modifier = Modifier.height(12.dp))

                ProfileInfoRow(
                    icon = Icons.Outlined.LocalHospital,
                    label = "Clinic Name",
                    value = user?.clinic?.name ?: "MedRay AI Multi-Specialty Clinic"
                )

                ProfileInfoRow(
                    icon = Icons.Outlined.Phone,
                    label = "Reception Contact",
                    value = if (!user?.clinic?.phone.isNullOrBlank()) "+91 ${user?.clinic?.phone}" else "+91 98765 43210"
                )

                ProfileInfoRow(
                    icon = Icons.Outlined.QrCode,
                    label = "Dynamic UPI VPA",
                    value = user?.clinic?.upiVpa ?: "medray@upi"
                )

                ProfileInfoRow(
                    icon = Icons.Outlined.Payments,
                    label = "Default OPD Fee",
                    value = "₹${(user?.clinic?.defaultConsultationFee ?: 500.0).toInt()} per consultation"
                )
            }
        }

        // 3. App Security & System Info
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = PureWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "System & Security",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )

                Spacer(modifier = Modifier.height(12.dp))

                ProfileInfoRow(
                    icon = Icons.Outlined.Shield,
                    label = "Security Standard",
                    value = "256-bit Encryption · ABDM Ready"
                )

                ProfileInfoRow(
                    icon = Icons.Outlined.CloudSync,
                    label = "Offline Sync",
                    value = "SQLite Room Outbox Active"
                )

                ProfileInfoRow(
                    icon = Icons.Outlined.Info,
                    label = "App Version",
                    value = "v0.1.5 (Build 6) · Production"
                )
            }
        }

        // 4. Sign Out Button
        Button(
            onClick = { showLogoutDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.Logout,
                contentDescription = "Sign Out",
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Sign Out of Staff Account",
                color = Color(0xFFDC2626),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out of your staff session?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MedRayBluePrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Slate400)
            Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Slate800)
        }
    }
}
