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
}
