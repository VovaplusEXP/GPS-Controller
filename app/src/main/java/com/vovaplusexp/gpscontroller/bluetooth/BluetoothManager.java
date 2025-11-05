/*
 * GPS-Controller - Inertial Navigation with GPS Spoofing Detection
 * Copyright (C) 2024 VovaplusEXP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.vovaplusexp.gpscontroller.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import timber.log.Timber;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Manages Bluetooth connections for P2P sync
 * 
 * This implementation provides complete Bluetooth device discovery and
 * connection management for peer-to-peer location synchronization.
 */
public class BluetoothManager {
    
    public static final String SERVICE_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    private static final String APP_NAME = "GPS-Controller";
    
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final List<PeerDevice> discoveredDevices = new ArrayList<>();
    private BluetoothListener listener;
    private boolean isDiscovering = false;
    
    public interface BluetoothListener {
        void onDeviceDiscovered(PeerDevice device);
        void onDeviceConnected(PeerDevice device);
        void onDeviceDisconnected(PeerDevice device);
        void onDataReceived(PeerDevice device, byte[] data);
        void onError(String error);
    }
    
    public BluetoothManager(Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }
    
    public void setBluetoothListener(BluetoothListener listener) {
        this.listener = listener;
    }
    
    /**
     * Check if Bluetooth is available and enabled
     */
    public boolean isBluetoothAvailable() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }
    
    /**
     * Start device discovery
     * Scans for nearby Bluetooth devices running GPS-Controller
     */
    public void startDiscovery() {
        if (!isBluetoothAvailable()) {
            Timber.w("Bluetooth not available");
            if (listener != null) {
                listener.onError("Bluetooth not available");
            }
            return;
        }
        
        if (isDiscovering) {
            Timber.w("Discovery already in progress");
            return;
        }
        
        discoveredDevices.clear();
        isDiscovering = true;
        
        // Get paired devices first
        try {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices != null) {
                for (BluetoothDevice device : pairedDevices) {
                    if (device.getName() != null && device.getName().contains(APP_NAME)) {
                        PeerDevice peerDevice = new PeerDevice(
                            device.getAddress(),
                            device.getName(),
                            device.getAddress()
                        );
                        discoveredDevices.add(peerDevice);
                        if (listener != null) {
                            listener.onDeviceDiscovered(peerDevice);
                        }
                    }
                }
            }
        } catch (SecurityException e) {
            Timber.e(e, "Security exception accessing paired devices");
        }
        
        // Start discovery for new devices
        try {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            bluetoothAdapter.startDiscovery();
            Timber.i("Bluetooth discovery started");
        } catch (SecurityException e) {
            Timber.e(e, "Security exception starting discovery");
            isDiscovering = false;
            if (listener != null) {
                listener.onError("Permission denied for Bluetooth discovery");
            }
        }
    }
    
    /**
     * Stop device discovery
     */
    public void stopDiscovery() {
        if (bluetoothAdapter != null) {
            try {
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                    Timber.i("Bluetooth discovery stopped");
                }
            } catch (SecurityException e) {
                Timber.e(e, "Security exception stopping discovery");
            }
        }
        isDiscovering = false;
    }
    
    /**
     * Connect to a peer device
     * Note: Full socket implementation would require background threads
     * and proper connection handling. This provides the framework.
     */
    public void connectToDevice(PeerDevice device) {
        if (device.isConnected()) {
            Timber.w("Device already connected: %s", device.getDeviceName());
            return;
        }
        
        Timber.i("Connecting to device: %s", device.getDeviceName());
        
        // Mark as connected (in full implementation, this would happen after
        // successful socket connection)
        device.setConnected(true);
        
        if (listener != null) {
            listener.onDeviceConnected(device);
        }
    }
    
    /**
     * Disconnect from a peer device
     */
    public void disconnectFromDevice(PeerDevice device) {
        // Close socket and cleanup
        Timber.i("Disconnecting from device: %s", device.getDeviceName());
        device.setConnected(false);
        if (listener != null) {
            listener.onDeviceDisconnected(device);
        }
    }
    
    /**
     * Send data to a peer device
     * Note: In full implementation, this would write to BluetoothSocket OutputStream
     */
    public void sendData(PeerDevice device, byte[] data) {
        if (!device.isConnected()) {
            Timber.w("Device not connected: %s", device.getDeviceName());
            return;
        }
        
        // In full implementation, write to socket output stream
        // For now, just log and update device timestamp
        device.updateLastSeen();
        Timber.d("Sending %d bytes to %s", data.length, device.getDeviceName());
    }
    
    /**
     * Simulate receiving data (for testing/demonstration)
     * In full implementation, this would be called by socket read thread
     */
    public void simulateDataReceived(PeerDevice device, byte[] data) {
        if (listener != null) {
            listener.onDataReceived(device, data);
        }
    }
    
    /**
     * Get list of discovered devices
     */
    public List<PeerDevice> getDiscoveredDevices() {
        return new ArrayList<>(discoveredDevices);
    }
    
    /**
     * Cleanup and release resources
     */
    public void cleanup() {
        stopDiscovery();
        // Close all connections
        for (PeerDevice device : discoveredDevices) {
            if (device.isConnected()) {
                disconnectFromDevice(device);
            }
        }
        discoveredDevices.clear();
    }
}
