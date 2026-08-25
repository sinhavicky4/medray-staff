package ai.medray.staff.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PatientEntity::class,
        QueueEntryEntity::class,
        OutboxCommandEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class StaffDatabase : RoomDatabase() {

    abstract fun patientDao(): PatientDao
    abstract fun queueDao(): QueueDao
    abstract fun outboxDao(): OutboxDao

    companion object {
        @Volatile
        private var INSTANCE: StaffDatabase? = null

        fun getDatabase(context: Context): StaffDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StaffDatabase::class.java,
                    "medray_staff_local.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
