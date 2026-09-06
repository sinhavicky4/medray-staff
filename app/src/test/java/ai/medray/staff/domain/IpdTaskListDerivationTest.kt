package ai.medray.staff.domain

import ai.medray.staff.data.model.Patient
import ai.medray.staff.data.network.AdmissionStatus
import ai.medray.staff.data.network.InvestigationOrder
import ai.medray.staff.data.network.InvestigationOrderStatus
import ai.medray.staff.data.network.IpdAdmission
import ai.medray.staff.data.network.MedicationAdministration
import ai.medray.staff.data.network.MedicationAdministrationStatus
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class IpdTaskListDerivationTest {

    private val now: Instant = Instant.parse("2026-09-06T12:00:00Z")

    private fun admission(admittedAt: Instant = now.minus(2, ChronoUnit.DAYS)): IpdAdmission = IpdAdmission(
        id = "adm-1",
        clinicId = "clinic-1",
        admissionNumber = "IPD-1",
        patientId = "pat-1",
        patient = Patient(id = "pat-1", clinicId = "clinic-1", fullName = "Test Patient", uhid = "1001"),
        status = AdmissionStatus.INPATIENT,
        admissionDateTime = admittedAt.toString(),
        admittingDoctorId = "doc-1",
        attendingDoctorId = "doc-1",
        reasonForAdmission = "Fever",
        provisionalDiagnosis = "Observation"
    )

    private fun medAdmin(
        status: MedicationAdministrationStatus,
        scheduledAt: Instant
    ): MedicationAdministration = MedicationAdministration(
        id = "ma-1",
        medicationOrderId = "mo-1",
        scheduledAt = scheduledAt.toString(),
        dose = "500mg",
        route = "Oral",
        status = status
    )

    private fun investigation(status: InvestigationOrderStatus, resultAt: String? = null): InvestigationOrder = InvestigationOrder(
        id = "inv-1",
        admissionId = "adm-1",
        investigationType = "LAB",
        testName = "CBC",
        status = status,
        resultAt = resultAt
    )

    // --- Vitals Due ---

    @Test
    fun `no vitals task when last reading was recent`() {
        val tasks = IpdTaskListDerivation.deriveTasksForAdmission(
            admission = admission(),
            medicationAdministrations = emptyList(),
            investigations = emptyList(),
            lastVitalsRecordedAt = now.minus(1, ChronoUnit.HOURS).toString(),
            now = now
        )
        assertTrue(tasks.none { it.taskType == IpdTaskType.VITALS_DUE })
    }

    @Test
    fun `vitals due once past the default 4-hour window since last reading`() {
        val tasks = IpdTaskListDerivation.deriveTasksForAdmission(
            admission = admission(),
            medicationAdministrations = emptyList(),
            investigations = emptyList(),
            lastVitalsRecordedAt = now.minus(5, ChronoUnit.HOURS).toString(),
            now = now
        )
        assertEquals(1, tasks.count { it.taskType == IpdTaskType.VITALS_DUE })
    }

    @Test
    fun `vitals due measured from admission time when none has ever been recorded`() {
        val tasks = IpdTaskListDerivation.deriveTasksForAdmission(
            admission = admission(admittedAt = now.minus(6, ChronoUnit.HOURS)),
            medicationAdministrations = emptyList(),
            investigations = emptyList(),
            lastVitalsRecordedAt = null,
            now = now
        )
        assertEquals(1, tasks.count { it.taskType == IpdTaskType.VITALS_DUE })
    }

    // --- Medication Due ---

    @Test
    fun `scheduled medication not yet due produces no task`() {
        val tasks = IpdTaskListDerivation.deriveTasksForAdmission(
            admission = admission(),
            medicationAdministrations = listOf(medAdmin(MedicationAdministrationStatus.SCHEDULED, now.plus(1, ChronoUnit.HOURS))),
            investigations = emptyList(),
            lastVitalsRecordedAt = now.toString(),
            now = now
        )
        assertTrue(tasks.none { it.taskType == IpdTaskType.MEDICATION_DUE })
    }

    @Test
    fun `scheduled medication past its scheduled time is due`() {
        val tasks = IpdTaskListDerivation.deriveTasksForAdmission(
            admission = admission(),
            medicationAdministrations = listOf(medAdmin(MedicationAdministrationStatus.SCHEDULED, now.minus(10, ChronoUnit.MINUTES))),
            investigations = emptyList(),
            lastVitalsRecordedAt = now.toString(),
            now = now
        )
        assertEquals(1, tasks.count { it.taskType == IpdTaskType.MEDICATION_DUE })
    }

    @Test
    fun `medication already marked DUE is always a task regardless of schedule`() {
        val tasks = IpdTaskListDerivation.deriveTasksForAdmission(
            admission = admission(),
            medicationAdministrations = listOf(medAdmin(MedicationAdministrationStatus.DUE, now.plus(1, ChronoUnit.HOURS))),
            investigations = emptyList(),
            lastVitalsRecordedAt = now.toString(),
            now = now
        )
        assertEquals(1, tasks.count { it.taskType == IpdTaskType.MEDICATION_DUE })
    }

    @Test
    fun `administered medication produces no task`() {
        val tasks = IpdTaskListDerivation.deriveTasksForAdmission(
            admission = admission(),
            medicationAdministrations = listOf(medAdmin(MedicationAdministrationStatus.ADMINISTERED, now.minus(1, ChronoUnit.HOURS))),
            investigations = emptyList(),
            lastVitalsRecordedAt = now.toString(),
            now = now
        )
        assertTrue(tasks.none { it.taskType == IpdTaskType.MEDICATION_DUE })
    }

    // --- Investigation pending ---

    @Test
    fun `ordered investigation with no result is pending`() {
        val tasks = IpdTaskListDerivation.deriveTasksForAdmission(
            admission = admission(),
            medicationAdministrations = emptyList(),
            investigations = listOf(investigation(InvestigationOrderStatus.ORDERED)),
            lastVitalsRecordedAt = now.toString(),
            now = now
        )
        assertEquals(1, tasks.count { it.taskType == IpdTaskType.INVESTIGATION_PENDING })
    }

    @Test
    fun `completed investigation with a result is not pending`() {
        val tasks = IpdTaskListDerivation.deriveTasksForAdmission(
            admission = admission(),
            medicationAdministrations = emptyList(),
            investigations = listOf(investigation(InvestigationOrderStatus.COMPLETED, resultAt = now.toString())),
            lastVitalsRecordedAt = now.toString(),
            now = now
        )
        assertTrue(tasks.none { it.taskType == IpdTaskType.INVESTIGATION_PENDING })
    }

    @Test
    fun `cancelled investigation is not pending`() {
        val tasks = IpdTaskListDerivation.deriveTasksForAdmission(
            admission = admission(),
            medicationAdministrations = emptyList(),
            investigations = listOf(investigation(InvestigationOrderStatus.CANCELLED)),
            lastVitalsRecordedAt = now.toString(),
            now = now
        )
        assertTrue(tasks.none { it.taskType == IpdTaskType.INVESTIGATION_PENDING })
    }

    // --- Tile counts ---

    @Test
    fun `tileCounts aggregates across every admission`() {
        val tasksAdmissionA = IpdTaskListDerivation.deriveTasksForAdmission(
            admission = admission(),
            medicationAdministrations = listOf(medAdmin(MedicationAdministrationStatus.DUE, now)),
            investigations = listOf(investigation(InvestigationOrderStatus.ORDERED)),
            lastVitalsRecordedAt = now.minus(5, ChronoUnit.HOURS).toString(),
            now = now
        )
        val tasksAdmissionB = IpdTaskListDerivation.deriveTasksForAdmission(
            admission = admission(),
            medicationAdministrations = emptyList(),
            investigations = emptyList(),
            lastVitalsRecordedAt = now.toString(),
            now = now
        )
        val counts = IpdTaskListDerivation.tileCounts(admissionCount = 2, allTasks = tasksAdmissionA + tasksAdmissionB)
        assertEquals(2, counts.patients)
        assertEquals(1, counts.vitalsDue)
        assertEquals(1, counts.medicationDue)
        assertEquals(1, counts.investigationTasks)
        assertEquals(3, counts.pendingTasks)
    }
}
