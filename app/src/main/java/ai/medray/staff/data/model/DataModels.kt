package ai.medray.staff.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

enum class UserRole {
    SUPER_ADMIN,
    CLINIC_ADMIN,
    DOCTOR,
    NURSE,
    RECEPTIONIST
}

enum class QueueStatus {
    WAITING,
    ARRIVED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    NO_SHOW
}

enum class AppointmentStatus {
    SCHEDULED,
    CHECKED_IN,
    COMPLETED,
    CANCELLED,
    NO_SHOW
}

enum class SelfCheckInStatus {
    PENDING,
    ASSIGNED,
    REJECTED
}

enum class InvoiceStatus {
    DRAFT,
    ISSUED,
    PAID,
    PARTIALLY_PAID,
    CANCELLED,
    REFUNDED
}

enum class PaymentMethod {
    CASH,
    UPI,
    CARD,
    NET_BANKING,
    OTHER
}

enum class DocumentKind {
    LAB_REPORT,
    RADIOLOGY,
    PRESCRIPTION,
    DISCHARGE_SUMMARY,
    INSURANCE,
    OTHER
}

data class Clinic(
    val id: String,
    val name: String,
    val address: String? = null,
    val phone: String? = null,
    val gstNumber: String? = null,
    val upiVpa: String? = null, // VPA for dynamic UPI QR generation
    val defaultConsultationFee: Double = 500.0
) : Serializable

data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val phone: String? = null,
    val roles: List<UserRole> = emptyList(),
    val role: String? = null,
    val clinicId: String? = null,
    val clinic: Clinic? = null,
    val activeClinic: Clinic? = null,
    val specialization: String? = null,
    val qualification: String? = null,
    val registrationNumber: String? = null
) : Serializable {
    val isNurse: Boolean 
        get() = roles.contains(UserRole.NURSE) || 
                role?.equals("NURSE", ignoreCase = true) == true || 
                specialization?.contains("nurse", ignoreCase = true) == true
                
    val isReceptionist: Boolean 
        get() = roles.contains(UserRole.RECEPTIONIST) || 
                roles.contains(UserRole.CLINIC_ADMIN) || 
                role?.contains("reception", ignoreCase = true) == true || 
                role?.contains("staff", ignoreCase = true) == true || 
                role?.contains("front", ignoreCase = true) == true || 
                !isNurse
                
    val isClinicAdmin: Boolean 
        get() = roles.contains(UserRole.CLINIC_ADMIN) || 
                roles.contains(UserRole.SUPER_ADMIN) || 
                role?.contains("admin", ignoreCase = true) == true
}

data class DoctorSummary(
    val id: String,
    val fullName: String,
    val specialization: String? = null
) : Serializable

data class Patient(
    val id: String,
    val clinicId: String,
    val fullName: String,
    val uhid: String,
    val phone: String? = null,
    val email: String? = null,
    val dob: String? = null,
    val age: Int? = null,
    val gender: String = "MALE",
    val bloodGroup: String? = null,
    val address: String? = null,
    val emergencyContact: String? = null,
    val photoUrl: String? = null,
    val createdAt: String? = null
) : Serializable

data class Vitals(
    val vitalsBp: String? = null,
    val vitalsTemperatureF: Double? = null,
    val vitalsPulseBpm: Int? = null,
    val vitalsRespRate: Int? = null,
    val vitalsSpo2: Int? = null,
    val vitalsWeightKg: Double? = null,
    val vitalsHeightCm: Double? = null
) : Serializable {
    val bmi: Double?
        get() {
            val weight = vitalsWeightKg ?: return null
            val heightCm = vitalsHeightCm ?: return null
            if (heightCm <= 0) return null
            val heightM = heightCm / 100.0
            return (weight / (heightM * heightM) * 10.0).toInt() / 10.0
        }

    fun hasAnyReading(): Boolean =
        !vitalsBp.isNullOrBlank() || vitalsTemperatureF != null || vitalsPulseBpm != null ||
                vitalsRespRate != null || vitalsSpo2 != null || vitalsWeightKg != null || vitalsHeightCm != null
}

data class QueueEntry(
    val id: String,
    val clinicId: String,
    val patientId: String,
    val doctorId: String,
    val opdNumber: String,
    val chiefComplaint: String,
    val visitType: String = "FIRST_VISIT",
    val status: QueueStatus = QueueStatus.WAITING,
    val cancelReason: String? = null,
    val scheduledAt: String,
    val createdAt: String? = null,
    val createdBy: String? = null,
    val vitalsBp: String? = null,
    val vitalsTemperatureF: Double? = null,
    val vitalsPulseBpm: Int? = null,
    val vitalsRespRate: Int? = null,
    val vitalsSpo2: Int? = null,
    val vitalsWeightKg: Double? = null,
    val vitalsHeightCm: Double? = null,
    val patient: Patient? = null,
    val doctor: DoctorSummary? = null
) : Serializable {
    val vitals: Vitals
        get() = Vitals(
            vitalsBp = vitalsBp,
            vitalsTemperatureF = vitalsTemperatureF,
            vitalsPulseBpm = vitalsPulseBpm,
            vitalsRespRate = vitalsRespRate,
            vitalsSpo2 = vitalsSpo2,
            vitalsWeightKg = vitalsWeightKg,
            vitalsHeightCm = vitalsHeightCm
        )

    val formattedTime: String
        get() {
            val raw = createdAt ?: scheduledAt
            return try {
                if (raw.contains("T")) {
                    val timePart = raw.substringAfter("T").substringBefore("Z").substringBefore("+").substringBefore(".")
                    val parts = timePart.split(":")
                    if (parts.size >= 2) {
                        val hour = parts[0].toInt()
                        val min = parts[1]
                        val ampm = if (hour >= 12) "PM" else "AM"
                        val h12 = if (hour % 12 == 0) 12 else hour % 12
                        "$h12:$min $ampm"
                    } else raw
                } else if (raw.contains(":")) {
                    raw
                } else {
                    raw
                }
            } catch (_: Exception) {
                raw
            }
        }

    val addedByDisplay: String
        get() = createdBy?.ifBlank { "Front Desk" } ?: "Front Desk"
}

data class Appointment(
    val id: String,
    val clinicId: String,
    val patientId: String,
    val doctorId: String,
    val scheduledAt: String,
    val durationMinutes: Int = 15,
    val chiefComplaint: String,
    val visitType: String = "FIRST_VISIT",
    val status: AppointmentStatus = AppointmentStatus.SCHEDULED,
    val cancelReason: String? = null,
    val patient: Patient,
    val doctor: DoctorSummary
) : Serializable

data class InvoiceLineItem(
    val id: String,
    val invoiceId: String,
    val description: String,
    val quantity: Int = 1,
    val unitPrice: Double,
    val amount: Double
) : Serializable

data class Payment(
    val id: String,
    val invoiceId: String,
    val amount: Double,
    val paymentMethod: PaymentMethod,
    val transactionRef: String? = null,
    val recordedAt: String
) : Serializable

data class Invoice(
    val id: String,
    val clinicId: String,
    val patientId: String,
    val visitId: String? = null,
    val invoiceNumber: String,
    val status: InvoiceStatus = InvoiceStatus.ISSUED,
    val discountAmount: Double = 0.0,
    val subtotal: Double = 0.0,
    val total: Double = 0.0,
    val netPaid: Double = 0.0,
    val balanceDue: Double = 0.0,
    val issuedAt: String,
    val lineItems: List<InvoiceLineItem> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val patient: Patient? = null,
    val clinic: Clinic? = null
) : Serializable

data class PatientDocument(
    val id: String,
    val patientId: String,
    val clinicId: String,
    val documentKind: DocumentKind = DocumentKind.LAB_REPORT,
    val fileUrl: String,
    val fileName: String,
    val fileSize: Long? = null,
    val mimeType: String? = null,
    val notes: String? = null,
    val createdAt: String
) : Serializable

data class SelfCheckIn(
    val id: String,
    val clinicId: String,
    val patientId: String,
    val status: SelfCheckInStatus = SelfCheckInStatus.PENDING,
    val chiefComplaint: String? = null,
    val vitals: Vitals? = null,
    val createdAt: String,
    val patient: Patient? = null
) : Serializable
