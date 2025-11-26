package com.vovaplusexp.gpscontroller.models

data class RoadSegment(
    val id: Long,
    val startLat: Double,
    val startLon: Double,
    val endLat: Double,
    val endLon: Double
)
