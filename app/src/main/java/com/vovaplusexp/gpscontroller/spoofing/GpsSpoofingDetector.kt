package com.vovaplusexp.gpscontroller.spoofing

import com.vovaplusexp.gpscontroller.models.Location
import com.vovaplusexp.gpscontroller.utils.MathUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GpsSpoofingDetector @Inject constructor() {

    private var lastGpsLocation: Location? = null

    fun analyzeLocation(
        currentLocation: Location,
        imuSpeed: Float,
        imuBearing: Float
    ): LocationTrust {
        val currentFlags = mutableListOf<SpoofingFlag>()

        if (currentLocation.isFromMockProvider) {
            if (currentLocation.provider != "gps-controller") {
                currentFlags.add(SpoofingFlag.MOCK_PROVIDER)
            }
        }

        lastGpsLocation?.let { lastLoc ->
            val distance = lastLoc.distanceTo(currentLocation)
            val timeDelta = (currentLocation.timestamp - lastLoc.timestamp) / 1000

            if (timeDelta > 0) {
                val apparentSpeed = distance / timeDelta
                if (apparentSpeed > MAX_REALISTIC_SPEED) {
                    currentFlags.add(SpoofingFlag.TELEPORTATION)
                }

                val gpsSpeed = currentLocation.speed
                if (Math.abs(gpsSpeed - imuSpeed) > SPEED_DIFF_THRESHOLD) {
                    currentFlags.add(SpoofingFlag.SPEED_MISMATCH)
                }
            }

            if (currentLocation.hasBearing()) {
                val bearingDiff = Math.abs(
                    MathUtils.angleDifference(currentLocation.bearing, imuBearing)
                )
                if (bearingDiff > BEARING_DIFF_THRESHOLD) {
                    currentFlags.add(SpoofingFlag.BEARING_MISMATCH)
                }
            }
        }

        lastGpsLocation = currentLocation

        val trustLevel = when {
            currentFlags.size >= 3 -> LocationTrust.TrustLevel.SPOOFED
            currentFlags.isNotEmpty() -> LocationTrust.TrustLevel.SUSPICIOUS
            else -> LocationTrust.TrustLevel.TRUSTED
        }

        return LocationTrust(
            trustLevel,
            currentFlags.toList(),
            calculateConfidence(trustLevel)
        )
    }

    private fun calculateConfidence(trustLevel: LocationTrust.TrustLevel): Float {
        return when (trustLevel) {
            LocationTrust.TrustLevel.TRUSTED -> 1.0f
            LocationTrust.TrustLevel.SUSPICIOUS -> 0.5f
            LocationTrust.TrustLevel.SPOOFED -> 0.0f
        }
    }

    enum class SpoofingFlag {
        TELEPORTATION,
        SPEED_MISMATCH,
        BEARING_MISMATCH,
        MOCK_PROVIDER,
        NO_MOVEMENT
    }

    companion object {
        private const val MAX_REALISTIC_SPEED = 300.0f  // m/s (~1080 km/h)
        private const val SPEED_DIFF_THRESHOLD = 10.0f  // m/s
        private const val BEARING_DIFF_THRESHOLD = 45.0f // degrees
    }
}
