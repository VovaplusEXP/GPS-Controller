package com.vovaplusexp.gpscontroller.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.SensorManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.vovaplusexp.gpscontroller.R
import com.vovaplusexp.gpscontroller.models.Location
import com.vovaplusexp.gpscontroller.models.SensorData
import com.vovaplusexp.gpscontroller.sensors.*
import com.vovaplusexp.gpscontroller.utils.MathUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class InertialNavigationService : Service() {

    @Inject
    lateinit var sensorManager: SensorManager

    private lateinit var sensorProcessor: SensorDataProcessor
    private lateinit var orientationTracker: OrientationTracker
    private lateinit var movementDetector: MovementDetector

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val stateMutex = Mutex()

    // State
    private var currentLocation: Location? = null
    private var velocity = floatArrayOf(0f, 0f, 0f) // m/s in world frame
    private var lastUpdateTime: Long = 0

    private val _locationFlow = MutableStateFlow<Location?>(null)
    val locationFlow = _locationFlow.asStateFlow()

    inner class LocalBinder : Binder() {
        fun getService(): InertialNavigationService = this@InertialNavigationService
    }

    override fun onCreate() {
        super.onCreate()
        Timber.i("InertialNavigationService creating")

        sensorProcessor = SensorDataProcessor(sensorManager)
        orientationTracker = OrientationTracker()
        movementDetector = MovementDetector()

        sensorProcessor.setSensorDataListener(::onSensorDataUpdated)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.i("InertialNavigationService started")
        sensorProcessor.start()
        return START_STICKY
    }

    suspend fun initializeLocation(location: Location) {
        stateMutex.withLock {
            this.currentLocation = location.copy()
            this.velocity = floatArrayOf(0f, 0f, 0f)
            this.lastUpdateTime = System.currentTimeMillis()
            _locationFlow.value = this.currentLocation
            Timber.i("Initialized inertial navigation at: %.6f, %.6f",
                location.latitude, location.longitude)
        }
    }

    private fun onSensorDataUpdated(data: SensorData) {
        serviceScope.launch {
            updateInertialNavigation(data)
        }
    }

    private suspend fun updateInertialNavigation(data: SensorData) {
        val currentTime = System.currentTimeMillis()
        val localLastUpdateTime = lastUpdateTime
        val dt = (currentTime - localLastUpdateTime) / 1000.0f

        if (localLastUpdateTime == 0L || dt > 1.0f) {
            // Skip first update or if too much time passed
            stateMutex.withLock {
                lastUpdateTime = currentTime
            }
            return
        }

        // Update orientation
        orientationTracker.updateFromGravityAndMagnetic(sensorProcessor.gravity, data.magnetic)
        orientationTracker.updateFromGyroscope(data.gyroscope, dt)

        val linearAccel = sensorProcessor.linearAcceleration
        movementDetector.update(linearAccel)

        val worldAccel = MathUtils.rotateToWorldFrame(linearAccel, orientationTracker.rotationMatrix)

        stateMutex.withLock {
            val currentVel = velocity
            if (movementDetector.isStationary) {
                currentVel[0] = 0f
                currentVel[1] = 0f
                currentVel[2] = 0f
            } else {
                currentVel[0] += worldAccel[0] * dt
                currentVel[1] += worldAccel[1] * dt
                currentVel[2] += worldAccel[2] * dt
            }

            currentLocation?.let { loc ->
                val latChange = (currentVel[1] * dt) / 111000.0
                val lonChange = (currentVel[0] * dt) / (111000.0 * Math.cos(Math.toRadians(loc.latitude)))

                loc.latitude += latChange
                loc.longitude += lonChange
                loc.timestamp = currentTime
                loc.source = Location.LocationSource.INERTIAL

                val speed = Math.sqrt((currentVel[0] * currentVel[0] + currentVel[1] * currentVel[1]).toDouble()).toFloat()
                loc.speed = speed
                loc.bearing = orientationTracker.azimuth

                _locationFlow.value = loc.copy()
            }
            lastUpdateTime = currentTime
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("InertialNavigationService destroyed")
        sensorProcessor.stop()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent): IBinder = binder

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Inertial Navigation", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GPS-Controller")
            .setContentText("Inertial navigation active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "InertialNavigationChannel"
        private const val NOTIFICATION_ID = 1001
    }
}
