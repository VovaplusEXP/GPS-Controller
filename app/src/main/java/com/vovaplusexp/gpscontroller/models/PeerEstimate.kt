package com.vovaplusexp.gpscontroller.models

data class PeerEstimate(
    val location: Location,
    val deviceId: Long,
    val timestamp: Long
) {
    val confidence: Float
        get() = location.confidence
}
