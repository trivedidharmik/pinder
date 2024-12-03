package ca.unb.mobiledev.pinder

import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import ca.unb.mobiledev.pinder.databinding.FragmentPlaceSelectionBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import java.io.IOException
import java.util.Locale
import kotlin.math.cos

class PlaceSelectionFragment : Fragment(), OnMapReadyCallback {

    companion object {
        private const val TAG = "PlaceSelectionFragment"
        private const val DEFAULT_ZOOM = 15f
        private const val DEFAULT_LAT = 45.9636  // Default to UNB coordinates
        private const val DEFAULT_LNG = -66.6431
    }

    private var _binding: FragmentPlaceSelectionBinding? = null
    private val binding get() = _binding!!
    private var map: GoogleMap? = null
    private var selectedLocation: LatLng? = null
    private var selectedAddress: String? = null
    private lateinit var permissionHelper: PermissionHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaceSelectionBinding.inflate(inflater, container, false)
        permissionHelper = PermissionHelper(this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        permissionHelper.checkAndRequestPermissions {
            setupMap()
            setupPlacesAutocomplete()
            setupButtons()
            loadExistingValues()
        }
    }

    private fun loadExistingValues() {
        arguments?.let { args ->
            // Load radius with default from preferences if not set
            val radius = args.getFloat("radius", PreferencesHelper(requireContext()).defaultRadius)
            binding.radiusInput.setText(radius.toString())

            selectedAddress = args.getString("address")
            val lat = args.getFloat("latitude").toDouble()
            val lng = args.getFloat("longitude").toDouble()

            if (lat != 0.0 && lng != 0.0) {
                selectedLocation = LatLng(lat, lng)
            }
            binding.selectedAddressText.text = selectedAddress ?: "Pick a place"
            selectedLocation?.let { updateMapMarker(it) }
        } ?: run {
            // If no arguments, set default radius from preferences
            val defaultRadius = PreferencesHelper(requireContext()).defaultRadius
            binding.radiusInput.setText(defaultRadius.toString())
        }
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap.apply {
            uiSettings.apply {
                isZoomControlsEnabled = true
                isScrollGesturesEnabled = true
                isZoomGesturesEnabled = true
            }

            setOnMapClickListener { latLng ->
                handleLocationSelection(latLng)
            }
        }

        // If we have a saved location, show it
        selectedLocation?.let {
            updateMapMarker(it)
        } ?: run {
            // If no location is selected, try to get user's location or use default
            val lastLocation = getLastKnownLocation()
            val initialLocation = if (lastLocation != null) {
                LatLng(lastLocation.latitude, lastLocation.longitude)
            } else {
                LatLng(DEFAULT_LAT, DEFAULT_LNG)
            }
            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, DEFAULT_ZOOM))
        }
    }

    private fun setupPlacesAutocomplete() {
        try {
            val autocompleteFragment = childFragmentManager
                .findFragmentById(R.id.place_autocomplete_fragment) as? AutocompleteSupportFragment
                ?: return

            val lastLocation = getLastKnownLocation()

            autocompleteFragment.apply {
                setPlaceFields(listOf(
                    Place.Field.ID,
                    Place.Field.DISPLAY_NAME,
                    Place.Field.FORMATTED_ADDRESS,
                    Place.Field.LOCATION
                ))

                setHint("Search for a location")

                // Set location bias based on user's location
                lastLocation?.let { location ->
                    val latRadian = Math.toRadians(location.latitude)
                    val degLatKm = 110.574 // km per degree of latitude
                    val degLongKm = 111.320 * cos(latRadian) // km per degree of longitude

                    val latDelta = 100.0 / degLatKm // +/- 100km in lat
                    val longDelta = 100.0 / degLongKm // +/- 100km in long

                    val bounds = RectangularBounds.newInstance(
                        LatLng(location.latitude - latDelta, location.longitude - longDelta),
                        LatLng(location.latitude + latDelta, location.longitude + longDelta)
                    )

                    setLocationBias(bounds)
                }

                setCountries("US", "CA")

                setOnPlaceSelectedListener(object : PlaceSelectionListener {
                    override fun onPlaceSelected(place: Place) {
                        Log.d(TAG, "Place selected: ${place.displayName}, ${place.formattedAddress}")
                        place.location?.let { latLng ->
                            selectedLocation = latLng
                            selectedAddress = place.formattedAddress
                            updateMapMarker(latLng)
                            binding.selectedAddressText.text = place.formattedAddress
                        }
                    }

                    override fun onError(status: com.google.android.gms.common.api.Status) {
                        Log.e(TAG, "Error selecting place: $status")
                        Toast.makeText(context, "Error selecting place", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up Places Autocomplete: ${e.message}")
            Toast.makeText(context, "Error setting up location search", Toast.LENGTH_LONG).show()
        }
    }

    private fun getLastKnownLocation(): Location? {
        val locationManager = ContextCompat.getSystemService(requireContext(), LocationManager::class.java)

        if (!permissionHelper.hasLocationPermission()) {
            Log.d(TAG, "Location permission not granted")
            return null
        }

        try {
            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }

            if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                return locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }

            return null
        } catch (e: SecurityException) {
            Log.e(TAG, "Security Exception when getting location: ${e.message}")
            return null
        }
    }

    private fun handleLocationSelection(latLng: LatLng) {
        selectedLocation = latLng
        updateMapMarker(latLng)
        updateAddressFromLocation(latLng)
    }

    private fun updateMapMarker(location: LatLng) {
        map?.apply {
            clear()
            addMarker(MarkerOptions().position(location))
            animateCamera(CameraUpdateFactory.newLatLngZoom(location, DEFAULT_ZOOM))
        }
    }

    private fun updateAddressFromLocation(latLng: LatLng) {
        try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) { addresses ->
                if (addresses.isNotEmpty()) {
                    val address = addresses[0]
                    selectedAddress = buildAddressText(address)
                    activity?.runOnUiThread {
                        binding.selectedAddressText.text = selectedAddress
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error getting address: ${e.message}")
            Toast.makeText(context, "Error getting address", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildAddressText(address: android.location.Address): String {
        return buildString {
            val streetNumber = address.subThoroughfare
            val street = address.thoroughfare
            if (!streetNumber.isNullOrEmpty() && !street.isNullOrEmpty()) {
                append("$streetNumber $street")
            } else if (!street.isNullOrEmpty()) {
                append(street)
            }

            address.locality?.let { append(", $it") }
            address.adminArea?.let { append(", $it") }
            address.postalCode?.let { append(", $it") }
            address.countryName?.let { append(", $it") }
        }
    }

    private fun setupButtons() {
        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.doneButton.setOnClickListener {
            if (validateSelection()) {
                returnSelectedPlace()
            }
        }
    }

    private fun validateSelection(): Boolean {
        if (selectedLocation == null || selectedAddress.isNullOrBlank()) {
            Toast.makeText(context, "Please select a location", Toast.LENGTH_SHORT).show()
            return false
        }

        val radius = binding.radiusInput.text.toString().toFloatOrNull()
        if (radius == null) {
            Toast.makeText(context, "Please enter a valid radius", Toast.LENGTH_SHORT).show()
            return false
        }

        if (radius < 50f) {
            Toast.makeText(context, "Radius must be at least 50 meters", Toast.LENGTH_SHORT).show()
            return false
        }

        if (radius > 10000f) {
            Toast.makeText(context, "Radius cannot exceed 10000 meters", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun returnSelectedPlace() {
        val radius = binding.radiusInput.text.toString().toFloatOrNull() ?: return
        Log.d(TAG, "Returning place data - Address: $selectedAddress, Lat: ${selectedLocation?.latitude}, Lng: ${selectedLocation?.longitude}, Radius: $radius")

        val result = Bundle().apply {
            putString("address", selectedAddress)
            putDouble("latitude", selectedLocation?.latitude ?: return)
            putDouble("longitude", selectedLocation?.longitude ?: return)
            putFloat("radius", radius)
        }

        findNavController().previousBackStackEntry?.savedStateHandle?.set("place_data", result)
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}