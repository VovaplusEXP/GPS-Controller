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

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import com.vovaplusexp.gpscontroller.models.SensorData;
import com.vovaplusexp.gpscontroller.utils.MathUtils;

/**
 * Processes raw sensor data for inertial navigation
 */
public class SensorDataProcessor implements SensorEventListener {
    
    private final SensorManager sensorManager;
    private SensorDataListener listener;
    
    // Sensor data
    private float[] accelerometer = new float[3];
    private float[] gyroscope = new float[3];
    private float[] magnetometer = new float[3];
    private float[] gravity = new float[3];
    
    // Filtered values
    private float[] filteredAccel = null;
    private static final float ALPHA = 0.8f;
    
    // Calibration offsets
    private float[] accelBias = new float[3];
    private float[] gyroBias = new float[3];
    
    private long lastTimestamp = 0;
    
    public interface SensorDataListener {
        void onSensorDataUpdated(SensorData data);
    }
    
    public SensorDataProcessor(SensorManager sensorManager) {
        this.sensorManager = sensorManager;
    }
    
    public void setSensorDataListener(SensorDataListener listener) {
        this.listener = listener;
    }
    
    public void start() {
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Sensor magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
        if (gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        }
        if (magnetometer != null) {
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }
    
    public void stop() {
        sensorManager.unregisterListener(this);
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        long currentTime = System.currentTimeMillis();
        
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, accelerometer, 0, 3);
                // Apply low-pass filter for gravity estimation
                gravity = MathUtils.lowPassFilter(accelerometer, gravity, ALPHA);
                // Apply calibration
                for (int i = 0; i < 3; i++) {
                    accelerometer[i] -= accelBias[i];
                }
                break;
                
            case Sensor.TYPE_GYROSCOPE:
                System.arraycopy(event.values, 0, gyroscope, 0, 3);
                // Apply calibration
                for (int i = 0; i < 3; i++) {
                    gyroscope[i] -= gyroBias[i];
                }
                break;
                
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, magnetometer, 0, 3);
                break;
        }
        
        // Throttle updates to ~100Hz
        if (currentTime - lastTimestamp >= 10) {
            lastTimestamp = currentTime;
            notifyListener();
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }
    
    private void notifyListener() {
        if (listener != null) {
            SensorData data = new SensorData();
            data.setAcceleration(accelerometer);
            data.setGyroscope(gyroscope);
            data.setMagnetic(magnetometer);
            data.setTimestamp(System.currentTimeMillis());
            listener.onSensorDataUpdated(data);
        }
    }
    
    /**
     * Get linear acceleration (gravity removed)
     */
    public float[] getLinearAcceleration() {
        return MathUtils.removeGravity(accelerometer, gravity);
    }
    
    /**
     * Get current gravity estimate
     */
    public float[] getGravity() {
        return gravity;
    }
    
    /**
     * Calibrate sensors by measuring bias
     */
    public void calibrate(int samples, CalibrationCallback callback) {
        new Thread(() -> {
            float[] accelSum = new float[3];
            float[] gyroSum = new float[3];
            
            try {
                Thread.sleep(1000); // Wait for stabilization
                
                for (int i = 0; i < samples; i++) {
                    for (int j = 0; j < 3; j++) {
                        accelSum[j] += accelerometer[j];
                        gyroSum[j] += gyroscope[j];
                    }
                    Thread.sleep(10);
                }
                
                for (int i = 0; i < 3; i++) {
                    accelBias[i] = accelSum[i] / samples;
                    gyroBias[i] = gyroSum[i] / samples;
                }
                
                // Remove gravity from accel bias
                accelBias[2] -= SensorManager.GRAVITY_EARTH;
                
                if (callback != null) {
                    callback.onCalibrationComplete(true);
                }
            } catch (InterruptedException e) {
                if (callback != null) {
                    callback.onCalibrationComplete(false);
                }
            }
        }).start();
    }
    
    public interface CalibrationCallback {
        void onCalibrationComplete(boolean success);
    }
}
