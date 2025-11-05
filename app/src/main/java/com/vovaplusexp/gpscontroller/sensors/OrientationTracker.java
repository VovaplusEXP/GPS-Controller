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

import android.hardware.SensorManager;

/**
 * Tracks device orientation using sensor fusion
 */
public class OrientationTracker {
    
    private float[] rotationMatrix = new float[9];
    private float[] orientation = new float[3];  // azimuth, pitch, roll
    private float[] quaternion = new float[4];   // For smoother rotation
    
    private float[] gravity = new float[3];
    private float[] geomagnetic = new float[3];
    
    private static final float EPSILON = 0.000001f;
    
    public OrientationTracker() {
        // Initialize rotation matrix to identity
        rotationMatrix[0] = 1; rotationMatrix[4] = 1; rotationMatrix[8] = 1;
        quaternion[3] = 1; // w component
    }
    
    /**
     * Update orientation from gravity and magnetic field
     */
    public void updateFromGravityAndMagnetic(float[] gravity, float[] magnetic) {
        System.arraycopy(gravity, 0, this.gravity, 0, 3);
        System.arraycopy(magnetic, 0, this.geomagnetic, 0, 3);
        
        float[] R = new float[9];
        float[] I = new float[9];
        
        if (SensorManager.getRotationMatrix(R, I, gravity, magnetic)) {
            System.arraycopy(R, 0, rotationMatrix, 0, 9);
            SensorManager.getOrientation(R, orientation);
        }
    }
    
    /**
     * Update orientation from gyroscope (integration)
     */
    public void updateFromGyroscope(float[] gyro, float deltaTime) {
        // Simple integration - could be improved with complementary filter
        float magnitude = (float) Math.sqrt(gyro[0] * gyro[0] + 
                                             gyro[1] * gyro[1] + 
                                             gyro[2] * gyro[2]);
        
        if (magnitude > EPSILON) {
            float angle = magnitude * deltaTime;
            
            // Rodrigues' rotation formula for quaternion update
            float sinHalfAngle = (float) Math.sin(angle / 2);
            float cosHalfAngle = (float) Math.cos(angle / 2);
            
            float[] axis = new float[3];
            axis[0] = gyro[0] / magnitude;
            axis[1] = gyro[1] / magnitude;
            axis[2] = gyro[2] / magnitude;
            
            float[] dq = new float[4];
            dq[0] = axis[0] * sinHalfAngle;
            dq[1] = axis[1] * sinHalfAngle;
            dq[2] = axis[2] * sinHalfAngle;
            dq[3] = cosHalfAngle;
            
            // Quaternion multiplication
            float[] newQ = multiplyQuaternions(quaternion, dq);
            System.arraycopy(newQ, 0, quaternion, 0, 4);
            
            // Normalize
            normalizeQuaternion(quaternion);
            
            // Convert to rotation matrix
            quaternionToRotationMatrix(quaternion, rotationMatrix);
            
            // Extract orientation
            SensorManager.getOrientation(rotationMatrix, orientation);
        }
    }
    
    public float[] getRotationMatrix() {
        return rotationMatrix;
    }
    
    public float[] getOrientation() {
        return orientation;
    }
    
    public float getAzimuth() {
        return (float) Math.toDegrees(orientation[0]);
    }
    
    public float getPitch() {
        return (float) Math.toDegrees(orientation[1]);
    }
    
    public float getRoll() {
        return (float) Math.toDegrees(orientation[2]);
    }
    
    private float[] multiplyQuaternions(float[] q1, float[] q2) {
        float[] result = new float[4];
        result[3] = q1[3] * q2[3] - q1[0] * q2[0] - q1[1] * q2[1] - q1[2] * q2[2];
        result[0] = q1[3] * q2[0] + q1[0] * q2[3] + q1[1] * q2[2] - q1[2] * q2[1];
        result[1] = q1[3] * q2[1] - q1[0] * q2[2] + q1[1] * q2[3] + q1[2] * q2[0];
        result[2] = q1[3] * q2[2] + q1[0] * q2[1] - q1[1] * q2[0] + q1[2] * q2[3];
        return result;
    }
    
    private void normalizeQuaternion(float[] q) {
        float norm = (float) Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]);
        if (norm > EPSILON) {
            q[0] /= norm;
            q[1] /= norm;
            q[2] /= norm;
            q[3] /= norm;
        }
    }
    
    private void quaternionToRotationMatrix(float[] q, float[] R) {
        float xx = q[0] * q[0];
        float xy = q[0] * q[1];
        float xz = q[0] * q[2];
        float xw = q[0] * q[3];
        float yy = q[1] * q[1];
        float yz = q[1] * q[2];
        float yw = q[1] * q[3];
        float zz = q[2] * q[2];
        float zw = q[2] * q[3];
        
        R[0] = 1 - 2 * (yy + zz);
        R[1] = 2 * (xy - zw);
        R[2] = 2 * (xz + yw);
        R[3] = 2 * (xy + zw);
        R[4] = 1 - 2 * (xx + zz);
        R[5] = 2 * (yz - xw);
        R[6] = 2 * (xz - yw);
        R[7] = 2 * (yz + xw);
        R[8] = 1 - 2 * (xx + yy);
    }
}
