/*
 * GPS-Controller - Inertial Navigation with GPS Spoofing Detection
 * Copyright (C) 2024 VovaplusEXP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.vovaplusexp.gpscontroller.bluetooth

/**
 * Represents a peer device in the P2P network
 */
data class PeerDevice(
    val deviceId: String,
    val deviceName: String,
    val bluetoothAddress: String
) {
    var isConnected: Boolean = false
        set(value) {
            field = value
            if (value) {
                lastSeen = System.currentTimeMillis()
            }
        }
    var lastSeen: Long = System.currentTimeMillis()
        private set

    fun updateLastSeen() {
        lastSeen = System.currentTimeMillis()
    }

    fun isStale(maxAgeMs: Long): Boolean {
        return System.currentTimeMillis() - lastSeen > maxAgeMs
    }
}
