/*
 * GPS-Controller - Inertial Navigation with GPS Spoofing Detection
 * Copyright (C) 2024 VovaplusEXP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.vovaplusexp.gpscontroller.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages app preferences and settings
 */
public class PreferencesManager {
    private static final String PREFS_NAME = "GPSControllerPrefs";
    
    private static final String KEY_SETUP_COMPLETE = "setup_complete";
    private static final String KEY_MAP_DOWNLOADED = "map_downloaded";
    private static final String KEY_CALIBRATION_DONE = "calibration_done";
    private static final String KEY_PEER_ENABLED = "peer_enabled";
    private static final String KEY_MOCK_LOCATION_ENABLED = "mock_location_enabled";
    private static final String KEY_SPOOFING_SENSITIVITY = "spoofing_sensitivity";
    
    private final SharedPreferences prefs;
    
    public PreferencesManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public boolean isSetupComplete() {
        return prefs.getBoolean(KEY_SETUP_COMPLETE, false);
    }
    
    public void setSetupComplete(boolean complete) {
        prefs.edit().putBoolean(KEY_SETUP_COMPLETE, complete).apply();
    }
    
    public boolean isMapDownloaded() {
        return prefs.getBoolean(KEY_MAP_DOWNLOADED, false);
    }
    
    public void setMapDownloaded(boolean downloaded) {
        prefs.edit().putBoolean(KEY_MAP_DOWNLOADED, downloaded).apply();
    }
    
    public boolean isCalibrationDone() {
        return prefs.getBoolean(KEY_CALIBRATION_DONE, false);
    }
    
    public void setCalibrationDone(boolean done) {
        prefs.edit().putBoolean(KEY_CALIBRATION_DONE, done).apply();
    }
    
    public boolean isPeerEnabled() {
        return prefs.getBoolean(KEY_PEER_ENABLED, false);
    }
    
    public void setPeerEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_PEER_ENABLED, enabled).apply();
    }
    
    public boolean isMockLocationEnabled() {
        return prefs.getBoolean(KEY_MOCK_LOCATION_ENABLED, true);
    }
    
    public void setMockLocationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_MOCK_LOCATION_ENABLED, enabled).apply();
    }
    
    public float getSpoofingSensitivity() {
        return prefs.getFloat(KEY_SPOOFING_SENSITIVITY, 0.5f);
    }
    
    public void setSpoofingSensitivity(float sensitivity) {
        prefs.edit().putFloat(KEY_SPOOFING_SENSITIVITY, sensitivity).apply();
    }
}
