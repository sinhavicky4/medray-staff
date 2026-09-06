package ai.medray.staff.data.outbox

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ai.medray.staff.data.local.StaffDatabase
import ai.medray.staff.data.network.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OutboxSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val db = StaffDatabase.getDatabase(appContext)
    private val api = ApiClient.getService(appContext)
    private val gson = Gson()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val pending = db.outboxDao().getAllPending()
        if (pending.isEmpty()) return@withContext Result.success()

        // hasRetryableFailures (distinct from hasFailures) is what decides
        // Result.retry() vs Result.success() below — once every failure
        // this round has been marked failedPermanently, there's nothing
        // left worth WorkManager re-invoking for.
        var hasRetryableFailures = false

        for (cmd in pending) {
            val failure = try {
                if (processCommand(cmd.commandType, cmd.payloadJson)) {
                    db.outboxDao().deleteCommand(cmd.id)
                    null
                } else {
                    "Server rejected command"
                }
            } catch (e: Exception) {
                e.message ?: "Network error"
            }
            if (failure != null) {
                db.outboxDao().recordAttemptFailure(cmd.id, failure)
                if (cmd.attempts + 1 >= MAX_ATTEMPTS) {
                    db.outboxDao().markFailedPermanently(cmd.id)
                } else {
                    hasRetryableFailures = true
                }
            }
        }

        if (hasRetryableFailures) Result.retry() else Result.success()
    }

    companion object {
        // A command stuck failing this many times is far more likely stale
        // or permanently invalid (e.g. the admission it targets no longer
        // exists) than one more retry away from succeeding — retrying it
        // forever, silently, previously left the nurse believing a write
        // had saved when it never would.
        private const val MAX_ATTEMPTS = 5
    }

    private suspend fun processCommand(commandType: String, payloadJson: String): Boolean {
        return when (commandType) {
            "UPDATE_VITALS" -> {
                val data = gson.fromJson(payloadJson, UpdateVitalsCommandPayload::class.java)
                val resp = api.updateQueueVitals(
                    id = data.queueEntryId,
                    req = data.vitals,
                    clinicId = data.clinicId
                )
                resp.isSuccessful
            }
            "UPDATE_STATUS" -> {
                val data = gson.fromJson(payloadJson, UpdateStatusCommandPayload::class.java)
                val resp = api.updateQueueStatus(
                    id = data.queueEntryId,
                    req = data.request,
                    clinicId = data.clinicId
                )
                resp.isSuccessful
            }
            "REGISTER_QUEUE" -> {
                val data = gson.fromJson(payloadJson, RegisterQueueCommandPayload::class.java)
                val resp = api.registerQueueEntry(
                    req = data.request,
                    clinicId = data.clinicId
                )
                resp.isSuccessful
            }
            // Vitals/notes/medication-administration creation are the only
            // IPD writes queued here — all three have a client-generated-id
            // upsert on the server (api/src/routes/ipdNursing.ts/
            // ipdMedications.ts), so a retried command can't create a
            // duplicate row. Status-changing IPD writes (medication
            // administration status, investigation status/result) are
            // deliberately online-only — see IpdRepository's own doc
            // comments for why (same reasoning BillingRepository already
            // applies to money: a silently-queued-then-lost clinical status
            // change is a real patient-safety risk, not just an
            // inconvenience).
            "CREATE_IPD_VITALS" -> {
                val data = gson.fromJson(payloadJson, CreateIpdVitalsCommandPayload::class.java)
                api.recordIpdVitals(req = data.request, clinicId = data.clinicId).isSuccessful
            }
            "CREATE_NURSING_NOTE" -> {
                val data = gson.fromJson(payloadJson, CreateNursingNoteCommandPayload::class.java)
                api.addNursingNote(req = data.request, clinicId = data.clinicId).isSuccessful
            }
            "CREATE_MEDICATION_ADMINISTRATION" -> {
                val data = gson.fromJson(payloadJson, CreateMedicationAdministrationCommandPayload::class.java)
                api.createMedicationAdministration(req = data.request, clinicId = data.clinicId).isSuccessful
            }
            else -> true
        }
    }
}

data class UpdateVitalsCommandPayload(
    val queueEntryId: String,
    val clinicId: String?,
    val vitals: UpdateVitalsRequest
)

data class UpdateStatusCommandPayload(
    val queueEntryId: String,
    val clinicId: String?,
    val request: UpdateQueueStatusRequest
)

data class RegisterQueueCommandPayload(
    val clinicId: String?,
    val request: RegisterQueueRequest
)

data class CreateIpdVitalsCommandPayload(
    val clinicId: String?,
    val request: CreateIpdVitalsRequest
)

data class CreateNursingNoteCommandPayload(
    val clinicId: String?,
    val request: CreateNursingNoteRequest
)

data class CreateMedicationAdministrationCommandPayload(
    val clinicId: String?,
    val request: CreateMedicationAdministrationRequest
)
