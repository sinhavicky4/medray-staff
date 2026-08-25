package ai.medray.staff.domain

import org.junit.Assert.*
import org.junit.Test

class UpiQrGeneratorTest {

    @Test
    fun testUpiUriConstruction() {
        val uri = UpiQrGenerator.createUpiUri(
            payeeVpa = "medray.clinic@icici",
            payeeName = "MedRay AI Clinic",
            amount = 500.0,
            invoiceNumber = "OPD-20260825-001",
            note = "Consultation Fee"
        )

        assertTrue(uri.startsWith("upi://pay?"))
        assertTrue(uri.contains("pa=medray.clinic@icici"))
        assertTrue(uri.contains("am=500.00"))
        assertTrue(uri.contains("cu=INR"))
        assertTrue(uri.contains("tr=MR-OPD20260825001"))
    }
}
