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
 * Represents a location estimate with metadata
 */
public class Location {
    private double latitude;
    private double longitude;
    private float accuracy;
    private float speed;
    private float bearing;
    private long timestamp;
    private LocationSource source;
    private float confidence;

    public enum LocationSource {
        GPS,
        INERTIAL,
        HYBRID,
        PEER_FUSED,
        MAP_MATCHED
    }

    public Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = System.currentTimeMillis();
        this.source = LocationSource.GPS;
        this.confidence = 1.0f;
    }

    public Location(double latitude, double longitude, float accuracy) {
        this(latitude, longitude);
        this.accuracy = accuracy;
    }

    // Getters and setters
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    
    public float getAccuracy() { return accuracy; }
    public void setAccuracy(float accuracy) { this.accuracy = accuracy; }
    
    public float getSpeed() { return speed; }
    public void setSpeed(float speed) { this.speed = speed; }
    
    public float getBearing() { return bearing; }
    public void setBearing(float bearing) { this.bearing = bearing; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public LocationSource getSource() { return source; }
    public void setSource(LocationSource source) { this.source = source; }
    
    public float getConfidence() { return confidence; }
    public void setConfidence(float confidence) { this.confidence = confidence; }

    /**
     * Calculate distance to another location in meters
     */
    public float distanceTo(Location other) {
        double earthRadius = 6371000; // meters
        double dLat = Math.toRadians(other.latitude - this.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(this.latitude)) * 
                   Math.cos(Math.toRadians(other.latitude)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return (float) (earthRadius * c);
    }

    /**
     * Convert to Android Location object
     */
    public android.location.Location toAndroidLocation() {
        android.location.Location loc = new android.location.Location("gps-controller");
        loc.setLatitude(latitude);
        loc.setLongitude(longitude);
        loc.setAccuracy(accuracy);
        loc.setSpeed(speed);
        loc.setBearing(bearing);
        loc.setTime(timestamp);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            loc.setElapsedRealtimeNanos(android.os.SystemClock.elapsedRealtimeNanos());
        }
        return loc;
    }

    /**
     * Create from Android Location object
     */
    public static Location fromAndroidLocation(android.location.Location loc) {
        Location location = new Location(loc.getLatitude(), loc.getLongitude(), loc.getAccuracy());
        location.setSpeed(loc.getSpeed());
        location.setBearing(loc.getBearing());
        location.setTimestamp(loc.getTime());
        return location;
    }
}
