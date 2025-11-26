package com.vovaplusexp.gpscontroller.ui

import app.cash.turbine.test
import com.vovaplusexp.gpscontroller.data.LocationRepository
import com.vovaplusexp.gpscontroller.models.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExperimentalCoroutinesApi
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var locationRepository: LocationRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        locationRepository = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `location updates are reflected in locationFlow`() = runTest {
        val testLocation = Location(
            latitude = 55.7558,
            longitude = 37.6173,
            altitude = 150.0f,
            speed = 20.0f,
            bearing = 90.0f,
            timestamp = System.currentTimeMillis(),
            provider = "gps",
            source = Location.LocationSource.GPS,
            confidence = 1.0f
        )
        val locationFlow = flowOf(null, testLocation)
        whenever(locationRepository.getLocationFlow()).thenReturn(locationFlow)

        viewModel = MainViewModel(locationRepository)

        viewModel.locationFlow.test {
            assertNull(awaitItem())
            assertEquals(testLocation, awaitItem())
        }
    }
}
