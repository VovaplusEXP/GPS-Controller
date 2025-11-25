package com.vovaplusexp.gpscontroller.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.vovaplusexp.gpscontroller.bluetooth.BluetoothManager
import com.vovaplusexp.gpscontroller.models.Location
import com.vovaplusexp.gpscontroller.models.PeerEstimate
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class PeerNavigationService : Service() {

    @Inject
    lateinit var bluetoothManager: BluetoothManager

    private val binder = LocalBinder()
    private val peerEstimates = mutableListOf<PeerEstimate>()

    inner class LocalBinder : Binder() {
        fun getService(): PeerNavigationService = this@PeerNavigationService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        Timber.i("PeerNavigationService created")
        bluetoothManager.start()
        bluetoothManager.setPeerDataListener(::onPeerDataReceived)
    }

    private fun onPeerDataReceived(estimate: PeerEstimate) {
        synchronized(peerEstimates) {
            peerEstimates.removeAll { it.deviceId == estimate.deviceId }
            peerEstimates.add(estimate)
        }
    }

    fun getFusedLocation(): Location? {
        synchronized(peerEstimates) {
            if (peerEstimates.isEmpty()) return null

            var weightedLat = 0.0
            var weightedLon = 0.0
            var totalWeight = 0.0

            for (estimate in peerEstimates) {
                val weight = estimate.confidence.toDouble()
                weightedLat += estimate.location.latitude * weight
                weightedLon += estimate.location.longitude * weight
                totalWeight += weight
            }

            if (totalWeight == 0.0) return null

            val fused = Location(weightedLat / totalWeight, weightedLon / totalWeight)
            fused.source = Location.LocationSource.PEER_FUSED
            fused.confidence = (totalWeight / peerEstimates.size).toFloat()
            return fused
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager.stop()
        Timber.i("PeerNavigationService destroyed")
    }
}
