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
import android.os.Handler
import android.os.Looper
import okhttp3.*
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Downloads and manages OSM map data
 */
class MapDownloadManager(private val context: Context) {

    private val httpClient: OkHttpClient = OkHttpClient()
    private var listener: DownloadListener? = null

    interface DownloadListener {
        fun onProgress(percent: Int)
        fun onComplete(file: File)
        fun onError(error: String)
    }

    fun setDownloadListener(listener: DownloadListener) {
        this.listener = listener
    }

    /**
     * Download OSM map file
     */
    fun downloadMap() {
        val request = Request.Builder()
            .url(OSM_URL)
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Timber.e(e, "Map download failed")
                notifyError("Download failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    notifyError("Download failed: ${response.code}")
                    return
                }

                try {
                    val outputFile = File(context.filesDir, "map.osm.pbf")
                    val input: InputStream? = response.body?.byteStream()
                    val output = FileOutputStream(outputFile)

                    val totalBytes = response.body?.contentLength() ?: -1
                    var downloadedBytes = 0L
                    val buffer = ByteArray(4096)
                    var read: Int

                    if (input != null) {
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloadedBytes += read

                            if (totalBytes > 0) {
                                val percent = ((downloadedBytes * 100) / totalBytes).toInt()
                                notifyProgress(percent)
                            }
                        }
                    }

                    output.flush()
                    output.close()
                    input?.close()

                    notifyComplete(outputFile)
                } catch (e: IOException) {
                    Timber.e(e, "Error saving map file")
                    notifyError("Error saving file: ${e.message}")
                }
            }
        })
    }

    /**
     * Get downloaded map file
     */
    fun getMapFile(): File? {
        val file = File(context.filesDir, "map.osm.pbf")
        return if (file.exists()) file else null
    }

    /**
     * Delete downloaded map
     */
    fun deleteMap(): Boolean {
        val file = getMapFile()
        return file != null && file.delete()
    }

    private fun notifyProgress(percent: Int) {
        listener?.let {
            Handler(Looper.getMainLooper()).post { it.onProgress(percent) }
        }
    }

    private fun notifyComplete(file: File) {
        listener?.let {
            Handler(Looper.getMainLooper()).post { it.onComplete(file) }
        }
    }

    private fun notifyError(error: String) {
        listener?.let {
            Handler(Looper.getMainLooper()).post { it.onError(error) }
        }
    }

    companion object {
        private const val OSM_URL = "https://download.geofabrik.de/russia/central-fed-district-latest.osm.pbf"
    }
}
