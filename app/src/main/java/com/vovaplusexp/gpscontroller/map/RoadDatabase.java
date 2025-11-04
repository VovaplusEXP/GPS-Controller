/*
 * GPS-Controller - Inertial Navigation with GPS Spoofing Detection
 * Copyright (C) 2024 VovaplusEXP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.vovaplusexp.gpscontroller.map;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.vovaplusexp.gpscontroller.models.RoadSegment;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite database for road segments
 */
public class RoadDatabase extends SQLiteOpenHelper {
    
    private static final String DATABASE_NAME = "roads.db";
    private static final int DATABASE_VERSION = 1;
    
    private static final String TABLE_ROADS = "road_segments";
    private static final String COL_ID = "id";
    private static final String COL_LAT1 = "lat1";
    private static final String COL_LON1 = "lon1";
    private static final String COL_LAT2 = "lat2";
    private static final String COL_LON2 = "lon2";
    private static final String COL_MIN_LAT = "min_lat";
    private static final String COL_MAX_LAT = "max_lat";
    private static final String COL_MIN_LON = "min_lon";
    private static final String COL_MAX_LON = "max_lon";
    
    public RoadDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_ROADS + " (" +
            COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_LAT1 + " REAL NOT NULL, " +
            COL_LON1 + " REAL NOT NULL, " +
            COL_LAT2 + " REAL NOT NULL, " +
            COL_LON2 + " REAL NOT NULL, " +
            COL_MIN_LAT + " REAL NOT NULL, " +
            COL_MAX_LAT + " REAL NOT NULL, " +
            COL_MIN_LON + " REAL NOT NULL, " +
            COL_MAX_LON + " REAL NOT NULL)";
        db.execSQL(createTable);
        
        // Create spatial index
        String createIndex = "CREATE INDEX idx_spatial ON " + TABLE_ROADS + 
            "(" + COL_MIN_LAT + ", " + COL_MAX_LAT + ", " + 
            COL_MIN_LON + ", " + COL_MAX_LON + ")";
        db.execSQL(createIndex);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROADS);
        onCreate(db);
    }
    
    /**
     * Insert a road segment
     */
    public void insertRoadSegment(double lat1, double lon1, double lat2, double lon2) {
        SQLiteDatabase db = getWritableDatabase();
        
        double minLat = Math.min(lat1, lat2);
        double maxLat = Math.max(lat1, lat2);
        double minLon = Math.min(lon1, lon2);
        double maxLon = Math.max(lon1, lon2);
        
        String sql = "INSERT INTO " + TABLE_ROADS + " VALUES (NULL, ?, ?, ?, ?, ?, ?, ?, ?)";
        db.execSQL(sql, new Object[]{lat1, lon1, lat2, lon2, minLat, maxLat, minLon, maxLon});
    }
    
    /**
     * Find nearest road segments within radius
     */
    public List<RoadSegment> findNearestRoads(double lat, double lon, double radiusMeters) {
        List<RoadSegment> segments = new ArrayList<>();
        
        // Convert radius to approximate degrees (rough estimate)
        double radiusDeg = radiusMeters / 111000.0; // ~111km per degree
        
        double minLat = lat - radiusDeg;
        double maxLat = lat + radiusDeg;
        double minLon = lon - radiusDeg;
        double maxLon = lon + radiusDeg;
        
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_ROADS + 
            " WHERE " + COL_MIN_LAT + " <= ? AND " + COL_MAX_LAT + " >= ?" +
            " AND " + COL_MIN_LON + " <= ? AND " + COL_MAX_LON + " >= ?" +
            " LIMIT 100";
        
        Cursor cursor = db.rawQuery(query, new String[]{
            String.valueOf(maxLat),
            String.valueOf(minLat),
            String.valueOf(maxLon),
            String.valueOf(minLon)
        });
        
        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(0);
                double lat1 = cursor.getDouble(1);
                double lon1 = cursor.getDouble(2);
                double lat2 = cursor.getDouble(3);
                double lon2 = cursor.getDouble(4);
                
                segments.add(new RoadSegment(id, lat1, lon1, lat2, lon2));
            } while (cursor.moveToNext());
        }
        cursor.close();
        
        return segments;
    }
    
    /**
     * Clear all road segments
     */
    public void clearAll() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_ROADS);
    }
    
    /**
     * Get database size in bytes
     */
    public long getDatabaseSize() {
        SQLiteDatabase db = getReadableDatabase();
        return db.getPageSize() * db.getPageCount();
    }
}
