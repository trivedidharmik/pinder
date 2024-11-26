package ca.unb.mobiledev.pinder

import android.app.AlertDialog
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import ca.unb.mobiledev.pinder.databinding.FragmentReminderCreationBinding
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import java.io.IOException
import java.util.Locale

class ReminderCreationFragment : Fragment(), OnMapReadyCallback {

    private companion object {
        const val DEFAULT_ZOOM = 15f
        const val DEFAULT_LAT = 45.9636  // Default to UNB coordinates
        const val DEFAULT_LNG = -66.6431
        const val MIN_RADIUS = 50f
        const val MAX_RADIUS = 10000f
        const val TAG = "ReminderCreation"
    }

    private var _binding: FragmentReminderCreationBinding? = null
    private val binding get() = _binding!!
    private lateinit var permissionHelper: PermissionHelper
    private lateinit var geofenceHelper: GeofenceHelper

    private val viewModel: ReminderViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }

    private var map: GoogleMap? = null
    private var selectedLocation: LatLng? = null
    private var currentZoom: Float = DEFAULT_ZOOM
    private var reminderId: Long = -1L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReminderCreationBinding.inflate(inflater, container, false)
        permissionHelper = PermissionHelper(this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeComponents()
    }

    private fun initializeComponents() {
        geofenceHelper = GeofenceHelper(requireContext())
        reminderId = arguments?.getLong("reminderId", -1L) ?: -1L

        if (!Places.isInitialized()) {
            Toast.makeText(requireContext(), "Error: Places API not initialized", Toast.LENGTH_LONG).show()
            return
        }

        permissionHelper.checkAndRequestPermissions {
            try {
                setupUI()
                setupPlacesAutocomplete()
                setupMap()
                loadReminderIfEditing()
            } catch (e: Exception) {
                Log.e(TAG, "Error in setup: ${e.message}")
                Toast.makeText(requireContext(), "Error setting up: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupUI() {
        binding.apply {
            buttonSave.setOnClickListener {
                if (validateInput()) {
                    saveReminder()
                }
            }

            buttonDelete.apply {
                visibility = if (reminderId != -1L) View.VISIBLE else View.GONE
                setOnClickListener {
                    if (reminderId != -1L) {
                        showDeleteConfirmationDialog()
                    }
                }
            }
        }
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap.apply {
            // Set up map UI settings
            uiSettings.apply {
                isZoomControlsEnabled = true
                isScrollGesturesEnabled = true
                isZoomGesturesEnabled = true
            }

            // Initialize with default location if none selected
            if (selectedLocation == null) {
                selectedLocation = LatLng(DEFAULT_LAT, DEFAULT_LNG)
                animateCameraToLocation(selectedLocation!!, DEFAULT_ZOOM)
            }

            setOnMapClickListener { latLng ->
                handleLocationSelection(latLng)
            }

            setOnCameraIdleListener {
                currentZoom = cameraPosition.zoom
            }

            setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
                override fun onMarkerDragStart(marker: Marker) {}
                override fun onMarkerDrag(marker: Marker) {}
                override fun onMarkerDragEnd(marker: Marker) {
                    handleLocationSelection(marker.position)
                }
            })
        }

        updateMapMarker()
    }

    private fun setupPlacesAutocomplete() {
        try {
            val autocompleteFragment = childFragmentManager
                .findFragmentById(R.id.autocomplete_fragment) as? AutocompleteSupportFragment
                ?: return

            autocompleteFragment.apply {
                setPlaceFields(listOf(
                    Place.Field.ID,
                    Place.Field.DISPLAY_NAME,
                    Place.Field.FORMATTED_ADDRESS,
                    Place.Field.LOCATION,
                    Place.Field.VIEWPORT
                ))

                setHint("Search for a location")
                setCountries("US", "CA")

                setOnPlaceSelectedListener(object : PlaceSelectionListener {
                    override fun onPlaceSelected(place: Place) {
                        Log.d(TAG, "Place selected: ${place.displayName}, ${place.formattedAddress}")
                        place.location?.let { location ->
                            selectedLocation = location
                            binding.editTextAddress.setText(place.formattedAddress)
                            animateCameraToLocation(location, DEFAULT_ZOOM)
                            updateMapMarker()
                        }
                    }

                    override fun onError(status: Status) {
                        Log.e(TAG, "An error occurred: $status")
                        Toast.makeText(
                            context,
                            "Error selecting place: ${status.statusMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up Places Autocomplete: ${e.message}")
            Toast.makeText(
                context,
                "Error setting up location search: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun handleLocationSelection(latLng: LatLng) {
        selectedLocation = latLng
        updateMapMarker()
        updateAddressFromLocation(latLng)
    }

    private fun animateCameraToLocation(location: LatLng, zoom: Float? = null) {
        map?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                location,
                zoom ?: currentZoom
            )
        )
    }

    private fun updateMapMarker() {
        map?.clear()
        selectedLocation?.let { location ->
            map?.addMarker(
                MarkerOptions()
                    .position(location)
                    .draggable(true)
                    .title("Selected Location")
            )
            animateCameraToLocation(location)
        }
    }

    private fun updateAddressFromLocation(latLng: LatLng) {
        try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) { addresses ->
                    handleGeocodeResult(addresses)
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                handleGeocodeResult(addresses ?: emptyList())
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error getting address: ${e.message}")
            activity?.runOnUiThread {
                Toast.makeText(context, "Error getting address: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleGeocodeResult(addresses: List<android.location.Address>) {
        if (addresses.isNotEmpty()) {
            val address = addresses[0]
            val addressText = buildAddressText(address)

            activity?.runOnUiThread {
                binding.editTextAddress.setText(addressText)
                childFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                    ?.let { it as? AutocompleteSupportFragment }
                    ?.setText(addressText)
            }
        } else {
            activity?.runOnUiThread {
                Toast.makeText(context, "Could not find address for this location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buildAddressText(address: android.location.Address): String {
        return buildString {
            // Street address
            val streetNumber = address.subThoroughfare
            val street = address.thoroughfare
            if (!streetNumber.isNullOrEmpty() && !street.isNullOrEmpty()) {
                append("$streetNumber $street")
            } else if (!street.isNullOrEmpty()) {
                append(street)
            }

            // City
            address.locality?.let { append(", $it") }

            // State/Province
            address.adminArea?.let { append(", $it") }

            // Postal code
            address.postalCode?.let { append(", $it") }

            // Country
            address.countryName?.let { append(", $it") }
        }
    }

    private fun validateInput(): Boolean {
        val title = binding.editTextTitle.text.toString()
        val description = binding.editTextDescription.text.toString()
        val address = binding.editTextAddress.text.toString()
        val radius = binding.editTextRadius.text.toString().toFloatOrNull() ?: 100f

        return when {
            title.isBlank() -> {
                Toast.makeText(context, "Please enter a title", Toast.LENGTH_SHORT).show()
                false
            }
            description.isBlank() -> {
                Toast.makeText(context, "Please enter a description", Toast.LENGTH_SHORT).show()
                false
            }
            address.isBlank() -> {
                Toast.makeText(context, "Please select a location", Toast.LENGTH_SHORT).show()
                false
            }
            selectedLocation == null -> {
                Toast.makeText(context, "Please select a location on the map", Toast.LENGTH_SHORT).show()
                false
            }
            radius < MIN_RADIUS -> {
                Toast.makeText(context, "Radius must be at least $MIN_RADIUS meters", Toast.LENGTH_SHORT).show()
                false
            }
            radius > MAX_RADIUS -> {
                Toast.makeText(context, "Radius cannot exceed $MAX_RADIUS meters", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun saveReminder() {
        permissionHelper.checkAndRequestPermissions {
            try {
                val reminder = createReminderFromInput()
                if (reminderId != -1L) {
                    updateExistingReminder(reminder)
                } else {
                    addNewReminder(reminder)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving reminder: ${e.message}")
                Toast.makeText(context, "Error saving reminder: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createReminderFromInput(): Reminder {
        val title = binding.editTextTitle.text.toString()
        val description = binding.editTextDescription.text.toString()
        val address = binding.editTextAddress.text.toString()
        val radius = binding.editTextRadius.text.toString().toFloatOrNull() ?: 100f
        val location = selectedLocation ?: throw IllegalStateException("Location not selected")

        return Reminder(
            id = if (reminderId != -1L) reminderId else 0,
            title = title,
            description = description,
            address = address,
            latitude = location.latitude,
            longitude = location.longitude,
            radius = radius
        )
    }

    private fun addNewReminder(reminder: Reminder) {
        viewModel.addReminder(reminder,
            onSuccess = {
                Toast.makeText(context, "Reminder saved", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            },
            onError = { error ->
                Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun updateExistingReminder(reminder: Reminder) {
        viewModel.updateReminder(reminder,
            onSuccess = {
                Toast.makeText(context, "Reminder updated", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            },
            onError = { error ->
                Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Reminder")
            .setMessage("Are you sure you want to delete this reminder?")
            .setPositiveButton("Delete") { _, _ -> deleteReminder() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteReminder() {
        viewModel.deleteReminder(
            reminderId,
            onSuccess = {
                Toast.makeText(context, "Reminder deleted", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            },
            onError = { error ->
                Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun loadReminderIfEditing() {
        if (reminderId != -1L) {
            viewModel.getReminder(reminderId).observe(viewLifecycleOwner) { reminder ->
                reminder?.let {
                    binding.apply {
                        editTextTitle.setText(it.title)
                        editTextDescription.setText(it.description)
                        editTextAddress.setText(it.address)
                        editTextRadius.setText(it.radius.toString())
                    }
                    selectedLocation = LatLng(it.latitude, it.longitude)
                    updateMapMarker()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}