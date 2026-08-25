package ai.medray.staff.ui.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ai.medray.staff.data.model.*
import ai.medray.staff.data.network.*
import ai.medray.staff.data.repository.*
import ai.medray.staff.ui.appointments.AppointmentsScreen
import ai.medray.staff.ui.auth.LoginScreen
import ai.medray.staff.ui.auth.OtpVerificationScreen
import ai.medray.staff.ui.billing.BillingScreen
import ai.medray.staff.ui.common.*
import ai.medray.staff.ui.nurse.FastVitalsEntryDialog
import ai.medray.staff.ui.nurse.NurseHomeScreen
import ai.medray.staff.ui.patients.PatientsScreen
import ai.medray.staff.ui.profile.ProfileScreen
import ai.medray.staff.ui.reception.ReceptionHomeScreen
import ai.medray.staff.ui.reception.WalkInRegisterDialog
import ai.medray.staff.ui.selfcheckins.SelfCheckInsScreen
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Otp : Screen("otp")
    object Queue : Screen("queue")
    object Patients : Screen("patients")
    object Appointments : Screen("appointments")
    object Billing : Screen("billing")
    object SelfCheckIns : Screen("self_checkins")
    object Profile : Screen("profile")
}

data class UpiPaymentModalData(
    val payeeVpa: String,
    val payeeName: String,
    val amount: Double,
    val invoiceNumber: String,
    val queueEntryId: String? = null
)

@Composable
fun StaffAppNavHost(
    authRepo: AuthRepository,
    queueRepo: QueueRepository,
    patientRepo: PatientRepository,
    appointmentRepo: AppointmentRepository,
    billingRepo: BillingRepository,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val googleProvider = remember { GoogleIdTokenProvider() }

    var currentUser by remember { mutableStateOf<User?>(null) }
    var phoneInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var authError by remember { mutableStateOf<String?>(null) }
    var isAuthLoading by remember { mutableStateOf(false) }
    var isPasswordLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }

    // Data States
    var queueEntries by remember { mutableStateOf<List<QueueEntry>>(emptyList()) }
    var patientsList by remember { mutableStateOf<List<Patient>>(emptyList()) }
    var appointmentsList by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var invoicesList by remember { mutableStateOf<List<Invoice>>(emptyList()) }
    var selfCheckInsList by remember { mutableStateOf<List<SelfCheckIn>>(emptyList()) }
    var doctors by remember { mutableStateOf<List<DoctorSummary>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDoctorId by remember { mutableStateOf<String?>(null) }

    // Dialog States
    var vitalsTargetEntry by remember { mutableStateOf<QueueEntry?>(null) }
    var showWalkInDialog by remember { mutableStateOf(false) }
    var upiModalData by remember { mutableStateOf<UpiPaymentModalData?>(null) }

    // Check initial auth on launch
    LaunchedEffect(Unit) {
        if (authRepo.isLoggedIn()) {
            val res = authRepo.getMe()
            if (res.isSuccess) {
                currentUser = res.getOrNull()
                navController.navigate(Screen.Queue.route) { popUpTo(0) }
            }
        }
    }

    // Refresh data coordinator
    fun refreshAllData() {
        coroutineScope.launch {
            val qRes = queueRepo.refreshQueue()
            if (qRes.isSuccess) queueEntries = qRes.getOrDefault(emptyList())

            val pRes = patientRepo.searchPatients("")
            if (pRes.isSuccess) patientsList = pRes.getOrDefault(emptyList())

            val aRes = appointmentRepo.listAppointments()
            if (aRes.isSuccess) appointmentsList = aRes.getOrDefault(emptyList())

            val bRes = billingRepo.listInvoices()
            if (bRes.isSuccess) invoicesList = bRes.getOrDefault(emptyList())
        }
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            refreshAllData()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Login.route
    val isAppAuthenticated = currentUser != null && currentRoute != Screen.Login.route && currentRoute != Screen.Otp.route

    val screenTitle = when (currentRoute) {
        Screen.Queue.route -> if (currentUser?.isNurse == true) "Triage Queue" else "OPD Queue"
        Screen.Patients.route -> "Patients Directory"
        Screen.Appointments.route -> "Appointments Schedule"
        Screen.Billing.route -> "Billing & Payments"
        Screen.SelfCheckIns.route -> "Self Check-In Kiosk"
        Screen.Profile.route -> "Staff Profile"
        else -> "MedRay Staff"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isAppAuthenticated,
        drawerContent = {
            if (isAppAuthenticated) {
                MedRayDrawerContent(
                    user = currentUser,
                    currentRoute = currentRoute,
                    pendingSelfCheckInCount = selfCheckInsList.size,
                    onNavigate = { route ->
                        coroutineScope.launch { drawerState.close() }
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo(Screen.Queue.route)
                                launchSingleTop = true
                            }
                        }
                    },
                    onLogout = {
                        coroutineScope.launch {
                            drawerState.close()
                            authRepo.logout()
                            currentUser = null
                            navController.navigate(Screen.Login.route) { popUpTo(0) }
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (isAppAuthenticated) {
                    MedRayTopBar(
                        title = screenTitle,
                        subtitle = currentUser?.clinic?.name ?: "Main Clinic",
                        user = currentUser,
                        onMenuClick = {
                            coroutineScope.launch { drawerState.open() }
                        },
                        onLogoutClick = {
                            authRepo.logout()
                            currentUser = null
                            navController.navigate(Screen.Login.route) { popUpTo(0) }
                        }
                    )
                }
            },
            bottomBar = {
                if (isAppAuthenticated) {
                    val bottomNavItems = if (currentUser?.isNurse == true) {
                        listOf(
                            BottomNavItem(Screen.Queue.route, "Triage", Icons.AutoMirrored.Filled.ListAlt, Icons.AutoMirrored.Outlined.ListAlt),
                            BottomNavItem(Screen.Patients.route, "Patients", Icons.Filled.People, Icons.Outlined.People),
                            BottomNavItem(Screen.Appointments.route, "Appointments", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
                            BottomNavItem(Screen.Profile.route, "Profile", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
                        )
                    } else {
                        listOf(
                            BottomNavItem(Screen.Queue.route, "OPD Queue", Icons.AutoMirrored.Filled.ListAlt, Icons.AutoMirrored.Outlined.ListAlt),
                            BottomNavItem(Screen.Patients.route, "Patients", Icons.Filled.People, Icons.Outlined.People),
                            BottomNavItem(Screen.Billing.route, "Billing", Icons.Filled.QrCode, Icons.Outlined.QrCode),
                            BottomNavItem(Screen.SelfCheckIns.route, "Check-Ins", Icons.Filled.HowToReg, Icons.Outlined.HowToReg)
                        )
                    }

                    MedRayBottomNav(
                        items = bottomNavItems,
                        currentRoute = currentRoute,
                        onItemClick = { route ->
                            if (route != currentRoute) {
                                navController.navigate(route) {
                                    popUpTo(Screen.Queue.route)
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Login.route,
                modifier = modifier.padding(innerPadding)
            ) {
                // 1. Login Screen
                composable(Screen.Login.route) {
                    LoginScreen(
                        phone = phoneInput,
                        onPhoneChange = { phoneInput = it },
                        onSendOtp = {
                            isAuthLoading = true
                            authError = null
                            coroutineScope.launch {
                                val res = authRepo.sendOtp(phoneInput)
                                isAuthLoading = false
                                if (res.isSuccess) {
                                    navController.navigate(Screen.Otp.route)
                                } else {
                                    authError = res.exceptionOrNull()?.message ?: "Failed to send OTP"
                                }
                            }
                        },
                        isSendingOtp = isAuthLoading,
                        email = emailInput,
                        onEmailChange = { emailInput = it },
                        password = passwordInput,
                        onPasswordChange = { passwordInput = it },
                        onPasswordLogin = {
                            isPasswordLoading = true
                            authError = null
                            coroutineScope.launch {
                                val res = authRepo.loginWithPassword(emailInput, passwordInput)
                                isPasswordLoading = false
                                if (res.isSuccess) {
                                    currentUser = res.getOrNull()
                                    navController.navigate(Screen.Queue.route) { popUpTo(0) }
                                } else {
                                    authError = res.exceptionOrNull()?.message ?: "Login failed"
                                }
                            }
                        },
                        isPasswordLoggingIn = isPasswordLoading,
                        onGoogleSignIn = {
                            val activity = context as? Activity ?: return@LoginScreen
                            isGoogleLoading = true
                            authError = null
                            coroutineScope.launch {
                                val tokenRes = googleProvider.requestIdToken(activity)
                                if (tokenRes.isSuccess) {
                                    val idToken = tokenRes.getOrNull()!!
                                    val authRes = authRepo.signInWithGoogle(idToken)
                                    isGoogleLoading = false
                                    if (authRes.isSuccess) {
                                        currentUser = authRes.getOrNull()
                                        navController.navigate(Screen.Queue.route) { popUpTo(0) }
                                    } else {
                                        authError = authRes.exceptionOrNull()?.message ?: "Google sign-in failed on server"
                                    }
                                } else {
                                    isGoogleLoading = false
                                    authError = tokenRes.exceptionOrNull()?.message ?: "Google Sign-In cancelled"
                                }
                            }
                        },
                        isGoogleSigningIn = isGoogleLoading,
                        error = authError
                    )
                }

                // 2. OTP Verification Screen
                composable(Screen.Otp.route) {
                    OtpVerificationScreen(
                        phone = phoneInput,
                        otpCode = otpInput,
                        onOtpChange = { otpInput = it },
                        onVerify = {
                            isAuthLoading = true
                            authError = null
                            coroutineScope.launch {
                                val res = authRepo.verifyOtp(phoneInput, otpInput)
                                isAuthLoading = false
                                if (res.isSuccess) {
                                    currentUser = res.getOrNull()
                                    navController.navigate(Screen.Queue.route) { popUpTo(0) }
                                } else {
                                    authError = res.exceptionOrNull()?.message ?: "Invalid OTP"
                                }
                            }
                        },
                        onResend = {
                            coroutineScope.launch {
                                authRepo.sendOtp(phoneInput)
                                Toast.makeText(context, "Verification OTP resent", Toast.LENGTH_SHORT).show()
                            }
                        },
                        isVerifying = isAuthLoading,
                        error = authError
                    )
                }

                // 3. Queue Screen (Nurse / Receptionist)
                composable(Screen.Queue.route) {
                    if (currentUser?.isNurse == true) {
                        NurseHomeScreen(
                            queue = queueEntries,
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            onRecordVitalsClick = { entry -> vitalsTargetEntry = entry },
                            onScanDocumentClick = { entry ->
                                Toast.makeText(context, "Document Scanner opened for ${entry.patient?.fullName}", Toast.LENGTH_SHORT).show()
                            },
                            onStatusChange = { entry, newStatus ->
                                coroutineScope.launch {
                                    queueRepo.updateStatus(entry.id, newStatus)
                                    refreshAllData()
                                }
                            }
                        )
                    } else {
                        ReceptionHomeScreen(
                            queue = queueEntries,
                            doctors = doctors,
                            selectedDoctorId = selectedDoctorId,
                            onDoctorFilterChange = { selectedDoctorId = it },
                            onNewWalkInClick = { showWalkInDialog = true },
                            onStatusChange = { entry, newStatus ->
                                coroutineScope.launch {
                                    queueRepo.updateStatus(entry.id, newStatus)
                                    refreshAllData()
                                }
                            },
                            onCollectPaymentClick = { entry ->
                                upiModalData = UpiPaymentModalData(
                                    payeeVpa = currentUser?.clinic?.upiVpa ?: "medray@upi",
                                    payeeName = currentUser?.clinic?.name ?: "MedRay AI Clinic",
                                    amount = currentUser?.clinic?.defaultConsultationFee ?: 500.0,
                                    invoiceNumber = entry.opdNumber,
                                    queueEntryId = entry.id
                                )
                            },
                            onWhatsAppClick = { entry ->
                                val phone = entry.patient?.phone ?: ""
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/91$phone?text=Hello%20${entry.patient?.fullName},%20your%20OPD%20token%20is%20${entry.opdNumber}"))
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                // 4. Patients Screen
                composable(Screen.Patients.route) {
                    PatientsScreen(
                        patients = patientsList,
                        searchQuery = searchQuery,
                        onSearchChange = { query ->
                            searchQuery = query
                            coroutineScope.launch {
                                val res = patientRepo.searchPatients(query)
                                if (res.isSuccess) patientsList = res.getOrDefault(emptyList())
                            }
                        },
                        onPatientClick = { patient ->
                            Toast.makeText(context, "Patient: ${patient.fullName} (UHID: ${patient.uhid})", Toast.LENGTH_SHORT).show()
                        },
                        onRegisterPatientClick = { showWalkInDialog = true }
                    )
                }

                // 5. Appointments Screen
                composable(Screen.Appointments.route) {
                    AppointmentsScreen(
                        appointments = appointmentsList,
                        onCheckInClick = { appt ->
                            coroutineScope.launch {
                                val res = appointmentRepo.checkIn(appt.id, null)
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Patient Checked In!", Toast.LENGTH_SHORT).show()
                                    refreshAllData()
                                }
                            }
                        },
                        onCancelClick = { appt ->
                            coroutineScope.launch {
                                val res = appointmentRepo.cancel(appt.id, "Cancelled at desk")
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Appointment Cancelled", Toast.LENGTH_SHORT).show()
                                    refreshAllData()
                                }
                            }
                        },
                        onRefresh = { refreshAllData() }
                    )
                }

                // 6. Billing Screen
                composable(Screen.Billing.route) {
                    BillingScreen(
                        invoices = invoicesList,
                        onCollectPaymentClick = { invoice ->
                            upiModalData = UpiPaymentModalData(
                                payeeVpa = currentUser?.clinic?.upiVpa ?: "medray@upi",
                                payeeName = currentUser?.clinic?.name ?: "MedRay AI Clinic",
                                amount = invoice.total,
                                invoiceNumber = invoice.invoiceNumber
                            )
                        },
                        onRefresh = { refreshAllData() }
                    )
                }

                // 7. Self Check-Ins Screen
                composable(Screen.SelfCheckIns.route) {
                    SelfCheckInsScreen(
                        checkIns = selfCheckInsList,
                        onAssignClick = { checkIn ->
                            showWalkInDialog = true
                        },
                        onRefresh = { refreshAllData() }
                    )
                }

                // 8. Profile Screen
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        user = currentUser,
                        onLogout = {
                            authRepo.logout()
                            currentUser = null
                            navController.navigate(Screen.Login.route) { popUpTo(0) }
                        }
                    )
                }
            }
        }
    }

    // Modal Dialogs
    vitalsTargetEntry?.let { entry ->
        FastVitalsEntryDialog(
            initialVitals = entry.vitals,
            patientName = entry.patient?.fullName ?: "Patient",
            onDismiss = { vitalsTargetEntry = null },
            onSave = { updatedVitals ->
                coroutineScope.launch {
                    queueRepo.updateVitals(entry.id, updatedVitals)
                    vitalsTargetEntry = null
                    refreshAllData()
                    Toast.makeText(context, "Vitals saved & synced with Doctor Tablet", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showWalkInDialog) {
        WalkInRegisterDialog(
            doctors = doctors,
            onDismiss = { showWalkInDialog = false },
            onRegister = { patientName, phone, doctorId, complaint, age, gender ->
                coroutineScope.launch {
                    val pRes = patientRepo.registerPatient(
                        RegisterPatientRequest(
                            fullName = patientName,
                            phone = phone,
                            age = age,
                            gender = gender
                        )
                    )
                    if (pRes.isSuccess) {
                        val newPatient = pRes.getOrNull()!!
                        queueRepo.registerQueueEntry(
                            patientId = newPatient.id,
                            doctorId = doctorId,
                            chiefComplaint = complaint
                        )
                        showWalkInDialog = false
                        refreshAllData()
                        Toast.makeText(context, "Walk-In Registered! Token issued.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to register patient", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    upiModalData?.let { data ->
        DynamicUpiQrDialog(
            payeeVpa = data.payeeVpa,
            payeeName = data.payeeName,
            amount = data.amount,
            invoiceNumber = data.invoiceNumber,
            onDismiss = { upiModalData = null },
            onMarkPaid = {
                coroutineScope.launch {
                    if (data.queueEntryId != null) {
                        queueRepo.updateStatus(data.queueEntryId, QueueStatus.WAITING)
                    }
                    upiModalData = null
                    refreshAllData()
                    Toast.makeText(context, "Payment of ₹${data.amount.toInt()} marked as PAID", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}
