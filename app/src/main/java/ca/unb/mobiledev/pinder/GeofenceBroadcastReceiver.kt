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
                handleGeofenceTransition(context, geofencingEvent, GeofenceType.ARRIVE_AT)
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                Log.d(TAG, "Geofence EXIT detected")
                handleGeofenceTransition(context, geofencingEvent, GeofenceType.LEAVE_AT)
            }
            else -> Log.d(TAG, "Unhandled geofence transition: $geofenceTransition")
        }
    }

    private fun handleGeofenceTransition(context: Context, geofencingEvent: GeofencingEvent, type: GeofenceType) {
        val triggeringGeofences = geofencingEvent.triggeringGeofences
        if (triggeringGeofences.isNullOrEmpty()) {
            Log.e(TAG, "No triggering geofences found")
            return
        }

        triggeringGeofences.forEach { geofence ->
            coroutineScope.launch {
                try {
                    val database = ReminderDatabase.getDatabase(context)
                    val reminder = database.reminderDao().getReminderById(geofence.requestId.toLong())

                    if (reminder?.let { ReminderMapper.fromEntity(it).geofenceType } == type) {
                        Log.d(TAG, "Processing geofence ID: ${geofence.requestId}")
                        val notificationHelper = NotificationHelper(context)
                        val reminderModel = reminder.let { ReminderMapper.fromEntity(it) }
                        notificationHelper.showLocationReminderNotification(reminderModel)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling geofence transition: ${e.message}", e)
                }
            }
        }
    }
}
