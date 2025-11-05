/*
 * GPS-Controller - Inertial Navigation with GPS Spoofing Detection
 * Copyright (C) 2024 VovaplusEXP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.vovaplusexp.gpscontroller.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.vovaplusexp.gpscontroller.R;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

/**
 * Fragment displaying the map with current location
 */
public class MapFragment extends Fragment {
    
    private MapView mapView;
    private Marker locationMarker;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        
        mapView = view.findViewById(R.id.map_view);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        
        // Set default location (Moscow)
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(new GeoPoint(55.7558, 37.6173));
        
        // Add location marker
        locationMarker = new Marker(mapView);
        locationMarker.setPosition(new GeoPoint(55.7558, 37.6173));
        locationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        locationMarker.setTitle("Current Location");
        mapView.getOverlays().add(locationMarker);
    }
    
    public void updateLocation(double latitude, double longitude) {
        if (locationMarker != null) {
            GeoPoint point = new GeoPoint(latitude, longitude);
            locationMarker.setPosition(point);
            mapView.getController().animateTo(point);
            mapView.invalidate();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }
}
