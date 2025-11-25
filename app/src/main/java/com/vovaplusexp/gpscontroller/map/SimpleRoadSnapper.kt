package com.vovaplusexp.gpscontroller.map

import com.vovaplusexp.gpscontroller.models.Location
import com.vovaplusexp.gpscontroller.models.RoadSegment
import com.vovaplusexp.gpscontroller.utils.MathUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimpleRoadSnapper @Inject constructor(private val roadDatabase: RoadDatabase) {

    fun snapLocation(location: Location): Location? {
        val nearbyRoads = roadDatabase.findNearestRoads(location.latitude, location.longitude, SEARCH_RADIUS)
        if (nearbyRoads.isEmpty()) return null

        var bestSnap: Location? = null
        var minDistance = Double.MAX_VALUE

        for (road in nearbyRoads) {
            val closestPoint = MathUtils.projectToSegment(
                location.latitude,
                location.longitude,
                road.startLat,
                road.startLon,
                road.endLat,
                road.endLon
            )
            val distance = MathUtils.haversineDistance(location.latitude, location.longitude, closestPoint[0], closestPoint[1])

            if (distance < minDistance && distance < MAX_SNAP_DISTANCE) {
                minDistance = distance
                bestSnap = Location(
                    latitude = closestPoint[0],
                    longitude = closestPoint[1],
                    speed = location.speed,
                    bearing = calculateBearing(road),
                    timestamp = location.timestamp,
                    source = Location.LocationSource.MAP_MATCHED,
                    confidence = location.confidence
                )
            }
        }
        return bestSnap
    }

    private fun calculateBearing(road: RoadSegment): Float {
        val y = road.endLon - road.startLon
        val x = road.endLat - road.startLat
        return (Math.toDegrees(Math.atan2(y, x)) + 360).toFloat() % 360
    }

    companion object {
        private const val SEARCH_RADIUS = 50.0 // meters
        private const val MAX_SNAP_DISTANCE = 30.0 // meters
    }
}
