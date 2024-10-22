package ca.unb.mobiledev.pinder

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import ca.unb.mobiledev.pinder.databinding.FragmentReminderCreationBinding
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener

class ReminderCreationFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentReminderCreationBinding? = null
    private val binding get() = _binding!!
    // Initialize ViewModel with factory
    private val viewModel: ReminderViewModel by viewModels {
        ReminderViewModelFactory(requireActivity().application)
    }
    private var map: GoogleMap? = null
    private var selectedLocation: LatLng? = null
    private lateinit var geofenceHelper: GeofenceHelper
    private var reminderId: Long = -1L
    private lateinit var autocompleteSupportFragment: AutocompleteSupportFragment
    private val TAG = "ReminderCreation"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReminderCreationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize GeofenceHelper
        geofenceHelper = GeofenceHelper(requireContext())

        // Get reminderId from arguments
        reminderId = arguments?.getLong("reminderId", -1L) ?: -1L

        if (!Places.isInitialized()) {
            Toast.makeText(requireContext(), "Error: Places API not initialized", Toast.LENGTH_LONG).show()
            return
        }

        try {
            setupUI()
            setupAutocomplete()
            setupMap()
            loadReminderIfEditing()
        } catch (e: Exception) {
            Log.e(TAG, "Error in setup: ${e.message}")
            Toast.makeText(requireContext(), "Error setting up: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupAutocomplete() {
        val autocompleteFragment =
            childFragmentManager.findFragmentById(R.id.autocomplete_fragment) as? AutocompleteSupportFragment

        autocompleteFragment?.apply {
            setPlaceFields(listOf(
                Place.Field.ID,
                Place.Field.DISPLAY_NAME,
                Place.Field.LOCATION,
                Place.Field.FORMATTED_ADDRESS
            ))

            setOnPlaceSelectedListener(object : PlaceSelectionListener {
                override fun onPlaceSelected(place: Place) {
                    try {
                        binding.editTextAddress.setText(place.formattedAddress)
                        place.location?.let { latLng ->
                            selectedLocation = latLng
                            updateMapMarker()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error selecting place: ${e.message}")
                    }
                }

                override fun onError(status: Status) {
                    Log.e(TAG, "Error: $status")
                    Toast.makeText(context, "Error: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
                }
            })
        } ?: run {
            Log.e(TAG, "Error getting autocomplete fragment")
        }
    }

    private fun moveMapCamera(latLng: LatLng) {
        map?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(latLng, 15f)
        )
    }

    // Helper function to clear autocomplete when needed
    private fun clearAutocomplete() {
        autocompleteSupportFragment.setText("")
    }

    private fun setupUI() {
        // Initialize save button
        binding.buttonSave.setOnClickListener {
            if (validateInput()) {
                saveReminder()
            }
        }
    }

    private fun validateInput(): Boolean {
        if (binding.editTextTitle.text.toString().isEmpty()) {
            Toast.makeText(context, "Please enter a title", Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.editTextAddress.text.toString().isEmpty()) {
            Toast.makeText(context, "Please select an address", Toast.LENGTH_SHORT).show()
            return false
        }
        if (selectedLocation == null) {
            Toast.makeText(context, "Please select a location on the map", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun loadReminderIfEditing() {
        if (reminderId != -1L) {
            viewModel.getReminder(reminderId).observe(viewLifecycleOwner) { reminder ->
                if (reminder != null) {
                    binding.editTextTitle.setText(reminder.title)
                }
                if (reminder != null) {
                    binding.editTextDescription.setText(reminder.description)
                }
                if (reminder != null) {
                    binding.editTextAddress.setText(reminder.address)
                }
                if (reminder != null) {
                    binding.editTextRadius.setText(reminder.radius.toString())
                }
                if (reminder != null) {
                    selectedLocation = LatLng(reminder.latitude, reminder.longitude)
                }
                updateMapMarker()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map?.setOnMapClickListener { latLng ->
            selectedLocation = latLng
            updateMapMarker()
        }
        updateMapMarker()
    }

    private fun updateMapMarker() {
        map?.clear()
        selectedLocation?.let { location ->
            map?.addMarker(MarkerOptions().position(location))
            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        }
    }

    private fun saveReminder() {
        try {
            val title = binding.editTextTitle.text.toString()
            val description = binding.editTextDescription.text.toString()
            val address = binding.editTextAddress.text.toString()
            val radius = binding.editTextRadius.text.toString().toFloatOrNull() ?: 100f

            selectedLocation?.let { location ->
                val reminder = Reminder(
                    id = if (reminderId != -1L) reminderId else 0,
                    title = title,
                    description = description,
                    address = address,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    radius = radius
                )

                // First save the reminder
                if (reminderId != -1L) {
                    viewModel.updateReminder(reminder)
                } else {
                    viewModel.addReminder(reminder)
                }

                // Then add geofence
                geofenceHelper.addGeofence(
                    reminder,
                    onSuccessListener = {
                        Toast.makeText(context, "Reminder saved successfully", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    },
                    onFailureListener = { exception ->
                        Log.e(TAG, "Failed to add geofence: ${exception.message}")
                        Toast.makeText(context, "Failed to add geofence: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } ?: run {
                Toast.makeText(context, "Please select a location", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving reminder: ${e.message}")
            Toast.makeText(context, "Error saving reminder: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}