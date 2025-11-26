package com.vovaplusexp.gpscontroller.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vovaplusexp.gpscontroller.data.LocationRepository
import com.vovaplusexp.gpscontroller.models.Location
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    val locationFlow: StateFlow<Location?> = locationRepository.getLocationFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val statusFlow: StateFlow<String> = locationRepository.getStatusFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, "Idle")

    fun startUpdates() {
        locationRepository.startLocationUpdates()
    }

    fun stopUpdates() {
        locationRepository.stopLocationUpdates()
    }
}
