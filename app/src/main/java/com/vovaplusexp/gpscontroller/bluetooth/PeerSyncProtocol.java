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

import com.vovaplusexp.gpscontroller.models.Location;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Protocol for P2P communication between devices
 */
public class PeerSyncProtocol {
    
    public static final int PACKET_SIZE = 28; // bytes
    
    /**
     * Packet structure (28 bytes total):
     * - timestamp: 8 bytes (long)
     * - latitude: 4 bytes (float)
     * - longitude: 4 bytes (float)
     * - confidence: 4 bytes (float)
     * - deviceId: 8 bytes (long hash)
     */
    public static class PeerPacket {
        public long timestamp;
        public float latitude;
        public float longitude;
        public float confidence;
        public long deviceId;
        
        public PeerPacket() {}
        
        public PeerPacket(Location location, long deviceId) {
            this.timestamp = System.currentTimeMillis();
            this.latitude = (float) location.getLatitude();
            this.longitude = (float) location.getLongitude();
            this.confidence = location.getConfidence();
            this.deviceId = deviceId;
        }
        
        /**
         * Serialize packet to bytes
         */
        public byte[] toBytes() {
            ByteBuffer buffer = ByteBuffer.allocate(PACKET_SIZE);
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.putLong(timestamp);
            buffer.putFloat(latitude);
            buffer.putFloat(longitude);
            buffer.putFloat(confidence);
            buffer.putLong(deviceId);
            return buffer.array();
        }
        
        /**
         * Deserialize packet from bytes
         */
        public static PeerPacket fromBytes(byte[] data) {
            if (data.length != PACKET_SIZE) {
                throw new IllegalArgumentException("Invalid packet size");
            }
            
            ByteBuffer buffer = ByteBuffer.wrap(data);
            buffer.order(ByteOrder.BIG_ENDIAN);
            
            PeerPacket packet = new PeerPacket();
            packet.timestamp = buffer.getLong();
            packet.latitude = buffer.getFloat();
            packet.longitude = buffer.getFloat();
            packet.confidence = buffer.getFloat();
            packet.deviceId = buffer.getLong();
            
            return packet;
        }
        
        /**
         * Convert to Location object
         */
        public Location toLocation() {
            Location location = new Location(latitude, longitude);
            location.setConfidence(confidence);
            location.setTimestamp(timestamp);
            location.setSource(Location.LocationSource.PEER_FUSED);
            return location;
        }
        
        /**
         * Check if packet is expired
         */
        public boolean isExpired(long maxAgeMs) {
            return (System.currentTimeMillis() - timestamp) > maxAgeMs;
        }
    }
}
