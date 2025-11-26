package com.vovaplusexp.gpscontroller.utils

import android.content.Context
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    fun isMockLocationEnabled(): Boolean {
        return prefs.getBoolean("mock_location_enabled", false)
    }

    fun isPeerEnabled(): Boolean {
        return prefs.getBoolean("peer_enabled", false)
    }

    var isSetupComplete: Boolean
        get() = prefs.getBoolean("setup_complete", false)
        set(value) = prefs.edit().putBoolean("setup_complete", value).apply()

    var isMapDownloaded: Boolean
        get() = prefs.getBoolean("map_downloaded", false)
        set(value) = prefs.edit().putBoolean("map_downloaded", value).apply()
}
