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

import android.app.Service;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import com.vovaplusexp.gpscontroller.models.Location;
import timber.log.Timber;

/**
 * Service for providing mock locations to other apps
 */
public class MockLocationService extends Service {
    
    private final IBinder binder = new LocalBinder();
    private LocationManager locationManager;
    private boolean isProviderEnabled = false;
    
    public class LocalBinder extends Binder {
        public MockLocationService getService() {
            return MockLocationService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Timber.i("MockLocationService created");
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        setupMockProvider();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    /**
     * Setup mock location provider
     */
    private void setupMockProvider() {
        try {
            locationManager.addTestProvider(
                LocationManager.GPS_PROVIDER,
                false, false, false, false, true, true, true,
                android.location.Criteria.POWER_LOW,
                android.location.Criteria.ACCURACY_FINE
            );
            locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
            isProviderEnabled = true;
            Timber.i("Mock location provider setup complete");
        } catch (SecurityException e) {
            Timber.e(e, "Failed to setup mock provider - permission denied");
        } catch (Exception e) {
            Timber.e(e, "Failed to setup mock provider");
        }
    }
    
    /**
     * Update mock location
     */
    public void updateLocation(Location location) {
        if (!isProviderEnabled) {
            Timber.w("Mock provider not enabled");
            return;
        }
        
        try {
            android.location.Location mockLocation = location.toAndroidLocation();
            mockLocation.setProvider(LocationManager.GPS_PROVIDER);
            mockLocation.setTime(System.currentTimeMillis());
            mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            
            locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, mockLocation);
            Timber.d("Mock location updated: %.6f, %.6f", location.getLatitude(), location.getLongitude());
        } catch (Exception e) {
            Timber.e(e, "Failed to update mock location");
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isProviderEnabled) {
            try {
                locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
            } catch (Exception e) {
                Timber.e(e, "Error removing test provider");
            }
        }
        Timber.i("MockLocationService destroyed");
    }
}
