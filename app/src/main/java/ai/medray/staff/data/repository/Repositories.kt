package ai.medray.staff.data.repository

import android.content.Context
import ai.medray.staff.data.local.PatientEntity
import ai.medray.staff.data.local.QueueEntryEntity
import ai.medray.staff.data.local.StaffDatabase
import ai.medray.staff.data.model.*
import ai.medray.staff.data.network.*
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
    private val interceptor = ApiClient.getAuthInterceptor(context)

    fun isLoggedIn(): Boolean = !interceptor.getToken().isNullOrBlank()

    fun getActiveClinicId(): String? = interceptor.getActiveClinicId()

    fun setActiveClinicId(clinicId: String) {
        interceptor.saveActiveClinicId(clinicId)
    }

    suspend fun sendOtp(phone: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val res = api.sendOtp(SendOtpRequest(phone = phone))
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!.message)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to send OTP"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyOtp(phone: String, code: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val res = api.verifyOtp(VerifyOtpRequest(phone = phone, code = code))
            if (res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                interceptor.saveToken(body.token)
                val activeClinic = body.user.activeClinic ?: body.user.clinic
                if (activeClinic != null) {
                    interceptor.saveActiveClinicId(activeClinic.id)
                }
                Result.success(body.user)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Invalid OTP"))
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
                    interceptor.saveActiveClinicId(activeClinic.id)
                }
                Result.success(user)
            } else {
                Result.failure(Exception("Failed to get user profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        interceptor.clearToken()
    }
}

class QueueRepository(private val context: Context) {
    private val api = ApiClient.getService(context)
    private val db = StaffDatabase.getDatabase(context)
    private val outbox = OutboxManager(context)
    private val interceptor = ApiClient.getAuthInterceptor(context)

    fun getLocalQueue(clinicId: String): Flow<List<QueueEntry>> {
        return db.queueDao().getQueue(clinicId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun refreshQueue(date: String? = null): Result<List<QueueEntry>> = withContext(Dispatchers.IO) {
        val clinicId = interceptor.getActiveClinicId()
        try {
            val res = api.getQueue(date = date, clinicId = clinicId)
            if (res.isSuccessful && res.body() != null) {
                val entries = res.body()!!
                val entities = entries.map { QueueEntryEntity.fromDomain(it) }
                db.queueDao().insertQueue(entities)
                Result.success(entries)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to fetch queue"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerQueueEntry(
        patientId: String,
        doctorId: String,
        chiefComplaint: String,
        visitType: String = "FIRST_VISIT",
        vitals: Vitals? = null
    ): Result<QueueEntry> = withContext(Dispatchers.IO) {
        val clinicId = interceptor.getActiveClinicId() ?: return@withContext Result.failure(Exception("No active clinic"))
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
                // Offline fallback enqueue
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
        val clinicId = interceptor.getActiveClinicId()
        val req = UpdateVitalsRequest(
            vitalsBp = vitals.vitalsBp,
            vitalsTemperatureF = vitals.vitalsTemperatureF,
            vitalsPulseBpm = vitals.vitalsPulseBpm,
            vitalsRespRate = vitals.vitalsRespRate,
            vitalsSpo2 = vitals.vitalsSpo2,
            vitalsWeightKg = vitals.vitalsWeightKg,
            vitalsHeightCm = vitals.vitalsHeightCm
        )

        // Optimistic local update
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
        val clinicId = interceptor.getActiveClinicId()
        val req = UpdateQueueStatusRequest(status = status, cancelReason = cancelReason)

        // Optimistic local update
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
}

class PatientRepository(private val context: Context) {
    private val api = ApiClient.getService(context)
    private val db = StaffDatabase.getDatabase(context)
    private val interceptor = ApiClient.getAuthInterceptor(context)

    suspend fun searchPatients(query: String): Result<List<Patient>> = withContext(Dispatchers.IO) {
        val clinicId = interceptor.getActiveClinicId()
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
        val clinicId = interceptor.getActiveClinicId()
        try {
            val res = api.registerPatient(req = req, clinicId = clinicId)
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
}

class AppointmentRepository(private val context: Context) {
    private val api = ApiClient.getService(context)
    private val interceptor = ApiClient.getAuthInterceptor(context)

    suspend fun listAppointments(date: String? = null, doctorId: String? = null): Result<List<Appointment>> = withContext(Dispatchers.IO) {
        val clinicId = interceptor.getActiveClinicId()
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
        val clinicId = interceptor.getActiveClinicId()
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
        val clinicId = interceptor.getActiveClinicId()
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
        val clinicId = interceptor.getActiveClinicId()
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
    private val interceptor = ApiClient.getAuthInterceptor(context)

    suspend fun listInvoices(patientId: String? = null): Result<List<Invoice>> = withContext(Dispatchers.IO) {
        val clinicId = interceptor.getActiveClinicId()
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
        discountAmount: Double,
        lineItems: List<CreateInvoiceLineItemInput>
    ): Result<Invoice> = withContext(Dispatchers.IO) {
        val clinicId = interceptor.getActiveClinicId()
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
        val clinicId = interceptor.getActiveClinicId()
        try {
            val req = RecordPaymentRequest(
                amount = amount,
                paymentMethod = paymentMethod,
                transactionRef = transactionRef
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
        notes: String? = null
    ): Result<PatientDocument> = withContext(Dispatchers.IO) {
        try {
            val reqFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, reqFile)
            val kindPart = kind.name.toRequestBody("text/plain".toMediaTypeOrNull())
            val notesPart = notes?.toRequestBody("text/plain".toMediaTypeOrNull())

            val res = api.uploadDocument(patientId, filePart, kindPart, notesPart)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("Document upload failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
