package ca.unb.mobiledev.pinder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import ca.unb.mobiledev.pinder.data.ReminderDatabase
import ca.unb.mobiledev.pinder.data.ReminderMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class NotificationActionReceiver : BroadcastReceiver() {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val reminderId = intent.getLongExtra(NotificationHelper.EXTRA_REMINDER_ID, -1)
        if (reminderId == -1L) return

        when (intent.action) {
            NotificationHelper.ACTION_MARK_DONE -> handleMarkDone(context, reminderId)
            NotificationHelper.ACTION_SNOOZE -> handleSnooze(context, reminderId)
        }
    }

    private fun handleMarkDone(context: Context, reminderId: Long) {
        coroutineScope.launch {
            val database = ReminderDatabase.getDatabase(context)
            database.reminderDao().updateReminderStatus(
                reminderId,
                ReminderStatus.COMPLETED.name,
                System.currentTimeMillis()
            )

            // Remove the notification
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(reminderId.toInt())

            // Remove the geofence
            GeofenceHelper(context).removeGeofence(reminderId.toString())
        }
    }

    private fun handleSnooze(context: Context, reminderId: Long) {
        // Remove the current notification
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(reminderId.toInt())

        // Schedule a new notification for later (e.g., 1 hour later)
        coroutineScope.launch {
            val database = ReminderDatabase.getDatabase(context)
            val reminderEntity = database.reminderDao().getReminderById(reminderId)
            reminderEntity?.let { entity ->
                val reminder = ReminderMapper.fromEntity(entity)
                // Schedule a delayed notification
                scheduleDelayedNotification(context, reminder)
            }
        }
    }

    private fun scheduleDelayedNotification(context: Context, reminder: Reminder) {
        val notificationHelper = NotificationHelper(context)
        // Schedule the notification after 1 hour
        val workManager = WorkManager.getInstance(context)

        val notificationWork = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(1, TimeUnit.HOURS)
            .setInputData(workDataOf(
                "reminderId" to reminder.id
            ))
            .build()

        workManager.enqueue(notificationWork)
    }
}