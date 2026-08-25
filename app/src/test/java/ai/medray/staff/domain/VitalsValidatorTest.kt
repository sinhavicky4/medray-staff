package ai.medray.staff.domain

import ai.medray.staff.data.model.Vitals
import org.junit.Assert.*
import org.junit.Test

class VitalsValidatorTest {

    @Test
    fun testBpValidation() {
        assertTrue(VitalsValidator.validateBp("120/80"))
        assertTrue(VitalsValidator.validateBp("140/90"))
        assertTrue(VitalsValidator.validateBp(null))
        assertTrue(VitalsValidator.validateBp(""))

        // Invalid BP
        assertFalse(VitalsValidator.validateBp("120-80"))
        assertFalse(VitalsValidator.validateBp("80/120")) // Systolic must be > diastolic
        assertFalse(VitalsValidator.validateBp("invalid"))
        assertFalse(VitalsValidator.validateBp("350/80")) // Out of range
    }

    @Test
    fun testVitalsEvaluationNormal() {
        val vitals = Vitals(
            vitalsBp = "120/80",
            vitalsPulseBpm = 75,
            vitalsTemperatureF = 98.6,
            vitalsSpo2 = 99,
            vitalsWeightKg = 70.0,
            vitalsHeightCm = 175.0
        )
        val eval = VitalsValidator.evaluate(vitals)
        assertEquals(VitalsSeverity.NORMAL, eval.overallSeverity)
        assertEquals(22.8, vitals.bmi!!, 0.1)
    }

    @Test
    fun testVitalsEvaluationHypertensiveCrisis() {
        val vitals = Vitals(
            vitalsBp = "190/125",
            vitalsPulseBpm = 80
        )
        val eval = VitalsValidator.evaluate(vitals)
        assertEquals(VitalsSeverity.CRITICAL, eval.overallSeverity)
        assertEquals(VitalsSeverity.CRITICAL, eval.bpSeverity)
        assertTrue(eval.bpMessage!!.contains("Hypertensive Crisis"))
    }

    @Test
    fun testVitalsEvaluationHypoxia() {
        val vitals = Vitals(
            vitalsBp = "120/80",
            vitalsSpo2 = 88
        )
        val eval = VitalsValidator.evaluate(vitals)
        assertEquals(VitalsSeverity.CRITICAL, eval.overallSeverity)
        assertEquals(VitalsSeverity.CRITICAL, eval.spo2Severity)
        assertTrue(eval.spo2Message!!.contains("Hypoxia"))
    }
}
