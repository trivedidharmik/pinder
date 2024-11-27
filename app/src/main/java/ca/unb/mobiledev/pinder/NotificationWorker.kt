package ca.unb.mobiledev.pinder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ca.unb.mobiledev.pinder.data.ReminderDatabase
import ca.unb.mobiledev.pinder.data.ReminderMapper

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val reminderId = inputData.getLong("reminderId", -1)
        if (reminderId == -1L) return Result.failure()

        val database = ReminderDatabase.getDatabase(applicationContext)
        val reminderEntity = database.reminderDao().getReminderById(reminderId)

        reminderEntity?.let { entity ->
            val reminder = ReminderMapper.fromEntity(entity)
            if (reminder.status == ReminderStatus.PENDING) {
                NotificationHelper(applicationContext)
                    .showLocationReminderNotification(reminder)
            }
        }

        return Result.success()
    }
}