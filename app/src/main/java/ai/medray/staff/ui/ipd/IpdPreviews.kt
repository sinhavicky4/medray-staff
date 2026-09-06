package ai.medray.staff.ui.ipd

// Compose Previews for the new IPD screens — new to this codebase (no
// @Preview usage existed anywhere before this file), added specifically
// because the app's API_BASE_URL points only at the deployed prod backend
// (no Phase 1 IPD routes live there yet), so a real on-device walkthrough
// isn't possible until either a local API override is used or Phase 1/2
// deploy. These previews are the one form of visual verification available
// with no backend and no device — open this file in Android Studio's Split
// or Design view to render them.

import androidx.compose.ui.tooling.preview.Preview
import ai.medray.staff.data.model.DoctorSummary
import ai.medray.staff.data.model.Patient
import ai.medray.staff.data.network.*
import ai.medray.staff.domain.IpdTaskListItem
import ai.medray.staff.domain.IpdTaskType
import ai.medray.staff.ui.theme.MedRayStaffTheme

private val previewPatientA = Patient(id = "p1", clinicId = "c1", fullName = "Aarav Sharma", uhid = "65823")
private val previewPatientB = Patient(id = "p2", clinicId = "c1", fullName = "Priya Nair", uhid = "55489")

private val previewBed = BedSummary(id = "b1", label = "101-A", room = RoomSummary(id = "r1", name = "101", ward = WardSummary(id = "w1", name = "General Ward")))

private val previewAdmissions = listOf(
    IpdAdmission(
        id = "adm-1", clinicId = "c1", admissionNumber = "IPD-20260906-001", patientId = "p1", patient = previewPatientA,
        status = AdmissionStatus.INPATIENT, admissionDateTime = "2026-09-05T04:00:00Z", admittingDoctorId = "d1", attendingDoctorId = "d1",
        attendingDoctor = DoctorSummary(id = "d1", fullName = "Dr. Rao"), reasonForAdmission = "Fever and abdominal pain",
        provisionalDiagnosis = "Suspected appendicitis",
        bedAssignments = listOf(BedAssignmentSummary(id = "ba1", bedId = "b1", bed = previewBed, status = BedAssignmentStatus.ACTIVE))
    ),
    IpdAdmission(
        id = "adm-2", clinicId = "c1", admissionNumber = "IPD-20260906-002", patientId = "p2", patient = previewPatientB,
        status = AdmissionStatus.DISCHARGE_INITIATED, admissionDateTime = "2026-09-03T10:00:00Z", admittingDoctorId = "d1", attendingDoctorId = "d1",
        attendingDoctor = DoctorSummary(id = "d1", fullName = "Dr. Rao"), reasonForAdmission = "Post-op recovery",
        provisionalDiagnosis = "Appendectomy recovery", dischargeInitiatedAt = "2026-09-06T02:00:00Z"
    )
)

private val previewTasks = listOf(
    IpdTaskListItem(admissionId = "adm-1", patientName = "Aarav Sharma", taskType = IpdTaskType.VITALS_DUE, label = "Vitals due"),
    IpdTaskListItem(admissionId = "adm-1", patientName = "Aarav Sharma", taskType = IpdTaskType.MEDICATION_DUE, label = "Medication due"),
    IpdTaskListItem(admissionId = "adm-2", patientName = "Priya Nair", taskType = IpdTaskType.INVESTIGATION_PENDING, label = "CBC pending")
)

@Preview(showBackground = true, name = "Ward Home")
@androidx.compose.runtime.Composable
private fun IpdWardHomeScreenPreview() {
    MedRayStaffTheme {
        IpdWardHomeScreen(
            userName = "Nurse Priya",
            admissions = previewAdmissions,
            tasks = previewTasks,
            isLoading = false,
            onRefresh = {},
            onPatientsClick = {},
            onTaskListClick = {},
            onPatientClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Patient List")
@androidx.compose.runtime.Composable
private fun IpdPatientListScreenPreview() {
    MedRayStaffTheme {
        IpdPatientListScreen(
            admissions = previewAdmissions,
            searchQuery = "",
            onSearchChange = {},
            isLoading = false,
            onRefresh = {},
            onPatientClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Task List")
@androidx.compose.runtime.Composable
private fun IpdTaskListScreenPreview() {
    MedRayStaffTheme {
        IpdTaskListScreen(tasks = previewTasks, isLoading = false, onRefresh = {}, onTaskClick = {})
    }
}

@Preview(showBackground = true, name = "Patient Chart")
@androidx.compose.runtime.Composable
private fun IpdPatientChartScreenPreview() {
    MedRayStaffTheme {
        IpdPatientChartScreen(
            chart = IpdChartData(
                admission = previewAdmissions[0],
                vitals = listOf(
                    IpdVitalsReading(admissionId = "adm-1", recordedAt = "2026-09-06T05:00:00Z", temperatureF = 99.5, bloodPressure = "120/80", pulseBpm = 78, spo2Percent = 98)
                ),
                notes = listOf(NursingNote(admissionId = "adm-1", note = "Patient resting comfortably.", createdAt = "2026-09-06T05:05:00Z")),
                medicationOrders = listOf(
                    MedicationOrder(
                        id = "mo1", admissionId = "adm-1", medicationName = "Amoxicillin", dose = "500mg", route = "Oral", frequency = "1-0-1", startAt = "2026-09-06T00:00:00Z",
                        administrations = listOf(MedicationAdministration(id = "ma1", medicationOrderId = "mo1", scheduledAt = "2026-09-06T10:00:00Z", dose = "500mg", route = "Oral", status = MedicationAdministrationStatus.DUE))
                    )
                ),
                investigations = listOf(InvestigationOrder(id = "inv1", admissionId = "adm-1", investigationType = "LAB", testName = "CBC", status = InvestigationOrderStatus.ORDERED)),
                timeline = listOf(IpdTimelineEvent(id = "t1", eventType = "ADMISSION_CREATED", occurredAt = "2026-09-05T04:00:00Z", summary = "Admission IPD-20260906-001 requested"))
            ),
            error = null,
            onBack = {},
            onRefresh = {},
            onRecordVitals = {},
            onAddNursingNote = {},
            onCreateMedicationAdministration = {},
            onUpdateMedicationAdministrationStatus = { _, _ -> },
            onUpdateInvestigationStatus = { _, _ -> },
            onAddInvestigationResult = { _, _ -> }
        )
    }
}
