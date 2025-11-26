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

import android.hardware.SensorManager
import kotlin.math.*

/**
 * Tracks device orientation using sensor fusion
 */
class OrientationTracker {

    val rotationMatrix = FloatArray(9)
    val orientation = FloatArray(3) // azimuth, pitch, roll
    private val quaternion = FloatArray(4) // For smoother rotation

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)

    init {
        // Initialize rotation matrix to identity
        rotationMatrix[0] = 1f
        rotationMatrix[4] = 1f
        rotationMatrix[8] = 1f
        quaternion[3] = 1f // w component
    }

    /**
     * Update orientation from gravity and magnetic field
     */
    fun updateFromGravityAndMagnetic(gravity: FloatArray, magnetic: FloatArray) {
        System.arraycopy(gravity, 0, this.gravity, 0, 3)
        System.arraycopy(magnetic, 0, this.geomagnetic, 0, 3)

        val r = FloatArray(9)
        val i = FloatArray(9)

        if (SensorManager.getRotationMatrix(r, i, gravity, magnetic)) {
            System.arraycopy(r, 0, rotationMatrix, 0, 9)
            SensorManager.getOrientation(r, orientation)
        }
    }

    /**
     * Update orientation from gyroscope (integration)
     */
    fun updateFromGyroscope(gyro: FloatArray, deltaTime: Float) {
        // Simple integration - could be improved with complementary filter
        val magnitude = sqrt(gyro[0] * gyro[0] + gyro[1] * gyro[1] + gyro[2] * gyro[2])

        if (magnitude > EPSILON) {
            val angle = magnitude * deltaTime

            // Rodrigues' rotation formula for quaternion update
            val sinHalfAngle = sin(angle / 2)
            val cosHalfAngle = cos(angle / 2)

            val axis = FloatArray(3)
            axis[0] = gyro[0] / magnitude
            axis[1] = gyro[1] / magnitude
            axis[2] = gyro[2] / magnitude

            val dq = FloatArray(4)
            dq[0] = axis[0] * sinHalfAngle
            dq[1] = axis[1] * sinHalfAngle
            dq[2] = axis[2] * sinHalfAngle
            dq[3] = cosHalfAngle

            // Quaternion multiplication
            val newQ = multiplyQuaternions(quaternion, dq)
            System.arraycopy(newQ, 0, quaternion, 0, 4)

            // Normalize
            normalizeQuaternion(quaternion)

            // Convert to rotation matrix
            quaternionToRotationMatrix(quaternion, rotationMatrix)

            // Extract orientation
            SensorManager.getOrientation(rotationMatrix, orientation)
        }
    }

    val azimuth: Float
        get() = Math.toDegrees(orientation[0].toDouble()).toFloat()

    val pitch: Float
        get() = Math.toDegrees(orientation[1].toDouble()).toFloat()

    val roll: Float
        get() = Math.toDegrees(orientation[2].toDouble()).toFloat()

    private fun multiplyQuaternions(q1: FloatArray, q2: FloatArray): FloatArray {
        val result = FloatArray(4)
        result[3] = q1[3] * q2[3] - q1[0] * q2[0] - q1[1] * q2[1] - q1[2] * q2[2]
        result[0] = q1[3] * q2[0] + q1[0] * q2[3] + q1[1] * q2[2] - q1[2] * q2[1]
        result[1] = q1[3] * q2[1] - q1[0] * q2[2] + q1[1] * q2[3] + q1[2] * q2[0]
        result[2] = q1[3] * q2[2] + q1[0] * q2[1] - q1[1] * q2[0] + q1[2] * q2[3]
        return result
    }

    private fun normalizeQuaternion(q: FloatArray) {
        val norm = sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3])
        if (norm > EPSILON) {
            q[0] /= norm
            q[1] /= norm
            q[2] /= norm
            q[3] /= norm
        }
    }

    private fun quaternionToRotationMatrix(q: FloatArray, r: FloatArray) {
        val xx = q[0] * q[0]
        val xy = q[0] * q[1]
        val xz = q[0] * q[2]
        val xw = q[0] * q[3]
        val yy = q[1] * q[1]
        val yz = q[1] * q[2]
        val yw = q[1] * q[3]
        val zz = q[2] * q[2]
        val zw = q[2] * q[3]

        r[0] = 1 - 2 * (yy + zz)
        r[1] = 2 * (xy - zw)
        r[2] = 2 * (xz + yw)
        r[3] = 2 * (xy + zw)
        r[4] = 1 - 2 * (xx + zz)
        r[5] = 2 * (yz - xw)
        r[6] = 2 * (xz - yw)
        r[7] = 2 * (yz + xw)
        r[8] = 1 - 2 * (xx + yy)
    }

    companion object {
        private const val EPSILON = 0.000001f
    }
}
