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

/**
 * Represents a peer device in the P2P network
 */
public class PeerDevice {
    private String deviceId;
    private String deviceName;
    private String bluetoothAddress;
    private boolean connected;
    private long lastSeen;
    
    public PeerDevice(String deviceId, String deviceName, String bluetoothAddress) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.bluetoothAddress = bluetoothAddress;
        this.connected = false;
        this.lastSeen = System.currentTimeMillis();
    }
    
    public String getDeviceId() { return deviceId; }
    public String getDeviceName() { return deviceName; }
    public String getBluetoothAddress() { return bluetoothAddress; }
    
    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) {
        this.connected = connected;
        if (connected) {
            this.lastSeen = System.currentTimeMillis();
        }
    }
    
    public long getLastSeen() { return lastSeen; }
    public void updateLastSeen() { this.lastSeen = System.currentTimeMillis(); }
    
    public boolean isStale(long maxAgeMs) {
        return (System.currentTimeMillis() - lastSeen) > maxAgeMs;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PeerDevice)) return false;
        PeerDevice that = (PeerDevice) o;
        return deviceId.equals(that.deviceId);
    }
    
    @Override
    public int hashCode() {
        return deviceId.hashCode();
    }
}
