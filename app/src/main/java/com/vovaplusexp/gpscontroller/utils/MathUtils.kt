package com.vovaplusexp.gpscontroller.utils

import kotlin.math.*

fun lowPassFilter(input: FloatArray, output: FloatArray?, alpha: Float): FloatArray {
    val result = output ?: FloatArray(input.size)
    for (i in input.indices) {
        result[i] = result[i] + alpha * (input[i] - result[i])
    }
    return result
}

fun removeGravity(accel: FloatArray, gravity: FloatArray): FloatArray {
    val linearAcceleration = FloatArray(3)
    linearAcceleration[0] = accel[0] - gravity[0]
    linearAcceleration[1] = accel[1] - gravity[1]
    linearAcceleration[2] = accel[2] - gravity[2]
    return linearAcceleration
}

fun FloatArray.magnitude(): Float {
    return sqrt(this[0] * this[0] + this[1] * this[1] + this[2] * this[2])
}


object MathUtils {

    fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    fun projectToSegment(px: Double, py: Double, ax: Double, ay: Double, bx: Double, by: Double): DoubleArray {
        val abx = bx - ax
        val aby = by - ay
        val apx = px - ax
        val apy = py - ay
        val ab2 = abx * abx + aby * aby
        val ap_ab = apx * abx + apy * aby
        var t = ap_ab / ab2
        t = max(0.0, min(1.0, t))
        return doubleArrayOf(ax + t * abx, ay + t * aby)
    }

    fun angleDifference(angle1: Float, angle2: Float): Float {
        var diff = (angle2 - angle1 + 180) % 360 - 180
        if (diff < -180) diff += 360
        return diff
    }

    fun rotateToWorldFrame(vector: FloatArray, rotationMatrix: FloatArray): FloatArray {
        val result = FloatArray(3)
        result[0] = rotationMatrix[0] * vector[0] + rotationMatrix[1] * vector[1] + rotationMatrix[2] * vector[2]
        result[1] = rotationMatrix[3] * vector[0] + rotationMatrix[4] * vector[1] + rotationMatrix[5] * vector[2]
        result[2] = rotationMatrix[6] * vector[0] + rotationMatrix[7] * vector[1] + rotationMatrix[8] * vector[2]
        return result
    }
}
