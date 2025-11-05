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
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import timber.log.Timber;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
    
    // Socket management fields
    private BluetoothServerSocket serverSocket;
    private final Map<String, BluetoothSocket> activeSockets = new HashMap<>();
    private final Map<String, Thread> readThreads = new HashMap<>();
    private AcceptThread acceptThread;
    
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
     */
    public void connectToDevice(PeerDevice device) {
        if (device.isConnected()) {
            Timber.w("Device already connected: %s", device.getDeviceName());
            return;
        }
        
        // Get BluetoothDevice
        try {
            BluetoothDevice btDevice = bluetoothAdapter.getRemoteDevice(
                device.getBluetoothAddress()
            );
            
            // Start connection in separate thread
            ConnectThread connectThread = new ConnectThread(btDevice);
            connectThread.start();
            
        } catch (IllegalArgumentException | SecurityException e) {
            Timber.e(e, "Invalid device address");
            if (listener != null) {
                listener.onError("Cannot connect to device");
            }
        }
    }
    
    /**
     * Disconnect from a peer device
     */
    public void disconnectFromDevice(PeerDevice device) {
        String address = device.getBluetoothAddress();
        
        // Close read thread
        Thread thread = readThreads.remove(address);
        if (thread instanceof ConnectedThread) {
            ((ConnectedThread) thread).cancel();
        }
        
        // Close socket
        BluetoothSocket socket = activeSockets.remove(address);
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                Timber.e(e, "Error closing socket");
            }
        }
        
        device.setConnected(false);
        
        if (listener != null) {
            listener.onDeviceDisconnected(device);
        }
        
        Timber.i("Disconnected from device: %s", device.getDeviceName());
    }
    
    /**
     * Send data to a peer device
     */
    public void sendData(PeerDevice device, byte[] data) {
        if (!device.isConnected()) {
            Timber.w("Device not connected: %s", device.getDeviceName());
            return;
        }
        
        String address = device.getBluetoothAddress();
        Thread thread = readThreads.get(address);
        
        if (thread instanceof ConnectedThread) {
            ((ConnectedThread) thread).write(data);
            device.updateLastSeen();
        } else {
            Timber.w("No connection thread for device: %s", device.getDeviceName());
        }
    }
    
    /**
     * Manage connected socket
     */
    private synchronized void manageConnectedSocket(BluetoothSocket socket) {
        try {
            BluetoothDevice device = socket.getRemoteDevice();
            String address = device.getAddress();
            
            // Save socket
            activeSockets.put(address, socket);
            
            // Create PeerDevice if doesn't exist
            PeerDevice peerDevice = findDeviceByAddress(address);
            if (peerDevice == null) {
                peerDevice = new PeerDevice(
                    address,
                    device.getName() != null ? device.getName() : "Unknown",
                    address
                );
                discoveredDevices.add(peerDevice);
            }
            
            peerDevice.setConnected(true);
            
            // Start read thread
            ConnectedThread connectedThread = new ConnectedThread(socket, address);
            readThreads.put(address, connectedThread);
            connectedThread.start();
            
            // Notify listener
            if (listener != null) {
                listener.onDeviceConnected(peerDevice);
            }
            
        } catch (SecurityException e) {
            Timber.e(e, "Security exception in manageConnectedSocket");
        }
    }
    
    /**
     * Find device by ID
     */
    private PeerDevice findDeviceById(String deviceId) {
        for (PeerDevice device : discoveredDevices) {
            if (device.getDeviceId().equals(deviceId)) {
                return device;
            }
        }
        return null;
    }
    
    /**
     * Find device by Bluetooth address
     */
    private PeerDevice findDeviceByAddress(String address) {
        for (PeerDevice device : discoveredDevices) {
            if (device.getBluetoothAddress().equals(address)) {
                return device;
            }
        }
        return null;
    }
    
    /**
     * Handle connection loss
     */
    private void connectionLost(String deviceId) {
        PeerDevice device = findDeviceById(deviceId);
        if (device != null) {
            device.setConnected(false);
            activeSockets.remove(device.getBluetoothAddress());
            readThreads.remove(device.getBluetoothAddress());
            
            if (listener != null) {
                listener.onDeviceDisconnected(device);
            }
        }
    }
    
    /**
     * Start Bluetooth server for incoming connections
     */
    public void startServer() {
        if (acceptThread != null) {
            Timber.w("Server already running");
            return;
        }
        
        acceptThread = new AcceptThread();
        acceptThread.start();
        Timber.i("Bluetooth server started");
    }
    
    /**
     * Stop Bluetooth server
     */
    public void stopServer() {
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
            Timber.i("Bluetooth server stopped");
        }
    }
    
    /**
     * Get list of discovered devices
     */
    public List<PeerDevice> getDiscoveredDevices() {
        return new ArrayList<>(discoveredDevices);
    }
    
    /**
     * AcceptThread - Server mode for incoming connections
     */
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket serverSocket;
        
        AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                // Create server socket with UUID
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(
                    APP_NAME, UUID.fromString(SERVICE_UUID)
                );
            } catch (IOException | SecurityException e) {
                Timber.e(e, "Socket's listen() method failed");
            }
            serverSocket = tmp;
        }
        
        public void run() {
            BluetoothSocket socket;
            while (true) {
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    Timber.e(e, "Socket's accept() method failed");
                    break;
                }
                
                if (socket != null) {
                    // Connection established
                    manageConnectedSocket(socket);
                }
            }
        }
        
        public void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Timber.e(e, "Could not close the connect socket");
            }
        }
    }
    
    /**
     * ConnectThread - Client mode for outgoing connections
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;
        
        ConnectThread(BluetoothDevice device) {
            this.device = device;
            BluetoothSocket tmp = null;
            
            try {
                // Create socket for connection
                tmp = device.createRfcommSocketToServiceRecord(
                    UUID.fromString(SERVICE_UUID)
                );
            } catch (IOException | SecurityException e) {
                Timber.e(e, "Socket's create() method failed");
            }
            socket = tmp;
        }
        
        public void run() {
            // Stop discovery to speed up connection
            try {
                bluetoothAdapter.cancelDiscovery();
            } catch (SecurityException e) {
                Timber.e(e, "Cannot cancel discovery");
            }
            
            try {
                // Connect (blocking call)
                socket.connect();
            } catch (IOException | SecurityException connectException) {
                try {
                    socket.close();
                } catch (IOException closeException) {
                    Timber.e(closeException, "Could not close socket");
                }
                
                if (listener != null) {
                    listener.onError("Connection failed: " + device.getName());
                }
                return;
            }
            
            // Connection established
            manageConnectedSocket(socket);
        }
        
        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Timber.e(e, "Could not close socket");
            }
        }
    }
    
    /**
     * ConnectedThread - Data exchange thread
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private final String deviceId;
        
        ConnectedThread(BluetoothSocket socket, String deviceId) {
            this.socket = socket;
            this.deviceId = deviceId;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Timber.e(e, "Error getting socket streams");
            }
            
            inputStream = tmpIn;
            outputStream = tmpOut;
        }
        
        public void run() {
            byte[] buffer = new byte[PeerSyncProtocol.PACKET_SIZE];
            
            while (true) {
                try {
                    // Read data (blocking)
                    int bytes = inputStream.read(buffer);
                    
                    if (bytes == PeerSyncProtocol.PACKET_SIZE) {
                        // Complete packet received
                        byte[] packet = new byte[PeerSyncProtocol.PACKET_SIZE];
                        System.arraycopy(buffer, 0, packet, 0, PeerSyncProtocol.PACKET_SIZE);
                        
                        // Find PeerDevice by deviceId
                        PeerDevice device = findDeviceById(deviceId);
                        if (device != null && listener != null) {
                            listener.onDataReceived(device, packet);
                        }
                    }
                } catch (IOException e) {
                    Timber.e(e, "Connection lost");
                    connectionLost(deviceId);
                    break;
                }
            }
        }
        
        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
                outputStream.flush();
            } catch (IOException e) {
                Timber.e(e, "Error writing to stream");
            }
        }
        
        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Timber.e(e, "Could not close socket");
            }
        }
    }
    
    /**
     * Cleanup and release resources
     */
    public void cleanup() {
        stopDiscovery();
        stopServer();
        
        // Close all connections
        for (PeerDevice device : discoveredDevices) {
            if (device.isConnected()) {
                disconnectFromDevice(device);
            }
        }
        
        // Close all threads
        for (Thread thread : readThreads.values()) {
            if (thread instanceof ConnectedThread) {
                ((ConnectedThread) thread).cancel();
            }
        }
        
        activeSockets.clear();
        readThreads.clear();
        discoveredDevices.clear();
    }
}
