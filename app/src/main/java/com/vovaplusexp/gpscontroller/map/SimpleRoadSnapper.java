/*
 * GPS-Controller - Inertial Navigation with GPS Spoofing Detection
 * Copyright (C) 2024 VovaplusEXP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.vovaplusexp.gpscontroller.map;

import com.vovaplusexp.gpscontroller.models.Location;
import com.vovaplusexp.gpscontroller.models.RoadSegment;
import com.vovaplusexp.gpscontroller.utils.MathUtils;
import java.util.List;

/**
 * Snaps location estimates to nearest roads
 */
public class SimpleRoadSnapper {
    
    private static final double MAX_SNAP_DISTANCE = 30.0;  // meters
    private static final double SEARCH_RADIUS = 50.0;      // meters
    
    private final RoadDatabase roadDatabase;
    
    public SimpleRoadSnapper(RoadDatabase roadDatabase) {
        this.roadDatabase = roadDatabase;
    }
    
    /**
     * Snap location to nearest road
     */
    public Location snapToRoad(Location location) {
        List<RoadSegment> nearbyRoads = roadDatabase.findNearestRoads(
            location.getLatitude(),
            location.getLongitude(),
            SEARCH_RADIUS
        );
        
        if (nearbyRoads.isEmpty()) {
            return location; // No roads nearby
        }
        
        RoadSegment closestRoad = null;
        double minDistance = Double.MAX_VALUE;
        double[] closestPoint = null;
        
        // Find closest road segment
        for (RoadSegment road : nearbyRoads) {
            double[] projected = MathUtils.projectPointOnSegment(
                location.getLatitude(), location.getLongitude(),
                road.getLat1(), road.getLon1(),
                road.getLat2(), road.getLon2()
            );
            
            double distance = MathUtils.haversineDistance(
                location.getLatitude(), location.getLongitude(),
                projected[0], projected[1]
            );
            
            if (distance < minDistance) {
                minDistance = distance;
                closestRoad = road;
                closestPoint = projected;
            }
        }
        
        // Only snap if within reasonable distance
        if (closestPoint != null && minDistance <= MAX_SNAP_DISTANCE) {
            Location snapped = new Location(closestPoint[0], closestPoint[1]);
            snapped.setAccuracy(location.getAccuracy());
            snapped.setSpeed(location.getSpeed());
            snapped.setBearing(calculateBearing(closestRoad));
            snapped.setSource(Location.LocationSource.MAP_MATCHED);
            snapped.setConfidence(location.getConfidence() * 1.1f); // Slightly higher confidence
            return snapped;
        }
        
        return location; // Too far from roads
    }
    
    /**
     * Calculate bearing of road segment
     */
    private float calculateBearing(RoadSegment road) {
        double lat1 = Math.toRadians(road.getLat1());
        double lat2 = Math.toRadians(road.getLat2());
        double lonDiff = Math.toRadians(road.getLon2() - road.getLon1());
        
        double y = Math.sin(lonDiff) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) -
                   Math.sin(lat1) * Math.cos(lat2) * Math.cos(lonDiff);
        
        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (float) ((bearing + 360) % 360);
    }
}
