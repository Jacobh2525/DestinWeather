package com.destinweather.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class RadarRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // NOAA NWS radar tile URLs
    // Format: https://opengeo.ncep.noaa.gov/geoserver/conus/conus_cref_qcd/ows?
    // service=WMS&version=1.3.0&request=GetMap&layers=conus_cref_qcd&styles=&
    // bbox={minLon},{minLat},{maxLon},{maxLat}&width=512&height=512&crs=EPSG:4326&format=image/png

    suspend fun fetchRadarTile(
        minLat: Double,
        minLon: Double,
        maxLat: Double,
        maxLon: Double,
        timestamp: String? = null
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            // Build the WMS request URL for NOAA radar tiles
            val url = buildString {
                append("https://opengeo.ncep.noaa.gov/geoserver/conus/conus_cref_qcd/ows")
                append("?service=WMS")
                append("&version=1.3.0")
                append("&request=GetMap")
                append("&layers=conus_cref_qcd")
                append("&styles=")
                append("&bbox=$minLat,$minLon,$maxLat,$maxLon")
                append("&width=512")
                append("&height=512")
                append("&crs=EPSG:4326")
                append("&format=image/png")
                append("&transparent=true")
                timestamp?.let { append("&time=$it") }
            }

            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                response.body?.bytes()?.let { bytes ->
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    Result.success(bitmap)
                } ?: Result.failure(IOException("Empty response body"))
            } else {
                Result.failure(IOException("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Alternative: Use RainViewer API (free, no key required for basic usage)
    // https://api.rainviewer.com/public/weather-maps.json
    suspend fun fetchRainViewerTile(
        z: Int,
        x: Int,
        y: Int,
        timestamp: Int
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            // RainViewer tiles format
            val url = "https://tilecache.rainviewer.com/v2/radar/$timestamp/256/$z/$x/$y/2/1_1.png"

            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                response.body?.bytes()?.let { bytes ->
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    Result.success(bitmap)
                } ?: Result.failure(IOException("Empty response body"))
            } else {
                Result.failure(IOException("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Fetch available radar timestamps from RainViewer
    suspend fun fetchRadarTimestamps(): Result<List<Int>> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.rainviewer.com/public/weather-maps.json"

            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val json = response.body?.string()
                // Parse the JSON to extract radar timestamps
                // The response contains "radar": {"past": [...], "nowcast": [...]}
                val timestamps = mutableListOf<Int>()

                json?.let {
                    // Extract past radar frames
                    val pastPattern = """"past":\s*\[(.*?)\]""".toRegex()
                    val pastMatch = pastPattern.find(it)
                    pastMatch?.groupValues?.get(1)?.let { pastData ->
                        val timePattern = """"time":\s*(\d+)""".toRegex()
                        timePattern.findAll(pastData).forEach { match ->
                            timestamps.add(match.groupValues[1].toInt())
                        }
                    }
                }

                Result.success(timestamps)
            } else {
                Result.failure(IOException("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        // Zoom level to degree mapping (approximate)
        fun getBoundsForZoomLevel(zoom: Int, centerLat: Double, centerLon: Double): BoundingBox {
            val latSpan = 180.0 / (1 shl zoom)
            val lonSpan = 360.0 / (1 shl zoom)

            return BoundingBox(
                minLat = centerLat - latSpan / 2,
                maxLat = centerLat + latSpan / 2,
                minLon = centerLon - lonSpan / 2,
                maxLon = centerLon + lonSpan / 2
            )
        }
    }

    data class BoundingBox(
        val minLat: Double,
        val maxLat: Double,
        val minLon: Double,
        val maxLon: Double
    )
}
