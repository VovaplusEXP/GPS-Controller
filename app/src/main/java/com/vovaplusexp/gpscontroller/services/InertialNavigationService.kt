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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.vovaplusexp.gpscontroller.R
import com.vovaplusexp.gpscontroller.models.Location
import com.vovaplusexp.gpscontroller.models.SensorData
import com.vovaplusexp.gpscontroller.sensors.MovementDetector
import com.vovaplusexp.gpscontroller.sensors.OrientationTracker
import com.vovaplusexp.gpscontroller.sensors.SensorDataProcessor
import com.vovaplusexp.gpscontroller.utils.MathUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Core service for inertial navigation updates
 */
class InertialNavigationService : Service() {

    private val binder = LocalBinder()
    private lateinit var sensorManager: SensorManager
    private lateinit var sensorProcessor: SensorDataProcessor

    private val orientationTracker = OrientationTracker()
    private val movementDetector = MovementDetector()

    private var lastUpdateTime = 0L

    private val _currentLocation = MutableStateFlow(
        Location(
            latitude = 0.0,
            longitude = 0.0,
            speed = 0f,
            bearing = 0f,
            timestamp = 0L,
            source = Location.LocationSource.INERTIAL,
            confidence = 0f
        )
    )
    val currentLocation: StateFlow<Location> = _currentLocation.asStateFlow()

    inner class LocalBinder : Binder() {
        fun getService(): InertialNavigationService = this@InertialNavigationService
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorProcessor = SensorDataProcessor(sensorManager)
        sensorProcessor.setSensorDataListener(object : SensorDataProcessor.SensorDataListener {
            override fun onSensorDataUpdated(data: SensorData) {
                onSensorData(data)
            }
        })
        lastUpdateTime = System.currentTimeMillis()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification())
        sensorProcessor.start()
        Timber.i("InertialNavigationService started")
        return START_STICKY
    }

    override fun onDestroy() {
        sensorProcessor.stop()
        Timber.i("InertialNavigationService stopped")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun onSensorData(data: SensorData) {
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastUpdateTime) / 1000.0f
        lastUpdateTime = currentTime

        if (deltaTime > 1.0f) {
            Timber.w("Large delta time: $deltaTime, skipping update")
            return
        }

        // Update orientation (could be improved with sensor fusion)
        orientationTracker.updateFromGravityAndMagnetic(
            sensorProcessor.getGravity(),
            data.magnetic
        )
        orientationTracker.updateFromGyroscope(data.gyroscope, deltaTime)

        // Update movement detection
        val linearAcceleration = sensorProcessor.getLinearAcceleration()
        movementDetector.update(linearAcceleration)

        // If stationary, reset velocity and don't update position
        if (movementDetector.isStationary) {
            val updatedLocation = _currentLocation.value.copy(
                speed = 0f,
                bearing = orientationTracker.azimuth
            )
            _currentLocation.value = updatedLocation
            return
        }

        // Rotate acceleration to world frame
        val worldAcceleration = MathUtils.rotateToWorldFrame(
            linearAcceleration,
            orientationTracker.rotationMatrix
        )

        // Integrate acceleration to get velocity
        val newSpeed = _currentLocation.value.speed + worldAcceleration[0] * deltaTime
        val newBearing = orientationTracker.azimuth

        // Integrate velocity to get position
        val distance = newSpeed * deltaTime
        val (newLat, newLon) = calculateNewPosition(
            _currentLocation.value.latitude,
            _currentLocation.value.longitude,
            distance.toDouble(),
            newBearing.toDouble()
        )

        val updatedLocation = _currentLocation.value.copy(
            latitude = newLat,
            longitude = newLon,
            speed = newSpeed,
            bearing = newBearing,
            timestamp = currentTime
        )
        _currentLocation.value = updatedLocation
    }

    fun resetToLocation(location: Location) {
        _currentLocation.value = location
    }

    private fun calculateNewPosition(lat: Double, lon: Double, distance: Double, bearing: Double): Pair<Double, Double> {
        val r = 6371000 // Earth radius in meters
        val latRad = Math.toRadians(lat)
        val lonRad = Math.toRadians(lon)
        val bearingRad = Math.toRadians(bearing)

        val newLatRad = Math.asin(
            Math.sin(latRad) * Math.cos(distance / r) +
                    Math.cos(latRad) * Math.sin(distance / r) * Math.cos(bearingRad)
        )
        val newLonRad = lonRad + Math.atan2(
            Math.sin(bearingRad) * Math.sin(distance / r) * Math.cos(latRad),
            Math.cos(distance / r) - Math.sin(latRad) * Math.sin(newLatRad)
        )
        return Pair(Math.toDegrees(newLatRad), Math.toDegrees(newLonRad))
    }

    private fun createNotification(): Notification {
        val channelId = "inertial_navigation"
        val channel = NotificationChannel(
            channelId,
            "Inertial Navigation",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Inertial Navigation")
            .setContentText("Running in the background")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }
}
