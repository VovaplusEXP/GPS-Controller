/*
 * GPS-Controller - Inertial Navigation with GPS Spoofing Detection
 * Copyright (C) 2024 VovaplusEXP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.vovaplusexp.gpscontroller.sensors;

import com.vovaplusexp.gpscontroller.utils.MathUtils;

/**
 * Detects device movement and stationary periods
 */
public class MovementDetector {
    
    private static final float STATIONARY_THRESHOLD = 0.1f;  // m/s²
    private static final long STATIONARY_TIME_MS = 1000;     // 1 second
    
    private float currentAccelMagnitude = 0;
    private long stationaryStartTime = 0;
    private boolean isStationary = false;
    
    /**
     * Update with new acceleration data
     */
    public void update(float[] linearAcceleration) {
        currentAccelMagnitude = MathUtils.magnitude(linearAcceleration);
        
        if (currentAccelMagnitude < STATIONARY_THRESHOLD) {
            if (stationaryStartTime == 0) {
                stationaryStartTime = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - stationaryStartTime > STATIONARY_TIME_MS) {
                isStationary = true;
            }
        } else {
            stationaryStartTime = 0;
            isStationary = false;
        }
    }
    
    /**
     * Check if device is stationary
     */
    public boolean isStationary() {
        return isStationary;
    }
    
    /**
     * Get current acceleration magnitude
     */
    public float getAccelerationMagnitude() {
        return currentAccelMagnitude;
    }
}
