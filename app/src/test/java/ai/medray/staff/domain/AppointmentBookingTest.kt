package ai.medray.staff.domain

import ai.medray.staff.data.model.formatToIsoUtc
import ai.medray.staff.data.network.BookAppointmentRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AppointmentBookingTest {

    @Test
    fun `formatToIsoUtc converts local date and time to valid ISO UTC instant`() {
        val kolkataZone = ZoneId.of("Asia/Kolkata")
        val date = LocalDate.of(2026, 9, 10)
        // 10:30 AM IST is 05:00 AM UTC
        val result = formatToIsoUtc(date, 10, 30, kolkataZone)
        assertEquals("2026-09-10T05:00:00Z", result)
    }

    @Test
    fun `formatToIsoUtc handles UTC zone directly`() {
        val utcZone = ZoneId.of("UTC")
        val date = LocalDate.of(2026, 9, 15)
        val result = formatToIsoUtc(date, 14, 0, utcZone)
        assertEquals("2026-09-15T14:00:00Z", result)
    }

    @Test
    fun `BookAppointmentRequest default parameters match backend requirements`() {
        val req = BookAppointmentRequest(
            patientId = "patient-123",
            doctorId = "doc-456",
            scheduledAt = "2026-09-10T05:00:00Z",
            chiefComplaint = "Fever and body pain"
        )

        assertEquals(15, req.durationMinutes)
        assertEquals("FIRST_VISIT", req.visitType)
        assertEquals("patient-123", req.patientId)
        assertEquals("doc-456", req.doctorId)
        assertEquals("Fever and body pain", req.chiefComplaint)
    }
}
