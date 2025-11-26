/*
 * GPS-Controller - Inertial Navigation with GPS Spoofing Detection
 * Copyright (C) 2024 VovaplusEXP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.vovaplusexp.gpscontroller.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.vovaplusexp.gpscontroller.R

/**
 * Fragment showing diagnostic information
 */
class DiagnosticsFragment : Fragment() {

    private lateinit var sensorDataText: TextView
    private lateinit var eventLogText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_diagnostics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sensorDataText = view.findViewById(R.id.sensor_data_text)
        eventLogText = view.findViewById(R.id.event_log_text)

        sensorDataText.text = "Sensor data will appear here..."
        eventLogText.text = "Event log will appear here..."
    }
}
