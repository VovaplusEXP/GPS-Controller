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
import android.os.Binder;
import android.os.IBinder;
import com.vovaplusexp.gpscontroller.map.RoadDatabase;
import com.vovaplusexp.gpscontroller.map.SimpleRoadSnapper;
import com.vovaplusexp.gpscontroller.models.Location;
import timber.log.Timber;

/**
 * Service for map matching / road snapping
 */
public class MapMatchingService extends Service {
    
    private final IBinder binder = new LocalBinder();
    private RoadDatabase roadDatabase;
    private SimpleRoadSnapper roadSnapper;
    
    public class LocalBinder extends Binder {
        public MapMatchingService getService() {
            return MapMatchingService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Timber.i("MapMatchingService created");
        roadDatabase = new RoadDatabase(this);
        roadSnapper = new SimpleRoadSnapper(roadDatabase);
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    /**
     * Snap location to nearest road
     */
    public Location snapToRoad(Location location) {
        return roadSnapper.snapToRoad(location);
    }
    
    public RoadDatabase getRoadDatabase() {
        return roadDatabase;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (roadDatabase != null) {
            roadDatabase.close();
        }
        Timber.i("MapMatchingService destroyed");
    }
}
