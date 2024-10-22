package ca.unb.mobiledev.pinder

import android.app.Application
import android.util.Log
import com.google.android.libraries.places.api.Places

class PinderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializePlaces()
    }

    private fun initializePlaces() {
        try {
            if (!Places.isInitialized()) {
                Places.initialize(applicationContext, getString(R.string.google_maps_key))
                Log.d("PinderApplication", "Places API initialized successfully")
            }
        } catch (e: Exception) {
            Log.e("PinderApplication", "Error initializing Places API: ${e.message}")
        }
    }
}