/*
 * GPS-Controller - Inertial Navigation with GPS Spoofing Detection
 * Copyright (C) 2024 VovaplusEXP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.vovaplusexp.gpscontroller.models;

/**
 * Represents a location estimate from a peer device
 */
public class PeerEstimate {
    private String deviceId;
    private Location location;
    private float confidence;
    private long timestamp;
    
    public PeerEstimate(String deviceId, Location location, float confidence) {
        this.deviceId = deviceId;
        this.location = location;
        this.confidence = confidence;
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getDeviceId() { return deviceId; }
    public Location getLocation() { return location; }
    public float getConfidence() { return confidence; }
    public long getTimestamp() { return timestamp; }
    
    public boolean isExpired(long maxAgeMs) {
        return (System.currentTimeMillis() - timestamp) > maxAgeMs;
    }
}
