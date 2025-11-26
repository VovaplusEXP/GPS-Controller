/*
 * GPS-Controller - Inertial Navigation with GPS Spoofing Detection
 * Copyright (C) 2024 VovaplusEXP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.vovaplusexp.gpscontroller.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.vovaplusexp.gpscontroller.models.SensorData
import com.vovaplusexp.gpscontroller.utils.lowPassFilter
import com.vovaplusexp.gpscontroller.utils.removeGravity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Processes raw sensor data for inertial navigation
 */
class SensorDataProcessor(private val sensorManager: SensorManager) : SensorEventListener {

    private var listener: SensorDataListener? = null

    // Sensor data
    private val accelerometer = FloatArray(3)
    private val gyroscope = FloatArray(3)
    private val magnetometer = FloatArray(3)
    private val gravity = FloatArray(3)

    // Calibration offsets
    private val accelBias = FloatArray(3)
    private val gyroBias = FloatArray(3)

    private var lastTimestamp = 0L

    interface SensorDataListener {
        fun onSensorDataUpdated(data: SensorData)
    }

    fun setSensorDataListener(listener: SensorDataListener) {
        this.listener = listener
    }

    fun start() {
        val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (accelerometerSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME)
        }
        if (gyroscopeSensor != null) {
            sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME)
        }
        if (magnetometerSensor != null) {
            sensorManager.registerListener(this, magnetometerSensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val currentTime = System.currentTimeMillis()

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometer, 0, 3)
                // Apply low-pass filter for gravity estimation
                lowPassFilter(accelerometer, gravity, ALPHA)
                // Apply calibration
                for (i in 0..2) {
                    accelerometer[i] -= accelBias[i]
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                System.arraycopy(event.values, 0, gyroscope, 0, 3)
                // Apply calibration
                for (i in 0..2) {
                    gyroscope[i] -= gyroBias[i]
                }
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometer, 0, 3)
            }
        }

        // Throttle updates to ~100Hz
        if (currentTime - lastTimestamp >= 10) {
            lastTimestamp = currentTime
            notifyListener()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Not used
    }

    private fun notifyListener() {
        listener?.let {
            val data = SensorData(
                accelerometer = accelerometer.clone(),
                gyroscope = gyroscope.clone(),
                magnetic = magnetometer.clone(),
                timestamp = System.currentTimeMillis()
            )
            it.onSensorDataUpdated(data)
        }
    }

    /**
     * Get linear acceleration (gravity removed)
     */
    fun getLinearAcceleration(): FloatArray {
        return removeGravity(accelerometer, gravity)
    }

    /**
     * Get current gravity estimate
     */
    fun getGravity(): FloatArray {
        return gravity
    }

    /**
     * Calibrate sensors by measuring bias
     */
    fun calibrate(samples: Int, callback: CalibrationCallback) {
        GlobalScope.launch(Dispatchers.IO) {
            val accelSum = FloatArray(3)
            val gyroSum = FloatArray(3)

            try {
                delay(1000) // Wait for stabilization

                for (i in 0 until samples) {
                    for (j in 0..2) {
                        accelSum[j] += accelerometer[j]
                        gyroSum[j] += gyroscope[j]
                    }
                    delay(10)
                }

                for (i in 0..2) {
                    accelBias[i] = accelSum[i] / samples
                    gyroBias[i] = gyroSum[i] / samples
                }

                // Remove gravity from accel bias
                accelBias[2] -= SensorManager.GRAVITY_EARTH

                callback.onCalibrationComplete(true)
            } catch (e: InterruptedException) {
                callback.onCalibrationComplete(false)
            }
        }
    }

    interface CalibrationCallback {
        fun onCalibrationComplete(success: Boolean)
    }

    companion object {
        private const val ALPHA = 0.8f
    }
}
