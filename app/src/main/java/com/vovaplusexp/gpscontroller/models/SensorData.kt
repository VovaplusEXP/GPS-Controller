package com.vovaplusexp.gpscontroller.models

data class SensorData(
    val accelerometer: FloatArray,
    val gyroscope: FloatArray,
    val magnetic: FloatArray,
    val timestamp: Long
)
