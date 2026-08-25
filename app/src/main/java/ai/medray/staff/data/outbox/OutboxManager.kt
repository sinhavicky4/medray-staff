package ai.medray.staff.data.outbox

import android.content.Context
import androidx.work.*
import ai.medray.staff.data.local.OutboxCommandEntity
import ai.medray.staff.data.local.StaffDatabase
import com.google.gson.Gson
import java.util.UUID
import java.util.concurrent.TimeUnit

class OutboxManager(private val context: Context) {

    private val db = StaffDatabase.getDatabase(context)
    private val gson = Gson()

    suspend fun <T> enqueue(commandType: String, payload: T, clientGeneratedId: String = UUID.randomUUID().toString()) {
        val json = gson.toJson(payload)
        val entity = OutboxCommandEntity(
            id = clientGeneratedId,
            commandType = commandType,
            payloadJson = json
        )
        db.outboxDao().insertCommand(entity)
        triggerSync()
    }

    fun triggerSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<OutboxSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "medray_staff_outbox_sync",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
