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

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Helper for managing runtime permissions
 */
public class PermissionHelper {
    
    public static final int PERMISSION_REQUEST_LOCATION = 1001;
    public static final int PERMISSION_REQUEST_BLUETOOTH = 1002;
    public static final int PERMISSION_REQUEST_ALL = 1003;
    
    private static final String[] LOCATION_PERMISSIONS = {
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    };
    
    private static final String[] LOCATION_BACKGROUND_PERMISSIONS = {
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    };
    
    private static final String[] BLUETOOTH_PERMISSIONS = 
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? new String[]{
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        } : new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        };
    
    public static boolean hasLocationPermission(Context context) {
        for (String permission : LOCATION_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) 
                != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean hasBackgroundLocationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(context, 
                Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
    
    public static boolean hasBluetoothPermission(Context context) {
        for (String permission : BLUETOOTH_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) 
                != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    public static void requestLocationPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, 
            LOCATION_PERMISSIONS, PERMISSION_REQUEST_LOCATION);
    }
    
    public static void requestBackgroundLocationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(activity, 
                new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 
                PERMISSION_REQUEST_LOCATION);
        }
    }
    
    public static void requestBluetoothPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, 
            BLUETOOTH_PERMISSIONS, PERMISSION_REQUEST_BLUETOOTH);
    }
    
    public static void requestAllPermissions(Activity activity) {
        String[] allPermissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            allPermissions = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            };
        } else {
            allPermissions = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            };
        }
        ActivityCompat.requestPermissions(activity, allPermissions, PERMISSION_REQUEST_ALL);
    }
}
