package ai.medray.staff.ui.navigation

import ai.medray.staff.ui.common.PrescriptionViewerDialog

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import ai.medray.staff.ui.theme.*
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
import ai.medray.staff.ui.splash.SplashScreen
import ai.medray.staff.ui.auth.LoginScreen
import ai.medray.staff.ui.auth.OtpVerificationScreen
import ai.medray.staff.ui.billing.BillingScreen
import ai.medray.staff.ui.common.*
import ai.medray.staff.ui.nurse.FastVitalsEntryDialog
import ai.medray.staff.ui.nurse.NurseHomeScreen
import ai.medray.staff.ui.patients.PatientsScreen
import ai.medray.staff.ui.profile.ProfileScreen
import ai.medray.staff.ui.reception.AddToQueueForPatientDialog
import ai.medray.staff.ui.reception.ReceptionHomeScreen
import ai.medray.staff.ui.reception.WalkInRegisterDialog
import ai.medray.staff.ui.selfcheckins.SelfCheckInsScreen
import kotlinx.coroutines.launch


fun getSampleQueue(): List<QueueEntry> {
    return listOf(
        QueueEntry(
            id = "mock-1",
            clinicId = "clinic-1",
            patientId = "pat-1",
            doctorId = "doc-1",
            opdNumber = "Token #1 · OPD-2026-001",
            chiefComplaint = "High fever (102°F) and acute throat pain for 3 days",
            status = QueueStatus.ARRIVED,
            scheduledAt = "2026-08-25T10:00:00Z",
            createdAt = "2026-08-25T09:45:00Z",
            createdBy = "Front Desk",
            vitalsBp = "120/80",
            vitalsPulseBpm = 78,
            vitalsSpo2 = 98,
            vitalsTemperatureF = 101.8,
            vitalsWeightKg = 68.0,
            vitalsHeightCm = 172.0,
            patient = Patient(
                id = "pat-1",
                clinicId = "clinic-1",
                fullName = "Aarav Sharma",
                uhid = "MR-2026-0182",
                phone = "9876543210",
                age = 34,
                gender = "MALE"
            ),
            doctor = DoctorSummary(
                id = "doc-1",
                fullName = "Rajesh Sharma",
                specialization = "General Physician"
            )
        ),
        QueueEntry(
            id = "mock-2",
            clinicId = "clinic-1",
            patientId = "pat-2",
            doctorId = "doc-1",
            opdNumber = "Token #2 · OPD-2026-002",
            chiefComplaint = "Severe headache, dizziness, and elevated blood pressure",
            status = QueueStatus.WAITING,
            scheduledAt = "2026-08-25T10:15:00Z",
            createdAt = "2026-08-25T10:05:00Z",
            createdBy = "Kiosk Check-In",
            vitalsBp = "150/95",
            vitalsPulseBpm = 86,
            vitalsSpo2 = 97,
            vitalsTemperatureF = 98.4,
            vitalsWeightKg = 74.5,
            vitalsHeightCm = 168.0,
            patient = Patient(
                id = "pat-2",
                clinicId = "clinic-1",
                fullName = "Pooja Verma",
                uhid = "MR-2026-0195",
                phone = "9812345678",
                age = 42,
                gender = "FEMALE"
            ),
            doctor = DoctorSummary(
                id = "doc-1",
                fullName = "Rajesh Sharma",
                specialization = "General Physician"
            )
        ),
        QueueEntry(
            id = "mock-3",
            clinicId = "clinic-1",
            patientId = "pat-3",
            doctorId = "doc-2",
            opdNumber = "Token #3 · OPD-2026-003",
            chiefComplaint = "Routine hypertension follow-up & prescription renewal",
            status = QueueStatus.IN_PROGRESS,
            scheduledAt = "2026-08-25T10:30:00Z",
            createdAt = "2026-08-25T10:20:00Z",
            createdBy = "Sister Anita (Nurse)",
            vitalsBp = null,
            vitalsPulseBpm = null,
            vitalsSpo2 = null,
            vitalsTemperatureF = null,
            vitalsWeightKg = null,
            vitalsHeightCm = null,
            patient = Patient(
                id = "pat-3",
                clinicId = "clinic-1",
                fullName = "Rohan Mehta",
                uhid = "MR-2026-0204",
                phone = "9765432109",
                age = 58,
                gender = "MALE"
            ),
            doctor = DoctorSummary(
                id = "doc-2",
                fullName = "Ananya Roy",
                specialization = "Cardiologist"
            )
        ),
        QueueEntry(
            id = "mock-4",
            clinicId = "clinic-1",
            patientId = "pat-4",
            doctorId = "doc-2",
            opdNumber = "Token #4 · OPD-2026-004",
            chiefComplaint = "Chest congestion, seasonal allergies, dry cough",
            status = QueueStatus.COMPLETED,
            scheduledAt = "2026-08-25T09:30:00Z",
            createdAt = "2026-08-25T09:15:00Z",
            createdBy = "Front Desk",
            vitalsBp = "118/76",
            vitalsPulseBpm = 72,
            vitalsSpo2 = 99,
            vitalsTemperatureF = 98.6,
            vitalsWeightKg = 62.0,
            vitalsHeightCm = 165.0,
            patient = Patient(
                id = "pat-4",
                clinicId = "clinic-1",
                fullName = "Deepak Patel",
                uhid = "MR-2026-0158",
                phone = "9988776655",
                age = 29,
                gender = "MALE"
            ),
            doctor = DoctorSummary(
                id = "doc-2",
                fullName = "Ananya Roy",
                specialization = "Cardiologist"
            )
        )
    )
}


fun getSampleAppointments(): List<Appointment> {
    return listOf(
        Appointment(
            id = "appt-1",
            clinicId = "clinic-1",
            patientId = "pat-1",
            doctorId = "doc-1",
            scheduledAt = "2026-08-25T10:00:00Z",
            chiefComplaint = "General Physician Consultation",
            status = AppointmentStatus.SCHEDULED,
            patient = Patient(
                id = "pat-1",
                clinicId = "clinic-1",
                fullName = "Aarav Sharma",
                uhid = "10008",
                phone = "9876543210",
                age = 34,
                gender = "MALE"
            ),
            doctor = DoctorSummary("doc-1", "Rajesh Sharma", "General Physician")
        ),
        Appointment(
            id = "appt-2",
            clinicId = "clinic-1",
            patientId = "pat-2",
            doctorId = "doc-1",
            scheduledAt = "2026-08-25T10:30:00Z",
            chiefComplaint = "Fever and cold symptoms",
            status = AppointmentStatus.CHECKED_IN,
            patient = Patient(
                id = "pat-2",
                clinicId = "clinic-1",
                fullName = "Pooja Verma",
                uhid = "10007",
                phone = "9812345678",
                age = 42,
                gender = "FEMALE"
            ),
            doctor = DoctorSummary("doc-1", "Rajesh Sharma", "General Physician")
        ),
        Appointment(
            id = "appt-3",
            clinicId = "clinic-1",
            patientId = "pat-3",
            doctorId = "doc-2",
            scheduledAt = "2026-08-25T11:00:00Z",
            chiefComplaint = "Cardiology follow up check",
            status = AppointmentStatus.COMPLETED,
            patient = Patient(
                id = "pat-3",
                clinicId = "clinic-1",
                fullName = "Rohan Mehta",
                uhid = "10006",
                phone = "9765432109",
                age = 58,
                gender = "MALE"
            ),
            doctor = DoctorSummary("doc-2", "Ananya Roy", "Cardiologist")
        )
    )
}

fun getSampleInvoices(): List<Invoice> {
    return listOf(
        Invoice(
            id = "inv-1",
            clinicId = "clinic-1",
            invoiceNumber = "20260825-001",
            patientId = "pat-1",
            status = InvoiceStatus.PAID,
            total = 500.0,
            issuedAt = "2026-08-25T09:30:00Z",
            patient = Patient(
                id = "pat-1",
                clinicId = "clinic-1",
                fullName = "Aarav Sharma",
                uhid = "10008",
                phone = "9876543210"
            )
        ),
        Invoice(
            id = "inv-2",
            clinicId = "clinic-1",
            invoiceNumber = "20260825-002",
            patientId = "pat-2",
            status = InvoiceStatus.ISSUED,
            total = 500.0,
            issuedAt = "2026-08-25T10:00:00Z",
            patient = Patient(
                id = "pat-2",
                clinicId = "clinic-1",
                fullName = "Pooja Verma",
                uhid = "10007",
                phone = "9812345678"
            )
        ),
        Invoice(
            id = "inv-3",
            clinicId = "clinic-1",
            invoiceNumber = "20260825-003",
            patientId = "pat-3",
            status = InvoiceStatus.PAID,
            total = 800.0,
            issuedAt = "2026-08-25T10:15:00Z",
            patient = Patient(
                id = "pat-3",
                clinicId = "clinic-1",
                fullName = "Rohan Mehta",
                uhid = "10006",
                phone = "9765432109"
            )
        )
    )
}

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
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
    val invoiceId: String? = null,
    val queueEntryId: String? = null,
    val patientId: String? = null,
    val doctorId: String? = null,
    val doctorName: String? = null
)

@Composable
fun StaffAppNavHost(
    authRepo: AuthRepository,
    queueRepo: QueueRepository,
    patientRepo: PatientRepository,
    appointmentRepo: AppointmentRepository,
    billingRepo: BillingRepository,
    doctorRepo: DoctorRepository,
    selfCheckInRepo: SelfCheckInRepository,
    visitRepo: VisitRepository,
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

    val googlePlayServicesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val tokenRes = googleProvider.parseSignInResult(result.data)
        if (tokenRes.isSuccess) {
            isGoogleLoading = true
            authError = null
            coroutineScope.launch {
                val idToken = tokenRes.getOrNull()!!
                val authRes = authRepo.signInWithGoogle(idToken)
                isGoogleLoading = false
                if (authRes.isSuccess) {
                    currentUser = authRes.getOrNull()
                    navController.navigate(Screen.Queue.route) { popUpTo(0) }
                } else {
                    authError = authRes.exceptionOrNull()?.message ?: "Google sign-in failed on server"
                }
            }
        } else {
            isGoogleLoading = false
            authError = tokenRes.exceptionOrNull()?.message ?: "Google Sign-In cancelled"
        }
    }

    // Data States
    var queueEntries by remember { mutableStateOf<List<QueueEntry>>(getSampleQueue()) }
    var patientsList by remember { mutableStateOf<List<Patient>>(emptyList()) }
    var appointmentsList by remember { mutableStateOf<List<Appointment>>(getSampleAppointments()) }
    var invoicesList by remember { mutableStateOf<List<Invoice>>(getSampleInvoices()) }
    var selfCheckInsList by remember { mutableStateOf<List<SelfCheckIn>>(emptyList()) }
    var doctors by remember { mutableStateOf(listOf(DoctorSummary("doc-1", "Rajesh Sharma", "General Physician"), DoctorSummary("doc-2", "Ananya Roy", "Cardiologist"))) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDoctorId by remember { mutableStateOf<String?>(null) }

    // Dialog States
    var vitalsTargetEntry by remember { mutableStateOf<QueueEntry?>(null) }
    var rxTargetEntry by remember { mutableStateOf<QueueEntry?>(null) }
    var currentVisitForRx by remember { mutableStateOf<Visit?>(null) }
    var showWalkInDialog by remember { mutableStateOf(false) }
    var selectedPatientForDirectQueue by remember { mutableStateOf<Patient?>(null) }
    var showAddToQueueDialog by remember { mutableStateOf(false) }
    var upiModalData by remember { mutableStateOf<UpiPaymentModalData?>(null) }

// Initial auth check handled seamlessly in SplashScreen

    var isRefreshing by remember { mutableStateOf(false) }

    // Refresh data coordinator
    fun refreshAllData() {
        coroutineScope.launch {
            isRefreshing = true
            try {
                val qRes = queueRepo.refreshQueue()
                if (qRes.isSuccess) queueEntries = qRes.getOrDefault(emptyList())

                val pRes = patientRepo.searchPatients("")
                if (pRes.isSuccess) patientsList = pRes.getOrDefault(emptyList())

                val aRes = appointmentRepo.listAppointments()
                if (aRes.isSuccess) appointmentsList = aRes.getOrDefault(emptyList())

                val bRes = billingRepo.listInvoices()
                if (bRes.isSuccess) invoicesList = bRes.getOrDefault(emptyList())

                val dRes = doctorRepo.listDoctors()
                if (dRes.isSuccess) doctors = dRes.getOrDefault(doctors)

                val sRes = selfCheckInRepo.listPending()
                if (sRes.isSuccess) selfCheckInsList = sRes.getOrDefault(emptyList())
            } finally {
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            refreshAllData()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Login.route
    val isAppAuthenticated = currentUser != null && currentRoute != Screen.Login.route && currentRoute != Screen.Otp.route && currentRoute != Screen.Splash.route

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
                startDestination = Screen.Splash.route,
                modifier = modifier
                    .fillMaxSize()
                    .background(Slate50)
                    .padding(innerPadding)
            ) {
                // 0. Splash Screen
                composable(Screen.Splash.route) {
                    SplashScreen(
                        onFinished = {
                            coroutineScope.launch {
                                if (authRepo.isLoggedIn()) {
                                    val res = authRepo.getMe()
                                    if (res.isSuccess) {
                                        currentUser = res.getOrNull()
                                        navController.navigate(Screen.Queue.route) {
                                            popUpTo(Screen.Splash.route) { inclusive = true }
                                        }
                                        return@launch
                                    }
                                }
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            }
                        }
                    )
                }

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
                                    try {
                                        googlePlayServicesLauncher.launch(googleProvider.getSignInIntent(activity))
                                    } catch (e: Exception) {
                                        authError = tokenRes.exceptionOrNull()?.message ?: e.message ?: "Google Sign-In cancelled"
                                    }
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
                            userName = currentUser?.fullName,
                            isRefreshing = isRefreshing,
                            onRefresh = { refreshAllData() },
                            onSearchChange = { searchQuery = it },
                            onRecordVitalsClick = { entry -> vitalsTargetEntry = entry },
                            onViewPrescriptionClick = { entry ->
                                rxTargetEntry = entry
                                coroutineScope.launch {
                                    val res = visitRepo.getPatientVisits(entry.patientId)
                                    if (res.isSuccess) {
                                        currentVisitForRx = res.getOrNull()?.firstOrNull()
                                    }
                                }
                            },
                            onScanDocumentClick = { entry ->
                                Toast.makeText(context, "Document Scanner opened for ${entry.patient?.fullName}", Toast.LENGTH_SHORT).show()
                            },
                            onStatusChange = { entry, newStatus ->
                                queueEntries = queueEntries.map { if (it.id == entry.id) it.copy(status = newStatus) else it }
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
                            userName = currentUser?.fullName,
                            isRefreshing = isRefreshing,
                            onRefresh = { refreshAllData() },
                            onDoctorFilterChange = { selectedDoctorId = it },
                            onNewWalkInClick = { showWalkInDialog = true },
                            onRecordVitalsClick = { entry -> vitalsTargetEntry = entry },
                            onViewPrescriptionClick = { entry ->
                                rxTargetEntry = entry
                                coroutineScope.launch {
                                    val res = visitRepo.getPatientVisits(entry.patientId)
                                    if (res.isSuccess) {
                                        currentVisitForRx = res.getOrNull()?.firstOrNull()
                                    }
                                }
                            },
                            onStatusChange = { entry, newStatus ->
                                queueEntries = queueEntries.map { if (it.id == entry.id) it.copy(status = newStatus) else it }
                                coroutineScope.launch {
                                    queueRepo.updateStatus(entry.id, newStatus)
                                    refreshAllData()
                                }
                            },
                            onCollectPaymentClick = { entry ->
                                // No fallback to "medray@upi" — a QR that looks legitimate
                                // but pays into a placeholder VPA instead of this clinic's
                                // real account would misdirect a patient's money with no
                                // indication anything was wrong. Block entirely until a
                                // real UPI ID is configured (web: Clinic Settings).
                                val configuredUpiId = currentUser?.clinic?.upiId?.ifBlank { null } ?: currentUser?.clinic?.upiVpa?.ifBlank { null }
                                if (configuredUpiId == null) {
                                    Toast.makeText(context, "This clinic hasn't set up a UPI ID yet. Ask your Clinic Admin to add one in Clinic Settings on the web portal.", Toast.LENGTH_LONG).show()
                                } else {
                                    upiModalData = UpiPaymentModalData(
                                        payeeVpa = configuredUpiId,
                                        payeeName = currentUser?.clinic?.name ?: "MedRay AI Clinic",
                                        amount = currentUser?.clinic?.defaultConsultationFee ?: 500.0,
                                        invoiceNumber = entry.opdNumber,
                                        queueEntryId = entry.id,
                                        patientId = entry.patientId,
                                        doctorId = entry.doctorId,
                                        doctorName = entry.doctor?.fullName
                                    )
                                }
                            },
                            onWhatsAppClick = { entry ->
                                val phone = entry.patient?.phone ?: ""
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/91$phone?text=Hello%20${entry.patient?.fullName},%20your%20OPD%20token%20is%20${entry.opdNumber}"))
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }

                // 4. Patients Screen
                composable(Screen.Patients.route) {
                    PatientsScreen(
                        patients = patientsList,
                        searchQuery = searchQuery,
                        isRefreshing = isRefreshing,
                        onRefresh = { refreshAllData() },
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
                        onRegisterPatientClick = { showWalkInDialog = true },
                        onAddToQueueClick = { patient ->
                            selectedPatientForDirectQueue = patient
                            showAddToQueueDialog = true
                        }
                    )
                }

                // 5. Appointments Screen
                composable(Screen.Appointments.route) {
                    AppointmentsScreen(
                        appointments = appointmentsList,
                        isRefreshing = isRefreshing,
                        onRefresh = { refreshAllData() },
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
                        }
                    )
                }

                // 6. Billing Screen
                composable(Screen.Billing.route) {
                    BillingScreen(
                        invoices = invoicesList,
                        isRefreshing = isRefreshing,
                        onRefresh = { refreshAllData() },
                        onCollectPaymentClick = { invoice ->
                            // Same block as the queue's Collect Payment — see its comment.
                            val configuredUpiId = currentUser?.clinic?.upiId?.ifBlank { null } ?: currentUser?.clinic?.upiVpa?.ifBlank { null }
                            if (configuredUpiId == null) {
                                Toast.makeText(context, "This clinic hasn't set up a UPI ID yet. Ask your Clinic Admin to add one in Clinic Settings on the web portal.", Toast.LENGTH_LONG).show()
                            } else {
                                upiModalData = UpiPaymentModalData(
                                    payeeVpa = configuredUpiId,
                                    payeeName = currentUser?.clinic?.name ?: "MedRay AI Clinic",
                                    // Not invoice.total — a partially-paid invoice must only
                                    // collect what's still owed, not re-charge the full amount.
                                    amount = invoice.balanceDue,
                                    invoiceNumber = invoice.invoiceNumber,
                                    invoiceId = invoice.id
                                )
                            }
                        }
                    )
                }

                // 7. Self Check-Ins Screen
                composable(Screen.SelfCheckIns.route) {
                    SelfCheckInsScreen(
                        checkIns = selfCheckInsList,
                        isRefreshing = isRefreshing,
                        onRefresh = { refreshAllData() },
                        onAssignClick = { checkIn ->
                            showWalkInDialog = true
                        }
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
                // Immediate UI update in local state
                queueEntries = queueEntries.map { q ->
                    if (q.id == entry.id) {
                        q.copy(
                            vitalsBp = updatedVitals.vitalsBp,
                            vitalsTemperatureF = updatedVitals.vitalsTemperatureF,
                            vitalsPulseBpm = updatedVitals.vitalsPulseBpm,
                            vitalsRespRate = updatedVitals.vitalsRespRate,
                            vitalsSpo2 = updatedVitals.vitalsSpo2,
                            vitalsWeightKg = updatedVitals.vitalsWeightKg,
                            vitalsHeightCm = updatedVitals.vitalsHeightCm
                        )
                    } else q
                }
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
            existingPatients = patientsList,
            onDismiss = { showWalkInDialog = false },
            onRegister = { patientName, phone, doctorId, complaint, age, gender ->
                coroutineScope.launch {
                    val formattedPhone = if (phone.startsWith("+91")) phone else "+91$phone"
                    val pRes = patientRepo.registerPatient(
                        RegisterPatientRequest(
                            fullName = patientName,
                            phone = formattedPhone,
                            age = age,
                            gender = gender
                        )
                    )
                    var targetPatient = pRes.getOrNull()
                    if (targetPatient == null) {
                        val searchRes = patientRepo.searchPatients(phone)
                        targetPatient = searchRes.getOrNull()?.firstOrNull()
                    }
                    if (targetPatient != null) {
                        queueRepo.registerQueueEntry(
                            patientId = targetPatient.id,
                            doctorId = doctorId,
                            chiefComplaint = complaint
                        )
                        showWalkInDialog = false
                        refreshAllData()
                        Toast.makeText(context, "Walk-In Registered! Token issued.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to register: ${pRes.exceptionOrNull()?.message ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onAddExisting = { patient, doctorId, complaint ->
                coroutineScope.launch {
                    val res = queueRepo.registerQueueEntry(
                        patientId = patient.id,
                        doctorId = doctorId,
                        chiefComplaint = complaint
                    )
                    showWalkInDialog = false
                    if (res.isSuccess) {
                        refreshAllData()
                        Toast.makeText(context, "${patient.fullName} added to Dr.'s queue!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to add: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showAddToQueueDialog && selectedPatientForDirectQueue != null) {
        val patient = selectedPatientForDirectQueue!!
        AddToQueueForPatientDialog(
            patient = patient,
            doctors = doctors,
            onDismiss = {
                showAddToQueueDialog = false
                selectedPatientForDirectQueue = null
            },
            onAddToQueue = { doctorId, complaint, vitals ->
                coroutineScope.launch {
                    val res = queueRepo.registerQueueEntry(
                        patientId = patient.id,
                        doctorId = doctorId,
                        chiefComplaint = complaint,
                        vitals = vitals
                    )
                    showAddToQueueDialog = false
                    selectedPatientForDirectQueue = null
                    if (res.isSuccess) {
                        refreshAllData()
                        Toast.makeText(context, "${patient.fullName} added to Dr.'s queue! Token issued.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to add: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
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
                    val invoiceAmount = data.amount
                    // Existing invoice (Billing screen's Collect Payment) — record the
                    // payment against it directly and only report success if the server
                    // actually confirms it, instead of always showing the success toast.
                    if (data.invoiceId != null) {
                        val paymentRes = billingRepo.recordPayment(
                            invoiceId = data.invoiceId,
                            amount = invoiceAmount,
                            paymentMethod = PaymentMethod.UPI,
                            transactionRef = "UPI-${data.invoiceNumber}"
                        )
                        upiModalData = null
                        if (paymentRes.isSuccess) {
                            refreshAllData()
                            Toast.makeText(context, "Payment of ₹${invoiceAmount.toInt()} recorded & added to Billing Ledger!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Payment collected but failed to save to the ledger — please record it manually or retry.", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }
                    // 1. Create formal invoice in billing ledger if patientId exists
                    if (data.patientId != null) {
                        val createRes = billingRepo.createInvoice(
                            patientId = data.patientId,
                            discountAmount = 0.0,
                            lineItems = listOf(
                                CreateInvoiceLineItemInput(
                                    description = "OPD Consultation Fee (Dr. ${data.doctorName ?: "Doctor"})",
                                    quantity = 1,
                                    unitPrice = invoiceAmount,
                                    amount = invoiceAmount
                                )
                            )
                        )
                        if (createRes.isSuccess) {
                            val inv = createRes.getOrNull()!!
                            billingRepo.recordPayment(
                                invoiceId = inv.id,
                                amount = invoiceAmount,
                                paymentMethod = PaymentMethod.UPI,
                                transactionRef = "UPI-OPD-${data.invoiceNumber}"
                            )
                        }
                    }
                    // 2. Advance Queue Status to WAITING
                    if (data.queueEntryId != null) {
                        queueRepo.updateStatus(data.queueEntryId, QueueStatus.WAITING)
                    }
                    upiModalData = null
                    refreshAllData()
                    Toast.makeText(context, "Payment of ₹${invoiceAmount.toInt()} recorded & added to Billing Ledger!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    rxTargetEntry?.let { entry ->
        PrescriptionViewerDialog(
            entry = entry,
            visit = currentVisitForRx,
            onDismiss = {
                rxTargetEntry = null
                currentVisitForRx = null
            },
            onShareWhatsApp = {
                val phone = entry.patient?.phone ?: ""
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/91$phone?text=Hello%20${entry.patient?.fullName},%20your%20medical%20prescription%20for%20Token%20${entry.opdNumber}%20is%20ready%20at%20${currentUser?.clinic?.name ?: "MedRay AI Clinic"}."))
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}
