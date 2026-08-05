package com.destinweather.data.repository

import com.destinweather.data.model.RainViewerResponse
import com.destinweather.viewmodel.RadarFrame
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class RadarRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun fetchRadarTimestamps(): Result<List<RadarFrame>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.rainviewer.com/public/weather-maps.json")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val json = response.body?.string()
                val apiResponse = gson.fromJson(json, RainViewerResponse::class.java)

                // Last 7 past frames (~1hr, 10min apart) + up to 3 nowcast frames (future)
                val pastFrames = apiResponse.radar.past.map { frameData ->
                    RadarFrame(
                        time = frameData.time,
                        path = frameData.path,
                        isNowcast = false
                    )
                }.takeLast(7)

                val futureFrames = apiResponse.radar.nowcast.map { frameData ->
                    RadarFrame(
                        time = frameData.time,
                        path = frameData.path,
                        isNowcast = true
                    )
                }.take(3)

                Result.success(pastFrames + futureFrames)
            } else {
                Result.failure(IOException("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
