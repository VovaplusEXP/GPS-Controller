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
 * Represents sensor data from accelerometer, gyroscope, and magnetometer
 */
public class SensorData {
    private float[] acceleration = new float[3];  // m/s²
    private float[] gyroscope = new float[3];     // rad/s
    private float[] magnetic = new float[3];      // μT
    private long timestamp;
    
    public SensorData() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public float[] getAcceleration() { return acceleration; }
    public void setAcceleration(float[] acceleration) {
        System.arraycopy(acceleration, 0, this.acceleration, 0, 3);
    }
    
    public float[] getGyroscope() { return gyroscope; }
    public void setGyroscope(float[] gyroscope) {
        System.arraycopy(gyroscope, 0, this.gyroscope, 0, 3);
    }
    
    public float[] getMagnetic() { return magnetic; }
    public void setMagnetic(float[] magnetic) {
        System.arraycopy(magnetic, 0, this.magnetic, 0, 3);
    }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
