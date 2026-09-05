package ai.medray.staff.ui.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClinicSignupFormStateTest {

    @Test
    fun `isStep1Valid requires clinicName to be non-blank`() {
        val empty = ClinicSignupFormState()
        assertFalse(empty.isStep1Valid)

        val whitespaceOnly = ClinicSignupFormState(clinicName = "   ")
        assertFalse(whitespaceOnly.isStep1Valid)

        val valid = ClinicSignupFormState(clinicName = "Care Clinic")
        assertTrue(valid.isStep1Valid)
    }

    @Test
    fun `isStep2Valid checks admin fields and password rules`() {
        val validStep1 = ClinicSignupFormState(
            clinicName = "Care Clinic"
        )
        assertFalse(validStep1.isStep2Valid)

        val missingPassword = validStep1.copy(
            adminFullName = "Dr. Sharma",
            adminEmail = "sharma@example.com",
            adminPhone = "9876543210"
        )
        assertFalse(missingPassword.isStep2Valid)

        val shortPassword = missingPassword.copy(
            password = "short",
            confirmPassword = "short"
        )
        assertFalse(shortPassword.isStep2Valid)

        val mismatchedPassword = missingPassword.copy(
            password = "validPassword123",
            confirmPassword = "differentPassword"
        )
        assertFalse(mismatchedPassword.isStep2Valid)
        assertFalse(mismatchedPassword.passwordsMatch)

        val validStep2 = missingPassword.copy(
            password = "validPassword123",
            confirmPassword = "validPassword123"
        )
        assertTrue(validStep2.isStep2Valid)
        assertTrue(validStep2.passwordsMatch)
        assertTrue(validStep2.isValid)
    }

    @Test
    fun `isValid requires both step 1 and step 2 to be valid`() {
        val step2Only = ClinicSignupFormState(
            clinicName = "",
            adminFullName = "Dr. Sharma",
            adminEmail = "sharma@example.com",
            adminPhone = "9876543210",
            password = "validPassword123",
            confirmPassword = "validPassword123"
        )
        assertFalse(step2Only.isStep1Valid)
        assertTrue(step2Only.isStep2Valid)
        assertFalse(step2Only.isValid)

        val fullyValid = step2Only.copy(clinicName = "Apex Health Clinic")
        assertTrue(fullyValid.isValid)
    }
}
