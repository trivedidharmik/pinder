package ca.unb.mobiledev.pinder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ca.unb.mobiledev.pinder.data.ReminderDatabase
import ca.unb.mobiledev.pinder.data.ReminderMapper
import com.google.android.gms.location.GeofenceStatusCodes

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    private val TAG = "GeofenceBroadcast"
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "GeofenceBroadcastReceiver triggered")

        if (context == null || intent == null) {
            Log.e(TAG, "Null context or intent")
            return
        }

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.e(TAG, "Null GeofencingEvent")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "Geofencing error: $errorMessage")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        Log.d(TAG, "Geofence transition type: $geofenceTransition")

        when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER,
            Geofence.GEOFENCE_TRANSITION_DWELL -> {
                Log.d(TAG, "Geofence ENTER/DWELL detected")
                handleGeofenceEnter(context, geofencingEvent)
            }
            else -> Log.d(TAG, "Unhandled geofence transition: $geofenceTransition")
        }
    }

    private fun handleGeofenceEnter(context: Context, geofencingEvent: GeofencingEvent) {
        val triggeringGeofences = geofencingEvent.triggeringGeofences
        if (triggeringGeofences.isNullOrEmpty()) {
            Log.e(TAG, "No triggering geofences found")
            return
        }

        val triggeringLocation = geofencingEvent.triggeringLocation
        Log.d(TAG, "Triggering location: lat=${triggeringLocation?.latitude}, lng=${triggeringLocation?.longitude}")

        triggeringGeofences.forEach { geofence ->
            Log.d(TAG, "Processing geofence ID: ${geofence.requestId}")
            handleGeofenceTransition(context, geofence.requestId)
        }
    }

    private fun handleGeofenceTransition(context: Context, reminderId: String) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Fetching reminder with ID: $reminderId")
                val database = ReminderDatabase.getDatabase(context)
                val reminder = database.reminderDao().getReminderById(reminderId.toLong())

                if (reminder == null) {
                    Log.e(TAG, "Reminder not found for ID: $reminderId")
                    return@launch
                }

                Log.d(TAG, "Found reminder: ${reminder.title} with status ${reminder.status}")

                if (reminder.status == "PENDING") {
                    Log.d(TAG, "Showing notification for reminder: ${reminder.title}")
                    val notificationHelper = NotificationHelper(context)
                    val reminderModel = ReminderMapper.fromEntity(reminder)
                    notificationHelper.showLocationReminderNotification(reminderModel)
                } else {
                    Log.d(TAG, "Skipping notification - reminder status is: ${reminder.status}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling geofence transition: ${e.message}", e)
            }
        }
    }
}
