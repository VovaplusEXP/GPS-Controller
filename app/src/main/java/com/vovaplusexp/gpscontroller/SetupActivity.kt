/*
 * GPS-Controller - Inertial Navigation with GPS Spoofing Detection
 * Copyright (C) 2024 VovaplusEXP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.vovaplusexp.gpscontroller

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.vovaplusexp.gpscontroller.map.MapDownloadManager
import com.vovaplusexp.gpscontroller.utils.PermissionHelper
import com.vovaplusexp.gpscontroller.utils.PreferencesManager
import java.io.File

/**
 * First-run setup activity
 */
class SetupActivity : AppCompatActivity() {

    private lateinit var welcomeText: TextView
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var nextButton: MaterialButton
    private lateinit var skipButton: MaterialButton

    private lateinit var prefsManager: PreferencesManager
    private lateinit var mapDownloadManager: MapDownloadManager

    private var currentStep = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefsManager = PreferencesManager(this)

        // Skip setup if already completed
        if (prefsManager.isSetupComplete) {
            startMainActivity()
            return
        }

        setContentView(R.layout.activity_setup)

        initializeViews()
        mapDownloadManager = MapDownloadManager(this)

        showStep(0)
    }

    private fun initializeViews() {
        welcomeText = findViewById(R.id.welcome_text)
        statusText = findViewById(R.id.status_text)
        progressBar = findViewById(R.id.progress_bar)
        nextButton = findViewById(R.id.next_button)
        skipButton = findViewById(R.id.skip_button)

        nextButton.setOnClickListener { nextStep() }
        skipButton.setOnClickListener { skipStep() }
    }

    private fun showStep(step: Int) {
        currentStep = step

        when (step) {
            0 -> { // Welcome
                welcomeText.setText(R.string.setup_welcome)
                statusText.setText(R.string.setup_description)
                nextButton.setText(R.string.setup_next)
                skipButton.visibility = View.GONE
                progressBar.visibility = View.GONE
            }
            1 -> { // Permissions
                welcomeText.setText(R.string.setup_permissions)
                statusText.setText(R.string.setup_location_permission)
                nextButton.setText(R.string.setup_next)
                skipButton.visibility = View.GONE
                progressBar.visibility = View.GONE
            }
            2 -> { // Map download
                welcomeText.setText(R.string.setup_map_download)
                statusText.setText(R.string.setup_map_optional)
                nextButton.setText(R.string.setup_download)
                skipButton.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
            }
            3 -> { // Calibration
                welcomeText.setText(R.string.setup_calibration)
                statusText.setText(R.string.setup_calibration_instruction)
                nextButton.setText(R.string.setup_start)
                skipButton.visibility = View.GONE
                progressBar.visibility = View.GONE
            }
            4 -> { // Complete
                completeSetup()
            }
        }
    }

    private fun nextStep() {
        when (currentStep) {
            0 -> showStep(1)
            1 -> requestPermissions()
            2 -> downloadMap()
            3 -> showStep(4)
        }
    }

    private fun skipStep() {
        if (currentStep == 2) {
            showStep(3)
        }
    }

    private fun requestPermissions() {
        if (!PermissionHelper.hasLocationPermission(this)) {
            PermissionHelper.requestLocationPermission(this)
        } else {
            showStep(2)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (PermissionHelper.hasLocationPermission(this)) {
            showStep(2)
        } else {
            Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadMap() {
        progressBar.visibility = View.VISIBLE
        nextButton.isEnabled = false
        statusText.setText(R.string.downloading)

        mapDownloadManager.setDownloadListener(object : MapDownloadManager.DownloadListener {
            override fun onProgress(percent: Int) {
                progressBar.progress = percent
                statusText.text = getString(R.string.downloading) + " " + percent + "%"
            }

            override fun onComplete(file: File) {
                prefsManager.isMapDownloaded = true
                statusText.text = "Map downloaded successfully"
                nextButton.isEnabled = true
                showStep(3)
            }

            override fun onError(error: String) {
                statusText.text = "Download failed: $error"
                nextButton.isEnabled = true
                progressBar.visibility = View.GONE
            }
        })

        mapDownloadManager.downloadMap()
    }

    private fun completeSetup() {
        prefsManager.isSetupComplete = true
        startMainActivity()
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
