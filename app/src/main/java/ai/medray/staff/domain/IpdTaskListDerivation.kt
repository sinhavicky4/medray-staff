package ai.medray.staff.domain

import ai.medray.staff.data.network.IpdAdmission
import ai.medray.staff.data.network.InvestigationOrder
import ai.medray.staff.data.network.InvestigationOrderStatus
import ai.medray.staff.data.network.MedicationAdministration
import ai.medray.staff.data.network.MedicationAdministrationStatus
import java.time.Duration
import java.time.Instant

enum class IpdTaskType { VITALS_DUE, MEDICATION_DUE, INVESTIGATION_PENDING }

data class IpdTaskListItem(
    val admissionId: String,
    val patientName: String,
    val taskType: IpdTaskType,
    val label: String,
    // Present for MEDICATION_DUE (the administration's own scheduledAt);
    // null for VITALS_DUE/INVESTIGATION_PENDING, which have no single due
    // instant to sort by beyond "it's already pending."
    val dueAt: Instant? = null
)

data class IpdWardTileCounts(
    val patients: Int,
    val vitalsDue: Int,
    val medicationDue: Int,
    val investigationTasks: Int
) {
    val pendingTasks: Int get() = vitalsDue + medicationDue + investigationTasks
}

/**
 * The "Task List" / Ward Home tile counts (spec §17 "My Ward": Patients /
 * Vitals Due / Medication Due / Pending Tasks / Investigation Tasks) are
 * explicitly NOT backed by any backend entity — there is no scheduler, no
 * recurring "vitals every 4h" order, no clinic-wide task table (confirmed
 * against api/src/routes/ipd*.ts). Everything here is derived client-side
 * from ordinary list reads the app already has to fetch one admission at a
 * time.
 *
 * Medication Due and Investigation Pending are both directly backed by real
 * backend state (a MedicationAdministration in SCHEDULED past its
 * scheduledAt or already DUE; an InvestigationOrder not yet ORDERED/
 * ACCEPTED/IN_PROGRESS with no result). Vitals Due is NOT backed by
 * anything — no field anywhere records "when vitals are next expected." The
 * 4-hour default below is a heuristic drawn from spec §29's own worked
 * example ("Doctor enters: Vitals every 4 hours"), not a configured or
 * enforced schedule — surface it in the UI as exactly that (a suggestion,
 * not a hard deadline the backend is tracking) rather than implying more
 * precision than the data actually supports.
 */
object IpdTaskListDerivation {

    private val DEFAULT_VITALS_DUE_AFTER: Duration = Duration.ofHours(4)

    private fun parseInstant(iso: String?): Instant? = if (iso.isNullOrBlank()) null else try {
        Instant.parse(iso)
    } catch (_: Exception) {
        null
    }

    /**
     * Tasks for one admission. [medicationAdministrations]/[investigations]
     * should already be scoped to this admission (both backend endpoints
     * require an admissionId query param, there is no clinic-wide list).
     * [lastVitalsRecordedAt] is the most recent IpdVitalsReading.recordedAt
     * for this admission, if any has ever been taken.
     */
    fun deriveTasksForAdmission(
        admission: IpdAdmission,
        medicationAdministrations: List<MedicationAdministration>,
        investigations: List<InvestigationOrder>,
        lastVitalsRecordedAt: String?,
        now: Instant = Instant.now(),
        vitalsDueAfter: Duration = DEFAULT_VITALS_DUE_AFTER
    ): List<IpdTaskListItem> {
        val patientName = admission.patient?.fullName ?: "Patient"
        val tasks = mutableListOf<IpdTaskListItem>()

        val lastVitals = parseInstant(lastVitalsRecordedAt)
        val admittedAt = parseInstant(admission.admissionDateTime)
        val vitalsBaseline = lastVitals ?: admittedAt
        if (vitalsBaseline != null && Duration.between(vitalsBaseline, now) >= vitalsDueAfter) {
            tasks += IpdTaskListItem(
                admissionId = admission.id,
                patientName = patientName,
                taskType = IpdTaskType.VITALS_DUE,
                label = "Vitals due"
            )
        }

        for (admin in medicationAdministrations) {
            val scheduledAt = parseInstant(admin.scheduledAt)
            val isDueNow = admin.status == MedicationAdministrationStatus.DUE ||
                (admin.status == MedicationAdministrationStatus.SCHEDULED && scheduledAt != null && !scheduledAt.isAfter(now))
            if (isDueNow) {
                tasks += IpdTaskListItem(
                    admissionId = admission.id,
                    patientName = patientName,
                    taskType = IpdTaskType.MEDICATION_DUE,
                    label = "Medication due",
                    dueAt = scheduledAt
                )
            }
        }

        val pendingInvestigationStatuses = setOf(
            InvestigationOrderStatus.ORDERED,
            InvestigationOrderStatus.ACCEPTED,
            InvestigationOrderStatus.IN_PROGRESS
        )
        for (inv in investigations) {
            if (inv.status in pendingInvestigationStatuses && inv.resultAt == null) {
                tasks += IpdTaskListItem(
                    admissionId = admission.id,
                    patientName = patientName,
                    taskType = IpdTaskType.INVESTIGATION_PENDING,
                    label = "${inv.testName} pending"
                )
            }
        }

        return tasks
    }

    fun tileCounts(admissionCount: Int, allTasks: List<IpdTaskListItem>): IpdWardTileCounts = IpdWardTileCounts(
        patients = admissionCount,
        vitalsDue = allTasks.count { it.taskType == IpdTaskType.VITALS_DUE },
        medicationDue = allTasks.count { it.taskType == IpdTaskType.MEDICATION_DUE },
        investigationTasks = allTasks.count { it.taskType == IpdTaskType.INVESTIGATION_PENDING }
    )
}
