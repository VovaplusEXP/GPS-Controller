/*
 * GPS-Controller - Inertial Navigation with GPS Spoofing Detection
 * Copyright (C) 2024 VovaplusEXP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.vovaplusexp.gpscontroller.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.vovaplusexp.gpscontroller.map.RoadDatabase
import com.vovaplusexp.gpscontroller.map.SimpleRoadSnapper
import com.vovaplusexp.gpscontroller.models.Location
import timber.log.Timber

/**
 * Service for map matching / road snapping
 */
class MapMatchingService : Service() {

    private val binder = LocalBinder()
    private lateinit var roadDatabase: RoadDatabase
    private lateinit var roadSnapper: SimpleRoadSnapper

    inner class LocalBinder : Binder() {
        fun getService(): MapMatchingService = this@MapMatchingService
    }

    override fun onCreate() {
        super.onCreate()
        Timber.i("MapMatchingService created")
        roadDatabase = RoadDatabase(this)
        roadSnapper = SimpleRoadSnapper(roadDatabase)
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    /**
     * Snap location to nearest road
     */
    fun snapToRoad(location: Location): Location? {
        return roadSnapper.snapLocation(location)
    }

    fun getRoadDatabase(): RoadDatabase {
        return roadDatabase
    }

    override fun onDestroy() {
        super.onDestroy()
        roadDatabase.close()
        Timber.i("MapMatchingService destroyed")
    }
}
