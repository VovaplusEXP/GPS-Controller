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
 * Represents a road segment for map matching
 */
public class RoadSegment {
    private long id;
    private double lat1;
    private double lon1;
    private double lat2;
    private double lon2;
    private double minLat;
    private double maxLat;
    private double minLon;
    private double maxLon;
    
    public RoadSegment(long id, double lat1, double lon1, double lat2, double lon2) {
        this.id = id;
        this.lat1 = lat1;
        this.lon1 = lon1;
        this.lat2 = lat2;
        this.lon2 = lon2;
        this.minLat = Math.min(lat1, lat2);
        this.maxLat = Math.max(lat1, lat2);
        this.minLon = Math.min(lon1, lon2);
        this.maxLon = Math.max(lon1, lon2);
    }
    
    public long getId() { return id; }
    public double getLat1() { return lat1; }
    public double getLon1() { return lon1; }
    public double getLat2() { return lat2; }
    public double getLon2() { return lon2; }
    public double getMinLat() { return minLat; }
    public double getMaxLat() { return maxLat; }
    public double getMinLon() { return minLon; }
    public double getMaxLon() { return maxLon; }
}
