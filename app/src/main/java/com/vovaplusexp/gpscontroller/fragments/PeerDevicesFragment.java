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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.vovaplusexp.gpscontroller.R;

/**
 * Fragment for managing peer devices
 */
public class PeerDevicesFragment extends Fragment {
    
    private SwitchMaterial peerEnableSwitch;
    private RecyclerView devicesRecyclerView;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_peer_devices, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        peerEnableSwitch = view.findViewById(R.id.peer_enable_switch);
        devicesRecyclerView = view.findViewById(R.id.devices_recycler_view);
        
        devicesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        peerEnableSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Handle peer mode toggle
        });
    }
}
