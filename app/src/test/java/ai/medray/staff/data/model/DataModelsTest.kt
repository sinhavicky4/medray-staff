package ai.medray.staff.data.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

// Regression coverage for the "queue/appointment times show as UTC, not
// local" bug — formatIsoTimeLocal replaced a naive substring parse of the
// server's UTC-suffixed ISO string that never converted anything.
class DataModelsTest {

    @Test
    fun `formatIsoTimeLocal converts a UTC instant to the device's local clock time`() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))
            assertEquals("12:48 PM", formatIsoTimeLocal("2026-08-26T07:18:00.000Z"))
            // Crosses midnight in IST (18:30 UTC + 5:30 = 00:00 next day).
            assertEquals("12:00 AM", formatIsoTimeLocal("2026-08-26T18:30:00.000Z"))
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test
    fun `formatIsoTimeLocal falls back to the raw string on an unparseable input`() {
        assertEquals("not-a-date", formatIsoTimeLocal("not-a-date"))
    }

    @Test
    fun `formatIsoTimeLocal returns empty string for null or blank input`() {
        assertEquals("", formatIsoTimeLocal(null))
        assertEquals("", formatIsoTimeLocal(""))
    }
}
