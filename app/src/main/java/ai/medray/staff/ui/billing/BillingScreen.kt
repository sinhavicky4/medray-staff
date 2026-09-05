package ai.medray.staff.ui.billing

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.medray.staff.data.model.Invoice
import ai.medray.staff.data.model.InvoiceStatus
import ai.medray.staff.ui.common.MedRayPullRefreshBox
import ai.medray.staff.ui.common.QuickFilterPill
import ai.medray.staff.ui.common.StatCard
import ai.medray.staff.ui.theme.*

@Composable
fun BillingScreen(
    invoices: List<Invoice>,
    onCollectPaymentClick: (Invoice) -> Unit,
    onInvoiceClick: (Invoice) -> Unit = {},
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<String>("ALL") }

    val paidCount = invoices.count { it.status == InvoiceStatus.PAID }
    val pendingCount = invoices.count { it.status == InvoiceStatus.ISSUED || it.status == InvoiceStatus.PARTIALLY_PAID || it.status == InvoiceStatus.DRAFT }
    val totalCollected = invoices.filter { it.status == InvoiceStatus.PAID }.sumOf { it.total }
    val totalPending = invoices.filter { it.status != InvoiceStatus.PAID && it.status != InvoiceStatus.CANCELLED }.sumOf { it.total }

    val filtered = remember(invoices, searchQuery, selectedFilter) {
        invoices.filter { invoice ->
            val matchesFilter = when (selectedFilter) {
                "PENDING" -> invoice.status != InvoiceStatus.PAID && invoice.status != InvoiceStatus.CANCELLED
                "PAID" -> invoice.status == InvoiceStatus.PAID
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                invoice.invoiceNumber.lowercase().contains(q) ||
                        invoice.patient?.fullName?.lowercase()?.contains(q) == true ||
                        invoice.patient?.phone?.contains(q) == true
            }
            matchesFilter && matchesSearch
        }
    }

    MedRayPullRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
        // 1. Header & Refresh
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Billing & UPI Payments",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = "Dynamic on-screen UPI QR & instant receipts",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(38.dp)
                        .background(PureWhite, RoundedCornerShape(10.dp))
                        .border(1.dp, Slate200, RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = MedRayBluePrimary, modifier = Modifier.size(18.dp))
                }
            }
        }

        // 2. Interactive KPI Stat Cards (2x2 Grid)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    StatCard(
                        title = "Total Collected",
                        value = "₹${totalCollected.toInt()}",
                        footer = "$paidCount settled invoices",
                        icon = Icons.Filled.Payments,
                        iconBg = Color(0xFFDCFCE7),
                        iconTint = Color(0xFF16A34A),
                        isSelected = selectedFilter == "PAID",
                        onClick = { selectedFilter = if (selectedFilter == "PAID") "ALL" else "PAID" },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    StatCard(
                        title = "Pending Due",
                        value = "₹${totalPending.toInt()}",
                        footer = "$pendingCount unpaid bills",
                        icon = Icons.Filled.PendingActions,
                        iconBg = Color(0xFFFEF3C7),
                        iconTint = Color(0xFFD97706),
                        isSelected = selectedFilter == "PENDING",
                        onClick = { selectedFilter = if (selectedFilter == "PENDING") "ALL" else "PENDING" },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }

        // 3. Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search invoice #, patient, or phone…", fontSize = 13.sp, color = Slate400) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MedRayBluePrimary, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
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
                    label = "All Invoices (${invoices.size})",
                    isSelected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" }
                )
                QuickFilterPill(
                    label = "Pending ($pendingCount)",
                    isSelected = selectedFilter == "PENDING",
                    dotColor = Color(0xFFD97706),
                    onClick = { selectedFilter = "PENDING" }
                )
                QuickFilterPill(
                    label = "Paid ($paidCount)",
                    isSelected = selectedFilter == "PAID",
                    dotColor = Color(0xFF16A34A),
                    onClick = { selectedFilter = "PAID" }
                )
            }
        }

        // 5. Invoices List
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
                            Icons.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = Slate300,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching invoices" else "No invoices generated yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate700
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "New OPD tokens and completed consultations will generate invoices here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { invoice ->
                InvoiceCard(
                    invoice = invoice,
                    onCollectPayment = { onCollectPaymentClick(invoice) },
                    onClick = { onInvoiceClick(invoice) }
                )
            }
        }
    }
}
}

@Composable
fun InvoiceCard(
    invoice: Invoice,
    onCollectPayment: () -> Unit,
    onClick: () -> Unit = {}
) {
    val patient = invoice.patient
    val isPaid = invoice.status == InvoiceStatus.PAID
    val statusBg = if (isPaid) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
    val statusText = if (isPaid) Color(0xFF16A34A) else Color(0xFFD97706)
    val statusDot = if (isPaid) Color(0xFF22C55E) else Color(0xFFF59E0B)

    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        shadowElevation = 1.dp,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Invoice Number & Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                ) {
                    Surface(
                        color = MedRayBlueLight,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "INV-${invoice.invoiceNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MedRayBluePrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(statusDot, CircleShape))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isPaid) "PAID" else "PENDING DUE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body: Patient Name & Billing Amount
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = patient?.fullName ?: "OPD Patient",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        if (!patient?.uhid.isNullOrBlank()) {
                            Text(
                                text = "UHID: ${patient?.uhid}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (!patient?.phone.isNullOrBlank()) {
                            Text(
                                text = "· 📞 +91 ${patient?.phone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${invoice.total.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaid) Color(0xFF16A34A) else MedRayBluePrimary
                    )
                    Text(
                        text = "OPD Fee",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400
                    )
                }
            }

            // Action: UPI QR Collection Button if unpaid
            if (!isPaid) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onCollectPayment,
                    colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Collect ₹${invoice.total.toInt()} via UPI QR",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
