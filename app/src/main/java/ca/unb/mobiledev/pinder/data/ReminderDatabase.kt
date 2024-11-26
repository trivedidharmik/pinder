package ca.unb.mobiledev.pinder.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ReminderEntity::class], version = 2) // Update version to 2
abstract class ReminderDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: ReminderDatabase? = null

        // Migration from version 1 to 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns to the reminders table
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

        fun getDatabase(context: Context): ReminderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReminderDatabase::class.java,
                    "reminder_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}