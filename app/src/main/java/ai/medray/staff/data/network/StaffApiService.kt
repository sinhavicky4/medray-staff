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
    val paymentMethod: PaymentMethod,
    val transactionRef: String? = null
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
    suspend fun listUsers(@Query("clinicId") clinicId: String? = null): Response<List<User>>

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
    @GET("billing")
    suspend fun listInvoices(
        @Query("patientId") patientId: String? = null,
        @Query("status") status: String? = null,
        @Query("clinicId") clinicId: String? = null
    ): Response<List<Invoice>>

    @GET("billing/{id}")
    suspend fun getInvoice(@Path("id") id: String): Response<Invoice>

    @POST("billing")
    suspend fun createInvoice(
        @Body req: CreateInvoiceRequest,
        @Query("clinicId") clinicId: String? = null
    ): Response<Invoice>

    @POST("billing/{id}/payments")
    suspend fun recordPayment(
        @Path("id") id: String,
        @Body req: RecordPaymentRequest,
        @Query("clinicId") clinicId: String? = null
    ): Response<Invoice>

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
}
