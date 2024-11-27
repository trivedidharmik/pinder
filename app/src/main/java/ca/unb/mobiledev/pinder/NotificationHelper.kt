package ca.unb.mobiledev.pinder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationHelper(private val context: Context) {
    private val TAG = "NotificationHelper"

    companion object {
        const val CHANNEL_ID_LOCATION = "location_reminders"
        const val CHANNEL_ID_GENERAL = "general_reminders"
        const val ACTION_MARK_DONE = "ca.unb.mobiledev.pinder.ACTION_MARK_DONE"
        const val ACTION_SNOOZE = "ca.unb.mobiledev.pinder.ACTION_SNOOZE"
        const val EXTRA_REMINDER_ID = "reminder_id"
    }

    init {
        createNotificationChannels()
    }

    fun showLocationReminderNotification(reminder: Reminder) {
        Log.d(TAG, "Preparing to show notification for reminder: ${reminder.id}")

        if (!checkNotificationPermission()) {
            Log.e(TAG, "Notification permission not granted")
            return
        }

        try {
            // Create notification channel first
            createNotificationChannels()

            // Create all the necessary intents
            val contentIntent = createContentIntent(reminder)
            val markDonePendingIntent = createMarkDoneIntent(reminder)
            val snoozePendingIntent = createSnoozeIntent(reminder)

            // Build the notification
            val notification = NotificationCompat.Builder(context, CHANNEL_ID_LOCATION)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(reminder.title)
                .setContentText(reminder.description)
                .setStyle(NotificationCompat.BigTextStyle().bigText(
                    "${reminder.description}\nLocation: ${reminder.address}"
                ))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_check, "Mark as Done", markDonePendingIntent)
                .addAction(R.drawable.ic_snooze, "Snooze", snoozePendingIntent)
                .build()

            // Show the notification
            with(NotificationManagerCompat.from(context)) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    notify(reminder.id.toInt(), notification)
                    Log.d(TAG, "Notification shown successfully for reminder: ${reminder.id}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification: ${e.message}", e)
        }
    }

    private fun createContentIntent(reminder: Reminder): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }

        return PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createMarkDoneIntent(reminder: Reminder): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_MARK_DONE
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }

        return PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createSnoozeIntent(reminder: Reminder): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }

        return PendingIntent.getBroadcast(
            context,
            reminder.id.toInt() + 1000, // Offset to make it unique
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannels() {
        val locationChannel = NotificationChannel(
            CHANNEL_ID_LOCATION,
            "Location Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for location-based reminders"
            enableVibration(true)
            enableLights(true)
        }

        val generalChannel = NotificationChannel(
            CHANNEL_ID_GENERAL,
            "General Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "General reminder notifications"
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannels(listOf(locationChannel, generalChannel))
    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}