package ai.medray.staff.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import ai.medray.staff.data.model.*

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey val id: String,
    val clinicId: String,
    val fullName: String,
    val uhid: String,
    val phone: String?,
    val email: String?,
    val dob: String?,
    val age: Int?,
    val gender: String,
    val bloodGroup: String?,
    val address: String?,
    val emergencyContact: String?,
    val photoUrl: String?,
    val createdAt: String?
) {
    fun toDomain(): Patient = Patient(
        id = id,
        clinicId = clinicId,
        fullName = fullName,
        uhid = uhid,
        phone = phone,
        email = email,
        dob = dob,
        age = age,
        gender = gender,
        bloodGroup = bloodGroup,
        address = address,
        emergencyContact = emergencyContact,
        photoUrl = photoUrl,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(p: Patient): PatientEntity = PatientEntity(
            id = p.id,
            clinicId = p.clinicId,
            fullName = p.fullName,
            uhid = p.uhid,
            phone = p.phone,
            email = p.email,
            dob = p.dob,
            age = p.age,
            gender = p.gender,
            bloodGroup = p.bloodGroup,
            address = p.address,
            emergencyContact = p.emergencyContact,
            photoUrl = p.photoUrl,
            createdAt = p.createdAt
        )
    }
}

@Entity(tableName = "queue_entries")
data class QueueEntryEntity(
    @PrimaryKey val id: String,
    val clinicId: String,
    val patientId: String,
    val doctorId: String,
    val doctorName: String?,
    val doctorSpecialization: String?,
    val patientName: String?,
    val patientPhone: String?,
    val patientUhid: String?,
    val patientGender: String?,
    val patientAge: Int?,
    val opdNumber: String,
    val chiefComplaint: String,
    val visitType: String,
    val status: String,
    val cancelReason: String?,
    val scheduledAt: String,
    val createdAt: String? = null,
    val createdBy: String? = null,
    val vitalsBp: String?,
    val vitalsTemperatureF: Double?,
    val vitalsPulseBpm: Int?,
    val vitalsRespRate: Int?,
    val vitalsSpo2: Int?,
    val vitalsWeightKg: Double?,
    val vitalsHeightCm: Double?
) {
    fun toDomain(): QueueEntry {
        val p = if (patientName != null && patientUhid != null) {
            Patient(
                id = patientId,
                clinicId = clinicId,
                fullName = patientName,
                uhid = patientUhid,
                phone = patientPhone,
                gender = patientGender ?: "MALE",
                age = patientAge
            )
        } else null

        val d = if (doctorName != null) {
            DoctorSummary(id = doctorId, fullName = doctorName, specialization = doctorSpecialization)
        } else null

        val queueStatus = try {
            QueueStatus.valueOf(status)
        } catch (_: Exception) {
            QueueStatus.WAITING
        }

        return QueueEntry(
            id = id,
            clinicId = clinicId,
            patientId = patientId,
            doctorId = doctorId,
            opdNumber = opdNumber,
            chiefComplaint = chiefComplaint,
            visitType = visitType,
            status = queueStatus,
            cancelReason = cancelReason,
            scheduledAt = scheduledAt,
            createdAt = createdAt,
            createdBy = createdBy,
            vitalsBp = vitalsBp,
            vitalsTemperatureF = vitalsTemperatureF,
            vitalsPulseBpm = vitalsPulseBpm,
            vitalsRespRate = vitalsRespRate,
            vitalsSpo2 = vitalsSpo2,
            vitalsWeightKg = vitalsWeightKg,
            vitalsHeightCm = vitalsHeightCm,
            patient = p,
            doctor = d
        )
    }

    companion object {
        fun fromDomain(q: QueueEntry): QueueEntryEntity = QueueEntryEntity(
            id = q.id,
            clinicId = q.clinicId,
            patientId = q.patientId,
            doctorId = q.doctorId,
            doctorName = q.doctor?.fullName,
            doctorSpecialization = q.doctor?.specialization,
            patientName = q.patient?.fullName,
            patientPhone = q.patient?.phone,
            patientUhid = q.patient?.uhid,
            patientGender = q.patient?.gender,
            patientAge = q.patient?.age,
            opdNumber = q.opdNumber,
            chiefComplaint = q.chiefComplaint,
            visitType = q.visitType,
            status = q.status.name,
            cancelReason = q.cancelReason,
            scheduledAt = q.scheduledAt,
            createdAt = q.createdAt,
            createdBy = q.createdBy,
            vitalsBp = q.vitalsBp,
            vitalsTemperatureF = q.vitalsTemperatureF,
            vitalsPulseBpm = q.vitalsPulseBpm,
            vitalsRespRate = q.vitalsRespRate,
            vitalsSpo2 = q.vitalsSpo2,
            vitalsWeightKg = q.vitalsWeightKg,
            vitalsHeightCm = q.vitalsHeightCm
        )
    }
}

@Entity(tableName = "outbox_commands")
data class OutboxCommandEntity(
    @PrimaryKey val id: String,
    val commandType: String,
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val attempts: Int = 0,
    val lastError: String? = null,
    // Set once attempts hits OutboxSyncWorker.MAX_ATTEMPTS — stops the
    // worker retrying a command forever (it previously did, silently, with
    // no way for the nurse to ever learn a write never made it). A
    // permanently-failed command is excluded from getAllPending() and
    // surfaced instead via getFailedCount() for the Ward Home banner.
    val failedPermanently: Boolean = false
)

// Cache-and-fall-back for the ward roster only (mirrors QueueEntryEntity/
// QueueDao exactly) — a nurse needs to see *something* if the network blips
// mid-shift. Deliberately NOT extended to vitals/notes/medication/
// investigation detail reads: a stale cached clinical reading presented as
// current is a worse failure mode than a clear "couldn't load, retry" error
// (same "don't paper over a stale/failed read with fabricated confidence"
// principle BillingRepository already applies to money, extended here to
// clinically-sensitive detail reads).
@Entity(tableName = "ipd_admissions")
data class IpdAdmissionEntity(
    @PrimaryKey val id: String,
    val clinicId: String,
    val admissionNumber: String,
    val patientId: String,
    val patientName: String?,
    val patientUhid: String?,
    val status: String,
    val admissionDateTime: String,
    val attendingDoctorId: String,
    val attendingDoctorName: String?,
    val reasonForAdmission: String,
    val provisionalDiagnosis: String,
    val bedLabel: String?,
    val roomName: String?,
    val wardName: String?
) {
    fun toDomain(): ai.medray.staff.data.network.IpdAdmission {
        val patient = if (patientName != null && patientUhid != null) {
            Patient(id = patientId, clinicId = clinicId, fullName = patientName, uhid = patientUhid)
        } else null
        val doctor = if (attendingDoctorName != null) {
            DoctorSummary(id = attendingDoctorId, fullName = attendingDoctorName)
        } else null
        val bed = if (bedLabel != null && roomName != null && wardName != null) {
            ai.medray.staff.data.network.BedSummary(
                id = "",
                label = bedLabel,
                room = ai.medray.staff.data.network.RoomSummary(id = "", name = roomName, ward = ai.medray.staff.data.network.WardSummary(id = "", name = wardName))
            )
        } else null
        return ai.medray.staff.data.network.IpdAdmission(
            id = id,
            clinicId = clinicId,
            admissionNumber = admissionNumber,
            patientId = patientId,
            patient = patient,
            status = ai.medray.staff.data.network.AdmissionStatus.valueOf(status),
            admissionDateTime = admissionDateTime,
            admittingDoctorId = attendingDoctorId,
            attendingDoctorId = attendingDoctorId,
            attendingDoctor = doctor,
            reasonForAdmission = reasonForAdmission,
            provisionalDiagnosis = provisionalDiagnosis,
            bedAssignments = if (bed != null) {
                listOf(
                    ai.medray.staff.data.network.BedAssignmentSummary(
                        id = "",
                        bedId = bed.id,
                        bed = bed,
                        status = ai.medray.staff.data.network.BedAssignmentStatus.ACTIVE
                    )
                )
            } else emptyList()
        )
    }

    companion object {
        fun fromDomain(a: ai.medray.staff.data.network.IpdAdmission): IpdAdmissionEntity {
            val bed = a.currentBed
            return IpdAdmissionEntity(
                id = a.id,
                clinicId = a.clinicId,
                admissionNumber = a.admissionNumber,
                patientId = a.patientId,
                patientName = a.patient?.fullName,
                patientUhid = a.patient?.uhid,
                status = a.status.name,
                admissionDateTime = a.admissionDateTime,
                attendingDoctorId = a.attendingDoctorId,
                attendingDoctorName = a.attendingDoctor?.fullName,
                reasonForAdmission = a.reasonForAdmission,
                provisionalDiagnosis = a.provisionalDiagnosis,
                bedLabel = bed?.label,
                roomName = bed?.room?.name,
                wardName = bed?.room?.ward?.name
            )
        }
    }
}
