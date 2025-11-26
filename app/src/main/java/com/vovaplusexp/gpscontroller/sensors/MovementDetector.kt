/*
 * GPS-Controller - Inertial Navigation with GPS Spoofing Detection
 * Copyright (C) 2024 VovaplusEXP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.vovaplusexp.gpscontroller.sensors

import com.vovaplusexp.gpscontroller.utils.magnitude

/**
 * Detects device movement and stationary periods
 */
class MovementDetector {

    private var currentAccelMagnitude = 0f
    private var stationaryStartTime = 0L

    var isStationary = false
        private set

    /**
     * Update with new acceleration data
     */
    fun update(linearAcceleration: FloatArray) {
        currentAccelMagnitude = linearAcceleration.magnitude()

        if (currentAccelMagnitude < STATIONARY_THRESHOLD) {
            if (stationaryStartTime == 0L) {
                stationaryStartTime = System.currentTimeMillis()
            } else if (System.currentTimeMillis() - stationaryStartTime > STATIONARY_TIME_MS) {
                isStationary = true
            }
        } else {
            stationaryStartTime = 0L
            isStationary = false
        }
    }

    /**
     * Get current acceleration magnitude
     */
    fun getAccelerationMagnitude(): Float {
        return currentAccelMagnitude
    }

    companion object {
        private const val STATIONARY_THRESHOLD = 0.1f // m/s²
        private const val STATIONARY_TIME_MS = 1000L  // 1 second
    }
}
