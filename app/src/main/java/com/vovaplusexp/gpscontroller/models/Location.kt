package com.vovaplusexp.gpscontroller.models

import android.location.Location as AndroidLocation

data class Location(
    var latitude: Double,
    var longitude: Double,
    var speed: Float,
    var bearing: Float,
    var timestamp: Long,
    var source: LocationSource,
    var confidence: Float,
    var altitude: Float = 0f,
    var isFromMockProvider: Boolean = false,
    var provider: String? = null
) {
    enum class LocationSource {
        GPS,
        INERTIAL,
        HYBRID,
        PEER_FUSED,
        MAP_MATCHED
    }

    fun distanceTo(other: Location): Float {
        val results = FloatArray(1)
        AndroidLocation.distanceBetween(latitude, longitude, other.latitude, other.longitude, results)
        return results[0]
    }

    fun hasBearing(): Boolean = bearing != 0f

    companion object {
        @JvmStatic
        fun fromAndroidLocation(loc: AndroidLocation): Location {
            return Location(
                latitude = loc.latitude,
                longitude = loc.longitude,
                speed = loc.speed,
                bearing = loc.bearing,
                timestamp = loc.time,
                source = LocationSource.GPS,
                confidence = loc.accuracy,
                altitude = loc.altitude.toFloat(),
                isFromMockProvider = loc.isFromMockProvider,
                provider = loc.provider
            )
        }
    }
}
