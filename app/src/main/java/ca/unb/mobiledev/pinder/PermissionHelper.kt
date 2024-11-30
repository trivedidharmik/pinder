package ca.unb.mobiledev.pinder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class PermissionHelper(private val fragment: Fragment) {

    private val requiredPermissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    private val permissionLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            onPermissionsGranted()
        } else {
            showPermissionSettingsDialog()
        }
    }

    fun checkAndRequestPermissions(onGranted: () -> Unit) {
        this.onPermissionsGranted = onGranted

        val notGrantedPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(fragment.requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        when {
            notGrantedPermissions.isEmpty() -> onGranted()
            shouldShowRationale(notGrantedPermissions) -> showPermissionRationaleDialog(notGrantedPermissions)
            else -> permissionLauncher.launch(notGrantedPermissions.toTypedArray())
        }
    }

    private lateinit var onPermissionsGranted: () -> Unit

    private fun shouldShowRationale(permissions: List<String>): Boolean {
        return permissions.any { permission ->
            fragment.shouldShowRequestPermissionRationale(permission)
        }
    }

    private fun showPermissionRationaleDialog(permissions: List<String>) {
        androidx.appcompat.app.AlertDialog.Builder(fragment.requireContext())
            .setTitle("Permissions Required")
            .setMessage("This app needs location and notification permissions to alert you when you're near your reminders. " +
                    "Please grant these permissions to use the app's features.")
            .setPositiveButton("Grant") { _, _ ->
                permissionLauncher.launch(permissions.toTypedArray())
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showPermissionSettingsDialog() {
        androidx.appcompat.app.AlertDialog.Builder(fragment.requireContext())
            .setTitle("Permissions Required")
            .setMessage("Some permissions are permanently denied. Please enable them in Settings to use all features.")
            .setPositiveButton("Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun openAppSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", fragment.requireContext().packageName, null)
            fragment.startActivity(this)
        }
    }
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            fragment.requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}