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
        @Part("documentKind") documentKind: RequestBody,
        @Part("notes") notes: RequestBody? = null
    ): Response<PatientDocument>

    @DELETE("patients/documents/{documentId}")
    suspend fun deleteDocument(@Path("documentId") documentId: String): Response<Unit>

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
