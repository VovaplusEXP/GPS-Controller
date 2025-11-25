package com.vovaplusexp.gpscontroller.data

import android.annotation.SuppressLint
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.vovaplusexp.gpscontroller.models.Location
import com.vovaplusexp.gpscontroller.services.NavigationServiceManager
import com.vovaplusexp.gpscontroller.spoofing.GpsSpoofingDetector
import com.vovaplusexp.gpscontroller.spoofing.LocationTrust
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import android.location.Location as AndroidLocation

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val locationManager: LocationManager,
    private val serviceManager: NavigationServiceManager,
    private val spoofingDetector: GpsSpoofingDetector
) : LocationRepository {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _locationFlow = MutableStateFlow<Location?>(null)
    private val _statusFlow = MutableStateFlow("Idle")

    private var inertialLocation: Location? = null
    private var isSpoofing = false

    private val gpsLocationListener = object : LocationListener {
        override fun onLocationChanged(location: AndroidLocation) {
            scope.launch {
                handleGpsLocation(Location.fromAndroidLocation(location))
            }
        }

        override fun onProviderEnabled(provider: String) {
            _statusFlow.value = "GPS enabled"
        }

        override fun onProviderDisabled(provider: String) {
            _statusFlow.value = "GPS disabled"
        }
    }

    override fun getLocationFlow(): Flow<Location?> = _locationFlow.asStateFlow()
    override fun getStatusFlow(): Flow<String> = _statusFlow.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    @SuppressLint("MissingPermission")
    override fun startLocationUpdates() {
        _statusFlow.value = "Starting services..."
        serviceManager.startServices()
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0f, gpsLocationListener)

        scope.launch {
            serviceManager.inertialServiceFlow
                .flatMapLatest { service ->
                    service?.locationFlow ?: flowOf(null)
                }
                .collect { location ->
                    inertialLocation = location
                    if (location != null && isSpoofing) {
                        updateFinalLocation(location)
                    }
                }
        }
        _statusFlow.value = "Services started"
    }

    override fun stopLocationUpdates() {
        locationManager.removeUpdates(gpsLocationListener)
        serviceManager.stopServices()
        _statusFlow.value = "Services stopped"
    }

    private fun handleGpsLocation(location: Location) {
        val imuSpeed = inertialLocation?.speed ?: 0f
        val imuBearing = inertialLocation?.bearing ?: 0f

        val trust = spoofingDetector.analyzeLocation(location, imuSpeed, imuBearing)

        when (trust.trustLevel) {
            LocationTrust.TrustLevel.TRUSTED -> {
                isSpoofing = false
                _statusFlow.value = "GPS: Normal"
                updateFinalLocation(location)
                serviceManager.initializeInertialLocation(location)
            }
            LocationTrust.TrustLevel.SUSPICIOUS -> {
                isSpoofing = false
                _statusFlow.value = "GPS: Suspicious - ${trust.description}"
                updateFinalLocation(location)
            }
            LocationTrust.TrustLevel.SPOOFED -> {
                isSpoofing = true
                _statusFlow.value = "GPS: SPOOFED! ${trust.description}"
                // Don't update from GPS, rely on inertial flow
            }
        }
    }

    private fun updateFinalLocation(location: Location) {
        _locationFlow.value = location
        serviceManager.updateMockLocation(location)
    }
}
