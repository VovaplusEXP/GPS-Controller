/*
 * GPS-Controller - Inertial Navigation with GPS Spoofing Detection
 * Copyright (C) 2024 VovaplusEXP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.vovaplusexp.gpscontroller.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import com.vovaplusexp.gpscontroller.R;
import com.vovaplusexp.gpscontroller.bluetooth.*;
import com.vovaplusexp.gpscontroller.models.Location;
import com.vovaplusexp.gpscontroller.models.PeerEstimate;
import timber.log.Timber;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for P2P peer navigation synchronization
 */
public class PeerNavigationService extends Service {
    
    private static final String CHANNEL_ID = "PeerNavigationChannel";
    private static final int NOTIFICATION_ID = 1002;
    private static final long MAX_PACKET_AGE_MS = 5000;
    
    private final IBinder binder = new LocalBinder();
    private BluetoothManager bluetoothManager;
    private final List<PeerEstimate> peerEstimates = new ArrayList<>();
    
    public class LocalBinder extends Binder {
        public PeerNavigationService getService() {
            return PeerNavigationService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Timber.i("PeerNavigationService created");
        
        bluetoothManager = new BluetoothManager(this);
        bluetoothManager.setBluetoothListener(new BluetoothManager.BluetoothListener() {
            @Override
            public void onDeviceDiscovered(PeerDevice device) {
                Timber.i("Peer discovered: %s", device.getDeviceName());
            }
            
            @Override
            public void onDeviceConnected(PeerDevice device) {
                Timber.i("Peer connected: %s", device.getDeviceName());
            }
            
            @Override
            public void onDeviceDisconnected(PeerDevice device) {
                Timber.i("Peer disconnected: %s", device.getDeviceName());
                removePeerEstimates(device.getDeviceId());
            }
            
            @Override
            public void onDataReceived(PeerDevice device, byte[] data) {
                handlePeerData(device, data);
            }
            
            @Override
            public void onError(String error) {
                Timber.e("Bluetooth error: %s", error);
            }
        });
        
        // Start server for incoming connections
        if (bluetoothManager.isBluetoothAvailable()) {
            bluetoothManager.startServer();
            Timber.i("Bluetooth server started for P2P");
        }
        
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    /**
     * Handle data received from peer
     */
    private void handlePeerData(PeerDevice device, byte[] data) {
        try {
            PeerSyncProtocol.PeerPacket packet = PeerSyncProtocol.PeerPacket.fromBytes(data);
            
            if (!packet.isExpired(MAX_PACKET_AGE_MS)) {
                Location location = packet.toLocation();
                PeerEstimate estimate = new PeerEstimate(
                    device.getDeviceId(),
                    location,
                    packet.confidence
                );
                
                synchronized (peerEstimates) {
                    peerEstimates.add(estimate);
                    // Remove old estimates
                    peerEstimates.removeIf(e -> e.isExpired(MAX_PACKET_AGE_MS));
                }
            }
        } catch (Exception e) {
            Timber.e(e, "Error processing peer data");
        }
    }
    
    /**
     * Broadcast location to peers
     */
    public void broadcastLocation(Location location, long deviceId) {
        PeerSyncProtocol.PeerPacket packet = new PeerSyncProtocol.PeerPacket(location, deviceId);
        byte[] data = packet.toBytes();
        
        for (PeerDevice device : bluetoothManager.getDiscoveredDevices()) {
            if (device.isConnected()) {
                bluetoothManager.sendData(device, data);
            }
        }
    }
    
    /**
     * Fuse estimates from multiple peers
     */
    public Location fusePeerEstimates() {
        synchronized (peerEstimates) {
            if (peerEstimates.isEmpty()) {
                return null;
            }
            
            if (peerEstimates.size() == 1) {
                return peerEstimates.get(0).getLocation();
            }
            
            double totalWeight = 0;
            double weightedLat = 0;
            double weightedLon = 0;
            
            for (PeerEstimate estimate : peerEstimates) {
                float weight = estimate.getConfidence();
                weightedLat += estimate.getLocation().getLatitude() * weight;
                weightedLon += estimate.getLocation().getLongitude() * weight;
                totalWeight += weight;
            }
            
            Location fused = new Location(
                weightedLat / totalWeight,
                weightedLon / totalWeight
            );
            fused.setSource(Location.LocationSource.PEER_FUSED);
            fused.setConfidence((float) (totalWeight / peerEstimates.size()));
            
            return fused;
        }
    }
    
    private void removePeerEstimates(String deviceId) {
        synchronized (peerEstimates) {
            peerEstimates.removeIf(e -> e.getDeviceId().equals(deviceId));
        }
    }
    
    public BluetoothManager getBluetoothManager() {
        return bluetoothManager;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bluetoothManager != null) {
            bluetoothManager.stopServer();
            bluetoothManager.cleanup();
        }
        Timber.i("PeerNavigationService destroyed");
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Peer Navigation",
                NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    
    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GPS-Controller")
            .setContentText("P2P sync active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }
}
