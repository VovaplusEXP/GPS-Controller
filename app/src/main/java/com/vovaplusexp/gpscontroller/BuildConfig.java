/*
 * GPS-Controller - Inertial Navigation with GPS Spoofing Detection
 * Copyright (C) 2024 VovaplusEXP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.vovaplusexp.gpscontroller;

/**
 * Build configuration constants
 */
public final class BuildConfig {
    public static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("debug", "true"));
    public static final String APPLICATION_ID = "com.vovaplusexp.gpscontroller";
    public static final String BUILD_TYPE = "debug";
    public static final int VERSION_CODE = 1;
    public static final String VERSION_NAME = "1.0";
}
