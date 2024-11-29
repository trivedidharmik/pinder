package ca.unb.mobiledev.pinder

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceHelper(private val context: Context) {
    companion object {
        private const val TAG = "GeofenceHelper"
        const val ACTION_GEOFENCE_RECEIVED = "ca.unb.mobiledev.pinder.ACTION_GEOFENCE_RECEIVED"
    }

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = ACTION_GEOFENCE_RECEIVED
        }
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    fun addGeofence(
        reminder: Reminder,
        onSuccessListener: () -> Unit,
        onFailureListener: (Exception) -> Unit
    ) {
        if (!checkPermissions()) {
            Log.e(TAG, "Permissions not granted for geofencing")
            onFailureListener(Exception("Location permissions not granted"))
            return
        }

        try {
            Log.d(TAG, "Adding geofence for reminder ${reminder.id} at location (${reminder.latitude}, ${reminder.longitude})")
            val geofence = buildGeofence(reminder)
            val request = buildGeofencingRequest(geofence)

            geofencingClient.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully added geofence for reminder: ${reminder.id}")
                    onSuccessListener()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to add geofence: ${e.message}", e)
                    onFailureListener(e)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when adding geofence: ${e.message}", e)
            onFailureListener(e)
        }
    }

    private fun buildGeofence(reminder: Reminder): Geofence {
        return Geofence.Builder()
            .setRequestId(reminder.id.toString())
            .setCircularRegion(
                reminder.latitude,
                reminder.longitude,
                reminder.radius
            )
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL)
            .setLoiteringDelay(30000) // 30 seconds dwell time
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .build()
    }

    private fun buildGeofencingRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL)
            .addGeofence(geofence)
            .build()
    }

    fun updateGeofence(
        reminder: Reminder,
        onSuccessListener: () -> Unit,
        onFailureListener: (Exception) -> Unit
    ) {
        removeGeofence(reminder.id.toString()) {
            addGeofence(reminder, onSuccessListener, onFailureListener)
        }
    }

    fun removeGeofence(
        geofenceId: String,
        onComplete: (() -> Unit)? = null
    ) {
        geofencingClient.removeGeofences(listOf(geofenceId))
            .addOnCompleteListener {
                Log.d(TAG, "Removed geofence: $geofenceId")
                onComplete?.invoke()
            }
    }

    private fun checkPermissions(): Boolean {
        val fineLocation = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val backgroundLocation = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return fineLocation && coarseLocation && backgroundLocation
    }
}