// SettingsFragment.kt
package ca.unb.mobiledev.pinder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import ca.unb.mobiledev.pinder.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferencesHelper: PreferencesHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        preferencesHelper = PreferencesHelper(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadCurrentSettings()
        setupSaveButton()
    }

    private fun loadCurrentSettings() {
        binding.apply {
            editTextDefaultRadius.setText(preferencesHelper.defaultRadius.toString())
            editTextDefaultSnoozeDuration.setText(preferencesHelper.defaultSnoozeDuration.toString())
        }
    }

    private fun setupSaveButton() {
        binding.buttonSaveSettings.setOnClickListener {
            if (validateInput()) {
                saveSettings()
            }
        }
    }

    private fun validateInput(): Boolean {
        val radius = binding.editTextDefaultRadius.text.toString().toFloatOrNull()
        val snoozeDuration = binding.editTextDefaultSnoozeDuration.text.toString().toIntOrNull()

        return when {
            radius == null || radius < 50f -> {
                showError("Radius must be at least 50 meters")
                false
            }
            radius > 10000f -> {
                showError("Radius cannot exceed 10000 meters")
                false
            }
            snoozeDuration == null || snoozeDuration < 1 -> {
                showError("Snooze duration must be at least 1 minute")
                false
            }
            snoozeDuration > 1440 -> { // 24 hours in minutes
                showError("Snooze duration cannot exceed 24 hours")
                false
            }
            else -> true
        }
    }

    private fun saveSettings() {
        val radius = binding.editTextDefaultRadius.text.toString().toFloat()
        val snoozeDuration = binding.editTextDefaultSnoozeDuration.text.toString().toInt()

        preferencesHelper.apply {
            defaultRadius = radius
            defaultSnoozeDuration = snoozeDuration
        }

        Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}