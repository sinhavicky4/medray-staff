package ai.medray.staff.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients WHERE clinicId = :clinicId ORDER BY fullName ASC")
    fun getPatients(clinicId: String): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE id = :id LIMIT 1")
    suspend fun getPatientById(id: String): PatientEntity?

    @Query("SELECT * FROM patients WHERE fullName LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR uhid LIKE '%' || :query || '%'")
    suspend fun searchPatients(query: String): List<PatientEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatients(patients: List<PatientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: PatientEntity)
}

@Dao
interface QueueDao {
    @Query("""
        SELECT * FROM queue_entries 
        WHERE clinicId = :clinicId 
        ORDER BY 
            CASE 
                WHEN status IN ('COMPLETED', 'CANCELLED', 'NO_SHOW') THEN 2 
                ELSE 1 
            END ASC,
            COALESCE(createdAt, scheduledAt) ASC,
            opdNumber ASC
    """)
    fun getQueue(clinicId: String): Flow<List<QueueEntryEntity>>

    @Query("""
        SELECT * FROM queue_entries 
        WHERE clinicId = :clinicId 
        ORDER BY 
            CASE 
                WHEN status IN ('COMPLETED', 'CANCELLED', 'NO_SHOW') THEN 2 
                ELSE 1 
            END ASC,
            COALESCE(createdAt, scheduledAt) ASC,
            opdNumber ASC
    """)
    suspend fun getQueueEntriesSync(clinicId: String): List<QueueEntryEntity>

    @Query("SELECT * FROM queue_entries WHERE id = :id LIMIT 1")
    suspend fun getQueueEntryById(id: String): QueueEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueue(entries: List<QueueEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueEntry(entry: QueueEntryEntity)

    @Query("DELETE FROM queue_entries WHERE id = :id")
    suspend fun deleteQueueEntry(id: String)
}

@Dao
interface OutboxDao {
    @Query("SELECT * FROM outbox_commands ORDER BY createdAt ASC")
    suspend fun getAllPending(): List<OutboxCommandEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(cmd: OutboxCommandEntity)

    @Query("DELETE FROM outbox_commands WHERE id = :id")
    suspend fun deleteCommand(id: String)

    @Query("UPDATE outbox_commands SET attempts = attempts + 1, lastError = :error WHERE id = :id")
    suspend fun recordAttemptFailure(id: String, error: String)
}
