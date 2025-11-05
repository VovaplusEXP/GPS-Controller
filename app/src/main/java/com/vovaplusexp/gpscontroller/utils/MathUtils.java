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

/**
 * Mathematical utility functions for inertial navigation
 */
public class MathUtils {
    
    /**
     * Project a point onto a line segment
     */
    public static double[] projectPointOnSegment(double px, double py, 
                                                  double x1, double y1, 
                                                  double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        
        if (dx == 0 && dy == 0) {
            return new double[]{x1, y1};
        }
        
        double t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));
        
        return new double[]{x1 + t * dx, y1 + t * dy};
    }
    
    /**
     * Calculate Haversine distance between two lat/lon points in meters
     */
    public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371000; // meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return earthRadius * c;
    }
    
    /**
     * Rotate vector from device frame to world frame
     */
    public static float[] rotateToWorldFrame(float[] vector, float[] rotationMatrix) {
        float[] result = new float[3];
        
        result[0] = rotationMatrix[0] * vector[0] + rotationMatrix[1] * vector[1] + rotationMatrix[2] * vector[2];
        result[1] = rotationMatrix[3] * vector[0] + rotationMatrix[4] * vector[1] + rotationMatrix[5] * vector[2];
        result[2] = rotationMatrix[6] * vector[0] + rotationMatrix[7] * vector[1] + rotationMatrix[8] * vector[2];
        
        return result;
    }
    
    /**
     * Remove gravity from accelerometer reading
     */
    public static float[] removeGravity(float[] acceleration, float[] gravity) {
        float[] linear = new float[3];
        linear[0] = acceleration[0] - gravity[0];
        linear[1] = acceleration[1] - gravity[1];
        linear[2] = acceleration[2] - gravity[2];
        return linear;
    }
    
    /**
     * Apply low-pass filter for smoothing
     */
    public static float[] lowPassFilter(float[] input, float[] output, float alpha) {
        if (output == null) return input.clone();
        
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + alpha * (input[i] - output[i]);
        }
        return output;
    }
    
    /**
     * Calculate vector magnitude
     */
    public static float magnitude(float[] vector) {
        float sum = 0;
        for (float v : vector) {
            sum += v * v;
        }
        return (float) Math.sqrt(sum);
    }
    
    /**
     * Normalize angle to [-180, 180] degrees
     */
    public static float normalizeAngle(float angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }
    
    /**
     * Calculate angular difference
     */
    public static float angleDifference(float angle1, float angle2) {
        float diff = angle2 - angle1;
        return normalizeAngle(diff);
    }
}
