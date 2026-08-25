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
    val lastError: String? = null
)
