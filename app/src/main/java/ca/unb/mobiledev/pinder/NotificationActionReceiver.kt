package ca.unb.mobiledev.pinder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import ca.unb.mobiledev.pinder.data.ReminderDatabase
import ca.unb.mobiledev.pinder.data.ReminderMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class NotificationActionReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "NotificationReceiver"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Log.e(TAG, "Null context or intent")
            return
        }

        Log.d(TAG, "Received action: ${intent.action}")
        val reminderId = intent.getLongExtra(NotificationHelper.EXTRA_REMINDER_ID, -1)
        Log.d(TAG, "For reminder ID: $reminderId")

        if (reminderId == -1L) {
            Log.e(TAG, "Invalid reminder ID")
            return
        }

        when (intent.action) {
            NotificationHelper.ACTION_MARK_DONE -> {
                Log.d(TAG, "Handling mark done action")
                handleMarkDone(context, reminderId)
            }
            NotificationHelper.ACTION_SNOOZE -> {
                Log.d(TAG, "Handling snooze action")
                handleSnooze(context, reminderId)
            }
        }
    }

    private fun handleMarkDone(context: Context, reminderId: Long) {
        coroutineScope.launch {
            try {
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

                // Show completion toast
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        "Reminder marked as completed",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                Log.d(TAG, "Successfully marked reminder $reminderId as done")
            } catch (e: Exception) {
                Log.e(TAG, "Error marking reminder as done: ${e.message}")
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        "Error completing reminder",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun handleSnooze(context: Context, reminderId: Long) {
        // Remove the current notification
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(reminderId.toInt())

        coroutineScope.launch {
            try {
                val database = ReminderDatabase.getDatabase(context)
                val reminderEntity = database.reminderDao().getReminderById(reminderId)
                reminderEntity?.let { entity ->
                    val reminder = ReminderMapper.fromEntity(entity)
                    scheduleDelayedNotification(context, reminder)

                    // Show snooze confirmation toast
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            context,
                            "Reminder snoozed for 1 hour",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    Log.d(TAG, "Successfully snoozed reminder $reminderId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error snoozing reminder: ${e.message}")
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        "Error snoozing reminder",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun scheduleDelayedNotification(context: Context, reminder: Reminder) {
        try {
            Log.d(TAG, "Scheduling delayed notification for reminder: ${reminder.id}")

            val workManager = WorkManager.getInstance(context)
            val preferencesHelper = PreferencesHelper(context)
            val snoozeDurationMinutes = preferencesHelper.defaultSnoozeDuration.toLong()

            // Create unique work name using reminder ID
            val workName = "snooze_reminder_${reminder.id}"

            // Cancel any existing snoozed notifications for this reminder
            workManager.cancelUniqueWork(workName)

            val notificationWork = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(snoozeDurationMinutes, TimeUnit.MINUTES)
                .setInputData(workDataOf(
                    "reminderId" to reminder.id,
                    "isSnooze" to true
                ))
                .build()

            workManager.enqueueUniqueWork(
                workName,
                ExistingWorkPolicy.REPLACE,
                notificationWork
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling delayed notification: ${e.message}", e)
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    context,
                    "Failed to schedule snoozed reminder",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}