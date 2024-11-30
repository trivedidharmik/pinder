package ca.unb.mobiledev.pinder

import android.content.Context
import android.content.SharedPreferences

class PreferencesHelper(context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREF_NAME = "pinder_preferences"
        private const val KEY_DEFAULT_RADIUS = "default_radius"
        private const val KEY_DEFAULT_SNOOZE_DURATION = "default_snooze_duration"

        // Default values
        const val DEFAULT_RADIUS = 100f
        const val DEFAULT_SNOOZE_DURATION = 60 // minutes
    }

    var defaultRadius: Float
        get() = preferences.getFloat(KEY_DEFAULT_RADIUS, DEFAULT_RADIUS)
        set(value) = preferences.edit().putFloat(KEY_DEFAULT_RADIUS, value).apply()

    var defaultSnoozeDuration: Int
        get() = preferences.getInt(KEY_DEFAULT_SNOOZE_DURATION, DEFAULT_SNOOZE_DURATION)
        set(value) = preferences.edit().putInt(KEY_DEFAULT_SNOOZE_DURATION, value).apply()
}