package com.vovaplusexp.gpscontroller.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Binder
import android.os.IBinder
import com.vovaplusexp.gpscontroller.models.Location as AppLocation
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MockLocationService : Service() {

    @Inject
    lateinit var locationManager: LocationManager

    private val binder = LocalBinder()
    private val providerName = LocationManager.GPS_PROVIDER

    inner class LocalBinder : Binder() {
        fun getService(): MockLocationService = this@MockLocationService
    }

    override fun onBind(intent: Intent): IBinder = binder

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()
        try {
            locationManager.addTestProvider(
                providerName,
                false,
                false,
                false,
                false,
                true,
                true,
                true,
                0,
                5
            )
            locationManager.setTestProviderEnabled(providerName, true)
            Timber.i("Mock location provider added and enabled")
        } catch (e: SecurityException) {
            Timber.e(e, "Failed to add mock location provider")
        }
    }

    fun updateLocation(location: AppLocation) {
        try {
            val mockLocation = Location(providerName).apply {
                latitude = location.latitude
                longitude = location.longitude
                altitude = location.altitude.toDouble()
                speed = location.speed
                bearing = location.bearing
                accuracy = location.confidence * 10 // Approximate
                time = System.currentTimeMillis()
                elapsedRealtimeNanos = System.nanoTime()
            }
            locationManager.setTestProviderLocation(providerName, mockLocation)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set mock location")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            locationManager.removeTestProvider(providerName)
            Timber.i("Mock location provider removed")
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove mock location provider")
        }
    }
}
