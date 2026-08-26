package ai.medray.staff.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val ISO_TIME_LOCAL_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH).withZone(ZoneId.systemDefault())

/**
 * Formats a server ISO-8601 UTC instant (e.g. "2026-08-26T07:18:00.000Z")
 * as a 12-hour local clock time (e.g. "12:48 PM"). Server timestamps are
 * always UTC ("Z"-suffixed) — this actually converts to the device's own
 * timezone rather than reading the UTC digits as if they were already
 * local, which is what every previous ad-hoc substring parse of these
 * timestamps in this app silently did. Locale pinned to English so the
 * AM/PM marker is always "AM"/"PM" regardless of the device's own locale
 * setting (some locales render "a" as lowercase or non-Latin script).
 */
fun formatIsoTimeLocal(iso: String?): String {
    if (iso.isNullOrBlank()) return iso ?: ""
    return try {
        ISO_TIME_LOCAL_FORMATTER.format(Instant.parse(iso))
    } catch (_: DateTimeParseException) {
        iso
    }
}

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
    val upiId: String? = null,
    val upiVpa: String? = null, // VPA for dynamic UPI QR generation
    val defaultConsultationFee: Double = 500.0
) : java.io.Serializable

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
    val specialization: String? = null,
    val consultationFee: Double? = null
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
    val doctor: DoctorSummary? = null,
    // Sum of AdvancePayment rows collected before a doctor opened this
    // patient's chart, not yet folded into a real Invoice — see
    // completeVisitAndInvoice (api/src/routes/visits.ts).
    val advancePaidTotal: Double = 0.0
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
        get() = formatIsoTimeLocal(createdAt ?: scheduledAt)

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
    val method: PaymentMethod,
    val note: String? = null,
    val recordedAt: String
) : Serializable

data class Invoice(
    val id: String,
    val clinicId: String,
    val patientId: String,
    val visitId: String? = null,
    val status: InvoiceStatus = InvoiceStatus.ISSUED,
    val discountAmount: Double = 0.0,
    val subtotal: Double = 0.0,
    val total: Double = 0.0,
    val netPaid: Double = 0.0,
    val balanceDue: Double = 0.0,
    val createdAt: String? = null,
    val lineItems: List<InvoiceLineItem> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val patient: Patient? = null,
    val clinic: Clinic? = null
) : Serializable {
    // No real "invoice number" exists server-side — an Invoice is
    // identified by id (a UUID), same as the web app. invoiceNumber/
    // issuedAt used to be raw deserialized fields the server never
    // actually sends (real field is createdAt, not issuedAt), so Gson
    // silently left them null at runtime despite their non-null Kotlin
    // type — rendering as the literal text "INV-null" wherever displayed,
    // and a guaranteed NullPointerException the moment anything called a
    // String method on invoiceNumber (BillingScreen's search filter did).
    // Derived the same way InvoiceDetailClient.tsx displays it on web:
    // id.slice(0, 8).toUpperCase().
    val invoiceNumber: String
        get() = id.take(8).uppercase()
}

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

data class PrescriptionItem(
    val id: String? = null,
    val medicineName: String,
    val genericName: String? = null,
    val brandName: String? = null,
    val dosage: String,
    val frequencyCode: String,
    val durationDays: Int? = null,
    val foodInstruction: String,
    val route: String = "Oral",
    val sortOrder: Int = 0
) : Serializable

data class Prescription(
    val id: String,
    val visitId: String? = null,
    val doctorId: String? = null,
    val patientId: String? = null,
    val status: String = "SIGNED",
    val diagnosisRef: List<String> = emptyList(),
    val adviceNotes: String? = null,
    val testsAdvised: String? = null,
    val canvasImageUrl: String? = null,
    val pdfUrl: String? = null,
    val items: List<PrescriptionItem> = emptyList(),
    val createdAt: String? = null
) : Serializable

data class Visit(
    val id: String,
    val clinicId: String,
    val patientId: String,
    val doctorId: String,
    val chiefComplaint: String? = null,
    val diagnosis: String? = null,
    val status: String = "COMPLETED",
    val vitals: Vitals? = null,
    val prescriptions: List<Prescription> = emptyList(),
    val documents: List<PatientDocument> = emptyList(),
    val doctor: DoctorSummary? = null,
    val clinic: Clinic? = null,
    val createdAt: String
) : Serializable
