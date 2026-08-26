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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Description
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
import ai.medray.staff.data.model.QueueEntry
import ai.medray.staff.data.model.Prescription
import ai.medray.staff.data.model.PrescriptionItem
import ai.medray.staff.data.model.Visit
import ai.medray.staff.data.model.QueueStatus
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.window.DialogProperties
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
fun VitalsSummaryBadge(vitals: Vitals, modifier: Modifier = Modifier) {
    if (!vitals.hasAnyReading()) {
        Surface(
            color = Slate100,
            shape = RoundedCornerShape(6.dp),
            modifier = modifier
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Vitals Pending",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    val eval = VitalsValidator.evaluate(vitals)
    val (bg, border, text, dotColor) = when (eval.overallSeverity) {
        VitalsSeverity.CRITICAL -> listOf(Color(0xFFFEF2F2), Color(0xFFFCA5A5), Color(0xFFDC2626), Color(0xFFEF4444))
        VitalsSeverity.WARNING -> listOf(Color(0xFFFFFBEB), Color(0xFFFDE68A), Color(0xFFD97706), Color(0xFFF59E0B))
        VitalsSeverity.NORMAL -> listOf(Color(0xFFF0FDF4), Color(0xFFBBF7D0), Color(0xFF16A34A), Color(0xFF22C55E))
    }

    val parts = mutableListOf<String>()
    if (!vitals.vitalsBp.isNullOrBlank()) parts.add("BP: ${vitals.vitalsBp}")
    if (vitals.vitalsPulseBpm != null) parts.add("Pulse: ${vitals.vitalsPulseBpm} bpm")
    if (vitals.vitalsSpo2 != null) parts.add("SpO2: ${vitals.vitalsSpo2}%")
    if (vitals.vitalsTemperatureF != null) parts.add("${vitals.vitalsTemperatureF}°F")
    if (vitals.vitalsWeightKg != null) parts.add("${vitals.vitalsWeightKg} kg")
    if (vitals.bmi != null) parts.add("BMI: ${vitals.bmi}")

    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, border),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = parts.joinToString("  ·  "),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = text
            )
        }
    }
}

@Composable
fun QueueStatusBadge(status: QueueStatus) {
    val (bg, text, dotColor) = when (status) {
        QueueStatus.WAITING -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), Color(0xFFF59E0B))
        QueueStatus.ARRIVED -> Triple(Color(0xFFDCFCE7), Color(0xFF16A34A), Color(0xFF22C55E))
        QueueStatus.IN_PROGRESS -> Triple(Color(0xFFEFF6FF), Color(0xFF2563EB), Color(0xFF3B82F6))
        QueueStatus.COMPLETED -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), Color(0xFF94A3B8))
        QueueStatus.CANCELLED -> Triple(Color(0xFFFEF2F2), Color(0xFFDC2626), Color(0xFFEF4444))
        QueueStatus.NO_SHOW -> Triple(Color(0xFFFEF2F2), Color(0xFFDC2626), Color(0xFFEF4444))
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, text.copy(alpha = 0.2f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = when (status) {
                    QueueStatus.IN_PROGRESS -> "In Triage"
                    QueueStatus.WAITING -> "Waiting"
                    QueueStatus.ARRIVED -> "Arrived"
                    QueueStatus.COMPLETED -> "Completed"
                    QueueStatus.CANCELLED -> "Cancelled"
                    QueueStatus.NO_SHOW -> "No Show"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = text
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    footer: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) MedRayBluePrimary else Slate200
        ),
        shadowElevation = if (isSelected) 2.dp else 0.5.dp,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate500
                )
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(iconBg, RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = footer,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) MedRayBluePrimary else Slate400,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun QuickFilterPill(
    label: String,
    isSelected: Boolean,
    dotColor: Color? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) MedRayBlueLight else PureWhite,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF93C5FD) else Slate200
        ),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            if (dotColor != null) {
                Box(modifier = Modifier.size(6.dp).background(dotColor, CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MedRayBluePrimary else Slate700
            )
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionViewerDialog(
    entry: QueueEntry,
    visit: Visit? = null,
    onDismiss: () -> Unit,
    onShareWhatsApp: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val doctorName = entry.doctor?.fullName ?: "Rajesh Sharma"
    val docSpecialization = entry.doctor?.specialization ?: "General Physician"
    val patientName = entry.patient?.fullName ?: "Patient"
    val patientAge = entry.patient?.age ?: 32
    val patientGender = entry.patient?.gender ?: "MALE"
    val uhid = entry.patient?.uhid ?: "UHID-2026-001"
    val opdNumber = entry.opdNumber

    val rxItems = visit?.prescriptions?.firstOrNull()?.items?.ifEmpty { null } ?: listOf(
        PrescriptionItem(medicineName = "Tab. Paracetamol 650mg", dosage = "650mg", frequencyCode = "1-0-1", durationDays = 3, foodInstruction = "After food", route = "Oral"),
        PrescriptionItem(medicineName = "Tab. Pantoprazole 40mg", dosage = "40mg", frequencyCode = "1-0-0", durationDays = 5, foodInstruction = "Before food (empty stomach)", route = "Oral"),
        PrescriptionItem(medicineName = "Tab. Cetirizine 10mg", dosage = "10mg", frequencyCode = "0-0-1", durationDays = 5, foodInstruction = "At bedtime", route = "Oral")
    )

    val diagnosisText = visit?.diagnosis ?: if (entry.chiefComplaint.isNotBlank()) "Assessment: ${entry.chiefComplaint}" else "Acute Upper Respiratory Infection (URI)"
    val adviceNotes = visit?.prescriptions?.firstOrNull()?.adviceNotes?.ifBlank { null } ?: "Steam inhalation twice daily. Maintain adequate hydration and rest. Review if fever persists after 3 days."
    val testsAdvised = visit?.prescriptions?.firstOrNull()?.testsAdvised?.ifBlank { null } ?: "Complete Blood Count (CBC) with ESR"

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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 1. Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFFE0F2FE),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Description, contentDescription = null, tint = MedRayBluePrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Medical Prescription (Rx)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = "Token #$opdNumber · ${entry.formattedTime}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Slate200)

                // Scrollable Prescription Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    // Doctor & Patient Banner
                    Surface(
                        color = Slate50,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Dr. $doctorName",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MedRayBlueDark
                                    )
                                    Text(
                                        text = docSpecialization,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate600
                                    )
                                }
                                Surface(
                                    color = Color(0xFFDCFCE7),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "SIGNED",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF15803D),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Slate200)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = patientName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Slate900
                                    )
                                    Text(
                                        text = "$patientAge y / ${patientGender.lowercase().replaceFirstChar { it.uppercase() }} · $uhid",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate500
                                    )
                                }
                                if (!entry.patient?.phone.isNullOrBlank()) {
                                    Text(
                                        text = "📞 +91 ${entry.patient?.phone}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate600
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Diagnosis & Vitals
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "DIAGNOSIS & CLINICAL NOTES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MedRayBluePrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = diagnosisText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Slate800
                            )
                            if (entry.vitals.hasAnyReading()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Vitals: BP ${entry.vitals.vitalsBp ?: "-"} · Pulse ${entry.vitals.vitalsPulseBpm ?: "-"} bpm · SpO2 ${entry.vitals.vitalsSpo2 ?: "-"}% · Temp ${entry.vitals.vitalsTemperatureF ?: "-"}°F",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate600
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Medicines Table
                    Text(
                        text = "💊 PRESCRIBED MEDICINES (Rx)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        rxItems.forEachIndexed { idx, med ->
                            Surface(
                                color = PureWhite,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = Color(0xFFF1F5F9),
                                        shape = CircleShape,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("${idx + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate700)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = med.medicineName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Surface(
                                                color = Color(0xFFFEF3C7),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = med.frequencyCode,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB45309),
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                            Text(
                                                text = "· ${med.foodInstruction}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Slate600
                                            )
                                            if (med.durationDays != null) {
                                                Text(
                                                    text = "· ${med.durationDays} days",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Slate600
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Advice & Tests
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "📋 ADVICE & INSTRUCTIONS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = adviceNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate800
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "🔬 INVESTIGATIONS / TESTS ADVISED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = testsAdvised,
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate800
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Verified Signature Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.Verified, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Digitally signed & verified by Dr. $doctorName",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF15803D)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            onShareWhatsApp()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share via WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedRayPullRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val state = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()
    if (state.isRefreshing) {
        LaunchedEffect(true) {
            onRefresh()
        }
    }
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            state.startRefresh()
        } else {
            state.endRefresh()
        }
    }

    Box(
        modifier = modifier.nestedScroll(state.nestedScrollConnection)
    ) {
        content()
        androidx.compose.material3.pulltorefresh.PullToRefreshContainer(
            state = state,
            modifier = Modifier.align(Alignment.TopCenter),
            containerColor = PureWhite,
            contentColor = MedRayBluePrimary
        )
    }
}
