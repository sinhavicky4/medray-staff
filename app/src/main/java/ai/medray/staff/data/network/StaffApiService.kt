package ai.medray.staff.data.network

import ai.medray.staff.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

data class OtpRequestBody(val phone: String)
data class OtpRequestResponseDto(val requestId: String, val expiresInSeconds: Int)

data class OtpVerifyBody(
    val requestId: String,
    val code: String,
    val deviceFingerprint: String = "staff-android-device",
    val deviceName: String = "Android Smartphone",
    val client: String = "mobile"
)

data class GoogleSignInBody(
    val idToken: String,
    val deviceFingerprint: String = "staff-android-device",
    val deviceName: String = "Android Smartphone"
)

data class PasswordLoginBody(
    val email: String,
    val password: String
)

// Mirrors api/src/routes/publicClinicSignup.ts's signupSchema exactly — the
// admin sets their own password here (no generated temp password, unlike
// staff added later via CreateStaffRequest below), since there's no admin
// on the other end yet to hand one to. Unauthenticated: no session/CSRF
// needed, same as PlatformConfigResponse's GET config below.
data class ClinicSignupBody(
    val clinicName: String,
    val clinicAddress: String? = null,
    val clinicPhone: String? = null,
    val adminFullName: String,
    val adminEmail: String,
    val adminPhone: String,
    val password: String
)

data class ClinicSignupResponse(val ok: Boolean = false, val message: String? = null)

// Mirrors api/src/routes/users.ts's userCreateSchema — roles are restricted
// server-side to RECEPTIONIST/NURSE/DOCTOR/GENERAL (a Clinic Admin can't
// mint a peer CLINIC_ADMIN this way, that's POST /api/clinics/:id/admin,
// Super-Admin-only and out of scope for this app).
data class CreateStaffRequest(
    val email: String,
    val fullName: String,
    val phone: String,
    val roles: List<UserRole>,
    val departmentId: String? = null
)

// The server always returns a fresh tempPassword on create/resend-invite —
// the admin relays it to the new hire (email delivery is best-effort and
// may silently no-op if SMTP isn't configured, same as every other admin-
// provisioned account in this codebase), so the UI must show it, not just
// assume the email arrived.
data class CreateStaffResponse(
    val id: String,
    val email: String,
    val phone: String? = null,
    val fullName: String,
    val roles: List<UserRole> = emptyList(),
    val clinicId: String? = null,
    val tempPassword: String? = null
)

data class UpdateStaffRequest(
    val fullName: String? = null,
    val roles: List<UserRole>? = null,
    val email: String? = null,
    val phone: String? = null
)

data class ResendInviteResponse(val id: String, val tempPassword: String? = null)
data class DeactivateStaffResponse(val id: String, val deletedAt: String? = null)

data class RegisterQueueRequest(
    val id: String? = null,
    val patientId: String,
    val doctorId: String,
    val chiefComplaint: String,
    val visitType: String = "FIRST_VISIT",
    val scheduledAt: String? = null,
    val vitalsBp: String? = null,
    val vitalsTemperatureF: Double? = null,
    val vitalsPulseBpm: Int? = null,
    val vitalsRespRate: Int? = null,
    val vitalsSpo2: Int? = null,
    val vitalsWeightKg: Double? = null,
    val vitalsHeightCm: Double? = null
)

data class UpdateQueueStatusRequest(
    val status: QueueStatus,
    val cancelReason: String? = null
)

data class UpdateVitalsRequest(
    val vitalsBp: String? = null,
    val vitalsTemperatureF: Double? = null,
    val vitalsPulseBpm: Int? = null,
    val vitalsRespRate: Int? = null,
    val vitalsSpo2: Int? = null,
    val vitalsWeightKg: Double? = null,
    val vitalsHeightCm: Double? = null
)

data class RegisterPatientRequest(
    val fullName: String,
    val phone: String?,
    val email: String? = null,
    val dob: String? = null,
    val age: Int? = null,
    val gender: String = "MALE",
    val bloodGroup: String? = null,
    val address: String? = null,
    val emergencyContact: String? = null
)

data class BookAppointmentRequest(
    val patientId: String,
    val doctorId: String,
    val scheduledAt: String,
    val durationMinutes: Int = 15,
    val chiefComplaint: String,
    val visitType: String = "FIRST_VISIT"
)

data class RescheduleAppointmentRequest(val scheduledAt: String)
data class CancelAppointmentRequest(val cancelReason: String)

data class AssignSelfCheckInRequest(
    val doctorId: String,
    val visitType: String = "FIRST_VISIT",
    val vitalsBp: String? = null,
    val vitalsTemperatureF: Double? = null,
    val vitalsPulseBpm: Int? = null,
    val vitalsRespRate: Int? = null,
    val vitalsSpo2: Int? = null,
    val vitalsWeightKg: Double? = null,
    val vitalsHeightCm: Double? = null
)

data class CreateInvoiceLineItemInput(
    val description: String,
    val quantity: Int = 1,
    val unitPrice: Double,
    val amount: Double
)

data class CreateInvoiceRequest(
    val patientId: String,
    val visitId: String? = null,
    val discountAmount: Double = 0.0,
    val lineItems: List<CreateInvoiceLineItemInput>
)

data class RecordPaymentRequest(
    val amount: Double,
    val method: PaymentMethod,
    val note: String? = null
)

data class ShareInvoiceRequest(
    val channel: String
)

data class ShareInvoiceResponse(
    val success: Boolean,
    val messageId: String? = null
)

// Pre-visit fee collection — before a doctor has opened this patient's
// chart, so no Invoice exists yet to record an ordinary payment against.
// See AdvancePayment (api/prisma/schema.prisma) and POST
// /api/queue/:id/advance-payment.
data class CollectAdvancePaymentRequest(
    val amount: Double,
    val method: PaymentMethod,
    val note: String? = null
)

// --- IPD (Inpatient Department) — Phase 3 staff app ---
// Mirrors api/src/routes/ipd*.ts one-for-one. Field names mirror the Prisma
// model fields, same convention every other model in this file already
// follows against its own backend counterpart.

enum class AdmissionStatus {
    ADMISSION_REQUESTED, ADMISSION_APPROVED, BED_RESERVED, ADMITTED, INPATIENT,
    DISCHARGE_INITIATED, DISCHARGED, CANCELLED, TRANSFERRED_OUT, LAMA, ABSCONDED, DECEASED
}

enum class BedAssignmentStatus { RESERVED, ACTIVE, RELEASED, CANCELLED }
enum class DoctorAssignmentRole { ADMITTING, ATTENDING, CONSULTING }
enum class IpdOrderStatus { ORDERED, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED }
enum class MedicationAdministrationStatus { SCHEDULED, DUE, ADMINISTERED, HELD, REFUSED, MISSED, CANCELLED }
enum class InvestigationOrderStatus { ORDERED, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED }
enum class InvestigationResultType { STRUCTURED, PDF, IMAGE_DOCUMENT }

data class WardSummary(val id: String, val name: String)
data class RoomSummary(val id: String, val name: String, val ward: WardSummary)
data class BedSummary(val id: String, val label: String, val room: RoomSummary)

data class BedAssignmentSummary(
    val id: String,
    val bedId: String,
    val bed: BedSummary? = null,
    val status: BedAssignmentStatus
)

data class DoctorAssignmentSummary(
    val id: String,
    val doctorId: String,
    val doctor: DoctorSummary? = null,
    val role: DoctorAssignmentRole,
    val isActive: Boolean
)

// GET /api/ipd/admissions and GET /api/ipd/admissions/:id share this shape —
// the list endpoint omits some fields the detail endpoint includes (e.g.
// insurance/referral detail), all left nullable here rather than split into
// two DTOs, same as this file's existing single-DTO-for-list-and-detail
// convention (compare QueueEntry, used for both GET /queue and its writes).
data class IpdAdmission(
    val id: String,
    val clinicId: String,
    val admissionNumber: String,
    val patientId: String,
    val patient: Patient? = null,
    val status: AdmissionStatus,
    val admissionDateTime: String,
    val admittingDoctorId: String,
    val attendingDoctorId: String,
    val attendingDoctor: DoctorSummary? = null,
    val reasonForAdmission: String,
    val provisionalDiagnosis: String,
    // Spec §29's doctor-set vitals frequency (null = no order set yet;
    // clients fall back to their own default heuristic — see
    // IpdTaskListDerivation's own doc comment).
    val vitalsFrequencyHours: Int? = null,
    val dischargeInitiatedAt: String? = null,
    val dischargedAt: String? = null,
    val createdAt: String = "",
    // Only ever the ACTIVE assignment(s) — server-side filtered, "current
    // bed"/"current doctor," not history.
    val bedAssignments: List<BedAssignmentSummary> = emptyList(),
    val doctorAssignments: List<DoctorAssignmentSummary> = emptyList()
) {
    val currentBed: BedSummary? get() = bedAssignments.firstOrNull { it.status == BedAssignmentStatus.ACTIVE }?.bed
}

data class IpdVitalsReading(
    val id: String? = null,
    val admissionId: String,
    val recordedAt: String? = null,
    val temperatureF: Double? = null,
    val bloodPressure: String? = null,
    val pulseBpm: Int? = null,
    val respRatePerMin: Int? = null,
    val spo2Percent: Int? = null,
    val bloodGlucose: Double? = null,
    val weightKg: Double? = null,
    val painScore: Int? = null,
    val intakeMl: Double? = null,
    val outputMl: Double? = null,
    val recordedBy: DoctorSummary? = null
)

data class NursingNote(
    val id: String? = null,
    val admissionId: String,
    val note: String,
    val createdAt: String? = null,
    val author: DoctorSummary? = null
)

// Doctor-authored, signed clinical note (Phase 4) — distinct from
// NursingNote (DRAFT/SIGNED/AMENDED amend chain vs. plain append-only).
// Read-only here: nurses never author these, only read them.
data class DoctorProgressNote(
    val id: String? = null,
    val admissionId: String,
    val status: String,
    val versionNumber: Int,
    val note: String,
    val createdAt: String? = null,
    val author: DoctorSummary? = null
)

data class MedicationOrder(
    val id: String,
    val admissionId: String,
    val medicationName: String,
    val dose: String,
    val route: String,
    val frequency: String,
    val startAt: String,
    val endAt: String? = null,
    val instructions: String? = null,
    val administrations: List<MedicationAdministration> = emptyList()
)

data class MedicationAdministration(
    val id: String? = null,
    val medicationOrderId: String,
    val admissionId: String? = null,
    val scheduledAt: String,
    val actualAt: String? = null,
    val dose: String,
    val route: String,
    val status: MedicationAdministrationStatus = MedicationAdministrationStatus.SCHEDULED,
    val administeredBy: DoctorSummary? = null,
    val reason: String? = null,
    val notes: String? = null
)

data class UpdateMedicationAdministrationStatusRequest(
    val status: MedicationAdministrationStatus,
    val actualAt: String? = null,
    val reason: String? = null,
    val notes: String? = null
)

data class InvestigationOrder(
    val id: String,
    val admissionId: String,
    val investigationType: String,
    val testName: String,
    val instructions: String? = null,
    val status: InvestigationOrderStatus,
    val resultType: InvestigationResultType? = null,
    val resultText: String? = null,
    val resultAt: String? = null,
    val reviewedAt: String? = null
)

data class UpdateInvestigationStatusRequest(val status: InvestigationOrderStatus)
data class AddInvestigationResultRequest(
    val resultType: InvestigationResultType,
    val resultText: String? = null,
    val documentId: String? = null
)

// Spec §17/§47 Phase 3's discharge checklist — seeded server-side (7 fixed
// items, one per IpdDischargeChecklistItemType) the moment discharge is
// initiated. `notes` is optional and only ever set by an explicit toggle
// call that includes it — the UI here doesn't collect it yet (V1 keeps to
// a plain checkbox per item), but the field is read/round-tripped since
// the backend already supports it.
enum class IpdDischargeChecklistItemType {
    VITALS_RECORDED, MEDICATIONS_RECONCILED, BELONGINGS_RETURNED, PATIENT_EDUCATED,
    SUMMARY_HANDED, BED_CLEARED, FOLLOWUP_SCHEDULED
}

data class IpdDischargeChecklistItem(
    val id: String,
    val itemType: IpdDischargeChecklistItemType,
    val completed: Boolean,
    val completedAt: String? = null,
    val completedBy: ChecklistCompletedBy? = null,
    val notes: String? = null
)

data class ChecklistCompletedBy(val id: String, val fullName: String)

data class UpdateChecklistItemRequest(val completed: Boolean, val notes: String? = null)

data class IpdTimelineEvent(
    val id: String,
    val eventType: String,
    val occurredAt: String,
    val actorEmail: String? = null,
    val summary: String
)

// id is optional client-generated (offline-retry safety, see
// IpdRepository) — server upserts on it when present, generates its own
// when omitted, mirroring MedicationAdministration's existing pattern
// (api/src/routes/ipdNursing.ts, ipdMedications.ts).
data class CreateIpdVitalsRequest(
    val id: String? = null,
    val admissionId: String,
    val temperatureF: Double? = null,
    val bloodPressure: String? = null,
    val pulseBpm: Int? = null,
    val respRatePerMin: Int? = null,
    val spo2Percent: Int? = null,
    val bloodGlucose: Double? = null,
    val weightKg: Double? = null,
    val painScore: Int? = null,
    val intakeMl: Double? = null,
    val outputMl: Double? = null
)

data class CreateNursingNoteRequest(
    val id: String? = null,
    val admissionId: String,
    val note: String
)

data class CreateMedicationAdministrationRequest(
    val id: String? = null,
    val medicationOrderId: String,
    val scheduledAt: String,
    val dose: String,
    val route: String
)

interface StaffApiService {

    // Auth
    @POST("auth/otp/request")
    suspend fun requestOtp(@Body req: OtpRequestBody): Response<OtpRequestResponseDto>

    @POST("auth/otp/verify")
    suspend fun verifyOtp(@Body req: OtpVerifyBody): Response<User>

    @POST("auth/google/mobile")
    suspend fun signInWithGoogle(@Body req: GoogleSignInBody): Response<User>

    @POST("auth/login")
    suspend fun loginWithPassword(@Body req: PasswordLoginBody): Response<User>

    @GET("auth/me")
    suspend fun getMe(): Response<User>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    // Clinics & Doctors
    @GET("clinics")
    suspend fun listClinics(): Response<List<Clinic>>

    @GET("users")
    suspend fun listUsers(
        @Query("clinicId") clinicId: String? = null,
        @Query("includeDeleted") includeDeleted: Boolean? = null
    ): Response<List<User>>

    // Staff management — Clinic Admin adding/editing Receptionist/Nurse/
    // Doctor/General staff on their own clinic. See users.ts's canManageStaff.
    @POST("users")
    suspend fun createStaff(
        @Body req: CreateStaffRequest,
        @Query("clinicId") clinicId: String? = null
    ): Response<CreateStaffResponse>

    @PATCH("users/{id}")
    suspend fun updateStaff(
        @Path("id") id: String,
        @Body req: UpdateStaffRequest,
        @Query("clinicId") clinicId: String? = null
    ): Response<User>

    @POST("users/{id}/resend-invite")
    suspend fun resendStaffInvite(
        @Path("id") id: String,
        @Query("clinicId") clinicId: String? = null
    ): Response<ResendInviteResponse>

    @DELETE("users/{id}")
    suspend fun deactivateStaff(
        @Path("id") id: String,
        @Query("clinicId") clinicId: String? = null
    ): Response<DeactivateStaffResponse>

    @POST("users/{id}/restore")
    suspend fun restoreStaff(
        @Path("id") id: String,
        @Query("clinicId") clinicId: String? = null
    ): Response<DeactivateStaffResponse>

    // Queue
    @GET("queue")
    suspend fun getQueue(
        @Query("date") date: String? = null,
        @Query("clinicId") clinicId: String? = null
    ): Response<List<QueueEntry>>

    @POST("queue")
    suspend fun registerQueueEntry(
        @Body req: RegisterQueueRequest,
        @Query("clinicId") clinicId: String? = null
    ): Response<QueueEntry>

    @PATCH("queue/{id}/status")
    suspend fun updateQueueStatus(
        @Path("id") id: String,
        @Body req: UpdateQueueStatusRequest,
        @Query("clinicId") clinicId: String? = null
    ): Response<QueueEntry>

    @PATCH("queue/{id}/vitals")
    suspend fun updateQueueVitals(
        @Path("id") id: String,
        @Body req: UpdateVitalsRequest,
        @Query("clinicId") clinicId: String? = null
    ): Response<QueueEntry>

    @POST("queue/{id}/advance-payment")
    suspend fun collectAdvancePayment(
        @Path("id") id: String,
        @Body req: CollectAdvancePaymentRequest,
        @Query("clinicId") clinicId: String? = null
    ): Response<QueueEntry>

    // Patients
    @GET("patients")
    suspend fun searchPatients(
        @Query("search") query: String? = null,
        @Query("clinicId") clinicId: String? = null
    ): Response<List<Patient>>

    @GET("patients/{id}")
    suspend fun getPatient(@Path("id") id: String): Response<Patient>

    @POST("patients")
    suspend fun registerPatient(
        @Body req: RegisterPatientRequest,
        @Query("clinicId") clinicId: String? = null
    ): Response<Patient>

    @PATCH("patients/{id}")
    suspend fun updatePatient(
        @Path("id") id: String,
        @Body req: RegisterPatientRequest
    ): Response<Patient>

    @Multipart
    @POST("patients/{id}/photo")
    suspend fun uploadPatientPhoto(
        @Path("id") id: String,
        @Part photo: MultipartBody.Part
    ): Response<Patient>

    // Appointments
    @GET("appointments")
    suspend fun listAppointments(
        @Query("date") date: String? = null,
        @Query("doctorId") doctorId: String? = null,
        @Query("clinicId") clinicId: String? = null
    ): Response<List<Appointment>>

    @POST("appointments")
    suspend fun createAppointment(
        @Body req: BookAppointmentRequest,
        @Query("clinicId") clinicId: String? = null
    ): Response<Appointment>

    @POST("appointments/{id}/reschedule")
    suspend fun rescheduleAppointment(
        @Path("id") id: String,
        @Body req: RescheduleAppointmentRequest,
        @Query("clinicId") clinicId: String? = null
    ): Response<Appointment>

    @POST("appointments/{id}/check-in")
    suspend fun checkInAppointment(
        @Path("id") id: String,
        @Body vitals: UpdateVitalsRequest,
        @Query("clinicId") clinicId: String? = null
    ): Response<Appointment>

    @POST("appointments/{id}/no-show")
    suspend fun markAppointmentNoShow(
        @Path("id") id: String,
        @Query("clinicId") clinicId: String? = null
    ): Response<Appointment>

    @POST("appointments/{id}/cancel")
    suspend fun cancelAppointment(
        @Path("id") id: String,
        @Body req: CancelAppointmentRequest,
        @Query("clinicId") clinicId: String? = null
    ): Response<Appointment>

    // Self Check-Ins
    @GET("self-checkins")
    suspend fun listSelfCheckIns(
        @Query("status") status: String = "PENDING",
        @Query("clinicId") clinicId: String? = null
    ): Response<List<SelfCheckIn>>

    @POST("self-checkins/{id}/assign")
    suspend fun assignSelfCheckIn(
        @Path("id") id: String,
        @Body req: AssignSelfCheckInRequest,
        @Query("clinicId") clinicId: String? = null
    ): Response<QueueEntry>

    // Billing
    // NOTE: the real server mounts this router at /api/invoices, not
    // /api/billing — these three paths were fixed to match. createInvoice
    // below is left pointing at the (nonexistent) old path deliberately:
    // there is no POST / route for invoices at all server-side — a visit's
    // invoice is auto-created when a doctor completes the visit — so this
    // endpoint has nothing to call yet and is out of scope for this fix.
    @GET("invoices")
    suspend fun listInvoices(
        @Query("patientId") patientId: String? = null,
        @Query("status") status: String? = null,
        @Query("clinicId") clinicId: String? = null
    ): Response<List<Invoice>>

    @GET("invoices/{id}")
    suspend fun getInvoice(
        @Path("id") id: String,
        @Query("clinicId") clinicId: String? = null
    ): Response<Invoice>

    @POST("billing")
    suspend fun createInvoice(
        @Body req: CreateInvoiceRequest,
        @Query("clinicId") clinicId: String? = null
    ): Response<Invoice>

    @POST("invoices/{id}/payments")
    suspend fun recordPayment(
        @Path("id") id: String,
        @Body req: RecordPaymentRequest,
        @Query("clinicId") clinicId: String? = null
    ): Response<Invoice>

    @POST("invoices/{id}/share")
    suspend fun shareInvoice(
        @Path("id") id: String,
        @Body req: ShareInvoiceRequest,
        @Query("clinicId") clinicId: String? = null
    ): Response<ShareInvoiceResponse>

    // Visits & Prescriptions
    @GET("visits")
    suspend fun listVisits(
        @Query("patientId") patientId: String,
        @Query("limit") limit: Int = 20
    ): Response<List<Visit>>

    @GET("prescriptions/{id}")
    suspend fun getPrescription(@Path("id") id: String): Response<Prescription>

    // Documents
    @GET("patients/{patientId}/documents")
    suspend fun listDocuments(@Path("patientId") patientId: String): Response<List<PatientDocument>>

    @Multipart
    @POST("patients/{patientId}/documents")
    suspend fun uploadDocument(
        @Path("patientId") patientId: String,
        @Part file: MultipartBody.Part,
        @Part("kind") kind: RequestBody,
        @Part("visitId") visitId: RequestBody? = null,
        @Part("notes") notes: RequestBody? = null,
        // IPD investigation-result attach — ties the upload to one admission
        // (api/src/routes/patients.ts, fixed alongside the web IPD phase to
        // accept this the same way it already accepted visitId).
        @Part("admissionId") admissionId: RequestBody? = null
    ): Response<PatientDocument>

    @DELETE("patients/documents/{documentId}")
    suspend fun deleteDocument(@Path("documentId") documentId: String): Response<Unit>

    // IPD (Inpatient Department) — Phase 3 staff app. Nurse-scoped: see
    // §30's Web/Staff/Doctor role mapping (STAFF APP = Nurse) — no admission-
    // create/approve/bed-reserve, medication/investigation-order, discharge-
    // summary-authoring, or billing endpoints here, those are the web
    // portal's and doctor app's respective surfaces.
    @GET("ipd/admissions")
    suspend fun listIpdAdmissions(
        @Query("status") status: AdmissionStatus? = null,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("clinicId") clinicId: String? = null
    ): Response<List<IpdAdmission>>

    @GET("ipd/admissions/{id}")
    suspend fun getIpdAdmission(
        @Path("id") id: String,
        @Query("clinicId") clinicId: String? = null
    ): Response<IpdAdmission>

    @GET("ipd/nursing/vitals")
    suspend fun listIpdVitals(
        @Query("admissionId") admissionId: String,
        @Query("cursor") cursor: String? = null,
        @Query("clinicId") clinicId: String? = null
    ): Response<List<IpdVitalsReading>>

    @POST("ipd/nursing/vitals")
    suspend fun recordIpdVitals(
        @Body req: CreateIpdVitalsRequest,
        @Query("clinicId") clinicId: String? = null
    ): Response<IpdVitalsReading>

    @GET("ipd/nursing/notes")
    suspend fun listNursingNotes(
        @Query("admissionId") admissionId: String,
        @Query("cursor") cursor: String? = null,
        @Query("clinicId") clinicId: String? = null
    ): Response<List<NursingNote>>

    @POST("ipd/nursing/notes")
    suspend fun addNursingNote(
        @Body req: CreateNursingNoteRequest,
        @Query("clinicId") clinicId: String? = null
    ): Response<NursingNote>

    // Read-only — nurses never author DoctorProgressNotes, only display
    // them (see IpdPatientChartScreen.kt's Progress Notes tab).
    @GET("ipd/progress-notes")
    suspend fun listProgressNotes(
        @Query("admissionId") admissionId: String,
        @Query("clinicId") clinicId: String? = null
    ): Response<List<DoctorProgressNote>>

    @GET("ipd/medications")
    suspend fun listMedicationOrders(
        @Query("admissionId") admissionId: String,
        @Query("clinicId") clinicId: String? = null
    ): Response<List<MedicationOrder>>

    @GET("ipd/medication-administrations")
    suspend fun listMedicationAdministrations(
        @Query("admissionId") admissionId: String,
        @Query("clinicId") clinicId: String? = null
    ): Response<List<MedicationAdministration>>

    @POST("ipd/medication-administrations")
    suspend fun createMedicationAdministration(
        @Body req: CreateMedicationAdministrationRequest,
        @Query("clinicId") clinicId: String? = null
    ): Response<MedicationAdministration>

    @PATCH("ipd/medication-administrations/{id}/status")
    suspend fun updateMedicationAdministrationStatus(
        @Path("id") id: String,
        @Body req: UpdateMedicationAdministrationStatusRequest,
        @Query("clinicId") clinicId: String? = null
    ): Response<MedicationAdministration>

    @GET("ipd/investigations")
    suspend fun listInvestigations(
        @Query("admissionId") admissionId: String,
        @Query("clinicId") clinicId: String? = null
    ): Response<List<InvestigationOrder>>

    @POST("ipd/investigations/{id}/status")
    suspend fun updateInvestigationStatus(
        @Path("id") id: String,
        @Body req: UpdateInvestigationStatusRequest,
        @Query("clinicId") clinicId: String? = null
    ): Response<InvestigationOrder>

    @POST("ipd/investigations/{id}/result")
    suspend fun addInvestigationResult(
        @Path("id") id: String,
        @Body req: AddInvestigationResultRequest,
        @Query("clinicId") clinicId: String? = null
    ): Response<InvestigationOrder>

    @GET("ipd/discharge/{admissionId}/checklist")
    suspend fun listDischargeChecklist(
        @Path("admissionId") admissionId: String,
        @Query("clinicId") clinicId: String? = null
    ): Response<List<IpdDischargeChecklistItem>>

    @PATCH("ipd/discharge/checklist/{id}")
    suspend fun updateDischargeChecklistItem(
        @Path("id") id: String,
        @Body req: UpdateChecklistItemRequest,
        @Query("clinicId") clinicId: String? = null
    ): Response<IpdDischargeChecklistItem>

    @GET("ipd/timeline")
    suspend fun listIpdTimeline(
        @Query("admissionId") admissionId: String,
        @Query("cursor") cursor: String? = null,
        @Query("clinicId") clinicId: String? = null
    ): Response<List<IpdTimelineEvent>>

    // Chat Assistant — mirrors web's api.chat.* (web/src/lib/api.ts). Gated
    // server-side to SUPER_ADMIN/CLINIC_ADMIN/RECEPTIONIST/NURSE, same roles
    // this app already runs as.
    @GET("chat/history")
    suspend fun getChatHistory(@Query("clinicId") clinicId: String? = null): Response<ChatHistoryResponse>

    @POST("chat/message")
    suspend fun sendChatMessage(
        @Body req: SendChatMessageRequest,
        @Query("clinicId") clinicId: String? = null
    ): Response<ChatResponse>

    @DELETE("chat/history")
    suspend fun clearChatHistory(@Query("clinicId") clinicId: String? = null): Response<Unit>

    // Public, no auth — same one row (PlatformConfig) the web app reads so
    // both clients show the same configured assistant name instead of a
    // hardcoded default.
    @GET("config")
    suspend fun getPlatformConfig(): Response<PlatformConfigResponse>

    // Public, no auth — mirrors web's POST /api/public/clinic-signup exactly
    // (same rate-limited, unauthenticated router). Creates a pendingApproval
    // Clinic + its first CLINIC_ADMIN in one transaction; the admin can't
    // actually log in until they verify their email (link sent to
    // adminEmail, opened in the device's browser — no in-app deep link yet)
    // AND a Super Admin approves the clinic on the web portal.
    @POST("public/clinic-signup")
    suspend fun signUpClinic(@Body req: ClinicSignupBody): Response<ClinicSignupResponse>
}

data class SendChatMessageRequest(val messages: List<ChatMessage>)
data class PlatformConfigResponse(val appName: String = "MedRay AI", val chatAssistantName: String = "Swati")
