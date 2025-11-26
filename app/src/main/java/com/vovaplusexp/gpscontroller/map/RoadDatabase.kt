/*
 * GPS-Controller - Inertial Navigation with GPS Spoofing Detection
 * Copyright (C) 2024 VovaplusEXP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.vovaplusexp.gpscontroller.map

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.vovaplusexp.gpscontroller.models.RoadSegment
import java.io.File
import java.util.ArrayList

/**
 * SQLite database for road segments
 */
class RoadDatabase(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = "CREATE TABLE $TABLE_ROADS (" +
                "$COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COL_LAT1 REAL NOT NULL, " +
                "$COL_LON1 REAL NOT NULL, " +
                "$COL_LAT2 REAL NOT NULL, " +
                "$COL_LON2 REAL NOT NULL, " +
                "$COL_MIN_LAT REAL NOT NULL, " +
                "$COL_MAX_LAT REAL NOT NULL, " +
                "$COL_MIN_LON REAL NOT NULL, " +
                "$COL_MAX_LON REAL NOT NULL)"
        db.execSQL(createTable)

        // Create spatial index
        val createIndex = "CREATE INDEX idx_spatial ON $TABLE_ROADS " +
                "($COL_MIN_LAT, $COL_MAX_LAT, $COL_MIN_LON, $COL_MAX_LON)"
        db.execSQL(createIndex)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ROADS")
        onCreate(db)
    }

    /**
     * Insert a road segment
     */
    fun insertRoadSegment(lat1: Double, lon1: Double, lat2: Double, lon2: Double) {
        val db = writableDatabase

        val minLat = lat1.coerceAtMost(lat2)
        val maxLat = lat1.coerceAtLeast(lat2)
        val minLon = lon1.coerceAtMost(lon2)
        val maxLon = lon1.coerceAtLeast(lon2)

        val sql = "INSERT INTO $TABLE_ROADS VALUES (NULL, ?, ?, ?, ?, ?, ?, ?, ?)"
        db.execSQL(sql, arrayOf(lat1, lon1, lat2, lon2, minLat, maxLat, minLon, maxLon))
    }

    /**
     * Find nearest road segments within radius
     */
    fun findNearestRoads(lat: Double, lon: Double, radiusMeters: Double): List<RoadSegment> {
        val segments = ArrayList<RoadSegment>()

        // Convert radius to approximate degrees (rough estimate)
        val radiusDeg = radiusMeters / 111000.0 // ~111km per degree

        val minLat = lat - radiusDeg
        val maxLat = lat + radiusDeg
        val minLon = lon - radiusDeg
        val maxLon = lon + radiusDeg

        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_ROADS " +
                " WHERE $COL_MIN_LAT <= ? AND $COL_MAX_LAT >= ?" +
                " AND $COL_MIN_LON <= ? AND $COL_MAX_LON >= ?" +
                " LIMIT 100"

        val cursor: Cursor = db.rawQuery(
            query,
            arrayOf(
                maxLat.toString(),
                minLat.toString(),
                maxLon.toString(),
                minLon.toString()
            )
        )

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(0)
                val lat1 = cursor.getDouble(1)
                val lon1 = cursor.getDouble(2)
                val lat2 = cursor.getDouble(3)
                val lon2 = cursor.getDouble(4)

                segments.add(RoadSegment(id, lat1, lon1, lat2, lon2))
            } while (cursor.moveToNext())
        }
        cursor.close()

        return segments
    }

    /**
     * Clear all road segments
     */
    fun clearAll() {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_ROADS")
    }

    /**
     * Get database size in bytes
     */
    fun getDatabaseSize(): Long {
        val db = readableDatabase
        val path = db.path
        if (path != null) {
            val dbFile = File(path)
            return dbFile.length()
        }
        return 0
    }

    companion object {
        private const val DATABASE_NAME = "roads.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_ROADS = "road_segments"
        private const val COL_ID = "id"
        private const val COL_LAT1 = "lat1"
        private const val COL_LON1 = "lon1"
        private const val COL_LAT2 = "lat2"
        private const val COL_LON2 = "lon2"
        private const val COL_MIN_LAT = "min_lat"
        private const val COL_MAX_LAT = "max_lat"
        private const val COL_MIN_LON = "min_lon"
        private const val COL_MAX_LON = "max_lon"
    }
}
