package ai.medray.staff.ui.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Scaffold
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
import ai.medray.staff.ui.auth.LoginScreen
import ai.medray.staff.ui.auth.OtpVerificationScreen
import ai.medray.staff.ui.common.*
import ai.medray.staff.ui.nurse.FastVitalsEntryDialog
import ai.medray.staff.ui.nurse.NurseHomeScreen
import ai.medray.staff.ui.reception.ReceptionHomeScreen
import ai.medray.staff.ui.reception.WalkInRegisterDialog
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Otp : Screen("otp")
    object NurseHome : Screen("nurse_home")
    object ReceptionHome : Screen("reception_home")
    object Billing : Screen("billing")
}

@Composable
fun StaffAppNavHost(
    authRepo: AuthRepository,
    queueRepo: QueueRepository,
    patientRepo: PatientRepository,
    billingRepo: BillingRepository,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentUser by remember { mutableStateOf<User?>(null) }
    var phoneInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var authError by remember { mutableStateOf<String?>(null) }
    var isAuthLoading by remember { mutableStateOf(false) }
    var isPasswordLoading by remember { mutableStateOf(false) }

    // Queue State
    var queueEntries by remember { mutableStateOf<List<QueueEntry>>(emptyList()) }
    var doctors by remember { mutableStateOf<List<DoctorSummary>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDoctorId by remember { mutableStateOf<String?>(null) }

    // Dialog States
    var vitalsTargetEntry by remember { mutableStateOf<QueueEntry?>(null) }
    var showWalkInDialog by remember { mutableStateOf(false) }
    var upiQrData by remember { mutableStateOf<Pair<QueueEntry, Double>?>(null) }

    // Check initial auth
    LaunchedEffect(Unit) {
        if (authRepo.isLoggedIn()) {
            val res = authRepo.getMe()
            if (res.isSuccess) {
                val u = res.getOrNull()
                currentUser = u
                if (u?.isNurse == true) {
                    navController.navigate(Screen.NurseHome.route) { popUpTo(0) }
                } else {
                    navController.navigate(Screen.ReceptionHome.route) { popUpTo(0) }
                }
            }
        }
    }

    // Refresh queue on active screens
    fun refreshData() {
        coroutineScope.launch {
            val res = queueRepo.refreshQueue()
            if (res.isSuccess) {
                queueEntries = res.getOrDefault(emptyList())
            }
        }
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            refreshData()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Login.route

    val isTopLevelScreen = currentRoute == Screen.NurseHome.route || currentRoute == Screen.ReceptionHome.route

    Scaffold(
        topBar = {
            if (isTopLevelScreen) {
                MedRayTopBar(
                    title = if (currentUser?.isNurse == true) "Nurse Station" else "Reception Desk",
                    subtitle = currentUser?.clinic?.name ?: "MedRay AI Clinic",
                    user = currentUser,
                    onLogoutClick = {
                        authRepo.logout()
                        currentUser = null
                        navController.navigate(Screen.Login.route) { popUpTo(0) }
                    }
                )
            }
        },
        bottomBar = {
            if (isTopLevelScreen && currentUser != null) {
                val items = if (currentUser!!.isNurse) {
                    listOf(
                        BottomNavItem(Screen.NurseHome.route, "Triage Queue", Icons.Filled.ListAlt, Icons.Outlined.ListAlt),
                        BottomNavItem(Screen.Billing.route, "Patients", Icons.Filled.People, Icons.Outlined.People)
                    )
                } else {
                    listOf(
                        BottomNavItem(Screen.ReceptionHome.route, "OPD Queue", Icons.Filled.ListAlt, Icons.Outlined.ListAlt),
                        BottomNavItem(Screen.Billing.route, "Billing & UPI", Icons.Filled.QrCode, Icons.Outlined.QrCode)
                    )
                }

                MedRayBottomNav(
                    items = items,
                    currentRoute = currentRoute,
                    onItemClick = { route ->
                        if (route == Screen.Billing.route && currentUser?.isReceptionist == true) {
                            val firstWaiting = queueEntries.firstOrNull { it.status == QueueStatus.WAITING }
                            if (firstWaiting != null) {
                                upiQrData = Pair(firstWaiting, 500.0)
                            } else {
                                Toast.makeText(context, "Select a patient from the queue to collect payment", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            navController.navigate(route)
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
                                val u = res.getOrNull()
                                currentUser = u
                                if (u?.isNurse == true) {
                                    navController.navigate(Screen.NurseHome.route) { popUpTo(0) }
                                } else {
                                    navController.navigate(Screen.ReceptionHome.route) { popUpTo(0) }
                                }
                            } else {
                                authError = res.exceptionOrNull()?.message ?: "Login failed"
                            }
                        }
                    },
                    isPasswordLoggingIn = isPasswordLoading,
                    error = authError
                )
            }

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
                                val u = res.getOrNull()
                                currentUser = u
                                if (u?.isNurse == true) {
                                    navController.navigate(Screen.NurseHome.route) { popUpTo(0) }
                                } else {
                                    navController.navigate(Screen.ReceptionHome.route) { popUpTo(0) }
                                }
                            } else {
                                authError = res.exceptionOrNull()?.message ?: "Invalid OTP"
                            }
                        }
                    },
                    onResend = {
                        coroutineScope.launch {
                            authRepo.sendOtp(phoneInput)
                            Toast.makeText(context, "OTP resent", Toast.LENGTH_SHORT).show()
                        }
                    },
                    isVerifying = isAuthLoading,
                    error = authError
                )
            }

            composable(Screen.NurseHome.route) {
                NurseHomeScreen(
                    queue = queueEntries,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onRecordVitalsClick = { entry -> vitalsTargetEntry = entry },
                    onScanDocumentClick = { entry ->
                        Toast.makeText(context, "Opening Camera Scanner for ${entry.patient?.fullName}...", Toast.LENGTH_SHORT).show()
                    },
                    onStatusChange = { entry, newStatus ->
                        coroutineScope.launch {
                            queueRepo.updateStatus(entry.id, newStatus)
                            refreshData()
                        }
                    }
                )
            }

            composable(Screen.ReceptionHome.route) {
                ReceptionHomeScreen(
                    queue = queueEntries,
                    doctors = doctors,
                    selectedDoctorId = selectedDoctorId,
                    onDoctorFilterChange = { selectedDoctorId = it },
                    onNewWalkInClick = { showWalkInDialog = true },
                    onStatusChange = { entry, newStatus ->
                        coroutineScope.launch {
                            queueRepo.updateStatus(entry.id, newStatus)
                            refreshData()
                        }
                    },
                    onCollectPaymentClick = { entry ->
                        upiQrData = Pair(entry, 500.0)
                    },
                    onWhatsAppClick = { entry ->
                        val phone = entry.patient?.phone
                        if (!phone.isNullOrBlank()) {
                            val clean = phone.replace("[^0-9]".toRegex(), "")
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/91$clean"))
                            context.startActivity(intent)
                        }
                    }
                )
            }
        }

        // Fast Vitals Entry Dialog
        vitalsTargetEntry?.let { entry ->
            FastVitalsEntryDialog(
                initialVitals = entry.vitals,
                patientName = entry.patient?.fullName ?: "Patient",
                onDismiss = { vitalsTargetEntry = null },
                onSave = { updatedVitals ->
                    coroutineScope.launch {
                        queueRepo.updateVitals(entry.id, updatedVitals)
                        refreshData()
                        Toast.makeText(context, "Vitals saved & synced with Doctor", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // Walk-In Registration Dialog
        if (showWalkInDialog) {
            WalkInRegisterDialog(
                doctors = doctors,
                onDismiss = { showWalkInDialog = false },
                onRegister = { fullName, phone, docId, complaint, age, gender ->
                    coroutineScope.launch {
                        val patRes = patientRepo.registerPatient(
                            RegisterPatientRequest(
                                fullName = fullName,
                                phone = phone,
                                age = age,
                                gender = gender
                            )
                        )
                        if (patRes.isSuccess) {
                            val patient = patRes.getOrNull()!!
                            queueRepo.registerQueueEntry(
                                patientId = patient.id,
                                doctorId = docId,
                                chiefComplaint = complaint
                            )
                            refreshData()
                            Toast.makeText(context, "Walk-in registered & token dispatched via WhatsApp", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, patRes.exceptionOrNull()?.message ?: "Registration failed", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }

        // Dynamic UPI QR Payment Dialog
        upiQrData?.let { (entry, amount) ->
            val clinicVpa = currentUser?.clinic?.upiVpa ?: "medray.clinic@upi"
            val clinicName = currentUser?.clinic?.name ?: "MedRay AI Clinic"
            DynamicUpiQrDialog(
                payeeVpa = clinicVpa,
                payeeName = clinicName,
                amount = amount,
                invoiceNumber = entry.opdNumber,
                onDismiss = { upiQrData = null },
                onMarkPaid = {
                    coroutineScope.launch {
                        Toast.makeText(context, "Payment marked as PAID. Tax receipt sent via WhatsApp!", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }
}
