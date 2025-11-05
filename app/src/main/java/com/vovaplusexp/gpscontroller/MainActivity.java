/*
 * GPS-Controller - Inertial Navigation with GPS Spoofing Detection
 * Copyright (C) 2024 VovaplusEXP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.vovaplusexp.gpscontroller;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.vovaplusexp.gpscontroller.fragments.*;
import com.vovaplusexp.gpscontroller.models.Location;
import com.vovaplusexp.gpscontroller.services.*;
import com.vovaplusexp.gpscontroller.spoofing.*;
import com.vovaplusexp.gpscontroller.utils.*;
import timber.log.Timber;

/**
 * Main activity with navigation between fragments
 */
public class MainActivity extends AppCompatActivity {
    
    private TextView statusText;
    private BottomNavigationView bottomNavigation;
    
    private InertialNavigationService inertialService;
    private MockLocationService mockLocationService;
    private MapMatchingService mapMatchingService;
    private PeerNavigationService peerNavigationService;
    
    private boolean servicesStarted = false;
    
    private LocationManager locationManager;
    private GpsSpoofingDetector spoofingDetector;
    private Location lastGpsLocation;
    
    private PreferencesManager prefsManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize Timber logging
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
        
        setContentView(R.layout.activity_main);
        
        prefsManager = new PreferencesManager(this);
        
        initializeViews();
        checkPermissions();
        
        spoofingDetector = new GpsSpoofingDetector();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        
        // Start services
        startServices();
        
        // Load initial fragment
        if (savedInstanceState == null) {
            loadFragment(new MapFragment());
        }
    }
    
    private void initializeViews() {
        statusText = findViewById(R.id.status_text);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_map) {
                fragment = new MapFragment();
            } else if (itemId == R.id.nav_peers) {
                fragment = new PeerDevicesFragment();
            } else if (itemId == R.id.nav_diagnostics) {
                fragment = new DiagnosticsFragment();
            }
            
            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }
    
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit();
    }
    
    private void checkPermissions() {
        if (!PermissionHelper.hasLocationPermission(this)) {
            PermissionHelper.requestLocationPermission(this);
        } else {
            startLocationUpdates();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PermissionHelper.PERMISSION_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000, // 1 second
            0,    // 0 meters
            gpsLocationListener
        );
    }
    
    private final LocationListener gpsLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull android.location.Location location) {
            handleGpsLocation(location);
        }
        
        @Override
        public void onProviderEnabled(@NonNull String provider) {
            updateStatus("GPS enabled");
        }
        
        @Override
        public void onProviderDisabled(@NonNull String provider) {
            updateStatus("GPS disabled");
        }
    };
    
    private void handleGpsLocation(android.location.Location gpsLocation) {
        if (inertialService == null) return;
        
        // Analyze for spoofing
        float imuSpeed = 0;
        float imuBearing = 0;
        
        if (inertialService.getCurrentLocation() != null) {
            imuSpeed = inertialService.getCurrentLocation().getSpeed();
            imuBearing = inertialService.getOrientationTracker().getAzimuth();
        }
        
        LocationTrustAnalyzer.LocationTrust trust = spoofingDetector.analyzeLocation(
            gpsLocation, imuSpeed, imuBearing
        );
        
        if (trust.isTrusted()) {
            // Use GPS location to reset inertial navigation
            Location location = Location.fromAndroidLocation(gpsLocation);
            inertialService.initializeLocation(location);
            lastGpsLocation = location;
            
            // Update mock location if enabled
            if (prefsManager.isMockLocationEnabled() && mockLocationService != null) {
                mockLocationService.updateLocation(location);
            }
            
            updateStatus("GPS: Normal");
        } else if (trust.isSpoofed()) {
            updateStatus("GPS: SPOOFED! " + trust.getDescription());
        } else {
            updateStatus("GPS: Suspicious - " + trust.getDescription());
        }
    }
    
    private void updateStatus(String status) {
        runOnUiThread(() -> {
            if (statusText != null) {
                statusText.setText(status);
            }
        });
    }
    
    private void startServices() {
        if (servicesStarted) return;
        
        Intent inertialIntent = new Intent(this, InertialNavigationService.class);
        Intent mockIntent = new Intent(this, MockLocationService.class);
        Intent mapIntent = new Intent(this, MapMatchingService.class);
        Intent peerIntent = new Intent(this, PeerNavigationService.class);
        
        startService(inertialIntent);
        bindService(inertialIntent, inertialConnection, Context.BIND_AUTO_CREATE);
        
        bindService(mockIntent, mockLocationConnection, Context.BIND_AUTO_CREATE);
        bindService(mapIntent, mapMatchingConnection, Context.BIND_AUTO_CREATE);
        
        if (prefsManager.isPeerEnabled()) {
            startService(peerIntent);
            bindService(peerIntent, peerConnection, Context.BIND_AUTO_CREATE);
        }
        
        servicesStarted = true;
    }
    
    private final ServiceConnection inertialConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            InertialNavigationService.LocalBinder binder = 
                (InertialNavigationService.LocalBinder) service;
            inertialService = binder.getService();
            Timber.i("InertialNavigationService connected");
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            inertialService = null;
        }
    };
    
    private final ServiceConnection mockLocationConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MockLocationService.LocalBinder binder = (MockLocationService.LocalBinder) service;
            mockLocationService = binder.getService();
            Timber.i("MockLocationService connected");
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mockLocationService = null;
        }
    };
    
    private final ServiceConnection mapMatchingConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MapMatchingService.LocalBinder binder = (MapMatchingService.LocalBinder) service;
            mapMatchingService = binder.getService();
            Timber.i("MapMatchingService connected");
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mapMatchingService = null;
        }
    };
    
    private final ServiceConnection peerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PeerNavigationService.LocalBinder binder = (PeerNavigationService.LocalBinder) service;
            peerNavigationService = binder.getService();
            Timber.i("PeerNavigationService connected");
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            peerNavigationService = null;
        }
    };
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (locationManager != null) {
            locationManager.removeUpdates(gpsLocationListener);
        }
        
        unbindService(inertialConnection);
        unbindService(mockLocationConnection);
        unbindService(mapMatchingConnection);
        
        if (peerNavigationService != null) {
            unbindService(peerConnection);
        }
    }
}
