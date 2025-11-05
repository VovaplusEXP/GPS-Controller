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
import android.os.Handler;
import android.os.Looper;
import okhttp3.*;
import timber.log.Timber;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Downloads and manages OSM map data
 */
public class MapDownloadManager {
    
    private static final String OSM_URL = "https://download.geofabrik.de/russia/central-fed-district-latest.osm.pbf";
    
    private final Context context;
    private final OkHttpClient httpClient;
    private DownloadListener listener;
    
    public interface DownloadListener {
        void onProgress(int percent);
        void onComplete(File file);
        void onError(String error);
    }
    
    public MapDownloadManager(Context context) {
        this.context = context;
        this.httpClient = new OkHttpClient();
    }
    
    public void setDownloadListener(DownloadListener listener) {
        this.listener = listener;
    }
    
    /**
     * Download OSM map file
     */
    public void downloadMap() {
        Request request = new Request.Builder()
            .url(OSM_URL)
            .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Timber.e(e, "Map download failed");
                notifyError("Download failed: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) {
                if (!response.isSuccessful()) {
                    notifyError("Download failed: " + response.code());
                    return;
                }
                
                try {
                    File outputFile = new File(context.getFilesDir(), "map.osm.pbf");
                    InputStream input = response.body().byteStream();
                    FileOutputStream output = new FileOutputStream(outputFile);
                    
                    long totalBytes = response.body().contentLength();
                    long downloadedBytes = 0;
                    byte[] buffer = new byte[4096];
                    int read;
                    
                    while ((read = input.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                        downloadedBytes += read;
                        
                        if (totalBytes > 0) {
                            int percent = (int) ((downloadedBytes * 100) / totalBytes);
                            notifyProgress(percent);
                        }
                    }
                    
                    output.flush();
                    output.close();
                    input.close();
                    
                    notifyComplete(outputFile);
                } catch (IOException e) {
                    Timber.e(e, "Error saving map file");
                    notifyError("Error saving file: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Get downloaded map file
     */
    public File getMapFile() {
        File file = new File(context.getFilesDir(), "map.osm.pbf");
        return file.exists() ? file : null;
    }
    
    /**
     * Delete downloaded map
     */
    public boolean deleteMap() {
        File file = getMapFile();
        return file != null && file.delete();
    }
    
    private void notifyProgress(int percent) {
        if (listener != null) {
            new Handler(Looper.getMainLooper()).post(() -> listener.onProgress(percent));
        }
    }
    
    private void notifyComplete(File file) {
        if (listener != null) {
            new Handler(Looper.getMainLooper()).post(() -> listener.onComplete(file));
        }
    }
    
    private void notifyError(String error) {
        if (listener != null) {
            new Handler(Looper.getMainLooper()).post(() -> listener.onError(error));
        }
    }
}
