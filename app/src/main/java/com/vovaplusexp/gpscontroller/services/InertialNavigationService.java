/*
 * GPS-Controller - Inertial Navigation with GPS Spoofing Detection
 * Copyright (C) 2024 VovaplusEXP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.vovaplusexp.gpscontroller.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import com.vovaplusexp.gpscontroller.R;
import com.vovaplusexp.gpscontroller.models.Location;
import com.vovaplusexp.gpscontroller.models.SensorData;
import com.vovaplusexp.gpscontroller.sensors.*;
import com.vovaplusexp.gpscontroller.utils.MathUtils;
import timber.log.Timber;

/**
 * Foreground service for inertial navigation
 */
public class InertialNavigationService extends Service {
    
    private static final String CHANNEL_ID = "InertialNavigationChannel";
    private static final int NOTIFICATION_ID = 1001;
    
    private final IBinder binder = new LocalBinder();
    
    private SensorManager sensorManager;
    private SensorDataProcessor sensorProcessor;
    private OrientationTracker orientationTracker;
    private MovementDetector movementDetector;
    
    // Inertial navigation state
    private Location currentLocation;
    private float[] velocity = new float[]{0, 0, 0};  // m/s in world frame
    private long lastUpdateTime = 0;
    
    private NavigationListener navigationListener;
    
    public interface NavigationListener {
        void onLocationUpdated(Location location);
        void onVelocityUpdated(float speed, float bearing);
    }
    
    public class LocalBinder extends Binder {
        public InertialNavigationService getService() {
            return InertialNavigationService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Timber.i("InertialNavigationService created");
        
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorProcessor = new SensorDataProcessor(sensorManager);
        orientationTracker = new OrientationTracker();
        movementDetector = new MovementDetector();
        
        sensorProcessor.setSensorDataListener(this::onSensorDataUpdated);
        
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.i("InertialNavigationService started");
        sensorProcessor.start();
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.i("InertialNavigationService destroyed");
        sensorProcessor.stop();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    public void setNavigationListener(NavigationListener listener) {
        this.navigationListener = listener;
    }
    
    /**
     * Initialize inertial navigation with a known location
     */
    public void initializeLocation(Location location) {
        this.currentLocation = location;
        this.velocity = new float[]{0, 0, 0};
        this.lastUpdateTime = System.currentTimeMillis();
        Timber.i("Initialized inertial navigation at: %.6f, %.6f", 
            location.getLatitude(), location.getLongitude());
    }
    
    /**
     * Process sensor data for inertial navigation
     */
    private void onSensorDataUpdated(SensorData data) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
        
        if (lastUpdateTime == 0 || deltaTime > 1.0f) {
            // Skip first update or if too much time passed
            lastUpdateTime = currentTime;
            return;
        }
        
        // Update orientation
        orientationTracker.updateFromGravityAndMagnetic(
            sensorProcessor.getGravity(),
            data.getMagnetic()
        );
        orientationTracker.updateFromGyroscope(data.getGyroscope(), deltaTime);
        
        // Get linear acceleration and rotate to world frame
        float[] linearAccel = sensorProcessor.getLinearAcceleration();
        movementDetector.update(linearAccel);
        
        float[] worldAccel = MathUtils.rotateToWorldFrame(
            linearAccel,
            orientationTracker.getRotationMatrix()
        );
        
        // ZUPT - Zero Velocity Update when stationary
        if (movementDetector.isStationary()) {
            velocity[0] = 0;
            velocity[1] = 0;
            velocity[2] = 0;
        } else {
            // Integrate acceleration to velocity
            velocity[0] += worldAccel[0] * deltaTime;
            velocity[1] += worldAccel[1] * deltaTime;
            velocity[2] += worldAccel[2] * deltaTime;
        }
        
        // Integrate velocity to position
        if (currentLocation != null) {
            // Convert m/s to degrees (approximate)
            double latChange = (velocity[1] * deltaTime) / 111000.0;
            double lonChange = (velocity[0] * deltaTime) / (111000.0 * Math.cos(Math.toRadians(currentLocation.getLatitude())));
            
            currentLocation.setLatitude(currentLocation.getLatitude() + latChange);
            currentLocation.setLongitude(currentLocation.getLongitude() + lonChange);
            currentLocation.setTimestamp(currentTime);
            currentLocation.setSource(Location.LocationSource.INERTIAL);
            
            // Calculate speed and bearing
            float speed = (float) Math.sqrt(velocity[0] * velocity[0] + velocity[1] * velocity[1]);
            currentLocation.setSpeed(speed);
            currentLocation.setBearing(orientationTracker.getAzimuth());
            
            // Reduce confidence over time
            float timeSinceInit = (currentTime - lastUpdateTime) / 1000.0f;
            float confidenceDegradation = Math.min(0.9f, timeSinceInit / 60.0f); // Max 90% degradation
            currentLocation.setConfidence(1.0f - confidenceDegradation);
            
            // Notify listener
            if (navigationListener != null) {
                navigationListener.onLocationUpdated(currentLocation);
                navigationListener.onVelocityUpdated(speed, orientationTracker.getAzimuth());
            }
        }
        
        lastUpdateTime = currentTime;
    }
    
    public Location getCurrentLocation() {
        return currentLocation;
    }
    
    public float[] getVelocity() {
        return velocity;
    }
    
    public OrientationTracker getOrientationTracker() {
        return orientationTracker;
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Inertial Navigation",
                NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    
    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GPS-Controller")
            .setContentText("Inertial navigation active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }
}
