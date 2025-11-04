/*
 * GPS-Controller - Inertial Navigation with GPS Spoofing Detection
 * Copyright (C) 2024 VovaplusEXP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.vovaplusexp.gpscontroller.spoofing;

import com.vovaplusexp.gpscontroller.spoofing.GpsSpoofingDetector.SpoofingFlag;
import com.vovaplusexp.gpscontroller.spoofing.GpsSpoofingDetector.TrustLevel;
import java.util.List;

/**
 * Analyzes location trust and provides recommendations
 */
public class LocationTrustAnalyzer {
    
    /**
     * Result of location trust analysis
     */
    public static class LocationTrust {
        private final TrustLevel trustLevel;
        private final List<SpoofingFlag> flags;
        private final float confidence;
        
        public LocationTrust(TrustLevel trustLevel, List<SpoofingFlag> flags, float confidence) {
            this.trustLevel = trustLevel;
            this.flags = flags;
            this.confidence = confidence;
        }
        
        public TrustLevel getTrustLevel() {
            return trustLevel;
        }
        
        public List<SpoofingFlag> getFlags() {
            return flags;
        }
        
        public float getConfidence() {
            return confidence;
        }
        
        public boolean isTrusted() {
            return trustLevel == TrustLevel.TRUSTED;
        }
        
        public boolean isSpoofed() {
            return trustLevel == TrustLevel.SPOOFED;
        }
        
        public String getDescription() {
            if (flags.isEmpty()) {
                return "GPS signal appears normal";
            }
            
            StringBuilder sb = new StringBuilder("Suspicious indicators: ");
            for (int i = 0; i < flags.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(flagToString(flags.get(i)));
            }
            return sb.toString();
        }
        
        private String flagToString(SpoofingFlag flag) {
            switch (flag) {
                case TELEPORTATION: return "Unrealistic speed";
                case SPEED_MISMATCH: return "Speed mismatch";
                case BEARING_MISMATCH: return "Direction mismatch";
                case MOCK_PROVIDER: return "Mock location";
                case NO_MOVEMENT: return "No device movement";
                default: return flag.toString();
            }
        }
    }
}
