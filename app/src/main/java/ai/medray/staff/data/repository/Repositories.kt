package ai.medray.staff.data.repository

import android.content.Context
import ai.medray.staff.data.local.PatientEntity
import ai.medray.staff.data.local.QueueEntryEntity
import ai.medray.staff.data.local.StaffDatabase
import ai.medray.staff.data.model.*
import ai.medray.staff.data.network.*
import ai.medray.staff.data.local.IpdAdmissionEntity
import ai.medray.staff.data.outbox.CreateIpdVitalsCommandPayload
import ai.medray.staff.data.outbox.CreateMedicationAdministrationCommandPayload
import ai.medray.staff.data.outbox.CreateNursingNoteCommandPayload
import ai.medray.staff.data.outbox.OutboxManager
import ai.medray.staff.data.outbox.RegisterQueueCommandPayload
import ai.medray.staff.data.outbox.UpdateStatusCommandPayload
import ai.medray.staff.data.outbox.UpdateVitalsCommandPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.UUID

class AuthRepository(private val context: Context) {
    private val api = ApiClient.getService(context)
    private val cookieJar = ApiClient.getCookieJar(context)
    private var lastRequestId: String? = null

    fun isLoggedIn(): Boolean = cookieJar.hasSession()

    fun getActiveClinicId(): String? = cookieJar.getActiveClinicId()

    fun setActiveClinicId(clinicId: String) {
        cookieJar.setActiveClinicId(clinicId)
    }

    suspend fun sendOtp(phone: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanPhone = phone.trim().replace("[^0-9+]".toRegex(), "")
            val res = api.requestOtp(OtpRequestBody(phone = cleanPhone))
            if (res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                lastRequestId = body.requestId
                Result.success(body.requestId)
            } else {
                val err = res.errorBody()?.string() ?: "Failed to send OTP"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyOtp(phone: String, code: String): Result<User> = withContext(Dispatchers.IO) {
        val reqId = lastRequestId ?: return@withContext Result.failure(Exception("Please request OTP first"))
        try {
            val res = api.verifyOtp(
                OtpVerifyBody(
                    requestId = reqId,
                    code = code.trim(),
                    deviceFingerprint = "staff-${android.os.Build.MODEL}-${android.os.Build.ID}",
                    deviceName = android.os.Build.MODEL ?: "Staff Phone",
                    client = "mobile"
                )
            )
            if (res.isSuccessful && res.body() != null) {
                val user = res.body()!!
                val activeClinic = user.activeClinic ?: user.clinic
                if (activeClinic != null) {
                    cookieJar.setActiveClinicId(activeClinic.id)
                }
                Result.success(user)
            } else {
                val err = res.errorBody()?.string() ?: "Invalid OTP"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val res = api.signInWithGoogle(
                GoogleSignInBody(
                    idToken = idToken,
                    deviceFingerprint = "staff-${android.os.Build.MODEL}-${android.os.Build.ID}",
                    deviceName = android.os.Build.MODEL ?: "Staff Phone"
                )
            )
            if (res.isSuccessful && res.body() != null) {
                val user = res.body()!!
                val activeClinic = user.activeClinic ?: user.clinic
                if (activeClinic != null) {
                    cookieJar.setActiveClinicId(activeClinic.id)
                }
                Result.success(user)
            } else {
                val err = res.errorBody()?.string() ?: "Google sign-in failed"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithPassword(email: String, pass: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val res = api.loginWithPassword(PasswordLoginBody(email = email.trim(), password = pass))
            if (res.isSuccessful && res.body() != null) {
                val user = res.body()!!
                val activeClinic = user.activeClinic ?: user.clinic
                if (activeClinic != null) {
                    cookieJar.setActiveClinicId(activeClinic.id)
                }
                Result.success(user)
            } else {
                val err = res.errorBody()?.string() ?: "Invalid credentials"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMe(): Result<User> = withContext(Dispatchers.IO) {
        try {
            val res = api.getMe()
            if (res.isSuccessful && res.body() != null) {
                val user = res.body()!!
                val activeClinic = user.activeClinic ?: user.clinic
                if (activeClinic != null) {
                    cookieJar.setActiveClinicId(activeClinic.id)
                }
                Result.success(user)
            } else {
                Result.failure(Exception("Session expired"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        cookieJar.clear()
    }
}

// Unauthenticated — used only from the pre-login "Sign Up Your Clinic"
// screen, same shared ApiClient/OkHttpClient every other repository uses
// (no session cookie to send yet, which is fine: this endpoint doesn't need
// one). See StaffApiService.signUpClinic's own doc comment for the full
// verify-email + Super-Admin-approval flow this kicks off.
class ClinicSignupRepository(private val context: Context) {
    private val api = ApiClient.getService(context)

    suspend fun signUp(
        clinicName: String,
        clinicAddress: String?,
        clinicPhone: String?,
        adminFullName: String,
        adminEmail: String,
        adminPhone: String,
        password: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val res = api.signUpClinic(
                ClinicSignupBody(
                    clinicName = clinicName.trim(),
                    clinicAddress = clinicAddress?.trim()?.ifBlank { null },
                    clinicPhone = clinicPhone?.trim()?.ifBlank { null },
                    adminFullName = adminFullName.trim(),
                    adminEmail = adminEmail.trim(),
                    adminPhone = adminPhone.trim(),
                    password = password
                )
            )
            if (res.isSuccessful && res.body()?.ok == true) {
                Result.success(res.body()?.message ?: "Check your email to verify your account.")
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Couldn't sign up your clinic. Please try again."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Clinic Admin managing their own clinic's Receptionist/Nurse/Doctor/General
// staff — mirrors web's /staff page, same users.ts endpoints, same
// STAFF_ROLES restriction enforced server-side (a Clinic Admin can't mint a
// peer CLINIC_ADMIN through this — see CreateStaffRequest's doc comment).
class StaffManagementRepository(private val context: Context) {
    private val api = ApiClient.getService(context)
    private val cookieJar = ApiClient.getCookieJar(context)

    suspend fun listStaff(includeDeleted: Boolean = false): Result<List<User>> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val res = api.listUsers(clinicId = clinicId, includeDeleted = if (includeDeleted) true else null)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to load staff directory"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createStaff(
        email: String,
        fullName: String,
        phone: String,
        roles: List<UserRole>
    ): Result<CreateStaffResponse> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val req = CreateStaffRequest(
                email = email.trim(),
                fullName = fullName.trim(),
                phone = if (phone.trim().startsWith("+")) phone.trim() else "+91${phone.trim()}",
                roles = roles
            )
            val res = api.createStaff(req = req, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to add staff member"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStaff(id: String, fullName: String?, roles: List<UserRole>?, email: String?, phone: String?): Result<User> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val res = api.updateStaff(id = id, req = UpdateStaffRequest(fullName, roles, email, phone), clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to update staff member"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resendInvite(id: String): Result<String?> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val res = api.resendStaffInvite(id = id, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!.tempPassword)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to resend invite"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deactivateStaff(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val res = api.deactivateStaff(id = id, clinicId = clinicId)
            if (res.isSuccessful) Result.success(Unit) else Result.failure(Exception(res.errorBody()?.string() ?: "Failed to deactivate staff member"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreStaff(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val res = api.restoreStaff(id = id, clinicId = clinicId)
            if (res.isSuccessful) Result.success(Unit) else Result.failure(Exception(res.errorBody()?.string() ?: "Failed to restore staff member"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class QueueRepository(private val context: Context) {
    private val api = ApiClient.getService(context)
    private val db = StaffDatabase.getDatabase(context)
    private val outbox = OutboxManager(context)
    private val cookieJar = ApiClient.getCookieJar(context)

    fun getLocalQueue(clinicId: String): Flow<List<QueueEntry>> {
        return db.queueDao().getQueue(clinicId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun refreshQueue(date: String? = null): Result<List<QueueEntry>> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val res = api.getQueue(date = date, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                val entries = res.body()!!
                if (entries.isNotEmpty()) {
                    val entities = entries.map { QueueEntryEntity.fromDomain(it) }
                    db.queueDao().insertQueue(entities)
                    Result.success(entries)
                } else {
                    val local = if (clinicId != null) db.queueDao().getQueueEntriesSync(clinicId).map { it.toDomain() } else emptyList()
                    Result.success(if (local.isNotEmpty()) local else entries)
                }
            } else {
                val local = if (clinicId != null) db.queueDao().getQueueEntriesSync(clinicId).map { it.toDomain() } else emptyList()
                Result.success(local)
            }
        } catch (e: Exception) {
            val local = if (clinicId != null) db.queueDao().getQueueEntriesSync(clinicId).map { it.toDomain() } else emptyList()
            Result.success(local)
        }
    }

    suspend fun registerQueueEntry(
        patientId: String,
        doctorId: String,
        chiefComplaint: String,
        visitType: String = "FIRST_VISIT",
        vitals: Vitals? = null
    ): Result<QueueEntry> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId() ?: return@withContext Result.failure(Exception("No active clinic"))
        val clientGenId = UUID.randomUUID().toString()

        val req = RegisterQueueRequest(
            id = clientGenId,
            patientId = patientId,
            doctorId = doctorId,
            chiefComplaint = chiefComplaint,
            visitType = visitType,
            vitalsBp = vitals?.vitalsBp,
            vitalsTemperatureF = vitals?.vitalsTemperatureF,
            vitalsPulseBpm = vitals?.vitalsPulseBpm,
            vitalsRespRate = vitals?.vitalsRespRate,
            vitalsSpo2 = vitals?.vitalsSpo2,
            vitalsWeightKg = vitals?.vitalsWeightKg,
            vitalsHeightCm = vitals?.vitalsHeightCm
        )

        try {
            val res = api.registerQueueEntry(req = req, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                val created = res.body()!!
                db.queueDao().insertQueueEntry(QueueEntryEntity.fromDomain(created))
                Result.success(created)
            } else {
                outbox.enqueue(
                    commandType = "REGISTER_QUEUE",
                    payload = RegisterQueueCommandPayload(clinicId = clinicId, request = req),
                    clientGeneratedId = clientGenId
                )
                Result.failure(Exception("Queued offline: will sync when connected"))
            }
        } catch (e: Exception) {
            outbox.enqueue(
                commandType = "REGISTER_QUEUE",
                payload = RegisterQueueCommandPayload(clinicId = clinicId, request = req),
                clientGeneratedId = clientGenId
            )
            Result.failure(Exception("Saved offline: will sync automatically"))
        }
    }

    suspend fun updateVitals(
        queueEntryId: String,
        vitals: Vitals
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        val req = UpdateVitalsRequest(
            vitalsBp = vitals.vitalsBp,
            vitalsTemperatureF = vitals.vitalsTemperatureF,
            vitalsPulseBpm = vitals.vitalsPulseBpm,
            vitalsRespRate = vitals.vitalsRespRate,
            vitalsSpo2 = vitals.vitalsSpo2,
            vitalsWeightKg = vitals.vitalsWeightKg,
            vitalsHeightCm = vitals.vitalsHeightCm
        )

        val existing = db.queueDao().getQueueEntryById(queueEntryId)
        if (existing != null) {
            val updated = existing.copy(
                vitalsBp = req.vitalsBp ?: existing.vitalsBp,
                vitalsTemperatureF = req.vitalsTemperatureF ?: existing.vitalsTemperatureF,
                vitalsPulseBpm = req.vitalsPulseBpm ?: existing.vitalsPulseBpm,
                vitalsRespRate = req.vitalsRespRate ?: existing.vitalsRespRate,
                vitalsSpo2 = req.vitalsSpo2 ?: existing.vitalsSpo2,
                vitalsWeightKg = req.vitalsWeightKg ?: existing.vitalsWeightKg,
                vitalsHeightCm = req.vitalsHeightCm ?: existing.vitalsHeightCm
            )
            db.queueDao().insertQueueEntry(updated)
        }

        try {
            val res = api.updateQueueVitals(id = queueEntryId, req = req, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                db.queueDao().insertQueueEntry(QueueEntryEntity.fromDomain(res.body()!!))
                Result.success(Unit)
            } else {
                outbox.enqueue(
                    commandType = "UPDATE_VITALS",
                    payload = UpdateVitalsCommandPayload(queueEntryId = queueEntryId, clinicId = clinicId, vitals = req)
                )
                Result.success(Unit)
            }
        } catch (e: Exception) {
            outbox.enqueue(
                commandType = "UPDATE_VITALS",
                payload = UpdateVitalsCommandPayload(queueEntryId = queueEntryId, clinicId = clinicId, vitals = req)
            )
            Result.success(Unit)
        }
    }

    suspend fun updateStatus(
        queueEntryId: String,
        status: QueueStatus,
        cancelReason: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        val req = UpdateQueueStatusRequest(status = status, cancelReason = cancelReason)

        val existing = db.queueDao().getQueueEntryById(queueEntryId)
        if (existing != null) {
            db.queueDao().insertQueueEntry(existing.copy(status = status.name, cancelReason = cancelReason))
        }

        try {
            val res = api.updateQueueStatus(id = queueEntryId, req = req, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                db.queueDao().insertQueueEntry(QueueEntryEntity.fromDomain(res.body()!!))
                Result.success(Unit)
            } else {
                outbox.enqueue(
                    commandType = "UPDATE_STATUS",
                    payload = UpdateStatusCommandPayload(queueEntryId = queueEntryId, clinicId = clinicId, request = req)
                )
                Result.success(Unit)
            }
        } catch (e: Exception) {
            outbox.enqueue(
                commandType = "UPDATE_STATUS",
                payload = UpdateStatusCommandPayload(queueEntryId = queueEntryId, clinicId = clinicId, request = req)
            )
            Result.success(Unit)
        }
    }

    // Pre-visit fee collection — money changed hands, so unlike
    // updateStatus/updateVitals above this must NOT optimistically report
    // success and queue for later retry on failure. A real confirmed/failed
    // Result, matching BillingRepository's own online-only payment calls.
    suspend fun collectAdvancePayment(
        queueEntryId: String,
        amount: Double,
        method: PaymentMethod,
        note: String? = null
    ): Result<QueueEntry> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val req = CollectAdvancePaymentRequest(amount = amount, method = method, note = note)
            val res = api.collectAdvancePayment(id = queueEntryId, req = req, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                val entry = res.body()!!
                db.queueDao().insertQueueEntry(QueueEntryEntity.fromDomain(entry))
                Result.success(entry)
            } else {
                Result.failure(Exception("Failed to record payment"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class PatientRepository(private val context: Context) {
    private val api = ApiClient.getService(context)
    private val db = StaffDatabase.getDatabase(context)
    private val cookieJar = ApiClient.getCookieJar(context)

    suspend fun searchPatients(query: String): Result<List<Patient>> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val res = api.searchPatients(query = query, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                val list = res.body()!!
                db.patientDao().insertPatients(list.map { PatientEntity.fromDomain(it) })
                Result.success(list)
            } else {
                val local = db.patientDao().searchPatients(query).map { it.toDomain() }
                Result.success(local)
            }
        } catch (e: Exception) {
            val local = db.patientDao().searchPatients(query).map { it.toDomain() }
            Result.success(local)
        }
    }

    suspend fun registerPatient(req: RegisterPatientRequest): Result<Patient> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val dob = req.dob ?: if (req.age != null && req.age > 0) {
                "${2026 - req.age}-01-01T00:00:00.000Z"
            } else {
                "1995-01-01T00:00:00.000Z"
            }
            val sanitizedReq = req.copy(
                dob = dob,
                phone = if (req.phone?.startsWith("+91") == true) req.phone else "+91${req.phone?.trim() ?: ""}"
            )
            val res = api.registerPatient(req = sanitizedReq, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                val patient = res.body()!!
                db.patientDao().insertPatient(PatientEntity.fromDomain(patient))
                Result.success(patient)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to register patient"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadPhoto(patientId: String, photoFile: File): Result<Patient> = withContext(Dispatchers.IO) {
        try {
            val reqFile = photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("photo", photoFile.name, reqFile)
            val res = api.uploadPatientPhoto(patientId, body)
            if (res.isSuccessful && res.body() != null) {
                val patient = res.body()!!
                db.patientDao().insertPatient(PatientEntity.fromDomain(patient))
                Result.success(patient)
            } else {
                Result.failure(Exception("Photo upload failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listDocuments(patientId: String): Result<List<PatientDocument>> = withContext(Dispatchers.IO) {
        try {
            val res = api.listDocuments(patientId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("Failed to load documents"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadDocument(
        patientId: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
        kind: String = "REPORT",
        visitId: String? = null,
        notes: String? = null,
        admissionId: String? = null
    ): Result<PatientDocument> = withContext(Dispatchers.IO) {
        try {
            val reqFile = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", fileName, reqFile)
            val kindPart = kind.toRequestBody("text/plain".toMediaTypeOrNull())
            val visitIdPart = visitId?.toRequestBody("text/plain".toMediaTypeOrNull())
            val notesPart = notes?.toRequestBody("text/plain".toMediaTypeOrNull())
            val admissionIdPart = admissionId?.toRequestBody("text/plain".toMediaTypeOrNull())

            val res = api.uploadDocument(patientId, filePart, kindPart, visitIdPart, notesPart, admissionIdPart)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                val errMsg = res.errorBody()?.string() ?: "Document upload failed"
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDocument(documentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = api.deleteDocument(documentId)
            if (res.isSuccessful) {
                Result.success(Unit)
            } else {
                val errMsg = res.errorBody()?.string() ?: "Failed to delete document"
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class AppointmentRepository(private val context: Context) {
    private val api = ApiClient.getService(context)
    private val cookieJar = ApiClient.getCookieJar(context)

    suspend fun listAppointments(date: String? = null, doctorId: String? = null): Result<List<Appointment>> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val res = api.listAppointments(date = date, doctorId = doctorId, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to load appointments"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createAppointment(req: BookAppointmentRequest): Result<Appointment> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val res = api.createAppointment(req = req, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to book appointment"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkIn(appointmentId: String, vitals: Vitals?): Result<Appointment> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        val req = UpdateVitalsRequest(
            vitalsBp = vitals?.vitalsBp,
            vitalsTemperatureF = vitals?.vitalsTemperatureF,
            vitalsPulseBpm = vitals?.vitalsPulseBpm,
            vitalsRespRate = vitals?.vitalsRespRate,
            vitalsSpo2 = vitals?.vitalsSpo2,
            vitalsWeightKg = vitals?.vitalsWeightKg,
            vitalsHeightCm = vitals?.vitalsHeightCm
        )
        try {
            val res = api.checkInAppointment(id = appointmentId, vitals = req, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Check-in failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancel(appointmentId: String, reason: String): Result<Appointment> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val res = api.cancelAppointment(id = appointmentId, req = CancelAppointmentRequest(reason), clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("Cancellation failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class BillingRepository(private val context: Context) {
    private val api = ApiClient.getService(context)
    private val cookieJar = ApiClient.getCookieJar(context)

    suspend fun listInvoices(patientId: String? = null): Result<List<Invoice>> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val res = api.listInvoices(patientId = patientId, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch invoices"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createInvoice(
        patientId: String,
        discountAmount: Double = 0.0,
        lineItems: List<CreateInvoiceLineItemInput>
    ): Result<Invoice> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val req = CreateInvoiceRequest(
                patientId = patientId,
                discountAmount = discountAmount,
                lineItems = lineItems
            )
            val res = api.createInvoice(req = req, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to create invoice"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordPayment(
        invoiceId: String,
        amount: Double,
        paymentMethod: PaymentMethod,
        transactionRef: String? = null
    ): Result<Invoice> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val req = RecordPaymentRequest(
                amount = amount,
                method = paymentMethod,
                note = transactionRef
            )
            val res = api.recordPayment(id = invoiceId, req = req, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("Failed to record payment"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInvoice(invoiceId: String): Result<Invoice> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val res = api.getInvoice(id = invoiceId, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("Failed to load invoice"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun shareInvoice(invoiceId: String, channel: String): Result<Unit> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val res = api.shareInvoice(id = invoiceId, req = ShareInvoiceRequest(channel = channel), clinicId = clinicId)
            if (res.isSuccessful && res.body()?.success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to send invoice"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class DocumentRepository(private val context: Context) {
    private val api = ApiClient.getService(context)

    suspend fun listDocuments(patientId: String): Result<List<PatientDocument>> = withContext(Dispatchers.IO) {
        try {
            val res = api.listDocuments(patientId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("Failed to load documents"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadDocument(
        patientId: String,
        file: File,
        kind: DocumentKind,
        visitId: String? = null,
        notes: String? = null,
        admissionId: String? = null
    ): Result<PatientDocument> = withContext(Dispatchers.IO) {
        try {
            val reqFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, reqFile)
            val kindPart = kind.serverKind.toRequestBody("text/plain".toMediaTypeOrNull())
            val visitIdPart = visitId?.toRequestBody("text/plain".toMediaTypeOrNull())
            val notesPart = notes?.toRequestBody("text/plain".toMediaTypeOrNull())
            val admissionIdPart = admissionId?.toRequestBody("text/plain".toMediaTypeOrNull())

            val res = api.uploadDocument(patientId, filePart, kindPart, visitIdPart, notesPart, admissionIdPart)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                val errMsg = res.errorBody()?.string() ?: "Document upload failed"
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadDocument(
        patientId: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
        kind: String = "REPORT",
        visitId: String? = null,
        notes: String? = null,
        admissionId: String? = null
    ): Result<PatientDocument> = withContext(Dispatchers.IO) {
        try {
            val reqFile = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", fileName, reqFile)
            val kindPart = kind.toRequestBody("text/plain".toMediaTypeOrNull())
            val visitIdPart = visitId?.toRequestBody("text/plain".toMediaTypeOrNull())
            val notesPart = notes?.toRequestBody("text/plain".toMediaTypeOrNull())
            val admissionIdPart = admissionId?.toRequestBody("text/plain".toMediaTypeOrNull())

            val res = api.uploadDocument(patientId, filePart, kindPart, visitIdPart, notesPart, admissionIdPart)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                val errMsg = res.errorBody()?.string() ?: "Document upload failed"
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDocument(documentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = api.deleteDocument(documentId)
            if (res.isSuccessful) {
                Result.success(Unit)
            } else {
                val errMsg = res.errorBody()?.string() ?: "Failed to delete document"
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class DoctorRepository(private val context: Context) {
    private val api = ApiClient.getService(context)
    private val cookieJar = ApiClient.getCookieJar(context)

    suspend fun listDoctors(): Result<List<DoctorSummary>> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val res = api.listUsers(clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                val doctors = res.body()!!
                    .filter { it.roles.contains(UserRole.DOCTOR) || it.roles.contains(UserRole.CLINIC_ADMIN) }
                    .map {
                        DoctorSummary(
                            id = it.id,
                            fullName = it.fullName,
                            specialization = it.specialization
                        )
                    }
                Result.success(if (doctors.isNotEmpty()) doctors else defaultDoctors())
            } else {
                Result.success(defaultDoctors())
            }
        } catch (e: Exception) {
            Result.success(defaultDoctors())
        }
    }

    private fun defaultDoctors(): List<DoctorSummary> = listOf(
        DoctorSummary(id = "doc-1", fullName = "Dr. Rajesh Sharma", specialization = "General Physician"),
        DoctorSummary(id = "doc-2", fullName = "Dr. Ananya Roy", specialization = "Pediatrician")
    )
}

class SelfCheckInRepository(private val context: Context) {
    private val api = ApiClient.getService(context)
    private val cookieJar = ApiClient.getCookieJar(context)

    suspend fun listPending(): Result<List<SelfCheckIn>> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val res = api.listSelfCheckIns(status = "PENDING", clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch self check-ins"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun assign(id: String, doctorId: String, visitType: String = "FIRST_VISIT"): Result<QueueEntry> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val req = AssignSelfCheckInRequest(doctorId = doctorId, visitType = visitType)
            val res = api.assignSelfCheckIn(id = id, req = req, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("Failed to assign kiosk check-in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class VisitRepository(private val context: Context) {
    private val api = ApiClient.getService(context)

    suspend fun getPatientVisits(patientId: String): Result<List<Visit>> = withContext(Dispatchers.IO) {
        try {
            val res = api.listVisits(patientId = patientId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch visits"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPrescription(id: String): Result<Prescription> = withContext(Dispatchers.IO) {
        try {
            val res = api.getPrescription(id)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch prescription"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class ChatRepository(private val context: Context) {
    private val api = ApiClient.getService(context)
    private val cookieJar = ApiClient.getCookieJar(context)

    suspend fun getHistory(): Result<List<ChatMessage>> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val res = api.getChatHistory(clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!.messages)
            } else {
                Result.failure(Exception("Failed to load chat history"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(messages: List<ChatMessage>): Result<ChatResponse> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val res = api.sendChatMessage(SendChatMessageRequest(messages), clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "The assistant didn't respond — try again."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearHistory(): Result<Unit> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val res = api.clearChatHistory(clinicId = clinicId)
            if (res.isSuccessful) Result.success(Unit) else Result.failure(Exception("Failed to clear the conversation"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** The configured assistant display name (default "Swati") — same PlatformConfig row the web app's /chat page reads. */
    suspend fun getAssistantName(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val res = api.getPlatformConfig()
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!.chatAssistantName)
            } else {
                Result.failure(Exception("Failed to load config"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * IPD (Inpatient Department) — Phase 3 staff app, Nurse-scoped (spec §30:
 * STAFF APP = Nurse). See ipd/IpdWardScreens.kt / IpdPatientChartScreen.kt
 * for the screens this backs.
 *
 * Only the ward roster (`listAdmissions`) caches-and-falls-back, same as
 * QueueRepository.refreshQueue() — everything else (vitals/notes/
 * medication/investigation/timeline detail) is a live, uncached read: a
 * stale cached clinical reading shown as current is a worse failure mode
 * than a clear "couldn't load" error.
 */
class IpdRepository(private val context: Context) {
    private val api = ApiClient.getService(context)
    private val db = StaffDatabase.getDatabase(context)
    private val outbox = OutboxManager(context)
    private val cookieJar = ApiClient.getCookieJar(context)

    fun getLocalAdmissions(clinicId: String): Flow<List<IpdAdmission>> =
        db.ipdAdmissionDao().getAdmissions(clinicId).map { entities -> entities.map { it.toDomain() } }

    suspend fun refreshAdmissions(status: AdmissionStatus? = AdmissionStatus.INPATIENT): Result<List<IpdAdmission>> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            // limit is capped at 50 server-side (admissionQuerySchema,
            // api/src/routes/ipdAdmissions.ts) — a higher value 400s the
            // whole request, confirmed live against a real device.
            val res = api.listIpdAdmissions(status = status, limit = 50, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                val admissions = res.body()!!
                if (clinicId != null) db.ipdAdmissionDao().insertAdmissions(admissions.map { IpdAdmissionEntity.fromDomain(it) })
                Result.success(admissions)
            } else {
                val local = if (clinicId != null) db.ipdAdmissionDao().getAdmissionsSync(clinicId).map { it.toDomain() } else emptyList()
                Result.success(local)
            }
        } catch (e: Exception) {
            val local = if (clinicId != null) db.ipdAdmissionDao().getAdmissionsSync(clinicId).map { it.toDomain() } else emptyList()
            Result.success(local)
        }
    }

    suspend fun getAdmission(admissionId: String): Result<IpdAdmission> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val res = api.getIpdAdmission(id = admissionId, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to load this patient's chart"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listVitals(admissionId: String): Result<List<IpdVitalsReading>> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val res = api.listIpdVitals(admissionId = admissionId, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) Result.success(res.body()!!)
            else Result.failure(Exception("Failed to load vitals"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Outbox-eligible — see the backend touch in ipdNursing.ts (client-id
    // upsert) that makes this safe to retry.
    suspend fun recordVitals(req: CreateIpdVitalsRequest): Result<IpdVitalsReading> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        val withId = if (req.id != null) req else req.copy(id = UUID.randomUUID().toString())
        try {
            val res = api.recordIpdVitals(req = withId, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                outbox.enqueue("CREATE_IPD_VITALS", CreateIpdVitalsCommandPayload(clinicId, withId), withId.id!!)
                Result.failure(Exception("Queued offline: will sync when connected"))
            }
        } catch (e: Exception) {
            outbox.enqueue("CREATE_IPD_VITALS", CreateIpdVitalsCommandPayload(clinicId, withId), withId.id!!)
            Result.failure(Exception("Saved offline: will sync automatically"))
        }
    }

    suspend fun listNursingNotes(admissionId: String): Result<List<NursingNote>> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val res = api.listNursingNotes(admissionId = admissionId, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) Result.success(res.body()!!)
            else Result.failure(Exception("Failed to load nursing notes"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addNursingNote(req: CreateNursingNoteRequest): Result<NursingNote> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        val withId = if (req.id != null) req else req.copy(id = UUID.randomUUID().toString())
        try {
            val res = api.addNursingNote(req = withId, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                outbox.enqueue("CREATE_NURSING_NOTE", CreateNursingNoteCommandPayload(clinicId, withId), withId.id!!)
                Result.failure(Exception("Queued offline: will sync when connected"))
            }
        } catch (e: Exception) {
            outbox.enqueue("CREATE_NURSING_NOTE", CreateNursingNoteCommandPayload(clinicId, withId), withId.id!!)
            Result.failure(Exception("Saved offline: will sync automatically"))
        }
    }

    suspend fun listMedicationOrders(admissionId: String): Result<List<MedicationOrder>> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val res = api.listMedicationOrders(admissionId = admissionId, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) Result.success(res.body()!!)
            else Result.failure(Exception("Failed to load medication orders"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createMedicationAdministration(req: CreateMedicationAdministrationRequest): Result<MedicationAdministration> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        val withId = if (req.id != null) req else req.copy(id = UUID.randomUUID().toString())
        try {
            val res = api.createMedicationAdministration(req = withId, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                outbox.enqueue("CREATE_MEDICATION_ADMINISTRATION", CreateMedicationAdministrationCommandPayload(clinicId, withId), withId.id!!)
                Result.failure(Exception("Queued offline: will sync when connected"))
            }
        } catch (e: Exception) {
            outbox.enqueue("CREATE_MEDICATION_ADMINISTRATION", CreateMedicationAdministrationCommandPayload(clinicId, withId), withId.id!!)
            Result.failure(Exception("Saved offline: will sync automatically"))
        }
    }

    // Deliberately online-only, no Outbox — the state machine forbids a
    // same-state retry (SCHEDULED->[DUE,CANCELLED], DUE->[ADMINISTERED,
    // HELD,REFUSED,MISSED,CANCELLED], api/src/ipd/stateMachine.ts), so a
    // naive retry-on-ambiguous-failure isn't safely idempotent here. A
    // silently-queued-then-lost administration status is a real patient-
    // safety risk (a nurse believing a dose was given/withheld when the
    // write never actually landed) — same reasoning BillingRepository
    // already applies to money.
    suspend fun updateMedicationAdministrationStatus(id: String, req: UpdateMedicationAdministrationStatusRequest): Result<MedicationAdministration> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val res = api.updateMedicationAdministrationStatus(id = id, req = req, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) Result.success(res.body()!!)
            else Result.failure(Exception(res.errorBody()?.string() ?: "Couldn't update — try again once connected"))
        } catch (e: Exception) {
            Result.failure(Exception("Offline — try again once connected"))
        }
    }

    suspend fun listInvestigations(admissionId: String): Result<List<InvestigationOrder>> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val res = api.listInvestigations(admissionId = admissionId, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) Result.success(res.body()!!)
            else Result.failure(Exception("Failed to load investigations"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Online-only — infrequent, deliberate action; no client-id safety net
    // server-side for this endpoint.
    suspend fun updateInvestigationStatus(id: String, status: InvestigationOrderStatus): Result<InvestigationOrder> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val res = api.updateInvestigationStatus(id = id, req = UpdateInvestigationStatusRequest(status), clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) Result.success(res.body()!!)
            else Result.failure(Exception(res.errorBody()?.string() ?: "Couldn't update — try again once connected"))
        } catch (e: Exception) {
            Result.failure(Exception("Offline — try again once connected"))
        }
    }

    // Online-only — result-attach already depends on the (online-only)
    // document-upload endpoint anyway, no point making half the flow
    // offline-safe.
    suspend fun addInvestigationResult(id: String, req: AddInvestigationResultRequest): Result<InvestigationOrder> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val res = api.addInvestigationResult(id = id, req = req, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) Result.success(res.body()!!)
            else Result.failure(Exception(res.errorBody()?.string() ?: "Couldn't save the result — try again once connected"))
        } catch (e: Exception) {
            Result.failure(Exception("Offline — try again once connected"))
        }
    }

    suspend fun listTimeline(admissionId: String): Result<List<IpdTimelineEvent>> = withContext(Dispatchers.IO) {
        val clinicId = cookieJar.getActiveClinicId()
        try {
            val res = api.listIpdTimeline(admissionId = admissionId, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) Result.success(res.body()!!)
            else Result.failure(Exception("Failed to load timeline"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
