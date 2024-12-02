package ca.unb.mobiledev.pinder

import android.app.AlertDialog
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

class ReminderCreationFragment : Fragment() {

    private companion object {
        const val MIN_RADIUS = 50f
        const val MAX_RADIUS = 10000f
        const val TAG = "ReminderCreation"
        const val DEFAULT_LAT = 45.9636f
        const val DEFAULT_LNG = -66.6431f
        private const val PENDING_LOCATION_KEY = "pending_location_update"
    }

    private var _binding: FragmentReminderCreationBinding? = null
    private val binding get() = _binding!!
    private lateinit var permissionHelper: PermissionHelper
    private lateinit var geofenceHelper: GeofenceHelper
    private var pendingLocationUpdate: Bundle? = null

    private val viewModel: ReminderViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }

    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null
    private var selectedRadius: Float? = null
    private var reminderId: Long = -1L
    private var selectedGeofenceType: GeofenceType = GeofenceType.ARRIVE_AT

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
        savedInstanceState?.let {
            // Restore pending location update if exists
            pendingLocationUpdate = it.getBundle(PENDING_LOCATION_KEY)
        }
        initializeComponents()
    }

    private fun initializeComponents() {
        geofenceHelper = GeofenceHelper(requireContext())
        reminderId = arguments?.getLong("reminderId", -1L) ?: -1L

        permissionHelper.checkAndRequestPermissions {
            try {
                setupUI()
                setupLocationSelection()
                observePlaceSelection()
                if (reminderId == -1L) {
                    loadDefaultValues()
                } else {
                    loadReminderIfEditing()
                }
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
            // Setup geofence type selection
            toggleGeofenceType.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    selectedGeofenceType = when (checkedId) {
                        R.id.buttonArriveAt -> GeofenceType.ARRIVE_AT
                        R.id.buttonLeaveAt -> GeofenceType.LEAVE_AT
                        else -> GeofenceType.ARRIVE_AT
                    }
                }
            }
            buttonArriveAt.isChecked = true
        }
    }

    private fun setupLocationSelection() {
        binding.locationCard.setOnClickListener {
            Log.d(TAG, "Location card clicked - Current values: " +
                    "Address: ${binding.textViewSelectedAddress.text}, " +
                    "Lat: $selectedLatitude, Lng: $selectedLongitude, Radius: $selectedRadius")

            try {
                val bundle = Bundle().apply {
                    putString("address", binding.textViewSelectedAddress.text.toString())
                    putFloat("radius", selectedRadius ?: PreferencesHelper(requireContext()).defaultRadius)
                    putFloat("latitude", selectedLatitude?.toFloat() ?: DEFAULT_LAT.toFloat())
                    putFloat("longitude", selectedLongitude?.toFloat() ?: DEFAULT_LNG.toFloat())
                }

                findNavController().navigate(
                    R.id.action_reminderCreationFragment_to_placeSelectionFragment,
                    bundle
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error navigating to place selection: ${e.message}", e)
                Toast.makeText(context, "Error opening location selection: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observePlaceSelection() {
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Bundle>("place_data")?.observe(
            viewLifecycleOwner) { result ->
            result?.let {
                pendingLocationUpdate = it  // Store the pending update
                updateLocationUI(it)  // Update UI with new location
            }
        }
    }

    private fun updateLocationUI(locationData: Bundle) {
        selectedLatitude = locationData.getDouble("latitude")
        selectedLongitude = locationData.getDouble("longitude")
        selectedRadius = locationData.getFloat("radius")

        binding.apply {
            textViewSelectedAddress.text = locationData.getString("address")
            textViewRadius.apply {
                visibility = View.VISIBLE
                text = "Radius: ${selectedRadius?.toInt()} meters"
            }
            geofenceTypeContainer.visibility = View.VISIBLE
        }
    }

    private fun loadReminderIfEditing() {
        if (reminderId != -1L) {
            viewModel.getReminder(reminderId).observe(viewLifecycleOwner) { reminder ->
                reminder?.let {
                    // Only load location data if there's no pending update
                    if (pendingLocationUpdate == null) {
                        binding.apply {
                            editTextTitle.setText(it.title)
                            editTextDescription.setText(it.description)
                            textViewSelectedAddress.text = it.address
                            textViewRadius.apply {
                                visibility = View.VISIBLE
                                text = "Radius: ${it.radius.toInt()} meters"
                            }
                            // Show geofence type container and set the correct selection
                            geofenceTypeContainer.visibility = View.VISIBLE
                            when (it.geofenceType) {
                                GeofenceType.ARRIVE_AT -> buttonArriveAt.isChecked = true
                                GeofenceType.LEAVE_AT -> buttonLeaveAt.isChecked = true
                            }
                        }
                        selectedLatitude = it.latitude
                        selectedLongitude = it.longitude
                        selectedRadius = it.radius
                        selectedGeofenceType = it.geofenceType  // Save the geofence type
                    } else {
                        // Load non-location data only
                        binding.apply {
                            editTextTitle.setText(it.title)
                            editTextDescription.setText(it.description)
                            // Show geofence type container and set the correct selection
                            geofenceTypeContainer.visibility = View.VISIBLE
                            when (it.geofenceType) {
                                GeofenceType.ARRIVE_AT -> buttonArriveAt.isChecked = true
                                GeofenceType.LEAVE_AT -> buttonLeaveAt.isChecked = true
                            }
                        }
                        selectedGeofenceType = it.geofenceType  // Save the geofence type
                        // Restore pending location update
                        updateLocationUI(pendingLocationUpdate!!)
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pendingLocationUpdate?.let {
            outState.putBundle(PENDING_LOCATION_KEY, it)
        }
    }

    private fun loadDefaultValues() {
        val preferencesHelper = PreferencesHelper(requireContext())
        selectedRadius = preferencesHelper.defaultRadius
        binding.textViewSelectedAddress.text = "Pick a Place"
    }

    private fun validateInput(): Boolean {
        val title = binding.editTextTitle.text.toString()
        val description = binding.editTextDescription.text.toString()

        return when {
            title.isBlank() -> {
                showError("Please enter a title")
                false
            }
            description.isBlank() -> {
                showError("Please enter a description")
                false
            }
            selectedLatitude == null || selectedLongitude == null -> {
                showError("Please select a location")
                false
            }
            selectedRadius == null -> {
                showError("Please set a radius")
                false
            }
            selectedRadius!! < MIN_RADIUS -> {
                showError("Radius must be at least $MIN_RADIUS meters")
                false
            }
            selectedRadius!! > MAX_RADIUS -> {
                showError("Radius cannot exceed $MAX_RADIUS meters")
                false
            }
            else -> true
        }
    }

    private fun createReminderFromInput(): Reminder {
        return Reminder(
            id = if (reminderId != -1L) reminderId else 0,
            title = binding.editTextTitle.text.toString(),
            description = binding.editTextDescription.text.toString(),
            address = binding.textViewSelectedAddress.text.toString(),
            latitude = selectedLatitude!!,
            longitude = selectedLongitude!!,
            radius = selectedRadius!!,
            geofenceType = selectedGeofenceType
        )
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
                showError("Error saving reminder: ${e.message}")
            }
        }
    }

    private fun addNewReminder(reminder: Reminder) {
        viewModel.addReminder(reminder,
            onSuccess = {
                Toast.makeText(context, "Reminder saved", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            },
            onError = { error ->
                showError("Error: $error")
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
                showError("Error: $error")
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
                showError("Error: $error")
            }
        )
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}