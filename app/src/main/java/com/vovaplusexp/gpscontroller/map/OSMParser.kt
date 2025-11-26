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

import de.topobyte.osm4j.core.access.OsmIterator
import de.topobyte.osm4j.core.model.iface.OsmNode
import de.topobyte.osm4j.core.model.iface.OsmTag
import de.topobyte.osm4j.core.model.iface.OsmWay
import de.topobyte.osm4j.pbf.seq.PbfIterator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileInputStream

/**
 * Parses OSM PBF files and extracts road data
 *
 * This implementation provides a simplified OSM parser that extracts
 * road segments from .osm.pbf files and populates the road database.
 *
 * Note: For production use, consider using a dedicated OSM library like
 * osmosis or osm4j for better performance and PBF format support.
 */
class OSMParser(private val roadDatabase: RoadDatabase) {

    private var listener: ParserListener? = null
    private var isCancelled = false

    // Node cache for coordinate lookups
    private val nodeCache = HashMap<Long, NodeData>()

    /**
     * Internal class to store node coordinates
     */
    private data class NodeData(val lat: Double, val lon: Double)

    interface ParserListener {
        fun onProgress(percent: Int)
        fun onComplete(roadCount: Int)
        fun onError(error: String)
    }

    fun setParserListener(listener: ParserListener) {
        this.listener = listener
    }

    /**
     * Parse OSM file and populate database
     *
     * This implementation supports real OSM PBF file parsing with fallback
     * to sample network generation for non-PBF files.
     */
    fun parseFile(osmFile: File) {
        isCancelled = false

        GlobalScope.launch(Dispatchers.IO) {
            try {
                Timber.i("Parsing OSM file: %s", osmFile.absolutePath)

                if (!osmFile.exists()) {
                    notifyError("OSM file not found")
                    return@launch
                }

                val fileSize = osmFile.length()
                Timber.i("OSM file size: %d bytes", fileSize)

                // Clear node cache
                nodeCache.clear()

                val roadCount = if (osmFile.name.endsWith(".osm.pbf")) {
                    parsePbfFile(osmFile, fileSize)
                } else {
                    // Fallback: generate sample network
                    Timber.w("Not a PBF file, generating sample network")
                    generateSampleRoadNetwork()
                }

                notifyComplete(roadCount)

            } catch (e: Exception) {
                Timber.e(e, "Error parsing OSM file")
                notifyError(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Cancel ongoing parsing operation
     */
    fun cancel() {
        isCancelled = true
        nodeCache.clear()
    }

    /**
     * Parse PBF file with real OSM data
     *
     * @param osmFile OSM PBF file to parse
     * @param fileSize Size of the file for progress tracking
     * @return Number of road segments created
     */
    private fun parsePbfFile(osmFile: File, fileSize: Long): Int {
        var roadCount = 0
        var bytesRead: Long
        var lastProgress = 0

        // First pass: collect node coordinates
        Timber.i("First pass: collecting nodes")
        FileInputStream(osmFile).use { input ->
            val iterator: OsmIterator = PbfIterator(input, true)

            while (iterator.hasNext()) {
                if (isCancelled) {
                    notifyError("Parsing cancelled")
                    return 0
                }

                val entity = iterator.next()

                if (entity is OsmNode) {
                    // Limit cache size
                    if (nodeCache.size < NODE_CACHE_SIZE) {
                        nodeCache[entity.id] = NodeData(entity.latitude, entity.longitude)
                    }
                }

                // Update progress (0-50%)
                bytesRead = input.channel.position()
                val progress = ((bytesRead * 50) / fileSize).toInt()
                if (progress != lastProgress) {
                    notifyProgress(progress)
                    lastProgress = progress
                }
            }
        }

        Timber.i("Collected %d nodes", nodeCache.size)

        // Second pass: process roads
        Timber.i("Second pass: processing roads")
        FileInputStream(osmFile).use { input ->
            val iterator: OsmIterator = PbfIterator(input, true)
            bytesRead = 0
            lastProgress = 50

            while (iterator.hasNext()) {
                if (isCancelled) {
                    notifyError("Parsing cancelled")
                    return 0
                }

                val entity = iterator.next()

                if (entity is OsmWay) {
                    // Check if this is a road
                    if (isHighway(entity)) {
                        val segments = processRoadWay(entity)
                        roadCount += segments
                    }
                }

                // Update progress (50-100%)
                bytesRead = input.channel.position()
                val progress = 50 + ((bytesRead * 50) / fileSize).toInt()
                if (progress != lastProgress) {
                    notifyProgress(progress)
                    lastProgress = progress
                }
            }
        }

        Timber.i("Processed %d road segments", roadCount)
        return roadCount
    }

    /**
     * Check if a way is a highway/road
     *
     * @param way OSM way to check
     * @return true if the way is a road
     */
    private fun isHighway(way: OsmWay): Boolean {
        for (i in 0 until way.numberOfTags) {
            val tag: OsmTag = way.getTag(i)
            if (tag.key == "highway") {
                val value = tag.value
                return value in listOf(
                    "motorway", "trunk", "primary", "secondary", "tertiary", "residential",
                    "unclassified", "service", "motorway_link", "trunk_link", "primary_link",
                    "secondary_link"
                )
            }
        }
        return false
    }

    /**
     * Process a road way and extract segments
     *
     * @param way OSM way representing a road
     * @return Number of segments created
     */
    private fun processRoadWay(way: OsmWay): Int {
        var segmentCount = 0
        val nodeCount = way.numberOfNodes
        if (nodeCount < 2) {
            return 0
        }

        val lats = DoubleArray(nodeCount)
        val lons = DoubleArray(nodeCount)
        var validNodes = 0

        // Get node coordinates from cache
        for (i in 0 until nodeCount) {
            val nodeId = way.getNodeId(i)
            val node = nodeCache[nodeId]
            if (node != null) {
                lats[validNodes] = node.lat
                lons[validNodes] = node.lon
                validNodes++
            }
        }

        // Create segments between consecutive nodes
        for (i in 0 until validNodes - 1) {
            roadDatabase.insertRoadSegment(
                lats[i], lons[i],
                lats[i + 1], lons[i + 1]
            )
            segmentCount++
        }
        return segmentCount
    }

    /**
     * Generate sample road network for testing
     * This creates a grid of roads for demonstration purposes
     *
     * @return Number of road segments created
     */
    private fun generateSampleRoadNetwork(): Int {
        Timber.i("Generating sample road network")
        var roadCount = 0

        // Moscow city center coordinates (example)
        val centerLat = 55.7558
        val centerLon = 37.6173
        val gridSize = 0.01 // approximately 1 km
        val gridDimension = 20 // 20x20 grid

        // Create horizontal roads
        for (i in 0 until gridDimension) {
            val lat = centerLat - gridDimension / 2.0 * gridSize + i * gridSize
            for (j in 0 until gridDimension - 1) {
                val lon1 = centerLon - gridDimension / 2.0 * gridSize + j * gridSize
                val lon2 = lon1 + gridSize
                roadDatabase.insertRoadSegment(lat, lon1, lat, lon2)
                roadCount++
            }
        }

        // Create vertical roads
        for (i in 0 until gridDimension) {
            val lon = centerLon - gridDimension / 2.0 * gridSize + i * gridSize
            for (j in 0 until gridDimension - 1) {
                val lat1 = centerLat - gridDimension / 2.0 * gridSize + j * gridSize
                val lat2 = lat1 + gridSize
                roadDatabase.insertRoadSegment(lat1, lon, lat2, lon)
                roadCount++
            }
        }

        Timber.i("Generated %d sample road segments", roadCount)
        return roadCount
    }

    /**
     * Notify listener of progress
     */
    private fun notifyProgress(percent: Int) {
        listener?.onProgress(percent)
    }

    /**
     * Notify listener of completion
     */
    private fun notifyComplete(roadCount: Int) {
        listener?.onComplete(roadCount)
    }

    /**
     * Notify listener of error
     */
    private fun notifyError(error: String) {
        listener?.onError(error)
    }

    companion object {
        private const val NODE_CACHE_SIZE = 100000
    }
}
