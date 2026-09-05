package ai.medray.staff.ui.navigation

import ai.medray.staff.core.config.BrandConfig
import ai.medray.staff.ui.common.PrescriptionViewerDialog

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import ai.medray.staff.ui.auth.ClinicSignupScreen
import ai.medray.staff.ui.auth.ClinicSignupFormState
import ai.medray.staff.ui.admin.StaffManagementScreen
import ai.medray.staff.ui.admin.AddEditStaffDialog
import ai.medray.staff.ui.billing.BillingScreen
import ai.medray.staff.ui.common.*
import ai.medray.staff.ui.nurse.FastVitalsEntryDialog
import ai.medray.staff.ui.nurse.NurseHomeScreen
import ai.medray.staff.ui.patients.PatientsScreen
import ai.medray.staff.ui.patients.PatientDetailsDialog
import ai.medray.staff.ui.profile.ProfileScreen
import ai.medray.staff.ui.reception.AddToQueueForPatientDialog
import ai.medray.staff.ui.reception.ReceptionHomeScreen
import ai.medray.staff.ui.reception.WalkInRegisterDialog
import ai.medray.staff.ui.selfcheckins.SelfCheckInsScreen
import ai.medray.staff.ui.selfcheckins.AssignSelfCheckInDialog
import ai.medray.staff.domain.InvoicePdfActions
import ai.medray.staff.ui.chat.ChatScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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
            patientId = "pat-1",
            status = InvoiceStatus.PAID,
            total = 500.0,
            createdAt = "2026-08-25T09:30:00Z",
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
            patientId = "pat-2",
            status = InvoiceStatus.ISSUED,
            total = 500.0,
            createdAt = "2026-08-25T10:00:00Z",
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
            patientId = "pat-3",
            status = InvoiceStatus.PAID,
            total = 800.0,
            createdAt = "2026-08-25T10:15:00Z",
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
    object Chat : Screen("chat")
    object ClinicSignup : Screen("clinic_signup")
    object StaffManagement : Screen("staff_management")
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
    chatRepo: ChatRepository,
    clinicSignupRepo: ClinicSignupRepository,
    staffManagementRepo: StaffManagementRepository,
    placesRepository: PlacesAutocompleteRepository,
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

    // Clinic sign-up state (pre-auth, see Screen.ClinicSignup)
    var clinicSignupForm by remember { mutableStateOf(ClinicSignupFormState()) }
    var isClinicSignupSubmitting by remember { mutableStateOf(false) }
    var clinicSignupSubmitted by remember { mutableStateOf(false) }
    var clinicSignupConfirmationMessage by remember { mutableStateOf<String?>(null) }
    var clinicSignupError by remember { mutableStateOf<String?>(null) }

    // Staff management state (Screen.StaffManagement, Clinic Admin only)
    var staffList by remember { mutableStateOf<List<User>>(emptyList()) }
    var staffListLoading by remember { mutableStateOf(false) }
    var staffListError by remember { mutableStateOf<String?>(null) }
    var staffListLoaded by remember { mutableStateOf(false) }
    var staffDialogTarget by remember { mutableStateOf<User?>(null) }
    var showAddStaffDialog by remember { mutableStateOf(false) }
    var staffActionBusy by remember { mutableStateOf(false) }
    var staffActionError by remember { mutableStateOf<String?>(null) }
    var staffTempPasswordReveal by remember { mutableStateOf<Pair<String, String>?>(null) } // fullName to tempPassword
    var showDeactivatedStaff by remember { mutableStateOf(false) }

    fun reloadStaffList() {
        coroutineScope.launch {
            staffListLoading = true
            staffListError = null
            val res = staffManagementRepo.listStaff(includeDeleted = showDeactivatedStaff)
            staffListLoading = false
            staffListLoaded = true
            if (res.isSuccess) {
                staffList = res.getOrNull().orEmpty()
            } else {
                staffListError = res.exceptionOrNull()?.message ?: "Couldn't load staff list."
            }
        }
    }

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
    var upiModalBusy by remember { mutableStateOf(false) }
    var invoiceDetailTarget by remember { mutableStateOf<Invoice?>(null) }
    var invoiceShareBusyChannel by remember { mutableStateOf<String?>(null) }
    var assignSelfCheckInTarget by remember { mutableStateOf<SelfCheckIn?>(null) }
    var assignSelfCheckInBusy by remember { mutableStateOf(false) }
    var patientDetailTarget by remember { mutableStateOf<Patient?>(null) }
    var patientDetailVisits by remember { mutableStateOf<List<Visit>>(emptyList()) }
    var patientDetailVisitsLoading by remember { mutableStateOf(false) }

    // Chat Assistant state — hydrated from the server-persisted thread the
    // first time Screen.Chat is visited each session (same pattern as web's
    // useChatAssistant loading via GET /history on mount), not re-fetched
    // on every visit since the in-memory list already reflects it.
    var chatMessages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var chatHistoryLoaded by remember { mutableStateOf(false) }
    var chatAssistantName by remember { mutableStateOf("Swati") }
    var chatInput by remember { mutableStateOf("") }
    var chatSending by remember { mutableStateOf(false) }
    var chatPendingAction by remember { mutableStateOf<ChatPendingAction?>(null) }
    var chatConfirming by remember { mutableStateOf(false) }
    var chatError by remember { mutableStateOf<String?>(null) }

// Initial auth check handled seamlessly in SplashScreen

    var isRefreshing by remember { mutableStateOf(false) }

    // Shown after any queue-registration action (walk-in, existing-patient
    // add, self-check-in assign) — surfaces QueueEntry.tokenAlertFailed
    // instead of leaving a failed WhatsApp token alert silent to reception,
    // which is what happened in production when queue_token_alert wasn't
    // approved/synced on the WhatsApp BSP dashboard.
    fun showQueueRegistrationToast(patientName: String, tokenAlertFailed: Boolean) {
        if (tokenAlertFailed) {
            Toast.makeText(
                context,
                "$patientName added to queue — but the WhatsApp token alert failed to send. Please let them know their token number directly.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(context, "$patientName added to queue! Token issued.", Toast.LENGTH_SHORT).show()
        }
    }

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

    fun sendChatMessage(text: String) {
        if (text.isBlank() || chatSending) return
        val next = chatMessages + ChatMessage(role = "user", content = text, timestamp = java.time.Instant.now().toString())
        chatMessages = next
        chatInput = ""
        chatSending = true
        chatError = null
        coroutineScope.launch {
            val res = chatRepo.sendMessage(next)
            chatSending = false
            if (res.isSuccess) {
                val body = res.getOrNull()!!
                chatMessages = body.messages
                chatPendingAction = body.pendingAction
            } else {
                chatError = res.exceptionOrNull()?.message ?: "The assistant didn't respond — try again."
            }
        }
    }

    // Mirrors useChatAssistant.ts's confirmAction() switch — each branch
    // calls the same repository method the app's own dedicated flow for
    // that action already uses, so a confirmed chat action behaves
    // identically to doing it by hand elsewhere in the app.
    fun confirmChatAction() {
        val action = chatPendingAction ?: return
        val i = action.input
        chatConfirming = true
        chatError = null
        coroutineScope.launch {
            val result: Result<String> = try {
                when (action.name) {
                    "propose_create_patient" -> {
                        val res = patientRepo.registerPatient(
                            RegisterPatientRequest(
                                fullName = i["fullName"] as? String ?: "",
                                phone = i["phone"] as? String,
                                dob = i["dob"] as? String,
                                gender = i["gender"] as? String ?: "MALE",
                                email = i["email"] as? String,
                                address = i["address"] as? String
                            )
                        )
                        res.map { "✅ Registered ${it.fullName} (UHID ${it.uhid})." }
                    }
                    "propose_register_queue_entry" -> {
                        val res = queueRepo.registerQueueEntry(
                            patientId = i["patientId"] as? String ?: "",
                            doctorId = i["doctorId"] as? String ?: "",
                            chiefComplaint = i["chiefComplaint"] as? String ?: ""
                        )
                        res.map { "✅ Added ${it.patient?.fullName ?: "patient"} to the queue (OPD ${it.opdNumber})." }
                    }
                    "propose_record_vitals" -> {
                        val queueEntryId = i["queueEntryId"] as? String ?: ""
                        val vitals = Vitals(
                            vitalsBp = i["vitalsBp"] as? String,
                            vitalsTemperatureF = (i["vitalsTemperatureF"] as? Number)?.toDouble(),
                            vitalsPulseBpm = (i["vitalsPulseBpm"] as? Number)?.toInt(),
                            vitalsRespRate = (i["vitalsRespRate"] as? Number)?.toInt(),
                            vitalsSpo2 = (i["vitalsSpo2"] as? Number)?.toInt(),
                            vitalsWeightKg = (i["vitalsWeightKg"] as? Number)?.toDouble(),
                            vitalsHeightCm = (i["vitalsHeightCm"] as? Number)?.toDouble()
                        )
                        val patientName = queueEntries.find { it.id == queueEntryId }?.patient?.fullName ?: "patient"
                        queueRepo.updateVitals(queueEntryId, vitals).map { "✅ Vitals saved for $patientName." }
                    }
                    "propose_book_appointment" -> {
                        val res = appointmentRepo.createAppointment(
                            BookAppointmentRequest(
                                patientId = i["patientId"] as? String ?: "",
                                doctorId = i["doctorId"] as? String ?: "",
                                scheduledAt = i["scheduledAt"] as? String ?: "",
                                chiefComplaint = i["chiefComplaint"] as? String ?: ""
                            )
                        )
                        res.map { "✅ Appointment booked for ${it.patient.fullName}." }
                    }
                    else -> Result.failure(Exception("Unknown action"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
            chatConfirming = false
            if (result.isSuccess) {
                chatMessages = chatMessages + ChatMessage(role = "assistant", content = result.getOrNull())
                chatPendingAction = null
                refreshAllData()
            } else {
                chatError = result.exceptionOrNull()?.message ?: "Couldn't complete that action"
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Login.route
    val isAppAuthenticated = currentUser != null && currentRoute != Screen.Login.route && currentRoute != Screen.Otp.route && currentRoute != Screen.Splash.route && currentRoute != Screen.ClinicSignup.route

    val screenTitle = when (currentRoute) {
        Screen.Queue.route -> if (currentUser?.isNurse == true) "Triage Queue" else "OPD Queue"
        Screen.Patients.route -> "Patients Directory"
        Screen.Appointments.route -> "Appointments Schedule"
        Screen.Billing.route -> "Billing & Payments"
        Screen.SelfCheckIns.route -> "Self Check-In Kiosk"
        Screen.Profile.route -> "Staff Profile"
        Screen.Chat.route -> "Chat Assistant"
        Screen.StaffManagement.route -> "Staff Management"
        else -> BrandConfig.APP_NAME
    }
    val screenSubtitle = if (currentRoute == Screen.Chat.route) chatAssistantName else (currentUser?.clinic?.name ?: "Main Clinic")

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isAppAuthenticated,
        drawerContent = {
            if (isAppAuthenticated) {
                MedRayDrawerContent(
                    user = currentUser,
                    currentRoute = currentRoute,
                    pendingSelfCheckInCount = selfCheckInsList.size,
                    chatAssistantEnabled = currentUser?.clinic?.chatAssistantEnabled == true,
                    staffManagementEnabled = currentUser?.isClinicAdmin == true,
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
                        subtitle = screenSubtitle,
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
                    val bottomNavItems = if (currentUser?.isClinicAdmin == true) {
                        listOf(
                            BottomNavItem(Screen.Queue.route, "OPD Queue", Icons.AutoMirrored.Filled.ListAlt, Icons.AutoMirrored.Outlined.ListAlt),
                            BottomNavItem(Screen.Patients.route, "Patients", Icons.Filled.People, Icons.Outlined.People),
                            BottomNavItem(Screen.StaffManagement.route, "Staff", Icons.Filled.Badge, Icons.Outlined.Badge),
                            BottomNavItem(Screen.Profile.route, "Profile", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
                        )
                    } else if (currentUser?.isNurse == true) {
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
                        error = authError,
                        onSignUpClick = {
                            clinicSignupForm = ClinicSignupFormState()
                            clinicSignupSubmitted = false
                            clinicSignupError = null
                            navController.navigate(Screen.ClinicSignup.route)
                        }
                    )
                }

                // 1b. Clinic Sign-Up Screen (pre-auth)
                composable(Screen.ClinicSignup.route) {
                    ClinicSignupScreen(
                        formState = clinicSignupForm,
                        onFormChange = { clinicSignupForm = it },
                        onSubmit = {
                            isClinicSignupSubmitting = true
                            clinicSignupError = null
                            coroutineScope.launch {
                                val res = clinicSignupRepo.signUp(
                                    clinicName = clinicSignupForm.clinicName,
                                    clinicAddress = clinicSignupForm.clinicAddress,
                                    clinicPhone = clinicSignupForm.clinicPhone,
                                    adminFullName = clinicSignupForm.adminFullName,
                                    adminEmail = clinicSignupForm.adminEmail,
                                    adminPhone = clinicSignupForm.adminPhone,
                                    password = clinicSignupForm.password
                                )
                                isClinicSignupSubmitting = false
                                if (res.isSuccess) {
                                    clinicSignupConfirmationMessage = res.getOrNull()
                                    clinicSignupSubmitted = true
                                } else {
                                    clinicSignupError = res.exceptionOrNull()?.message ?: "Couldn't sign up your clinic. Please try again."
                                }
                            }
                        },
                        isSubmitting = isClinicSignupSubmitting,
                        submitted = clinicSignupSubmitted,
                        confirmationMessage = clinicSignupConfirmationMessage,
                        error = clinicSignupError,
                        onBackToLogin = {
                            navController.navigate(Screen.Login.route) { popUpTo(Screen.Login.route) { inclusive = true } }
                        },
                        placesRepository = placesRepository
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
                                        // Same per-doctor fee (same DEFAULT_CONSULTATION_FEE=600
                                        // fallback) the auto-generated invoice will later charge
                                        // at visit completion — Clinic.defaultConsultationFee
                                        // doesn't exist server-side at all.
                                        amount = entry.doctor?.consultationFee?.takeIf { it > 0 } ?: 600.0,
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
                            patientDetailTarget = patient
                            patientDetailVisits = emptyList()
                            patientDetailVisitsLoading = true
                            coroutineScope.launch {
                                val res = visitRepo.getPatientVisits(patient.id)
                                patientDetailVisits = res.getOrDefault(emptyList())
                                patientDetailVisitsLoading = false
                            }
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
                        },
                        onInvoiceClick = { invoice -> invoiceDetailTarget = invoice }
                    )
                }

                // 7. Self Check-Ins Screen
                composable(Screen.SelfCheckIns.route) {
                    SelfCheckInsScreen(
                        checkIns = selfCheckInsList,
                        isRefreshing = isRefreshing,
                        onRefresh = { refreshAllData() },
                        onAssignClick = { checkIn ->
                            assignSelfCheckInTarget = checkIn
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

                // 9. Chat Assistant Screen
                composable(Screen.Chat.route) {
                    LaunchedEffect(Unit) {
                        if (!chatHistoryLoaded) {
                            chatHistoryLoaded = true
                            val res = chatRepo.getHistory()
                            if (res.isSuccess) chatMessages = res.getOrDefault(emptyList())
                            val nameRes = chatRepo.getAssistantName()
                            if (nameRes.isSuccess) chatAssistantName = nameRes.getOrDefault("Swati")
                        }
                    }
                    ChatScreen(
                        messages = chatMessages,
                        input = chatInput,
                        onInputChange = { chatInput = it },
                        sending = chatSending,
                        pendingAction = chatPendingAction,
                        confirming = chatConfirming,
                        error = chatError,
                        onSend = { sendChatMessage(chatInput) },
                        onSuggestedPrompt = { sendChatMessage(it) },
                        onConfirmAction = { confirmChatAction() },
                        onDismissAction = { chatPendingAction = null }
                    )
                }

                // Staff Management (Clinic Admin only)
                composable(Screen.StaffManagement.route) {
                    LaunchedEffect(Unit) {
                        if (!staffListLoaded) reloadStaffList()
                    }
                    StaffManagementScreen(
                        staff = staffList,
                        isLoading = staffListLoading,
                        error = staffListError,
                        showDeactivated = showDeactivatedStaff,
                        onShowDeactivatedChange = {
                            showDeactivatedStaff = it
                            reloadStaffList()
                        },
                        onRefresh = { reloadStaffList() },
                        onAddStaffClick = {
                            staffActionError = null
                            showAddStaffDialog = true
                        },
                        onEditStaffClick = { staffMember ->
                            staffActionError = null
                            staffDialogTarget = staffMember
                        },
                        onResendInvite = { staffMember ->
                            coroutineScope.launch {
                                staffActionBusy = true
                                val res = staffManagementRepo.resendInvite(staffMember.id)
                                staffActionBusy = false
                                if (res.isSuccess) {
                                    val tempPassword = res.getOrNull()
                                    if (!tempPassword.isNullOrBlank()) {
                                        staffTempPasswordReveal = staffMember.fullName to tempPassword
                                    } else {
                                        Toast.makeText(context, "Invite resent to ${staffMember.fullName}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, res.exceptionOrNull()?.message ?: "Couldn't resend invite", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onDeactivateStaff = { staffMember ->
                            coroutineScope.launch {
                                staffActionBusy = true
                                val res = staffManagementRepo.deactivateStaff(staffMember.id)
                                staffActionBusy = false
                                if (res.isSuccess) reloadStaffList()
                                else Toast.makeText(context, res.exceptionOrNull()?.message ?: "Couldn't deactivate staff member", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onRestoreStaff = { staffMember ->
                            coroutineScope.launch {
                                staffActionBusy = true
                                val res = staffManagementRepo.restoreStaff(staffMember.id)
                                staffActionBusy = false
                                if (res.isSuccess) reloadStaffList()
                                else Toast.makeText(context, res.exceptionOrNull()?.message ?: "Couldn't restore staff member", Toast.LENGTH_SHORT).show()
                            }
                        },
                        actionBusy = staffActionBusy
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
                        val qRes = queueRepo.registerQueueEntry(
                            patientId = targetPatient.id,
                            doctorId = doctorId,
                            chiefComplaint = complaint
                        )
                        showWalkInDialog = false
                        refreshAllData()
                        showQueueRegistrationToast(targetPatient.fullName, qRes.getOrNull()?.tokenAlertFailed == true)
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
                        showQueueRegistrationToast(patient.fullName, res.getOrNull()?.tokenAlertFailed == true)
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
                        showQueueRegistrationToast(patient.fullName, res.getOrNull()?.tokenAlertFailed == true)
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
            busy = upiModalBusy,
            onDismiss = { upiModalData = null },
            onMarkPaid = {
                if (!upiModalBusy) {
                    upiModalBusy = true
                    coroutineScope.launch {
                        val invoiceAmount = data.amount
                        try {
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
                            } else if (data.queueEntryId != null) {
                                // Pre-visit fee collection (Queue screen's Collect Payment) —
                                // no Invoice exists yet at this point; this is folded into a
                                // real Payment automatically once the visit later completes
                                // (see completeVisitAndInvoice, api/src/routes/visits.ts).
                                // Previously this tried to create+pay an invoice with just a
                                // patientId, which never had a matching backend route and
                                // always failed silently behind an unconditional success toast.
                                val paymentRes = queueRepo.collectAdvancePayment(
                                    queueEntryId = data.queueEntryId,
                                    amount = invoiceAmount,
                                    method = PaymentMethod.UPI,
                                    note = "UPI-OPD-${data.invoiceNumber}"
                                )
                                upiModalData = null
                                if (paymentRes.isSuccess) {
                                    refreshAllData()
                                    Toast.makeText(context, "Payment of ₹${invoiceAmount.toInt()} recorded!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Payment collected but failed to save — please record it manually or retry.", Toast.LENGTH_LONG).show()
                                }
                            }
                        } finally {
                            upiModalBusy = false
                        }
                    }
                }
            }
        )
    }

    invoiceDetailTarget?.let { invoice ->
        InvoiceDetailDialog(
            invoice = invoice,
            busyChannel = invoiceShareBusyChannel,
            onDismiss = { invoiceDetailTarget = null },
            onShareWhatsApp = {
                if (invoiceShareBusyChannel == null) {
                    invoiceShareBusyChannel = "whatsapp"
                    coroutineScope.launch {
                        val res = billingRepo.shareInvoice(invoice.id, "whatsapp")
                        invoiceShareBusyChannel = null
                        if (res.isSuccess) {
                            Toast.makeText(context, "Invoice sent to patient's WhatsApp!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, res.exceptionOrNull()?.message ?: "Failed to send WhatsApp invoice", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            onShareEmail = {
                if (invoiceShareBusyChannel == null) {
                    invoiceShareBusyChannel = "email"
                    coroutineScope.launch {
                        val res = billingRepo.shareInvoice(invoice.id, "email")
                        invoiceShareBusyChannel = null
                        if (res.isSuccess) {
                            Toast.makeText(context, "Invoice emailed to patient!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, res.exceptionOrNull()?.message ?: "Failed to send invoice email", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            onDownloadPdf = {
                coroutineScope.launch {
                    val clinicName = currentUser?.clinic?.name ?: "MedRay AI Clinic"
                    val savedFileName = withContext(Dispatchers.IO) {
                        InvoicePdfActions.downloadToDownloads(context, invoice, clinicName)
                    }
                    if (savedFileName != null) {
                        Toast.makeText(context, "Saved $savedFileName to Downloads", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Couldn't save the PDF — requires Android 10 or newer", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onPrint = {
                val clinicName = currentUser?.clinic?.name ?: "MedRay AI Clinic"
                InvoicePdfActions.print(context, invoice, clinicName)
            }
        )
    }

    assignSelfCheckInTarget?.let { checkIn ->
        AssignSelfCheckInDialog(
            checkIn = checkIn,
            doctors = doctors,
            busy = assignSelfCheckInBusy,
            onDismiss = { if (!assignSelfCheckInBusy) assignSelfCheckInTarget = null },
            onConfirm = { doctorId ->
                if (!assignSelfCheckInBusy) {
                    assignSelfCheckInBusy = true
                    coroutineScope.launch {
                        val res = selfCheckInRepo.assign(id = checkIn.id, doctorId = doctorId)
                        assignSelfCheckInBusy = false
                        assignSelfCheckInTarget = null
                        if (res.isSuccess) {
                            refreshAllData()
                            showQueueRegistrationToast(checkIn.patient?.fullName ?: "Patient", res.getOrNull()?.tokenAlertFailed == true)
                        } else {
                            Toast.makeText(context, res.exceptionOrNull()?.message ?: "Failed to assign doctor", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        )
    }

    patientDetailTarget?.let { patient ->
        PatientDetailsDialog(
            patient = patient,
            visits = patientDetailVisits,
            visitsLoading = patientDetailVisitsLoading,
            onDismiss = { patientDetailTarget = null },
            onAddToQueueClick = {
                patientDetailTarget = null
                selectedPatientForDirectQueue = patient
                showAddToQueueDialog = true
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

    // Add / edit staff (Clinic Admin, Staff Management screen)
    if (showAddStaffDialog || staffDialogTarget != null) {
        AddEditStaffDialog(
            existing = staffDialogTarget,
            isBusy = staffActionBusy,
            error = staffActionError,
            onDismiss = {
                showAddStaffDialog = false
                staffDialogTarget = null
                staffActionError = null
            },
            onSubmit = { fullName, email, phone, roles ->
                val editing = staffDialogTarget
                coroutineScope.launch {
                    staffActionBusy = true
                    staffActionError = null
                    val res = if (editing != null) {
                        staffManagementRepo.updateStaff(editing.id, fullName, roles, email, phone)
                    } else {
                        staffManagementRepo.createStaff(email, fullName, phone, roles)
                    }
                    staffActionBusy = false
                    if (res.isSuccess) {
                        showAddStaffDialog = false
                        staffDialogTarget = null
                        reloadStaffList()
                        if (editing == null) {
                            val tempPassword = (res.getOrNull() as? CreateStaffResponse)?.tempPassword
                            if (!tempPassword.isNullOrBlank()) {
                                staffTempPasswordReveal = fullName to tempPassword
                            }
                        }
                    } else {
                        staffActionError = res.exceptionOrNull()?.message ?: "Couldn't save staff member. Please try again."
                    }
                }
            }
        )
    }

    // One-time reveal of a generated temp password after create/resend-invite —
    // email delivery is best-effort, so the admin needs a fallback way to hand
    // the new staff member their first-login credential.
    staffTempPasswordReveal?.let { (fullName, tempPassword) ->
        AlertDialog(
            onDismissRequest = { staffTempPasswordReveal = null },
            title = { Text("Temporary Password", fontFamily = HeadingFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Share this temporary password with $fullName. They'll be asked to set a new one on first login.",
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        tempPassword,
                        fontFamily = HeadingFontFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedRayBluePrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEFF6FF), RoundedCornerShape(10.dp))
                            .padding(vertical = 12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { staffTempPasswordReveal = null }) {
                    Text("Done", fontFamily = HeadingFontFamily, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
