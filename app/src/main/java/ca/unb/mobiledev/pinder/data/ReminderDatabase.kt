package ca.unb.mobiledev.pinder.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ReminderEntity::class], version = 3)
abstract class ReminderDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: ReminderDatabase? = null

        // Previous migrations remain the same
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    ALTER TABLE reminders 
                    ADD COLUMN status TEXT NOT NULL DEFAULT 'PENDING'
                """)

                db.execSQL("""
                    ALTER TABLE reminders 
                    ADD COLUMN priority TEXT NOT NULL DEFAULT 'MEDIUM'
                """)

                db.execSQL("""
                    ALTER TABLE reminders 
                    ADD COLUMN createdAt INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}
                """)

                db.execSQL("""
                    ALTER TABLE reminders 
                    ADD COLUMN completedAt INTEGER
                """)
            }
        }

        // New migration from version 2 to 3
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    ALTER TABLE reminders 
                    ADD COLUMN geofenceType TEXT NOT NULL DEFAULT 'ARRIVE_AT'
                """)
            }
        }

        fun getDatabase(context: Context): ReminderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReminderDatabase::class.java,
                    "reminder_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}