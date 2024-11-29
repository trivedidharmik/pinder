package ca.unb.mobiledev.pinder

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ca.unb.mobiledev.pinder.data.ReminderDatabase
import ca.unb.mobiledev.pinder.data.ReminderMapper

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "NotificationWorker"
    }

    override suspend fun doWork(): Result {
        val reminderId = inputData.getLong("reminderId", -1)
        val isSnooze = inputData.getBoolean("isSnooze", false)

        if (reminderId == -1L) {
            Log.e(TAG, "Invalid reminder ID")
            return Result.failure()
        }

        try {
            val database = ReminderDatabase.getDatabase(applicationContext)
            val reminderEntity = database.reminderDao().getReminderById(reminderId)

            reminderEntity?.let { entity ->
                val reminder = ReminderMapper.fromEntity(entity)
                if (reminder.status == ReminderStatus.PENDING) {
                    NotificationHelper(applicationContext).apply {
                        if (isSnooze) {
                            showSnoozedReminderNotification(reminder)
                        } else {
                            showLocationReminderNotification(reminder)
                        }
                    }
                    return Result.success()
                }
            }

            return Result.failure()
        } catch (e: Exception) {
            Log.e(TAG, "Error in NotificationWorker: ${e.message}", e)
            return Result.retry()
        }
    }
}