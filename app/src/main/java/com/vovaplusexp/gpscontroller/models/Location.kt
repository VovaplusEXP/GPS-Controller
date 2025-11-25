package com.vovaplusexp.gpscontroller.models

import android.location.Location as AndroidLocation

data class Location(
    @JvmField var latitude: Double,
    @JvmField var longitude: Double,
    @JvmField var speed: Float,
    @JvmField var bearing: Float,
    @JvmField var timestamp: Long,
    @JvmField var source: LocationSource,
    @JvmField var confidence: Float,
    @JvmField var altitude: Float = 0f,
    @JvmField var isFromMockProvider: Boolean = false,
    @JvmField var provider: String? = null
) {
    constructor(latitude: Double, longitude: Double) : this(
        latitude = latitude,
        longitude = longitude,
        speed = 0f,
        bearing = 0f,
        timestamp = System.currentTimeMillis(),
        source = LocationSource.GPS,
        confidence = 1.0f
    )

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
