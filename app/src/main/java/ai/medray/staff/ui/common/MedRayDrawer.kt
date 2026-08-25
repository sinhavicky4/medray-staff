package ai.medray.staff.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.medray.staff.BuildConfig
import ai.medray.staff.data.model.User
import ai.medray.staff.ui.theme.*

data class DrawerMenuItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun MedRayDrawerContent(
    user: User?,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    pendingSelfCheckInCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        DrawerMenuItem(
            route = "queue",
            title = if (user?.isNurse == true) "Triage Queue" else "OPD Queue",
            selectedIcon = Icons.AutoMirrored.Filled.ListAlt,
            unselectedIcon = Icons.AutoMirrored.Outlined.ListAlt
        ),
        DrawerMenuItem(
            route = "patients",
            title = "Patients Directory",
            selectedIcon = Icons.Filled.People,
            unselectedIcon = Icons.Outlined.People
        ),
        DrawerMenuItem(
            route = "appointments",
            title = "Appointments",
            selectedIcon = Icons.Filled.CalendarMonth,
            unselectedIcon = Icons.Outlined.CalendarMonth
        ),
        DrawerMenuItem(
            route = "billing",
            title = "Billing & UPI QR",
            selectedIcon = Icons.Filled.QrCode,
            unselectedIcon = Icons.Outlined.QrCode
        ),
        DrawerMenuItem(
            route = "self_checkins",
            title = "Self Check-Ins",
            selectedIcon = Icons.Filled.HowToReg,
            unselectedIcon = Icons.Outlined.HowToReg,
            badgeCount = pendingSelfCheckInCount
        ),
        DrawerMenuItem(
            route = "profile",
            title = "Staff Profile",
            selectedIcon = Icons.Filled.AccountCircle,
            unselectedIcon = Icons.Outlined.AccountCircle
        )
    )

    ModalDrawerSheet(
        drawerContainerColor = PureWhite,
        drawerContentColor = Slate900,
        modifier = modifier.width(320.dp).fillMaxHeight()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // Header: Brand & Clinic Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(PureWhite, RoundedCornerShape(11.dp))
                        .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(11.dp))
                        .padding(5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = ai.medray.staff.R.drawable.ic_medray_logo),
                        contentDescription = "MedRay AI Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "MedRay AI Staff",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = user?.clinic?.name ?: "Main Clinic",
                        style = MaterialTheme.typography.bodySmall,
                        color = MedRayTealPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // User Profile Card
            Surface(
                color = Slate50,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    // Avatar
                    Surface(
                        color = MedRayBluePrimary,
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = user?.fullName?.take(2)?.uppercase() ?: "ST",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user?.fullName ?: "Staff Member",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            val roleBg = if (user?.isNurse == true) MedRayTealContainer else MedRayBlueContainer
                            val roleColor = if (user?.isNurse == true) MedRayTealDark else MedRayBlueDark
                            Surface(
                                color = roleBg,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (user?.isNurse == true) "NURSE" else "RECEPTIONIST",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = roleColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Slate200, modifier = Modifier.padding(bottom = 12.dp))

            // Navigation Items
            Column(
                modifier = Modifier.weight(1f)
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title,
                                tint = if (isSelected) MedRayBluePrimary else Slate600
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MedRayBluePrimary else Slate800
                            )
                        },
                        badge = {
                            if (item.badgeCount > 0) {
                                Badge(
                                    containerColor = StatusWarningBg,
                                    contentColor = StatusWarningText
                                ) {
                                    Text(item.badgeCount.toString(), fontWeight = FontWeight.Bold)
                                }
                            }
                        },
                        selected = isSelected,
                        onClick = { onNavigate(item.route) },
                        shape = RoundedCornerShape(12.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MedRayBlueLight,
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 12.dp))

            // Logout Button
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLogout() }
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Sign Out",
                        tint = StatusErrorText,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Sign Out",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = StatusErrorText
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Version info footer
            Text(
                text = "${BuildConfig.VERSION_NAME_DISPLAY} · Online",
                style = MaterialTheme.typography.labelSmall,
                color = Slate400,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}
