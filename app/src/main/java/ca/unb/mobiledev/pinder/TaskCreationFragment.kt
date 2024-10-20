package ca.unb.mobiledev.pinder

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.Geofence
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.MarkerOptions

class TaskCreationFragment : Fragment(), OnMapReadyCallback {

    private lateinit var geofenceHelper: GeofenceHelper

    private lateinit var map: GoogleMap

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_task_creation, container, false)

        // Initialize the map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Let the user select a location on the map
        map.setOnMapClickListener { latLng ->
            // Save selected location and add marker
            map.clear()
            map.addMarker(MarkerOptions().position(latLng).title("Task Location"))
            // Save the latLng for geofence
        }
    }

    private fun addTaskWithGeofence(taskName: String, lat: Double, lng: Double) {
        // Check for location permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            // Request permission if not granted
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101)
        } else {
            // Permission granted, add the geofence
            createAndAddGeofence(taskName, lat, lng)
        }
    }

    private fun createAndAddGeofence(taskName: String, lat: Double, lng: Double) {
        val geofence = Geofence.Builder()
            .setRequestId(taskName)
            .setCircularRegion(lat, lng, 100f)  // 100m radius
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        // Initialize GeofenceHelper
        geofenceHelper = GeofenceHelper(requireContext())
        val pendingIntent = createGeofencePendingIntent()

        // Call addGeofence with proper parameters
        geofenceHelper.addGeofence(geofence, pendingIntent,
            onSuccessListener = {
                Toast.makeText(requireContext(), "Geofence added successfully", Toast.LENGTH_SHORT).show()
            },
            onFailureListener = {
                Toast.makeText(requireContext(), "Failed to add geofence", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            // Check if the location permission was granted
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, call addTaskWithGeofence again with actual parameters
                addTaskWithGeofence("YourTaskName", 37.7749, -122.4194) // Example coordinates
            } else {
                // Permission denied, show a message
                Toast.makeText(requireContext(), "Location permission is required for geofencing", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Create PendingIntent for the geofence
    private fun createGeofencePendingIntent(): PendingIntent {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
