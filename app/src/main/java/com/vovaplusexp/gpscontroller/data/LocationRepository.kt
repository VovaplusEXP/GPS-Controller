package com.vovaplusexp.gpscontroller.data

import com.vovaplusexp.gpscontroller.models.Location
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun getLocationFlow(): Flow<Location>
    fun getStatusFlow(): Flow<String>
    fun startLocationUpdates()
    fun stopLocationUpdates()
}
