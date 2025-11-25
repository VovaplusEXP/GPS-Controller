package com.vovaplusexp.gpscontroller.services

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.vovaplusexp.gpscontroller.models.Location
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationServiceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _inertialService = MutableStateFlow<InertialNavigationService?>(null)
    val inertialServiceFlow = _inertialService.asStateFlow()

    private val _mockLocationService = MutableStateFlow<MockLocationService?>(null)
    val mockLocationServiceFlow = _mockLocationService.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val inertialConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as InertialNavigationService.LocalBinder
            _inertialService.value = binder.getService()
            Timber.i("InertialNavigationService connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            _inertialService.value = null
            Timber.i("InertialNavigationService disconnected")
        }
    }

    private val mockLocationConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MockLocationService.LocalBinder
            _mockLocationService.value = binder.getService()
            Timber.i("MockLocationService connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            _mockLocationService.value = null
            Timber.i("MockLocationService disconnected")
        }
    }

    fun startServices() {
        Intent(context, InertialNavigationService::class.java).also { intent ->
            context.startService(intent)
            context.bindService(intent, inertialConnection, Context.BIND_AUTO_CREATE)
        }
        Intent(context, MockLocationService::class.java).also { intent ->
            context.startService(intent)
            context.bindService(intent, mockLocationConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun stopServices() {
        if (_inertialService.value != null) context.unbindService(inertialConnection)
        if (_mockLocationService.value != null) context.unbindService(mockLocationConnection)

        context.stopService(Intent(context, InertialNavigationService::class.java))
        context.stopService(Intent(context, MockLocationService::class.java))
    }

    fun initializeInertialLocation(location: Location) {
        scope.launch {
            _inertialService.value?.initializeLocation(location)
        }
    }

    fun updateMockLocation(location: Location) {
        _mockLocationService.value?.updateLocation(location)
    }
}
