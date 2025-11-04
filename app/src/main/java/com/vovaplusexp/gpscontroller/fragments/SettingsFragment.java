/*
 * GPS-Controller - Inertial Navigation with GPS Spoofing Detection
 * Copyright (C) 2024 VovaplusEXP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.vovaplusexp.gpscontroller.fragments;

import androidx.preference.PreferenceFragmentCompat;

/**
 * Fragment for app settings
 */
public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(android.os.Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(com.vovaplusexp.gpscontroller.R.xml.preferences, rootKey);
    }
}
