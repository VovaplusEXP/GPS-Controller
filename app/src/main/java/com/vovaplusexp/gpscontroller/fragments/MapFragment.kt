package com.vovaplusexp.gpscontroller.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.vovaplusexp.gpscontroller.R
import com.vovaplusexp.gpscontroller.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@AndroidEntryPoint
class MapFragment : Fragment() {

    private lateinit var mapView: MapView
    private var locationMarker: Marker? = null
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        Configuration.getInstance().load(context, androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()))

        mapView = view.findViewById(R.id.map_view)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.controller.setZoom(15.0)

        locationMarker = Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapView.overlays.add(locationMarker)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.locationFlow.collect { location ->
                    location?.let {
                        val geoPoint = GeoPoint(it.latitude, it.longitude)
                        locationMarker?.position = geoPoint
                        mapView.controller.animateTo(geoPoint)
                        mapView.invalidate()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}
