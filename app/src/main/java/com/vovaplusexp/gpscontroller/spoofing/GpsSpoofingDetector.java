/*
 * GPS-Controller - Inertial Navigation with GPS Spoofing Detection
 * Copyright (C) 2024 VovaplusEXP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.vovaplusexp.gpscontroller.spoofing;

import com.vovaplusexp.gpscontroller.models.Location;
import com.vovaplusexp.gpscontroller.utils.MathUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects GPS spoofing attacks
 */
public class GpsSpoofingDetector {
    
    private static final float MAX_REALISTIC_SPEED = 300.0f;  // m/s (~1080 km/h)
    private static final float SPEED_DIFF_THRESHOLD = 10.0f;  // m/s
    private static final float BEARING_DIFF_THRESHOLD = 45.0f; // degrees
    
    private Location lastGpsLocation = null;
    private float lastImuSpeed = 0;
    private float lastImuBearing = 0;
    
    private List<SpoofingFlag> currentFlags = new ArrayList<>();
    
    public enum SpoofingFlag {
        TELEPORTATION,
        SPEED_MISMATCH,
        BEARING_MISMATCH,
        MOCK_PROVIDER,
        NO_MOVEMENT
    }
    
    public enum TrustLevel {
        TRUSTED,
        SUSPICIOUS,
        SPOOFED
    }
    
    /**
     * Analyze a GPS location for spoofing indicators
     */
    public LocationTrustAnalyzer.LocationTrust analyzeLocation(
            android.location.Location gpsLocation,
            float imuSpeed,
            float imuBearing) {
        
        currentFlags.clear();
        
        // Check for mock provider
        if (gpsLocation.isFromMockProvider()) {
            // Only flag if it's not our own mock provider
            if (!gpsLocation.getProvider().equals("gps-controller")) {
                currentFlags.add(SpoofingFlag.MOCK_PROVIDER);
            }
        }
        
        if (lastGpsLocation != null) {
            Location current = Location.fromAndroidLocation(gpsLocation);
            
            // Check for teleportation
            float distance = lastGpsLocation.distanceTo(current);
            long timeDelta = (gpsLocation.getTime() - lastGpsLocation.getTimestamp()) / 1000; // seconds
            
            if (timeDelta > 0) {
                float apparentSpeed = distance / timeDelta;
                
                if (apparentSpeed > MAX_REALISTIC_SPEED) {
                    currentFlags.add(SpoofingFlag.TELEPORTATION);
                }
                
                // Check speed mismatch with IMU
                float gpsSpeed = gpsLocation.hasSpeed() ? gpsLocation.getSpeed() : apparentSpeed;
                if (Math.abs(gpsSpeed - imuSpeed) > SPEED_DIFF_THRESHOLD) {
                    currentFlags.add(SpoofingFlag.SPEED_MISMATCH);
                }
            }
            
            // Check bearing mismatch
            if (gpsLocation.hasBearing()) {
                float bearingDiff = Math.abs(
                    MathUtils.angleDifference(gpsLocation.getBearing(), imuBearing)
                );
                if (bearingDiff > BEARING_DIFF_THRESHOLD) {
                    currentFlags.add(SpoofingFlag.BEARING_MISMATCH);
                }
            }
        }
        
        // Update for next check
        lastGpsLocation = Location.fromAndroidLocation(gpsLocation);
        lastImuSpeed = imuSpeed;
        lastImuBearing = imuBearing;
        
        // Determine trust level
        TrustLevel trustLevel;
        if (currentFlags.size() >= 3) {
            trustLevel = TrustLevel.SPOOFED;
        } else if (currentFlags.size() >= 1) {
            trustLevel = TrustLevel.SUSPICIOUS;
        } else {
            trustLevel = TrustLevel.TRUSTED;
        }
        
        return new LocationTrustAnalyzer.LocationTrust(
            trustLevel,
            new ArrayList<>(currentFlags),
            calculateConfidence(trustLevel)
        );
    }
    
    private float calculateConfidence(TrustLevel trustLevel) {
        switch (trustLevel) {
            case TRUSTED:
                return 1.0f;
            case SUSPICIOUS:
                return 0.5f;
            case SPOOFED:
                return 0.0f;
            default:
                return 0.5f;
        }
    }
    
    public List<SpoofingFlag> getCurrentFlags() {
        return new ArrayList<>(currentFlags);
    }
}
