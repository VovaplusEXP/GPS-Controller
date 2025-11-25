package com.vovaplusexp.gpscontroller.ui

import app.cash.turbine.test
import com.vovaplusexp.gpscontroller.data.LocationRepository
import com.vovaplusexp.gpscontroller.models.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var locationRepository: LocationRepository
    private lateinit var viewModel: MainViewModel

    private val locationFlow = MutableStateFlow<Location?>(null)
    private val statusFlow = MutableStateFlow("Idle")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        locationRepository = mock()
        whenever(locationRepository.getLocationFlow()) doReturn locationFlow
        whenever(locationRepository.getStatusFlow()) doReturn statusFlow
        viewModel = MainViewModel(locationRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `locationFlow should emit location from repository`() = runTest {
        val testLocation = Location(1.0, 2.0, 0f, 0f, 0L, Location.LocationSource.GPS, 1.0f)

        viewModel.locationFlow.test {
            assertEquals(null, awaitItem()) // Initial value

            locationFlow.value = testLocation
            assertEquals(testLocation, awaitItem())
        }
    }

    @Test
    fun `statusFlow should emit status from repository`() = runTest {
        viewModel.statusFlow.test {
            assertEquals("Idle", awaitItem()) // Initial value

            statusFlow.value = "GPS Enabled"
            assertEquals("GPS Enabled", awaitItem())
        }
    }
}
