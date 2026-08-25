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

        var hasFailures = false

        for (cmd in pending) {
            try {
                val success = processCommand(cmd.commandType, cmd.payloadJson)
                if (success) {
                    db.outboxDao().deleteCommand(cmd.id)
                } else {
                    hasFailures = true
                    db.outboxDao().recordAttemptFailure(cmd.id, "Server rejected command")
                }
            } catch (e: Exception) {
                hasFailures = true
                db.outboxDao().recordAttemptFailure(cmd.id, e.message ?: "Network error")
            }
        }

        if (hasFailures) Result.retry() else Result.success()
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
