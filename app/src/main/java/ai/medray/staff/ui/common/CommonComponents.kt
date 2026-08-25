package ai.medray.staff.ui.common

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ai.medray.staff.data.model.QueueStatus
import ai.medray.staff.data.model.User
import ai.medray.staff.data.model.Vitals
import ai.medray.staff.domain.UpiQrGenerator
import ai.medray.staff.domain.VitalsSeverity
import ai.medray.staff.domain.VitalsValidator
import ai.medray.staff.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedRayTopBar(
    title: String,
    subtitle: String? = null,
    user: User? = null,
    isOffline: Boolean = false,
    onMenuClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onClinicClick: () -> Unit = {}
) {
    Surface(
        color = PureWhite,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Open Navigation Menu", tint = Slate800)
                    }
                },
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            if (user != null) {
                                val roleLabel = when {
                                    user.isNurse -> "NURSE"
                                    user.isReceptionist -> "RECEPTION"
                                    else -> "STAFF"
                                }
                                val roleBg = if (user.isNurse) MedRayTealContainer else MedRayBlueContainer
                                val roleColor = if (user.isNurse) MedRayTealDark else MedRayBlueDark

                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = roleBg,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = roleLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = roleColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    if (isOffline) {
                        Surface(
                            color = StatusWarningBg,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.CloudOff,
                                    contentDescription = "Offline",
                                    tint = StatusWarningText,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Offline Mode",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusWarningText
                                )
                            }
                        }
                    }

                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Slate700)
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (user?.clinic != null) {
                            DropdownMenuItem(
                                text = { Text("Clinic: ${user.clinic.name}") },
                                onClick = {
                                    showMenu = false
                                    onClinicClick()
                                },
                                leadingIcon = { Icon(Icons.Outlined.LocalHospital, null) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Sign Out", color = StatusErrorText) },
                            onClick = {
                                showMenu = false
                                onLogoutClick()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Logout, null, tint = StatusErrorText) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PureWhite)
            )
        }
    }
}

data class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun MedRayBottomNav(
    items: List<BottomNavItem>,
    currentRoute: String,
    onItemClick: (String) -> Unit
) {
    NavigationBar(
        containerColor = PureWhite,
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemClick(item.route) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (item.badgeCount > 0) {
                                Badge(containerColor = MedRayBluePrimary) {
                                    Text(item.badgeCount.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.title
                        )
                    }
                },
                label = {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MedRayBluePrimary,
                    selectedTextColor = MedRayBluePrimary,
                    unselectedIconColor = Slate500,
                    unselectedTextColor = Slate500,
                    indicatorColor = MedRayBlueLight
                )
            )
        }
    }
}

@Composable
fun VitalsSummaryBadge(vitals: Vitals) {
    if (!vitals.hasAnyReading()) {
        Surface(
            color = Slate100,
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = "Vitals not recorded",
                style = MaterialTheme.typography.bodySmall,
                color = Slate400,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        return
    }

    val eval = VitalsValidator.evaluate(vitals)
    val (bg, border, text) = when (eval.overallSeverity) {
        VitalsSeverity.CRITICAL -> Triple(VitalsAbnormalBg, StatusErrorBorder, VitalsAbnormalText)
        VitalsSeverity.WARNING -> Triple(StatusWarningBg, StatusWarningBorder, StatusWarningText)
        VitalsSeverity.NORMAL -> Triple(VitalsNormalBg, StatusSuccessBorder, VitalsNormalText)
    }

    val parts = mutableListOf<String>()
    if (!vitals.vitalsBp.isNullOrBlank()) parts.add("BP ${vitals.vitalsBp}")
    if (vitals.vitalsSpo2 != null) parts.add("SpO2 ${vitals.vitalsSpo2}%")
    if (vitals.vitalsPulseBpm != null) parts.add("Pulse ${vitals.vitalsPulseBpm}")
    if (vitals.vitalsTemperatureF != null) parts.add("${vitals.vitalsTemperatureF}°F")
    if (vitals.vitalsWeightKg != null) parts.add("${vitals.vitalsWeightKg}kg")
    if (vitals.bmi != null) parts.add("BMI ${vitals.bmi}")

    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, border)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = parts.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = text
            )
        }
    }
}

@Composable
fun QueueStatusBadge(status: QueueStatus) {
    val (bg, text) = when (status) {
        QueueStatus.WAITING -> Pair(StatusWarningBg, StatusWarningText)
        QueueStatus.ARRIVED -> Pair(StatusSuccessBg, StatusSuccessText)
        QueueStatus.IN_PROGRESS -> Pair(MedRayBlueContainer, MedRayBlueDark)
        QueueStatus.COMPLETED -> Pair(Slate100, Slate700)
        QueueStatus.CANCELLED -> Pair(StatusErrorBg, StatusErrorText)
        QueueStatus.NO_SHOW -> Pair(StatusErrorBg, StatusErrorText)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = status.name.replace("_", " "),
            style = MaterialTheme.typography.labelSmall,
            color = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun DynamicUpiQrDialog(
    payeeVpa: String,
    payeeName: String,
    amount: Double,
    invoiceNumber: String,
    onDismiss: () -> Unit,
    onMarkPaid: () -> Unit
) {
    val upiPayload = remember(payeeVpa, amount, invoiceNumber) {
        UpiQrGenerator.createUpiUri(payeeVpa, payeeName, amount, invoiceNumber)
    }
    val qrBitmap = remember(upiPayload) {
        try {
            UpiQrGenerator.generateQrBitmap(upiPayload, 512)
        } catch (_: Exception) {
            null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Scan & Pay via UPI",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    text = "Invoice #$invoiceNumber",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "₹${String.format("%.2f", amount)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MedRayBluePrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .border(2.dp, Slate200, RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "UPI QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Accepts GPay, PhonePe, Paytm, BHIM & all UPI apps",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            onMarkPaid()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusSuccessText),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mark Paid")
                    }
                }
            }
        }
    }
}
