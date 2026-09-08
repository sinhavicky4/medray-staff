package ai.medray.staff.domain

import ai.medray.staff.data.model.User
import ai.medray.staff.data.model.UserRole
import ai.medray.staff.data.network.CreateIpdAdmissionRequest
import ai.medray.staff.data.network.IpdBed
import ai.medray.staff.data.network.RoomSummary
import ai.medray.staff.data.network.WardSummary
import org.junit.Assert.*
import org.junit.Test

class IpdAdmissionCreationTest {

    private fun isAuthorizedToAdmit(user: User): Boolean {
        return user.isClinicAdmin || 
               user.roles.contains(UserRole.RECEPTIONIST) || 
               (user.isReceptionist && !user.isNurse)
    }

    @Test
    fun `Clinic Admin is authorized to admit patient to IPD`() {
        val adminUser = User(
            id = "admin-1",
            email = "admin@example.com",
            fullName = "Clinic Administrator",
            roles = listOf(UserRole.CLINIC_ADMIN)
        )
        assertTrue(adminUser.isClinicAdmin)
        assertTrue(isAuthorizedToAdmit(adminUser))
    }

    @Test
    fun `Receptionist is authorized to admit patient to IPD`() {
        val receptionistUser = User(
            id = "rec-1",
            email = "reception@example.com",
            fullName = "Front Desk Staff",
            roles = listOf(UserRole.RECEPTIONIST)
        )
        assertTrue(receptionistUser.isReceptionist)
        assertTrue(isAuthorizedToAdmit(receptionistUser))
    }

    @Test
    fun `Pure nurse is not authorized to create admission`() {
        val nurseUser = User(
            id = "nurse-1",
            email = "nurse@example.com",
            fullName = "Staff Nurse",
            roles = listOf(UserRole.NURSE)
        )
        assertTrue(nurseUser.isNurse)
        assertFalse(nurseUser.isClinicAdmin)
        assertFalse(isAuthorizedToAdmit(nurseUser))
    }

    @Test
    fun `Nurse with dual receptionist role is authorized to create admission`() {
        val dualUser = User(
            id = "dual-1",
            email = "dual@example.com",
            fullName = "Dual Role Staff",
            roles = listOf(UserRole.NURSE, UserRole.RECEPTIONIST)
        )
        assertTrue(dualUser.isNurse)
        assertTrue(dualUser.roles.contains(UserRole.RECEPTIONIST))
        assertTrue(isAuthorizedToAdmit(dualUser))
    }

    @Test
    fun `CreateIpdAdmissionRequest populates correct default values`() {
        val req = CreateIpdAdmissionRequest(
            patientId = "pat-101",
            admittingDoctorId = "doc-1",
            attendingDoctorId = "doc-2",
            reasonForAdmission = "Severe acute asthma exacerbation",
            provisionalDiagnosis = "Status asthmaticus"
        )

        assertEquals("pat-101", req.patientId)
        assertEquals("doc-1", req.admittingDoctorId)
        assertEquals("doc-2", req.attendingDoctorId)
        assertEquals("OPD", req.source)
        assertEquals("ELECTIVE", req.type)
        assertEquals("SELF_PAY", req.paymentType)
        assertNull(req.attendantName)
        assertNull(req.attendantPhone)
    }

    @Test
    fun `IpdBed data model holds room and ward relations correctly`() {
        val bed = IpdBed(
            id = "bed-101",
            label = "Bed 101-A",
            status = "AVAILABLE",
            dailyRate = 2500.0,
            room = RoomSummary(
                id = "room-1",
                name = "Deluxe Room 1",
                ward = WardSummary(
                    id = "ward-1",
                    name = "General Medical Ward"
                )
            )
        )

        assertEquals("Bed 101-A", bed.label)
        assertEquals("AVAILABLE", bed.status)
        assertEquals("Deluxe Room 1", bed.room.name)
        assertEquals("General Medical Ward", bed.room.ward.name)
        assertEquals(2500.0, bed.dailyRate!!, 0.001)
    }
}
